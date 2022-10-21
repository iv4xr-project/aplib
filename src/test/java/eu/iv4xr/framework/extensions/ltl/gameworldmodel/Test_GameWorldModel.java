package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.AfterAll;

import com.google.gson.JsonIOException;

import eu.iv4xr.framework.extensions.ltl.BuchiModelChecker;
import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.extensions.ltl.LTL;

import static eu.iv4xr.framework.extensions.ltl.LTL.eventually;
import static org.junit.jupiter.api.Assertions.*;

import java.io.File;
import java.io.IOException;

public class Test_GameWorldModel {
	
	@AfterAll
	static public void cleanup() {
		File fmodel0 = new File("model0.json"); 
		fmodel0.delete() ;
	}
	
	@Test
	public void test_save_and_load() throws JsonIOException, IOException {
		var model0 = LabRecruitsModel.mk_ButtonDoor1Level() ;
		model0.save("model0.json");
		
		GameWorldModel model1 = GameWorldModel.loadGameWorldModelFromFile("model0.json") ;
		assertTrue(model1.initialState.equals(model0.initialState)) ;
		assertTrue(model1.blockers.equals(model0.blockers)) ;
		assertTrue(model1.zones.equals(model0.zones)) ;
		assertTrue(model1.objectlinks.equals(model0.objectlinks)) ;
	}
	
	
	@Test
	public void test_reset()  {
		var model0 = LabRecruitsModel.mk_ButtonDoor1Level() ;
		model0.travelTo("button1");
		model0.interact("button1");
		assertEquals(3,model0.history.size()) ;
		assertEquals("button1",model0.getCurrentState().currentAgentLocation) ;
		GWObject d1 = model0.getCurrentState().objects.get("door1") ;
		boolean isOpen = (Boolean) d1.properties.get("isOpen") ;
		assertEquals(true,isOpen) ;
		
		model0.reset(); 
		assertEquals(1,model0.history.size()) ;
		assertEquals("button2",model0.getCurrentState().currentAgentLocation) ;
		assertTrue(model0.getCurrentState().equals(model0.initialState)) ;
	}
	
	
	@Test
	public void test_bactrack()  {
		var model0 = LabRecruitsModel.mk_ButtonDoor1Level() ;
		model0.travelTo("button1");
		model0.interact("button1");
		assertEquals(3,model0.history.size()) ;
		assertEquals("button1",model0.getCurrentState().currentAgentLocation) ;
		GWObject d1 = model0.getCurrentState().objects.get("door1") ;
		boolean isOpen = (Boolean) d1.properties.get("isOpen") ;
		assertEquals(true,isOpen) ;
		
		model0.backTrackToPreviousState() ;
		assertEquals(2,model0.history.size()) ;
		assertEquals("button1",model0.getCurrentState().currentAgentLocation) ;
		d1 = model0.getCurrentState().objects.get("door1") ;
		isOpen = (Boolean) d1.properties.get("isOpen") ;
		assertEquals(false,isOpen) ;
		
		model0.backTrackToPreviousState() ;
		assertEquals(1,model0.history.size()) ;
		assertEquals("button2",model0.getCurrentState().currentAgentLocation) ;
		d1 = model0.getCurrentState().objects.get("door1") ;
		isOpen = (Boolean) d1.properties.get("isOpen") ;
		assertEquals(false,isOpen) ;
		assertTrue(model0.getCurrentState().equals(model0.initialState)) ;
	}
	
	@Test
	public void test_longer_interactions() {
		var buttondoor1 = LabRecruitsModel.mk_ButtonDoor1Level() ;
		buttondoor1.travelTo("button1");
		//System.out.println(buttondoor1.toString()) ;
		buttondoor1.interact("button1");
		buttondoor1.travelTo("door1");
		buttondoor1.travelTo("button3");
		buttondoor1.interact("button3");
		buttondoor1.travelTo("door2");
		buttondoor1.travelTo("button4");
		buttondoor1.interact("button4");
		buttondoor1.travelTo("door2");
		buttondoor1.travelTo("door1");
		buttondoor1.travelTo("door3");
		
		assertEquals("door3",buttondoor1.getCurrentState().currentAgentLocation) ;
		GWObject d1 = buttondoor1.getCurrentState().objects.get("door1") ;
		GWObject d2 = buttondoor1.getCurrentState().objects.get("door2") ;
		GWObject d3 = buttondoor1.getCurrentState().objects.get("door3") ;
		boolean d1_isOpen = (Boolean) d1.properties.get("isOpen") ;
		boolean d2_isOpen = (Boolean) d1.properties.get("isOpen") ;
		boolean d3_isOpen = (Boolean) d1.properties.get("isOpen") ;
		assertTrue(d1_isOpen && d2_isOpen && d3_isOpen) ;
		
		assertTrue(! buttondoor1.isBlocking("door1")
				&& ! buttondoor1.isBlocking("door2")
				&& ! buttondoor1.isBlocking("door3")) ;
	
	}
	
	@Test
	public void test_MC() {
		var buttondoor1 = LabRecruitsModel.mk_ButtonDoor1Level() ;
		var mc = new BuchiModelChecker(buttondoor1) ;
		LTL<IExplorableState> solved = eventually(S -> {
			GWState st = (GWState) S ;
			boolean door3isOpen = (Boolean) st.objects.get("door3").properties.get(GameWorldModel.IS_OPEN_NAME) ;
			return st.currentAgentLocation.equals("door3") && door3isOpen ;
		});
		var sequence = mc.find(solved,13) ;
		
		assertTrue(sequence.path.size() > 0) ;
		assertEquals("door3",buttondoor1.getCurrentState().currentAgentLocation) ;
		assertTrue(! buttondoor1.isBlocking("door1")
				&& ! buttondoor1.isBlocking("door2")
				&& ! buttondoor1.isBlocking("door3")) ;
		
		System.out.println(mc.stats.toString()) ;
		System.out.println(">>> solution length: " + sequence.path.size()) ;
		int k = 0 ;
		for (var step : sequence.path) {
			System.out.print(">> step " + k + ":") ;
			GWTransition tr = (GWTransition) step.fst ;
			if (tr!=null) System.out.print("" + tr.getId()) ;
			System.out.println("") ;
			k++ ;	
		}
	}
	
}
