package ca.brood.softlogger.datafunction;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;

public class DataProcessingManager {
	private static Logger log;
	private static Map<Class<? extends DataFunction>, DataFunction> functions;
	private static DataFunction nullFunction;
	
	static {
		functions = new HashMap<Class<? extends DataFunction>, DataFunction>();
		log = Logger.getLogger(DataProcessingManager.class);
		nullFunction = new NullDataFunction();
	}
	
	public static DataFunction getDataFunction(Class<? extends DataFunction> functionClass) {
		if (functions.containsKey(functionClass)) {
			return functions.get(functionClass);
		} else {
			try {
				DataFunction newFunc = functionClass.newInstance();
				functions.put(functionClass, newFunc);
				log.info("Loaded a new data function: "+functionClass);
				return newFunc;
			} catch (Exception e) {
				log.error("Error when trying to instantiate: "+functionClass);
			}
		}
		return nullFunction;
	}
}
