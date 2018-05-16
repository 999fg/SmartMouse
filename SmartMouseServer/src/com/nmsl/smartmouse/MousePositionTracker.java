package com.nmsl.smartmouse;

import java.awt.MouseInfo;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MousePositionTracker {
	public static void main(String args[]) {
		FileOutputStream output = null;
		try {
			output = new FileOutputStream("C:/Users/Jaemin Shin/Documents/out.csv");
		} catch (FileNotFoundException e){
			e.printStackTrace();
		}
		while (true) {
			System.out.println(MouseInfo.getPointerInfo().getLocation());
			System.out.println(new Date().getTime());
			try {
				output.write((MouseInfo.getPointerInfo().getLocation().getX() + "," + MouseInfo.getPointerInfo().getLocation().getY() + "," + new Date().getTime()+"\n").getBytes());
			} catch (IOException e) {
				e.printStackTrace();
			}
			try {
				Thread.sleep(1);
			} catch (InterruptedException e){
				e.printStackTrace();
			}
		}
	}
}
