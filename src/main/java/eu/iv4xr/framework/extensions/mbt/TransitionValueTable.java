package eu.iv4xr.framework.extensions.mbt;

import java.util.*;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.agents.State;

public class TransitionValueTable {
	
	public Map<MBTStateConfiguration,Map<MBTTransition,Float>> transValues = new HashMap<>() ;

	
	public TransitionValueTable() { }
	
	/**
	 * Build an initial table using the transitions data in the given
	 * model. The values of the states and transitions will all be
	 * initialized to the given start-value.
	 */
	public void initializeFromModel(MBTModel<? extends State>  model, float startValue) {
		transValues.clear();
		for (var trs : model.transitions.entrySet()) {
			var C1 = trs.getKey() ;
			Map<MBTTransition,Float> outgoings = new HashMap<>() ;
			transValues.put(C1, outgoings) ;
			var outgoingzzz = trs.getValue() ;
			for (var tr : outgoingzzz) {
				outgoings.put(tr,startValue) ;
				// also add the target-conf:
				var C2 = tr.dest ;
				if (transValues.get(C2) == null) {
					Map<MBTTransition,Float> outgoingsC2 = new HashMap<>() ;
					transValues.put(C2, outgoingsC2) ;
				}
			}
		}	
	}
	
	
	public Float getTransitionValue(MBTTransition tr) {
		var outgoings = transValues.get(tr.src) ;
		if (outgoings == null)
			return null ;
		return outgoings.get(tr) ;
	}
	
	public Float getBestTransitionValue(MBTStateConfiguration st) {
		var outgoings = transValues.get(st) ;
		if (outgoings == null || outgoings.isEmpty()) {
			return null ;
		}
		Float bestval = outgoings.values().stream().min((x,y) -> Float.compare(x,y)).get() ;
		return bestval ;
	}
	
	public List<String> getBestValuedAction(MBTStateConfiguration st) {
		var outgoings = transValues.get(st) ;
		if (outgoings == null) {
			return new LinkedList<String>() ;
		}
		Float bestval = getBestTransitionValue(st) ;
		return outgoings.entrySet().stream()
				.filter(e -> e.getValue() <= bestval)
				.map(e -> e.getKey().action)
				.collect(Collectors.toList()) ;
	}
	
	public void updateTransitionValue(MBTTransition tr, Float newValue) {
		var outgoings = transValues.get(tr.src) ;
		if (outgoings == null) {
			outgoings = new HashMap<MBTTransition,Float>() ;
			transValues.put(tr.src, outgoings) ;
		}
		outgoings.put(tr, newValue) ;
	}
	
}
