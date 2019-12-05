package eu.iv4xr.framework.mainConcepts;

public class GCD {
	
	public int Calculate_GCD(int x,int y) {
		
		if (y != 0)
        return Calculate_GCD(y, x % y);
     else
    	return x;
	}
}