package mswat.core.macro;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Currency;
import java.util.HashMap;
import java.util.Scanner;
import java.util.Stack;
import java.util.regex.Pattern;

import mswat.core.CoreController;
import mswat.core.activityManager.Node;
import mswat.interfaces.ContentReceiver;

import android.os.Environment;
import android.os.SystemClock;
import android.util.Log;

public class MacroManagment {
	private final static String LT = "Macro";

	private HashMap<String, ArrayList<String>> macros;
	private long lastStep = 0;
	private long lastWrite = 0;
	private String filepath;
	private String currentName;
	private ArrayList<String> currentMacro;

	public static final int NAV_MACRO = 0;
	public static final int TOUCH_MACRO = 1;
	private int macroMode = NAV_MACRO;

	public MacroManagment() {
		macros = new HashMap<String, ArrayList<String>>();
		filepath = Environment.getExternalStorageDirectory().toString()
				+ "/macros/macros.text";
		File f = new File(filepath);
		Scanner scanner;
		try {
			scanner = new Scanner(f);

			while (scanner.hasNextLine()) {
				String line = scanner.nextLine();
				Log.d(LT, "l:" + line);
				String split[] = line.split(";");
				if (split.length > 1) {
					String splitModes[] = split[1].split(Pattern.quote("!*!"));
					int mode = 0;
					ArrayList<String> commands = new ArrayList<String>();
					for (String com : splitModes) {
						//Log.d(LT, "Split string:" + com);

						if ((mode % 2) == 0) {
							String[] splitCommands = com.split(",");
							for (String com1 : splitCommands){
								Log.d(LT, "Add command:" + com1);

								commands.add(com1);
							}
						} else {
							commands.add(com);
							Log.d(LT, "Add command:" + com);
						}
						commands.add("!*!");
						Log.d(LT, "Add command:" + "!*!");

						mode++;
					}

					macros.put(split[0], commands);

				}
			}
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public void createMacro(String name) {
		Log.d(LT, "new macro");
		currentMacro = new ArrayList<String>();
		currentName = name;
	}

	public boolean addStepTo(String macro, String step) {
		long ls = System.currentTimeMillis();
		if (macroMode == TOUCH_MACRO
				|| ((currentMacro.size() > 0 || (currentMacro.size() == 0 && !step
						.equals("Ok"))) && ((ls - lastStep) > 500) )) {
			lastStep = ls;
			Log.d(LT, "macro:" + step);

			return currentMacro.add(step);
		}

		return false;
	}

	public boolean finishMacro() {
		long ls = System.currentTimeMillis();
		if ((ls - lastWrite) > 500) {
			lastWrite = ls;
			macros.put(currentName, currentMacro);
			// code should be in the method bellow (bug issues with
			// screenReader)
			File file = new File(filepath);
			FileWriter fw;

			try {
				fw = new FileWriter(file, true);
				fw.write(currentName + ";");
				for (String macro : currentMacro) {
					fw.write(macro + ",");
				}
				fw.write("\n");
				fw.close();

			} catch (IOException e) {
				Log.d(LT, "exception");

				e.printStackTrace();
			}
			return true;
		}
		return false;
	} 

	public Stack<String> runMacro(String macro) {
		ArrayList<String> cm = macros.get(macro); 
		Stack<String> st = new Stack<String>();
		for (String s : cm) {
			Log.d(LT, "added to command: " + s);
			st.add(0, s);
		}
		return st;

	}

	public void changeMode(int mode) {
		addStepTo(currentName, "!*!");
		macroMode = mode;

	}

}
