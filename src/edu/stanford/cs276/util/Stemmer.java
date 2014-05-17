package edu.stanford.cs276.util;

import edu.stanford.cs276.util.stemmer.EnglishSnowballStemmerFactory;

public class Stemmer {
	
	public static String getStem(String word){
		String stem_word = word;
		try {
			stem_word = EnglishSnowballStemmerFactory.getInstance().process(word);

		} catch (Exception e) {
			e.printStackTrace();
		}
		return stem_word;
	}
	
	public static String scrub(String input){
		return input.toLowerCase().replaceAll("[^0-9a-z]+", " ").replace("\\s+", " ").trim();
	}

}
