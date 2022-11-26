# Example: the game MiniDungeon

Within iv4xr-core project, we have included a simple game called MiniDungeon, to provide you with a bigger testing example than the smallish [GCGGame](./testagent_tutorial_1.md) example. You can try the MiniDungeon game by running
the main-method of the class [DungeonApp](../../src/main/java/nl/uu/cs/aplib/exampleUsages/miniDungeon/DungeonApp.java). You can change game configuration there e.g. to have more (or less) monsters, to have a bigger level, etc:

```Java
main(String[] args) throws Exception {		
		MiniDungeonConfig config = new MiniDungeonConfig() ;
		config.numberOfMonsters = 20 ;
		config.numberOfHealPots = 8 ;		
		config.viewDistance = 5 ;
    ...
		var app = new DungeonApp(config) ;
		deploy(app);
	}
```

![MiniDungeon](./minidungeonShot2.png)

The game is played by either one or two players (Frodo and Smaegol), played on a single computer (e.g. they can use two keyboards connected to a single laptop). They players can either work together, or against each other.

A player can go from one level to the next one, until the final level. Access to the next level is guarded by a _shrine_, which can teleport the player to the next level. However, the shrine must be _cleansed_ first before it can be used as a teleporter. To cleanse it, the player needs to use a _scroll_ (gray icon in the game). There are usually several scrolls dropped in a level, but only one of them (a holy scroll) can do the cleansing. The player does not know which scroll is holy until it tries to use it on a shrine. There are also monsters in the levels that can hurt the player, but also potions that can heal the player or enhance its combat.

Control:

* Player-1 (Frodo): move up/left/down/right with wasd keys. Key e uses a healing potion, key r uses a rage potion.
* Player-2 (Smeagol): move up/left/down/right with ijkl keys. Key e uses o healing potion, key p uses a rage potion.
* Moving onto an object interacts with it. If it is a scroll or potion, the player will pick it and put it inside its bag.

Players:

* Frodo's bag can hold two items, but Frodo has less health point (HP).
* Smeagol's bag can only hold one item. Smeagol has more HP and higher attack rating.

### Testing MiniDunegon

A simple example of a test using an agent is shown in [SimpleTestMiniDungeonWithAgent](../../src/test/java/eu/iv4xr/framework/exampleTestAgentUsage/miniDungeon/SimpleTestMiniDungeonWithAgent.java):

```Java
...
var G = SEQ(
   goalLib.smartEntityInCloseRange(agent,"S0_1"),
   goalLib.entityInteracted("S0_1"),
   goalLib.smartEntityInCloseRange(agent,"SM0"),
   goalLib.entityInteracted("SM0"),
   SUCCESS()) ;

		// Now, create an agent, attach the game to it, and give it the above goal:
		agent. attachState(state)
			 . attachEnvironment(env)
			 . setGoal(G) ;

		Thread.sleep(1000);

		// Now we run the agent:
		System.out.println(">> Start agent loop...") ;
		int k = 0 ;
		while(G.getStatus().inProgress()) {
			agent.update();
			System.out.println("** [" + k + "] agent @" + Utils.toTile(state.worldmodel.position)) ;
			// delay to slow it a bit for displaying:
			Thread.sleep(10);
			if (k>=300) break ;
			k++ ;
		}
		assertTrue(G.getStatus().success())	 ;
		WorldEntity shrine = state.worldmodel.elements.get("SM0") ;
		System.out.println("=== " + shrine) ;
		assertTrue((Boolean) shrine.properties.get("cleansed")) ;
```


In this test the agent will search and pick up scroll S0_1,
	 * and then use it on the shrine of level-0. We expect that
	 * the shrine will then be cleansed.

### Programming test agent, preparation

Before we can use test agents to test the game there are several things we need to prepare:

* Build an basic interface to the game. This has to be implemented as a subclass of [Iv4xrEnvironment](../../src/main/java/eu/iv4xr/framework/mainConcepts/Iv4xrEnvironment.java). For the MiniDungeon game this is implemented in the class [MyAgentEnv](../../src/main/java/nl/uu/cs/aplib/exampleUsages/miniDungeon/testAgent/MyAgentEnv.java). Among other things, a metod called `observe()` needs to be implemented. This returns a representation of the gamestate. This needs to be an instance of so-called [WorldModel](../../src/main/java/eu/iv4xr/framework/mainConcepts/WorldModel.java), which is meant to be a generic representation of a game's state. This entails that you also need to implement a method that can translate your actual gamestate to an instance of WorldModel.

* Define a class that will become your agent-state. This has to be implemented as a subclass of [ Iv4xrAgentState](../../src/main/java/eu/iv4xr/framework/mainConcepts/Iv4xrAgentState.java). It will automatically inherit a reference to a WorldModel, representing the current (well, more precisely, the lastly observed) gamestate. Additionally you have the freedom to add more fields to keep track other information. In our example, this is implemented by the class [MyAgentState](../../src/main/java/nl/uu/cs/aplib/exampleUsages/miniDungeon/testAgent/MyAgentState.java). An important thing to note is that this class of hold a navigation-graph to enable automated path finding for the test agents. This enables the agent to auto-navigate and auto-explore the game world. Iv4xr provides several implementation of navigation graph; here we will use a grid-world graph (and it is also a multi-level graph, since the MiniDungeon game is multi level).
