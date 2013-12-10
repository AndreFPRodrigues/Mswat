package mswat.core.activityManager;

import java.util.ArrayList;

import android.graphics.Rect;
import android.view.accessibility.AccessibilityNodeInfo;

/**
 * Represents one node of the tree of content
 * 
 * @author Andre Rodrigues
 * 
 */
public class Node {

	private int x;
	private int y;
	private String description;
	private Rect bounds;
	private int iconIndex = -1;
	private AccessibilityNodeInfo nodeInfo;

	private ArrayList<Node> childList;

	public boolean isInside(Rect rec) {
		return false;
	}

	public Node(String text, Rect bounds, AccessibilityNodeInfo source) {
		this.x = bounds.centerX();
		this.y = bounds.centerY();
		description = text;
		this.bounds = bounds;
		nodeInfo = source;

		// //////////////////////
		// TO DO list of childs//
		// //////////////////////
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	public String getName() {
		return description;
	}

	public void setIcon(int i) {
		iconIndex = i;
	}

	public int getIconIndex() {
		return iconIndex;
	}

	public ArrayList<Node> getChilds() {
		return childList;
	}

	public AccessibilityNodeInfo getAccessNode() {
		return nodeInfo;
	}

	public double distanceTo(double x, double y) {
		return Math.sqrt(Math.pow(y - this.y, 2) + Math.pow(x - this.x, 2));
	}

	public boolean isInside(double x, double y) {

		return bounds.contains((int) x, (int) y);
	}

	public Rect getBounds() {
		return bounds;
	}

	@Override
	public String toString() {
		// node text representation
		/*
		 * String s = "("+description + "," + getX() + "," + getY() + "," +
		 * iconIndex+")";
		 */
		String s = "(" + description + "," + bounds.top + "," + bounds.left
				+ "," + bounds.width() + "," + bounds.height() +","
				+ iconIndex + ")";
		return s;
	}

}
