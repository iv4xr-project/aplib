package eu.iv4xr.framework.mainConcepts;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import nl.uu.cs.aplib.utils.Pair;
import nl.uu.cs.aplib.utils.Parsable;

/**
 * Observation-events represent observations that a test-agent
 * ({@link TestAgent}) collects. There are several types of observations, such
 * as verdict and coverage-event, represented by different subclasses of this
 * class.
 * 
 * @author Wish
 *
 */
public class ObservationEvent implements Serializable, Parsable {

    private static final long serialVersionUID = 1L;

    /**
     * A name to classify the event as belonging to some family of semantically
     * similar events.
     */
    protected String familyName;

    ObservationEvent() {
    }

    /**
     * Create an observation-event with the given family-name. Family-name is used
     * to identify that the event belongs to some meaningful family.
     */
    public ObservationEvent(String family) {
        if (family == null)
            throw new IllegalArgumentException("Trying to create an ObservationEvent with a null id.");
        this.familyName = family;
    }

    /**
     * Get the family-name of this observation-event.
     */
    public String getFamilyName() {
        return familyName;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof ObservationEvent) {
            return familyName.equals(((ObservationEvent) o).familyName);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return familyName.hashCode();
    }

    @Override
    public String toString() {
        return familyName;
    }

    public ObservationEvent parse(String s) {
        if (s == null)
            throw new IllegalArgumentException("Trying to parse an ObservationEvent from a null string.");
        return new ObservationEvent(s);
    }

    /**
     * When performing a series of tests, we usually want to know how 'complete' the
     * tests were. Of course testing cannot completely guarantee absence of bugs,
     * but we can still measure relative completeness with respect to a set of
     * coverage-checkpoint (or simply coverage-point), each representing certain
     * family of behaviors of the program-under-test. We can think this check-point
     * as a certain point in the execution of the program-under-test. All executions
     * that visit this check-point is thus the family of the behavior that the
     * check-point represents.
     * 
     * A set of tests (also called a test-suite) is said to be relatively
     * complete/adequate if the tests in this set activates/visits the
     * coverage-points that we set out for the program-under-test.
     * 
     * <p>
     * The tester/developer should determine what which coverage points to
     * cover when testing a given program-under-test. There should also be a way for
     * a test-agent to observe that it visits such a coverage point. When this
     * happen, the test agent can report this as an instance of this class
     * CoveragePointEvent.
     * 
     * @author Wish
     *
     */
    public static class CoveragePointEvent extends ObservationEvent {

        /**
         * A unique ID that identifies the covered coverage-point.
         */
        public String coveragePointId ;
        
        /**
         * Create a coverage-event representing that we just covered the
         * coverage-point specified by its id. We also specify the family
         * to which this event belongs to. Family-name is used to
         * identify that the event belongs to some meaningful family.
         */
        public CoveragePointEvent(String family, String coveragePointId) {
            super(family);
            this.coveragePointId = coveragePointId ;
        }

    }

    /**
     * This class is used to represent observation-events that need to be
     * time-stamped.
     */
    public static class TimeStampedObservationEvent extends ObservationEvent {

        private static final long serialVersionUID = 1L;

        protected LocalTime timestamp;

        /**
         * Further information about this event. It can be null, or something that is
         * not an empty string.
         */
        protected String info;

        TimeStampedObservationEvent() {
            super();
        }

        /**
         * Create a time-stamped observation-event. The stamped time is the time of this
         * creation.
         * 
         * @param familyName A name to classify this event as belonging to the said
         *                   family.
         * @param info       Additional information to describe the event.
         */
        public TimeStampedObservationEvent(String familyName, String info) {
            super(familyName);
            timestamp = LocalTime.now();
            if (info != null && info.length() == 0)
                throw new IllegalArgumentException("The info part cannot be empty.");
            this.info = info;
        }

        /**
         * Create a time-stamped observation-event with the given id. The stamped time
         * is the time of this creation.
         */
        public TimeStampedObservationEvent(String familyName) {
            this(familyName, null);
        }

        /**
         * Get the event time-stamp.
         */
        public LocalTime getTimestamp() {
            return timestamp;
        }

        /**
         * Get the event's description string, if it has any (else it returns a null).
         */
        public String getInfo() {
            return info;
        }

