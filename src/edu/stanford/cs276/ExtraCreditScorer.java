package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

import edu.stanford.cs276.util.StopWordHandler;
import edu.stanford.cs276.util.stemmer.EnglishSnowballStemmerFactory;

public class ExtraCreditScorer extends SmallestWindowScorer
{

	public ExtraCreditScorer(Map<String, Double> idfs,Map<Query,Map<String, Document>> queryDict) 
	{
		super(idfs, queryDict);
	}
	
	
	///////start bm25 code

	public static final Character[] alphabet = {'s',' ',};
	
	public static void computeExhaustiveOneEdits(String query,
			Set<String> allCandidates) {
		// Deletion
		for (int idx = 0; idx < query.length(); ++idx)
			allCandidates.add(query.substring(0, idx)
					+ query.substring(idx + 1));

		// adjacent
		for (int idx = 0; idx < query.length() - 1; ++idx)
			allCandidates.add(query.substring(0, idx)
					+ query.substring(idx + 1, idx + 2)
					+ query.substring(idx, idx + 1) + query.substring(idx + 2));

		// replacements
		for (int idx = 0; idx < query.length(); ++idx) {
			for (int iAlpha = 0; iAlpha < alphabet.length; iAlpha++) {
				char character = alphabet[iAlpha].charValue();
				allCandidates.add(query.substring(0, idx)
						+ String.valueOf(character) + query.substring(idx + 1));
			}
		}

		// insertions
		for (int idx = 0; idx <= query.length(); ++idx) {
			for (int iAlpha = 0; iAlpha < alphabet.length; iAlpha++) {
				char character = alphabet[iAlpha].charValue();
				allCandidates.add(query.substring(0, idx)
						+ String.valueOf(character) + query.substring(idx));
			}
		}
	}
	
	public static String scrub(String input){
		return input.toLowerCase().replaceAll("[^0-9a-z]+", " ").replace("\\s+", " ").trim();
	}
	
	static List<String> checkUnigramsInQuery(String candidate, List<String> query_words, List<String> qTermsInUrl) {
		String[] termsCand= candidate.split(" ");
		//check if all unigrams of this candidate are present in our query, then add to candidates
		for (int j = 0; j < termsCand.length; j++) {
			String currentTermOfQuery = termsCand[j];
			if(query_words.contains(getStem(currentTermOfQuery))){
				if(!qTermsInUrl.contains(currentTermOfQuery)){
					qTermsInUrl.add(currentTermOfQuery);
				}
			}
		}
		return qTermsInUrl;
	}
	
	@Override
	public double getSimScore(Document d, Query q) {
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
		this.normalizeTFs(tfs, d, q);
		Map<String,Double> tfQuery = getQueryFreqs(q);
		
		double closenessOfFirstWord = calcCloseness(d);
		double uniquenessQueryTermsInDoc = calcUniqueness(q, tfs);
		double relevanceURL = calcURLrelevance(q.queryWords, d.url);
		
		double score = this.getWindowScore(d, q, tfs, tfQuery);
		//double netScore = score+ (1+Math.pow(Math.E, (-1.0d)*closenessOfFirstWord) + uniquenessQueryTermsInDoc);
		
		double netScore = Math.log10(score)+Math.log10(closenessOfFirstWord)+Math.log10(uniquenessQueryTermsInDoc)+Math.log10(relevanceURL);
		return netScore;
	}

