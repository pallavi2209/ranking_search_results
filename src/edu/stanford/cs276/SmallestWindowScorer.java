package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;
import java.util.Iterator;
import java.util.HashSet;

import edu.stanford.cs276.util.Stemmer;
import edu.stanford.cs276.util.StopWordHandler;

//doesn't necessarily have to use task 2 (could use task 1, in which case, you'd probably like to extend CosineSimilarityScorer instead)
public class SmallestWindowScorer extends BM25Scorer
{


	public SmallestWindowScorer(Map<String, Double> idfs,Map<Query,Map<String, Document>> queryDict) 
	{
		super(idfs, queryDict);
		//super(idfs);
		//handleSmallestWindow();
		
	}

	/////smallest window specifichyperparameters////////
	static double B = 0.05;    	    
	static double boostmod = 0.0001;
	// /////////////weights///////////////////////////
	static double urlweight = 8.0;
	static double titleweight = 6.0;
	static double bodyweight = 1.0;
	static double headerweight = 7.5;
	static double anchorweight = 1.2;

	// /////bm25 specific weights///////////////
	static double burl = 1.0d;
	static double btitle = 1.0d;
	static double bheader = 1.0d;
	static double bbody = 1.0d;
	static double banchor = 1.0d;

	static double k1 = 50.0;
	static double pageRankLambda = 80.0d;
	static double pageRankLambdaPrime = 1.0d;

	//////////////////////////////
	
	private void handleSmallestWindow() {
		setParameters(urlweight, titleweight, bodyweight, headerweight,
				anchorweight, burl, btitle, bheader, bbody, banchor, k1,
				pageRankLambda, pageRankLambdaPrime);
		calcAverageLengths();
		
	}

	@Override
	public double getSimScore(Document d, Query q) {
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
		this.normalizeTFs(tfs, d, q);
		Map<String,Double> tfQuery = getQueryFreqs(q);
		double netScore = getWindowScore(d, q, tfs, tfQuery);
		return netScore;
	}

	public double getWindowScore(Document d, Query q,
			Map<String, Map<String, Double>> tfs, Map<String, Double> tfQuery) {

		double score = this.getNetScore(tfs, q, tfQuery, d);
		double boost = getBoost(q, d);
		double netScore = score * boost;
		return netScore;
	}

	public ArrayList<String> getUniqueTerms(List<String> terms) {
		HashSet set = new HashSet();
		set.addAll(terms);
		ArrayList<String> unique = new ArrayList<String>();
		unique.addAll(set);
		return unique;
	}

	public double getBoost(Query q, Document d){

		int smallestWindow = Integer.MAX_VALUE;
		ArrayList<String> queryTerms = getUniqueTerms(q.queryWords);
		ArrayList<String> fields = new ArrayList<String>();
		fields.add(Stemmer.scrub(d.url));

		if (d.title != null) fields.add(d.title.toLowerCase());

		if (d.headers != null) {
			for (String header : d.headers) {
				fields.add(header);
			}
		}

		if (d.anchors != null) {
			for (String anchorText : d.anchors.keySet()) {
				fields.add(anchorText);
			}
		}

		for (String field : fields) {
			Map<String, List<Integer>> hits = getHits(field, q);
			int size = getWindowSize(hits, queryTerms);
			if (size < smallestWindow) smallestWindow = size;
		}

		if (d.body_hits != null) {
			int size = getWindowSize(d.body_hits, queryTerms);
			if (size < smallestWindow) smallestWindow = size;
		}
		
		List<String> queryTermsNoStop = StopWordHandler.removeStopWords(q.queryWords);

		double boost = calculateBoost(smallestWindow, queryTermsNoStop.size());

		System.out.println(boost);

		return boost;
	}


	private static Map<String, List<Integer>> getHits(String field, Query q) {
		
		List<String> stemmedQterms = new ArrayList<String>();
		for (String qWord : q.queryWords) {
			String stemmedWord = Stemmer.getStem(qWord);
			stemmedQterms.add(stemmedWord);
		}

		HashMap<String, List<Integer>> hits = new HashMap<String, List<Integer>>();

		String[] terms = field.split("\\s+");
		for (int i = 0; i < terms.length; i++) {
			String stemmedTerm = Stemmer.getStem(terms[i]);
			if (hits.containsKey(stemmedTerm)) {
				hits.get(stemmedTerm).add(i);
			}else{
				if(stemmedQterms.contains(stemmedTerm)){
					hits.put(stemmedTerm, new ArrayList<Integer>());
					hits.get(stemmedTerm).add(i);
				}
			}
		}

		return hits;

	}

	

	private static int getWindowSize(Map<String, List<Integer>> hits, List<String> queryWords){
		ArrayList<Integer> positions = new ArrayList<Integer>();
		ArrayList<Iterator<Integer>> iterators = new ArrayList<Iterator<Integer>>();

		for (String term : queryWords) {
			if(StopWordHandler.isStopWord(term)){
				continue;
			}
			String stemmedTerm = Stemmer.getStem(term);
			if (!hits.containsKey(stemmedTerm)) return Integer.MAX_VALUE;
			iterators.add(hits.get(stemmedTerm).iterator());
		}

		for (Iterator<Integer> i : iterators) {
			positions.add(i.next());
		}

		int smallestWindow = Integer.MAX_VALUE;

		while (true) {
			int window = Collections.max(positions) - Collections.min(positions) +1;
			if (window < smallestWindow) smallestWindow = window;
			if (!advanceWindow(positions, iterators)) break;
		}

		return smallestWindow;
		
	}

	private static boolean advanceWindow(List<Integer> positions, List<Iterator<Integer>> iterators) {
		int min = positions.get(0);
		int minIndex = 0;
		for (int i = 1; i < positions.size(); i++) {
			int x = positions.get(i);
			if (x < min) {
				x = min;
				minIndex = i;
			}
		}
		Iterator<Integer> iterator = iterators.get(minIndex);
		if (iterator.hasNext()) {
			positions.set(minIndex, iterator.next());
			return true;
		} else {
			return false;
		}

	}


	private static double calculateBoost(int window, int length) {

		if(window == Integer.MAX_VALUE){
			return 1.0;
		}
		double boost = 1.0 +  0.1/((double)(window-length) + 0.5);
		return boost;

	}
	
}
