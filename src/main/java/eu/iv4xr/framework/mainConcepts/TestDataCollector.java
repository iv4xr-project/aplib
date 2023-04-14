package eu.iv4xr.framework.mainConcepts;

import static eu.iv4xr.framework.mainConcepts.ObservationEvent.*;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import nl.uu.cs.aplib.utils.CSVUtility;
import nl.uu.cs.aplib.utils.Parsable;

/**
 * This class is used to collect information from one or more test-agents.
 * Test-agents collect information in the form of instances of
 * {@link ObservationEvent} that they then send to a TestDataCollector. Such a
 * collector collects three types of data:
 * 
 * <ul>
 * <li>Time-stamped events. These are instances of
 * {@link ObservationEvent.TimeStampedObservationEvent}. They are stored
 * chronologically in a list and is useful for debugging. E.g. when a negative
 * verdict is observed, we can see in this list what are the events that preceed
 * it.
 * 
 * <li>Verdicts. These are a special kind of time-stamped events that report
 * things that the agent observed as correct as well as as wrong.
 * 
 * <li>Coverage events. These are instances of
 * {@link ObservationEvent.CoveragePointEvent} representing the coverage-points
 * the test-agent manage to cover.
 * </ul>
 * 
 * @author Wish
 *
 */
public class TestDataCollector implements Parsable {

    public TestDataCollector() {
    }

    /**
     * This class is used to hold a chronological list of time-stamped events.
     */
    static class EventTrace implements Parsable {
        List<TimeStampedObservationEvent> trace = new LinkedList<>();
        int numOfPassVerdicts = 0;
        int numOfUndecidedVerdicts = 0;
        int numOfFailVerdicts = 0;
        VerdictEvent lastFailVerdict = null;

        public EventTrace() {
        }

        /**
         * Add the event e to this event-trace. If it is an error-event, this will be
         * registered so. If cummulativeTrace is non null, the event will also added
         * into that trace. Access to this method is synchronized (mutual exclusive).
         */
        synchronized void registerEvent(TimeStampedObservationEvent e) {
            if (e == null)
                return;
            trace.add(e);
            if (e instanceof VerdictEvent) {
                VerdictEvent e_ = (VerdictEvent) e;
                if (e_.isPass())
                    numOfPassVerdicts++;
                else if (((VerdictEvent) e).isFail()) {
                    numOfFailVerdicts++;
                    lastFailVerdict = (VerdictEvent) e;
                } else
                    numOfUndecidedVerdicts++;
            }
        }

        @Override
        public String toString() {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public Parsable parse(String s) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("TODO");
        }
        
        /**
         * Clear the trace and reset verdict-counts to 0.
         */
        public void reset() {
        	trace.clear(); 
            numOfPassVerdicts = 0;
            numOfUndecidedVerdicts = 0;
            numOfFailVerdicts = 0;
            lastFailVerdict = null;
        }
    }

    /**
     * This class is used to keep track which coverage-points were visited.
     */
    static class CoverageMap implements Parsable {

        Map<String, Integer> coverage = new HashMap<>();

        @Override
        public String toString() {
            throw new UnsupportedOperationException("TODO");
        }

        @Override
        public Parsable parse(String s) {
            // TODO Auto-generated method stub
            throw new UnsupportedOperationException("TODO");
        }

        Map<String, Integer> getCoverage() {
            return coverage;
        }

        synchronized void startTrackingCoveragePoint(String coveragePointId) {
            if (!coverage.containsKey(coveragePointId)) {
                coverage.put(coveragePointId, 0);
            }
        }

        synchronized void registerVisit(CoveragePointEvent e, CoverageMap collectiveCovMap) {
            if (e == null)
                return;
            int count = 1;
            if (coverage.containsKey(e.coveragePointId)) {
                count += coverage.get(e.coveragePointId);
            }
            coverage.put(e.coveragePointId, count);
            if (collectiveCovMap != null) {
                collectiveCovMap.registerVisit(e, null);
            }
        }
        
        /**
         * Reset all counts to 0.
         */
        synchronized void reset() {
        	for(var e : coverage.entrySet()) {
        		e.setValue(0) ;
        	}
        	
        }

    }

