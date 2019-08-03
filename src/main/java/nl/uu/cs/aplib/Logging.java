package nl.uu.cs.aplib;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import nl.uu.cs.aplib.MainConcepts.BasicAgent;

/**
 * Define aplib logging.
 */
public class Logging {
	
	public static final String APLIBLOGGER = "APLIBlogger" ;
	
	/**
	 * Get the logger instance used by aplib.
	 */
	static public Logger getAPLIBlogger() {
		return Logger.getLogger(APLIBLOGGER) ;
	}
	
	/**
	 * Attach {@code System.err} to listed to this agent's logger.
	 */
	static public void addSystemErrAsLogger() {
		Logger logger = getAPLIBlogger()  ;
		logger.addHandler(new ConsoleHandler());
	}
	
	/**
	 * Attach a file specified by the filename to receive messages sent to aplib's
	 * logger. The filename can include a path to the file.
	 */
	static public void attachLogFile(String filename) {
		Logger logger = getAPLIBlogger()  ;
		try {
			var fh = new FileHandler(filename);  
	        logger.addHandler(fh);
	        var formatter = new SimpleFormatter();  
	        fh.setFormatter(formatter);  
		}
		catch(Exception e) { 
			// swallow exception....
		}
	}
	
	/**
	 * To set the the logging level of aplib logger. Logging messages of lower level
	 * will then be ignored.
	 */
	static public void setLoggingLevel(Level level) {
		Logger logger = getAPLIBlogger()  ;
		logger.setLevel(level);
	}

}
