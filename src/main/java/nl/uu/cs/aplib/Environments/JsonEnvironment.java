package nl.uu.cs.aplib.Environments;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import nl.uu.cs.aplib.MainConcepts.Environment;

public class JsonEnvironment extends Environment {
	
	Gson gson = new GsonBuilder().serializeNulls().create();
	
	public Object sendCommand(String id, String command, Object arg, Class classOfReturnObject) {
		String result = sendCommand_(id,command,gson.toJson(arg)) ;
		if (classOfReturnObject == null) return null ;
		return gson.fromJson(result, classOfReturnObject) ;
	}

}
