package eu.iv4xr.framework.mainConcepts;

/**
 * An extension of {@link TestAgent} that can also has emotion.
 * 
 * @author Wish
 */
// NOTE: since emotion update requires a reference to the agent itself, we need
// to put the extension of the emotion-state here, under the agent, rather
// than as extension of State, since State maintains no reference to agent.
public class EmotiveTestAgent extends TestAgent {

	/**
	 * Field holding the agent's emotion.
	 */
	protected IEmotionState estate;

	public EmotiveTestAgent() {
		super();
	}
	
	/**
     * Create a plain instance of emotive-TestAgent with the given id and role. To be useful
     * you will need to add few other things to it, e.g. a state and a goal. You
     * also need to link it to a {@link TestDataCollector}.
     */
    public EmotiveTestAgent(String id, String role) {
        super(id, role);
    }

	/**
	 * Attach an emotion state to this agent. By this we mean attaching a structure
	 * that can represent the agent's emotion.
	 */
	public EmotiveTestAgent attachEmotionState(IEmotionState estate) {
		this.estate = estate;
		return this;
	}

	/**
	 * Return the agent current emotion.
	 */
	public IEmotionState getEmotionState() {
		return estate;
	}

	/**
	 * This will do the standard test-agent update() as defined by
	 * {@link TestAgent}. Additionally, this will also trigger an update to the
	 * emotional-state held by this agent.
	 */
	@Override
	public void update() {
		super.update();
		if (estate != null) {
			estate.updateEmotion(this);
		}
	}

}
