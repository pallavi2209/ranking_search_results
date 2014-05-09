package edu.stanford.cs276;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class AScorer 
{
	
	Map<String,Double> idfs;
	String[] TFTYPES = {"url","title","body","header","anchor"};
	
	public AScorer(Map<String,Double> idfs)
	{
		this.idfs = idfs;
	}
	
	//scores each document for each query
	public abstract double getSimScore(Document d, Query q);
	
	//handle the query vector
	public Map<String,Double> getQueryFreqs(Query q)
	{
		Map<String,Double> tfQuery = new HashMap<String,Double>();
		
		/*
		 * @//TODO : Your code here
		 */
		
		return tfQuery;
	}
	

	
	////////////////////Initialization/Parsing Methods/////////////////////
	
	/*
	 * @//TODO : Your code here
	 */
	
	
    ////////////////////////////////////////////////////////
	
	
	/*/
	 * Creates the various kinds of term frequences (url, title, body, header, and anchor)
	 * You can override this if you'd like, but it's likely that your concrete classes will share this implementation
	 */
	public Map<String,Map<String, Double>> getDocTermFreqs(Document d, Query q)
	{
		//map from tf type -> queryWord -> score
		Map<String,Map<String, Double>> tfs = new HashMap<String,Map<String, Double>>();
		
		////////////////////Initialization/////////////////////
		
		/*
		 * @//TODO : Your code here
		 */
		
	    ////////////////////////////////////////////////////////
		
		//////////handle counts//////
		
		//loop through query terms increasing relevant tfs
		for (String queryWord : q.queryWords)
		{
			/*
			 * @//TODO : Your code here
			 */
			
		}
		return tfs;
	}
	

}
