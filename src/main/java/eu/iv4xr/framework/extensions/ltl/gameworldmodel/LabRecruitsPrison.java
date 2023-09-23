package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.util.*;

/**
 * To generate a LabRecruits level-definition for a prison-like
 * level.
 * 
 * <p>WORK IN PROGRESS. Should move this to iv4xrDemo.
 */
public class LabRecruitsPrison {
	
	int numberOfCellsPerBlock ;
	int numberOfPrisonBlocks ;
	
	public LabRecruitsPrison(int numberOfPrisonGroups, int numberOfCellsPerGroup) {
		this.numberOfCellsPerBlock = numberOfCellsPerGroup ;
		this.numberOfPrisonBlocks = numberOfPrisonGroups ;
	}
	
	/**
	 * True if the cell is the one to appear in the first far west side
	 * of the whole prison.
	 */
	boolean isFarWestCell(int blockNr, int cellNr) {
		return blockNr==0 && (cellNr/2 == 0) ;
	}
	
	/**
	 * True if the cell is bordering with the east wall of a block.
	 */
	boolean isEastWallCell(int cellNr) {
		return cellNr == numberOfCellsPerBlock-1
				|| cellNr == numberOfCellsPerBlock-2 ;
	}
	
	static class Str {
		String str ;
		
		Str (String str) {
			this.str = str ;
		}
		
		void append(String s) {
			str += s ;
		}
		void append(Str z) {
			str += z.str ;
		}
		
		@Override
		public String toString() {
			return str ;
		}
	}
	
	static class Txt {
		List<Str> content  = new LinkedList<>() ;
		
		Txt(String ...strings) {
			for (var s : strings) {
				content.add( new Str(s)) ;
			}
		}
		
		Txt beside(Txt T) {
			int k = 0 ;
			while (k < content.size()) {
				if (k >= T.content.size())
					break ;
				content.get(k).append(T.content.get(k));
				k++ ;
			}
			while (k < T.content.size()) {
				content.add(T.content.get(k)) ;
				k++ ;
			}
			return this ;
		}
		
		Txt above(Txt T) {
			content.addAll(T.content) ;
			return this ;
		}
		
		Txt copy() {
			Txt U = new Txt() ;
			for (var t : content) {
				U.content.add(new Str("" + t.str)) ;
			}
			return U ;
		}
		
		@Override
		public String toString() {
			StringBuffer z = new StringBuffer() ;
			int k = 0 ;
			for (var s : content) {
				if (k>0)
					z.append("\n") ;
				z.append(s.str) ;
				k++ ;
			}
			return z.toString() ;
		}
		
	}
	
	String duplicate(String c, int n) {
		StringBuffer buf = new StringBuffer() ;
		for (int k=0; k<n; k++)
			buf.append(c) ;
		return buf.toString() ;
		
	}
	
	/*
	  wwwwwwwww
	  wfffffwfffw
	  wfffffwfffw
	  wwwDwwwwDww
	  w
	  w
	  wwwwwwwww
	  wfffwfffw
	  wfffwfffw
	  wwDwwwDww
	  
	 */
	
	Txt mkCell_layer0(int blockNr, int cellNr) {
		if (cellNr < 0) {
			// not a cell, always in south-east corner of a block
			return new Txt(
			"fffffx",
			"wfffww",
			"fffffw",
			"fffffw",
			"wwwwww") ;
		}
		if (cellNr % 2 == 0) {
			// north-side
			if (isFarWestCell(blockNr,cellNr ))
				return new Txt(
				"wwwwwww",
				"wfffffw",
				"wwwfwww",
				"wwwdwww",
				"xbfffff") ;	
			if (isEastWallCell(cellNr))   
				return new Txt(
				"wwwwww",
				"fffffw",
				"wwfwww",
				"wwdwww",
				"bffffx") ;
			else // middle cell:
				return new Txt(
				"wwwwww",
				"fffffw",
				"wwfwww",
				"wwdwww",
				"bfffff") ;		
		}
		// else south-side:
		if (isFarWestCell(blockNr,cellNr ))
			return new Txt(
			"xbfffff",
			"wwwdwww",
			"wwwfwww",
			"wfffffw",
			"wwwwwww") ;
		if (isEastWallCell(cellNr))   
			return new Txt(
			"bffffx",
			"wwdwww",
			"wwfwww",
			"fffffw",
			"wwwwww") ;
		else // middle cell:
			return new Txt(
			"bfffff",
			"wwdwww",
			"wwfwww",
			"fffffw",
			"wwwwww") ;	
	}
	
	
	Txt corridorBetweenCells_level0(int blockNr) {
		
		int N = 6*(numberOfCellsPerBlock/2 + 1) ;
		
		if (numberOfPrisonBlocks==1) {
			return new Txt(
				"wfff" + duplicate("f",N-6) + "ffw",
				"wfff" + duplicate("f",N-6) + "ffw",
				"wfff" + duplicate("w",N-6) + "ffw",
				"wfff" + duplicate("f",N-6) + "ffw",
				"wfff" + duplicate("f",N-6) + "ffw"
			) ;
		}
		else {
			if (blockNr == 0) {
				return new Txt(
					"wfff" + duplicate("f",N-6) + "ffw",
					"wfff" + duplicate("f",N-6) + "ffw",
					"wfff" + duplicate("w",N-6) + "ffd",
					"wfff" + duplicate("f",N-6) + "ffw",
					"wfff" + duplicate("f",N-6) + "fbw"
				) ;
			}
			else if (blockNr == numberOfPrisonBlocks-1) {
				return new Txt(
						"bff" + duplicate("f",N-6) + "ffw",
						"wff" + duplicate("f",N-6) + "ffw",
						"fff" + duplicate("w",N-6) + "ffw",
						"wff" + duplicate("f",N-6) + "ffw",
						"fff" + duplicate("f",N-6) + "ffw"
					) ;
			}
			else { // middle block
				return new Txt(
						"bff" + duplicate("f",N-6) + "ffw",
						"wff" + duplicate("f",N-6) + "ffw",
						"fff" + duplicate("w",N-6) + "ffd",
						"wff" + duplicate("f",N-6) + "ffw",
						"fff" + duplicate("f",N-6) + "fbw"
					) ;
			}
		}
	}
	
	Txt mkPrison() {
		Txt blank5 = new Txt("","","","","") ;
		Txt northCells = blank5.copy() ;
		Txt southCells = blank5.copy() ;
		Txt corridor   = blank5.copy() ;
		for (int blockNr=0; blockNr<numberOfPrisonBlocks; blockNr++) {
			//System.out.println(">>> Block " + blockNr) ;
			for (int C=0; C<numberOfCellsPerBlock;C++) {
				if (C % 2 == 0) {
					//System.out.println(">>> Block " + blockNr + ", C=" + C) ;
					northCells.beside(mkCell_layer0(blockNr,C)) ;
				}
				else {
					southCells.beside(mkCell_layer0(blockNr,C)) ;
				}
				
			}
			if (numberOfPrisonBlocks % 2 == 1) {
				southCells.beside(mkCell_layer0(blockNr,-1)) ;
			}
			Txt Z = corridorBetweenCells_level0(blockNr) ;
			//System.out.println(">>>  Z:\n" + Z) ;
			corridor.beside(Z) ;
			//System.out.println(">>>  \n" + corridor) ;
		}
		return northCells.above(corridor).above(southCells) ;
	}
	
	
	
	public static void main(String[] args) {
		LabRecruitsPrison prison = new LabRecruitsPrison(3,5) ;
		System.out.println("" + prison.mkPrison()) ;
	}

}
