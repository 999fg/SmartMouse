package com.nmsl.smartmouse;

import java.awt.*;

public class test {

	public static void main(String args[]) {
		double a = 370392302;
		System.out.println(a / 1000 );
		Robot robot = null;
		try {
			robot = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		for(int i = 0; i < 1920; i ++) {
			for(int j = 0; j < 1080; j++) {
				robot.mouseMove(i,j);
				try {
					Thread.sleep(1);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}
	}
}
