package mswat.core;

import mswat.core.activityManager.NodeListController;
import mswat.core.ioManager.Monitor;
import android.os.Handler;

public class CoreController {

	// handles monitor messages
	Handler monitorMessages = new Handler();

	// handles received messages
	Handler receivedMessages = new Handler();

	// handles nodeListControllerMessages
	Handler nodeControllerMessages = new Handler();
	
	//Modules where to forward messages
	NodeListController nController;
	Monitor monitor;
	
	/**
	 * Iniatialise CoreController
	 * @param nController
	 * @param monitor
	 */
	public CoreController(NodeListController nController, Monitor monitor){
		this.monitor=monitor;
		this.nController=nController;
	}

	/**
	 * Stub method, when a message creates a handler to handle the message
	 * 
	 * @param s
	 */
	public void receive(String message) {
		receivedMessages(message);

	}

	/**
	 * Broadcasts monitor messages
	 * 
	 * @param device
	 *            - event origin
	 * @param type
	 * @param code
	 * @param value
	 */
	public void monitorMessages(final int device, final int type,
			final int code, final int value) {
		monitorMessages.post(new Runnable() {
			public void run() {

			}
		});
	}

	/**
	 * Forwards the message to the appropriate component
	 * 
	 * @param message
	 */
	public void receivedMessages(final String message) {
		receivedMessages.post(new Runnable() {
			public void run() {

				// Separates and fowards messages to the apropriate module
				switch (0) {

				}
			}
		});
	}

	/**
	 * Broadcasts node controller messages
	 * @param message
	 */
	public void nodeMessages(final String message) {
		nodeControllerMessages.post(new Runnable() {
			public void run() {

			}
		});
	}

}
