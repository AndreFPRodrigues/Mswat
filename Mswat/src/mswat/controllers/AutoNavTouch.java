package mswat.controllers;

import java.util.ArrayList;

import mswat.core.CoreController;
import mswat.core.activityManager.Node;
import mswat.interfaces.ContentReceiver;
import mswat.interfaces.IOReceiver;
import mswat.touch.TouchPatternRecognizer;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.SystemClock;
import android.util.Log;

/**
 * Auto navigation using touchscreen as a switch Navigation is done firstly
 * across lines and then across the row
 * 
 * @author Andre Rodrigues
 * 
 */
public class AutoNavTouch extends ControlInterface implements IOReceiver,
		ContentReceiver {

	private final String LT = "AutoNav";
	private int time = 3000;
	private boolean navigate = true;
	private NavTree navTree;
	private final int NAV_TREE_ROW = 0;
	private final int NAV_TREE_LINE = 1;
	private int navMode = NAV_TREE_LINE;

	private TouchPatternRecognizer tpr;

	@Override
	public void onReceive(Context context, Intent intent) {
		// Triggered when the service starts
		if (intent.getAction().equals("mswat_init")
				&& intent.getExtras().get("controller").equals("autoNav")) {
			Log.d(LT, "Auto Navigation initialised");

			// starts monitoring touchscreen
			int deviceIndex = CoreController.monitorTouch();

			// blocks the touch screen
			CoreController.commandIO(CoreController.SET_BLOCK, deviceIndex,
					true);

			// register receivers
			registerContentReceiver();
			registerIOReceiver();

			// initialise touch pattern Recogniser
			tpr = new TouchPatternRecognizer();

			// initialise line/row navigation controller
			if (navTree == null)
				navTree = new NavTree();
			autoNav();

		} else if (intent.getAction().equals("mswat_stop")) {
			Log.d(LT, "MSWaT STOP");
			navigate = false;
			CoreController.clearHightlights();
		}
	}

	@Override
	public boolean registerContentReceiver() {
		return CoreController.registerContentReceiver(this);

	}

	@Override
	public int registerIOReceiver() {
		return CoreController.registerIOReceiver(this);
	}

	@Override
	public void onUpdateContent(ArrayList<Node> content) {
		if (navTree == null)
			navTree = new NavTree();
		createNavList(content);
	}

	@Override
	public void onUpdateIO(int device, int type, int code, int value,
			int timestamp) {
		// Debugg purposes stop the service
		int touchType;
		if ((touchType = tpr.identifyOnRelease(type, code, value, timestamp)) != -1) {

			switch (touchType) {
			case TouchPatternRecognizer.LONGPRESS:
				// Stops service
				CoreController.stopService();
				navigate = false;
				break;
			}

			// either change navigation mode or select current focused
			// node
			if (navMode == NAV_TREE_LINE) {
				navMode = NAV_TREE_ROW;
			} else {
				navMode = NAV_TREE_LINE;
				focusIndex(navTree.getCurrentIndex());
				selectCurrent();
			}
		}
	}

	/**
	 * Update auto nav controller
	 * 
	 */
	private void createNavList(ArrayList<Node> arrayList) {
		navTree.navTreeUpdate(arrayList);

	}

	/**
	 * Auto navigation thread responsible to automatically cycle through
	 * lines/nodes and highlight them
	 */
	private void autoNav() {
		Thread auto = new Thread(new Runnable() {
			public void run() {

				Node n;
				CoreController.home();

				while (navigate) {
					SystemClock.sleep(time);
					if (navTree.available()) {

						switch (navMode) {
						case NAV_TREE_LINE:
							n = navTree.nextLineStart();
						
							if (n != null) {
								
								if (n.getName().equals("SCROLL")) {
									CoreController.textToSpeech("SCROLL");
									CoreController.hightlight(0, 0,
											(float) 0.6,
											(int) CoreController.S_WIDTH,
											(int) CoreController.S_HEIGHT,
											Color.LTGRAY);
								} else{
									CoreController.textToSpeech(n.getName() + " Linha");
									CoreController.hightlight(
											n.getBounds().top - 40, 0,
											(float) 0.6,
											(int) CoreController.S_WIDTH, n
													.getBounds().height(),
											Color.BLUE);
								}
							}
							break;
						case NAV_TREE_ROW:
							n = navTree.nextNode();
							if (n != null) {
								CoreController.hightlight(
										n.getBounds().top - 40,
										n.getBounds().left, (float) 0.6,
										(int) n.getBounds().width(), n
												.getBounds().height(),
										Color.CYAN);
								CoreController.textToSpeech(n.getName());
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

	/**
	 * Class that creates the struct to navigate in lines/rows
	 * 
	 * @author Andre Rodrigues
	 * 
	 */
	class NavTree {

		private ArrayList<ArrayList<Node>> navTree = new ArrayList<ArrayList<Node>>();

		// [0] - line index
		// [1] - collum index
		private int[] index = new int[2];

		private boolean changedLine = false;
		
		private int minBoundTop=-1;
		private int maxBoundBot=-1;
		/**
		 * Updates navigation tree with the current content
		 */
		void navTreeUpdate(ArrayList<Node> list) {
			index[0] = -1;
			index[1] = -1;
			navTree.clear();
			int size = list.size();
			ArrayList<Node> aux = new ArrayList<Node>();
			Node node;
			if (size > 0) {
				aux.add(list.get(0));
				node = list.get(0);
				minBoundTop=node.getBounds().top;
				maxBoundBot= node.getBounds().bottom;
				for (int i = 1; i < size; i++) {
					
					if (list.get(i).getY() < maxBoundBot
							&& list.get(i).getY() > minBoundTop) {
						if(list.get(i).getBounds().top<minBoundTop){
							minBoundTop= list.get(i).getBounds().top;
						}
						if(list.get(i).getBounds().bottom > maxBoundBot){
							minBoundTop= list.get(i).getBounds().bottom;
						}
						aux.add(list.get(i));
						node = list.get(i);
					} else {
						
						navTree.add(aux);
						aux = new ArrayList<Node>();
						aux.add(list.get(i));
						node = list.get(i);
						minBoundTop=node.getBounds().top;
						maxBoundBot= node.getBounds().bottom;
					}

				}
				navTree.add(aux);
			}
		}

		/**
		 * Calculates the index of the node in the Core framework representation
		 * 
		 */
		int getCurrentIndex() {
			int aux_index = 0;
			for (int i = 0; i < navTree.size(); i++) {
				if (index[0] == i)
					return aux_index + index[1];
				else
					aux_index += navTree.get(i).size();
			}
			return aux_index;
		}

		/**
		 * Check if the navigation tree is available
		 * 
		 * @return
		 */
		boolean available() {
			return navTree.size() > 0;
		}

		/**
		 * Get the first node of the next line
		 * 
		 * @return
		 */
		Node nextLineStart() {
			if (navTree.size() < 1)
				return null;
			changedLine = true;
			// index[1] = 0;
			index[0] = (index[0] + 1) % navTree.size();

			return navTree.get(index[0]).get(0);
		}

		/**
		 * Get next node in the current row
		 * 
		 * @return
		 */
		Node nextNode() {
			if (navTree.size() < 1)
				return null;

			index[1]++;

			if (index[0] != -1 && index[1] >= navTree.get(index[0]).size()) {
				index[1] = -1;
				return null;
			} else
				return navTree.get(index[0]).get(index[1]);
		}

		Node getCurrentNode() {
			if (navTree.size() > 0 && index[0] > -1 && index[1] > -1) {
				Log.d(LT, "Auto Navigation initialised:" + index[0]);
				return navTree.get(index[0]).get(index[1]);
			}
			return null;
		}

	}

}
