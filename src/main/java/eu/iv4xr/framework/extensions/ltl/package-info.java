/**
 * This package provides the following features:
 * 
 *   <ol>
 *   <li> The classes {@link LTL} and {@link BoundedLTL} implementing Linear Temporal Logic
 *       formulas, and the bounded variation of them. These formulas
 *       can be evaluated on finite sequence of 'items'.
 *       
 *   <li> A bounded and lazy model-checker for checking if a target-model 
 *       M1 has an execution that satisfies a specification-model M2 
 *       (and returns a witnessing execution of this). The specification-model 
 *       is expressed as a finite state machine (FSM) with either a standard 
 *       acceptance criterion, or an omega (more specifically, Buchi) 
 *       acceptance criterion. 
 *       M1 is anything that implements the interface {@link ITargetModel}.
 *       
 *   <li> An interface ITargetModel that a target system must implement, if
 *       you want to use the model checker to check it.
 *   </ol>    
 *       
 *  TODO: translation from LTLs to specification-FSMs. Probably will only
 *  support limited translation (won't allow recursion to the left-side of
 *  until).
 * 
 */
package eu.iv4xr.framework.extensions.ltl;