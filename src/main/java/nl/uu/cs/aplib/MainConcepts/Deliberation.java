package nl.uu.cs.aplib.MainConcepts;

import java.util.List;
import java.util.Random;

import nl.uu.cs.aplib.MainConcepts.Strategy.PrimitiveStrategy;

/**
 * This class implements a deliberation process for agents. Agent deliberation
 * in aplib is defined as the process of making a choice of which action an
 * agent should execute, if there are multiple actions enabled on the agent's
 * current state. The agent will then invoke the method
 * {@link #deliberate(SimpleState, List)} of this class, passing to it the set of currently
 * enabled actions. The method will the make the choice. This root
 * implementation will just choose one randomly. You need to write your own
 * subclass if you want to have a more sophisticated deliberation process.
 * 
 * @author wish
 *
 */
public class Deliberation {
	
	protected long rndseed = 1287821 ; // a prime and a palindrome :D
	protected Random rnd = new Random(rndseed) ;
	
	public Deliberation() { }
	
	public PrimitiveStrategy deliberate(SimpleState currentstate, List<PrimitiveStrategy> candidates) {
		return candidates.get(rnd.nextInt(candidates.size())) ;
	}

}
