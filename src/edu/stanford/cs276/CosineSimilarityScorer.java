package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

public class CosineSimilarityScorer extends AScorer
{
	public CosineSimilarityScorer(Map<String,Double> idfs)
	{
		super(idfs);
	}
	
	public static final String URL = "url";
	public static final String TITLE = "title";
	public static final String BODY = "body";
	public static final String HEADER = "header";
	public static final String ANCHOR = "anchor";
	
	///////////////weights///////////////////////////
	
	double urlweight = 8.0;
	double titleweight = 6.0;
	double bodyweight = 1.0;
	double headerweight = 7.5;
	double anchorweight = 1.2;

    
    double smoothingBodyLength = 500;
    //////////////////////////////////////////
	
	public double getNetScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery,Document d)
	{
		double score = 0.0;

		for (String query_word : q.queryWords) {
			double tfQueryWord;
			double tfDocUrl = 0.0d, tfDocTitle = 0.0d, tfDocBody= 0.0d, tfDocHeader= 0.0d, tfDocAnchor= 0.0d; 
			tfQueryWord = tfQuery.get(query_word);
			if(tfs.containsKey(URL)){
				if(tfs.get(URL).containsKey(query_word)){
					tfDocUrl = tfs.get(URL).get(query_word);
				}
			}
			if(tfs.containsKey(TITLE)){
				if(tfs.get(TITLE).containsKey(query_word)){
					tfDocTitle = tfs.get(TITLE).get(query_word);
				}
			}
			if(tfs.containsKey(BODY)){
				if(tfs.get(BODY).containsKey(query_word)){
					tfDocBody = tfs.get(BODY).get(query_word);
				}
			}
			if(tfs.containsKey(HEADER)){
				if(tfs.get(HEADER).containsKey(query_word)){
					tfDocHeader = tfs.get(HEADER).get(query_word);
				}
			}
			if(tfs.containsKey(ANCHOR)){
				if(tfs.get(ANCHOR).containsKey(query_word)){
					tfDocAnchor = tfs.get(ANCHOR).get(query_word);
				}
			}
			
			Double termScore = tfQueryWord*((urlweight*tfDocUrl) + (titleweight*tfDocTitle) + (bodyweight*tfDocBody) + (headerweight*tfDocHeader) + (anchorweight*tfDocAnchor));
			score+= termScore;
		}
		
		return score;
	}

	
	private  static DecimalFormat df = new DecimalFormat("#.#####");
	
	public void printAllScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery,Document d)
	{

		double urls= 0.0, titles=0.0, bodys=0.0, headers=0.0, anchors=0.0;
		for (String query_word : q.queryWords) {
			double tfQueryWord;
			double tfDocUrl = 0.0d, tfDocTitle = 0.0d, tfDocBody= 0.0d, tfDocHeader= 0.0d, tfDocAnchor= 0.0d; 
			tfQueryWord = tfQuery.get(query_word);
			if(tfs.containsKey(URL)){
				if(tfs.get(URL).containsKey(query_word)){
					tfDocUrl = tfs.get(URL).get(query_word);
				}
			}
			if(tfs.containsKey(TITLE)){
				if(tfs.get(TITLE).containsKey(query_word)){
					tfDocTitle = tfs.get(TITLE).get(query_word);
				}
			}
			if(tfs.containsKey(BODY)){
				if(tfs.get(BODY).containsKey(query_word)){
					tfDocBody = tfs.get(BODY).get(query_word);
				}
			}
			if(tfs.containsKey(HEADER)){
				if(tfs.get(HEADER).containsKey(query_word)){
					tfDocHeader = tfs.get(HEADER).get(query_word);
				}
			}
			if(tfs.containsKey(ANCHOR)){
				if(tfs.get(ANCHOR).containsKey(query_word)){
					tfDocAnchor = tfs.get(ANCHOR).get(query_word);
				}
			}
			
			//Double termScore = tfQueryWord*((urlweight*tfDocUrl) + (titleweight*tfDocTitle) + (bodyweight*tfDocBody) + (headerweight*tfDocHeader) + (anchorweight*tfDocAnchor));
			urls += tfQueryWord * tfDocUrl;
			titles += tfQueryWord * tfDocTitle;
			bodys += tfQueryWord*tfDocBody;
			headers += tfQueryWord* tfDocHeader;
			anchors += tfQueryWord* tfDocAnchor;
		}
	}
	
	public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q)
	{
		Double doc_length = (double)(d.body_length + smoothingBodyLength);
		for (Entry<String,Map<String, Double>> tfTypeEntry : tfs.entrySet()) {
			String tf_type = tfTypeEntry.getKey();
			for (Entry<String, Double> tfEntry : tfTypeEntry.getValue().entrySet()) {
				String query_word = tfEntry.getKey();
				Double value = tfEntry.getValue();
				Double norm_value = value/doc_length;
				tfs.get(tf_type).put(query_word, norm_value);
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
