package edu.stanford.cs276;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class CosineSimilarityScorer extends AScorer
{
	public CosineSimilarityScorer(Map<String,Double> idfs)
	{
		super(idfs);
	}
	
	///////////////weights///////////////////////////
    double urlweight = -1;
    double titleweight  = -1;
    double bodyweight = -1;
    double headerweight = -1;
    double anchorweight = -1;
    
    double smoothingBodyLength = -1;
    //////////////////////////////////////////
	
	public double getNetScore(Map<String,Map<String, Double>> tfs, Query q, Map<String,Double> tfQuery,Document d)
	{
		double score = 0.0;
		
		/*
		 * @//TODO : Your code here
		 */
		
		return score;
	}

	
	public void normalizeTFs(Map<String,Map<String, Double>> tfs,Document d, Query q)
	{
		/*
		 * @//TODO : Your code here
		 */
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
