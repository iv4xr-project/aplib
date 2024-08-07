/**
 * Contains some automated testing algorithms.
 * 
 * <ul>
 * 
 * <li> {@link IInteractiveWorldGoalLib} and {@link IInteractiveWorldTacticLib}. These
 * are not algorithms actually :) They just provide interfaces of common goals and tactics
 * for testing games. You still need to implement these interfaces of course.
 * 
 *  <li> {@link Sa1Solver} and {@link Sa2Solver}. These are automated algorithms for
 *  solving testing tasks. They operate on goals like navigateTo() that under the hood
 *  solve navigation (path-finding). In other words, the solvers work at a <b>higher level</b>,
 *  where path-finding is already taken care for it. 
 *  
 *  <li> {@link BasicSearch}. This is basically a random algorithm for solving testing
 *  tasks. Like the SaXSolvers above, it also operates at a high level.
 *  
 *  <li> {@link XEvolutionary}: an implementation of an evolutionary algorithm for solving
 *  testing tasks. Like SaXSolvers, it also operates at a high level.
 *  
 *  <li> {@link XMCTS}: an implementation of the Monte Carlo Tree Search algorithm for solving
 *  testing tasks. MCTS is a reinforcement-learning algorithm. Like SaXSolvers, it also 
 *  operates at a high level.
 *  
 *  <li> {@link XQAlg}: an implementation of the Q-learning algorithm for solving
 *  testing tasks. Q is a reinforcement-learning algorithm. Like SaXSolvers, it also 
 *  operates at a high level.
 *  
 *  <li> {@link AQAlg}: an implementation of the Q-learning algorithm for solving
 *  testing tasks. Q is a reinforcement-learning algorithm. Unlike the other algorithms,
 *  AQalg operates at the primitive action level. So, it get no free path-finding help.
 *  
 * </ul>
 * 
 */
package eu.iv4xr.framework.goalsAndTactics;