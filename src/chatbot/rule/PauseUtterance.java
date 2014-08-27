package chatbot.rule;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import chatbot.log.Logger;
import chatbot.log.Logger.Level;

public class PauseUtterance {
	private static Logger log = new Logger("PauseUtterance", Level.NORMAL);
	private static ArrayList<String> pauseUtterances = new ArrayList<String>();
	
	public static void readPauseUtterances(String filePath) {
		try {
			File file = new File(filePath);
			
			if (file.exists() && !file.isDirectory()) {
				String line;
				BufferedReader reader = new BufferedReader(new FileReader(filePath));
				while ((line = reader.readLine()) != null) {
					log.debug("Adding \"" + line + "\" into the pause file.");
					pauseUtterances.add(line);
				}
				reader.close();
			}
		
			else log.severe("No pause file (pause.txt) was found.");
		}
		
		catch (Exception e) {
			e.printStackTrace();
			log.severe("Error occurred when reading the pause file.");
		}
	}
	
	public static ArrayList<String> getPauseUtterances() {
		return pauseUtterances;
	}
}