package nl.uu.cs.aplib.iv4xr.verification;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.*;

import nl.uu.cs.aplib.Utils.Parsable;

public class TestDataCollector implements Parsable {
	
	TestDataCollector() { }
	
	public static class ObservationEvent implements Serializable, Parsable {
		
		private static final long serialVersionUID = 1L;
		
		protected String id ;
		
		ObservationEvent() { }
		public ObservationEvent(String id) { 
			if (id==null) throw new IllegalArgumentException("Trying to create an ObservationEvent with a null id.") ;
			this.id = id ; 
		}
		
		public String getId() { return id ; }
		
		@Override
		public boolean equals(Object o) {
			if (o instanceof ObservationEvent) {
				return id.equals(((ObservationEvent) o).id)  ;
			}
			return false ;
		}
		
		@Override
		public int hashCode() {
			return id.hashCode() ;
		}
		
		@Override
		public String toString() { return id; }
		
		public ObservationEvent parse(String s) { 
			if (s==null) throw new IllegalArgumentException("Trying to parse an ObservationEvent from a null string.") ;
			return new ObservationEvent(s) ;
		}
	}
	
	public static class CoveragePointEvent extends ObservationEvent { }
	
	public static class TimeStampedObservationEvent extends ObservationEvent {
		
		private static final long serialVersionUID = 1L;

		protected LocalTime timestamp ;
		
		/**
		 * Further information about this event. It can be null, or something that is not an empty string.
		 */
		protected String info ;
		
		TimeStampedObservationEvent() { super() ; }

		public TimeStampedObservationEvent(String id, String info) {
			super(id) ;
			timestamp = LocalTime.now() ;
			if (info != null && info.length()==0) throw new IllegalArgumentException("The info part cannot be empty.") ;
			this.info = info ;
		}
		
		public TimeStampedObservationEvent(String id) {
			this(id,null) ;
		}
		
		public LocalTime getTimestamp() { return timestamp; }
		public String getInfo() { return info ; }
		
		@Override
		/**
		 * Two time-stamped events are equal if their id and time stamps are equal.
		 */
		public boolean equals(Object o) {
			if (o instanceof TimeStampedObservationEvent) {
				var o_ = (TimeStampedObservationEvent) o ;
				return id.equals(o_.id) && timestamp.equals(o_.timestamp) ;
			}
			return false ;
		}
		
		@Override
		public int hashCode() {
			return Objects.hash(id,timestamp) ;
		}
		
		@Override
		public String toString() {
			var info_ = info ;
			if (info_ == null) info_ = "null" ;
			return id + ";" + timestamp  + ";" + info_ ;
		}
		
		@Override
		public TimeStampedObservationEvent parse(String s) {
			if (s==null) throw new IllegalArgumentException("Trying to parse an TimeStampedObservationEvent from a null string.") ;
			var parts = s.split(";") ;
			if (parts.length != 3) throw new IllegalArgumentException("Parse error on: " + s) ;
			TimeStampedObservationEvent o = new TimeStampedObservationEvent() ;
			o.id = parts[0] ; 
			o.timestamp = LocalTime.parse(parts[1]) ;
			o.info = parts[2] ;
			if (o.info.equals("null")) o.info = null ;
			return o ;
		}
	}
	
	public static class VerdictEvent extends TimeStampedObservationEvent { 
		
		/**
		 * True means it is ok. False represents error. Null represents undecided.
		 */
		protected Boolean verdict = null ;
		
		public VerdictEvent(String id, String info, Boolean v) {
			super(id,info) ;
			if (v != null) {
				if (v.booleanValue()) verdict = true ; else verdict = false ;
			}
		}
		
		public boolean isPass() { return verdict != null && verdict.booleanValue() ; }
		public boolean isFail() { return verdict != null && !verdict.booleanValue() ; }
		public boolean isUndecided() { return verdict == null ; }
		
		@Override
		/**
		 * Two verdicts events are equal if their id, their time stamps, and their verdicts are equal.
		 */
		public boolean equals(Object o) {
			if (super.equals(o) && o instanceof VerdictEvent) {
				var o_ = (VerdictEvent) o ;
				if (verdict == null) return o_.verdict == null ;
				if (o_.verdict == null) return verdict == null ;
				return verdict.equals(o_.verdict) xxx ;
			}
			return false ;
		}
	}
	
	static class EventTrace implements Parsable {
		List<TimeStampedObservationEvent> trace = new LinkedList<>() ;
		int numOfErrorEvent = 0 ;
		VerdictEvent lastErrorEvent = null ;
		public EventTrace() { }
		
		/**
		 * Add the event e to this event-trace. If it is an error-event, this will be registered so.
		 * If cummulativeTrace is non null, the event will also added into that trace.
		 * Access to this method is synchronized (mutual exclusive).
		 */
		synchronized void registerEvent(TimeStampedObservationEvent e) {
			if (e == null) return ;
			trace.add(e) ;
			if (e instanceof VerdictEvent) {
				numOfErrorEvent++ ;
				lastErrorEvent = (VerdictEvent) e ;
			}
		}

