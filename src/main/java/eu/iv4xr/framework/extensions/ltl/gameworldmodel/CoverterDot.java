package eu.iv4xr.framework.extensions.ltl.gameworldmodel;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

/**
 * For producing DOT visualization of {@link GameWorldModel}.
 *
 * @author Wish
 */
public class CoverterDot {
	
	public static void saveAs(String filename,GameWorldModel model, 
			boolean drawInZoneArrows,
			boolean drawInteractionSelfLoop) throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(toDOT(model,drawInZoneArrows,drawInteractionSelfLoop));
        writer.close();
	}
	
	public static String toDOT(GameWorldModel model, 
			boolean drawInZoneArrows,
			boolean drawInteractionSelfLoop) {
		StringBuffer out = new StringBuffer() ;
		
		String name = model.name == "" ? "model" : model.name ;
		out.append("digraph " + name + "{\n") ;
		
		// first identify objects that inhabit multiple zones (these should be blockers,
		// but we'll not assuming that here)
		Map<String,List<String>> inMultipleZones = new HashMap<>() ;
		for (String o : model.defaultInitialState.objects.keySet()) {
			List<String> inZones = new LinkedList<>() ;
			for (var Z : model.zones) {
				if (Z.members.contains(o)) {
					inZones.add(Z.id) ;
				}
			}
			if (inZones.size() > 1) {
				inMultipleZones.put(o, inZones) ;
			}
		}
		
		// construct the zones:
		for(GWZone zone : model.zones) {
			out.append("  subgraph cluster_" + zone.id + " {\n") ;
			out.append("    label = \"Zone " + zone.id + "\" ;\n" ) ;
			out.append("    style=filled;\n" ) ;
			out.append("    color=lightblue;\n" ) ;
			out.append("    node [style=filled,fillcolor=white];\n") ;
            			
            for(String o : zone.members) {
               String o_name = o ;
           	
               if (inMultipleZones.keySet().contains(o)) {
            	   o_name = "" + o + "_" + zone.id ;
               }
               out.append("    " + o_name) ;
               if (model.blockers.contains(o)) {
            	   // check if it is initially open or close:
            	   GWObject obj = model.defaultInitialState.objects.get(o) ;
            	   Object isOpen_ = obj.properties.get(GameWorldModel.IS_OPEN_NAME) ;
            	   boolean isInitiallyOpen = isOpen_ == null ? false : (Boolean) isOpen_ ;
            	   if (isInitiallyOpen) {
            		   out.append("[shape=Square]") ;
            	   }
            	   else {
            		   out.append("[shape=Square,fillcolor=lightgrey]") ; 
            	   }
            	   
               }
               out.append(";\n") ;
               if (drawInteractionSelfLoop) {
            	   out.append("    " + o_name + " -> " + o_name + ";\n") ;
               }
               // end of object:
            }
            
            if (drawInZoneArrows) {
            	String[] dummy = {} ;
            	String[] members = zone.members.toArray(dummy) ;
            	for (int k=0; k<members.length-1 ; k++) {
            		String o1 = members[k] ;
            		if (inMultipleZones.keySet().contains(o1)) {
            			o1 = o1 + "_" + zone.id ;
            		}
            		for (int n=k+1; n<members.length; n++) {
            			String o2 = members[n] ;
            			if (inMultipleZones.keySet().contains(o2)) {
                			o2 = o2 + "_" + zone.id ;
                		}
            			out.append("    " + o1 + " -> " + o2 + " [dir=both];\n") ;
            		}
            	}
            }
            
			// end of zone:
			out.append("  }\n") ;
		}
		
		// add arrows between zones:
		for (var C : inMultipleZones.entrySet()) {
			String o = C.getKey() ;
			List<String> ZS = C.getValue() ;
			for(int k=0; k<ZS.size()-1 ; k++) {
				String Z1 = ZS.get(k) ;
				String o1 = "" + o + "_" + Z1 ;
				for (int n=k+1; n<ZS.size(); n++) {
					String Z2 = ZS.get(n) ;
					String o2 = "" + o + "_" + Z2 ;
					out.append("  " + o1 + " -> " + o2 + "[dir=both,color=blue];\n") ;
				}
			}
		}
        
		// object connections:
        for(var C : model.objectlinks.entrySet()) {
        	String i = C.getKey() ;
        	List<String> ivariants = new LinkedList<>() ;
        	if (inMultipleZones.keySet().contains(i)) {
        		for (String Z : inMultipleZones.get(i)) {
        			ivariants.add("" + i + "_" + Z) ;
        		}
        	}
        	if (ivariants.isEmpty())
        		ivariants.add(i) ;
        	for (String o : C.getValue()) {
        		List<String> ovariants = new LinkedList<>() ;
        		if (inMultipleZones.keySet().contains(o)) {
            		for (String Z : inMultipleZones.get(o)) {
            			ovariants.add("" + o + "_" + Z) ;
            		}
            	}
            	if (ovariants.isEmpty())
            		ovariants.add(o) ;
            	for (String i_ : ivariants) {
            		for (String o_ : ovariants) {
            			out.append("  " + i_ + " -> " + o_ + "[arrowhead=box,color=red];\n") ;
            		}
            	}
        	}
        }
		
		out.append("}") ;
		return out.toString() ;
	}

}
