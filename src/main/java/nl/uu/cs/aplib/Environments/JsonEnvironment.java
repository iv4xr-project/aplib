package nl.uu.cs.aplib.Environments;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nl.uu.cs.aplib.MainConcepts.Environment;

/**
 * A root class of an Environment that communicates with the real environment by
 * exchanging JSON objects. This class is intended to be subclassed by a real
 * implementation. At least you must provide an implementation of (you must
 * override) the method
 * {@link nl.uu.cs.aplib.MainConcepts.Environment#sendCommand_(String,String,String)}
 * of the class {@link nl.uu.cs.aplib.MainConcepts.Environment}. The 2nd String
 * is the name of the command for the real environment, and the 3rd String is an
 * argument for the command. This class JsonEnvironment will allow you to send
 * an Object as an argument. This will be serialized to a string in the JSON
 * format, and will be the string used as the 3rd String in
 * {@code sendCommand_}. Also, this class assume that {@code sendCommand_}
 * returns a string in the JSON format as well, which it then deserialize to
 * construct the Object it represents.
 * 
 * @author Wish
 */
public class JsonEnvironment extends Environment {
	
	Gson gson = new GsonBuilder().serializeNulls().create();
	
	/**
	 * Send a command to the actual environment.
	 * 
	 * @param id  The id of the entity in the actual environment to which this command is directed.
	 * @param command The name of the command itself.
	 * @param arg The argument of the command.
	 * @param classOfReturnObject The class of the return value of the command.
	 * @return The value returned by the command.
	 */
	public Object sendCommand(String id, String command, Object arg, Class classOfReturnObject) {
		String result = sendCommand_(id,command,gson.toJson(arg)) ;
		if (classOfReturnObject == null) return null ;
		return gson.fromJson(result, classOfReturnObject) ;
	}

}
