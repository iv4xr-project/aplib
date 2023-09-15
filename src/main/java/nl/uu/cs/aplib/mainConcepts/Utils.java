package nl.uu.cs.aplib.mainConcepts;

import java.util.function.Predicate;

import eu.iv4xr.framework.mainConcepts.WorldModel;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentStateExtended;
import nl.uu.cs.aplib.mainConcepts.GoalStructure.PrimitiveGoal;
import nl.uu.cs.aplib.utils.Pair;

public class Utils {

	
	static Goal getInnerRandomGoal(GoalStructure G) {
		if (G instanceof PrimitiveGoal) {
			var P = (PrimitiveGoal) G ;
			Goal g = P.goal ;
			if (g.name.startsWith("random")) {
				return g ;
			}
		}
		else {
			for (var H : G.subgoals) {
				Goal g = getInnerRandomGoal(H) ;
				if (g != null) return g ;
			}
		}
		return null ;
	}

	public static void changeRandomGoal(GoalStructure G, Predicate<Pair<MyAgentState,WorldModel>> f) {
	   Goal g = getInnerRandomGoal(G) ;
	   g.toSolve(f) ;
	}
	
	
	
	
	
	
}
