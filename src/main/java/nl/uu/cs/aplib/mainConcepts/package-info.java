/**
 * This package provides classes that form the main concepts of the
 * aplib agent-programming. It provides classes such as these:
 * 
 *   <ol>
 *   <li> {@link BasicAgent} defines a basic aplib agent. Such an agent can
 *   be given a goal-structure. When 'executed', the agent will then interpret
 *   the solvers that come with the goal-structure in order to solve the
 *   goal-structure.
 *   
 *   <li> {@link Goal} and {@link GoalStructure} for constructing goals that
 *   can be given to an aplib agent.
 *   
 *   <li> {@link Action} and {@link Tactic} for constructing goal-solvers.
 *   
 *   <li> {@link SimpleState} that can be subclassed to define a rich state
 *   (also called 'belief' in some agent-programming jargon)
 *   for aplib-agents.
 *   
 *   <li> {@link Environment} as an 'interface' to the actual environment that an
 *   aplib agent is intended to interact with. 
 *   
 *   </ol>
 *   
 * 
 */
package nl.uu.cs.aplib.mainConcepts;