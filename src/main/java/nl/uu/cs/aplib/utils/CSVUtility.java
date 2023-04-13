package nl.uu.cs.aplib.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;

import org.apache.commons.lang3.StringUtils;

public class CSVUtility {
    

    /**
     * Write a list of numerical data to a CSV-file. Each row of the data is an array of
     * numerical values (which can be integer or float, for example). The corresponding toString()
     * will be used to covert them to strings. A number is allowed to be null, which will be
     * translated to an empty string.
     * 
     * The parameter columnNames specifies the name of each of the column of the csv file. They will 
     * become the first row of the csv-file.
     * 
     * The rows should be of equal length as the number of columns specified in the parameter columnNames,
     * or shorter. If it is shorted, it will be filled to the right as if it contains nulls.
     */
    public static void exportToCSVfile(Character separator, 
            String[] columnNames,
            List<Number[]> data, String filename) throws IOException {
        List<String[]> data_ = new LinkedList<>() ;
        data_.add(columnNames) ;
        for(Number[] row : data) {
            // create a row, fill each cell with empty string:
            String[] row_ = new String[columnNames.length] ;
            for(int k=0; k<row_.length; k++) row_[k] = "" ;
            
            // translate the original row:
            for(int k=0; k<row.length; k++) {
                if(row[k] != null) row_[k] = row[k].toString() ;
            }
            data_.add(row_) ;
        }
        exportToCSVfile(separator,data_,filename) ;
    }
    
    /**
     * Write a sequence of rows to a CSV-file with the given filename. Each row is assumed to be
     * an array of strings. No header-row is assumed.
     * The separator will be used to separate each cell-entry, e.g. use ',' or ';'.
     */
    public static void exportToCSVfile(Character separator, List<String[]> data, String filename) throws IOException {

        if (separator==null) separator = ',' ;
        
        StringBuffer buf = new StringBuffer();
        int k = 0;
        for (String[] row : data) {
            if (k > 0)
                buf.append("\n");
            for (int y = 0; y < row.length; y++) {
                if (y > 0)
                    buf.append(separator);
                buf.append(row[y]);
            }
            k++;
        }
        if(k>0) buf.append("\n") ;
        BufferedWriter writer = new BufferedWriter(new FileWriter(filename));
        writer.write(buf.toString());
        writer.close();

    }
    
    /**
     * Replace ',' with space.
     */
    public static String cleanUpCommas(String s) {
      return s.replace(',',' ') ;
    }
    
    public static String readTxtFile(String fname) throws IOException {
    	Path file = Paths.get(fname) ;
    	return Files.readString(file) ;  
    }
    
    /**
     * Read a CSV-file.
     */
    public static List<String[]> readCSV(Character separator, String fname) throws IOException {
    	Path file = Paths.get(fname) ;
    	List<String> content = Files.readAllLines(file) ;
    	
    	List<String[]> data = new LinkedList<>() ;
    	for(var row : content) {
    		String[] cells = StringUtils.split(row,separator) ;
    		//for(int k=0; k<cells.length; k++) {
    		//	System.out.print("  " + cells[k]) ;
    		//}
    		//System.out.println("") ;
    		data.add(cells) ;
    	}
    	return data ;
    }

}
