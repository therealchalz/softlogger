package ca.brood.softlogger.modbus.register;

public enum RegisterSizeType {
	SIGNED,
	UNSIGNED,
	FLOAT;
	
	public static RegisterSizeType fromString(String attr) {
		if (attr.equalsIgnoreCase("s"))
			return SIGNED;
		if (attr.equalsIgnoreCase("int"))
			return SIGNED;
		if (attr.equalsIgnoreCase("signed"))
			return SIGNED;
		if (attr.equalsIgnoreCase("u"))
			return UNSIGNED;
		if (attr.equalsIgnoreCase("uint"))
			return UNSIGNED;
		if (attr.equalsIgnoreCase("unsigned"))
			return UNSIGNED;
		if (attr.equalsIgnoreCase("f"))
			return FLOAT;
		if (attr.equalsIgnoreCase("float"))
			return FLOAT;
		
		return UNSIGNED;
	}
}
