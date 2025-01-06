package eu.iv4xr.framework.extensions.mbt;

import java.util.*;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.utils.Pair;

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
	
	/**
	 * Get the highest value of the transitions that go out from the given 
	 * configuration.
	 * Null is returned if the configuration has no outgoing transitions.
	 */
	Float getMaxTransitionValue(MBTStateConfiguration st) {
		var outgoings = transValues.get(st) ;
		if (outgoings == null || outgoings.isEmpty()) {
			return null ;
		}
		Float bestval = outgoings.values().stream().max((x,y) -> Float.compare(x,y)).get() ;
		return bestval ;
	}
	
	/**
	 * Return actions that have at least one transition that go out from
	 * the given state, and whose transition-value is the highest.
	 * Null, if the config has no outgoing transition.
	 */
	public List<String> getActionsWithMaxValue(MBTStateConfiguration st) {
		var outgoings = transValues.get(st) ;
		if (outgoings == null) {
			return new LinkedList<String>() ;
		}
		Float bestval = getMaxTransitionValue(st) ;
		return outgoings.entrySet().stream()
				.filter(e -> e.getValue() <= bestval)
				.map(e -> e.getKey().action)
				.collect(Collectors.toList()) ;
	}
	
	List<String> getOutgoingActions(MBTStateConfiguration st) {
		var outgoings = transValues.get(st) ;
		if (outgoings == null) {
			return null ;
		}
		return outgoings.keySet().stream()
			.map(tr -> tr.action)
			.collect(Collectors.toList()) ;
	}
	
	Float getOutgoingActionAvrgValue(MBTStateConfiguration st, String action) {
		var outgoings = transValues.get(st) ;
		if (outgoings == null) {
			return null ;
		}
		var values = outgoings.entrySet().stream()
			.filter(tr -> tr.getKey().action.equals(action))
			.map(tr -> tr.getValue())
			.collect(Collectors.toList())
			;
		if (values.isEmpty())
			return null ;
		double r = values.stream().collect(Collectors.averagingDouble(v ->  v)) ;
		return (float) r ;
	}
	
	
	public Pair<List<String>, Float> getActionsWithMaxAverageValue(
			MBTStateConfiguration st,
			List<String> actions
			) {
		float maxAvrgVal = Float.NEGATIVE_INFINITY ;
		for (var a : actions) {
			var v = getOutgoingActionAvrgValue(st,a) ;
			if (v != null && v > maxAvrgVal) {
				maxAvrgVal = v ;
			}
		}
		if (maxAvrgVal == Float.NEGATIVE_INFINITY )
			return null ;
		
		final float maxAvrgVal_ = maxAvrgVal ;
		
		List<String> maxactions = actions.stream()
			.filter(a -> {
				var v = getOutgoingActionAvrgValue(st,a) ;
				return v != null && v >= maxAvrgVal_ ;
			 })
			.collect(Collectors.toList()) ;

		return new Pair<>(maxactions,maxAvrgVal) ;
	}
	
	public Map<MBTTransition,Float> getAllTransitionsValues() {
		Map<MBTTransition,Float> trans = new HashMap<>() ;
		for (var trg : transValues.values()) {
			for (var trval : trg.entrySet()) {
				trans.put(trval.getKey(), trval.getValue()) ;
			}
		}
		return trans ;
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
