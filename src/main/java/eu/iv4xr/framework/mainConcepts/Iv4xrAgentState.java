package eu.iv4xr.framework.mainConcepts;

import java.util.function.BiFunction;

import eu.iv4xr.framework.extensions.ltl.BasicModelChecker;
import eu.iv4xr.framework.extensions.ltl.BuchiModelChecker;
import eu.iv4xr.framework.extensions.ltl.gameworldmodel.GameWorldModel;
import eu.iv4xr.framework.extensions.pathfinding.Navigatable;
import eu.iv4xr.framework.extensions.pathfinding.SimpleNavGraph;
import eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph;
import eu.iv4xr.framework.spatial.meshes.Mesh;
import nl.uu.cs.aplib.agents.State;
import nl.uu.cs.aplib.mainConcepts.Environment;

/**
 * Representing the state of an iv4xr agent. It extends the class
 * {@link nl.uu.cs.aplib.agents.State}. As such, we can then attach a Prolog
 * reasoner to this state. See {@link nl.uu.cs.aplib.agents.State}.
 * 
 * <p>
 * The new features of this class is that it also holds a representation of the
 * state of the target world. This is represented as an instance of
 * {@link WorldModel}. And, along with it it can also hold a navigation graph to
 * facilitate automated navigation over the target world.
 * 
 * The type paremeter NavgraphNode represents the type of the nodes in {@link #worldNavigation}, or the type
 * that is used to identify the nodes.
 * 
 * @author Wish
 *
 */
// NOTE: we can't embed emotion here because emotion needs reference to the agent itself
// to update; and an instance of State does not have that reference.
public class Iv4xrAgentState<NavgraphNode> extends State {

	/**
	 * Representing the current state of the target world.
	 */
	public WorldModel worldmodel;

	/**
	 * A navigation graph for navigating the the target world. Every node represent
	 * a physical location in the target world. When two nodes are connected by an
	 * edge, it means that there is a way to travel between them in the target
	 * world. You can use {@link eu.iv4xr.framework.extensions.pathfinding.AStar} to
	 * calculate a path between two nodes and use this path to guide an agent to
	 * travel between them.
	 * 
	 * <p>
	 * Note that {@link eu.iv4xr.framework.extensions.pathfinding.Navigatable} is
	 * only an interface; you will need a concrete implementation of this. An
	 * example of such an implementation is
	 * {@link eu.iv4xr.framework.extensions.pathfinding.SimpleNavGraph}.
	 * 
	 * <p>
	 * This class does not provide a method to construct a navigation graph.
	 * Something else must construct this graph, which you then can attach it to
	 * this state.
	 */
	public Navigatable<NavgraphNode> worldNavigation;
	
	/**
	 * A behavior model of the game-under-test. As a model, it may describe the
	 * system under test at a certain level of abstraction. If such a model is
	 * given, the agent (that owns this state) can then exploit it, e.g. to help it
	 * solves goals. The model can for example implements an extended Finite State
	 * Machine (EFSM). Importantly, note that the model, as an instance of
	 * {@link GameWorldModel}, implements the interface
	 * {@link eu.iv4xr.framework.extensions.ltl.ITargetModel} and hence it can be
	 * queried using e.g. {@link eu.iv4xr.framework.extensions.BasicModelChecker}.
	 * 
	 * Alternatively, the model that is given may also be empty initially, and the
	 * agent may be equipped with instrumentation to incrementally build the model.
	 * This is done by {@link #gwmodelLearner}.
	 */
	public GameWorldModel gwmodel ;
	
	/**
	 * A function that incrementally builds the behavior model attached to
	 * this state, see {@link #gwmodel}. This function, if defined, is called by 
	 * {@link #updateState(String)}. The function inspects this state, and 
	 * can be used to register new model elements discovered in the current state.
	 */
	@SuppressWarnings("rawtypes")
	public BiFunction<Iv4xrAgentState,GameWorldModel,Void> gwmodelLearner ;
	
	/**
	 * A model checker which can be used to query {@link #gwmodel}. This checker can be used
	 * to find a sequence of steps over  the gwmodel that lead to a certain state (on the model).
	 */
	public BasicModelChecker checker ;
	
	/**
	 * A LTL-model checker which to query {@link #gwmodel}.
	 */
	public BuchiModelChecker ltlchecker ;
	

	@Override
	public Iv4xrEnvironment env() {
		return (Iv4xrEnvironment) super.env();
	}

	/**
	 * Return the value in {@link #worldmodel}.
	 * 
	 * @return
	 */
	public WorldModel worldmodel() {
		return worldmodel;
	}

	/**
	 * Return the navigation-graph in {@link Iv4xrAgentState#worldNavigation}.
	 */
	public Navigatable<NavgraphNode> worldNavigation() {
		return worldNavigation;
	}
	
	public Iv4xrAgentState<NavgraphNode> setWorldNavigation(Navigatable<NavgraphNode> navgraph) {
		this.worldNavigation = navgraph ;
		return this ;
	}

	/**
	 * Link the given environment to this State. An instance of
	 * {@link Iv4xrEnvironment} is needed as the environment. 
	 */
	@Override
	public Iv4xrAgentState<NavgraphNode> setEnvironment(Environment env) {
		if(env == null) {
			throw new IllegalArgumentException("Cannot attach a null environment to a state.") ;
		}
		if (!(env instanceof Iv4xrEnvironment)) {
			throw new IllegalArgumentException("This class requires an Iv4xrEnvironment as its environment.");
		}
		super.setEnvironment(env);
		return this;
	}
	
