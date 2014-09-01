package chatbot.process;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import chatbot.log.Logger;
import chatbot.log.Logger.Level;

public class PrefixOfUtterance {
	private static Logger log = new Logger("PrefixOfUtterance", Level.NORMAL);
	private static ArrayList<String> PrefixUtterances = new ArrayList<String>();
	
	public static void readPrefixUtterances(String filePath) {
		try {
			File file = new File(filePath);
			
			if (file.exists() && !file.isDirectory()) {
				String line;
				BufferedReader reader = new BufferedReader(new FileReader(filePath));
				while ((line = reader.readLine()) != null) {
					log.debug("Adding \"" + line + "\" into the prefix file.");
					PrefixUtterances.add(line);
				}
				reader.close();
			}
		
			else log.severe("No prefix file (prefix.txt) was found.");
		}
		
		catch (Exception e) {
			e.printStackTrace();
			log.severe("Error occurred when reading the prefix file.");
		}
	}
	
	public static ArrayList<String> getPrefixUtterances() {
		return PrefixUtterances;
	}
}