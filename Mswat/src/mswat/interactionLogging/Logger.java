package mswat.interactionLogging;

/**
 * Logging class with 3 modes
 * log touch - logging coordinates of presses, slides, long presses
 * log touch and content actions - same has above plus what content was pressed
 * log content actions - only what content was selected (only available if navigation done through the framework)
 * @author Andre Rodrigues
 *
 */
public class Logger {
	
	
	/**
	 * stub method, filters relevant messages
	 * @param message
	 */
	public void onReceived(String message){
		
	}

	
	/**
	 * Write the string record into the log file
	 * @param record
	 * @return
	 */
	public boolean logToFile(String record){
		return false;
	}
}
