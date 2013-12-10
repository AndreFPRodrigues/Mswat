package mswat.controllers;

import mswat.core.CoreController;
import android.content.BroadcastReceiver;

/**
 * Parent of all interfacing schemes
 * @author Andre Rodrigues
 *
 */

public abstract class ControlInterface extends BroadcastReceiver{
	
	
	
	/**
	 * Send navNext message
	 */
	void navNext(){
		CoreController.commandNav(CoreController.NAV_NEXT,0);
	}
	
	/**
	 * Send navPrev message
	 */
	void navPrev(){
		CoreController.commandNav(CoreController.NAV_PREV,0); 
	}
	
	/**
	 * Send selectCurrent message
	 */
	void selectCurrent(){
		CoreController.commandNav(CoreController.SELECT_CURRENT,0);
	}
	
	/**
	 * Send focusIndex message
	 */
	void focusIndex(int index){
		CoreController.commandNav(CoreController.FOCUS_INDEX,index);
 
	}
	
	
}
