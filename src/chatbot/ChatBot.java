package chatbot;

import java.util.Scanner;
import chatbot.log.Logger;
import chatbot.log.Logger.Level;
import chatbot.plugin.JMegaHAL;
import chatbot.process.InputProcessor;
import chatbot.ros.ROSControl;
import chatbot.ros.ROSListener;
import chatbot.ros.ROSPublisher;
import chatbot.rule.PauseUtterance;
import chatbot.rule.RuleProcessor;

public class ChatBot {
	private static Logger log = new Logger("ChatBot", Level.NORMAL);
	private static final String QUIT = "#quit";
	private static final String robotname = "zeno";
	
	public static String topic = "general";
	public static String lastOutputUtterance = "";
	public static boolean isSpeaking = false;
	public static boolean shutUp = false;
	public static boolean sendNextSentence = false;
	public static boolean nothingSpokenYet = false;
	public static boolean speakingTheLastSentence = false;
	public static boolean needJMegaHAL = true;
	public static boolean needWolframAlpha = false;
	
	public static void main(String... args) {
		// Read all the rule XML files
		RuleProcessor.readRules("./res");
		
		// Read pause.txt file
		PauseUtterance.readPauseUtterances("./res/pause.txt");
		
		// Create ROS listeners & publishers
		System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
		ROSListener.createInputListener();
		ROSListener.createNextSentenceListener();
		ROSControl.initializePublisher("ChatBotTalker", "itf_talk");
		
		// Initialize JMegaHAL
		if (needJMegaHAL) JMegaHAL.initializeFromCorpus("./res/jmegahal");
		
		// Add predefined variables to the system
		RuleProcessor.updateSystemVariable("robotname", robotname);
		
		Scanner scanner = new Scanner(System.in);
		try {
//			log.info("Enter \"" + QUIT + "\" to exit the program.");
			String userInput = "";
			
			// For command line interface
			while (!QUIT.equals(userInput)) {
				System.out.print("> ");
				userInput = scanner.nextLine();
				
				if (!"".equals(userInput.trim())) {
					// If it is speaking, send out the "shut up" signal
					if (isSpeaking) {
						if (!speakingTheLastSentence) shutUp = true;
						continue;
					}
					
					// Directly publish an utterance
					if (userInput.startsWith(">")) {
						ROSPublisher.publish("itf_talk", userInput.replace(">", "").trim());
						continue;
					}
					
					speakingTheLastSentence = false;
					nothingSpokenYet = true;
					lastOutputUtterance = new InputProcessor().getReply(userInput);
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