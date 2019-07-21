package nl.uu.cs.aplib.MainConcepts;

import java.util.Comparator;

public class APEvent {
	
	/**
	 * A unique ID for the event.
	 */
	String ID ;
	String Name ;
	Object[] args ;
	
	int priority ;
	
	/**
	 * Constructor, obviously.
	 * 
	 * @param id Unique ID to identify the event.
	 * @param name
	 * @param priority Lesser number means higher priority. 
	 * @param args
	 */
	public APEvent(String id, String name, int priority, Object ... args) {
		id = id ; Name = name ; this.priority = priority ; this.args = args ;
	}
	
	public String getID() { return ID ; }

	public String getName() {
		return Name;
	}

	public Object[] getArgs() {
		return args;
	}

	public int getPriority() {
		return priority;
	}
	
	static class APEventComparator implements Comparator<APEvent> {

		@Override
		public int compare(APEvent o1, APEvent o2) {
			if (o1.priority<o2.priority) return -1 ;
			if (o1.priority>o2.priority) return 1 ;
			return 0 ;
		}
		
	}

}
