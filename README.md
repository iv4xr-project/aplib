# Aplib: an Agent Programming Library

Aplib is a Java library to program multi agent programs. As it is now, Aplib is still under development. 

An _agent_ is essentially a program that is used to influence an environment towards a certain goal. The typical use case is when the environment is _not_ fully under the agent's control (e.g. it may have some autonomous behavior, or there may be other agents influencing its behavior), but nevertheless we are interested in trying to get it to a state that satisfies whatever the goal we have in mind. An agent programming framework likes aplib strives to offer a more declarative way to program such an agent. 

A newly created aplib agent does nothing. To make it does something, we give it a _goal_ and a _strategy_ to solve the goal. In the nutshell, a strategy is a collection of _guarded actions_. The _guard_ of a guarded action specifies when the action can be fired. When it is given a goal, the agent will start executing in ticked cycles. At each tick, the agent collects the actions which are enabled (their guards are satisfied); one will then be selected and executed. This may trigger interactions with the environment and change the agent's state. If the new state solves the goal, the agent is done, and else it waits for the next tick. This is repeated until the goal is solved, or until the agent runs out of some computing budget.


