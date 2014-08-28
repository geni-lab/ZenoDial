package chatbot.ros;

import org.ros.message.MessageListener;
import org.ros.node.topic.Subscriber;
import chatbot.ChatBot;
import chatbot.log.Logger;
import chatbot.log.Logger.Level;
import chatbot.process.InputProcessor;

public class ROSListener {
	private static Logger log = new Logger("ROSListener", Level.NORMAL);
	
	public static void createInputListener() {
		Subscriber<std_msgs.String> subscriber = ROSControl.initializeSubscriber("ChatBotListener", "itf_listen");
		subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
			@Override
			public void onNewMessage(std_msgs.String message) {
				String msgReceived = message.getData();
				log.info("Message reached: \"" + msgReceived + "\"");
				
				if (ChatBot.isSpeaking) {
					if (!ChatBot.speakingTheLastSentence) ChatBot.shutUp = true;
				}
				
				else {
					ChatBot.speakingTheLastSentence = false;
					ChatBot.nothingSpokenYet = true;
					ChatBot.lastOutputUtterance = new InputProcessor().getReply(msgReceived);
				}
			}
		});
	}
	
	public static void createNextSentenceListener() {
		Subscriber<std_msgs.String> subscriber = ROSControl.initializeSubscriber("ChatBotNextSentenceListener", "itf_next_sentence");
		subscriber.addMessageListener(new MessageListener<std_msgs.String>() {
			@Override
			public void onNewMessage(std_msgs.String message) {
				ChatBot.sendNextSentence = true;
//				String msgReceived = message.getData();
//				log.info("Google Voice completed.");
			}
		});
	}
}