package mswat.caseStudy.controllers.autonav;

import java.util.ArrayList;

import mswat.core.CoreController;
import mswat.core.activityManager.Node;
import android.graphics.Rect;
import android.util.Log;

/**
 * Class that creates the struct to navigate in lines/rows
 * 
 * @author Andre Rodrigues
 * 
 */
public class NavTree {
	private final String LT = "AutoNav";

	private ArrayList<ArrayList<Node>> navTree = new ArrayList<ArrayList<Node>>();

	// [0] - line index
	// [1] - collum index
	private int[] index = new int[2];

	private boolean changedLine = false;

	boolean pause = false;
	boolean unpause = false;

	private int minBoundTop = -1;
	private int maxBoundBot = -1;

	/**
	 * Updates navigation tree with the current content
	 */
	void navTreeUpdate(ArrayList<Node> list) {
		index[0] = -1;
		index[1] = -1;
		navTree.clear();
		int size = list.size();
		ArrayList<Node> aux = new ArrayList<Node>();
		aux.add(new Node("voltar", new Rect(), null));
		navTree.add(aux);
		aux = new ArrayList<Node>();
		Node node;
		if (size > 0) {
			aux.add(list.get(0));
			node = list.get(0);
			minBoundTop = node.getBounds().top;
			maxBoundBot = node.getBounds().bottom;

			for (int i = 1; i < size; i++) {

				if (list.get(i).getBounds().left < CoreController.M_WIDTH) {

					if (list.get(i).getY() <= maxBoundBot
							&& list.get(i).getY() >= minBoundTop) {

						if (list.get(i).getBounds().top < minBoundTop) {

							minBoundTop = list.get(i).getBounds().top;
						}
						if (list.get(i).getBounds().bottom > maxBoundBot) {
							minBoundTop = list.get(i).getBounds().bottom;
						}

						aux.add(list.get(i));
						node = list.get(i);
					} else {

						navTree.add(aux);
						aux = new ArrayList<Node>();
						aux.add(list.get(i));
						node = list.get(i);
						minBoundTop = node.getBounds().top;
						maxBoundBot = node.getBounds().bottom;
					}
				}

			}
			navTree.add(aux);
		}
	}
	/**
	 * Only prepared for nexus S android 4.1.2 default phone call
	 * @param contact 
	 */
	public void prepareCall(String contact) {
		 Log.d(LT, "before: " + navTree.toString());
		//if (navTree.size()>1 && navTree.get(1).size() > 1) {
			
			
			Node n = navTree.get(0).get(0);
			navTree.clear();

			ArrayList<Node> answerNode = new ArrayList<Node>();
			answerNode.add(new Node("" + contact, new Rect(), null));
			answerNode.add(n);
			navTree.add(answerNode);
			answerNode = new ArrayList<Node>();
			answerNode.add(n);
			navTree.add(answerNode);
		//}
		 Log.d(LT, "after: " + navTree.toString());
	}

	/**
	 * Calculates the index of the node in the Core framework representation
	 * 
	 */
	int getCurrentIndex() {

		if (index[0] == navTree.size() - 1
				&& navTree.get(index[0]).get(0).getName().equals("SCROLL"))
			return -55;

		int aux_index = 0;
		for (int i = 0; i < navTree.size(); i++) {
			if (index[0] == i)
				if (index[1] == -1) {
					//Log.d(LT, "index: " + aux_index);

					return aux_index-1;
				} else {
					//Log.d(LT, "index: " + aux_index);

					return aux_index + index[1]-1;
				}
			else
				aux_index += navTree.get(i).size();
		}
		//Log.d(LT, "index: " + aux_index);
		return aux_index-1;
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
		if (navTree.size() < 1 && navTree.get(0).size() < 1)
			return null;
		changedLine = true;
		// index[1] = 0;

		if ((index[0] + 1) >= navTree.size()) {
			pause = true;
			// CoreController.home();
		}

		index[0] = (index[0] + 1) % navTree.size();
		if (unpause && !AutoNav.handlingCall) {
			index[0] = 0;
			pause = false;
			unpause = false;
			
			/*if (!AutoNav.keyboardState)
				CoreController.home();*/

		}

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
		} else if (index[0] >= 0 && index[1] >= 0)
			return navTree.get(index[0]).get(index[1]);
		else
			return null;
	}

	Node getCurrentNode() {
		 //Log.d(LT,"size"+ navTree.size() +" i0" +index[0]+ " i1"+index[1]);
		if (navTree.size() > 0 && index[0] > -1 && index[1] > -1) {
			return navTree.get(index[0]).get(index[1]);
		}
		
		return null;
	}

	/**
	 * Reset collumn index of the autonav keyboard
	 */
	public void resetColumnIndex() {
		index[1] = -1;
	}

	/**
	 * Used in keyboard to nav the current row again after a touch
	 */
	public void prevRow() {
		index[0]--;

	}

	@Override
	public String toString() {
		return navTree.toString();
	}

	public int lineSize() {
		if (navTree != null && navTree.size()>0 && index[0] > -1 && navTree.get(index[0]) != null)
			return navTree.get(index[0]).size();
		else
			return 0;
	}

	public void resetColumnSearch() {
		index[1] = -1;

	}
	
	public int getSize(){
		int si = navTree.size();
		int size=0;
		for(int i=0;i<si;i++)
			size+=navTree.get(i).size();
		return size;
	}
	public String [] getDescriptions(){
		String result[] = new String [getSize()];
		int i=-1;
		for(ArrayList<Node> ns :navTree){
			for(Node n :ns){
				i++;
				result[i]=n.getName();
			}
		}
		return result;
	}
	public boolean checkBack() {
		if(index[0]==0 && index[1]==-1 && navTree.size()>0 && navTree.get(0).get(0).getName().equals("voltar"))
				return true;
		return false;
	}

}