    /**
     * The coverage-map of each agent. By "each agent" we mean a test-agent that has
     * been registered to this TestDataCollector.
     */
    protected Map<String, CoverageMap> perAgentCoverage = new HashMap<>();

    /**
     * The cummulative coverage-map of all agents. By "agents" we mean the
     * test-agents that have been registered to this TestDataCollector.
     */
    protected CoverageMap collectiveCoverageMap = new CoverageMap();

    /**
     * The trace of time-stamped events of each agent.
     */
    protected Map<String, EventTrace> perAgentEventTrace = new HashMap<>();

    /**
     * Add a coverage-point to the set of coverage-points whose coverage will be tracked.
     */
    public void startTrackingCoveragePoint(String coveragePointId) {
        collectiveCoverageMap.startTrackingCoveragePoint(coveragePointId);
        for (CoverageMap CM : perAgentCoverage.values())
            CM.startTrackingCoveragePoint(coveragePointId);
    }

    /**
     * Register a test-agent to this data collector. We do not literally pass a
     * test-agent, but rather its id.
     * 
     * @param agentUniqueId The id of the agent. This is assumed to be unique.
     */
    public void registerTestAgent(String agentUniqueId) {
        if (perAgentCoverage.containsKey(agentUniqueId))
            return;
        perAgentEventTrace.put(agentUniqueId, new EventTrace());
        CoverageMap CM = new CoverageMap();
        perAgentCoverage.put(agentUniqueId, CM);
        for (String coveragePoint : collectiveCoverageMap.coverage.keySet())
            CM.startTrackingCoveragePoint(coveragePoint);
    }

    /**
     * Register a visit to a coverage-point by a test-agent.
     * 
     * @param agentUniqueId Unique id of the test-agent.
     * @param e             An event representing the visit to some coverage point
     *                      of interest.
     */
    public void registerVisit(String agentUniqueId, CoveragePointEvent e) {
        if (!perAgentCoverage.containsKey(agentUniqueId))
            return;
        CoverageMap CM = perAgentCoverage.get(agentUniqueId);
        CM.registerVisit(e, collectiveCoverageMap);
    }

    /**
     * Register an observation event. This allows a test-agent to register whatever
     * event of interest to this data collector. This will be recorded into the
     * agent's trace. Note that an event can also be a verdict-event, where the test
     * agent reports that it has seen something that is either according to its
     * expectation, or violating it.
     * 
     * @param agentUniqueId Unique id of the test-agent.
     * @param e             An event representing the observation to record.
     */
    public void registerEvent(String agentUniqueId, TimeStampedObservationEvent e) {
        if (!perAgentEventTrace.containsKey(agentUniqueId))
            return;
        EventTrace ET = perAgentEventTrace.get(agentUniqueId);
        ET.registerEvent(e);
    }

    /**
     * Return a map describing which coverage-points were covered by the specified
     * agent. If m is the returned map, it maps a set of
     * {@link ObservationEvent.CoveragePointEvent} to integers. Each
     * coverage-point-event in the map represents a unique coverage-point to track.
     * The integer it is mapped to is the number of times the coverage-point is
     * visited.
     */
    public Map<String, Integer> getTestAgentCoverage(String agentUniqueId) {
        CoverageMap CM = perAgentCoverage.get(agentUniqueId);
        if (CM == null)
            throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.");
        return CM.coverage;
    }

    /**
     * Return a map describing which coverage-points were collectively covered all
     * test-agents registered to this TestDataCollector. If m is the returned map,
     * it maps a set of {@link ObservationEvent.CoveragePointEvent} to integers.
     * Each coverage-point-event in the map represents a unique coverage-point to
     * track. The integer it is mapped to is the number of times the coverage-point
     * is visited.
     */
    public Map<String, Integer> getCollectiveCoverage() {
        return collectiveCoverageMap.coverage;
    }

    /**
     * Return the "trace" of the specified test-agent. This trace is a
     * chronologically ordered list of time-stamped events that this test-agent
     * reported to this TestDataCollector. Note that verdicts count as time-stamped
     * events.
     */
    public List<TimeStampedObservationEvent> getTestAgentTrace(String agentUniqueId) {
        EventTrace ET = perAgentEventTrace.get(agentUniqueId);
        if (ET == null)
            throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.");
        return ET.trace;
    }
    
