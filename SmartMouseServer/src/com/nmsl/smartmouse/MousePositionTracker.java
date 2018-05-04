package com.nmsl.smartmouse;

import java.awt.MouseInfo;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

public class MousePositionTracker {
	public static void main(String args[]) {
		Date date;
		while (true) {
			System.out.println(MouseInfo.getPointerInfo().getLocation());
			date = new Date();
	        System.out.println(new Timestamp(date.getTime()));
		}
	}
}
