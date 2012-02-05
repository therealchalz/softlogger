package ca.brood.softlogger.util;

import org.apache.log4j.Logger;

public class Util {
	private static Logger log = Logger.getLogger(Util.class);
	public static int parseInt(String number) throws NumberFormatException {
		int ret;
		if (number.matches("0[xX][0-9a-fA-F]+")) { //Hex number
			ret = Integer.parseInt(number.substring(3), 16);
		} else if (number.startsWith("0")) { //Octal number
			ret = Integer.parseInt(number,8);
		} else {
			ret = Integer.parseInt(number);
		}
		return ret;
	}
}
