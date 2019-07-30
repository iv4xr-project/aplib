package nl.uu.cs.aplib.MainConcepts;

/**
 * 
 * In aplib, agents are used to interact and control an environment. This class
 * is a root class for representing this environment. This class is meant to be
 * <b>extended/subclassed</b>. For a minimalistic example implementation, see
 * the class {@link nl.uu.cs.aplib.Environments.ConsoleEnvironment}.
 * 
 * <p>
 * The idea is that this class, or rather, your subclass of this class, should
 * provide snapshot information on the current state of whatever actual
 * information that your agents try to control. You do not have to provide all
 * information. Instead, you only need to provide information that is relevant
 * for the logic of your agents. You also need to provide methods to allow your
 * agents to send commands to the actual environment. This class suggests one
 * method called {@link sendCommand_} that you can use as the primitive to
 * implement your own set of methods to command the actual environment.
 * Implementing {@link sendCommand_} is not mandatory; it is only a suggestion.
 * Any set of methods are fine, as long as your agents can access them.
 * 
 * @author wish
 *
 */
public class Environment {
	
		
	public Environment() { }
	
	/**
	 * Inspect the actual environment and reflects its actual state into this
	 * abstract representation.
	 */
	public void refresh() { }
	
	/**
	 * This will reset the actual environment. By reset we mean to put it back in
	 * some initial state. In reality this may involve re-deploying the environment.
	 * It is your responsibility to make sure that after calling this method the
	 * environment is indeed available and is in a legal initial state.
	 */
	public void restart() {  }
	
	
	/**
	 * Send the specified command to the environment. This method also anticipate
	 * that the environment is populated by multiple reactive entities, so the
	 * command includes an ID if addressing a specific entity in the environment is
	 * needed.
	 * 
	 * <p>
	 * Implementing this method is not mandatory. However, implementing it allows
	 * you to build various commands around it that your agents can use.
	 * 
	 * @param id      The unique ID of the object to which the command is directed.
	 * @param command The name of the command.
	 * @param arg     The arguments to be passed along with the command.
	 * @return true if the command is successfully executed; false if the
	 *         environment rejects it.
	 * 
	 *         The method may also throws a runtime exception.
	 */
	protected String sendCommand_(String id, String command, String arg) {
		throw new UnsupportedOperationException() ;
	}
		

}
