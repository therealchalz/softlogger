package ca.brood.softlogger.modbus.register;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import ca.brood.softlogger.modbus.Device;

public class ScanGroup implements Comparable<ScanGroup>{
	private int ttl = 0;			//number of milliseconds until next scan
	private final int scanRate;		//Intervals between scans.  Aka ttl reset value.
	private SortedSet<RealRegister> registers;
	private boolean cannotKeepUp;
	
	public ScanGroup(int scanRate) {
		this.scanRate = scanRate;
		registers = new TreeSet<RealRegister>();
		cannotKeepUp = false;
	}
	
	public void addRegister(RealRegister add) {
		registers.add(add);
	}
	
	public SortedSet<RealRegister> getRegisters() {
		return registers;
	}
	
	public void elapsed(long elapsedMillis) {
		//allowing negative values ensures ordering remains constant when several groups
		//reach EOL at the same time.
		//It is also used for performance monitoring - a negative TTL means we're running slow
		ttl -= elapsedMillis;
	}
	
	public boolean ready() {
		return ttl <=5;
	}
	
	public int getTtl() {
		return ttl;
	}
	
	public void reset() {
		reset(0);
	}
	
	public void reset(int timeSinceLastScan) {
		ttl = scanRate - timeSinceLastScan;
		
		if (scanRate < timeSinceLastScan && !cannotKeepUp) {
			Logger.getGlobal().warning("Scangroup with rate of "+scanRate+" ms cannot keep up.");
			cannotKeepUp = true;
		}
	}
	
	@Override
	public int compareTo(ScanGroup o) {
		return ttl - o.ttl;
	}
	
	public String toString() {
		return "ScanGroup: TTL: "+ttl+" scanRate: "+scanRate;
	}
	
}
