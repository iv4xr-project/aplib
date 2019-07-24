package nl.uu.cs.aplib.Environments;

import java.util.Scanner;

import nl.uu.cs.aplib.MainConcepts.Environment;

public class ConsoleEnvironment extends Environment {
	
	
	Scanner consoleInput = new Scanner(System.in);

	public void println(String str) {
		System.out.println(str) ;
	}
	
	public String readln() {
		return  consoleInput.nextLine() ;
	}
	
	public String ask(String s) {
		println(s) ; return readln() ;
	}
	
}
