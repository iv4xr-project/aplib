package nl.uu.cs.aplib.MainConcepts;

public class Budget implements Cloneable {
	
	Double amount ;
	
	public Budget() {
		amount = Double.POSITIVE_INFINITY ;
	}
	
	public Budget(double amount) { this.amount = amount ; }
	
	public boolean isUnlimited() { return amount.isInfinite() ; }
	
	public Double amount() { return amount ; }
	
	public boolean exhausted() { return !amount.isInfinite() && amount <= 0 ; }
	
	public void substract(double d) {
		if (!amount.isInfinite()) amount = amount - d ;
	}
	
	public void add(double d) { substract(-d) ; }
	
	/**
	 * Consume this budget with the amount specified by b.
	 */
	public void consume(Budget b) {
		if (b.isUnlimited()) throw new IllegalArgumentException() ;
		substract(b.amount) ;
	}
	
	
	
	/**
	 * Transfer a delta amount of budget from source to this budget. This delta
	 * will be added to this budget, and subtracted from the source budget.
	 */
	public void transfer(Budget source, Budget delta) {
		source.consume(delta) ;
		substract(-delta.amount) ;
	}
	
	@Override 
	public String toString() {
		//if (isUnlimited()) return "+inf" ;
		return Double.toString(amount) ;
	}

}
