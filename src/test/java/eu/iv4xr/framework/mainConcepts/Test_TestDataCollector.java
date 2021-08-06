package eu.iv4xr.framework.mainConcepts;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.junit.jupiter.api.Test;

import nl.uu.cs.aplib.utils.CSVUtility;
import nl.uu.cs.aplib.utils.Pair;

public class Test_TestDataCollector {
	
	@Test
	public void test_verdict_tracking() {
		var collector = new TestDataCollector() ;
		var verdictFail = new ObservationEvent.VerdictEvent("family","infobla",false) ;
		var verdictSuccess = new ObservationEvent.VerdictEvent("family","infobla",true) ;
		var verdictUndecided = new ObservationEvent.VerdictEvent("family","infobla",null) ;
		
		collector.registerTestAgent("agent1");
		collector.registerTestAgent("agent2");
		collector.registerEvent("agent1", verdictFail);
		collector.registerEvent("agent1", verdictSuccess);
		collector.registerEvent("agent1", verdictSuccess);
		collector.registerEvent("agent1", verdictUndecided);
		
		collector.registerEvent("agent2", verdictFail);
		

		assertTrue(collector.getNumberOfPassVerdictsSeen("agent1") == 2) ;
		assertTrue(collector.getNumberOfFailVerdictsSeen("agent1") == 1) ;
		assertTrue(collector.getNumberOfUndecidedVerdictsSeen("agent1") == 1) ;
		
		assertTrue(collector.getNumberOfPassVerdictsSeen() == 2) ;
		assertTrue(collector.getNumberOfFailVerdictsSeen() == 2) ;
		assertTrue(collector.getNumberOfUndecidedVerdictsSeen() == 1) ;
		
		assertTrue(collector.getLastFailVerdict() == verdictFail) ;
		assertTrue(collector.getLastFailVerdict("agent1") == verdictFail) ;
		assertTrue(collector.getLastFailVerdict("agent2") == verdictFail) ;
		
	}
	
	
	@Test
	public void test_coverage_tracking() {
		var collector = new TestDataCollector() ;
		var covPoint1 = new ObservationEvent.CoveragePointEvent("family","button1") ;
		var covPoint2 = new ObservationEvent.CoveragePointEvent("family","button2") ;
		var covPoint3 = new ObservationEvent.CoveragePointEvent("family","button3") ;
		
		collector.registerTestAgent("agent1") ;
		collector.registerTestAgent("agent2") ;
		
		collector.startTrackingCoveragePoint("button1");
		collector.startTrackingCoveragePoint("button2");
		collector.startTrackingCoveragePoint("button3");
		collector.startTrackingCoveragePoint("button4");
		
		collector.registerVisit("agent1",covPoint1) ;
		collector.registerVisit("agent1", new ObservationEvent.CoveragePointEvent("family","button1")) ;


		collector.registerVisit("agent2",covPoint1) ;
		collector.registerVisit("agent2",covPoint2) ;

		
		var coverage_A1 = collector.getTestAgentCoverage("agent1") ;
		var coverage    = collector.getCollectiveCoverage() ;
		
		assertTrue(coverage_A1.get("button1") == 2) ;
		assertTrue(coverage_A1.get("button3") == 0) ;

		assertTrue(coverage.get("button1") == 3) ;
		assertTrue(coverage.get("button2") == 1) ;
		assertTrue(coverage.get("button3") == 0) ;
		assertTrue(coverage.get("button4") == 0) ;
	}
	
	@Test
	public void test_scalar_tracing_and_saving() throws IOException {

		var collector = new TestDataCollector();

		collector.registerTestAgent("agent1");
		collector.registerTestAgent("agent2");
		collector.startTrackingCoveragePoint("button1");
		collector.startTrackingCoveragePoint("button2");
		collector.startTrackingCoveragePoint("button3");

		collector.registerEvent("agent1", new ObservationEvent.ScalarTracingEvent(new Pair("x", 0), new Pair("y", 0)));
		collector.registerEvent("agent1", new ObservationEvent.ScalarTracingEvent(new Pair("x", 1), new Pair("y", 0)));
		collector.registerEvent("agent1", new ObservationEvent.ScalarTracingEvent(new Pair("x", 1), new Pair("y", 1)));
		collector.registerEvent("agent2", new ObservationEvent.ScalarTracingEvent(new Pair("x", 2), new Pair("y", 2)));

		// System.out.println(collector.getTestAgentTrace("agent2")) ;
		String file1 = "target/tracefile1.csv";
		String file2 = "target/tracefile2.csv";
		collector.saveTestAgentScalarsTraceAsCSV("agent1",file1);
		collector.saveTestAgentScalarsTraceAsCSV("agent2",file2);
		
		var data1 = CSVUtility.readCSV(',', file1);
		var data2 = CSVUtility.readCSV(',', file2);

		assertEquals(4, data1.size());
		assertEquals(2, data2.size());
		
		assertEquals("[x, y]", "" + Arrays.asList(data1.get(0))) ;
		assertEquals("[0, 0]", "" + Arrays.asList(data1.get(1))) ;
		assertEquals("[2, 2]", "" + Arrays.asList(data2.get(1))) ;

		// cleaning the created files
		Files.delete(Paths.get(file1));
		Files.delete(Paths.get(file2));
	}
	
