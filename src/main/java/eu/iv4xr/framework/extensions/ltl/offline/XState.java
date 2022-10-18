package eu.iv4xr.framework.extensions.ltl.offline;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.utils.Pair;

/**
 * A generic representation of an agent-state, read from a csv-trace file. A
 * state is assumed to be described a vector of name-value pairs. E.g. a pair
 * (vn,x) represent a variable/property in the state named vn, whose value is x.
 * Only numeric values (float) are supported, with the exception of a property
 * representing time, which is of type long.
 * 
 * <p>
 * Two properties are included as first-class fields: this.pos and this.time to
 * represent the agent's location and the time when the state is sampled. The
 * position is represented as a {@link Vec3} and time is a long. If these
 * information are not available then they will be null.
 * 
 * <p>
 * Other name-value pairs are kept in the field this.values.
 * 
 * <p>
 * An XState can be enriched to contain derived information. See {@link #diff}
 * and {@link #history}. The enrichment is done from {@link XStateTrace#enrichTrace(String...)}.
 * 
 * 
 * @author Wish
 *
 */
public class XState {
	
	/**
	 * The position of the agent when this state was sampled.
	 */
	public Vec3 pos ;
	
	/**
	 * The time when this state was sampled.
	 */
	public Long time ;
	
	/**
	 * Key-value pairs representing the value of other variables/properties that
	 * make up this state. Only numeric/foat values are supported.
	 */
	public Map<String,Float> values = new HashMap<>() ;
	
	/**
	 * For every property name vn, diff.get(vn) is the difference between the value
	 * of vn in this state and its value in the "previous" state in the state-trace
	 * that contains them. See {@link XStateTrace#enrichTrace(String...)}.
	 */
	public Map<String,Float> diff = new HashMap<>() ;
	
	/**
	 * We can put here the "history" of a variable/property. Suppose vn is 
	 * a property whose history we want to track. Then history.get(vn) gives
	 * a list of pairs (p,x) of location and the value of vn through out the
	 * state-trace that contains this state.
	 * See {@link XStateTrace#enrichTrace(String...)}.
	 */
	public Map<String,List<Pair<Vec3,Float>>> history = new HashMap<>() ;
	
	public XState() { }
	
	public XState(Vec3 position, long timeStamp, Pair<String,Float> ... keyValuePairs) {
		this.pos = position ;
		this.time = timeStamp ;
		for (var kv : keyValuePairs) {
			values.put(kv.fst, kv.snd) ;
		}
	}
	
	@Override
	public String toString() {
		String s = "" ;
		s += "@" + pos + ", t:" + time ;
		for (var entry : values.entrySet()) {
			s += ", " + entry.getKey() + ":" + entry.getValue() ;
		}
		return s ;	
	}
	
	/**
	 * Return the value of the variable/property of the given name.
	 */
	public Float val(String vname) {
		return values.get(vname) ;
	}

	/**
	 * Return the difference of value of the variable/property of the given name
	 * in this state and its value in the "previous" state. This is the same
	 * as this.diff.get(vname).
	 */
	public Float diff(String vname) {
		return diff.get(vname) ;
	}
	
	public List<Vec3> history(String vname) {
		if (history.get(vname) == null) 
			return null ;
		return history.get(vname).stream().map(e -> e.fst).collect(Collectors.toList()) ;
	}
	
	public List<Vec3> history(String vname, Predicate<Float> p) {
		if (history.get(vname) == null) 
			return null ;
		return history.get(vname).stream()
				.filter(e -> p.test(e.snd))
				.map(e -> e.fst)
				.collect(Collectors.toList()) ;
	}
	
	public Float max(String vname) {
		var z = history.get(vname) ;
		if (z == null || z.size() == 0) 
			return null ;
		float m = z.get(0).snd ;
		for (var e : z) {
			if (e.snd > m) m = e.snd ;
		}
		return m ;
	}
	
	
	static final String HEALTH = "health" ;
	static final String HOPE = "hope" ;
	static final String JOY = "joy" ;
	static final String SATISFACTION = "satisfaction" ;
	static final String FEAR = "fear" ;
	static final String DISTRESS = "distress" ;
	static final String DISAPPOINTMENT = "disappointment" ;
	
	/**
	 * Return this.values.get("health").
	 */
	public Float health() { return values.get(HEALTH) ; }

	/**
	 * Return this.values.get("hope").
	 */
	public Float hope() { return values.get(HOPE) ; }
	
	/**
	 * Return this.values.get("joy").
	 */
	public Float joy() { return values.get(JOY) ; }
	
	/**
	 * Return this.values.get("satisfaction").
	 */
	public Float satisfaction() { return values.get(SATISFACTION) ; }
	
	/**
	 * Return this.values.get("fear").
	 */
	public Float fear() { return values.get(FEAR) ; }
	
	/**
	 * Return this.values.get("distress").
	 */
	public Float distress() { return values.get(DISTRESS) ; }
	
	/**
	 * Return this.values.get("disappointment").
	 */
	public Float disappointment() { return values.get(DISAPPOINTMENT) ; }
	
	/**
	 * Return this.diff.get("health").
	 */
	public Float dHealth() { return diff.get(HEALTH) ; }
	
	/**
	 * Return this.diff.get("hope").
	 */
	public Float dHope() { return diff.get(HOPE) ; }	
	
	/**
	 * Return this.diff.get("joy").
	 */
	public Float dJoy() { return diff.get(JOY) ; }
	public Float dSatisfaction() { return diff.get(SATISFACTION) ; }
	
	/**
	 * Return this.diff.get("fear").
	 */
	public Float dFear() { return diff.get(FEAR) ; }
	
	/**
	 * Return this.diff.get("distress").
	 */
	public Float dDistress() { return diff.get(DISTRESS) ; }
	public Float dDisappointment() { return diff.get(DISAPPOINTMENT) ; }
	
	/**
	 * Return this.history.get("hope").
	 */
	public List<Pair<Vec3,Float>> trHope() { return history.get(HOPE) ; }

	/**
	 * Return this.history.get("joy").
	 */
	public List<Pair<Vec3,Float>> trJoy() { return history.get(JOY) ; }

	public List<Pair<Vec3,Float>> trSatisfaction() { return history.get(SATISFACTION) ; }

	/**
	 * Return this.history.get("fear").
	 */
	public List<Pair<Vec3,Float>> trFear() { return history.get(FEAR) ; }
	
	/**
	 * Return this.history.get("distress").
	 */
	public List<Pair<Vec3,Float>> trDistress() { return history.get(DISTRESS) ; }
	public List<Pair<Vec3,Float>> trDisappointment() { return history.get(DISAPPOINTMENT) ; }

}
