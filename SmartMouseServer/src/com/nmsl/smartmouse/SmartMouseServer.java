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

import static java.lang.Double.NaN;


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
    private int lastFrequency1 = 0;
    private int lastFrequency2 = 0;
    private double d01 = 0.25;
    private double d02 = 0.25;
    private double d11 = 0.0;
    private double d12 = 0.0;
	//private double d1 = 0.0; // second method
	//private double d2 = 0.0; // second method
	//private double deltaX = 0.0; // second method
	//private double deltaY = 0.0; // second method
    private double d = 0.32;
    private long dt = 0;
    private long last_timestamp = 0;
    private long curr_timestamp = 0;
    private double c1 = 0.018242; //speed of 19kHz
	private double c2 = 0.016907; //speed of 20.5kHz
	private double ts = 0.04; //time interval 40ms
	private double x0 = 0.2;
	private double y0 = -0.2;
	private boolean initialized = false;
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
		private byte[] input_1 = new byte[882];
        private byte[] input_2 = new byte[882];
		private byte[] input_3 = new byte[882];
		private byte[] input_4 = new byte[882];
		private short[] input_short_1 = new short[441];
		private short[] input_short_2 = new short[441];
		private short[] input_short_3 = new short[441];
		private short[] input_short_4 = new short[441];
		//private byte[] input = new byte[4048];
		/*
		private byte[] input_0 = new byte[1764];
		private byte[] input_1 = new byte[1012];
		private byte[] input_2 = new byte[1012];
		private byte[] input_3 = new byte[1012];
		private byte[] input_4 = new byte[1012];
		private byte[] input_5 = new byte[1012];
		private byte[] input_6 = new byte[1012];
		private byte[] input_7 = new byte[1012];
		private byte[] input_8 = new byte[1012];
		*/
        private short[] input_short = new short[44100];

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
						ByteBuffer.wrap(input_1).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_1);
						System.arraycopy(input_short_1, 0, input_short, 0, 441);
						if ((c = mInputStream.read(input_2)) == -1) {
							break;
						}
						ByteBuffer.wrap(input_2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_2);
						System.arraycopy(input_short_2, 0, input_short, 0, 441);
						if ((c = mInputStream.read(input_3)) == -1) {
							break;
						}
						ByteBuffer.wrap(input_3).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_3);
						System.arraycopy(input_short_3, 0, input_short, 0, 441);
						if ((c = mInputStream.read(input_4)) == -1) {
							break;
						}
						ByteBuffer.wrap(input_4).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_4);
						System.arraycopy(input_short_4, 0, input_short, 0, 441);
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
								dataHandle_short(input_short);
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

    	void dataHandle(byte[] input1, byte[] input2) {
			//float[] input_float = new float[input.length/4];
			short[] input_short_1 = new short[442];
			//short[] input_short_2 = new short[506];
			//short[] input_short_3 = new short[506];
			//short[] input_short_4 = new short[506];
			//short[] input_short_5 = new short[506];
			//short[] input_short_6 = new short[506];
			//short[] input_short_7 = new short[506];
			//short[] input_short_8 = new short[506];
			short[] input_short_0 = new short[4096];
			double[] FFTResult;
			double max = 0;
			double max2 = 0;
        	int maxIndex = 0;
        	int maxIndex2 = 0;
			ByteBuffer.wrap(input1).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_1);
			//ByteBuffer.wrap(input2).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_2);
			//ByteBuffer.wrap(input3).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_3);
			//ByteBuffer.wrap(input4).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_4);
			//ByteBuffer.wrap(input5).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_5);
			//ByteBuffer.wrap(input6).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_6);
			//ByteBuffer.wrap(input7).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_7);
			//ByteBuffer.wrap(input8).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer().get(input_short_8);
			System.arraycopy(input_short_1, 0, input_short_0, 0, 506);
			//System.arraycopy(input_short_2, 0, input_short_0, 506, 506);
			//System.arraycopy(input_short_3, 0, input_short_0, 1012, 506);
			//System.arraycopy(input_short_4, 0, input_short_0, 1518, 506);
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
			//FFTResult = new FFT(input_short_1.length).getFreqSpectrumFromShort(input_short_1);
			//FFTResult = new FFT(input.length/4).getFreqSpectrumFromFloat(input_float);

			//for (int j = 28185; j < 28285; j++){
			//for (int j = 14067; j < 14167; j++){
			for (int j = 1700; j < FFTResult.length; j++){
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

		/*
		void dataHandle_short(short[] input_s) {
			double[] FFTResult;
			double max1 = 0;
			double max2 = 0;
			int maxIndex1 = 0;
			int maxIndex2 = 0;
			double[] output = new double[input_s.length];
			for (int i = 0; i < input_s.length; i++)
				output[i] = (double) input_s[i] / Short.MAX_VALUE;
			double[] imag = new double[input_s.length];
			FFTResult = new GeneralFFT().transformBluestein(output, imag);
			//FFTResult = new FFT(input_short_1.length).getFreqSpectrumFromShort(input_short_1);
			//FFTResult = new FFT(input.length/4).getFreqSpectrumFromFloat(input_float);

			//for (int j = 28185; j < 28285; j++){
			//for (int j = 14067; j < 14167; j++){
			for (int j = 18950; j < 19050; j++){
				if (FFTResult[j] > max1) {
					max1 = FFTResult[j];
					maxIndex1 = j;
				}
			}
			for (int j = 20450; j < 20550; j++){
				if (FFTResult[j] > max2) {
					max2 = FFTResult[j];
					maxIndex2 = j;
				}
			}


			//for (int j = 31157; j < 31257; j++) {
			////for (int j = 12581; j < 12681; j++) {
			//	if (FFTResult[j] > max2) {
			//		max2 = FFTResult[j];
			//		maxIndex2 = j;
			//	}
			//}

			//System.out.println("LastFrequencies: " + lastFrequency1 + " " + lastFrequency2);
			//System.out.println("CurrFrequencies: " + maxIndex1 + "  " + maxIndex2);
			if (!initialized){
				lastFrequency1 = maxIndex1;
				lastFrequency2 = maxIndex2;
				initialized = true;
			} else {
				//if (Math.abs(lastFrequency1 - maxIndex1) >= 10 || Math.abs(lastFrequency2 - maxIndex2) >= 10) {
				if (Math.abs(19000 - maxIndex1) <= 5 || Math.abs(20500 - maxIndex2) <= 5) {
						return;
				} else {
					//d11 = d01 + (maxIndex1 - lastFrequency1) * c1 / lastFrequency1 * ts;
					//d12 = d02 + (maxIndex2 - lastFrequency2) * c2 / lastFrequency2 * ts;
					//d11 = d01 + (maxIndex1 - 19000) * c1 / 19000 * ts;
					//d12 = d02 + (maxIndex2 - 20500) * c2 / 20500 * ts;
					double theta = Math.acos((d11 * d11 + d * d - d12 * d12) / (2 * d * d11));
					//System.out.println("d11: " + d11 + " d12: "+ d12 + " theta: " + theta);
					double x1 = d11 * Math.cos(theta);
					double y1 = d11 * Math.sin(theta);
					//double x2 = d11 * Math.cos(-1 * theta);
					//double y2 = d11 * Math.sin(-1 * theta);
					//System.out.println("lastFrequency: " + lastFrequency1 + " currFrequency: " + maxIndex1 + " x1-x0: " + (x1-x0) + " y1-y0: " + (y1-y0));

					//for (int i = 0; i < 20; i ++) {
					//	//robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX() + (x1 - x0) * 200000000 * -1 / 10), (int) (MouseInfo.getPointerInfo().getLocation().getY() + (y1 - y0) * 200000000 * -1 / 10));
					//	robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX()), (int) (MouseInfo.getPointerInfo().getLocation().getY() + (y1 - y0) * 20000 * -1 / 10));
					//}

					//robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX() + (x1 - x0) * 20000000 * -1), (int) (MouseInfo.getPointerInfo().getLocation().getY() + (y1 - y0) * 20000000 * -1));
					robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX()), (int) (MouseInfo.getPointerInfo().getLocation().getY()+ (y1 - y0) * 200000000 * -1));
					x0 = x1;
					y0 = y1;
					d01 = d11;
					d02 = d12;
					lastFrequency1 = maxIndex1;
					lastFrequency2 = maxIndex2;
				}
			}
		}
		*/

		void dataHandle_short(short[] input_s) {
			double[] FFTResult;
			double max1 = 0;
			double max2 = 0;
			int maxIndex1 = 0;
			int maxIndex2 = 0;
			double[] output = new double[input_s.length];
			for (int i = 0; i < input_s.length; i++)
				output[i] = (double) input_s[i] / Short.MAX_VALUE;
			double[] imag = new double[input_s.length];
			FFTResult = new GeneralFFT().transformBluestein(output, imag);
			for (int j = 18950; j < 19050; j++){
				if (FFTResult[j] > max1) {
					max1 = FFTResult[j];
					maxIndex1 = j;
				}
			}
			for (int j = 20450; j < 20550; j++){
				if (FFTResult[j] > max2) {
					max2 = FFTResult[j];
					maxIndex2 = j;
				}
			}
			if (!initialized) {
				last_timestamp = new Date().getTime();
				initialized = true;
			} else {
				curr_timestamp = new Date().getTime();
				dt = curr_timestamp - last_timestamp;
				last_timestamp = curr_timestamp;
				//if (Math.abs(lastFrequency1 - maxIndex1) >= 10 || Math.abs(lastFrequency2 - maxIndex2) >= 10) {
				if (Math.abs(19000 - maxIndex1) >= 25 || Math.abs(20500 - maxIndex2) >= 25 || Math.abs(19000 - maxIndex1) <= 4 || Math.abs(20500 - maxIndex2) <=4) {
					return;
				} else {
					//d11 = d01 + (maxIndex1 - 19000) * c1 / 19000 * dt / 1000;
					//d12 = d02 + (maxIndex2 - 20500) * c2 / 20500 * dt / 1000;
					d11 = d01 + (19000 - maxIndex1) * 346.6 / 19000 * dt / 1000;
					d12 = d02 + (20500 - maxIndex2) * 346.6 / 20500 * dt / 1000;
					//System.out.println("d11, d01, maxIndex1, c1, dt:" + d11 + " " + d01+ " " + maxIndex1 + " " + c1 + " " + dt);
					double theta = Math.acos((d11 * d11 + d * d - d12 * d12) / (2 * d * d11));
					//System.out.println("d11: " + d11 + " d12: "+ d12 + " theta: " + theta);
					double x1 = d11 * Math.cos(theta);
					double y1 = -1 * d11 * Math.sin(theta);
					System.out.println("d11 " + d11 + " d12 " + d12 + "theta_equation" + ((d11 * d11 + d * d - d12 * d12) / (2 * d * d11)) + " theta "+ theta);
					//System.out.println("deltaX:" + (x1 - x0) + " deltaY: " + (y1 - y0) + " x1: " + x1 + " y1: " + y1 + " theta: " + theta + " abs(maxIndex1 - 19000): " + Math.abs(maxIndex1 - 19000) + " abs(maxIndex2 - 20500): " + Math.abs(maxIndex2 - 20500));
					//System.out.println("deltaX:" + (x1 - x0) + " deltaY: " + (y1 - y0) + " x1: " + x1 + " y1: " + y1 + " theta: " + theta + " abs(maxIndex1 - 19000): " + Math.abs(maxIndex1 - 19000) + " abs(maxIndex2 - 20500): " + Math.abs(maxIndex2 - 20500));
					//robot.mouseMove((int) ((x1 / d) * 1920), (int) ((y1 / 0.4) * 1080));
					robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX() + (x1 - x0) * 1000), (int) (MouseInfo.getPointerInfo().getLocation().getY() + (y1 - y0) * 1000 * -1));
					x0 = x1;
					y0 = y1;
					d01 = d11;
					d02 = d12;
					/*
					if (x0 < 0) {
						double theta1 = Math.atan(Math.abs(y0) / Math.abs(x0));
						double theta2 = Math.atan(Math.abs(y0) / (Math.abs(x0)+d));
						d1 = (maxIndex1 / 19000 - 1) * 346.6 * dt / 1000;
						d2 = (maxIndex2 / 20500 - 1) * 346.6 * dt / 1000;
						deltaX = d1 * Math.cos(theta1) + d2 * Math.cos(theta2);
						deltaY = d1 * Math.sin(theta1) + d2 * Math.sin(theta2);
					} else if (x0 > 0 && x0 < d) {
						double theta1 = Math.atan(Math.abs(y0) / Math.abs(x0));
						double theta2 = Math.atan(Math.abs(y0) / Math.abs(d-x0));
						d1 = (maxIndex1 / 19000 - 1) * 346.6 * dt / 1000;
						d2 = (maxIndex2 / 20500 - 1) * 346.6 * dt / 1000;
						deltaX = -1 * d1 * Math.cos(theta1) + d2 * Math.cos(theta2);
						deltaY = d1 * Math.sin(theta1) + d2 * Math.sin(theta2);
					} else if (x0 > d) {
						double theta1 = Math.atan(Math.abs(y0) / Math.abs(x0));
						double theta2 = Math.atan(Math.abs(y0) / Math.abs(x0 - d));
						d1 = (maxIndex1 / 19000 - 1) * 346.6 * dt / 1000;
						d2 = (maxIndex2 / 20500 - 1) * 346.6 * dt / 1000;
						deltaX = -1 * d1 * Math.cos(theta1) + -1 * d2 * Math.cos(theta2);
						deltaY = d1 * Math.sin(theta1) + d2 * Math.sin(theta2);
					}
					System.out.println("deltaX:" + deltaX + " deltaY: " + deltaY);
					//robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX() + deltaX), (int) (MouseInfo.getPointerInfo().getLocation().getY() + deltaY));
					//robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX()), (int) (MouseInfo.getPointerInfo().getLocation().getY()+ (y1 - y0) * 200000000 * -1));
					x0 = x0 + deltaX;
					y0 = y0 + deltaY;
					*/
				}
			}
		}
	}


    private static void log(String msg) {
        System.out.println("["+(new Date()) + "] " + msg);  
    }
        
}