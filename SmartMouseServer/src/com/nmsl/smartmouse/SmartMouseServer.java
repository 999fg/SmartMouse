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
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
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
    private int parityCheck = 1;
   
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
			
								
	        new Receiver(mStreamConnection).start();
        }
		
	}
	
        
    
    class Receiver extends Thread {
    	
    	private InputStream mInputStream = null; 
        private OutputStream mOutputStream = null; 
        private String mRemoteDeviceString = null;
        private StreamConnection mStreamConnection = null;
		//private byte[] input = new byte[44100];
		//private byte[] input = new byte[1764];
		//private byte[] input_1 = new byte[882];
        //private byte[] input_2 = new byte[882];
		//private byte[] input = new byte[4048];
		private byte[] input_1 = new byte[1012];
		private byte[] input_2 = new byte[1012];
		private byte[] input_3 = new byte[1012];
		private byte[] input_4 = new byte[1012];
		private byte[] input_5 = new byte[1012];
		private byte[] input_6 = new byte[1012];
		private byte[] input_7 = new byte[1012];
		private byte[] input_8 = new byte[1012];
        //private short[] input_short = new short[32768];

        //private double[] FFTResult;
        private int indexCounter = 0;

        
        
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
				
	    		//Reader mReader = new BufferedReader(new InputStreamReader( mInputStream, Charset.forName(StandardCharsets.UTF_8.name())));
				
	    		boolean isDisconnected = false;
	    		
				Sender("You have accessed the server.");
	    		
				while(true){

					log("ready");
	
			        
		            //StringBuilder stringBuilder = new StringBuilder();
		            //int readCount = 0;
		            int c = 0;
		            double max = 0;
		            int maxIndex = 0;


					while ((c = mInputStream.read(input_1)) != -1) {
						if ((c = mInputStream.read(input_2)) == -1) {
							break;
						}
						if ((c = mInputStream.read(input_3)) == -1) {
							break;
						}
						if ((c = mInputStream.read(input_4)) == -1) {
							break;
						}
						/*
						if ((c = mInputStream.read(input_5)) == -1) {
							break;
						}
						if ((c = mInputStream.read(input_6)) == -1) {
							break;
						}
						if ((c = mInputStream.read(input_7)) == -1) {
							break;
						}
						if ((c = mInputStream.read(input_8)) == -1) {
							break;
						}
						*/
						//System.arraycopy(input_1, 0, input, 0, 1012);
						//System.arraycopy(input_2, 0, input, 1012, 1012);
						//System.arraycopy(input_1, 0, input, 0, 512);
						//System.arraycopy(input_2, 0, input, 512, 512);
						Thread dataHandlingThread = new Thread(new Runnable() {
							public void run() {
								//dataHandle(input_1, input_2, input_3, input_4, input_5, input_6, input_7, input_8);
								dataHandle(input_1, input_2, input_3, input_4);
							}
						});
						dataHandlingThread.start();
					}


					if ( c == -1){

						log("Client has been disconnected");

						count--;
						log("Current number of clients: " + count);

						isDisconnected = true;
						Thread.currentThread().interrupt();

						break;
					}
	
		            if ( isDisconnected ) break;
		            
		            //String recvMessage = stringBuilder.toString();
			        //log( mRemoteDeviceString + ": " + recvMessage );
					//robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX() + Double.parseDouble(recvMessage.split(",")[0]) / 1 ), (int) (MouseInfo.getPointerInfo().getLocation().getY()));
					//robot.mouseMove((int) (Double.parseDouble(recvMessage.split(",")[0]) / 10 ), (int) (MouseInfo.getPointerInfo().getLocation().getY()));

			        /*
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
			        */
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

    	void dataHandle(byte[] input1, byte[] input2, byte[] input3, byte[] input4) {
			//float[] input_float = new float[input.length/4];
			short[] input_short_1 = new short[506];
			short[] input_short_2 = new short[506];
			short[] input_short_3 = new short[506];
			short[] input_short_4 = new short[506];
			//short[] input_short_5 = new short[506];
			//short[] input_short_6 = new short[506];
			//short[] input_short_7 = new short[506];
			//short[] input_short_8 = new short[506];
			short[] input_short_0 = new short[16384];
			double[] FFTResult;
			double max = 0;
			double max2 = 0;
        	int maxIndex = 0;
        	int maxIndex2 = 0;
			ByteBuffer.wrap(input1).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_1);
			ByteBuffer.wrap(input2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_2);
			ByteBuffer.wrap(input3).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_3);
			ByteBuffer.wrap(input4).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_4);
			//ByteBuffer.wrap(input5).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_5);
			//ByteBuffer.wrap(input6).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_6);
			//ByteBuffer.wrap(input7).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_7);
			//ByteBuffer.wrap(input8).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_8);
			System.arraycopy(input_short_1, 0, input_short_0, 0, 506);
			System.arraycopy(input_short_2, 0, input_short_0, 506, 506);
			System.arraycopy(input_short_3, 0, input_short_0, 1012, 506);
			System.arraycopy(input_short_4, 0, input_short_0, 1518, 506);
			//System.arraycopy(input_short_5, 0, input_short_0, 506*4, 506);
			//System.arraycopy(input_short_6, 0, input_short_0, 506*5, 506);
			//System.arraycopy(input_short_7, 0, input_short_0, 506*6, 506);
			//System.arraycopy(input_short_8, 0, input_short_0, 506*7, 506);
			//ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short);
			//ByteBuffer.wrap(input).order(ByteOrder.LITTLE_ENDIAN).asFloatBuffer().get(input_float);
			//System.out.println(input_float.length);
			/*
			for (int i = 0; i < input_float.length; i++){
				if (maxValue < input_float[i])
					maxValue = input_float[i];
			}
			*/
			//System.out.println(maxValue);
			FFTResult = new FFT(input_short_0.length).getFreqSpectrumFromShort(input_short_0);
			//FFTResult = new FFT(input.length/4).getFreqSpectrumFromFloat(input_float);

			//for (int j = 28185; j < 28285; j++){
			//for (int j = 14067; j < 14167; j++){
			for (int j = 7000; j < 7100; j++){
					if (FFTResult[j] > max) {
					max = FFTResult[j];
					maxIndex = j;
				}
			}
/*
			for (int j = 31157; j < 31257; j++) {
			//for (int j = 12581; j < 12681; j++) {
				if (FFTResult[j] > max2) {
					max2 = FFTResult[j];
					maxIndex2 = j;
				}
			}
*/
//			System.out.println(maxIndex + "  " + maxIndex2);
			System.out.println(maxIndex);
		}
	}
    
    
    private static void log(String msg) {  

        System.out.println("["+(new Date()) + "] " + msg);  
    }
        
}