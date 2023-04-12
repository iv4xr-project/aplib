package eu.iv4xr.framework.extensions.ltl;

import java.util.List;

public interface ITargetModel {
	
	public void reset() ;
	
	public IExplorableState getCurrentState() ;
	
	public boolean backTrackToPreviousState() ;
	
	public List<ITransition> availableTransitions() ;
	
	public void execute(ITransition tr) ;
	

}
