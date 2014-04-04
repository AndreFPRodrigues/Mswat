package mswat.interfaces;

import java.util.ArrayList;

import mswat.core.activityManager.Node;

public interface ContentReceiver {
	// type of content to be received
	final int ALL_CONTENT = 0;
	final int DESCRIBABLE = 1;
	final int CLICKABLE = 2;

	// both clickable and describable
	final int INTERACTIVE = 3;
	
	// both clickable and describable
		final int INTERACTIVE_CHILDREN= 4;
	

	/**
	 * Registers receiver, returns the receiver identifier (index)
	 * 
	 * @return
	 */
	public int registerContentReceiver();

	/**
	 * Receives arrayList with the updated content
	 * 
	 * @param content
	 */
	public abstract void onUpdateContent(ArrayList<Node> content);

	public abstract int getType();

	

}
