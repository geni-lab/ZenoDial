package chatbot.plugin;

import org.jibble.jmegahal.*;
import chatbot.log.Logger;
import chatbot.log.Logger.Level;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Scanner;

public class JMegaHAL {
	private static Logger log = new Logger("JMegaHAL", Level.DEBUG);
	private static JMegaHal megahal;
	private static String BRAIN = "./res/jmegahal/brain.ser";
	
	public static void initializeFromCorpus(String filePath) {
		try {
			ArrayList<File> listOfFiles = new ArrayList<File>(Arrays.asList(new File(filePath).listFiles()));
			megahal = new JMegaHal();
			
			for (File file : listOfFiles) {
				// Skip the brain file
				if (file.getPath().equals(BRAIN)) continue;
				
				log.debug("Adding " + file.getPath());
				
				BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
		        StringBuffer buffer = new StringBuffer();
		        int ch = 0;
		        
		        while ((ch = reader.read()) != -1) {
		            buffer.append((char) ch);
		            
		            if ("\n".indexOf((char) ch) >= 0) {
		            	if (!"#".equals(buffer.toString()) && buffer.toString().contains(" ")) addSentenceToTheBrain(buffer.toString());
		                buffer = new StringBuffer();
		            }
		        }
		        
		        megahal.add(buffer.toString());
		        reader.close();
			}
			
			saveBrain();
		}
		
		catch (IOException e) {
			log.severe("Cannot initialize JMegaHAL from corpus.");
			e.printStackTrace();
		}
	}
	
	public static void initializeFromBrain(String brainFilePath) {
		try {
			File file = new File(brainFilePath);
			ObjectInputStream in = new ObjectInputStream(new FileInputStream(file));
			megahal = (JMegaHal) in.readObject();
			in.close();
		}
		
		catch (Exception e) {
			log.severe("Cannot initialize JMegaHAL from the brain file.");
			e.printStackTrace();
		}
	}
	
	public static String getReply(String input) {
		log.debug("Getting reply from JMegaHal for input: \"" + input + "\"");		
		String reply = megahal.getSentence(input);
		
		if ("".compareTo(reply.trim()) != 0) log.debug("JMegaHAL Reply = " + reply);
		else log.debug("JMegaHAL Reply = <None>");
		
//		addSentenceToTheBrain(input);
//		saveBrain();
		
		return reply;
	}
	
	private static void saveBrain() {
		try {
			ObjectOutput out = new ObjectOutputStream(new FileOutputStream(BRAIN));
			out.writeObject(megahal);
			out.close();
		}
		
		catch (IOException e) {
			log.warning("Cannot save JMegaHAL's brain.");
			e.printStackTrace();
		}
	}
	
	private static void addSentenceToTheBrain(String sentence) {
		// Make sure it contains more than one words
        if (sentence.replace('\r', ' ').replace('\n', ' ').trim().contains(" ")) megahal.add(sentence);
	}
	
	public static void main(String... args) {
		initializeFromCorpus("./res/jmegahal");
		System.out.println("JMegaHAL:");
		Scanner scanner = new Scanner(System.in);
		String userInput = "";
		
		while ((userInput = scanner.nextLine()) != null) {
			getReply(userInput);
		}
		
		scanner.close();
	}
}