package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import com.sun.org.apache.bcel.internal.generic.GETSTATIC;

import edu.stanford.cs276.util.Stemmer;
import edu.stanford.cs276.util.StopWordHandler;
import edu.stanford.cs276.util.stemmer.EnglishSnowballStemmerFactory;

public class ExtraCreditScorer extends BM25Scorer
{

	public ExtraCreditScorer(Map<String, Double> idfs,Map<Query,Map<String, Document>> queryDict) 
	{
		super(idfs, queryDict);
	}

	public final Character[] alphabet = {'s',' ','-'};
	
	public void computeExhaustiveOneEdits(String query,
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
	
	public List<String> checkUnigramsInQuery(String candidate, List<String> query_words, List<String> qTermsInUrl) {
		String[] termsCand= candidate.split(" ");
		//check if all unigrams of this candidate are present in our query, then add to candidates
		for (int j = 0; j < termsCand.length; j++) {
			String currentTermOfQuery = termsCand[j];
			if(query_words.contains(Stemmer.getStem(currentTermOfQuery))){
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
		double relevanceURL = calcURLrelevance(q.queryWords, d.url);
		double score = this.getNetScore(tfs, q, tfQuery, d);
		double netScore = Math.log10(score)+Math.log10(relevanceURL);
		return netScore;
	}

	public double calcURLrelevance(List<String> queryWords, String url) {
		
		Map<String, Integer> qTermsInUrl = new HashMap<String, Integer>();
		int numMatch = 0;
		String[] strPartial = url.split("/+");
		
		List<String> stemmedQwords = new ArrayList<String>();
		for (String string : queryWords) {
			stemmedQwords.add(Stemmer.getStem(string));
		}
		
		for (String string : strPartial) {
			List<String> qTermsInPartialUrl= new ArrayList<String>();
			String strUrlpart = Stemmer.scrub(string);
			Set<String> allCandidates = new HashSet<String>();
			allCandidates.add(strUrlpart);
			computeExhaustiveOneEdits(strUrlpart, allCandidates);
			for (String cand : allCandidates) {
				checkUnigramsInQuery(cand, queryWords, qTermsInPartialUrl);
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
		
		double relScore = 1.0d + (double)numMatch*0.01d;
		return relScore;
	}
}
