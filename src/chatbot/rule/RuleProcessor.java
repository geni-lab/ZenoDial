package chatbot.rule;

import chatbot.ChatBot;
import chatbot.log.Logger;
import chatbot.log.Logger.Level;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.Stack;
import java.util.Map.Entry;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class RuleProcessor {
	private static Logger log = new Logger("RuleProcessor", Level.DEBUG);
	private static final double MIN_UTIL = 0.001;
	private static final double NEXT_UTIL = 10.0;
	private static final double MUST_UTIL = 1000.0;
	private static final double PREVIOUS_UTIL = -1.0;
	private static final String USER_UTTERANCE = "UU";
	private static ArrayList<Rule> rules = new ArrayList<Rule>();
	private static HashMap<String, String> variables = new HashMap<String, String>();
	
	public static String getUserUtteranceNotation() {
		return USER_UTTERANCE;
	}
	
	public static double getVariableUtilValue() {
		return MUST_UTIL;
	}
	
	public static void readRules(String filePath) {
		try {
			// Get all the rule XML files
			ArrayList<File> listOfFiles = new ArrayList<File>(Arrays.asList(new File(filePath).listFiles()));
			
			// Loop through each of the rule XML files
			for (File file : listOfFiles) {
				// Skip directory
				if (file.isDirectory() || !file.getName().endsWith(".xml")) continue;
				
				log.debug("Processing " + file.getName());
				Document xmlDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(file.getPath());
				xmlDocument.getDocumentElement().normalize();
				NodeList ruleList = xmlDocument.getElementsByTagName("rule");
				
				// Loop through each of the <rule> in the rule XML file
				for (int i = 0; i < ruleList.getLength(); i++) {
					Node ruleNode = ruleList.item(i);
					log.debug("Currently reading rule: " + ruleNode.getAttributes().getNamedItem("id").getNodeValue());
					
					Rule rule;
					// If there is a cache attribute and the value is "no", create the rule accordingly
					if (ruleNode.getAttributes().getNamedItem("cache") != null && "no".equals(ruleNode.getAttributes().getNamedItem("cache").getNodeValue())) {
						rule = new Rule(ruleNode.getAttributes().getNamedItem("id").getNodeValue(), false);
					}
					// else, default value for the cache attribute is true
					else {
						rule = new Rule(ruleNode.getAttributes().getNamedItem("id").getNodeValue());
					}
					rules.add(rule);
					
					// Get all the <case> in the rule
					NodeList caseList = ruleNode.getChildNodes();
					
					// Loop through each of the <case> in the rule
					for (int j = 0; j < caseList.getLength(); j++) {
						Node caseNode = caseList.item(j);
						
						// Get <case> and skip the unwanted nodes
						if (!caseNode.getNodeName().equals("#text") && !caseNode.getNodeName().equals("#comment")) {
							log.debug("Reading case no. " + (j + 1) / 2 + " in rule: " + ruleNode.getAttributes().getNamedItem("id").getNodeValue());
							
							// Get all the <condition> & <effect> in the <case>
							NodeList conditionEffectList = caseNode.getChildNodes();
							
							// Loop through each of the <condition> & <effect> in the <case>
							for (int k = 0; k < conditionEffectList.getLength(); k++) {
								
								// Get either <condition> or <effect> and skip the unwanted nodes
								if (!conditionEffectList.item(k).getNodeName().equals("#text") && !conditionEffectList.item(k).getNodeName().equals("#comment")) {
									// For <condition>
									if (conditionEffectList.item(k).getNodeName().toLowerCase().equals("condition")) {
										Node conditionNode = conditionEffectList.item(k);
										readCondition(conditionNode, rule);
									}
									
									// For <effect>
									else if (conditionEffectList.item(k).getNodeName().toLowerCase().equals("effect")) {
										Node effectNode = conditionEffectList.item(k);
										readEffect(effectNode, rule);
									}
									
									else {
										log.severe("Only <condition> or <effect> node is accepted. No <" + conditionEffectList.item(k).getNodeName() + ">");
									}
								}
							}
						}
					}
				}
			}
		}
		
		catch (ParserConfigurationException | SAXException | IOException e) {
			log.severe("Cannot parse the rule XML file.");
			e.printStackTrace();
		}
	}
	
	private static void readCondition(Node conditionNode, Rule rule) {
		String condition = "";
		Stack<String> conditionOperators = new Stack<String>();
		
		if (conditionNode.hasAttributes()) {
			// If there exist an operator attribute in <condition>, push it to the stack
			if (conditionNode.getAttributes().getNamedItem("operator") != null) {
				conditionOperators.push(getOperator(conditionNode.getAttributes().getNamedItem("operator").getNodeValue()));
			}
		}
		
		// Get all the nodes under <condition>, i.e. <if> || <and> || <or> || <not>
		NodeList list = conditionNode.getChildNodes();
		
		// Loop through each of the nodes under <condition>
		for (int i = 0; i < list.getLength(); i++) {
			if (!list.item(i).getNodeName().equals("#text") && !list.item(i).getNodeName().equals("#comment")) {
				// Extract <if>, with <and>, <or>, and <not> handling
				// For <if> that's not the last one in the list, +2 is to get rid of the # nodes
				if (i + 2 < list.getLength()) {
					// For the 1st <if> under <not>
					if (conditionOperators.peek().equals("not") && !condition.startsWith("!")) {
						condition = "!" + condition + readSubConditions(list.item(i), conditionOperators, rule) + " || ";
					}
					
					// For the rest of the <if> under <not>
					if (conditionOperators.peek().equals("not")) {
						condition = condition + readSubConditions(list.item(i), conditionOperators, rule) + " || ";
					}
					
					// For operators other than <not>
					else {
						condition += readSubConditions(list.item(i), conditionOperators, rule) + conditionOperators.peek();
					}
				}
				
				// For the last <if>
				else {
					condition += readSubConditions(list.item(i), conditionOperators, rule);
				}
			}
		}

		if (conditionOperators.size() > 0) conditionOperators.clear();
		log.debug("*** IF (" + condition + ")");
		
		// Add <condition> to the rule object
		rule.setCondition(condition);
	}
	
	private static String readSubConditions(Node node, Stack<String> conditionOperators, Rule rule) {
		String subCondition = "";
		
		// If it's a <if>
		if (node.getNodeName().toLowerCase().equals("if")) {
			// Store the raw conditions for "variable extraction" later
			rule.addRawCondition(node.getTextContent());
			
			// For <if> containing a variable attribute
			if (node.getAttributes().getNamedItem("variable") != null && node.getAttributes().getNamedItem("relation") != null) {
				subCondition = normalizeCondition("{" + node.getAttributes().getNamedItem("variable").getNodeValue() + "}", node.getAttributes().getNamedItem("relation").getNodeValue(), node.getTextContent());
				
				// Add the variable and its value into the variables hash map
				variables.put(node.getAttributes().getNamedItem("variable").getNodeValue(), node.getTextContent());
			}
			
			// For <if> containing only relation attribute
			else if (node.getAttributes().getNamedItem("relation") != null) {
				subCondition = normalizeCondition("{" + USER_UTTERANCE + "}", node.getAttributes().getNamedItem("relation").getNodeValue(), node.getTextContent());
			}
		}
		
		// If it's not a <if>, push the operator to the operator stack and then repeat the process recursively until get a <if> hit
		else {
			conditionOperators.push(getOperator(node.getNodeName().toLowerCase()));
			subCondition += "(";
			
			for (int i = 0; i < node.getChildNodes().getLength(); i++) {
				if (!node.getChildNodes().item(i).getNodeName().equals("#text") && !node.getChildNodes().item(i).getNodeName().equals("#comment")) {
					// For <if> that's not the last one in the list, +2 is to get rid of the # nodes
					if (i + 2 < node.getChildNodes().getLength()) {
						// For the 1st <if> under <not>
						if (conditionOperators.peek().equals("not") && !subCondition.startsWith("!")) {
							subCondition = "!" + subCondition + readSubConditions(node.getChildNodes().item(i), conditionOperators, rule) + " || ";
						}
						
						// For the rest of the <if> under <not>
						else if (conditionOperators.peek().equals("not")) {
							subCondition = subCondition + readSubConditions(node.getChildNodes().item(i), conditionOperators, rule) + " || ";
						}
						
						// For operators other than <not>
						else {
							subCondition += readSubConditions(node.getChildNodes().item(i), conditionOperators, rule) + conditionOperators.peek();
						}
					}
					
					// For the last <if>
					else {
						subCondition += readSubConditions(node.getChildNodes().item(i), conditionOperators, rule) + ")";
						conditionOperators.pop();
					}
				}
			}
		}
		
		return subCondition;
	}
	
	private static void readEffect(Node effectNode, Rule rule) {
		String effect = "";
		NodeList thenList = effectNode.getChildNodes();
		
		// Loop through each of the <then>
		for (int i = 0; i < thenList.getLength(); i++) {
			Node thenNode = thenList.item(i);
			String util = "";
			String variableName = "";
			String topic = "general";
			
			if (!thenNode.getNodeName().equals("#text") && !thenNode.getNodeName().equals("#comment")) {
				if (thenNode.hasAttributes()) {
					// Get the util attribute
					if (thenNode.getAttributes().getNamedItem("util") != null) {
						util = thenNode.getAttributes().getNamedItem("util").getNodeValue();
						effect += "[" + util + "] ";
					}
					
					// Get the variable attribute
					else if (thenNode.getAttributes().getNamedItem("variable") != null) {
						variableName = thenNode.getAttributes().getNamedItem("variable").getNodeValue();
						util = "must";
						effect += "{" + variableName + "} = ";
					}
					
					else if (thenNode.getAttributes().getNamedItem("topic") != null) {
						util = "must";
						effect += "[" + util + "] ";
					}
					
					else log.severe("No util is found!");
					
					// Get the topic attribute
					if (thenNode.getAttributes().getNamedItem("topic") != null) {
						topic = thenNode.getAttributes().getNamedItem("topic").getNodeValue();
						effect += "<" + topic + "> ";
					}
				}
				
				// For printing out the effect only
				if (i + 2 < thenList.getLength()) effect += thenNode.getTextContent() + " ";
				else effect += thenNode.getTextContent();
				
				// Create an object for <then>
				Then then = new Then(rule.getRuleID() + "-" + (i + 1) / 2, util, topic, variableName, thenNode.getTextContent());
				
				// Check if there is a <then> following a "next"
				if ("next".equals(util) && (i + 2 == thenList.getLength())) log.severe("There has to be another <then> following a \"next\" <then>");
				
				// Add <then> to the rule object
				else rule.addThen(then, getUtilValue(then.getUtil()));
			}
		}
		
		log.debug("*** THEN (" + effect + ")");
	}
	
	private static String getOperator(String inputOpt) {
		String operator = "";
		
		if (inputOpt.trim().equals("and")) {
			operator = " && ";
		}
		
		else if (inputOpt.trim().equals("or")) {
			operator = " || ";
		}
		
		else if (inputOpt.trim().equals("not")) {
			operator = "not";
		}
		
		else if ("".equals(inputOpt.trim())) log.severe("relation attribute cannot be empty.");
		else log.severe("No such relation: " + inputOpt);
		
		return operator;
	}
	
	private static String normalizeCondition(String variable, String operator, String content) {
		String normalCondition = "";
		
		if (content.contains(" * ")) {
			normalCondition = variable + ".match(/\\b" + content.replace(" * ", ".*") + "\\b/) !== null";
		}
		
		else if ("*".equals(content)) {
			normalCondition = variable + " !== null";
		}
		
		else if ("=".equals(operator)) {
			normalCondition = variable + " === \"\\b" + content + "\\b\"";
		}
		
		else if ("!=".equals(operator)) {
			normalCondition = variable + " !== \"\\b" + content + "\\b\"";
		}
		
		else if ("in".equals(operator)) {
			normalCondition = variable + ".match(/\\b" + content + "\\b/) !== null";
		}
		
		else if ("!in".equals(operator)) {
			normalCondition = variable + ".match(/\\b" + content + "\\b/) === null";
		}
		
		else if ("sw".equals(operator)) {
			normalCondition = variable + ".indexOf(\"" + content + "\") == 0";
		}
		
		else if ("ew".equals(operator)) {
			// Assume every utterance ends with a punctuation
			normalCondition = "(" + variable + ".lastIndexOf(\"" + content + "\") >= 0 && " + variable + ".lastIndexOf(\"" + content + "\") == " + variable + ".length - \"" + content + "\".length - 1)";
		}
		
		else {
			normalCondition = variable + " " + operator + " \"" + content + "\"";
		}
		
		return normalCondition;
	}
	
	private static String getConditionVariable(Rule rule, String userUtterance) {
		String condition = rule.getCondition();
		
		// Replace user utterance variable to the actual value of user utterance
		if (condition.contains("{UU}")) {
			condition = condition.replace("{UU}", "\"" + userUtterance + "\"");
		}
		
		// Replace other variables in the condition to wildcard for later processing if it satisfies the condition
		if (condition.contains("{") && condition.contains("}")) {
			rule.hasVariableInCondition(true);
			condition = condition.replaceAll("\\s*\\{.+?\\}\\s*", ".*");
		}
		
		log.debug("Evaluating: " + condition);
		return condition;
	}
	
	public static ArrayList<Rule> findMatchingRules(String userUtterance) {
		ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("JavaScript");
		ArrayList<Rule> matchedRules = new ArrayList<Rule>();
		ArrayList<Rule> rulesToRemove = new ArrayList<Rule>();
		String matchedRuleIDs = "";
		
		try {
			// Get the rules that satisfy the user utterance
			for (Rule rule : rules) {
				if ((boolean) scriptEngine.eval(getConditionVariable(rule, userUtterance))) {
					matchedRules.add(rule);
					
					// If the matched rule has variables in the condition that needed to be stored in the system, do so
					if (rule.hasVariableInCondition()) {
						String condition = rule.getCondition().replace("{UU}", "\"" + userUtterance + "\"");
						
						// Check if there are any other variables that need to be stored in the system
						if (StringUtils.countMatches(condition, "{") > 0) {
							// Loop through each of the raw conditions to find the actual "matching" condition, and then extract the variable values from the user utterances
							for (String rawCondition : rule.getRawCondition()) {
								int uuRawConditionCount = 1;
								Matcher uuconditionMatcher = Pattern.compile(rawCondition.replaceAll("\\{.+?\\}", "(.+)").replaceAll("\\s\\*\\s", ".*")).matcher(userUtterance);
								// Extract the variable values here if it's a match
								if (uuconditionMatcher.find()) {
									Matcher variableNameMatcher = Pattern.compile("\\{(.*?)\\}").matcher(rawCondition);
									while (variableNameMatcher.find()) {
										updateSystemVariable(variableNameMatcher.group(1), uuconditionMatcher.group(uuRawConditionCount));
										uuRawConditionCount++;
									}
								}
							}
						}
					}
					
					if ("".equals(matchedRuleIDs)) matchedRuleIDs += rule.getRuleID();
					else matchedRuleIDs += "-" + rule.getRuleID();
					
					log.debug("YES YESSS! Rule (" + rule.getRuleID() + ")");
				}
				
				else {
					log.debug("NO NOOOOO! Rule (" + rule.getRuleID() + ")");
				}
			}
		}
		
		catch (ScriptException e) {
			e.printStackTrace();
			log.severe("Error occurred when selecting the satisfied rules.");
		}
		
		// ==================================== Filter the output utterances base on the criteria: #On topic > On topic > Off topic > General ==================================== //
		log.debug("Current topic = " + ChatBot.topic);
		if ("".equals(matchedRuleIDs)) log.warning("No rules matched!!");
		else log.debug("Matched Rule IDs = " + matchedRuleIDs);
		
		// If there are # utterances, remove all of the off-topic-# utterances, if any
		if (matchedRuleIDs.contains("#")) {
			for (Rule rule : matchedRules) {
				if (rule.getRuleID().contains("#") && !rule.getRuleID().contains(ChatBot.topic)) {
					log.debug("Have off-topic-# utterances, removing: " + rule.getRuleID());
					rulesToRemove.add(rule);
					matchedRuleIDs = matchedRuleIDs.replace("-" + rule.getRuleID(), "").replace(rule.getRuleID() + "-", "");
					log.debug("New Matched Rule IDs = " + matchedRuleIDs);
				}
			}
			
			matchedRules.removeAll(rulesToRemove);
			rulesToRemove.clear();
		}
		
		// If the current topic is "general"
		if ("general".equals(ChatBot.topic)) { 
			// If there are any non-general utterances, remove all the general utterances
			if (StringUtils.countMatches(matchedRuleIDs, "general") <= StringUtils.countMatches(matchedRuleIDs, "-")) {
				for (Rule rule : matchedRules) {
					if (rule.getRuleID().contains("general")) {
						log.debug("Have non-general utterances, removing: " + rule.getRuleID());
						rulesToRemove.add(rule);
						matchedRuleIDs = matchedRuleIDs.replace("-" + rule.getRuleID(), "").replace(rule.getRuleID() + "-", "");
						log.debug("New Matched Rule IDs = " + matchedRuleIDs);
					}
				}
			}
			
			matchedRules.removeAll(rulesToRemove);
			rulesToRemove.clear();
		}
		
		// If the current topic is not "general"
		else {
			// If there are on-topic utterances, remove all off-topic and general utterances
			if (matchedRuleIDs.contains(ChatBot.topic)) {
				for (Rule rule : matchedRules) {
					if (!rule.getRuleID().contains(ChatBot.topic)) {
						log.debug("Have on-topic utterances, removing: " + rule.getRuleID());
						rulesToRemove.add(rule);
						matchedRuleIDs = matchedRuleIDs.replace("-" + rule.getRuleID(), "").replace(rule.getRuleID() + "-", "");
						log.debug("New Matched Rule IDs = " + matchedRuleIDs);
					}
				}
			}
			
			// If there are no on-topic utterances and the set contains not only general utterances, keep all off-topic utterances and remove all general utterances
			else if (!matchedRuleIDs.contains(ChatBot.topic) && StringUtils.countMatches(matchedRuleIDs, "general") <= StringUtils.countMatches(matchedRuleIDs, "-")) {
				for (Rule rule : matchedRules) {
					if (rule.getRuleID().contains("general")) {
						log.debug("Have off-topic utterances, removing: " + rule.getRuleID());
						rulesToRemove.add(rule);
						matchedRuleIDs = matchedRuleIDs.replace("-" + rule.getRuleID(), "").replace(rule.getRuleID() + "-", "");
						log.debug("New Matched Rule IDs = " + matchedRuleIDs);
					}
				}
			}
			
			// If there are no on-topic and off-topic utterances, keep only general utterances
			else {
				for (Rule rule : matchedRules) {
					if (!rule.getRuleID().contains("general")) {
						log.debug("Have only general utterances, removing: " + rule.getRuleID());
						rulesToRemove.add(rule);
						matchedRuleIDs = matchedRuleIDs.replace("-" + rule.getRuleID(), "").replace(rule.getRuleID() + "-", "");
						log.debug("New Matched Rule IDs = " + matchedRuleIDs);
					}
				}
			}
			
			matchedRules.removeAll(rulesToRemove);
			rulesToRemove.clear();
		}
		
		// If there are general utterances other than "generalnothingtosay" and "generalquestions", select them
		if (!"".equals(matchedRuleIDs.replace("generalquestions", "").replace("generalnothingtosay", "").replaceAll("-", ""))) {
			for (Rule rule : matchedRules) {
				if ("generalnothingtosay".equals(rule.getRuleID()) || "generalquestions".equals(rule.getRuleID())) {
					log.debug("Have other general utterances, removing: " + rule.getRuleID());
					rulesToRemove.add(rule);
					matchedRuleIDs = matchedRuleIDs.replace("-" + rule.getRuleID(), "").replace(rule.getRuleID() + "-", "");
					log.debug("New Matched Rule IDs = " + matchedRuleIDs);
				}
			}
			
			matchedRules.removeAll(rulesToRemove);
			rulesToRemove.clear();
		}
		
		return matchedRules;
	}
	
	public static double getUtilValue(String util) {
		Double utilValue = 0.0;
		
		try {
			if ("random".equals(util) || "next".equals(util)) {
				utilValue = new Random().nextDouble() + MIN_UTIL;
			}
			
			else if ("previous".equals(util)) {
				utilValue = PREVIOUS_UTIL;
			}
			
			else if ("followingNext".equals(util)) {
				utilValue = NEXT_UTIL;
			}
			
			else if ("must".equals(util)) {
				utilValue = MUST_UTIL;
			}
			
			else {
				utilValue = Double.parseDouble(util);
			}
		}
		
		catch (NumberFormatException e) {
			e.printStackTrace();
			log.severe("Cannot get the util value of \"" + util + "\"");
		}
		
		return utilValue;
	}
	
	public static List<Entry<Then, Double>> extractEffect(ArrayList<Rule> satisfiedRules) {
		List<Entry<Then, Double>> allEffects = new ArrayList<Entry<Then, Double>>();
		
		// Put all the effects from all satisfied rules
		for (Rule rule : satisfiedRules) {
			allEffects.addAll(new ArrayList<Entry<Then, Double>>(rule.getEffect().entrySet()));
		}
		
		return allEffects;
	}
	
	public static Rule getRule(String ruleID) {
		for (Rule rule : rules) {
			if (ruleID.equals(rule.getRuleID())) {
				return rule;
			}
		}
		
		log.warning("No such rule: " + ruleID);
		return null;
	}
	
	public static void updateSystemVariable(String variableName, String values) {
		log.debug("Updating variable: \"" + variableName + "\" to \"" + values + "\"");
		variables.put(variableName, values);
	}
	
	public static String getSystemVariable(String variableName) {
		if (variables.containsKey(variableName)) {
			return variables.get(variableName);
		}
		
		else {
			log.warning("No such variable in the system: " + variableName + "\n");
			return "";
		}
	}
	
	public static void contructCache(ArrayList<Rule> satisfiedRules, String outputUtterance, double utilValue) {
		// Update the util value for each of the satisfied rules
		for (Rule rule : satisfiedRules) {
			// Skip those no-need-to-cache rules
			if (!rule.needToCache()) continue;
			
			log.debug("Constructing cache for rule: " + rule.getRuleID());
			Iterator<Entry<Then, Double>> iterator = rule.getEffect().entrySet().iterator();
			int nextSeqID = 0; // To store the sequence ID of that "next" utterance
			while (iterator.hasNext()) {
				Entry<Then, Double> entry = iterator.next();
				
				// For the output utterance
				if (outputUtterance.equals(entry.getKey().getThen()) && entry.getValue() == utilValue) {
					// Reset the util value for the output utterance
					rule.getEffect().put(entry.getKey(), RuleProcessor.getUtilValue(entry.getKey().getUtil()));
					
					// If the output utterance has a "next" util, adjust the util value of its following one with "previous" util
					if ("next".equals(entry.getKey().getUtil())) {
						nextSeqID = Integer.parseInt(entry.getKey().getRuleSeqID().split("-")[1]);
					}
				}
				
				// Increase the util value by 1 for the unspoken utterances
				else if (entry.getValue() > 0) {
					rule.getEffect().put(entry.getKey(), entry.getValue() + 1);
				}
			}
			
			// If the output utterance is a "next" utterance, update the util value of its "previous" utterance accordingly
			if (nextSeqID > 0) {
				iterator = rule.getEffect().entrySet().iterator();
				
				while (iterator.hasNext()) {
					Entry<Then, Double> entry = iterator.next();
					
					if (Integer.parseInt(entry.getKey().getRuleSeqID().split("-")[1]) == nextSeqID + 1) {
						rule.getEffect().put(entry.getKey(), RuleProcessor.getUtilValue("followingNext"));
						log.debug("\"Next\" utterance of the same topic will be: \"" + entry.getKey().getThen() + "\"");
					}
				}
			}
		}
	}
}