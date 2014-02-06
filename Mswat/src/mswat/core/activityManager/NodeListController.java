package mswat.core.activityManager;

import java.util.ArrayList;

import mswat.core.CoreController;
import mswat.core.feedback.FeedBack;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Content Tree controller
 * 
 * @author Andre Rodrigues
 * 
 */
public class NodeListController {

	// Debugging purposes
	private final String LT = "NodeListController";

	private HierarchicalService hs;

	// Content list
	ArrayList<Node> listCurrentNodes = new ArrayList<Node>();

	// Auto feedback variables
	private boolean audioFeedback;
	private boolean visualFeedback;

	// navigation variables
	final int FOWARD = 0;
	final int BACKWARD = 1;
	int currentNavIndex = -1;
	int navDirection = FOWARD;
	boolean startedLineNav = true;

	/**
	 * Initialises reference to the hierarchical service
	 * 
	 * @param hs
	 */
	public NodeListController(HierarchicalService hs,
			boolean autoAudioFeedback, boolean autoVisualFeedback) {
		this.hs = hs;
		audioFeedback = autoAudioFeedback;
		visualFeedback = autoVisualFeedback;
	}

	/**
	 * Generates the list of the content tree last children
	 * 
	 * @return List of the current last children
	 */
	public ArrayList<Node> getContent() {
		return hs.getDescribedContent();
	}

	/**
	 * Generates the current content tree
	 * 
	 * @return current full content tree
	 */
	public ArrayList<Node> getFullContent() {
		AccessibilityNodeInfo parent = hs.getContentParent();
		ArrayList<Node> list = new ArrayList<Node>();
		Node n;
		// TODO
		return null;
	}

	private ArrayList<Node> getFullContent(ArrayList<Node> list,
			AccessibilityNodeInfo source) {
		return list;
		// TODO
	}

