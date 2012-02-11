package ca.brood.softlogger.util;

//import org.apache.log4j.Logger;

import ca.brood.softlogger.modbus.register.RegisterData;

public class Util {
	//private static Logger log = Logger.getLogger(Util.class);
	public static int parseInt(String number) throws NumberFormatException {
		int ret;
		if (number.matches("0[xX][0-9a-fA-F]+")) { //Hex number
			ret = Integer.parseInt(number.substring(3), 16);
		} else {
			ret = Integer.parseInt(number);
		}
		return ret;
	}
	public static RegisterData parseRegisterData(String val) throws NumberFormatException {
		RegisterData ret = new RegisterData();
		if (val.matches("[0-9]+\\.[0-9]+[fF]?")) { //floating point
			ret.setDataFloat(Float.parseFloat(val));
		} else if (val.equalsIgnoreCase("true")){
			ret.setData(true);
		} else if (val.equalsIgnoreCase("false")){
			ret.setData(false);
		} else {
			ret.setData(Integer.parseInt(val));
		}
		return ret;
	}
}
