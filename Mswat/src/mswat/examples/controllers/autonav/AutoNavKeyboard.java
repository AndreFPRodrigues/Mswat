package mswat.examples.controllers.autonav;

import java.util.ArrayList;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.webkit.WebView.FindListener;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.TextView;
import mswat.core.CoreController;
import mswat.core.activityManager.R;
import mswat.interfaces.IOReceiver;
import mswat.keyboard.SwatKeyboard;
import mswat.touch.TPRNexusS;
import mswat.touch.TouchRecognizer;

public class AutoNavKeyboard extends SwatKeyboard implements IOReceiver {
	private final String LT = "Keyboard";

	private Context c;
	private static WindowManager windowManager;
	private static WindowManager.LayoutParams params;
	private static RelativeLayout overlay;

	private static int keyIndex;

	private TouchRecognizer tpr;

	private final int DOWN = 0;
	private final int UP = 1;
	private int direction = DOWN;

	private StringBuilder lastWord = new StringBuilder();
	private String nodeText;

	private StringBuilder editText = new StringBuilder();

	/**
	 * Creates the struture of the keyboard to be navigated in collum-row style
	 * Last row are the commands to space, period, delete, close
	 */
	public void createAutoNavKeyboard() {
		ArrayList<ArrayList<String>> table = new ArrayList<ArrayList<String>>();
		ArrayList<String> row = new ArrayList<String>();
		for (int i = 0; i < 26; i++) {
			char c = (char) (i + 'a');

			if (c == 'e' || c == 'i' || c == 'o' || c == 'u') {
				row.add("para cima");
				row.add("para baixo");
				table.add(row);
				row = new ArrayList<String>();
			}
			row.add(c + "");
		}
		table.add(row);
		row = new ArrayList<String>();

		row.add("Espaço");
		row.add("Ponto");
		row.add("Apagar");
		row.add("Fechar");
		table.add(row);

		setKeyboard(table);

	}

	/**
	 * Auto navigation thread responsible to automatically cycle through
	 * lines/nodes and highlight them
	 */
	private static boolean navigate = false;
	private int time = 3000;
	private final int NAV_TREE_ROW = 0;
	private final int NAV_TREE_LINE = 1;
	private int navMode = NAV_TREE_LINE;

	private void autoNav() {
		navigate = true;
		resetSearch();
		Thread auto = new Thread(new Runnable() {
			public void run() {

				while (navigate) {

					// TODO maybe handler
					SystemClock.sleep(time);
					// Log.d(LT, "Paused");

					switch (navMode) {
					case NAV_TREE_LINE:
						if (direction == DOWN)
							navDown();
						else
							navUp();
						CoreController.textToSpeech(getCurrent() + " Linha");
						break;
					case NAV_TREE_ROW:
						if (navRight()) {
							resetSearch();
							direction = DOWN;
							navMode = NAV_TREE_LINE;
						} else {
							boolean waitFor = CoreController
									.waitFortextToSpeech(getCurrent());
						}
						break;
					}
				}

			}
		});

		auto.start();

	}

	/**
	 * Show keyboard layout
	 */
	@Override
	public void show(final Context c) {
		Handler hightligherHandler = new Handler();
		hightligherHandler.post(new Runnable() {
			public void run() {
				LayoutInflater layoutInflater = (LayoutInflater) c
						.getSystemService(c.LAYOUT_INFLATER_SERVICE);
				overlay = (RelativeLayout) layoutInflater.inflate(
						R.layout.autonavkeyboard, null);
				windowManager.addView(overlay, params);

			}
		});
	}

	/**
	 * Show keyboard layout
	 */
	public void writeToTextBox(final Context c, final String text) {

		Handler hightligherHandler = new Handler();
		hightligherHandler.post(new Runnable() {
			public void run() {

				if (overlay != null)
					windowManager.removeView(overlay);
				LayoutInflater layoutInflater = (LayoutInflater) c
						.getSystemService(c.LAYOUT_INFLATER_SERVICE);
				overlay = (RelativeLayout) layoutInflater.inflate(
						R.layout.autonavkeyboard, null);
				TextView tv = (TextView) overlay
						.findViewById(R.id.TextViewText);
				tv.append(text);
				windowManager.addView(overlay, params);

			}
		});
	}

