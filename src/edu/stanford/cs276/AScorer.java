package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public abstract class AScorer {

	Map<String, Double> idfs;
	String[] TFTYPES = { "url", "title", "body", "header", "anchor" };
	Double INCR = 1.0d;

	public AScorer(Map<String, Double> idfs) {
		this.idfs = idfs;
	}

	// scores each document for each query
	public abstract double getSimScore(Document d, Query q);

	// handle the query vector
	public Map<String, Double> getQueryFreqs(Query q) {
		Map<String, Double> tfQuery = new HashMap<String, Double>();

		for (String query_word : q.queryWords) {
			Double count = 0.0d;
			if (tfQuery.containsKey(query_word)) {
				count = tfQuery.get(query_word) + INCR;
			}else{
				count = INCR;
			}
			Double idf = 0.0d;
			if(idfs.containsKey(query_word)){
				idf = idfs.get(query_word);
			}else{
				idf = idfs.get(LoadHandler.termNotPresent);
			}
			tfQuery.put(query_word, count*idf);
		}
		return tfQuery;
	}

	/*
	 * / Creates the various kinds of term frequences (url, title, body, header,
	 * and anchor) You can override this if you'd like, but it's likely that
	 * your concrete classes will share this implementation
	 */
	public Map<String, Map<String, Double>> getDocTermFreqs(Document d, Query q) {
		// map from tf type -> queryWord -> score
		Map<String, Map<String, Double>> tfs = new HashMap<String, Map<String, Double>>();
//		System.out.println("\n ---------------------- \n");
//		System.out.println("Query: " + q.toString());
//		System.out.println("Doc: " + d.toString());

//		System.out.println("Empty tfs" + tfs.toString());
		String sUrl = TFTYPES[0]; // "url"
		String docUrl = d.url;
		String[] docUrlWords = docUrl.toLowerCase().split("\\W+");

//		System.out
//				.println("url words:" + Arrays.asList(docUrlWords).toString());
		tfs.put(sUrl, new HashMap<String, Double>());
		for (String query_word : q.queryWords) {
			Double count = 0.0d;
			for (int i = 0; i < docUrlWords.length; i++) {
				if (query_word.equals(docUrlWords[i])) {
					tfs.get(sUrl).put(query_word, count += INCR);
				}
			}
		}

//		System.out.println("tfs url: " + tfs.get("url").toString());
		String sTitle = TFTYPES[1]; // title
		String docTitle = d.title;
		if (docTitle != null) {
			String[] docTitleWords = docTitle.toLowerCase().split(" ");
			tfs.put(sTitle, new HashMap<String, Double>());
			for (String query_word : q.queryWords) {
				Double count = 0.0d;
				for (int i = 0; i < docTitleWords.length; i++) {
					if (query_word.equals(docTitleWords[i])) {
						tfs.get(sTitle).put(query_word, count += INCR);
					}
				}
			}
		}

//		System.out.println("tfs title: "
//				+ (tfs.containsKey("title") ? tfs.get("title").toString()
//						: "No title"));
		String sBody = TFTYPES[2];// body
		Map<String, List<Integer>> docBodyHits = d.body_hits;
		if (docBodyHits != null) {
			tfs.put(sBody, new HashMap<String, Double>());
			for (String query_word : q.queryWords) {
				if (docBodyHits.containsKey(query_word)) {
					Double count = Double.valueOf((double) docBodyHits.get(
							query_word).size());
					tfs.get(sBody).put(query_word, count);
				}
			}
		}

//		System.out.println("tfs bodyhits: "
//				+ (tfs.containsKey("body") ? tfs.get("body").toString()
//						: "No body"));

		String sHeader = TFTYPES[3]; // header
		List<String> docHeaders = d.headers;
		if (docHeaders != null) {
			tfs.put(sHeader, new HashMap<String, Double>());

			for (String query_word : q.queryWords) {
				Double count = 0.0d;

				for (String string : docHeaders) {
					String[] docHeaderWords = string.toLowerCase().split(" ");

					for (int j = 0; j < docHeaderWords.length; j++) {
						if (query_word.equals(docHeaderWords[j])) {
							tfs.get(sHeader).put(query_word, count += INCR);
						}
					}
				}
			}
		}

//		System.out.println("tfs header: "
//				+ (tfs.containsKey("header") ? tfs.get("header").toString()
//						: "No header"));
		String sAnchor = TFTYPES[4];
		Map<String, Integer> docAnchors = d.anchors;
		if (docAnchors != null) {
			tfs.put(sAnchor, new HashMap<String, Double>());

			for (String query_word : q.queryWords) {
				Double count = 0.0d;

				for (Entry<String, Integer> anchorEntry : docAnchors.entrySet()) {
					String[] anchorWords = anchorEntry.getKey().toLowerCase()
							.split(" ");
					Double countThisAnchor = Double
							.valueOf((double) anchorEntry.getValue());

					for (int j = 0; j < anchorWords.length; j++) {
						if (query_word.equals(anchorWords[j])) {
							if (tfs.get(sAnchor).containsKey(query_word)) {
								Double prevCount = tfs.get(sAnchor).get(
										query_word);
								count = prevCount + countThisAnchor;
							} else {
								count = countThisAnchor;
							}
							tfs.get(sAnchor).put(query_word, count);
						}
					}

				}
			}
		}
//		System.out.println("tfs anchor: "
//				+ (tfs.containsKey("anchor") ? tfs.get("anchor").toString()
//						: "No anchor"));

		// // //////////////////Initialization/////////////////////
		//
		// /*
		// * @//TODO : Your code here
		// */
		//
		// // //////////////////////////////////////////////////////
		//
		// // ////////handle counts//////
		//
		// // loop through query terms increasing relevant tfs
		// for (String queryWord : q.queryWords) {
		// /*
		// * @//TODO : Your code here
		// */
		//
		// }

//		System.out.println("tfs map:" + tfs.toString());
		return tfs;
	}

}
