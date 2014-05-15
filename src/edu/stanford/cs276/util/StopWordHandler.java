package edu.stanford.cs276.util;

import java.util.List;
import java.util.Map;

public class StopWordHandler {
	
	static String[] stopWords = { "a", "an", "and", "are", "as", "at", "be", "but",
			"by", "for", "if", "in", "into", "is", "it", "no", "not", "of",
			"on", "or", "such", "that", "the", "their", "then", "there",
			"these", "they", "this", "to", "was", "will", "with" };
	
	public static String scrubStopWords(String str){
		String resultStr = str;
		for (String stopWord : stopWords) {
			resultStr = (" "+ str + " ").replaceAll(" "+stopWord+" ", " ").replace("\\s+", " ").trim();
			str = resultStr;
		}
		return str.trim();
	}

	public static boolean isStopWord(String str){
		boolean isStopWord = false;
		for (String stop_word : stopWords) {
			if(str.equals(stop_word)){
				isStopWord = true;
				break;
			}
		}
		return isStopWord;
	}
	
	public static List<String> removeStopWords(List<String> queryWords){
		List<String> qWords = queryWords;
		for (String stop_word : stopWords) {
			if(qWords.contains(stop_word)){
				qWords.remove(stop_word);
			}
		}
		return qWords;
	}

}
