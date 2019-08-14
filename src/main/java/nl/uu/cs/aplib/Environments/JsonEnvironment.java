package nl.uu.cs.aplib.Environments;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nl.uu.cs.aplib.MainConcepts.Environment;
import nl.uu.cs.aplib.MainConcepts.Environment.EnvOperation;

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
	 * Send the specified command to the environment. This method will translate
	 * {@code arg} to JSON before sending it to the real environment. If the
	 * real environment replies with some value/object, this method expects this
	 * reply to be in JSON as well, which it then converts to an ordinary Java
	 * object.
	 * 
	 * @param invokerId A unique ID identifying the invoker of this method, e.g. in
	 *                  the format agentid.actionid.
	 * @param targetId  The unique ID of the object in the real environment to which
	 *                  the command is directed.
	 * @param command   The name of the command.
	 * @param arg       The arguments to be passed along with the command.
	 * @return an object returned by the real environment as the result of the command, if any.
	 * 
	 * <p>The method may also throws a runtime exception.
	 */
	public Object sendCommand(
	         String invokerId,
	         String targetId,
	         String command,
	         Object arg,
	         Class classOfReturnObject
	         ) {
		String result = (String) sendCommand(invokerId,targetId,command,gson.toJson(arg)) ;
		if (classOfReturnObject == null) return null ;
		return gson.fromJson(result, classOfReturnObject) ;
	}

}
