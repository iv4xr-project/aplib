package nl.uu.cs.aplib.Environments;

import java.util.Scanner;

import nl.uu.cs.aplib.MainConcepts.Environment;

/**
 * A simple implementation of {@link nl.uu.cs.aplib.MainConcepts.Environment}
 * that offers methods to read from and write to the console. Of course an agent
 * can just read or write to the console without the help of this Environment,
 * but the purpose of this class is to provide a simple example of implementing
 * an Environment.
 * 
 * @author wish
 *
 */
public class ConsoleEnvironment extends Environment {
	
	
	Scanner consoleInput = new Scanner(System.in);

	/**
	 * Write a string to the console.
	 */
	public void println(String str) {
		System.out.println(str) ;
	}
	
	/**
	 * Read a line from the console.
	 */
	public String readln() {
		return  consoleInput.nextLine() ;
	}
	
	/**
	 * Write the string s to the console (e.g. it could formulate a question), and
	 * then read a line from the console.
	 */
	public String ask(String s) {
		println(s) ; return readln() ;
	}
	
}