	@Test
	public void test_saving_collecteddata() throws IOException {
		
		var collector = new TestDataCollector() ;
		
		collector.registerTestAgent("agent1") ;
		collector.registerTestAgent("agent2") ;
		collector.startTrackingCoveragePoint("button1");
		collector.startTrackingCoveragePoint("button2");
		collector.startTrackingCoveragePoint("button3");
		
		collector.registerEvent("agent1", new ObservationEvent.ScalarTracingEvent(new Pair("x",0), new Pair("y",0)));
		collector.registerEvent("agent1", new ObservationEvent.ScalarTracingEvent(new Pair("x",1), new Pair("y",0)));
		collector.registerEvent("agent1", new ObservationEvent.ScalarTracingEvent(new Pair("x",1), new Pair("y",1)));
		collector.registerEvent("agent2", new ObservationEvent.ScalarTracingEvent(new Pair("x",2), new Pair("y",2)));
		
		collector.registerEvent("agent1", new ObservationEvent.VerdictEvent("check inv1","infobla",true));
		collector.registerEvent("agent1", new ObservationEvent.VerdictEvent("check inv2","infobla",false));
		collector.registerEvent("agent2", new ObservationEvent.VerdictEvent("check inv2","infobla",null));
		

		
		collector.registerVisit("agent1", new ObservationEvent.CoveragePointEvent("family","button1")) ;
		collector.registerVisit("agent1", new ObservationEvent.CoveragePointEvent("family","button1")) ;


		collector.registerVisit("agent2",new ObservationEvent.CoveragePointEvent("family","button1")) ;
		collector.registerVisit("agent2",new ObservationEvent.CoveragePointEvent("family","button2")) ;
		
		//System.out.println(collector.getTestAgentTrace("agent2")) ;
		String file    = "target/tracefile.csv" ;
		String covFile = "target/covfile.csv" ;
		collector.save(file,covFile);
		
		var data = CSVUtility.readCSV(',',file) ;
		var covdata = CSVUtility.readCSV(',',covFile) ;
		
		assertEquals(7, data.size()) ;
		assertEquals(3, covdata.size()) ;
		
		// cleaning the created files
		Files.delete(Paths.get(file));
		Files.delete(Paths.get(covFile));
	}
		
	
	
	//@Test
	public void for_generating_traces() throws IOException {
		var collector = new TestDataCollector();
		collector.registerTestAgent("agent1");

		collector.registerEvent("agent1", mockScalarEvent(0, 0, 0, 0, 20));
		collector.registerEvent("agent1", mockScalarEvent(1, 0, 0, 1, 40));
		collector.registerEvent("agent1", mockScalarEvent(2, 0, 0, 2, 40));
		collector.registerEvent("agent1", mockScalarEvent(2, 0, 1, 3, 20));
		collector.registerEvent("agent1", mockScalarEvent(2, 0, 2, 4, 80));
		collector.registerEvent("agent1", mockScalarEvent(2, 0, 1, 5, 10));
		collector.registerEvent("agent1", mockScalarEvent(3, 0, 1, 6, 200));
		collector.registerEvent("agent1", mockScalarEvent(3, 0, 2, 7, 100));
		collector.registerEvent("agent1", mockScalarEvent(3, 0, 3, 8, 20));
		collector.registerEvent("agent1", mockScalarEvent(3, 0, 4, 9, 20));

		// System.out.println(collector.getTestAgentTrace("agent2")) ;
		String file = "target/sampleTracefile.csv";
		collector.saveTestAgentScalarsTraceAsCSV("agent1", file);
	}
	
	private ObservationEvent.ScalarTracingEvent mockScalarEvent(int x, int y, int z, int t, float value) {
		Pair<String,Number>[] data = new Pair[5] ;
		data[0] = new Pair("posx",(Number) x) ;	
		data[1] = new Pair("posy",(Number) y) ;	
		data[2] = new Pair("posz",(Number) z) ;	
		data[3] = new Pair("turn",(Number) t) ;
		data[4] = new Pair("val",(Number) value) ;
		return new ObservationEvent.ScalarTracingEvent(data) ;
	}

}
