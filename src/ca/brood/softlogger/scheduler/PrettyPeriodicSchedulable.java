/*******************************************************************************
 * Copyright (c) 2013 Charles Hache. All rights reserved. 
 * 
 * This file is part of the softlogger project.
 * softlogger is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * softlogger is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the 
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with softlogger.  If not, see <http://www.gnu.org/licenses/>.
 * 
 * Contributors:
 *     Charles Hache - initial API and implementation
 ******************************************************************************/
package ca.brood.softlogger.scheduler;

import java.util.Calendar;

public class PrettyPeriodicSchedulable extends PeriodicSchedulable {
	
	private long nanoEpochTimestamp;
	private long milliWalltimeEpochTimestamp = 0;
	
	private long epochUpdateRate = 0;	//in ns.
	
	public PrettyPeriodicSchedulable() {
		super();
		updateEpochIfRequired();
	}
	
	public PrettyPeriodicSchedulable(PrettyPeriodicSchedulable o) {
		super(o);
		nanoEpochTimestamp = o.nanoEpochTimestamp;
		milliWalltimeEpochTimestamp = o.milliWalltimeEpochTimestamp;
		epochUpdateRate = o.epochUpdateRate;
		updateEpochIfRequired();
	}
	
	public PrettyPeriodicSchedulable clone() {
		return new PrettyPeriodicSchedulable(this);
	}
	
	public PrettyPeriodicSchedulable(int period) {
		super (period);
		updateEpochIfRequired();
		this.nextRun = getNextPrettyTime(System.nanoTime());
	}
	
	public PrettyPeriodicSchedulable(int period, Runnable action) {
		super(period, action);
		updateEpochIfRequired();
		this.nextRun = getNextPrettyTime(System.nanoTime());
	}
	
	public void updateEpoch() {
		milliWalltimeEpochTimestamp = System.currentTimeMillis();
		nanoEpochTimestamp = System.nanoTime();
	}
	
	public void setEpochUpdateRate(long rateSeconds) {
		epochUpdateRate = rateSeconds * 1000000000l;
	}
	
	private void updateEpochIfRequired() {
		long currentTimeNanos = System.nanoTime();
		
		if (milliWalltimeEpochTimestamp == 0 ||
			(epochUpdateRate != 0 && (currentTimeNanos - (epochUpdateRate +nanoEpochTimestamp) >= 0))) {
			updateEpoch();
		}
		
	}
	
	@Override
	public synchronized void setPeriod(int p) {
		period = p;
		setNextRun(getNextPrettyTime(System.nanoTime()));
	}
	
	private long getNextPrettyTime(long nanoTimestamp) {
		updateEpochIfRequired();
		
		long currentTimeMillis = milliWalltimeEpochTimestamp + ((nanoTimestamp-nanoEpochTimestamp)/1000000l);
		long nextTime = 0;
		long periodNanos = getPeriod() * 1000000l;
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(currentTimeMillis);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		long midnightMillis = cal.getTimeInMillis();
		long midnightNanos = nanoEpochTimestamp - ((milliWalltimeEpochTimestamp - midnightMillis) * 1000000l);
		
		long nanosToday = (nanoTimestamp - midnightNanos);
		float periodsToday = (float)nanosToday / (float)periodNanos;
		
		int fullPeriodsToday = (int) periodsToday;
		if (periodsToday - fullPeriodsToday >= 0.96) {
			fullPeriodsToday ++;
		}
		
		nextTime = midnightNanos + (periodNanos * (fullPeriodsToday+1l));
		
		return nextTime;
	}
	
	@Override
	protected synchronized void updateNextRunWithPeriod() {
		long oldNextRun = getNextRun();
		long periodNanos = getPeriod()*1000000l;
		long curTime = System.nanoTime();
		if ((curTime - oldNextRun) < 2l * periodNanos) {
			setNextRun(getNextPrettyTime(oldNextRun));
		} else {
			setNextRun(getNextPrettyTime(curTime));
		}
	}
}
