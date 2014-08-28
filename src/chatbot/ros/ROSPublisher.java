package chatbot.ros;

import java.util.ArrayList;
import java.util.Random;
import org.ros.node.topic.Publisher;
import chatbot.ChatBot;
import chatbot.log.Logger;
import chatbot.log.Logger.Level;
import chatbot.rule.PauseUtterance;

public class ROSPublisher {
	private static Logger log = new Logger("ROSPublisher", Level.NORMAL);
	
	public static void startPausePublisher(final String userUtterance) {
		Thread saySomethingThread = new Thread() {
			public void run() {
				try {
					Publisher<std_msgs.String> publisher = ROSControl.getPublisher("itf_talk");
					
					// Time to wait util the 1st pause sentence is published
					Thread.sleep(2000);
					
					while (ChatBot.nothingSpokenYet && PauseUtterance.getPauseUtterances() != null) {
						String utterance = PauseUtterance.getPauseUtterances().get(new Random().nextInt(PauseUtterance.getPauseUtterances().size()));

						if ("<UU>".equals(utterance)) {
							utterance = userUtterance;
						}
						
						std_msgs.String pubStr = publisher.newMessage();
						pubStr.setData(utterance);
						Thread.sleep(130);
						
						ChatBot.isSpeaking = true;
						log.debug("Publishing: " + utterance);
						publisher.publish(pubStr);
						ChatBot.sendNextSentence = false;
						
						// ==================================== Time to wait until the next pause sentence is published are the sum of the following Thread sleep times ==================================== //
						// Time to wait before publishing the actual reply
						Thread.sleep(500);
						ChatBot.isSpeaking = false;
						// Extra time to wait before publishing the the next pause sentence
						Thread.sleep(2500);
					}
				}
				
				catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}; saySomethingThread.start();
	}
	
	public static void publish(final String rosTopic, final String output) {		
		Thread talkThread = new Thread() {
			public void run() {
				// A thread to send utterances to TTS
			    Publisher<std_msgs.String> publisher = ROSControl.getPublisher(rosTopic);
				
				try {
					int maxSentenceLength = 90; // in terms of the number of characters
					int timer = 0;
					int maxWaitingTime = 100; // 100 x sleep(100) = 10 seconds
					ArrayList<String> sentences = splitUtterance(output, maxSentenceLength);
					
					// ==================================== Just in case if the robot is currently speaking, wait for it to finish, halt when timeout ==================================== //
					if (ChatBot.isSpeaking) {
						log.debug("+++++ Waiting for the current speech to finish +++++");
						log.debug("\"" + output + "\" is in the queue...");
						
						while (ChatBot.isSpeaking && timer < maxWaitingTime) {
							timer++;
							Thread.sleep(100);
							if (timer >= maxWaitingTime) log.warning("+++++ TIMEOUT! +++++");
						}
					}
					
					// ==================================== Send out the utterance sentence by sentence ==================================== //
					StopTheThread:
					for (int i = 0; i < sentences.size(); i++) {
						// Send the next sentence when the the first sentence is finished
						if (i != 0) {
							while (!ChatBot.sendNextSentence) {
								// Wait until it is allowed to send the next sentence
								Thread.sleep(100);
								
								// If a "shut up" signal is received, stop the thread from sending the rest of the sentences
								if (ChatBot.shutUp) {
									ChatBot.shutUp = false;
									log.warning("<< The speech is interrupted >>");
									break StopTheThread;
								}
							}
						}
						
						String sentence = sentences.get(i);
						std_msgs.String pubStr = publisher.newMessage();
						pubStr.setData(sentence);
						Thread.sleep(130);
						
						ChatBot.isSpeaking = true;
						log.debug("Publishing: " + sentence);
						publisher.publish(pubStr);
						ChatBot.sendNextSentence = false;
					}
					
					// ==================================== Wait for the last sentence to finish, halt when timeout ==================================== //
					ChatBot.speakingTheLastSentence = true;
					log.debug("+++++ Waiting for the last sentence to finish +++++");
					timer = 0;
					while (!ChatBot.sendNextSentence && timer < maxWaitingTime) {
						timer++;
						Thread.sleep(100);
						if (timer >= maxWaitingTime) log.warning("+++++ TIMEOUT! +++++");
					}
					log.debug("+++++ Last sentence finished +++++");
					ChatBot.isSpeaking = false;
			    }
			    
			    catch(InterruptedException e) {
			    	e.printStackTrace();
			    }
			}
		}; talkThread.start();
	}
	
	private static ArrayList<String> splitUtterance(String utterance, int maxLength) {
		utterance = utterance
				.replace('?', '.')
				.replace('!', '.')
				.replace(';', '.')
				.replace("...", ".")
				.toLowerCase();
		ArrayList<String> sentences = new ArrayList<String>();

		while (utterance.length() > 0 && utterance.length() > maxLength) {
			int indexToSplit = checkAndSplit(utterance, ".", maxLength, true);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, ",", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " where ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " who ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " whom ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " and ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " but ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " or ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " if ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " then ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " so ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " also ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " moreover ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " by the way ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " because of ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " because ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " hence ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " therefore ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " however ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " in ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " on ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " at ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " when ", maxLength, false);
			if (indexToSplit == 0) indexToSplit = checkAndSplit(utterance, " ", maxLength, false);
			
			if (indexToSplit > 0 && indexToSplit < maxLength) {
				sentences.add(utterance.substring(0, indexToSplit + 1));
				utterance = utterance.substring(indexToSplit + 1).trim();
			}
			
			else {
				log.severe("Error!! Index = " + indexToSplit);
			}
		}
		
		sentences.add(utterance);
//		int count = 1;
//		for (String sentence : sentences) {
//			System.out.println("(" + count + ") " + sentence);
//			count++;
//		}
		
		return sentences;
	}
	
	private static int checkAndSplit(String strToSplit, String splitter, int maxLength, boolean getTheFirstOne) {
		int index = 0;
		
		if (getTheFirstOne) index = strToSplit.indexOf(splitter);
		else index = strToSplit.substring(0, maxLength).trim().lastIndexOf(splitter);
		
		if (index >= maxLength || index < 0) return 0;
		else return index;
	}
}