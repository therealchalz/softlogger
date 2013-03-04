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
	
	public PrettyPeriodicSchedulable() {
		super();
	}
	
	public PrettyPeriodicSchedulable(PrettyPeriodicSchedulable o) {
		super(o);
	}
	
	public PrettyPeriodicSchedulable clone() {
		return new PrettyPeriodicSchedulable(this);
	}
	
	public PrettyPeriodicSchedulable(int period) {
		super (period);
		this.nextRun = getNextPrettyTime(System.currentTimeMillis());
	}
	
	public PrettyPeriodicSchedulable(int period, Runnable action) {
		super(period, action);
		this.nextRun = getNextPrettyTime(System.currentTimeMillis());
	}
	
	@Override
	public synchronized void setPeriod(int p) {
		period = p;
		setNextRun(getNextPrettyTime(System.currentTimeMillis()));
	}
	
	private long getNextPrettyTime(long currentTime) {
		long nextTime = 0;
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeInMillis(currentTime);
		cal.set(Calendar.HOUR, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MILLISECOND, 0);
		
		int fullPeriodsToday = (int) ((currentTime - cal.getTimeInMillis()) / period);
		nextTime = cal.getTimeInMillis() + (period * (fullPeriodsToday+1));
		return nextTime;
	}
	
	@Override
	protected synchronized void updateNextRunWithPeriod() {
		long oldNextRun = getNextRun();
		long period = getPeriod();
		long curTime = System.currentTimeMillis();
		if ((curTime - oldNextRun) < 2 * period) {
			setNextRun(getNextPrettyTime(oldNextRun));
		} else {
			setNextRun(getNextPrettyTime(curTime));
		}
	}
}
