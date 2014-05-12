package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.TreeMap;

//doesn't necessarily have to use task 2 (could use task 1, in which case, you'd probably like to extend CosineSimilarityScorer instead)
public class SmallestWindowScorer extends BM25Scorer
{

	/////smallest window specifichyperparameters////////
	static double B = 10;    	    
	double boostmod = -1;
	static int maxWindowSize = Integer.MAX_VALUE;

	//////////////////////////////

	public SmallestWindowScorer(Map<String, Double> idfs,Map<Query,Map<String, Document>> queryDict) 
	{
		super(idfs, queryDict);
		//super(idfs);
		handleSmallestWindow();
	}


	public void handleSmallestWindow()
	{
		/*
		 * @//TODO : Your code here
		 */
	}

	public double calculateBoost(Query q, int windowSize){
		int query_size = q.queryWords.size();
		// exp
//		int diff = query_size - windowSize;
//		double boost = 1 + 4*Math.pow(Math.E, diff);

		// inverse
		int diff = windowSize - query_size + 1;
		double boost = 1 +  4.0/((double)diff);
		
		
	//	System.out.print("qs=" + query_size + ", ws=" + windowSize + ", boost=" + boost  + "\t");
		return boost;
	}

	private static int calcSmWindowSize(String strField, List<String> queryWords) {
		int smWindowInField = maxWindowSize;
		Map<String, Integer> mapQindex = new HashMap<String, Integer>(); // maintains most proximity indexes of query terms

		if ( queryWords.size() == 1){
			if ( (" " + strField + " ").contains(" " + queryWords.get(0) + " ")){
				return 1;
			}else{
				return maxWindowSize;
			}
		}


		String[] arrFieldStr = strField.split("\\s+");
		for (String qWord : queryWords) {
			mapQindex.put(qWord, -1);
		}
		for (int i = 0; i < arrFieldStr.length; i++) {
			String word = arrFieldStr[i];

			// update indexes in map
			if(!mapQindex.containsKey(word)){
				continue;
			}
			mapQindex.put(word, i);
			int minQindex = Collections.min(mapQindex.values());
			if ( minQindex < 0){ // still havent seen atleast 1 term so no window.
				continue;
			}else{	
				int currWindowSize = i - minQindex + 1;
				smWindowInField = (smWindowInField<currWindowSize) ? smWindowInField : currWindowSize;
			}
		}
		return smWindowInField;
	}


	private static int calcsmWindowSizeForBody(Map<String, List<Integer>> docHits, List<String> queryWords){
		int smWindowInField = maxWindowSize;
		// At any time represents last seen position of all terms
		Map<String, Integer> mapQindex = new HashMap<String, Integer>(); 

		for( String q : queryWords){
			if ( !docHits.containsKey(q)) { return maxWindowSize;}
			mapQindex.put(q, -1);
		}

		if ( queryWords.size() == 1){return 1;}


		TreeMap<Integer,String> indexAll = new TreeMap<Integer,String>();
		for (String docWord : docHits.keySet()) {
			for (Integer index : docHits.get(docWord)) {
				indexAll.put(index, docWord);
			}
		}

		for (Map.Entry<Integer, String> entry : indexAll.entrySet()){
			int index = entry.getKey();
			String word = entry.getValue();
			// update indexes in map
			if(!mapQindex.containsKey(word)){
				continue;
			}
			mapQindex.put(word, index);
			int minQindex = Collections.min(mapQindex.values());
			if ( minQindex < 0){ // still havent seen atleast 1 term so no window.
				continue;
			}else{	
				int currWindowSize = index - minQindex + 1;
				smWindowInField = (smWindowInField<currWindowSize) ? smWindowInField : currWindowSize;
			}
		}
		indexAll.clear(); indexAll = null;
		return smWindowInField;
	}

	public double getBoost(Query q, Document d){
		int smWindowSize = maxWindowSize;
		for (String tfType : TFTYPES) {
			if (tfType.equals("url")) {
				String url = scrub(d.url);
				int smWindowInField = calcSmWindowSize(url, q.queryWords);
				smWindowSize = (smWindowInField < smWindowSize) ? smWindowInField : smWindowSize;

			} 
			
			if (tfType.equals("title")
					&& d.title != null) {
				String title = d.title.toLowerCase();
				int smWindowInField = calcSmWindowSize(title, q.queryWords);
				smWindowSize = (smWindowInField < smWindowSize) ? smWindowInField : smWindowSize;

			} 
			
			if (tfType.equals("body")
					&& d.body_hits != null) {
				int smWindowInField = calcsmWindowSizeForBody(d.body_hits, q.queryWords);
				smWindowSize = (smWindowInField < smWindowSize) ? smWindowInField : smWindowSize;
			} 
			
			if (tfType.equals("header")
					&& d.headers != null) {
				for (String strHeader : d.headers) {
					int smWindowInField = calcSmWindowSize(strHeader, q.queryWords);
					smWindowSize = (smWindowInField < smWindowSize) ? smWindowInField : smWindowSize;
				}
			} 
			
			if (tfType.equals("anchor") 
					&& d.anchors != null) {
				for (String strAnchor : d.anchors.keySet()) {
					int smWindowInField = calcSmWindowSize(strAnchor, q.queryWords);
					smWindowSize = (smWindowInField < smWindowSize) ? smWindowInField : smWindowSize;
				}
			}
		}

		return calculateBoost(q, smWindowSize);
	}

	@Override
	public double getSimScore(Document d, Query q) {
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);

		this.normalizeTFs(tfs, d, q);

		Map<String,Double> tfQuery = getQueryFreqs(q);
		double score  = this.getNetScore(tfs, q, tfQuery, d);
		double boost = getBoost(q, d);
		double netScore = score*boost;

		//System.out.println("q=" + q + ", boost=" + boost + ", score=" + score + ", fin=" + score*boost);
		return netScore;
	}


	public static String scrub(String input){
		return input.toLowerCase().replaceAll("[^0-9a-z]", " ").replace("\\s+", " ").trim();
	}
	
	public static void main(String args[]){
		List<String> q = new ArrayList<>();
		q.add("bill"); q.add("gates");q.add("talks");
		System.out.println(calcSmWindowSize("talks at stanford bill gates says talks foreign aid is threatened but big ideas can turn the tide" , q));
		Map<String,List<Integer>> dochist = new HashMap<String,List<Integer>>();
		Integer t[] = {22, 35, 87 ,98, 195, 354, 375, 895};
		dochist.put("bill", Arrays.asList(t));
		Integer t2[] = {23 ,36, 55, 88, 149, 176, 198, 235, 313,355 ,376, 408, 456, 551, 573, 587, 740, 774, 821, 829, 840, 896};
		dochist.put("gates", Arrays.asList(t2));
		Integer t3[] = {100 ,899};
		dochist.put("talks", Arrays.asList(t3));
		
		System.out.println(calcsmWindowSizeForBody(dochist, q));
	}

}
