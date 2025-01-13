package eu.iv4xr.framework.extensions.mbt;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
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
	
	
	MBTModel() { }
	
	public MBTModel(String name) { this.name = name ; }
	
	
	public MBTModel<S> addStates(MBTState<S> ... Z) {
		for (MBTState<S> st : Z) states.put(st.id, st) ;
		return this ;
	}
	
	/**
	 * The same as {@link #addStates(MBTState...)}.
	 */
	public MBTModel<S> addStatePreds(MBTState<S> ... Z) {
		return addStates(Z) ;
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
	
	/**
	 * Get all transitions stored in {@link #transitions}.
	 */
	public List<MBTTransition> getAllTransitions() {
		List<MBTTransition>  trs = new LinkedList<>() ;
		for (var trgroup : transitions.values()) {
			trs.addAll(trgroup) ;
		}
	    return trs ;
	}
	
	/**
	 * Get all state-configurations stored in {@link #transitions}.
	 */
	public Set<MBTStateConfiguration> getAllConfigurations() {
		var trs = getAllTransitions() ;
		Set<MBTStateConfiguration> Z = new HashSet<>() ;
		for (var T : trs) {
			Z.add(T.src) ;
			Z.add(T.dest) ;
		}
		return Z ;
	}
	
	@Override
	public String toString() {
		var z = new StringBuffer() ;
		z.append("** Model: " + name + "\n") ;
		z.append("   States (" + states.size() + "): " + states.keySet() + "\n") ;
		z.append("   Actions (" + actions.size() + "): " + actions.keySet() + "\n") ;
		z.append("   Transitions (" + getAllTransitions().size() + "):\n") ;
		int k=1 ;
		var trs = getAllTransitions() ;
		for (var tr : trs) {
			z.append("     [" + k + "] " + tr + "\n") ;
			k++ ;
		}
		return z.toString() ;
		
	}
	
	static class Wrapper {
		public List<MBTTransition> trans ;
		public Wrapper() {} 
	}
	
	/**
	 * Save the transitions to a file.
	 */
	public void saveTransitions(String filename) throws IOException {
		FileWriter fwriter = new FileWriter(filename) ;
		
		var wrapper = new Wrapper() ;
		wrapper.trans = getAllTransitions() ;
		
		Gson gson = new GsonBuilder()
			    . setPrettyPrinting()
			    . serializeNulls()
			    . create(); 
		//System.out.println(">>>> " + gson.toJson(base)) ;
		gson.toJson(wrapper, fwriter);
		
		fwriter.flush();
		fwriter.close();
	}
	
	public void saveDot(String filename) throws IOException {
		
		StringBuffer buf = new StringBuffer();
		
		buf.append("digraph " + (name.equals("") ? "M" : name) + " {\n\n") ;
		
		var trans = getAllTransitions() ;
		var configs = getAllConfigurations() ;
		Map<MBTStateConfiguration,Integer> configNr = new HashMap<>() ;
		int nr = 0 ;
		for (var C : configs) {
			buf.append("   C" + nr + "[label=\"" + C.states.toString() + "\"];\n") ;
			configNr.put(C,nr) ;
			nr++ ;
		}
		buf.append("\n") ;
		
		for(var T : trans) {
			String src = "C" + configNr.get(T.src) ;
			String dest = "C" + configNr.get(T.dest) ;
			buf.append("   " + src + " -> " + dest + " [label=\"" + T.action + "\"] ;\n") ;
		}
		
		buf.append("\n}") ;

		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(buf.toString());
        writer.close();
	}
	
	
	/**
	 * Load the transitions saved in the given json-file. If 
	 *
	 * @param writeOver If true, the transitions already in this model will be first wiped.
	 * 					Else, transitions form the file will be added into this model.
	 */
	public void loadTransitionsFromFile(String filename, Boolean writeOver) throws IOException {
		Gson gson = new Gson();
		Reader reader = Files.newBufferedReader(Paths.get(filename));
		Wrapper loaded = gson.fromJson(reader,Wrapper.class);
		if (writeOver)
			transitions.clear(); 
		for (var tr : loaded.trans) {
			var src = tr.src ;
			var outgoings = transitions.get(src) ;
			if (outgoings == null) {
				outgoings = new LinkedList<MBTTransition>() ;
				transitions.put(src,outgoings) ;
			}
			if (! outgoings.contains(tr)) {
				outgoings.add(tr) ;
			}
		}
	}
	

	
}