	/**
	 * Select current focused node
	 */
	public String selectFocus() {
		if (currentNavIndex != -1 && currentNavIndex < listCurrentNodes.size()) {
			Log.d(LT, "Index:" + currentNavIndex);
			Node n = listCurrentNodes.get(currentNavIndex);
			currentNavIndex = -1;
			if (n != null) {
				if (n.getAccessNode() != null) {
					if (n.getName().equals("SCROLL")) {
						if (navDirection == FOWARD) {
							if (!n.getAccessNode()
									.performAction(
											AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)) {
								navDirection = BACKWARD;
								n.getAccessNode()
										.performAction(
												AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
							}
						} else {
							if (!n.getAccessNode()
									.performAction(
											AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)) {
								navDirection = FOWARD;
								n.getAccessNode()
										.performAction(
												AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
							}
						}
					} else {
						if (n.getAccessNode().isClickable()) {
							n.getAccessNode().performAction(
									AccessibilityNodeInfo.ACTION_CLICK);
						} else {
							if (n.getAccessNode().getParent() != null) {
								n.getAccessNode()
										.getParent()
										.performAction(
												AccessibilityNodeInfo.ACTION_CLICK);
							}
						}

					}
				}
				return n.getName();
			}

		}
		/* VISUAL FEED BACK CLEAR
		if (visualFeedback){
			FeedBack.clearHightlights();
		}*/

		return "";

	}

	/**
	 * Navigate to next node
	 */
	public void navNext() {

		if (listCurrentNodes.size() > 0) {
			currentNavIndex = (currentNavIndex + 1) % listCurrentNodes.size();

			Node n = listCurrentNodes.get(currentNavIndex);

			AccessibilityNodeInfo node = n.getAccessNode();

			if (n.getName().equals("SCROLL")) {

				if (navDirection == FOWARD) {
					if (!node
							.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)) {
						navDirection = BACKWARD;
						node.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
					}
				} else {
					if (!node
							.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)) {
						navDirection = FOWARD;
						node.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
					}

				}
				if (visualFeedback){
					//FeedBack.clearHightlights();
				}

				currentNavIndex = -1;
			} else {

				node.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);

				// auto audioFeedback
				if (audioFeedback)
					FeedBack.textToSpeech(n.getName());

				// auto visual feedback
				if (visualFeedback) {
					FeedBack.hightlight(n.getBounds().top /*- 40*/, n
							.getBounds().left, (float) 0.6, n.getBounds()
							.width(), n.getBounds().height(), Color.BLUE);
				}
			}

		}

	}

	/**
	 * Navigate to previous node
	 */
	public void navPrev() {

		if (listCurrentNodes.size() > 0) {
			if (currentNavIndex < 1)
				currentNavIndex = listCurrentNodes.size() - 1;
			else
				currentNavIndex = (currentNavIndex - 1)
						% listCurrentNodes.size();

			Node n = listCurrentNodes.get(currentNavIndex);

			AccessibilityNodeInfo node = n.getAccessNode();
			if (n.getName().equals("SCROLL")) {

				if (navDirection == FOWARD) {
					if (!node
							.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD)) {
						navDirection = FOWARD;
						node.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD);
					}
				} else {
					if (!node
							.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_FORWARD)) {
						navDirection = BACKWARD;
						node.performAction(AccessibilityNodeInfoCompat.ACTION_SCROLL_BACKWARD);
					}

				}

				currentNavIndex = -1;
			} else {
				node.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);

				// auto audioFeedback
				if (audioFeedback)
					FeedBack.textToSpeech(n.getName());

				// auto visual feedback
				if (visualFeedback) {
					FeedBack.hightlight(n.getBounds().top /*- 40*/, n
							.getBounds().left, (float) 0.6, n.getBounds()
							.width(), n.getBounds().height(), Color.BLUE);
				}
			}

		}
	}

	/**
	 * Gets node name at given coords, null if none
	 * 
	 * @param x
	 *            coord
	 * @param y
	 *            coord
	 * @return String node description
	 */
	public String getNodeAt(double x, double y) {
		// hs.printViewItens(listCurrentNodes);
		String result = "null";

		int size = listCurrentNodes.size();

		for (int i = 0; i < size; i++) {

			if (listCurrentNodes.get(i).isInside(x, y)) {
				return listCurrentNodes.get(i).getName();
			}

		}

		return result;
	}

	/**
	 * Gets node name at given coords, null if none
	 * 
	 * @param x
	 *            coord
	 * @param y
	 *            coord
	 * @return String node description
	 */
	public int getNodeIndexAt(double x, double y) {
		// hs.printViewItens(listCurrentNodes);

		int size = listCurrentNodes.size();

		for (int i = 0; i < size; i++) {
			if (listCurrentNodes.get(i).isInside(x, y)) {
				return i;
			}
		}

		return -1;
	}

	public int getNodeIndexByName(String name) {
		int size = listCurrentNodes.size();

		for (int i = size-1; i > -1; i--) {
			if (listCurrentNodes.get(i).getName().equals(name)) {
				return i;
			}
		}

		return -1;
	}
	
	public String getNodeNameByIndex(int index) {
		return listCurrentNodes.get(index).getName();
	}


	/**
	 * Return
	 * 
	 * @param x
	 * @param y
	 * @return
	 */
	public String[] getNearNode(int x, int y, int radius) {
		ArrayList<String> aux = new ArrayList<String>();
		ArrayList<Integer> angles = new ArrayList<Integer>();

		int size = listCurrentNodes.size();
		boolean inseri = false;
		float degree;

		for (int i = 0; i < size; i++) {
			Node n = listCurrentNodes.get(i);
			if (!n.isInside(x, y) && n.distanceTo(x, y) < radius) {

				String resultado = n.getName() + ","
						+ CoreController.convertToMilX(n.getX() - x) + ","
						+ CoreController.convertToMilY(n.getY() - y);

				degree = getAngle(x, y, n.getX(), n.getY());

				//Log.d(LT, "Graus" + degree + " nome:" + n.getName());

				/*
				 * if(angles.size()==0){ angles.add((int)degree);
				 * aux.add(resultado); }
				 */
				for (int j = 0; j < angles.size(); j++) {
					if (angles.get(j) > degree) {
						angles.add(j, (int) degree);
						aux.add(j, resultado);
						inseri = true;
						break;
					}
				}
				if (!inseri) {
					angles.add((int) degree);
					aux.add(resultado);
				}
				inseri = false;
				// degree=360-(degree+90);
				/*
				 * if(angles.size()<) for(int j=0;j<angles.size();j++){
				 * if(angles.get(j)>degree && degree>0){ aux.add(j,resultado);
				 * angles.add(j,(int) degree); }else
				 * 
				 * if(degree<0 && Math.abs(degree)<40){
				 * angles.add(0,(int)degree); aux.add(0,resultado); }else {
				 * angles.add((int)degree); aux.add(resultado);
				 * 
				 * }
				 * 
				 * }
				 */

			}

		}

		String[] result = new String[aux.size()];
		aux.toArray(result);
		return result;
	}

	public int getNearestNode(int x, int y, String node1, String node2) {
		int i1 = getNodeIndexByName(node1);
		int i2 = getNodeIndexByName(node2);
		if (i1 > -1) {
			if (i2 > -1) {
				Node n1 = listCurrentNodes.get((i1));
				Node n2 = listCurrentNodes.get((i2));
				if (n1.distanceTo(x, y) > n2.distanceTo(x, y))
					return -i2;
				else
					return i1;
			} else
				return i1;
		}
		return -66;
	}

	public float getAngle(int x, int y, int x1, int y1) {
		float angle = (float) Math.toDegrees(Math.atan2(x1 - x, y1 - y));

		if (angle < 0) {
			angle += 360;
		}

		return angle;
	}

	/**
	 * Returns icon associated with string s
	 * 
	 * @param s
	 * @return drawable icon
	 */
	public Drawable getIcon(String s) {
		return null;
	}

	/**
	 * Update node list with the latest view content
	 * 
	 * @param list
	 */
	void updateList(ArrayList<Node> list) {
		listCurrentNodes.clear();
		listCurrentNodes = list;
		currentNavIndex = -1;

	}

	/**
	 * Sets automatic highlight on nav
	 * 
	 * @param state
	 */
	public void setAutoHighlight(boolean state) {
		visualFeedback = state;

	}

	/**
	 * Sets automatic TTS on nav
	 * 
	 * @param state
	 */
	public void setAutoTTS(boolean state) {
		audioFeedback = state;

	}

	/**
	 * Set current focus to index
	 * 
	 * @param index
	 */
	public void focusIndex(int index) {
		if (index == -55) {
			currentNavIndex = listCurrentNodes.size() - 1;
		} else {
			currentNavIndex = index;
			if (visualFeedback) {
				if (currentNavIndex < listCurrentNodes.size()
						&& currentNavIndex > -1) { 
					Node n = listCurrentNodes.get(currentNavIndex);
					
					FeedBack.hightlight(n.getBounds().top /*- 40*/, n
							.getBounds().left, (float) 0.6, n.getBounds()
							.width(), n.getBounds().height(), Color.BLUE);
				}
			}
		}
	}

	public Node getNodeByIndex(int index) {
		return listCurrentNodes.get(index);
	}

	public void highlightIndex(int index) {
		currentNavIndex = index;
		if (currentNavIndex < listCurrentNodes.size()
				&& currentNavIndex > -1) { 
			Node n = listCurrentNodes.get(currentNavIndex);
			
			FeedBack.addHighlight(n.getBounds().top /*- 40*/, n
					.getBounds().left, (float) 0.6, n.getBounds()
					.width(), n.getBounds().height(), Color.BLUE);
		}
		
	}

	
}
