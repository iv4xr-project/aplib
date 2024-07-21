package nl.uu.cs.aplib.utils;

import java.io.IOException;
import java.util.Scanner;

public class ConsoleUtils {
	
	public static void println(boolean yesPrintThem, String ... args){
		if (! yesPrintThem) return ;
		for (int k=0; k<args.length; k++) {
			System.out.println(args[k]) ;
		}
	}
	
	public static void hitAKeyToContinue(boolean interactive) throws IOException {
		if (! interactive) return ;
		System.out.println(">>>> hit a key to continue...") ;
		//Scanner scanner = new Scanner(System.in);
		//scanner.nextLine() ;
		//scanner.close();
		System.in.read() ;
	}
	

}
