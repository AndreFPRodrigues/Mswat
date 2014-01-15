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
import android.widget.RelativeLayout;
import mswat.core.CoreController;
import mswat.core.activityManager.R;
import mswat.interfaces.IOReceiver;
import mswat.keyboard.SwatKeyboard;
import mswat.touch.TouchPatternRecognizer;

public class AutoNavKeyboard extends SwatKeyboard implements IOReceiver {
	private final String LT = "Keyboard";

	private Context c;
	private WindowManager windowManager;
	private WindowManager.LayoutParams params;
	private RelativeLayout overlay;

	private int keyIndex;

	private TouchPatternRecognizer tpr;

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
	private boolean navigate = false;
	private int time = 3000;
	private final int NAV_TREE_ROW = 0;
	private final int NAV_TREE_LINE = 1;
	private int navMode = NAV_TREE_LINE;

	private void autoNav() {
		navigate = true;
		Thread auto = new Thread(new Runnable() {
			public void run() {

				while (navigate) {

					// TODO maybe handler
					SystemClock.sleep(time);
					// Log.d(LT, "Paused");

					switch (navMode) {
					case NAV_TREE_LINE:
						navDown();
						CoreController.textToSpeech(getCurrent() + " Linha");
						break;
					case NAV_TREE_ROW:
						if (navRight()) {
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

	@Override
	public void hide() {
		windowManager.removeView(overlay);
		overlay.removeAllViews();
		overlay = null;
		CoreController.unregisterIOReceiver(keyIndex);
		CoreController.stopKeyboard();
		navigate = false;

	}

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

		// start monitoring touch
		int deviceIndex = CoreController.monitorTouch();
		keyIndex = registerIOReceiver();
		
		

		autoNav();

	}

	@Override
	public void onReceive(Context context, Intent intent) {
		// Triggered when the service starts
		if (intent.getAction().equals("mswat_init")
				&& intent.getExtras().get("keyboard").equals("AutoNav")) {

			c = context;

			createAutoNavKeyboard();

			// initialise touch pattern Recogniser
			tpr = new TouchPatternRecognizer();
		}
	}

	@Override
	public int registerIOReceiver() {
		return CoreController.registerIOReceiver(this);

	}

	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {
		if ((tpr.identifyOnRelease(type, code, value, timestamp)) != -1) {
			// either change navigation mode or select current focused
			// node
			if (navMode == NAV_TREE_LINE) {
				navMode = NAV_TREE_ROW;
			} else {
				navMode = NAV_TREE_LINE;

				// Log.d(LT, "Write: " + navTree.getCurrentNode().getName());

				String nodeText = getCurrent();
				if (nodeText != null) {
					if (nodeText.length() > 2) {
						if (nodeText.equals("Espaço"))
							writeChar(' ');
						else if (nodeText.equals("Ponto"))
							writeChar('.');
						else if (nodeText.equals("Apagar"))
							backSpace();
						else {
							hide();
						}
					} else
						writeString(nodeText);

					resetSearch();
				}
			}
		}
	}

}
