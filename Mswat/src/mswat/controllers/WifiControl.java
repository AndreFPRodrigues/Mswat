package mswat.controllers;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

import mswat.core.CoreController;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.util.Log;

public class WifiControl extends ControlInterface {

	private final static String LT = "WifiControl";

	private ServerSocket serverSocket;
 
	Handler updateConversationHandler;

	Thread serverThread = null; 

	private final int NAV_NEXT = 0;
	private final int NAV_PREV = 1;
	private final int SELECT = 2;
	private final int FOCUS = 3;
	private final int TOUCH = 4;
	private final int AUTO_HIGHLIGHT = 5; 
	private final int HOME = 6;
	private final int BACK = 7;
	private final int CLICK = 8;

	public static final int SERVERPORT = 6000;

	public WifiControl() {

		Log.d(LT, "init_wifi");
		updateConversationHandler = new Handler();

		this.serverThread = new Thread(new ServerThread());
		this.serverThread.start();

	}

	protected void stop() {
		try {
			serverSocket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	class ServerThread implements Runnable {

		public void run() {
			Socket socket = null;
			try {
				serverSocket = new ServerSocket(SERVERPORT);
			} catch (IOException e) {
				e.printStackTrace();
			}
			while (!Thread.currentThread().isInterrupted()) {

				try {

					socket = serverSocket.accept();

					CommunicationThread commThread = new CommunicationThread(
							socket);
					new Thread(commThread).start();

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	class CommunicationThread implements Runnable {

		private Socket clientSocket;

		private BufferedReader input;

		public CommunicationThread(Socket clientSocket) {

			this.clientSocket = clientSocket;

			try {

				this.input = new BufferedReader(new InputStreamReader(
						this.clientSocket.getInputStream()));

			} catch (IOException e) {
				e.printStackTrace();
			}
		}

		public void run() {

			while (!Thread.currentThread().isInterrupted()) {

				try {

					String read = input.readLine();
					if (read != null)
						updateConversationHandler
								.post(new updateUIThread(read));

				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}

	}

	class updateUIThread implements Runnable {
		private String msg;

		public updateUIThread(String str) {
			this.msg = str;
		}

		@Override
		public void run() {
			String[] split = msg.split(",");
			int mode = -1;
			if (split[0].equals("navNext"))
				mode = NAV_NEXT;
			else if (split[0].equals("navPrev"))
				mode = NAV_PREV;
			else if (split[0].equals("select"))
				mode = SELECT;
			else if (split[0].equals("focus"))
				mode = FOCUS;
			else if (split[0].equals("touch"))
				mode = TOUCH;
			else if (split[0].equals("autoHighlight"))
				mode = AUTO_HIGHLIGHT;
			else if (split[0].equals("home"))
				mode = HOME;
			else if (split[0].equals("back"))
				mode = BACK;
			else if (split[0].equals("clickAt"))
				mode = CLICK;
			switch (mode) {
			case NAV_NEXT:
				navNext();
				break;
			case NAV_PREV:
				navPrev();
				break;
			case SELECT:
				selectCurrent();
				break;
			case FOCUS:
				if (split.length > 1) {
				
					focusIndex(split[1]);
				}
				break;
			case TOUCH:
				int type = Integer.parseInt(split[1]);
				CoreController.sendTouchIOReceivers(type);
				break;
			case AUTO_HIGHLIGHT:
				 CoreController.setAutoHighlight(true);
				break;
			case HOME:
				home();
				break;
			case BACK:
				 back();
				break;
			case CLICK:
				if (split.length > 1) {
					clickNode(split[1]);
				}
				break;

			}
			Log.d(LT, "Client Says: " + msg + "\n");
		}
	}

	@Override
	public void onReceive(Context arg0, Intent arg1) {
		// TODO Auto-generated method stub

	}
}