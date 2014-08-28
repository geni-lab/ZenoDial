package chatbot.rule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepeatUtterance {
	public static String getUtterance(String inputUtterance) {
		Matcher matcher = Pattern.compile("(?i)\\byou\\b").matcher(inputUtterance);
		Matcher matcher2 = Pattern.compile("(?i)\\byou're\\b").matcher(inputUtterance);
		
		if (matcher.find() || matcher2.find()) {
			return inputUtterance.toLowerCase()
					.replaceAll("\\baren't you\\b", "i am not")
					.replaceAll("\\byou aren't\\b", "i am not")
					.replaceAll("\\bare you\\b", "am i")
					.replaceAll("\\byou are\\b", "i am")
					.replaceAll("\\byou\\b", "i")
					.replaceAll("\\bare\\b", "am");
		}
		
		else {
			return inputUtterance.toLowerCase()
					.replaceAll("\\bam i\\b", "are you")
					.replaceAll("\\bi am\\b", "you are")
					.replaceAll("\\bi\\b", "you")
					.replaceAll("\\bam\\b", "are");
		}
	}
}