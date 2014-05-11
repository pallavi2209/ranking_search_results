package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;


public class BM25Scorer extends AScorer
{
	Map<Query,Map<String, Document>> queryDict;
	
	public BM25Scorer(Map<String,Double> idfs,Map<Query,Map<String, Document>> queryDict)
	{
		super(idfs);
		this.queryDict = queryDict;
		this.calcAverageLengths();
	}

	
	///////////////weights///////////////////////////
    double urlweight = 1d;
    double titleweight  = 2d;
    double bodyweight = 0.7d;
    double headerweight = 1.5d;
    double anchorweight = 3d;

    ///////bm25 specific weights///////////////
    double burl=0.75;
    double btitle=0.75;
    double bheader=0.75;
    double bbody=0.75;
    double banchor=0.75;

    double k1=1.2;
    double pageRankLambda=1;
    double pageRankLambdaPrime=-1;
    //////////////////////////////////////////
    
    ////////////bm25 data structures--feel free to modify ////////
    
    Map<Document,Map<String,Double>> lengths;
    Map<String,Double> avgLengths;
    Map<Document,Double> pagerankScores;
    Map<String, Double> weights;
    Map<String, Double> bvals;
    
    //////////////////////////////////////////
    
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
				if (doc.title != null) docLengths.put("title", (double)doc.title.length());
				docLengths.put("body", (double)doc.body_length);
				if (doc.headers != null) docLengths.put("header", (double)doc.headers.toString().length());
				if (doc.anchors != null) docLengths.put("anchor", (double)doc.anchors.toString().length());
				lengths.put(doc, docLengths);
				pagerankScores.put(doc, Math.log(doc.page_rank));
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
    	
    	//normalize avgLengths
		for (String tfType : this.TFTYPES)
		{
			avgLengths.put(tfType, avgLengths.get(tfType) * weights.get(tfType));
		}

    }
    
    ////////////////////////////////////
    
    
	public double getNetScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery,Document d)
	{
		double score = 0.0;
		
		for (String term : q.queryWords) {
			double tfi = 0.0;
			System.out.println(term);
			for (String tfType : this.TFTYPES) {
				if (!tfs.containsKey(tfType) || !tfs.get(tfType).containsKey(term)) continue;
				tfi += tfs.get(tfType).get(term);
			}
			double idf;
			if (this.idfs.containsKey(term)) {
				idf = this.idfs.get(term);
			} else {
				idf = this.idfs.get(LoadHandler.termNotPresent);
			}
			score += idf * (k1 + 1) * tfi / (k1 + tfi) + pageRankLambda * pagerankScores.get(d);
		}
		
		return score;
	}

	private double zoneLength(Document d, String zone) {
		if (zone.equals("url")) return d.url.length();
		if (zone.equals("body")) return d.body_length;
		if (zone.equals("title")) {
			return (d.title != null) ? d.title.length() : 0;
		}
		if (zone.equals("header")) {
			return (d.headers != null) ? d.headers.toString().length() : 0;
		}
		if (zone.equals("anchor")) {
			return (d.anchors != null) ? d.anchors.toString().length() : 0;
		}
		return 0;
	}

	//do bm25 normalization
	public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q)
	{
		for (String tfType : this.TFTYPES) {
			if (!tfs.containsKey(tfType)) continue;
			Double Bz = (1 - bvals.get(tfType)) + bvals.get(tfType) * zoneLength(d, tfType) / avgLengths.get(tfType);
			System.out.println(tfType);
			for (Entry<String, Double> tf : tfs.get(tfType).entrySet()) {
				tfs.get(tfType).put(tf.getKey(), tf.getValue() * weights.get(tfType) / Bz);
			}
		}
	}

	
	@Override
	public double getSimScore(Document d, Query q) 
	{
		
		Map<String,Map<String, Double>> tfs = this.getDocTermFreqs(d,q);
		
		this.normalizeTFs(tfs, d, q);
		
		Map<String,Double> tfQuery = getQueryFreqs(q);
		
		
        return getNetScore(tfs,q,tfQuery,d);
	}

	
	
	
}
