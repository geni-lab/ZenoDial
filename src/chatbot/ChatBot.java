package chatbot;

import java.util.Scanner;
import chatbot.log.Logger;
import chatbot.log.Logger.Level;
import chatbot.plugin.JMegaHAL;
import chatbot.process.InputProcessor;
import chatbot.process.PauseUtterance;
import chatbot.process.PrefixOfUtterance;
import chatbot.process.RemoveUnwantedCharacters;
import chatbot.process.RepeatUtterance;
import chatbot.ros.ROSControl;
import chatbot.ros.ROSListener;
import chatbot.ros.ROSPublisher;
import chatbot.rule.RuleProcessor;

public class ChatBot {
	private static Logger log = new Logger("ChatBot", Level.NORMAL);
	private static final String robotname = "zeno";
	
	public static String topic = "general";
	public static String lastOutputUtterance = "";
	public static boolean isSpeaking = false;
	public static boolean shutUp = false;
	public static boolean sendNextSentence = false;
	public static boolean nothingSpokenYet = false;
	public static boolean speakingTheLastSentence = false;
	public static boolean needJMegaHAL = false;
	public static boolean needWolframAlpha = false;
	
	public static void main(String... args) {
		// Read all the rule XML files
		RuleProcessor.readRules("./res");
		
		// Read pause.txt and prefix.txt file
		PauseUtterance.readPauseUtterances("./res/pause.txt");
		PrefixOfUtterance.readPrefixUtterances("./res/prefix.txt");
		
		// Create ROS listeners & publishers
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		ROSListener.createInputListener();
		ROSListener.createNextSentenceListener();
		ROSListener.createRobotNameListener();
		ROSControl.initializePublisher("ChatBotTalker", "zenodial_talk");
		
		// Initialize JMegaHAL
		JMegaHAL.initializeFromCorpus("./res/jmegahal");
		
		// Add predefined variables to the system
		RuleProcessor.updateSystemVariable("robotname", robotname);
		
		// Suggest JVM to do a garbage collection to clear all the XML node objects etc. 
		System.gc();
		
		Scanner scanner = new Scanner(System.in);
		try {
			// For command line interface
			log.info("Please speak or enter anything here.");
			System.out.print("> ");
			while (true) {
				String userInput = RemoveUnwantedCharacters.getString(scanner.nextLine(), true);
				
				if (!"".equals(userInput.trim())) {
					// If it is speaking, send out the "shut up" signal
					if (isSpeaking) {
						if (!speakingTheLastSentence) shutUp = true;
						continue;
					}
					
					// Directly publish an utterance if it starts with an "~"
					if (userInput.startsWith("~")) {
						ROSPublisher.publish("zenodial_talk", userInput.replace("~", "").trim());
						continue;
					}
					
					// Update the flags
					speakingTheLastSentence = false;
					nothingSpokenYet = true;
					
					// Store the rephrased user utterance for rules that need it
					RuleProcessor.updateSystemVariable("UU", RepeatUtterance.getUtterance(userInput));
					
					// Generate the reply
					lastOutputUtterance = new InputProcessor().getReply(userInput);

					// Suggest JVM to do a garbage collection after all
					System.gc();
					
					System.out.print("> ");
				}
			}
		}
		
		catch (Exception e) {
			e.printStackTrace();
		}
		
		finally {
			scanner.close();
		}
	}
}