    /**
     * Return the trace of the specified agent, containing only of ScalarTracingEvents.
     */
    public List<ScalarTracingEvent> getTestAgentScalarsTrace(String agentUniqueId) {
        EventTrace ET = perAgentEventTrace.get(agentUniqueId);
        if (ET == null)
            throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.");
        return ET.trace.stream()
                  .filter(e -> e instanceof ScalarTracingEvent)
                  .map(e -> (ScalarTracingEvent) e)
                  .collect(Collectors.toList()) ;
    }

    /**
     * Get the number of negative verdicts reported by the specified test-agent.
     */
    public int getNumberOfFailVerdictsSeen(String agentUniqueId) {
        EventTrace ET = perAgentEventTrace.get(agentUniqueId);
        if (ET == null)
            throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.");
        return ET.numOfFailVerdicts;
    }

    /**
     * Get the total number of negative verdicts reported by all test-agents.
     */
    public int getNumberOfFailVerdictsSeen() {
        int count = 0;
        for (EventTrace ET : perAgentEventTrace.values())
            count += ET.numOfFailVerdicts;
        return count;
    }

    /**
     * Get the number of positive verdicts reported by the specified test-agent.
     */
    public int getNumberOfPassVerdictsSeen(String agentUniqueId) {
        EventTrace ET = perAgentEventTrace.get(agentUniqueId);
        if (ET == null)
            throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.");
        return ET.numOfPassVerdicts;
    }

    /**
     * Get the total number of positive verdicts reported by all test-agents.
     */
    public int getNumberOfPassVerdictsSeen() {
        int count = 0;
        for (EventTrace ET : perAgentEventTrace.values())
            count += ET.numOfPassVerdicts;
        return count;
    }

    /**
     * Get the number of undecided verdicts reported by the specified test-agent.
     */
    public int getNumberOfUndecidedVerdictsSeen(String agentUniqueId) {
        EventTrace ET = perAgentEventTrace.get(agentUniqueId);
        if (ET == null)
            throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.");
        return ET.numOfUndecidedVerdicts;
    }

    /**
     * Get the total number of undecided verdicts reported by all test-agents.
     */
    public int getNumberOfUndecidedVerdictsSeen() {
        int count = 0;
        for (EventTrace ET : perAgentEventTrace.values())
            count += ET.numOfUndecidedVerdicts;
        return count;
    }

    /**
     * Get the last negative verdict reported by the specified test-agent.
     */
    public VerdictEvent getLastFailVerdict(String agentUniqueId) {
        EventTrace ET = perAgentEventTrace.get(agentUniqueId);
        if (ET == null)
            throw new IllegalArgumentException("Agent " + agentUniqueId + " is unknown.");
        return ET.lastFailVerdict;
    }

    /**
     * Get the last reported negative verdict.
     */
    public VerdictEvent getLastFailVerdict() {
        VerdictEvent err = null;
        for (EventTrace ET : perAgentEventTrace.values()) {
            VerdictEvent err2 = ET.lastFailVerdict;
            if (err2 == null)
                continue;
            if (err == null)
                err = err2;
            else {
                if (err2.timestamp.isAfter(err.timestamp))
                    err = err2;
            }
        }
        return err;
    }

    @Override
    public String toString() {
        throw new UnsupportedOperationException("TODO");
    }

