package eu.iv4xr.framework.extensions.ltl;

import static eu.iv4xr.framework.extensions.ltl.LTL.eventually;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import eu.iv4xr.framework.extensions.ltl.BasicModelChecker;
import eu.iv4xr.framework.extensions.ltl.BasicModelChecker.Path;
import eu.iv4xr.framework.extensions.ltl.BuchiModelChecker;
import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.extensions.ltl.LTL;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWState;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWTransition;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWTransition.GWTransitionType;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.LabRecruitsModel;

/**
 * For testing bounded MC performance.
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

	
	//@Test
	public void test1() throws IOException, InterruptedException {
		
		var fname = Paths.get(System.getProperty("user.dir"),
				"src","test","sometestdata",
				"ruinzenopus_LR_2.json")
				.toString() ;
		
		GameWorldModel model = GameWorldModel.loadGameWorldModelFromFile(fname) ;
		model.reset();
		model = LabRecruitsModel.attachLabRecruitsAlpha(model) ;
		model = simplify(model) ;
		
		System.out.println(">>> model: " + model) ;
		
		/*
		// manually debugging the model:
		
		int k = 0;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		
		GWTransition tr = new GWTransition(GWTransitionType.TRAVEL,"b0") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canTravelTo(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;

		tr = new GWTransition(GWTransitionType.INTERACT,"b0") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canInteract(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;

		tr = new GWTransition(GWTransitionType.TRAVEL,"d0") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canTravelTo(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;

		
		tr = new GWTransition(GWTransitionType.TRAVEL,"b1") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canTravelTo(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;
		
		tr = new GWTransition(GWTransitionType.INTERACT,"b1") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canInteract(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;

		tr = new GWTransition(GWTransitionType.TRAVEL,"d1") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canTravelTo(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;
		
		tr = new GWTransition(GWTransitionType.TRAVEL,"b3") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canTravelTo(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;
		
		tr = new GWTransition(GWTransitionType.INTERACT,"b3") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canInteract(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;

		tr = new GWTransition(GWTransitionType.TRAVEL,"d3") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canTravelTo(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;
		
		tr = new GWTransition(GWTransitionType.TRAVEL,"bJS") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canTravelTo(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;
		
		tr = new GWTransition(GWTransitionType.INTERACT,"bJS") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canInteract(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;

		tr = new GWTransition(GWTransitionType.TRAVEL,"dJS") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canTravelTo(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;

		tr = new GWTransition(GWTransitionType.TRAVEL,"bJE") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canTravelTo(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;
		
		tr = new GWTransition(GWTransitionType.INTERACT,"bJE") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canInteract(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;

		tr = new GWTransition(GWTransitionType.TRAVEL,"dJE") ;
		System.out.println("  TR:" + tr.type + "-->" + tr.target + ", allowed:" + model.canTravelTo(tr.target)) ;
		model.execute(tr); k++ ;
		System.out.println(">>> k=" + k + ", state:" + printState((GWState) model.history.get(0).fst)) ;
		System.out.println(">>> available trans:" + printAvailableTransitions(model) + "\n") ;
		
		*/

			 
		//var mc = new BuchiModelChecker(model) ;
		var mc = new BasicModelChecker(model) ;
		mc.findShortestMode = true ;
		LTL<IExplorableState> solved = eventually(S -> {
			GWState st = (GWState) S ;
			//boolean dFN0isOpen = (Boolean) st.objects.get("dFN0").properties.get(GameWorldModel.IS_OPEN_NAME) ;
			//return dFN0isOpen && st.currentAgentLocation.equals("dFN0")  ;
			return st.currentAgentLocation.equals("dJE")  ;
		});
		Predicate<IExplorableState> g  = S -> {
			GWState st = (GWState) S ;
			return st.currentAgentLocation.equals("Finish") ;
		} ;
		
		
		var time = System.currentTimeMillis() ;
		
		/*
		Path<IExplorableState>[] outs = new Path[1] ;
		Thread th = new Thread(() -> { outs[0] = mc.find(g,60) ;} ) ;
		th.start();
		Thread.sleep(1800000);
		th.stop();
		Path<IExplorableState> sequence = outs[0] ;
		*/
		
		var sequence = mc.find(g,55) ;
		
		time = System.currentTimeMillis() - time ;
		System.out.println(">>> time = " + time + " ms") ;
		System.out.println(">>> " + mc.stats) ;
		assertTrue(sequence != null) ;
		assertTrue(sequence.path.size() > 0) ;
		System.out.println(">>> solution: " + sequence.path.size()) ;
		int k = 0 ;
		for (var step : sequence.path) {
			System.out.print(">> step " + k + ":") ;
			GWTransition tr = (GWTransition) step.fst ;
			if (tr!=null) System.out.print("" + tr.getId()) ;
			System.out.println("") ;
			k++ ;	
		}
		/*
		
		for (k = model.history.size()-1; 0<=k; k--) {
			int i = model.history.size() - (k + 1) ;
			var tr = model.history.get(k).snd  ;
			if (tr != null) {
				var tr_ = (GWTransition) model.history.get(k).snd ;
				System.out.println("     TR: "  + tr.type + " --> " + tr_.target) ;
			}
			var S = (GWState) model.history.get(k).fst ;
			System.out.println(">>> i="+ i+ ": " + printState(S)) ;
			var T = S.clone() ;
			//System.out.println(">>> S = T: " + S.equals(T)) ;
			//System.out.println(">>> hashes: " + S.hashCode() + " vs " + T.hashCode()) ;
			
			
		}
		*/

	}

}
