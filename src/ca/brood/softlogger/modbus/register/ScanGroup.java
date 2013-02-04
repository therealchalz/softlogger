package ca.brood.softlogger.modbus.register;

import java.util.Comparator;
import java.util.SortedSet;
import java.util.TreeSet;

import ca.brood.softlogger.modbus.Device;

public class ScanGroup implements Comparable<ScanGroup>{
	private int ttl = 0;			//number of milliseconds until next scan
	private final int scanRate;		//Intervals between scans.  Aka ttl reset value.
	private final Device owner;
	private SortedSet<RealRegister> registers;
	
	public ScanGroup(int scanRate, Device owner) {
		this.scanRate = scanRate;
		this.owner = owner;
		registers = new TreeSet<RealRegister>();
	}
	
	public void addRegister(RealRegister add) {
		registers.add(add);
	}
	
	public void elapsed(int time) {
		//allowing negative values ensures ordering remains constant when several groups
		//reach EOL at the same time.
		ttl -= time;
	}
	
	public boolean ready() {
		return ttl <=5;
	}
	
	public int getTtl() {
		return ttl;
	}
	
	@Override
	public int compareTo(ScanGroup o) {
		return ttl - o.ttl;
	}
	
}
