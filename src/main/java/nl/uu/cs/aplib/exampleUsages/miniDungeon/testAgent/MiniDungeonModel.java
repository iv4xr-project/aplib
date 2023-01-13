package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;

import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import eu.iv4xr.framework.extensions.ltl.IExplorableState;
import eu.iv4xr.framework.extensions.ltl.ITransition;
import eu.iv4xr.framework.extensions.ltl.BasicModelChecker.Path;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWObject;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWState;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWTransition;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GWTransition.GWTransitionType;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;
import static nl.uu.cs.aplib.AplibEDSL.* ;

/**
 * Provides the semantic-function (alpha) for interactions, to be used to construct a model
 * for the MiniDungeon game using {@link GameWorldModel}.
 */
public class MiniDungeonModel {
	
	static List<String> bagContent(GWObject player) {
		List<String> bag = new LinkedList<>() ;
		String i = (String) player.properties.get("bagslot1") ;
		if (! i.equals("xxx"))  bag.add(i) ;
		if (player.id.equals("Frodo"))  {
			i = (String) player.properties.get("bagslot2") ;
			if (! i.equals("xxx"))  bag.add(i) ;
		}
		return bag ;	
	}
	
	static void putInBag(GWObject player, String item) {
		if (player.id.equals("Smeagol"))  {
			 player.properties.put("bagslot1",item) ;
		}
		else {
			String i = (String) player.properties.get("bagslot1") ;
			if (i.equals("xxx")) {
				player.properties.put("bagslot1",item) ;
			}
			else {
				player.properties.put("bagslot2",item) ;
			}
		}
	}
	
	static void removeFromBag(GWObject player, String item) {
		player.properties.put("bagslot1", "xxx") ;
	}
	
	static boolean isMoonShrine(GWObject o) {
		return o.type.equals("SHRINE")
				&& o.properties.get("shrinetype").equals("MoonShrine") ;
	}
	
	static boolean isImmortalShrine(GWObject o) {
		return o.type.equals("SHRINE")
				&& o.properties.get("shrinetype").equals("ShrineOfImmortals") ;
	}
	
	
	public static void alphaFunction(String mainplayer, String i, Set<String> affected, GWState S) {
		GWObject interacted = S.objects.get(i) ;
		if (interacted.type.equals("SCROLL")) {
			GWObject P = S.objects.get(mainplayer) ;
			int N = bagContent(P).size() ;
			//System.out.println(">>> alpla interact scroll " + i) ;
			// conservatively reject if the player already has an item:
			if (N>=1) return ;
			// else the scroll will be picked:
			interacted.destroyed = true ;
			putInBag(P,interacted.id) ;
			var bag = bagContent(P) ;
			//System.out.println(">>> alpla interact scroll " + i) ;
			//System.out.println(">>> frodo bag: " + P.properties.get("bagslot1") + ", # in bags:" + bag.size()) ;
			return ;
		}
		// else i is shrine:
		//System.out.println(">>> alpla interact shrine " + i) ;
		if (isMoonShrine(interacted) || isImmortalShrine(interacted)) {
			//System.out.println(">>> shrine ...") ;
			if ((Boolean) interacted.properties.get("cleansed"))
				return ;
			if (affected == null || affected.size() == 0)
				return ;
			GWObject P = S.objects.get(mainplayer) ;
			var bag = bagContent(P) ;
			//System.out.println(">>> bag size.") ;
			if (bag.size()==0) {
				// bag has no scroll... in this case implies N=0
				return ;
			}
			//System.out.println(">>> using a scroll...") ;
			String openingScroll = null ;
			for (var o : affected) {
				openingScroll = o ; break ;
			}
			if (openingScroll!= null && bag.contains(openingScroll)) {
				//System.out.println(">>> shrine " + i + " cleansed! with scroll " + bag.size() + ", " + bag.get(0)) ;
				interacted.properties.put("cleansed", true) ;
				if (isMoonShrine(interacted)) {
					interacted.properties.put(GameWorldModel.IS_OPEN_NAME,true) ;
				}
			}
			else {
				//System.out.println(">>> shrine " + i + " attempted! with scroll " + bag.size() + ", " + bag.get(0)) ;
			}
			removeFromBag(P,openingScroll) ;	
		}
	}
	
	static boolean isShrineId(String id) {
		return id.startsWith("SM") || id.startsWith("SS") || id.startsWith("SI") ;
	}
	
	static int getMapNrFromShrineId(String id) {
		int mapNr = Integer.parseInt(id.substring(2)) ;
		return mapNr ;
	}
	
	public static GoalStructure convert2ToGoalStructure(TestAgent agent, Path<Pair<IExplorableState,String>> sequence) {
		 Path<IExplorableState> sequence2 = new Path<>() ;
		 for (var step : sequence.path) {
			 Pair<ITransition,IExplorableState>  step2 = new Pair<>(step.fst, step.snd.fst) ;
			 sequence2.path.add(step2) ;
		 }
		 return convertToGoalStructure(agent,sequence2) ;
	}
	
	public static GoalStructure convertToGoalStructure(TestAgent agent, Path<IExplorableState> sequence) {
		List<GoalStructure> subgoals = new LinkedList<>() ;
		var goalLib = new GoalLib() ;
		GWState previousState = null ;
		for(var step : sequence.path) {
			GWTransition tr = (GWTransition) step.fst ;
			if (tr == null) {
				previousState = (GWState) step.snd ;
				continue ;
			}
			if (tr.type == GWTransitionType.INTERACT) {
					subgoals.add(goalLib.entityInteracted(tr.target)) ;	
			}
			else {
				// else it is a travel
				
				// special case when the travel is a teleport-step:
				if (previousState != null
					&& isShrineId(tr.target)
					&& isShrineId(previousState.currentAgentLocation)
					&& Math.abs(getMapNrFromShrineId(tr.target) - getMapNrFromShrineId(previousState.currentAgentLocation))
						   == 1								
					) {
					// the previous location is a shrine paired with the current one,
					// so the travel is a teleport travel, triggered by the previous
					// transition.
					// For the teleport step itself, no need to do anything:
					subgoals.add(goalLib.entityInteracted(previousState.currentAgentLocation)) ;
				}
				else {
					subgoals.add(goalLib.smartEntityInCloseRange(agent, tr.target)) ;
				}
			}
			previousState = (GWState) step.snd ;
		}
		GoalStructure[] dummy = {} ;
		GoalStructure G = SEQ(subgoals.toArray(dummy)) ;
		return G ;
	}

}
