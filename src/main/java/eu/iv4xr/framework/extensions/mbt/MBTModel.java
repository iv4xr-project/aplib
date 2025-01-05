package eu.iv4xr.framework.extensions.mbt;

import java.util.*;

import nl.uu.cs.aplib.agents.State;

/**
 * Action-based model. NOT standard fSM.
 * 
 * Our model support simultaneous states. That is, at a given moment
 * there can be multiple model-states that are consistent with the
 * current SUT concrete state. The set of current simultaneous states
 * is called configuration.
 * 
 * Transitions are unparameterized.
 */
public class MBTModel<S extends State> {
	
	public String name ;
	
	public Map<String,MBTState<S>> states = new HashMap<>() ;
	
	public Map<String,MBTAction<S>> actions = new HashMap<>() ;
	
	public Map<MBTStateConfiguration,List<MBTTransition>> transitions = new HashMap<>() ;
	
	public MBTModel(String name) { this.name = name ; }
	
	
	public MBTModel<S> addStates(MBTState<S> ... Z) {
		for (MBTState<S> st : Z) states.put(st.id, st) ;
		return this ;
	}
	
	public MBTModel<S> addActions(MBTAction<S> ... A) {
		for (MBTAction<S> a : A) actions.put(a.name, a) ;
		return this ;
	}
	
	public List<MBTAction<S>> enabledActions(S state) {
		 List<MBTAction<S>> enabled = new LinkedList<>() ;
		 for (var a : actions.values()) {
			 if (a.enabled(state)) enabled.add(a) ;
		 }
		 return enabled ;
	}
	
	

	
}
