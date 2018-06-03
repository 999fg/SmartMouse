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

    private double d11 = 0.0;
    private double d12 = 0.0;
    private double[] d01 = new double[961];
    private double[] d02 = new double[961];

    private double[] x0 = new double [961];
    private double[] y0 = new double [961];

    private int aliveParticles = 961;

    private double x0_real = 0.16;
    private double y0_real = 0.16 * -1;

    private boolean[] alive = new boolean [961];

    private double d = 0.32;
    private long dt = 0;
    private long last_timestamp = 0;
    private long curr_timestamp = 0;
    private int last_frequency_shift1 = 0;
    private int curr_frequency_shift1 = 0;
	private int last_frequency_shift2 = 0;
	private int curr_frequency_shift2 = 0;
	private int frequency_normalizer_count = 0;
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

    	for (int i = 1 ; i <= 31; i ++) {
    		for (int j = 1 ; j <= 31; j ++) {
    			x0[(i-1) * 31 + (j-1)] = (double) i / 100;
    			y0[(i-1) * 31 + (j-1)] = (double) j / 100 * -1;
    			d01[(i-1) * 31 + (j-1)] = Math.sqrt(x0[(i-1) * 31 + (j-1)] * x0[(i-1) * 31 + (j-1)] + y0[(i-1) * 31 + (j-1)] * y0[(i-1) * 31 + (j-1)]);
				d02[(i-1) * 31 + (j-1)] = Math.sqrt((d - x0[(i-1) * 31 + (j-1)]) * (d - x0[(i-1) * 31 + (j-1)]) + y0[(i-1) * 31 + (j-1)] * y0[(i-1) * 31 + (j-1)]);
				alive[(i-1) * 31 + (j-1)] = true;
				System.out.println("d01: " + d01[(i-1)*31+(j-1)]+" d02: " + d02[(i-1)*31+(j-1)]);
			}
		}
		robot.mouseMove(960, 540);
		System.out.println((x0_real / 0.32 * 1920) + " " + (y0_real / 0.32 * 1080 * -1));
		System.out.println((int) (x0_real / 0.32 * 1920) + " " + (int) (y0_real / 0.32 * 1080 * -1));
		//.mouseMove((int) (x0_real / 0.32 * 1920), (int) (y0_real / 0.32 * 1080 * -1));
		System.out.println(MouseInfo.getPointerInfo().getLocation().getX() + " " + MouseInfo.getPointerInfo().getLocation().getY());

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

		private byte[] input_1 = new byte[882];
        private byte[] input_2 = new byte[882];
		private byte[] input_3 = new byte[882];
		private byte[] input_4 = new byte[882];
		private short[] input_short_1 = new short[441];
		private short[] input_short_2 = new short[441];
		private short[] input_short_3 = new short[441];
		private short[] input_short_4 = new short[441];

        private short[] input_short = new short[44100];



        
        
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

			try {

				
	    		boolean isDisconnected = false;
	    		
				Sender("You have accessed the server.");
	    		
				while(true){

					log("ready");

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
						Thread dataHandlingThread = new Thread(new Runnable() {
							public void run() {
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
				last_frequency_shift1 = 19000 - maxIndex1;
				last_frequency_shift2 = 20500 - maxIndex2;
				initialized = true;
			} else {
				curr_frequency_shift1 = 19000 - maxIndex1;
				curr_frequency_shift2 = 20500 - maxIndex2;
				curr_timestamp = new Date().getTime();
				dt = curr_timestamp - last_timestamp;
				last_timestamp = curr_timestamp;
				//System.out.println(maxIndex1 +" " +  maxIndex2);
				//if (Math.abs(lastFrequency1 - maxIndex1) >= 10 || Math.abs(lastFrequency2 - maxIndex2) >= 10) {
				//if (Math.abs(curr_frequency_shift1 - last_frequency_shift1) > 10 || Math.abs(curr_frequency_shift2 - last_frequency_shift2) > 10 || Math.abs(19000 - maxIndex1) <= 5 || Math.abs(20500 - maxIndex2) <= 5) {
				if (Math.abs(19000 - maxIndex1) <= 5 || Math.abs(20500 - maxIndex2) <= 5) {
					if (last_frequency_shift1 > 15 || curr_frequency_shift2 > 15) {
						frequency_normalizer_count++;
					}
					if (frequency_normalizer_count > 20) {
						last_frequency_shift2 = 20500;
						last_frequency_shift1 = 19000;
					}
					/*
					else {
						last_frequency_shift1 = curr_frequency_shift1;
						last_frequency_shift2 = curr_frequency_shift2;
					}
					*/
					return;
				} else {
					last_frequency_shift1 = curr_frequency_shift1;
					last_frequency_shift2 = curr_frequency_shift2;
					double deltaX = 0;
					double deltaY = 0;
					for (int i = 0; i < 961; i ++) {
						if(alive[i]) {
							d11 = d01[i] + (19000 - maxIndex1) * 346.6 / 19000 * dt / 1000;
							d12 = d02[i] + (20500 - maxIndex2) * 346.6 / 20500 * dt / 1000;

							if (d > d11 + d12 || d11 > d + d12 || d12 > d + d11 || d11 <= 0 || d12 <= 0) {
								alive[i] = false;
								aliveParticles--;
								continue;
							}

							if ((d11 * d11 + d * d - d12 * d12) / (2 * d * d11) > 1) {
								System.out.println("d11: " + d11 + " d12: " + d12 + " d11+d12: " + (d11+d12));
							}

							double theta = Math.acos((d11 * d11 + d * d - d12 * d12) / (2 * d * d11));

							double x1 = d11 * Math.cos(theta);
							double y1 = -1 * d11 * Math.sin(theta);

							deltaX += (x1 - x0[i]);
							deltaY += (y1 - y0[i]);
							if (Double.isNaN(deltaX)) {
								if (Double.isNaN(theta)) {
									System.out.println("THETA IS NAN!!! :" + (d11 * d11 + d * d - d12 * d12) / (2 * d * d11));
								}
							}
							//System.out.println ("deltaX: " + deltaX + " deltaY " + deltaY);
							x0[i] = x1;
							y0[i] = y1;
							d01[i] = d11;
							d02[i] = d12;
						}
					}
					if (aliveParticles == 0) {
						for (int i = 1 ; i <= 31; i ++) {
							for (int j = 1 ; j <= 31; j ++) {
								x0[(i-1) * 31 + (j-1)] = (double) i / 100;
								y0[(i-1) * 31 + (j-1)] = (double) j / 100 * -1;
								d01[(i-1) * 31 + (j-1)] = Math.sqrt(x0[(i-1) * 31 + (j-1)] * x0[(i-1) * 31 + (j-1)] + y0[(i-1) * 31 + (j-1)] * y0[(i-1) * 31 + (j-1)]);
								d02[(i-1) * 31 + (j-1)] = Math.sqrt((d - x0[(i-1) * 31 + (j-1)]) * (d - x0[(i-1) * 31 + (j-1)]) + y0[(i-1) * 31 + (j-1)] * y0[(i-1) * 31 + (j-1)]);
								alive[(i-1) * 31 + (j-1)] = true;
								System.out.println("d01: " + d01[(i-1)*31+(j-1)]+" d02: " + d02[(i-1)*31+(j-1)]);
							}
						}
						aliveParticles = 961;
						x0_real = 0.16;
						y0_real = -1 * 0.16;
						robot.mouseMove((int) (x0_real / 0.32 * 1920), (int) (y0_real / 0.32 * 1080 * -1));
						return;
					}
					System.out.println ("aliveParticles:" + aliveParticles);
					deltaX = deltaX / aliveParticles;
					deltaY = deltaY / aliveParticles;
					System.out.println ("deltaX: " + deltaX + " deltaY " + deltaY);

					//robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX() + deltaX * 5000), (int) (MouseInfo.getPointerInfo().getLocation().getY() + deltaY * 5000 * -1));
					x0_real += deltaX;
					y0_real += deltaY;
					robot.mouseMove((int) (x0_real / 0.32 * 1920), (int) (y0_real / 0.32 * 1080 * -1));
					System.out.println(x0_real + " " + y0_real);

					/*
					d11 = d01 + (19000 - maxIndex1) * 346.6 / 19000 * dt / 1000;
					d12 = d02 + (20500 - maxIndex2) * 346.6 / 20500 * dt / 1000;

					double theta = Math.acos((d11 * d11 + d * d - d12 * d12) / (2 * d * d11));

					double x1 = d11 * Math.cos(theta);
					double y1 = -1 * d11 * Math.sin(theta);
					System.out.println("d11 " + d11 + " d12 " + d12 + "theta_equation" + ((d11 * d11 + d * d - d12 * d12) / (2 * d * d11)) + " theta "+ theta);

					robot.mouseMove((int) (MouseInfo.getPointerInfo().getLocation().getX() + (x1 - x0) * 1000), (int) (MouseInfo.getPointerInfo().getLocation().getY() + (y1 - y0) * 1000 * -1));
					x0 = x1;
					y0 = y1;
					d01 = d11;
					d02 = d12;
					*/

				}
			}
		}
	}


    private static void log(String msg) {
        System.out.println("["+(new Date()) + "] " + msg);  
    }
        
}