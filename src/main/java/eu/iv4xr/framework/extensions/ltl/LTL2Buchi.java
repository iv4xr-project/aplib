package eu.iv4xr.framework.extensions.ltl;

/**
 * For translating an LTL formula to a Buchi automaton. For now, we
 * exclude the following patterns:
 * 
 *    <ol>
 *    <li> The translator does not handle conjunctions "phi && psi";
 *    <li> also does not handle left-recursive until and weak-until.
 *         E.g. "(p U q) U r" cannot be handled.
 *    </ol>     
 * 
 * Before translating, the input LTL formula is first normalized using
 * the following rewrites to push negation inside:
 * 
 *    <ol>
 *    <li> "not X phi" to "X not phi"
 *    <li> "not(phi && psi)" to "(not phi) || (not psi)"  
 *    <li> "not(phi || psi)" to "(not phi) && (not psi)"  
 *    <li> "not(phi U psi) to "(phi && not psi) W (not phi && not psi)"
 *    <li> "not(phi W psi) to "(phi && not psi) U (not phi && not psi)"  
 *    </ol>
 * 
 * After the normalization, a Buchi automaton is constructed recursively.
 * The key recursions are these:
 * 
 *    <ol>
 *    <li> X phi where phi is non-atom LTL formula. Let B is the Buchi of
 *    phi, with S1 as the initial state. Create a new initial state S0,
 *    and add (S0,true,S1) as a new transition.
 *    
 *    <li> p U psi, where p is a state-predicate (atom). Let B is the Buchi of 
 *    psi. Let S1 be the initial state of B, Create a new initial state 
 *    S0, with (S0,p,S0) as a transition. For each out-going transitions
 *    of S1: (S1,q,T), add a new transition (S0,q,T).
 *     
 *    <li> phi U psi, where phi is not a state-predicate: unimplemented.
 *    
 *    <li> p W psi, where p is a state-predicate (atom). Let B is the Buchi of 
 *    psi. Let S1 be the initial state of B, Create a new initial state 
 *    S0, with (S0,p,S0) as a transition. 
 *    We also add S0 as an omega-accepting state.
 *    For each out-going transition of S1: (S1,q,T), add a new transition (S0,q,T). 
 *    
 *    <li> phi W psi, where phi is not a state-predicate: unimplemented.
 *    
 *    <li> phi || psi. Let B and C be the Buchis of phi and psi, respectively,
 *    and S1 and S2 their initial states. We make S1 as the initial state of
 *    the combined Buchi. We remove S2, and change every transition that goes
 *    from or to S2 to go from/to S1.
 *    
 *    <li> phi && psi. Not implemented. TODO.
 *    
 *    </ol>
 * 
 * 
 * @author Wish
 *
 */
public class LTL2Buchi {

}