	public static double calcURLrelevance(List<String> queryWords, String url) {
		
		Map<String, Integer> qTermsInUrl = new HashMap<String, Integer>();
		int numMatch = 0;
		String[] strPartial = url.split("/+");
		
		List<String> stemmedQwords = new ArrayList<String>();
		for (String string : queryWords) {
			stemmedQwords.add(getStem(string));
		}
		
		for (String string : strPartial) {
			List<String> qTermsInPartialUrl= new ArrayList<String>();
			String strUrlpart = scrub(string);
			Set<String> allCandidates = new HashSet<String>();
			allCandidates.add(strUrlpart);
			computeExhaustiveOneEdits(strUrlpart, allCandidates);
			for (String cand : allCandidates) {
				checkUnigramsInQuery(cand, stemmedQwords, qTermsInPartialUrl);
			}
			for (String term : qTermsInPartialUrl) {
				int count = 1;
				if(qTermsInUrl.containsKey(term)){
					count = qTermsInUrl.get(term)+1;
				}
				qTermsInUrl.put(term, count);
			}
		}
		for (int num : qTermsInUrl.values()) {
			numMatch+=num;
		}
		
		double relScore = 1.0d + (double)numMatch*0.1d;
		return relScore;
	}

	private double calcUniqueness(Query q, Map<String, Map<String, Double>> tfs) {
		double uniquenessQueryTermsInDoc;
		List<String> qWords = new ArrayList<String>();
		for (String string : q.queryWords) {
			qWords.add(string);
		}
		int numQueryWords = StopWordHandler.removeStopWords(qWords).size();
		//int numQueryWords = qWords.size();
		List<String> qTermsInDoc = new ArrayList<String>();
		for (Entry<String,Map<String, Double>> entry : tfs.entrySet()) {
			for (Entry<String, Double> mapEntry : entry.getValue().entrySet()) {
				String word = mapEntry.getKey();
				if(!qTermsInDoc.contains(word) && !StopWordHandler.isStopWord(word)){
					qTermsInDoc.add(mapEntry.getKey());
				}
			}
		}
		int numQTermsInDoc =  qTermsInDoc.size();
		double numUniqueTerms = (double)numQTermsInDoc/(double)numQueryWords;
		uniquenessQueryTermsInDoc = numUniqueTerms;
		return uniquenessQueryTermsInDoc;
	}

	private double calcCloseness(Document d) {
		double closenessOfFirstWord = 0.0d;
		int closestIndex = Integer.MAX_VALUE;
		if(d.body_hits!=null){
			for (Entry<String, List<Integer>> entry : d.body_hits.entrySet()) {
				if (!StopWordHandler.isStopWord(entry.getKey())) {
					for (int index : entry.getValue()) {
						closestIndex = (index < closestIndex) ? index
								: closestIndex;
					}
				}
			}
			
//			int zone_doc_length= d.body_length/5;
//			for(int i = 0; i<5 ; i++){
//				int minIndex = i*zone_doc_length;
//				int maxIndex = (i+1)*zone_doc_length;
//				if(closestIndex>=minIndex && closestIndex<=maxIndex){
//					closenessOfFirstWord = Math.pow(Math.E, ((i+1)));
//				}
//			}
			if(closestIndex<=100){
				closenessOfFirstWord = closestIndex;
			}
		}
		closenessOfFirstWord = 1+Math.pow(Math.E, (-1.0d)*closestIndex);
		return closenessOfFirstWord;
	}

	/*
	 * @//TODO : Your code here
	 */
	
	public static void main(String args[]){
		String url = "http://football.stanford.footballs/news/2011/september/cardinal_to_footballtickets-090711.html";
		List<String> q  = new ArrayList<String>();
		q.add("football");
		q.add("tickets");
		q.add("sales");
	
		System.out.println(q.toString().split("//s+").length);
	
		System.out.println(calcURLrelevance(q, url));
		System.out.println(scrub(url));
	}
	
	private static String getStem(String word){
		String stem_word = word;
		try {
			stem_word = EnglishSnowballStemmerFactory.getInstance().process(word);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return stem_word;
	}
	
//	private static String getStemString(String string){
//		StringBuilder strBuilder = new StringBuilder();
//		String[] words = string.split("//s+");
//		for (String word : words) {
//			String stem_word = word;
//			try {
//			stem_word = EnglishSnowballStemmerFactory.getInstance().process(word);
//			} catch (Exception e) {
//				e.printStackTrace();
//			}
//			strBuilder.append(stem_word);
//			strBuilder.append(" ");
//		}
//		return strBuilder.toString().trim();
//	}
	
	
}
