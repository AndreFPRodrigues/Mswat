package mswat.core.activityManager;

import java.util.ArrayList;

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

	//represents the ability to scroll
	private Node scrollNode = null;

	//Auto feedback variables
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
	public void selectFocus() {
		

		if (currentNavIndex != -1 && currentNavIndex < listCurrentNodes.size()) {
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
						if (n.getAccessNode().isClickable())
							n.getAccessNode().performAction(
									AccessibilityNodeInfo.ACTION_CLICK);
						else {
							if (n.getAccessNode().getParent() != null) {
								n.getAccessNode()
										.getParent()
										.performAction(
												AccessibilityNodeInfo.ACTION_CLICK);
							}
						}

					}
				}
			}
		}
		if(visualFeedback)
			FeedBack.clearHightlights();

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
				if(visualFeedback)
					FeedBack.clearHightlights();
				
				currentNavIndex = -1;
			} else {

				node.performAction(AccessibilityNodeInfoCompat.ACTION_ACCESSIBILITY_FOCUS);

				// auto audioFeedback
				if (audioFeedback)
					FeedBack.textToSpeech(n.getName());

				// auto visual feedback
				if (visualFeedback) {
					FeedBack.hightlight(n.getBounds().top - 40,
							n.getBounds().left, (float) 0.6, n.getBounds()
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
					FeedBack.hightlight(n.getBounds().top - 40,
							n.getBounds().left, (float) 0.6, n.getBounds()
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
		hs.printViewItens(listCurrentNodes);
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
	 * @param list
	 */
	void updateList(ArrayList <Node>list){
		listCurrentNodes.clear();
		listCurrentNodes=list;
		currentNavIndex=-1;
		
	}
	
	/**
	 * Sets automatic highlight on nav
	 * @param state
	 */
	public void setAutoHighlight(boolean state) {
		visualFeedback=state;
		
	}
	
	/**
	 * Sets automatic TTS on nav
	 * @param state
	 */
	public void setAutoTTS(boolean state) {
		audioFeedback=state;
		
	}

	/**
	 * Set current focus to index
	 * @param index
	 */
	public void focusIndex(int index) {
		currentNavIndex=index;
	}
}
