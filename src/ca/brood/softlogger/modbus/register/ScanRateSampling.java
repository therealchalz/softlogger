package ca.brood.softlogger.modbus.register;

public enum ScanRateSampling {
	SUM,
	MEAN,
	LATEST,
	LATCHON,
	LATCHOFF;
	
	public static ScanRateSampling fromString(String text) {
		if (text.equalsIgnoreCase("average"))
			return MEAN;
		if (text.equalsIgnoreCase("sum"))
			return SUM;
		if (text.equalsIgnoreCase("mean"))
			return MEAN;
		if (text.equalsIgnoreCase("accumulate"))
			return SUM;
		if (text.equalsIgnoreCase("latest"))
			return LATEST;
		if (text.equalsIgnoreCase("latchon"))
			return LATCHON;
		if (text.equalsIgnoreCase("latchoff"))
			return LATCHOFF;
		return MEAN;
	}
}