	/**
	 * Attach behavior model of the game-under-test. Such a model is an instance of
	 * {@link GameWorldModel}. As a model, it may describe the system under test at
	 * a certain level of abstraction. If such a model is given, the agent (that
	 * owns this state) can then exploit it, e.g. to help it solves goals. The model
	 * can for example implements an extended Finite State Machine (EFSM).
	 * Importantly, note that the model, as an instance of {@link GameWorldModel},
	 * implements the interface
	 * {@link eu.iv4xr.framework.extensions.ltl.ITargetModel} and hence it can be
	 * queried using e.g. {@link eu.iv4xr.framework.extensions.BasicModelChecker}.
	 * 
	 * <p>
	 * Alternatively, the model that is given may also be empty initially, and the
	 * agent may be equipped with instrumentation to incrementally build the model.
	 * If not null, the parameter gwmodelLearner specifies this instrumentation
	 * function mentioned above. This function will be called by
	 * {@link #updateState(String)}. The function inspects this state, and can be
	 * used to register new model elements discovered in the current state.
	 */
	@SuppressWarnings("rawtypes")
	public Iv4xrAgentState<NavgraphNode> attachBehaviorModel(
			GameWorldModel model,
			BiFunction<Iv4xrAgentState,GameWorldModel,Void> gwmodelLearner) {
		if (model == null)
			throw new IllegalArgumentException("The model can't be null.") ;
		this.gwmodel = model ;
		this.gwmodelLearner = gwmodelLearner ;
		this.checker = new BasicModelChecker(model) ;
		this.ltlchecker = new BuchiModelChecker(model) ;
		return this ;
	}
	

	/**
	 * This will call the observe() method of the environment attached to this state
	 * to obtain a fresh observation, and then uses it to update this state. If
	 * {@link #worldmodel} exists, this new observation will be merged into the
	 * {@link #worldmodel}.
	 */
	@Override
	public void updateState(String agentId) {
		// note: intentionally NOT calling super.updateState()
		var newObs = env().observe(agentId);
		if (worldmodel != null) {
			worldmodel.mergeNewObservation(newObs);
		}
		else {
			worldmodel = newObs ;
		}
		if (gwmodel != null && gwmodelLearner !=null){
			// if model learner is not null, invoke it:
			gwmodelLearner.apply(this,gwmodel) ;
		}
	}

	/**
	 * Covert the given surface-mesh into a navigation graph. More precisely, to an
	 * instance of {@link eu.iv4xr.framework.extensions.pathfinding.SimpleNavGraph}.
	 * Then the method attaches this navigation graph into the given agent-state.
	 */
	public static void loadSimpleNavGraph(Iv4xrAgentState<Integer> state, Mesh mesh) {
		state.worldNavigation = SimpleNavGraph.fromMeshFaceAverage(mesh);
	}

	/**
	 * If the environment attached to the given state has a navigation/surface-mesh,
	 * this method converts the mesh into a navigation graph. More precisely, to an
	 * instance of {@link eu.iv4xr.framework.extensions.pathfinding.SimpleNavGraph}.
	 * Then the method attaches this navigation graph into the given agent-state.
	 */
	public static void loadSimpleNavGraph(Iv4xrAgentState<Integer> state) {
		var env = state.env();
		if (env == null) {
			throw new IllegalArgumentException("The state has no environment.");
		}
		Mesh mesh = env.worldNavigableMesh();
		if (mesh == null) {
			throw new IllegalArgumentException("The environment attached to this state has no navigation-mesh.");
		}
		loadSimpleNavGraph(state,mesh) ;
	}
	
	/**
	 * Covert the given surface-mesh into a navigation graph. More precisely, to an
	 * instance of {@link eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph}.
	 * Then the method attaches this navigation graph into the given agent-state.
	 */
	public static void loadSurfaceNavGraph(
			Iv4xrAgentState<Integer> state, 
			Mesh mesh,
			float faceAreaThresholdToAddCenterNode) {	
		var navgraph = new SurfaceNavGraph(mesh,faceAreaThresholdToAddCenterNode) ;	
		state.worldNavigation = navgraph ;
	}
	
	/**
	 * If the environment attached to the given state has a navigation/surface-mesh,
	 * this method converts the mesh into a navigation graph. More precisely, to an
	 * instance of {@link eu.iv4xr.framework.extensions.pathfinding.SurfaceNavGraph}.
	 * Then the method attaches this navigation graph into the given agent-state.
	 */
	public static void loadSurfaceNavGraph(
			Iv4xrAgentState<Integer> state, 
			float faceAreaThresholdToAddCenterNode) {	
		var env = state.env();
		if (env == null) {
			throw new IllegalArgumentException("The state has no environment.");
		}
		Mesh mesh = env.worldNavigableMesh();
		if (mesh == null) {
			throw new IllegalArgumentException("The environment attached to this state has no navigation-mesh.");
		}
		loadSurfaceNavGraph(state,mesh,faceAreaThresholdToAddCenterNode) ;
	}
}
