package eu.iv4xr.framework.extensions.mbt;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Random;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.mbt.Test_MBT.MyAgentState;
import eu.iv4xr.framework.mainConcepts.TestAgent;

public class Test_ModelSaveAndModelTransitions {
	
	static String saveFile = "tmp/model1.json" ;
	
	@Test
	public void test_save_and_load_transitions() throws IOException {
		
		var mymodel = (new Test_MBT()).mkModel1() ;
		assertTrue(mymodel.getAllTransitions().size() == 0) ;
		var mystate = new MyAgentState(new SimpleGame()) ;
		var agent = new TestAgent() ;
		agent.attachState(mystate) ;
		var runner = new MBTRunner<MyAgentState>(mymodel) ;
		runner.rnd = new Random() ;
		runner.inferTransitions = true ;
		runner.stopSuiteGenerationOnFailedOrViolation = false ;
		var results = runner.generate(
				dummy -> { mystate.G = new SimpleGame(); return agent ;}, 
				20, 
				10) ;
		assertTrue(mymodel.getAllTransitions().size() >= 2) ;
		System.out.println(mymodel) ;
		mymodel.saveTransitions(saveFile);
		
		var mymodel2 = (new Test_MBT()).mkModel1() ;
		assertTrue(mymodel2.getAllTransitions().size() == 0) ;
		mymodel2.loadTransitionsFromFile(saveFile,true);
		assertTrue(mymodel2.getAllTransitions().size() == 5) ;
		//mymodel2.saveDot("tmp/model1.dot");
		System.out.println(mymodel2) ;	
		
	}
	
	@AfterAll
	static public void cleanup() {
		System.out.println(">>> cleaning up file " + saveFile) ;
		File f = new File(saveFile); 
		f.delete() ;
	}
	
}
