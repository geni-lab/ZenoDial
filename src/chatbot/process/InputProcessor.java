package chatbot.process;

import java.text.DecimalFormat;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import chatbot.ChatBot;
import chatbot.log.Logger;
import chatbot.log.Logger.Level;
import chatbot.plugin.JMegaHAL;
import chatbot.plugin.WolframAlpha;
import chatbot.ros.ROSPublisher;
import chatbot.rule.PrefixOfUtterance;
import chatbot.rule.Rule;
import chatbot.rule.RuleProcessor;
import chatbot.rule.Then;

public class InputProcessor {
	private static Logger log = new Logger("InputProcessor", Level.NORMAL);
	private boolean needToUpdateTopic = true;
	
	public String getReply(String userUtterance) {
		// Remove unwanted characters from the input, if any
		userUtterance = userUtterance.replaceAll("  ", " ").toLowerCase();
		
		// If after certain time the robot is still searching for the answer... say something
		ROSPublisher.startPausePublisher(userUtterance);
		
		// Find all the rules that are satisfied by the user utterance
		ArrayList<Rule> satisfiedRules = RuleProcessor.findMatchingRules(userUtterance);
		
		// Extract all the effects from the selected rules
		List<Entry<Then, Double>> potentialOutputUtterances = RuleProcessor.extractEffect(satisfiedRules);
		
		// If there are system variables need to be updated, update and then remove them from the set
		updateVariables(potentialOutputUtterances);
		
		// Get the JMegaHAL reply
		if (ChatBot.needJMegaHAL) {
			potentialOutputUtterances.add(new AbstractMap.SimpleEntry<Then, Double>(new Then("JMegaHAL", "random", JMegaHAL.getReply(userUtterance)), RuleProcessor.getUtilValue("random")));
			ChatBot.needJMegaHAL = false;
		}
		
		// Get Wolfram|Alpha reply
		if (ChatBot.needWolframAlpha) {
			String waReply = WolframAlpha.getReply(userUtterance).trim();
			if (!"".equals(waReply)) {
				waReply = PrefixOfUtterance.getPrefixUtterances().get(new Random().nextInt(PrefixOfUtterance.getPrefixUtterances().size())) + waReply;
				potentialOutputUtterances.add(new AbstractMap.SimpleEntry<Then, Double>(new Then("WolframAlpha", "random", waReply), RuleProcessor.getUtilValue("must")));
			}
			ChatBot.needWolframAlpha = false;
		}
		
		// Get OpenEphyra reply
//		if (ChatBot.needOpenEphyra) potentialOutputUtterances.add(new AbstractMap.SimpleEntry<Then, Double>(new Then("OpenEphyra", "random", OpenEphyraSearch.getReply(userUtterance)), RuleProcessor.getUtilValue("random")));
		
		// Sort the output utterances in descending order
		sortByUtils(potentialOutputUtterances);
		
		// Choose the most suitable utterance from the effects
		String outputUtterance = getFinalOutputUtterance(potentialOutputUtterances, satisfiedRules);
		
		// Print out the effects
		for (Entry<Then, Double> entry : potentialOutputUtterances) {
			log.info("[" + new DecimalFormat("#0.###").format(entry.getValue()) + "] " + entry.getKey().getThen());
		}
		
		ChatBot.nothingSpokenYet = false;
		System.out.println("--> " + outputUtterance);
		ROSPublisher.publish("itf_talk", outputUtterance);
		
		// Update the topic to the one specified in the output <then>
		if (needToUpdateTopic) updateTopic(potentialOutputUtterances);
		
		// Cache the output utterances for the next time
		RuleProcessor.contructCache(satisfiedRules, getRawOutputUtterance(potentialOutputUtterances), getOutputUtteranceUtilValue(potentialOutputUtterances));
		
		return outputUtterance;
	}
	
	private <K, V extends Comparable<? super V>> List<Entry<K, V>> updateVariables(List<Entry<K, V>> utterances) {
		List<Entry<K, V>> entryToBeRemoved = new ArrayList<Entry<K, V>>();
		
		// Loop through the utterance list, identify any the variables that needed to be updated 
		for (Entry<K, V> entry : utterances) {
			if (!"".equals(((Then) entry.getKey()).getVariable())) {
				RuleProcessor.updateSystemVariable(((Then) entry.getKey()).getVariable(), ((Then) entry.getKey()).getThen());
				entryToBeRemoved.add(entry);
			}
		}
		
		// Remove the update-variable utterances from the utterance list to make sure they won't be picked as the final output utterance
		for (Entry<K, V> entry : entryToBeRemoved) {
			utterances.remove(entry);
		}
		
		return utterances;
	}
	
	private String getFinalOutputUtterance(List<Entry<Then, Double>> utterances, ArrayList<Rule> satisfiedRules) {
		// Return the one with the highest util value, with the unwanted punctuation removed
		String output = RemoveUnwantedCharacters.getString(utterances.get(0).getKey().getThen());
		
		// For repeating the last utterance
		if ("@repeat".equals(output)) {
			needToUpdateTopic = false;
			return ChatBot.lastOutputUtterance;
		}
		
		// For redirecting to a particular topic's effect
		else if ("".equals(output) && !"".equals(utterances.get(0).getKey().getTopic())) {
			// Change the satisfiedRules & utterances so as to make sure this rule will be cached
			satisfiedRules.clear();
			satisfiedRules.add(RuleProcessor.getRule(utterances.get(0).getKey().getTopic()));
			utterances.clear();
			utterances.addAll(RuleProcessor.extractEffect(satisfiedRules));
			
			List<Entry<Then, Double>> newOutputUtterances = sortByUtils(updateVariables(utterances));
			output = RemoveUnwantedCharacters.getString(newOutputUtterances.get(0).getKey().getThen());
		}
		
		try {
			// Replace variables, if any, with their actual values
			if (output.contains("{") && output.contains("}")) {
				int numOfVariables = StringUtils.countMatches(output, "{");
				
				// Loop through the variables one by one and replace them with the appropriate values
				for (int i = 0; i < numOfVariables; i++) {
					Matcher matcher = Pattern.compile("\\{(.*?)\\}").matcher(output);
					if (matcher.find()) output = output.replace("{" + matcher.group(1) + "}", RuleProcessor.getSystemVariable(matcher.group(1)));
					else log.warning("Unknown error! No variable found?");
				}
			}
		}
		
		catch (Exception e) {
			log.warning("Error occurred when retrieving or replacing variables from the output utterance.");
			e.printStackTrace();
		}
		
		ChatBot.lastOutputUtterance = output;
		return output;
	}
	
	private String getRawOutputUtterance(List<Entry<Then, Double>> utterances) {
		// Return the one with the highest util value, unprocessed
		return utterances.get(0).getKey().getThen();
	}
	
	private Double getOutputUtteranceUtilValue(List<Entry<Then, Double>> utterances) {
		// Return the util value of the output utterance
		return utterances.get(0).getValue();
	}
	
	private void updateTopic(List<Entry<Then, Double>> utterances) {
		ChatBot.topic = utterances.get(0).getKey().getTopic();
		log.debug("Topic = " + ChatBot.topic);
	}
	
	private static <K, V extends Comparable<? super V>> List<Entry<K, V>> sortByUtils(List<Entry<K, V>> sortedEntries) {
		Collections.sort(sortedEntries, new Comparator<Entry<K, V>>() {
			@Override
			public int compare(Entry<K, V> e1, Entry<K, V> e2) {
				return e2.getValue().compareTo(e1.getValue());
			}
		});
		
		return sortedEntries;
	}
}