package eu.iv4xr.framework.mainConcepts;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import eu.iv4xr.framework.mainConcepts.TestDataCollector;

import static eu.iv4xr.framework.mainConcepts.ObservationEvent.*;
import static org.junit.jupiter.api.Assertions.*;

public class Test_TestAgent {

    @Test
    public void test_registerToDataCollector() {
        var agent1 = new TestAgent("A1", null);
        var agent2 = new TestAgent("A2", null);

        var datacollector = new TestDataCollector();
        assertTrue(datacollector.getNumberOfFailVerdictsSeen() == 0);
        assertTrue(datacollector.getNumberOfPassVerdictsSeen() == 0);
        assertTrue(datacollector.getNumberOfUndecidedVerdictsSeen() == 0);
        assertTrue(datacollector.getLastFailVerdict() == null);
        assertTrue(datacollector.getCollectiveCoverage().entrySet().isEmpty());

        agent1.setTestDataCollector(datacollector);
        agent2.setTestDataCollector(datacollector);
        assertTrue(agent1.getTestDataCollector() == datacollector);
        assertTrue(agent2.getTestDataCollector() == datacollector);
    }

    @Test
    public void test_registerVerdict() {
        var agent1 = new TestAgent("A1", null);
        var agent2 = new TestAgent("A2", null);
        var datacollector = new TestDataCollector();
        agent1.setTestDataCollector(datacollector);
        agent2.setTestDataCollector(datacollector);

        agent1.registerVerdict(new VerdictEvent("inv0", "some info", true));
        assertTrue(datacollector.getNumberOfFailVerdictsSeen() == 0);
        assertTrue(datacollector.getNumberOfPassVerdictsSeen() == 1);
        assertTrue(datacollector.getNumberOfPassVerdictsSeen("A1") == 1);
        assertTrue(datacollector.getNumberOfPassVerdictsSeen("A2") == 0);
        assertTrue(datacollector.getNumberOfUndecidedVerdictsSeen() == 0);
        assertTrue(datacollector.getLastFailVerdict() == null);
        assertTrue(datacollector.getCollectiveCoverage().entrySet().isEmpty());

        agent2.registerVerdict(new VerdictEvent("inv0", "some info", false));
        assertTrue(datacollector.getNumberOfFailVerdictsSeen() == 1);
        assertTrue(datacollector.getNumberOfFailVerdictsSeen("A1") == 0);
        assertTrue(datacollector.getNumberOfFailVerdictsSeen("A2") == 1);
        assertTrue(datacollector.getNumberOfPassVerdictsSeen() == 1);
        assertTrue(datacollector.getNumberOfPassVerdictsSeen("A1") == 1);
        assertTrue(datacollector.getNumberOfPassVerdictsSeen("A2") == 0);
        assertTrue(datacollector.getNumberOfUndecidedVerdictsSeen() == 0);
        assertTrue(datacollector.getLastFailVerdict().familyName.equals("inv0"));
        assertTrue(datacollector.getCollectiveCoverage().entrySet().isEmpty());

    }

    @Test
    public void test_registerEvent() {
        var agent1 = new TestAgent("A1", null);
        var agent2 = new TestAgent("A2", null);
        var datacollector = new TestDataCollector();
        agent1.setTestDataCollector(datacollector);
        agent2.setTestDataCollector(datacollector);

        assertTrue(datacollector.getTestAgentTrace("A1").isEmpty());
        assertTrue(datacollector.getTestAgentTrace("A1").isEmpty());

        agent1.registerEvent(new TimeStampedObservationEvent("giggle"));
        agent2.registerEvent(new TimeStampedObservationEvent("laugh"));
        agent1.registerEvent(new TimeStampedObservationEvent("frown"));
        agent2.registerEvent(new TimeStampedObservationEvent("cry"));

        assertTrue(datacollector.getTestAgentTrace("A1").size() == 2);
        assertTrue(datacollector.getTestAgentTrace("A1").get(0).familyName.equals("giggle"));
        assertTrue(datacollector.getTestAgentTrace("A1").get(1).familyName.equals("frown"));

        assertTrue(datacollector.getTestAgentTrace("A2").size() == 2);
        assertTrue(datacollector.getTestAgentTrace("A2").get(0).familyName.equals("laugh"));
        assertTrue(datacollector.getTestAgentTrace("A2").get(1).familyName.equals("cry"));
    }

    @Test
    public void test_registerCoverageVisit() {
        var agent1 = new TestAgent("A1", null);
        var agent2 = new TestAgent("A2", null);
        var datacollector = new TestDataCollector();
        agent1.setTestDataCollector(datacollector);
        agent2.setTestDataCollector(datacollector);

        String p1 = "p1" ;
        String p2 = "p2" ;
        datacollector.startTrackingCoveragePoint(p1);
        datacollector.startTrackingCoveragePoint(p2);
        assertTrue(datacollector.getCollectiveCoverage().get(p1) == 0);
        assertTrue(datacollector.getTestAgentCoverage("A1").get(p1) == 0);
        assertTrue(datacollector.getTestAgentCoverage("A2").get(p1) == 0);
        
        datacollector.registerVisit("A1", new CoveragePointEvent("coverage","p1"));
        
        assertTrue(datacollector.getCollectiveCoverage().get(p1) == 1);
        assertTrue(datacollector.getTestAgentCoverage("A1").get(p1) == 1);
        assertTrue(datacollector.getTestAgentCoverage("A2").get(p1) == 0);
        
        assertTrue(datacollector.getCollectiveCoverage().get(p2) == 0);
        assertTrue(datacollector.getTestAgentCoverage("A1").get(p2) == 0);
        assertTrue(datacollector.getTestAgentCoverage("A2").get(p2) == 0);

        datacollector.registerVisit("A2", new CoveragePointEvent("coverage","p1"));
        
        assertTrue(datacollector.getCollectiveCoverage().get(p1) == 2);
        assertTrue(datacollector.getTestAgentCoverage("A1").get(p1) == 1);
        assertTrue(datacollector.getTestAgentCoverage("A2").get(p1) == 1);
        
        assertTrue(datacollector.getCollectiveCoverage().get(p2) == 0);
        assertTrue(datacollector.getTestAgentCoverage("A1").get(p2) == 0);
        assertTrue(datacollector.getTestAgentCoverage("A2").get(p2) == 0);
        
        datacollector.registerVisit("A1", new CoveragePointEvent("coverage","p2"));
        
        assertTrue(datacollector.getCollectiveCoverage().get(p1) == 2);
        assertTrue(datacollector.getTestAgentCoverage("A1").get(p1) == 1);
        assertTrue(datacollector.getTestAgentCoverage("A2").get(p1) == 1);
        
        assertTrue(datacollector.getCollectiveCoverage().get(p2) == 1);
        assertTrue(datacollector.getTestAgentCoverage("A1").get(p2) == 1);
        assertTrue(datacollector.getTestAgentCoverage("A2").get(p2) == 0);
        
    }

}
