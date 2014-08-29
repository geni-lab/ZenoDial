package chatbot.rule;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RepeatUtterance {
	public static String getUtterance(String inputUtterance) {
		Matcher matcher = Pattern.compile("(?i)\\byou\\b").matcher(inputUtterance);
		Matcher matcher2 = Pattern.compile("(?i)\\byour\\b").matcher(inputUtterance);
		Matcher matcher3 = Pattern.compile("(?i)\\byou're\\b").matcher(inputUtterance);
		Matcher matcher4 = Pattern.compile("(?i)\\byours\\b").matcher(inputUtterance);
		
		if (matcher.find() || matcher2.find() || matcher3.find() || matcher4.find()) {
			return inputUtterance.toLowerCase()
					.replaceAll("\\baren't you\\b", "i am not")
					.replaceAll("\\byou aren't\\b", "i am not")
					.replaceAll("\\bare you\\b", "am i")
					.replaceAll("\\byou are\\b", "i am")
					.replaceAll("\\byou\\b", "i")
					.replaceAll("\\byour\\b", "my")
					.replaceAll("\\byours\\b", "mine")
					.replaceAll("\\bare\\b", "am")
					.replaceAll("\\bme\\b", "you");
		}
		
		else {
			return inputUtterance.toLowerCase()
					.replaceAll("\\bam i\\b", "are you")
					.replaceAll("\\bi am\\b", "you are")
					.replaceAll("\\bi'm\\b", "you are")
					.replaceAll("\\bi\\b", "you")
					.replaceAll("\\bme\\b", "you")
					.replaceAll("\\bmy\\b", "your")
					.replaceAll("\\bmine\\b", "yours")
					.replaceAll("\\bam\\b", "are");
		}
	}
}