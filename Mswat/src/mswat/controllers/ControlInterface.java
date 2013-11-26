package mswat.controllers;

/**
 * Parent of all interfacing schemes
 * @author Andre Rodrigues
 *
 */

public abstract class ControlInterface {
	
	/**
	 * Send/broadcast navNext message
	 */
	void navNext(){
		//TO DO 
	}
	
	/**
	 * Send/broadcast navPrev message
	 */
	void navPrev(){
		//TO DO 
	}
	
	/**
	 * Send/broadcast selectCurrent message
	 */
	void selectCurrent(){
		//TO DO 
	}
	
	/**
	 * Send/broadcast focusIndex message
	 */
	void focusIndex(int index){
		//TO DO 
	}
	
	/**
	 * Stub for receiving broadcast messages 
	 */
	abstract void onMessageReceive();
}