		@Override
		public String toString() {
			throw new UnsupportedOperationException("TODO") ;
		}
		
		@Override
		public Parsable parse(String s) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("TODO") ;
		}	
	}
	
	static class CoverageMap implements Parsable {
		
		Map<CoveragePointEvent,Integer> coverage = new HashMap<>() ;

		@Override
		public String toString() {
			throw new UnsupportedOperationException("TODO") ;
		}
		
		@Override
		public Parsable parse(String s) {
			// TODO Auto-generated method stub
			throw new UnsupportedOperationException("TODO") ;
		}
		
		Map<CoveragePointEvent,Integer> getCoverage() { return coverage ; }
		
		synchronized void startTrackingCoveragePoint(CoveragePointEvent e) {
			if (! coverage.containsKey(e)) {
				coverage.put(e,0) ;
			}
		}
		
		synchronized void registerVisit(CoveragePointEvent e, CoverageMap collectiveCovMap) {
			if (e == null) return ;
			int count = 1 ;
			if (coverage.containsKey(e)) {
				count +=  coverage.get(e) ;
			}
			coverage.put(e,count) ;
			if (collectiveCovMap != null) {
				collectiveCovMap.registerVisit(e,null);
			}
		}
		
	}
	
	protected Map<String,CoverageMap> perAgentCoverage = new HashMap<>() ;
	protected CoverageMap collectiveCoverageMap = new CoverageMap() ;
	protected Map<String,EventTrace> perAgentEventTrace = new HashMap<>() ;
	
	public void startTrackingCoveragePoint(CoveragePointEvent e) {
		collectiveCoverageMap.startTrackingCoveragePoint(e);
		for (CoverageMap CM : perAgentCoverage.values()) CM.startTrackingCoveragePoint(e);
	}
	
	public void registerTestAgent(String agentUniqueId) {
		if (perAgentCoverage.containsKey(agentUniqueId)) return ;
		perAgentEventTrace.put(agentUniqueId, new EventTrace()) ;
		CoverageMap CM = new CoverageMap() ;
		perAgentCoverage.put(agentUniqueId,CM) ;
		for (CoveragePointEvent e : collectiveCoverageMap.coverage.keySet()) CM.startTrackingCoveragePoint(e);
	}
	
	public void registerVisit(String agentUniqueId, CoveragePointEvent e) {
		if (! perAgentCoverage.containsKey(agentUniqueId)) return ;
		CoverageMap CM = perAgentCoverage.get(agentUniqueId) ;
		CM.registerVisit(e, collectiveCoverageMap);
	}
	
	public void registerEvent(String agentUniqueId, TimeStampedObservationEvent e) {
		if (! perAgentEventTrace.containsKey(agentUniqueId)) return ;
		EventTrace ET = perAgentEventTrace.get(agentUniqueId) ;
		ET.registerEvent(e);
	}
	
	public Map<CoveragePointEvent,Integer> getTestAgentCoverage(String agentUniqueId) {
		CoverageMap CM = perAgentCoverage.get(agentUniqueId) ;
		if (CM == null) throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.") ;
		return CM.coverage ; 
	}

	public Map<CoveragePointEvent,Integer> getCollectiveCoverage() {
		return collectiveCoverageMap.coverage ; 
	}
	
	public List<TimeStampedObservationEvent> getTestAgentTrace(String agentUniqueId) {
		EventTrace ET = perAgentEventTrace.get(agentUniqueId) ;
		if (ET == null) throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.") ;
		return ET.trace ;
	}
	
	public int getNumberOfErrorSeen(String agentUniqueId) {
		EventTrace ET = perAgentEventTrace.get(agentUniqueId) ;
		if (ET == null) throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.") ;
		return ET.numOfErrorEvent ;
	}
	
	public int getNumberOfErrorSeen() {
		int count = 0 ;
		for (EventTrace ET : perAgentEventTrace.values()) count += ET.numOfErrorEvent ;
		return count ;
	}
	
	public VerdictEvent getLastError(String agentUniqueId) {
		EventTrace ET = perAgentEventTrace.get(agentUniqueId) ;
		if (ET == null) throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.") ;
		return ET.lastErrorEvent ;
	}
	
	public VerdictEvent getLastError() {
		VerdictEvent err = null ;
		for (EventTrace ET : perAgentEventTrace.values()) {
			VerdictEvent err2 = ET.lastErrorEvent ;
			if (err2 == null) continue ;
			if (err == null) err = err2 ;
			else {
				if (err2.timestamp.isAfter(err.timestamp)) err = err2 ;
			}
		}
		return err ;
	}
	
	@Override
	public String toString() {
		throw new UnsupportedOperationException("TODO") ;
	}

	@Override
	public Parsable parse(String s) {
		// TODO Auto-generated method stub
		throw new UnsupportedOperationException("TODO") ;
	}

}
