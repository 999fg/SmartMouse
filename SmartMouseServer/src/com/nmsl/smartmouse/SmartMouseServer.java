package com.nmsl.smartmouse;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import javax.bluetooth.BluetoothStateException;
import javax.bluetooth.LocalDevice;
import javax.bluetooth.RemoteDevice;
import javax.bluetooth.UUID;
import javax.microedition.io.Connector;  
import javax.microedition.io.StreamConnection;  
import javax.microedition.io.StreamConnectionNotifier;  
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.awt.AWTException;
import java.awt.MouseInfo;
import java.awt.Robot;


public class SmartMouseServer{
	  
    public static void main(String[] args){ 
    	
    	
		log("Local Bluetooth device...\n");
        
    	LocalDevice local = null;
		try {
			
			local = LocalDevice.getLocalDevice();
		} catch (BluetoothStateException e2) {
			
		}   
		
    	log( "address: " + local.getBluetoothAddress() );
    	log( "name: " + local.getFriendlyName() );
    	
    	
    	Runnable r = new ServerRunable();
    	Thread thread = new Thread(r);
    	thread.start();
    	
    }
    
      
    private static void log(String msg) {  
    	
        System.out.println("["+(new Date()) + "] " + msg);  
    }

}


class ServerRunable implements Runnable{
	  
	//UUID for SPP
	final UUID uuid = new UUID("0000110100001000800000805F9B34FB", false);
    final String CONNECTION_URL_FOR_SPP = "btspp://localhost:"
    			+ uuid +";name=SPP Server";
  
    private StreamConnectionNotifier mStreamConnectionNotifier = null;  
    private StreamConnection mStreamConnection = null; 
    private int count = 0;
   
    private static Robot robot;
    
	@Override
	public void run() {

    	try {
    		
			mStreamConnectionNotifier = (StreamConnectionNotifier) Connector
						.open(CONNECTION_URL_FOR_SPP);
			
			log("Opened connection successful.");
		} catch (IOException e) {
			
			log("Could not open connection: " + e.getMessage());
			return;
		}
    	
    	try {
    		robot = new Robot();
    	} catch (AWTException e) {
    		e.printStackTrace();
    	}
   

    	log("Server is now running.");

    	
    	
        while(true){
        	
        	log("wait for client requests...");

			try {
				
				mStreamConnection = mStreamConnectionNotifier.acceptAndOpen();
			} catch (IOException e1) {
				
				log("Could not open connection: " + e1.getMessage() );
			}
			
        	
			count++;
			log("���� ���� ���� Ŭ���̾�Ʈ ��: " + count);
			
								
	        new Receiver(mStreamConnection).start();
        }
		
	}
	
        
    
    class Receiver extends Thread {
    	
    	private InputStream mInputStream = null; 
        private OutputStream mOutputStream = null; 
        private String mRemoteDeviceString = null;
        private StreamConnection mStreamConnection = null;
        
        
        Receiver(StreamConnection streamConnection){
        	
        	mStreamConnection = streamConnection;

			try {
			    	
				mInputStream = mStreamConnection.openInputStream();
				mOutputStream = mStreamConnection.openOutputStream();
									
				log("Open streams...");
			} catch (IOException e) {
				
				log("Couldn't open Stream: " + e.getMessage());

				Thread.currentThread().interrupt();		
				return;
			}
			
			
			try {
		        	
					RemoteDevice remoteDevice 
						= RemoteDevice.getRemoteDevice(mStreamConnection);
					
			        mRemoteDeviceString = remoteDevice.getBluetoothAddress();
			        
					log("Remote device");
					log("address: "+ mRemoteDeviceString);
			        
				} catch (IOException e1) {
					
					log("Found device, but couldn't connect to it: " + e1.getMessage());
					return;
			}
			
			log("Client is connected...");
        }
        
        
    	@Override
		public void run() {
    		
    		String[] recvAccelerometer = new String[3];
    		
    		double accelerationX = 0, accelerationY = 0, velocityX = 0, velocityY = 0, displacementX = 0, displacementY = 0;
    		double timestamp = 0, timeinterval = 0, tmp = 0;
    		int count = 0;
    		
			try {
				
	    		Reader mReader = new BufferedReader(new InputStreamReader
			         ( mInputStream, Charset.forName(StandardCharsets.UTF_8.name())));
				
	    		boolean isDisconnected = false;
	    		
				Sender("������ �����ϼ̽��ϴ�.");
	    		
				while(true){

					log("ready");
	
			        
		            StringBuilder stringBuilder = new StringBuilder();
		            int c = 0;
		            
		            
					while ( '\n' != (char)( c = mReader.read()) ) {
						
						if ( c == -1){
							
							log("Client has been disconnected");
							
							count--;
							log("���� ���� ���� Ŭ���̾�Ʈ ��: " + count);
							
							isDisconnected = true;
							Thread.currentThread().interrupt();
							
							break;
						}
						
						stringBuilder.append((char) c);
					}
	
		            if ( isDisconnected ) break;
		            
		            String recvMessage = stringBuilder.toString();
			        log( mRemoteDeviceString + ": " + recvMessage );
			        recvAccelerometer = recvMessage.split(",");
			        
			        if (count == 0) {
			        	timestamp = Double.parseDouble(recvAccelerometer[3]);
			        } else if (count == 1) {
			        	timeinterval = (Double.parseDouble(recvAccelerometer[3]) - timestamp) / 1000000000;
				        timestamp = Double.parseDouble(recvAccelerometer[3]);
				        accelerationX = Double.parseDouble(recvAccelerometer[0]);
				        velocityX = accelerationX * timeinterval + velocityX;
				        accelerationY = Double.parseDouble(recvAccelerometer[1]);
				        velocityY = accelerationY * timeinterval + velocityY;
			        } else {
			        	tmp = velocityX;
			        	timeinterval = (Double.parseDouble(recvAccelerometer[3]) - timestamp) / 1000000000;
				        timestamp = Double.parseDouble(recvAccelerometer[3]);
				        accelerationX = Double.parseDouble(recvAccelerometer[0]);
				        velocityX = accelerationX * timeinterval + velocityX;
				        displacementX = tmp * timeinterval + 0.5 * accelerationX * timeinterval * timeinterval;
				        tmp = velocityY;
				        accelerationY = Double.parseDouble(recvAccelerometer[1]);
				        velocityY = accelerationY * timeinterval + velocityY;
				        displacementY = tmp * timeinterval + 0.5 * accelerationY * timeinterval * timeinterval;
				        System.out.println("HAHA");
				        System.out.println(timeinterval + " " + accelerationX + " " + velocityX + " " + displacementX + " " + displacementY);
			        	//robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX() + displacementX * 10 ), (int) (MouseInfo.getPointerInfo().getLocation().getY() +displacementY * 10));
			        	robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX() + displacementX * 100 ), (int) (MouseInfo.getPointerInfo().getLocation().getY()));
			        }
			        count ++;
				}
				
			} catch (IOException e) {
				
				log("Receiver closed" + e.getMessage());
			}
		}
    	

    	void Sender(String msg){
        	
            PrintWriter printWriter = new PrintWriter(new BufferedWriter
            		(new OutputStreamWriter(mOutputStream, 
            				Charset.forName(StandardCharsets.UTF_8.name()))));
        	
    		printWriter.write(msg+"\n");
    		printWriter.flush();
    		
    		log( "Me : " + msg );
    	}
	}
    
    
    private static void log(String msg) {  
    	
        System.out.println("["+(new Date()) + "] " + msg);  
    }
        
}  