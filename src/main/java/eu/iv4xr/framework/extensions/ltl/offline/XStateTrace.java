package eu.iv4xr.framework.extensions.ltl.offline;

import java.io.IOException;
import java.util.* ;

import eu.iv4xr.framework.extensions.ltl.LTL;
import eu.iv4xr.framework.extensions.ltl.SATVerdict;
import eu.iv4xr.framework.spatial.Vec3;
import nl.uu.cs.aplib.utils.CSVUtility;
import nl.uu.cs.aplib.utils.Pair;

public class XStateTrace {
	
	String id ;
	List<XState> trace = new LinkedList<>() ;
	
	/**
	 * For each variable x, the diff of x is simply the difference between its value
	 * now and its value in the previous state. Maybe "first-derivative" is a better
	 * name :)
	 */
	void calculateDiffs() {
		XState previous = null ;
		for (var st : trace) {
			if (previous != null) {
				for (String vname : st.values.keySet()) {
					Float v = st.values.get(vname) ;
					Float v0 = previous.values.get(vname) ;
					if (v!= null && v0!=null) {
						st.diff.put(vname, v -  v0) ;
					}
				}
			}
			previous = st ;
		}
	}
	
	/**
	 * Fill in the history-tracking Be careful ... this uses quite some space.
	 */
	void addHistory(String ... vars) {
		Map<String,List<Pair<Vec3,Float>>> history = new HashMap<>() ;
		// create starting history for every var; all empty:
		for (String vname : vars) {
			history.put(vname, new LinkedList<Pair<Vec3,Float>>()) ;
		}
		for (var st : trace) {
			for (String vname : vars) {
				Float value = st.values.get(vname) ;
				if (st.pos != null && value != null) {
					history.get(vname).add(new Pair<Vec3,Float>(st.pos,value)) ;
				}
				List<Pair<Vec3,Float>>  h = new LinkedList<>() ;
				h.addAll(history.get(vname)) ;
				st.history.put(vname,h) ;
			}
		}
	}
	
	public void enrichTrace(String ... varsToEnhanceWithHistory) {
		calculateDiffs() ;
		addHistory(varsToEnhanceWithHistory) ;
	}
	
	private static Integer getColumIndex(String name, String[] colNames) {
		for(int k=0; k<colNames.length; k++) {
			if (colNames[k].equals(name)) {
				return k ;
			}
		}
		return null ;
	}
	
	private Vec3 constructPosition(Float x, Float y, Float z) {
		if (x == null) return null ;
		y = y==null ? 0f : y ;
		z = z==null ? 0f : z ;
		return new Vec3(x,y,z) ;
	}
	
	/**
	 * Read an XState-trace from a CSV-file.
	 * 
	 * @param filename
	 * @return
	 * @throws IOException
	 */
	public static XStateTrace readFromCSV(String filename) throws IOException {
		List<String[]> data = CSVUtility.readCSV(',', filename) ;
		if (data.size() == 0) return null ;
		String[] columnNames = data.get(0) ;
		Integer indexPosx = getColumIndex("posx",columnNames) ;
		Integer indexPosy = getColumIndex("posy",columnNames) ;
		Integer indexPosz = getColumIndex("posz",columnNames) ;
		Integer indexTime = getColumIndex("time",columnNames) ;
		
		XStateTrace trace = new XStateTrace() ;
		
		int k = 0 ;
		for (var row : data) {
			if(k>0) {
			   XState st = new XState() ;
			   if (indexPosx >= 0) {
				   float x = Float.parseFloat(row[indexPosx]) ;
				   float y = 0 ;
				   if (indexPosy>=0) {
					   y = Float.parseFloat(row[indexPosy]) ;
				   }
				   float z = 0 ;
				   if (indexPosz>=0) {
					   z = Float.parseFloat(row[indexPosz]) ;
				   }
				   st.pos = new Vec3(x,y,z) ;
			   }
			   if (indexTime>=0) {
				   st.time = Long.parseLong(row[indexTime]) ;
			   }
			   for (int c=0 ; c < row.length; c++) {
				   if (c==indexPosx || c==indexPosy || c==indexPosz || c==indexTime) {
					   continue ;
				   }
				   st.values.put(columnNames[c], Float.parseFloat(row[c])) ;						   
 			   }
			   trace.trace.add(st) ;
			}
			k++ ;
		}
		return trace ;
	}

	
	public SATVerdict satisfy(LTL<XState> ltl) {
		return ltl.sat(trace) ;
	}
	
	public static boolean satisfy(LTL ltl, List<XStateTrace> traces) {
		for (var tr : traces) {
			SATVerdict verdict = tr.satisfy(ltl) ;
			if (verdict == SATVerdict.SAT) return true ;
		}
		return false ;
	}
	
	public static boolean valid(LTL ltl, List<XStateTrace> traces) {
		for (var tr : traces) {
			SATVerdict verdict = tr.satisfy(ltl) ;
			if (verdict == SATVerdict.UNSAT) return false ;
		}
		return true ;
	}
	
	public static boolean unsat(LTL ltl, List<XStateTrace> traces) {
		return ! unsat(ltl,traces) ;
	}

}
