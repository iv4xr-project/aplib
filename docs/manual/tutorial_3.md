# Aplib Tutorial 3:  Subservient Agent, Autonomous Agent, and Multi agent

### Subservient vs autonomous agent

Any `aplib` agent can be deployed as a subservient or as an autonomous agent. When an agent is subservient, you control its tick (you cantrol when the agent's `update()` method is called). In contrast, an autonomous agent runs on its own thread and ticks itself at its own pace that you have no control of.

If you have an agent called `agent`, to run it in the subservient mode you can simply put it in a loop like this:

```java
agent.setGoal(g) ;
while (g.getStatus().inProgress()) {
   agent.update();
   // ... do your own things between ticks
}
```
Running agents in the subservient mode could be good enough for some applications. This mode is also useful when we want to test/verify an agent-based system as it allows the execution of such a system to be fully controlled by the verifier. E.g. this opens a way to do model checking on an agent-based system.


For `agent` to run autonomously, it needs to be a subclass of `nl.uu.cs.aplib.agents.AutonomousBasicAgent`. To run it we can do something like this:

```java
agent.setGoal(g) ;
new Thread(() -> agent.loop()) . start()
```

By default, A will invoke its own `update()` every 1000 ms. You can change this, e.g to every 500 ms like this:

```java
agent.setSamplingInterval(500)
```

### Basic primitives to synchronize autonomous agents

Let `agent` be an autonomous agent.

* **Pausing.** Once an autonomous agent runs, you can pause it by invoking `agent.pause()`. This may not immediately pause the agent. If it is in the middle of executing an action, it will first complete the execution of the action. Then it will pause.

* **Resuming.** When an agent is paused, invoking `agent.resume()` will cause it to resume its working. This also works when the agent is sleeping between ticks; `resume()` will awaken it and cause it to call `update()`. Additionally, a message arriving at the agent has the same effect as `resume()`.

* **Stopping.** Invoking `agent.stop()` will cause the agent to stop its autonomous execution. This may not immediately stop the agent. If it is in the middle of executing an action, it will first complete the execution of the action. Then it will stop.

* **Waiting for a goal to be solved.** You can let your main thread (or any other thread) to sleep, waiting until `agent` solves it current goal by invoking `agent.waitUntilTheGoalIsConcluded()` from the thread that you want to put to wait.

The above primitives will allow a main thread to have basic control on autonomous agents. You should not used them to implement inter-agent synchronization. `Aplib` agents can asycnrhonously send message to each other, which allow them to loosely synchronize themselves.

#### Sleeping behavior

If the agent has no goal, it will sleep until it is given a goal. Invoking `resume()`
 and an incoming message will in principle also awaken the agent. An agent may not have a goal at the beginning because none is given to it yet. When a top-level goal is solved, it will also be removed from the agent; so it will then sleep until a new goal is assigned to it.

At the end of a tick (at the end of `update()`), if the agent calculates that the next tick is still far away, it will sleep until the next tick.  Invoking `resume()` or `stop()` will awaken the agent. An incoming message will also do this.


### Multi-agent

Recall that `apsl` defines an agent-based system as an environment with one or more agents trying to control the same environment. So, once you have more than one agent that share the same environment you have a multi-agent system. It is thinkable, that you want to program your agents to work together. To do so, you need a way for the agents to communicate to each other.

To coordinate their work, agents can send messages to each other. To use this feature there are three requirements:

1. The agent must be an instance of `nl.uu.cs.aplib.agents.AutonomousBasicAgent` (or an instance of its subclass).
1. The agent must be configured to use a state which is an instance of `nl.uu.cs.aplib.agents.StateWithMessenger` (or an instance of its subclass).
1. The agent must register itself to a **communication node**, which is an instance of `nl.uu.cs.aplib.multiAgentSupport.ComNode`.

A state of type `StateWithMessenger` has access to the method `state.messenger().send(...)`. Actions of the agent that owns the state can use it to send messages to other agents.

Let me show an example. Let's first define a state structure. Let's have a simple state, maintaining just one integer called `counter`. Importantly, this state must extends the class `State` (which in turns extends `SimpleState`, adding to it, among other things, a facility for inter-agent communication):

```java
class MyState extends State {
   int counter == 0
}
```
Let define our first agent. We will also create an instance of a communication node, and let this agent to register to this node:

```java
var comNode = new ComNode() ;

var state1 = new MyState().setEnvironment(new NullEnvironment()) ;

var agent1 = new AutonomousBasicAgent("A1","teacher")
            . attachState(state1)
            . registerTo(comNode) ;
```

In this environment the agents will ony interact with each other, and not with any actual environment. So, we use the `NullEnvironment` as the Environment (a `NullEnvironment` does nothing).

Above, the string "A1" is a unique ID we assign to `agent1` and "teacher" is a string we use to identify its **role**. Multiple agents can have the same role.

Let's now create another agent, and register it to the same communication node:

```java
var state2 = new MyState().setEnvironment(new NullEnvironment()) ;

var agent2 = new AutonomousBasicAgent("A2","student")
             . attachState(state1)
             . registerTo(comNode) ;
```


#### Sending

When you program agents to solve goals, you basically program actions for each agent, that it uses to solve its goal. When programming an action, you can program it to send messages to other agents and to inspect and retrieve messages its owning agent receives. Primitives to send and receive messages are available through the agent's state, which is accessible from the agent's actions. If `s` is the state of an agent `A`, and you have made it so thar `s` is an instance of `StateWithMessenger`, the method `s.messenger()` gives you access to these send/receive primitives.

As an example, consider now the following action that we will attach to `agent1`. It will send a message to `agent2` and increase its state counter by one:

```java
var a0 = action("a0")
         . do1((MyState S)-> {
             S.messenger().send("A1",0, MsgCastType.SINGLECAST, "A2","blabla") ;
             return ++S.counter ;
         })
         . lift() ;
```

If we for example now attach the following goal to `agent1`:

```java
var g1 = goal("g1").toSolve((Integer x) -> x>=9).withStrategy(a0) . lift() ;
agent1.setGoal(g1) ;
```
At every call to `update()`, `agent1` will send a message named "blabla" to the agent with ID "A2" (thus, `agent2`). This will go on until `agent1` counter becomes equal to 0, at which time its goal is solved and it will become idle afterwards.


#### Receiving

Incoming messages are stored in a message queue, accessible from the agent's state through the method `state.messenger()`. The method `state.messenger().has(p)` checks if the message queue has a message satisfying the predicate p. The method `state.messenger().retrieve(p)` retrieve the first message in the queue that satisfies the predicate p. This message is is returned, and remove from the queue. If no such message is found, `null` is returned.

For example, here is an action that we will assign to `agent2`. It checks in the input queue if there is a message whose name is "blbla". If so, the first of such a message will be removed from the queue, and the agent state counter will be increased by one.

```java
var b1 = action("b1")
         . do1((MyState S)-> {
                S.messenger().retrieve(M -> M.getMsgName().equals("blabla")) ;
                return ++S.counter ;
          })
         . on_((MyState S) -> S.messenger().has(M -> M.getMsgName().equals("blabla")))
         . lift() ;
```

If we for example now attach the following goal to `agent2`:

```java
var g2 = goal("g2").toSolve((Integer x) -> x>=3).withStrategy(b1) . lift() ;
agent2.setGoal(g1) ;
```

At every call to `update()`, `agent2` will check if there is a message named "blabla" in its input queue. If so, this message will be removed. This is repeated until `agent2` has received 3 if such messages.

#### Communication primitives

Recall that an action is basically a function from the agent's state and its own state to solution. To construct an agent we do:

```java
action("name").do1(f)
```

Where the function `f` is a λ-expression of the form:

```java
agentstate -> {
  // do something
  // then return a proposed solution
}
```

The agent-state, assuming you have configured the agent to  have a state which is an intance of `StateWithMessenger`, gives you access to methods to send and receive messages. More precisely, the method `state.messanger()` returns a so-called **messanger** built-in the state, which in turn is an instance of the class `nl.uu.cs.aplib.multiAgentSupport.Messenger`. The messenger holds a **input-queue** storing all incoming messages for the agent that owns it (messages will stay in this queue until some action of the agent decides to remove them). The messenger has the following methods:

* `has(p)` checks of the messenger's input-queue has a messange satisfying the predicate p. This returns true or false.

* `retrieve(p)` will retrieve the first message in the input-queue that satisfies the predicate p. The message is returned, and removed from the queue. If there is no such message, null is returned.

* `send(srcID,k,casttype,target,name,args)` will send a single message to the specified target.
   * `srcID` is the sending agent unique ID.
   * `k` is message priority. Currently this has no effect.
   * `casttype` is either `MsgCastType.SINGLECAST`, `MsgCastType.BROADCAST`, or `MsgCastType.ROLECAST`. A singlecast message is targeted to a single agent, whise ID is specified by `target`. A broadcast message will be sent to all agents registered to the same communication node as the sending agent. A rolecast message will be sent to all agents from the same communication node, and sharing the same role as specified by `target`.
   * `name` is string which is the name/title of the message.
   * `args` is a series of 0 or more arguments of the message, which will be sent along with the message.
   * The method returns an instance of `Aknowledgement`, which is positive if the sending is successful, and else a negative acknowledgment is returned.


**Note:** to be able to send and receive messages, agents do not have to run autonomously. They indeed have to be instances of `AutonomousBasicAgent`. Instances of this class can be run autonomously, but also in subservient mode.
