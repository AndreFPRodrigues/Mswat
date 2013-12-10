package mswat.core.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


import mswat.core.activityManager.HierarchicalService;
import android.app.Service;
import android.os.AsyncTask;
import android.util.Log;

/**
 * Logging class with 3 modes (TODO)
 * log touch - logging coordinates of presses , slides, long presses 
 * log touch and content actions - same has above plus what content was pressed 
 * log content actions - only what content was selected (only available if navigation done through the framework)
 * 
 * @author Andre Rodrigues
 * 
 */
public class Logger {

	private final String LT = "Logging";
	private HierarchicalService context;

	public Logger(HierarchicalService hs) {
		context = hs;
	}

	/**
	 * Write to log file 
	 * @param message
	 */
	public void registerTouch(String message) {
		
		Log.d(LT, message);
		LogToFile task = new LogToFile(context, message);
		task.execute();

	}

	/**
	 * Write the string record into the log file
	 * 
	 * @param record
	 * @return
	 */
	public boolean logToFile(String record) {
		return false;
	}

	/**
	 * Writes to log file the last touch
	 */
	public static class LogToFile extends AsyncTask<Void, Void, Void> {

		private Service myContextRef;
		private String text;

		public LogToFile(Service myContextRef, String s) {
			this.myContextRef = myContextRef;
			text = s;
		}

		@Override
		protected Void doInBackground(Void... params) {
			File file = new File(myContextRef.getFilesDir() + "/logTest.txt");
			FileWriter fw;
			try {
				fw = new FileWriter(file, true);
				fw.write(text + "\n");
				fw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

			return null;

		}
	}
}