	/**
	 * Close/hide keyboard
	 */
	@Override
	public void hide() {
		if (overlay != null && navigate) {
			navigate = false;
			windowManager.removeView(overlay);
			overlay.removeAllViews();
			overlay = null;
			CoreController.unregisterIOReceiver(keyIndex);
			CoreController.stopKeyboard();
			windowManager = null;
			editText = new StringBuilder();
		}

	}

	/**
	 * Starts up the keyboard
	 */
	@Override
	public void start() {
		// prepare
		windowManager = (WindowManager) c.getSystemService(c.WINDOW_SERVICE);
		params = new WindowManager.LayoutParams(
				WindowManager.LayoutParams.FILL_PARENT,
				WindowManager.LayoutParams.FILL_PARENT,
				WindowManager.LayoutParams.TYPE_SYSTEM_ALERT,
				WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE
						| WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE,
				PixelFormat.TRANSLUCENT);

		// Create keyboard overlay
		show(c);

		Log.d(LT, "STARTED KEYBOARD");

		// start monitoring touch
		int deviceIndex = CoreController.monitorTouch();
		keyIndex = registerIOReceiver();

		// initialise touch pattern Recogniser
		tpr = CoreController.getActiveTPR();
		// start autonavigation of the keyboard
		autoNav();

		for (int i = 0; i < 100; i++)
			del();

	}

	/**
	 * Receive the init signal
	 */
	@Override
	public void onReceive(Context context, Intent intent) {
		// Triggered when the service starts

		if (intent.getAction().equals("mswat_init")
				&& intent.getExtras().get("keyboard").equals("AutoNav")) {

			c = context;

			createAutoNavKeyboard();

		} else if (intent.getAction().equals("mswat_keyboard")
				&& (!intent.getExtras().getBoolean("status"))) {
			hide();
		}

	}

	@Override
	public int registerIOReceiver() {
		return CoreController.registerIOReceiver(this);

	}

	/**
	 * On touch
	 */
	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {
		int touchtype;
		if ((touchtype=tpr.identifyOnRelease(type, code, value, timestamp)) != -1) {
			handleTouch(touchtype);
		}
	}

	private void handleTouch(int type) {
		
		Log.d(LT, "TOUCHED KEYBOARD");
		// either change navigation mode or select current focused
		// node
		if (navMode == NAV_TREE_LINE) {
			navMode = NAV_TREE_ROW;
			resetColumnSearch();
		} else {
			navMode = NAV_TREE_LINE;

			// Log.d(LT, "Write: " + navTree.getCurrentNode().getName());

			nodeText = getCurrent();
			if (nodeText != null) {
				if (nodeText.length() > 2) {
					if (nodeText.equals("Espaço")) {
						writeChar(' ');
						CoreController.waitFortextToSpeech(lastWord.toString());
						lastWord = new StringBuilder();
					} else if (nodeText.equals("Ponto")) {
						writeChar('.');
						editText.append(".");
						resetSearch();

					} else if (nodeText.equals("Apagar")) {
						backSpace();
						editText.deleteCharAt(editText.length() - 1);

						resetSearch();
					} else if (nodeText.contains("cima")) {
						direction = UP;
						resetSearchUp();
					} else if (nodeText.contains("baixo")) {
						resetSearchDown();
						direction = DOWN;

					} else {
						hide();
					}
				} else {
					writeString(nodeText);
					lastWord.append(nodeText);
					editText.append(nodeText);
					CoreController.waitFortextToSpeech(nodeText);
					direction = DOWN;
					resetSearch();

				}
			}
		}

	}

	@Override
	public void update() {
		if (nodeText != null)
			writeToTextBox(c, editText.toString());

	}

	@Override
	public void onTouchReceived(int type) {
		handleTouch(type);
	}

}
