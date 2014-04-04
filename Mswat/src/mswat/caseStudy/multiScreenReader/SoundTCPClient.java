package mswat.caseStudy.multiScreenReader;

import java.util.ArrayList;

import android.os.AsyncTask;
import android.util.Log;
import mswat.tcpClient.TCPClient;

public class SoundTCPClient {
	private final String LT = "ScreenReader";
	private TCPClient mTcpClient; 

	public SoundTCPClient(String ip){ 
		 
		// connect to the server
        new connectTask().execute(ip);
        
        
	}
	
	public void sendMessage(String message){
		 //Log.d(LT, "Message:" + message);

		 //sends the message to the server
        if (mTcpClient != null) {
            mTcpClient.sendMessage(message);
        }
	} 
	
	public void stopClient(){
		if(mTcpClient!=null)
			mTcpClient.stopClient();
	}
	
	 public class connectTask extends AsyncTask<String,String,TCPClient> {
		   
	        @Override
	        protected TCPClient doInBackground(String... message) {
	        	Log.d(LT,"IP:" + message[0]);
	            //we create a TCPClient object and
	            mTcpClient = new TCPClient(message[0], 7999, new TCPClient.OnMessageReceived() {
	                @Override
	                //here the messageReceived method is implemented
	                public void messageReceived(String message) {
	                    //this method calls the onProgressUpdate
	                    publishProgress(message);
	                }
	            });
	            mTcpClient.run(); 
	 
	            return null;
	        }
	 
	        @Override
	        protected void onProgressUpdate(String... values) {
	            super.onProgressUpdate(values);          
	            Log.d(LT, "received: " + values[0]);
	        
	        }
	    }
}
