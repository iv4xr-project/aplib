package eu.iv4xr.framework.extensions.ltl;

import static eu.iv4xr.framework.extensions.ltl.LTL.eventually;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWState;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWTransition;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.LabRecruitsModel;
import nl.uu.cs.aplib.utils.Pair;
import org.junit.jupiter.api.Test;

/**
 * For testing bounded MC, in particular for cases where it needs to re-visit the same
 * state multiple cases to find a solution within the given max-depth..
 */
public class Test_BoundedMC {
	
	// removing non-functional button-tags to simplify the model
	GameWorldModel simplify(GameWorldModel model) {
		for(var Z : model.zones) {
			Z.members.removeIf(id -> 
				id.startsWith("R") || id.startsWith("X") || id.startsWith("C")
				//|| id.equals("d4")
					) ;
		}
		return model ;
	}
	
	String printState(GWState S) {
		StringBuffer z = new StringBuffer() ;
		z.append("Current LOC:" + S.currentAgentLocation) ;
		List<String> opendoors = new LinkedList<>() ;
		for (var o : S.objects.values()) {
			if (o.type.equals("Door")) {
				if (o.properties.get("isOpen").equals(true)) {
					opendoors.add(o.id) ;
				}
			}
		}
		z.append(", open-doors: " + opendoors) ;
		return z.toString() ;
	}
	
	String printAvailableTransitions(GameWorldModel model) {
		String s = "" ;
		int k = 0 ;
		for (var tr : model.availableTransitions()) {
			if (k>0) s += ", " ;
			var tr_ = (GWTransition) tr ;
			s += tr_.type + " -> " + tr_.target ;
			k++ ;
		}
		return s ;
	}

	GameWorldModel loadZenopus() throws IOException {
		var fname = Paths.get(System.getProperty("user.dir"),
				"src","test","sometestdata",
				"ruinzenopus_LR_2.json")
				.toString() ;
		GameWorldModel model = GameWorldModel.loadGameWorldModelFromFile(fname) ;
		model.reset();
		model = LabRecruitsModel.attachLabRecruitsAlpha(model) ;
		model = simplify(model) ;
		return model ;
	}

	void printSolutionPath(List path) {
		System.out.println(">>> solution path:") ;
		int k = 0 ;
		for (var step : path) {
			var step_ = (Pair) step ;
			System.out.print(">> step " + k + ":") ;
			GWTransition tr = (GWTransition) step_.fst ;
			if (tr!=null) System.out.print("" + tr.getId()) ;
			System.out.println("") ;
			k++ ;
		}
	}

	@Test
	public void test_boundedSimpleMC() throws IOException {

		GameWorldModel model = loadZenopus() ;

		var mc = new BasicModelChecker(model) ;
		//mc.completeBoundedDSFMode = true ;
		Predicate<IExplorableState> g  = S -> {
			GWState st = (GWState) S ;
			return st.currentAgentLocation.equals("dFN1") ;
		} ;

		var time = System.currentTimeMillis() ;
		var sequence = mc.find(g,40) ;
		time = System.currentTimeMillis() - time ;
		System.out.println(">>> time = " + time + " ms") ;
		System.out.println(">>> " + mc.stats) ;
		assertTrue(sequence != null) ;
		assertTrue(sequence.path.size() > 0) ;
		System.out.println(">>> solution: " + sequence.path.size()) ;
		printSolutionPath(sequence.path) ;
	}

	@Test
	public void test_boundedBuchiMC() throws IOException, InterruptedException {

		GameWorldModel model = loadZenopus() ;

		//System.out.println(">>> model: " + model) ;

		var mc = new BuchiModelChecker(model) ;
		//mc.completeBoundedDSFMode = true ;
		LTL<IExplorableState> goal = eventually(S -> {
			GWState st = (GWState) S ;
			//boolean dFN0isOpen = (Boolean) st.objects.get("dFN0").properties.get(GameWorldModel.IS_OPEN_NAME) ;
			//return dFN0isOpen && st.currentAgentLocation.equals("dFN0")  ;
			return st.currentAgentLocation.equals("dFN1")  ;
		});

		var time = System.currentTimeMillis() ;
		var sequence = mc.find(goal,40) ;
		time = System.currentTimeMillis() - time ;
		System.out.println(">>> time = " + time + " ms") ;
		System.out.println(">>> " + mc.stats) ;
		assertTrue(sequence != null) ;
		assertTrue(sequence.path.size() > 0) ;
		printSolutionPath(sequence.path) ;
	}

	//@Test
	public void test1() throws IOException, InterruptedException {
		
		GameWorldModel model = loadZenopus() ;

		//System.out.println(">>> model: " + model) ;

		var mc = new BuchiModelChecker(model) ;
		//var mc = new BasicModelChecker(model) ;
		mc.completeBoundedDSFMode = true ;
		LTL<IExplorableState> goal = eventually(S -> {
			GWState st = (GWState) S ;
			//boolean dFN0isOpen = (Boolean) st.objects.get("dFN0").properties.get(GameWorldModel.IS_OPEN_NAME) ;
			//return dFN0isOpen && st.currentAgentLocation.equals("dFN0")  ;
			return st.currentAgentLocation.equals("dFC")  ;
		});
		Predicate<IExplorableState> g  = S -> {
			GWState st = (GWState) S ;
			return st.currentAgentLocation.equals("Finish") ;
			//return st.currentAgentLocation.equals("dFC") ;
		} ;

		var time = System.currentTimeMillis() ;

		var sequence = mc.find(goal,40) ;

		//var sequence = mc.find(g,40) ;
		//var sequence = mc.iterativeFind(S -> g.test(S),60) ;
		
		time = System.currentTimeMillis() - time ;
		System.out.println(">>> time = " + time + " ms") ;
		System.out.println(">>> " + mc.stats) ;
		assertTrue(sequence != null) ;
		assertTrue(sequence.path.size() > 0) ;
		System.out.println(">>> solution: " + sequence.path.size()) ;
		printSolutionPath(sequence.path) ;

	}

}
