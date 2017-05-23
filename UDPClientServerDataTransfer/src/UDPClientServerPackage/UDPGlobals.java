package UDPClientServerPackage;

import java.util.Random;

public class UDPGlobals {
	public static byte[] dummyByteArray = new byte[1000];
	public static Random randomObject = new Random();
	
	public static void displayMessage(Object aMessage) {
		System.out.println(aMessage);
	}
	
}
