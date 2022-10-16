package eu.iv4xr.framework.extensions.ltl.offline;

import java.util.*;

import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.utils.Pair;

public class XState {
	
	public Vec3 pos ;
	public Long time ;
	
	public Map<String,Float> values = new HashMap<>() ;
	public Map<String,Float> diff = new HashMap<>() ;
	public Map<String,List<Pair<Vec3,Float>>> history = new HashMap<>() ;
	
	public XState() { }
	
	public XState(Vec3 position, long timeStamp, Pair<String,Float> ... keyValuePairs) {
		this.pos = position ;
		this.time = timeStamp ;
		for (var kv : keyValuePairs) {
			values.put(kv.fst, kv.snd) ;
		}
	}
	
	static final String HEALTH = "health" ;
	static final String HOPE = "hope" ;
	static final String JOY = "joy" ;
	static final String SATISFACTION = "satisfaction" ;
	static final String FEAR = "fear" ;
	static final String DISTRESS = "distress" ;
	static final String DISAPPOINTMENT = "disappointment" ;
	
	public Float health() { return values.get(HEALTH) ; }
	
	public Float hope() { return values.get(HOPE) ; }
	public Float joy() { return values.get(JOY) ; }
	public Float satisfaction() { return values.get(SATISFACTION) ; }
	public Float fear() { return values.get(FEAR) ; }
	public Float distress() { return values.get(DISTRESS) ; }
	public Float disappointment() { return values.get(DISAPPOINTMENT) ; }
	
	public Float dHealth() { return diff.get(HEALTH) ; }
	
	public Float dHope() { return diff.get(HOPE) ; }	
	public Float dJoy() { return diff.get(JOY) ; }
	public Float dSatisfaction() { return diff.get(SATISFACTION) ; }
	public Float dFear() { return diff.get(FEAR) ; }
	public Float dDistress() { return diff.get(DISTRESS) ; }
	public Float dDisappointment() { return diff.get(DISAPPOINTMENT) ; }
	
	public List<Pair<Vec3,Float>> trHope() { return history.get(HOPE) ; }
	public List<Pair<Vec3,Float>> trJoy() { return history.get(JOY) ; }
	public List<Pair<Vec3,Float>> trSatisfaction() { return history.get(SATISFACTION) ; }
	public List<Pair<Vec3,Float>> trFear() { return history.get(FEAR) ; }
	public List<Pair<Vec3,Float>> trDistress() { return history.get(DISTRESS) ; }
	public List<Pair<Vec3,Float>> trDisappointment() { return history.get(DISAPPOINTMENT) ; }

}
