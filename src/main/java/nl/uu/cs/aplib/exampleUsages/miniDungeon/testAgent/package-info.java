/**
 * This package contains the implementation of iv4xr Env, State,
 * TacticLib, GoalLib etc, which are necessary to allow an aplib/iv4xr
 * test agent to control the game in the smart way.
 * 
 * <p>Keep in mind that the provided navigation and exploration
 * tactics/goals currently has no ability to deal with items that
 * block a corridor. The solution is for now to just generate
 * another dungeon where we have no corridors are not blocked by
 * items (use another random seed, for example). A better fix
 * would be to have a smarter navigation and exploration. TO DO.
 * 
 * <p>Simple demos: see {@link Demo1}, {@link Demo2}, and {@link Demo2b}.
 * 
 * <p>More advanced demos using SA1 algorithm: {@link Demo3} and {@link Demo3b}.
 * 
 * <p> Demo with testing: see in the test-tree: 
 * {@link eu.iv4xr.framework.exampleTestAgentUsage.miniDungeon.TestMiniDungeonWithAgent}.
 * 
 */
package nl.uu.cs.aplib.exampleUsages.miniDungeon.testAgent;