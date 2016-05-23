package com.accenture.nao.cv;

import org.opencv.core.Core;

public class MatchContourApp {

	public static void main(String[] s) {
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		System.out.println("\nFind tic-tac-toe board position");

		MatchContour mc = new MatchContour();
		mc.process("c:\\temp\\inputs\\scene.jpg", "c:\\temp\\output\\out.png");
	}
}