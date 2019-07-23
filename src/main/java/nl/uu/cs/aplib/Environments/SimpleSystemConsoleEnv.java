package nl.uu.cs.aplib.Environments;

import java.util.Scanner;

import nl.uu.cs.aplib.MainConcepts.Environment;

public class SimpleSystemConsoleEnv extends Environment {
	
	
	Scanner consoleInput = new Scanner(System.in);

	@Override
	public Object sendCommand(String id, String command, Object arg, Class classOfReturnObject) {
		if (command.equals("println")) {
			System.out.println((String) arg) ; return null ;
		}
		if (command.equals("readln")) {
			String inp = consoleInput.nextLine() ;
			//System.err.println(">>> " + inp) ;
			return inp ;
		}
		throw new IllegalArgumentException("Command " + command + "is not recognized.") ;
	}
	
	public void println(String str) {
		sendCommand(null,"println",">> " + str,null) ;
	}
	
	public String readln() {
		return (String) sendCommand(null,"readln",null,null) ;
	}
	
	
	
	public static void main(String[] args) {
		// quick test...
		var env = new SimpleSystemConsoleEnv() ;
		env.sendCommand(null,"println", "Hahaha",null) ;
		env.sendCommand(null,"readln",null,null) ;
		env.sendCommand(null,"println", "ok!",null) ;	
	}

}
