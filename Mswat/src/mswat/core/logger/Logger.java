package mswat.core.logger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.util.Log;

import mswat.core.CoreController;
import mswat.interfaces.IOReceiver;
import mswat.touch.TPRNexusS;
import mswat.touch.TouchRecognizer;

public class Logger extends BroadcastReceiver implements IOReceiver {

	private final static String LT = "Loggin";
	private static Context context;
	private int device;
	private TouchRecognizer tpr; 

	// Strings in queue to log
	private ArrayList<String> toLog = new ArrayList<String>();

	// number of records gathered before writing to file
	private final int RECORD_THRESHOLD = 2;

	private boolean logAtTouch;

	/**
	 * Initialises the logger
	 */
	@Override
	public void onReceive(Context context, Intent intent) {

		if (intent.getAction().equals("mswat_init")) {
			
			Log.d(LT,"Logger init");
			
			// context used to write to file
			this.context = context;
			
			//register logger to receive messages from the core controller
			CoreController.registerLogger(this);
			
			
			if ( intent.getExtras().getBoolean("logIO")) {

				logAtTouch = intent.getBooleanExtra("logAtTouch",false);

				// registers receivers 
				registerIOReceiver();
				
				

				// starts monitoring touchscreen
				device = CoreController.monitorTouch();

				// touch recogniser
				tpr = CoreController.getActiveTPR();

			}
		}
	}

	@Override
	public int registerIOReceiver() {
		return CoreController.registerIOReceiver(this);
	}

	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {
		if (this.device == device) {

			int result = tpr.identifyOnChange(type, code, value, timestamp);

			if (result != -1) {
				String s;
				int x = tpr.getLastX();
				int y = tpr.getLastY();
				int pressure = tpr.getPressure();
				int touchSize = tpr.getTouchSize();
				int identifier = tpr.getIdentifier();

				switch (result) {
				case TouchRecognizer.DOWN:
					if (!logAtTouch)
						s = "DOWN x:" + x + " y:" + y + " pressure:" + pressure
								+ " touchSize:" + touchSize + " id:"
								+ identifier;
					else
						s = "DOWN at:" + CoreController.getNodeAt(x, y) + " x:"
								+ x + " y:" + y + " pressure:" + pressure
								+ " touchSize:" + touchSize + " id:"
								+ identifier;
					toLog.add(s);
					 Log.d(LT, s);
					break;
				case TouchRecognizer.MOVE:

					s = "MOVE x:" + x + " y:" + y + " pressure:" + pressure
							+ " touchSize:" + touchSize + " id:" + identifier;
					toLog.add(s);
					 Log.d(LT, s);
					break;
				case TouchRecognizer.UP:
					if (!logAtTouch) {
						s = "UP x:" + x + " y:" + y + " pressure:" + pressure
								+ " touchSize:" + touchSize + " id:"
								+ identifier;
					} else
						s = "UP at:" + CoreController.getNodeAt(x, y) + " x:"
								+ x + " y:" + y + " pressure:" + pressure
								+ " touchSize:" + touchSize + " id:"
								+ identifier;
					toLog.add(s);
					 Log.d(LT, s);
					break;

				}
			}

			if (toLog.size() > RECORD_THRESHOLD) {
				registerToLog(toLog, context.getFilesDir() + "/logTest.txt");
				toLog = new ArrayList<String>();

			}

		}

	}

	/**
	 * Write to log file
	 * 
	 * @param message
	 */
	public void registerToLog(ArrayList<String> message, String filepath) {
		Log.d(LT, "Writing to:"+filepath );
		Log.d(LT, "Writing: "+message.toString() );

		LogToFile task = new LogToFile(context, message, filepath);
		task.execute();
 
	}

	/**
	 * Write the string record into the log file
	 * 
	 * @param record
	 * @return
	 */
	public boolean logToFile(String record) {
		if (toLog.size() > RECORD_THRESHOLD) {
			registerToLog(toLog, context.getFilesDir() + "/logTest.txt" );

			toLog = new ArrayList<String>();
		}

		return toLog.add(record);
	}

	/**
	 * Writes to log file the last touch
	 */
	public static class LogToFile extends AsyncTask<Void, Void, Void> {

		private Context myContextRef;
		private ArrayList<String> text;
		private String filepath;

		public LogToFile(Context context, ArrayList<String> message, String filepath) {
			this.myContextRef = context;
			text = message;
			this.filepath = filepath;
			
			//code should be in the method bellow (bug issues with screenReader)
			File file = new File(filepath );
			FileWriter fw;

			try {
				fw = new FileWriter(file, true);
				for (int i = 0; i < text.size(); i++) {
					fw.write(text.get(i) + "\n");
				}
				fw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}

		}

		@Override
		protected Void doInBackground(Void... params) {
			
			//code in the method above due to bug, should be here
			/*
			File file = new File(filepath );
			FileWriter fw;

			try {
				fw = new FileWriter(file, true);
				for (int i = 0; i < text.size(); i++) {
					fw.write(text.get(i) + "\n");
				}
				fw.close();

			} catch (IOException e) {
				e.printStackTrace();
			}
			 	*/
			return null;

		}
	}

	@Override
	public void onTouchReceived(int type) {
		// TODO Auto-generated method stub
		
	}

}
