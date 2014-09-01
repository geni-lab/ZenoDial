package chatbot.process;

public class RemoveUnwantedCharacters {
	public static String getString(String inputString, boolean needToRemovePunctuation) {
		String rtnString = inputString.replace("’", "'").replace("”", "\"").replace("‘", "'").replace("“", "\"").replace("~~", "Around");
		
		if (needToRemovePunctuation) return removePunctuation(rtnString);
		else return rtnString;
	}
	
	public static String removePunctuation(String inputString) {
		// If the inputString contains a punctuation at the end of the sentence, remove it
		if (",.?!~".contains(inputString.substring(inputString.length() - 1))) {
			return inputString.substring(0, inputString.length() - 1);
		}
		
		return inputString;
	}
}