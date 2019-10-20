package nl.uu.cs.aplib;

import java.util.logging.ConsoleHandler;
import java.util.logging.FileHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import nl.uu.cs.aplib.MainConcepts.BasicAgent;

/**
 * Define aplib logging. Use {@link #getAPLIBlogger()} to get aplib's logger.
 * This is a single logger accessible from anywhere. Use
 * {@link #setLoggingLevel(Level)} to globally set the logging level of this
 * logger. Use e.g. {@link #addSystemErrAsLogHandler()} or
 * {@link #attachFileAsLogHandler(String)} to attach listeners to this logger. Messages
 * sent to the logger will be echoed to its listerners.
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
	 * Attach {@code System.err} to listen to this agent's logger (also called log-handler).
	 */
	static public void addSystemErrAsLogHandler() {
		Logger logger = getAPLIBlogger()  ;
		logger.addHandler(new ConsoleHandler());
	}
	
	/**
	 * Remove all current handlers of the aplib logger.
	 */
	static public void clearLoggers() {
		Logger logger = getAPLIBlogger()  ;
		Handler[] handlers = logger.getHandlers() ;
		for (Handler h : handlers) logger.removeHandler(h);
	}
	
	/**
	 * Attach a file specified by the filename to receive messages sent to aplib's
	 * logger (in Logging jargon, the file becomes a log handler).
	 * The filename can include a path to the file.
	 */
	static public void attachFileAsLogHandler(String filename) {
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