    @Override
    public Parsable parse(String s) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Save the data collected to files. Collected non-coverage events will be saved to the file
     * fname (if it is not null). Coverage data are saved to covFname, if the latter is not null. 
     * 
     * Format of the events-file:
     * 
     *    For verdict-event: agent,time,"VerdictEvent",family,info,verdict
     *    For scalar-event: agent,time."ScalarTracingEvent",propname1,val1,propname2,val2,...
     *    
     * Formal of coverage-file:
     *    
     *    coverage-point-name,num-of-total-visits,agent1,num-visit-by-agent1,agent2,...
     * 
     */
	public void save(String fname, String covFname) throws IOException {

		// Saving the events:

		if (fname != null) {
			DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss:SSS");
			List<String> agents = perAgentEventTrace.keySet().stream().collect(Collectors.toList());
			agents.sort(Comparator.comparing(String::toString));
			
			List<String[]> data = new LinkedList<>();

			for (var A : agents) {
				var trace = getTestAgentTrace(A);
				for (var event : trace) {
					List<String> row = new LinkedList<>();
					row.add(A); // agent namme
					row.add(timeFormatter.format(event.timestamp)); // time-stamp
					row.add(event.getClass().getSimpleName()); // event-type (e.g. verdict)
					if (event instanceof VerdictEvent) {
						row.add(event.familyName); // family
						if(event.info!=null) row.add(CSVUtility.cleanUpCommas(event.info)); // info
						row.add("" + ((VerdictEvent) event).verdict); // verdict
					}
					if (event instanceof ScalarTracingEvent) {
						ScalarTracingEvent e = (ScalarTracingEvent) event;
						List<String> keys = new LinkedList<>();
						keys.addAll(e.values.keySet());
						keys.sort(Comparator.comparing(String::toString));
						for (var key : keys) {
							row.add(key);
							row.add("" + e.values.get(key));
						}
					}
					data.add((String[]) row.toArray(new String[0]));
				}
				CSVUtility.exportToCSVfile(',', data, fname);
			}
		}

		// Saving the coverage data:
		if (covFname != null) {
			List<String> coveragePoints = collectiveCoverageMap.coverage.keySet().stream().collect(Collectors.toList());
			List<String> agents = perAgentCoverage.keySet().stream().collect(Collectors.toList());
			coveragePoints.sort(Comparator.comparing(String::toString));
			agents.sort(Comparator.comparing(String::toString));

			List<String[]> data = new LinkedList<>();

			for (var cp : coveragePoints) {
				List<String> row = new LinkedList<>();
				row.add(cp); // the name of coverage-point
				row.add("" + collectiveCoverageMap.coverage.get(cp)); // total number of visits
				for (var A : agents) {
					row.add(A);  // agent name
					row.add("" + perAgentCoverage.get(A).coverage.get(cp)); // #visits by A
				}
				data.add((String[]) row.toArray(new String[0]));
			}
			CSVUtility.exportToCSVfile(',', data, covFname);
		}
	}
    
	/**
	 * Reset the agents' traces recorded in this data-collector, and also reset all
	 * coverage information tracked by this data-collector (that means reseting their
	 * visit-counts to 0). 
	 */
	public void reset() {
		for (var tr : this.perAgentEventTrace.values()) {
			tr.reset(); 
		}
		for (var cmap : this.perAgentCoverage.values()) {
			cmap.reset(); 
		}
		this.collectiveCoverageMap.reset(); 
	}

    /**
     * Merge two sets of collected test data. The merged data is put into a new
     * instance of TestDataCollector.
     * 
     * @param data2 test-date to merge with this test-data.
     */
    public TestDataCollector merge(TestDataCollector data2) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("TODO");
    }

    /**
     * Read collected test data from a file.
     */
    public static TestDataCollector readCollectedTestData(File file) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("TODO");
    }
    
    /**
     * Export the scalar-trace of an agent to a CSV-file. ';' is used as the separator.
     */
    public void saveTestAgentScalarsTraceAsCSV(String agentUniqueId, String filename) throws IOException {
        
        List<ScalarTracingEvent> trace = getTestAgentScalarsTrace(agentUniqueId) ;
        
        Set<String> propertyNames = new HashSet<>() ;
        for(var e : trace) {
        	propertyNames.addAll(e.values.keySet()) ;
        }
                
        List<String> collumnNames = propertyNames.stream().collect(Collectors.toList()) ;
        collumnNames.sort(Comparator.comparing(s -> s.toString()));
        
        int N = collumnNames.size() ;
        String[] collumnNames_ = new String[N] ;
        int k=0 ;
        for(var name : collumnNames) {
        	collumnNames_[k] = name ;
        	k++ ;
        }
        
        List<Number[]> data = new LinkedList<>() ;
        
        for(ScalarTracingEvent e : trace) {
        	Number[] row = new Number[N] ;
        	for(k=0; k<N; k++) {
        		row[k] = e.values.get(collumnNames_[k]) ;
        	}
        	data.add(row) ;
        }
        
        CSVUtility.exportToCSVfile(',', collumnNames_, data, filename);
    }

}
