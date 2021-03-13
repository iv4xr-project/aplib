package nl.uu.cs.aplib.environments;

import java.util.Scanner;

import nl.uu.cs.aplib.mainConcepts.Environment;
import nl.uu.cs.aplib.mainConcepts.Environment.EnvOperation;

/**
 * A simple implementation of {@link nl.uu.cs.aplib.mainConcepts.Environment}
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

    @Override
    protected String sendCommand_(EnvOperation cmd) {
        switch (cmd.command) {
        case "println":
            System.out.println((String) cmd.arg);
            return null;
        case "readln":
            String o = consoleInput.nextLine();
            return o;
        case "ask":
            System.out.println((String) cmd.arg);
            o = consoleInput.nextLine();
            return o;
        }
        throw new IllegalArgumentException();
    }

    /**
     * Write a string to the console.
     */
    public void println(String str) {
        sendCommand("ANONYMOUS", null, "println", str);
    }

    /**
     * Read a line from the console.
     */
    public String readln() {
        var o = sendCommand("ANONYMOUS", null, "readln", null);
        return (String) o;
    }

    /**
     * Write the string s to the console (e.g. it could formulate a question), and
     * then read a line from the console.
     */
    public String ask(String s) {
        var o = sendCommand("ANONYMOUS", null, "ask", s);
        return (String) o;
    }

}
