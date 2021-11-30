package eu.iv4xr.framework.mainConcepts;

public class EmotionTestAgent extends TestAgent {
	
	protected IEmotionState estate ;
	
	public EmotionTestAgent() { super() ; }
	
	public EmotionTestAgent attachEmptionState(IEmotionState estate) {
		this.estate = estate ;
		return this ;
	}
	
	public IEmotionState getEmotionState() {
		return estate ;
	}

	@Override
	public void update() {
		super.update() ;
		if (estate != null) {
			estate.updateEmotion(this);
		}
	}
	
}
