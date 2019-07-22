package nl.uu.cs.aplib.MainConcepts;

/**
 * Representing the agent's state (also called 'belief' in BDI). This has two main
 * elements:
 *   (1) the field env represents the current state of the agent's environment
 *   (2) information that the agent itself adds as a result of its own inference,
 *       if any. In this template, there is none. A concrete implementation of this
 *       class add whatever additional information it wishes.

 * @author wish
 *
 */
public abstract class SimpleState {
	
	Environment env ;
	
	public void setEnvironment(Environment env) { this.env = env ; }
	
	public void upateState() {
		 env.refresh() ;
	};

}
