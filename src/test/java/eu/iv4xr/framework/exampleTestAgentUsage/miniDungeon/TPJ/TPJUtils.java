package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ;

import java.util.logging.Level;

import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.Logging;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.DungeonApp;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.GameStatus;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentEnv;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.Utils;
import nl.uu.cs.aplib.mainConcepts.GoalStructure;
import nl.uu.cs.aplib.utils.Pair;

public class TPJUtils {
	
	/**
	 * Give the goal G to the agent and run it on MiniDungeon.
	 */
	static MyAgentState runAgent(TestAgent agent, 
			MiniDungeonConfig config,
			GoalStructure G, 
			int budget,
			int sleep,
			boolean stopAfterAgentDie,
			boolean withGraphics,
			boolean supressLogging,
			boolean verbosePrint) throws Exception {
		
		var states = runAgent(agent,null,config,G,null,budget,sleep,
				stopAfterAgentDie,
				withGraphics,
				supressLogging,
				verbosePrint) ;
		return states.fst ;	
	}
	
	/**
	 * Give the goals G1 and G2 to two agents and run it on MiniDungeon.
	 */
	static Pair<MyAgentState,MyAgentState> runAgent(TestAgent agent1, 
			TestAgent agent2,
			MiniDungeonConfig config,
			GoalStructure G1,
			GoalStructure G2,
			int budget,
			int sleep,
			boolean stopAfterAgentDie,
			boolean withGraphics,
			boolean supressLogging,
			boolean verbosePrint) throws Exception {
		
		// setting sound on/off, graphics on/off etc:
		DungeonApp app = new DungeonApp(config);
		app.soundOn = false;
		app.headless = !withGraphics ;
		if(withGraphics) DungeonApp.deploy(app);
		
		// should be after create the agent, else the constructor sets the visibility again
		if (supressLogging) {
			Logging.getAPLIBlogger().setLevel(Level.OFF);
		}

		MyAgentEnv env1 = new MyAgentEnv(app);
		MyAgentEnv env2 = new MyAgentEnv(app);
		MyAgentState state1 = new MyAgentState();
		MyAgentState state2 = new MyAgentState();
		
		agent1 . attachState(state1) . attachEnvironment(env1)  .setGoal(G1) ;
		agent1.addInv(new MD_invs().allInvs) ;
		agent1.resetLTLs() ;
		
		if (agent2 != null) {
			agent2. attachState(state2) . attachEnvironment(env2)  .setGoal(G2) ;
			agent2.addInv(new MD_invs().allInvs) ;
			agent2.resetLTLs() ;
		}
		
		
		
		Thread.sleep(1000);
		// Now we run the agent:
		System.out.println(">> Start agent loop...") ;
		int k = 0 ;
		int aterdieCount = 2 ;
		long time = System.currentTimeMillis() ;
		while(G1.getStatus().inProgress() || (G2 != null && G2.getStatus().inProgress())) {
			agent1.update();
			if (agent2 != null) {
				if (sleep>0) Thread.sleep(sleep); 
				agent2.update();
			}
			System.out.println("** [" + k + "/" + state1.val("aux","turn") + "]") ;
			if (verbosePrint) {
				System.out.println("   agent1 " + agent1.getId() 
					+ " @" + Utils.toTile(state1.worldmodel.position) 
				    + ", maze:" + state1.val("maze")
				    + ", hp:" + state1.val("hp")
				    + ", score:" + state1.val("score")
				    + ", goal-status:" + G1.getStatus());
				if (agent2 != null) {
			       System.out.println("   agent2 " + agent2.getId() 
			       + " @" + Utils.toTile(state2.worldmodel.position)
			       + ", maze:" + state2.val("maze")
				   + ", hp:" + state2.val("hp")
				   + ", score:" + state2.val("score")
				   + ", goal-status:" + G2.getStatus());
				}
			}
			// delay to slow it a bit for displaying:
			if (sleep>0) Thread.sleep(sleep); 
			if (k>=budget) break ;
			if (!state1.agentIsAlive() 
					&& (agent2==null || !state2.agentIsAlive())
					&& stopAfterAgentDie) {
				aterdieCount-- ;
			}
			if (aterdieCount<=0) break ;
			k++ ;
		}	
		time = System.currentTimeMillis() - time ;
		System.out.println(">> End of run. #turns=" + k + ", time=" + time) ;
		System.out.println("   Game status:" + state1.val("aux","status")) ;
		System.out.println("   G1 status:" + G1.getStatus()
				+ ", " + agent1.getId() + " hp: " + state1.val("hp")
				+ ", score: " + state1.val("score")) ;
		if (agent2 != null) {
			System.out.println("   "
					+ "G2 status:" + G2.getStatus()
					+ ", " + agent2.getId() + " hp: " + state2.val("hp")
					+ ", score: " + state2.val("score")) ;
		}
		//System.out.println(">>> Game status:" + state.gameStatus()) ;
		return new Pair<>(state1,state2) ;
	}

}
