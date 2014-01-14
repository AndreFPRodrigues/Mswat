package mswat.examples.controllers.autonav;

import java.util.ArrayList;

import mswat.controllers.ControlInterface;
import mswat.core.activityManager.Node;
import mswat.core.activityManager.R;
import android.content.Context;
import android.graphics.Color;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.WindowManager;
import android.widget.RelativeLayout;
import mswat.core.CoreController;
import mswat.keyboard.Keyboard;

public class AutoNavKeyboard extends Keyboard {
	private final String LT = "Keyboard";
	private int time = 3000;
	private boolean navigate = true;

	private final int NAV_TREE_ROW = 0;
	private final int NAV_TREE_LINE = 1;
	private int navMode = NAV_TREE_LINE;
	private WindowManager windowManager;
	private WindowManager.LayoutParams params;
	private NavTree navTree;

	public AutoNavKeyboard(Context c) {

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

		// Autonav
		navTree = new NavTree();
		createKeyboardTree();

	}

	private void createKeyboardTree() {
		ArrayList<Node> list = new ArrayList<Node>();
		int limit = 0;
		for (int i = 0; i < 26; i++) {
			char c = (char) (i + 'a');
			if (c == 'e' || c == 'i' || c == 'o' || c == 'u') {
				limit += 10;
			}
			Node n = new Node(c + "", new Rect(limit, limit, limit, limit),
					null);
			list.add(n);
		}
		limit += 10;
		Node n = new Node("Espaço", new Rect(limit, limit, limit, limit), null);
		list.add(n);
		n = new Node("Ponto", new Rect(limit, limit, limit, limit), null);
		list.add(n);
		n = new Node("Apagar", new Rect(limit, limit, limit, limit), null);
		list.add(n);
		n = new Node("Fechar", new Rect(limit, limit, limit, limit), null);
		list.add(n);

		navTree.navTreeUpdate(list);
		autoNav();

	}

	private RelativeLayout overlay;

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
		// if (overlay != null && windowManager != null) {
		windowManager.removeView(overlay);
		overlay.removeAllViews();
		overlay = null;
		// }
	}

	/**
	 * Auto navigation thread responsible to automatically cycle through
	 * lines/nodes and highlight them
	 */
	private void autoNav() {
		Thread auto = new Thread(new Runnable() {
			public void run() {

				Node n;
				// CoreController.home();

				while (navigate) {

					// TODO maybe handler
					SystemClock.sleep(time);
					// Log.d(LT, "Paused");

					if (navTree.available()) {

						switch (navMode) {
						case NAV_TREE_LINE:
							n = navTree.nextLineStart();
							if (n != null) {

								CoreController.textToSpeech(n.getName()
										+ " Linha");

							}
							break;
						case NAV_TREE_ROW:
							n = navTree.nextNode();
							if (n != null) {

								boolean waitFor = CoreController
										.waitFortextToSpeech(n.getName());
								Log.d(LT, "Read: " + n.getName());

							} else {
								navMode = NAV_TREE_LINE;
							}
							break;
						}
					}
				}
			}
		});

		auto.start();

	}

	public void touch(int touchType) {

		// either change navigation mode or select current focused
		// node
		if (navMode == NAV_TREE_LINE) {
			navMode = NAV_TREE_ROW;
		} else {
			navMode = NAV_TREE_LINE;

			// Log.d(LT, "Write: " + navTree.getCurrentNode().getName());
			String nodeText = navTree.getCurrentNode().getName();
			if (nodeText.length() > 2) {
				if (nodeText.equals("Espaço"))
					writeChar(' ');
				else if (nodeText.equals("Ponto"))
					writeChar('.');
				else if (nodeText.equals("Apagar"))
					backSpace();
				else {
					navigate = false;
					hide();
					AutoNav.keyboardState = false;
				}
			} else 
				writeString(nodeText);

			navTree.resetCollumIndex();
			navTree.prevRow();

		}

	}

}