        @Override
        /**
         * Two time-stamped events are equal if their id and time stamps are equal.
         */
        public boolean equals(Object o) {
            if (o instanceof TimeStampedObservationEvent) {
                var o_ = (TimeStampedObservationEvent) o;
                return familyName.equals(o_.familyName) && timestamp.equals(o_.timestamp);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(familyName, timestamp);
        }

        @Override
        public String toString() {
            var info_ = info;
            if (info_ == null)
                info_ = "null";
            return familyName + ";" + timestamp + ";" + info_;
        }

        @Override
        public TimeStampedObservationEvent parse(String s) {
            if (s == null)
                throw new IllegalArgumentException(
                        "Trying to parse an TimeStampedObservationEvent from a null string.");
            var parts = s.split(";");
            if (parts.length != 3)
                throw new IllegalArgumentException("Parse error on: " + s);
            return parseWorker(parts);
        }

        TimeStampedObservationEvent parseWorker(String[] parts) {
            TimeStampedObservationEvent o = new TimeStampedObservationEvent();
            o.familyName = parts[0];
            o.timestamp = LocalTime.parse(parts[1]);
            o.info = parts[2];
            if (o.info.equals("null"))
                o.info = null;
            return o;
        }
    }

    /**
     * When a test-agent interacts with a system-under-test, it will want to report
     * things that meet its expectations, as well as things that violate its
     * expectations (and may thus imply bugs). Such discoveries can be reported as
     * instances of this class VerdictEvent.
     * 
     */
    public static class VerdictEvent extends TimeStampedObservationEvent {

        /**
         * A "true" represents a positive verdict, which should be given when a
         * test-agent observes something that is correct, as it should be. A "false" on
         * the other hand, represents a negative verdict, which should be given when the
         * test-agent observes something that violates correctness. Null represents
         * undecided.
         */
        protected Boolean verdict = null;

        VerdictEvent() {
            super();
        }

        /**
         * Create a verdict-event.
         * 
         * @param familyName A name to classify the verdict-family this verdict can be
         *                   said to belong to.
         * @param info       Additional descriptive string explaining the verdict.
         * @param v          true if the verdict is positive, false if negative, null if
         *                   undecided.
         */
        public VerdictEvent(String familyName, String info, Boolean v) {
            super(familyName, info);
            if (v != null) {
                if (v.booleanValue())
                    verdict = true;
                else
                    verdict = false;
            }
        }

        /**
         * True if this verdict is a positive verdict.
         */
        public boolean isPass() {
            return verdict != null && verdict.booleanValue();
        }

        /**
         * True if this verdict is a negative verdict.
         */
        public boolean isFail() {
            return verdict != null && !verdict.booleanValue();
        }

        /**
         * True if this verdict is neither positive nor negative.
         * 
         * @return
         */
        public boolean isUndecided() {
            return verdict == null;
        }

        @Override
        /**
         * Two verdicts events are equal if their id, their time stamps, and their
         * verdicts are equal.
         */
        public boolean equals(Object o) {
            if (super.equals(o) && o instanceof VerdictEvent) {
                var o_ = (VerdictEvent) o;
                if (verdict == null)
                    return o_.verdict == null;
                if (o_.verdict == null)
                    return verdict == null;
                return verdict.equals(o_.verdict);
            }
            return false;
        }

        @Override
        public int hashCode() {
            return Objects.hash(familyName, timestamp, verdict);
        }

        @Override
        public String toString() {
            var s = super.toString();
            var v = "false";
            if (verdict == null)
                v = "null";
            else if (verdict.booleanValue())
                v = "true";
            return s + ";" + v;
        }

        @Override
        public VerdictEvent parse(String s) {
            if (s == null)
                throw new IllegalArgumentException("Trying to parse an VerdictEvent from a null string.");
            var parts = s.split(";");
            if (parts.length != 4)
                throw new IllegalArgumentException("Parse error on: " + s);
            TimeStampedObservationEvent e = parseWorker(parts);
            VerdictEvent o = new VerdictEvent();
            o.familyName = e.familyName;
            o.timestamp = e.timestamp;
            o.info = e.info;
            String v = parts[3];
            if (v.equals("null"))
                o.verdict = null;
            else if (v.equals("true"))
                o.verdict = true;
            else
                o.verdict = false;
            return o;
        }
    }
    
    /**
     * This event is used to record a vector of (p,vs) where p is an agent's current
     * position and vs is a bunch of scalar-values, organized as name-value pairs.
     */
    public static class ScalarTracingEvent extends TimeStampedObservationEvent {
        
        public Map<String,Number> values = new HashMap<>();
        
        public ScalarTracingEvent(Pair<String,Number> ... nameValuePairs) {
            super("Tracing",null);
            for(var nameValue : nameValuePairs) {
                values.put(nameValue.fst, nameValue.snd) ;
            }
        }
    }

}