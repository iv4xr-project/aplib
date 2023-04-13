package nl.uu.cs.aplib.environments;

import nl.uu.cs.aplib.mainConcepts.Environment;

/**
 * An Environment that does not interact with any real environment. So,
 * sending a command to this, or asking observation, will do nothing
 * and will simply return a null. 
 * This class is mainly used for testing agents' features that does not
 * need a real environment to be tested.
 */
public class NullEnvironment extends Environment {
	
	public NullEnvironment() {
		super() ;
	}
	
	@Override
	protected Object sendCommand_(EnvOperation commad) {
		// do and return nothing, since this environment does not interact with
		// any real environment
		return null ;
	}
	
	@Override
	public Object observe(String agentId) {
		// return nothing, since this environment does not interact with
		// any real environment
		return null ;
	}

}
