package chatbot.process;

public class RemoveUnwantedCharacters {
	public static String getString(String inputString) {
		return inputString.replace("’", "'").replace("”", "\"").replace("‘", "'").replace("“", "\"").replace("~~", "Around");
	}
}