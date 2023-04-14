package nl.uu.cs.aplib.environments;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nl.uu.cs.aplib.mainConcepts.Environment;

/**
 * A root class of an Environment that communicates with the real environment by
 * exchanging JSON objects. This class is intended to be subclassed by a real
 * implementation. At least you must provide an implementation of (you must
 * override) the methods:
 * 
 * <ol>
 * <li>
 * {@link nl.uu.cs.aplib.mainConcepts.Environment#sendCommand_(EnvOperation)}
 * This method should serialize the given command to a string in the JSON format
 * and send it to the real environment. If the environment replies, this is
 * expected to be as a string in the JSON format as well, which sendCommand_
 * will then return.
 * 
 * <p>
 * Agents should use the wrapper method sendCommand(...) instead, which
 * internally will call sendCommand_, and parse the returned JSON string into a
 * Java object.
 * 
 * <li>{@link nl.uu.cs.aplib.mainConcepts.Environment#observe(String)} to ask
 * the real environment to send back an observation from the perspective of a
 * given agent.
 * 
 * </ol>
 * 
 * 
 * @author Wish
 */
public class JsonEnvironment extends Environment {

    /**
     * Json builder and parser. We use this to serialize an object to a Json string,
     * and conversely to parse a string in the Json format to a Java object.
     */
    Gson gson;

    /**
     * A constructor that will initialize the internal Json builder/parser to some
     * simple setup.
     */
    public JsonEnvironment() {
        gson = new GsonBuilder().serializeNulls().create();
    }

    /**
     * A constructor that will use the given Json builder/parser as its own
     * builder/parser.
     */
    public JsonEnvironment(Gson jsonBuilderParser) {
        gson = jsonBuilderParser;
    }

    /**
     * This method will invoke the underlying sendCommand_, that in turn is expected
     * to send the command as a string in the Json format to the real environment.
     * If the command is supposed to return anything, sendCommand_ will return this
     * as a string in the Json format as well. If the parameter expectedTypeOfResult
     * is specified, the returned string will be parsed into an instance of the
     * specified type. If the parameter expectedTypeOfResult is left null, this
     * method won't know to what the Json string should be parsed to, so it will
     * simply return null.
     */
    @Override
    public Object sendCommand(String invokerId, String targetId, String command, Object arg,
            Class expectedTypeOfResult) {
        String result = (String) super.sendCommand(invokerId, targetId, command, arg, String.class);
        if (expectedTypeOfResult == null)
            return null;
        return gson.fromJson(result, expectedTypeOfResult);
    }

    /**
     * Override this method. The method should send the command to the real
     * environment in the Json format. If the command is supposed to return some
     * infomation back, this method is supposed to return this information as a
     * string in Json format. The wrapper method sendCommand will then parse this
     * Json string to an object with the corresponding structure.
     */
    @Override
    protected Object sendCommand_(EnvOperation cmd) {
        throw new UnsupportedOperationException();
    }

}
