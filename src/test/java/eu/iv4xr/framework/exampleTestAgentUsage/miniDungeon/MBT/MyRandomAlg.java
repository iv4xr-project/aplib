package eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.MBT;

import eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TPJ.RandomPlayTester;
import eu.iv4xr.framework.mainConcepts.TestAgent;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.MiniDungeon.MiniDungeonConfig;
import nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent.MyAgentState;

/**
 * Provide a method to run a random-algorithm to interact with MD. The
 * algorithm itself is implemented in {@link RandomPlayTester}.
 */
public class MyRandomAlg {

	/**
	 * Run a random-test algorithm on MD. It is given a time
	 * budget. If the agent dies, we start over and runs it again,
	 * until the budget is exhausted.
	 */
	static void run_Random(String agentId, MiniDungeonConfig config, 
			boolean soundOn,
			boolean graphicsOn,
			int timeBudget,
			int delayBetweenUpdates) {
		int step = 0 ;
		var time0 = System.currentTimeMillis() ;
		long timeOut = time0 + (timeBudget * 1000);
		while (System.currentTimeMillis() < timeOut) {
			TestAgent agent = MDRelauncher.agentRestart(agentId, config, soundOn, graphicsOn) ;
			var randomAlg = new RandomPlayTester() ;
			randomAlg.reallyRandom = true ;
			var G = randomAlg.randomPlay(agent) ;
			agent.setGoal(G) ;
			var S = (MyAgentState) agent.state() ;
			while (System.currentTimeMillis() < timeOut && S.agentIsAlive() && ! MDAbstraction.gameover(S)  ) {
				agent.update();
				step++ ;
				try {
					if (delayBetweenUpdates > 0)
					Thread.sleep(delayBetweenUpdates);
				}
				catch(Exception e) { } // swallow
			}
			System.out.println(">>> turn:" + step + ", alive:" + S.agentIsAlive()) ;
		}
		System.out.println("** Random on " + config.configname) ;
		System.out.println("** #turns:" + step) ;
		System.out.println("** time:" + ((System.currentTimeMillis() - time0)/1000) + " s") ;
	}
	
}
