package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class BM25Scorer extends AScorer {
	Map<Query, Map<String, Document>> queryDict;

	public BM25Scorer(Map<String, Double> idfs,
			Map<Query, Map<String, Document>> queryDict) {
		super(idfs);
		this.queryDict = queryDict;
		this.calcAverageLengths();
	}

	// /////////////weights///////////////////////////
	double urlweight = 8.0;
	double titleweight = 6.0;
	double bodyweight = 1.0;
	double headerweight = 7.5;
	double anchorweight = 1.2;

	// /////bm25 specific weights///////////////
	double burl = 0.6;
	double btitle = 0.6;
	double bheader = 0.6;
	double bbody = 1.0;
	double banchor = 0.6;

	double k1 = 55.0;
	double pageRankLambda = 65.0d;
	double pageRankLambdaPrime = 1.0d;

	// ////////////////////////////////////////

	public void setParameters(double urlweight, double titleweight,
			double bodyweight, double headerweight, double anchorweight,
			double burl, double btitle, double bheader, double bbody,
			double banchor, double k1, double pageRankLambda,
			double pageRankLambdaPrime) {
		this.urlweight = urlweight;
		this.titleweight = titleweight;
		this.bodyweight = bodyweight;
		this.headerweight = headerweight;
		this.anchorweight = anchorweight;
		this.burl = burl;
		this.btitle = btitle;
		this.bheader = bheader;
		this.bbody = bbody;
		this.banchor = banchor;
		this.k1 = k1;
		this.pageRankLambda = pageRankLambda;
		this.pageRankLambdaPrime = pageRankLambdaPrime;

	}

    ////////////bm25 data structures--feel free to modify ////////
    
    Map<Document,Map<String,Double>> lengths;
    Map<String,Double> avgLengths;
    Map<Document,Double> pagerankScores;
    Map<String, Double> weights;
    Map<String, Double> bvals;
    
    //////////////////////////////////////////
   
   //simple helper function to computer total length of anchor text
    double calcAnchorsLength(Map<String, Integer> anchors) {
    	double length = 0.0;
    	for (Entry<String, Integer> anchor : anchors.entrySet()) {
    		length += anchor.getValue() * anchor.getKey().split("\\s+").length;
    	}
    	return length;
    }

    double calcHeadersLength(List<String> headers) {
    	double length = 0.0;
    	for (String header : headers) {
    		length += header.split("\\s+").length;
    	}
    	return length;
    }

    double calcTitleLength(String title) {
    	return (double )title.split("\\s+").length;
    }



    //sets up average lengths for bm25, also handles pagerank
    public void calcAverageLengths()
    {
    	weights = new HashMap<String, Double>();
    	bvals = new HashMap<String, Double>();
    	lengths = new HashMap<Document,Map<String,Double>>();
    	avgLengths = new HashMap<String,Double>();
    	pagerankScores = new HashMap<Document,Double>();

    	weights.put("url", urlweight);
    	weights.put("title", titleweight);
    	weights.put("body", bodyweight);
    	weights.put("header", headerweight);
    	weights.put("anchor", anchorweight);

    	bvals.put("url", burl);
    	bvals.put("title", btitle);
    	bvals.put("header", bheader);
    	bvals.put("body", bbody);
    	bvals.put("anchor", banchor);

    	
		for (Entry<Query, Map<String, Document>> queryDictEntry : this.queryDict.entrySet()) {
			for (Entry<String, Document> docEntry : queryDictEntry.getValue().entrySet()) {
				String url = docEntry.getKey();
				Document doc = docEntry.getValue();
				Map<String, Double> docLengths = new HashMap<String, Double>();
				docLengths.put("url", (double)url.length());
				if (doc.title != null) docLengths.put("title", calcTitleLength(doc.title));
				docLengths.put("body", (double)doc.body_length);
				if (doc.headers != null) docLengths.put("header", calcHeadersLength(doc.headers));
				if (doc.anchors != null) docLengths.put("anchor", calcAnchorsLength(doc.anchors));
				lengths.put(doc, docLengths);
				if (doc.page_rank > 0) {
					pagerankScores.put(doc, calcPageRankFactor(doc));
				} else {
					pagerankScores.put(doc, 0.0);
				}
			}
		}

		for (Map<String, Double> zoneLengths : lengths.values()) {
			for (Entry<String, Double> zoneLength : zoneLengths.entrySet()) {
				String zone = zoneLength.getKey();
				Double length = zoneLength.getValue();
				if (avgLengths.containsKey(zone)) {
					avgLengths.put(zone, length / lengths.size() + avgLengths.get(zone));
				} else {
					avgLengths.put(zone, length / lengths.size());
				}
			}
		}

		// normalize avgLengths
		for (String tfType : this.TFTYPES) {
			avgLengths.put(tfType, avgLengths.get(tfType) * weights.get(tfType));
		}

	}

	private double calcPageRankFactor(Document doc) {
		double factor = Math.log10(pageRankLambdaPrime + (double) doc.page_rank);
		// double factor = (double)doc.page_rank/(double)(pageRankLambdaPrime+
		// doc.page_rank);
		// double factor = (double)doc.page_rank/(double)(pageRankLambda +
		// Math.pow(Math.E, (-1)*doc.page_rank));
		return factor;
	}

	// //////////////////////////////////

	public double getNetScore(Map<String, Map<String, Double>> tfs, Query q,
			Map<String, Double> tfQuery, Document d) {
		double score = 0.0;

		for (String term : q.queryWords) {
			double tfi = 0.0;
			for (String tfType : this.TFTYPES) {
				if (!tfs.containsKey(tfType)
						|| !tfs.get(tfType).containsKey(term))
					continue;
				tfi += tfs.get(tfType).get(term);
			}
			double idf;
			if (this.idfs.containsKey(term)) {
				idf = this.idfs.get(term);
			} else {
				idf = this.idfs.get(LoadHandler.termNotPresent);
			}
			score += idf * (k1 + 1) * tfi / (k1 + tfi);
		}

		score += pageRankLambda * pagerankScores.get(d);
		return score;
	}

	private double zoneLength(Document d, String zone) {
		if (zone.equals("url"))
			return d.url.length();
		if (zone.equals("body"))
			return d.body_length;
		if (zone.equals("title")) {
			return (d.title != null) ? d.title.length() : 0;
		}
		if (zone.equals("header")) {
			return (d.headers != null) ? calcHeadersLength(d.headers) : 0;
		}
		if (zone.equals("anchor")) {
			return (d.anchors != null) ? calcAnchorsLength(d.anchors) : 0;
		}
		return 0;
	}

	// do bm25 normalization
	public void normalizeTFs(Map<String, Map<String, Double>> tfs, Document d,
			Query q) {
		for (String tfType : this.TFTYPES) {
			if (!tfs.containsKey(tfType))
				continue;
			Double Bz = (1 - bvals.get(tfType)) + bvals.get(tfType)
					* zoneLength(d, tfType) / avgLengths.get(tfType);
			for (Entry<String, Double> tf : tfs.get(tfType).entrySet()) {
				tfs.get(tfType).put(tf.getKey(),
						tf.getValue() * weights.get(tfType) / Bz);
			}
		}
	}

	@Override
	public double getSimScore(Document d, Query q) {
		Map<String, Map<String, Double>> tfs = this.getDocTermFreqs(d, q);
		this.normalizeTFs(tfs, d, q);
		Map<String, Double> tfQuery = getQueryFreqs(q);
		return getNetScore(tfs, q, tfQuery, d);
	}

}
