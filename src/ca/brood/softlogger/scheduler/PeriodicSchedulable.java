/*******************************************************************************
 * Copyright (c) 2013 Charles Hache <chache@brood.ca>. All rights reserved. 
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
 *     Charles Hache <chache@brood.ca> - initial API and implementation
 ******************************************************************************/
package ca.brood.softlogger.scheduler;

public class PeriodicSchedulable implements Schedulable, Comparable<Schedulable> {

	private Long nextRun;
	private Integer period;
	private Runnable action;
	
	public PeriodicSchedulable() {
		period = Integer.MAX_VALUE;
		nextRun = Long.MAX_VALUE;
	}
	
	public PeriodicSchedulable(PeriodicSchedulable o) {
		nextRun = o.nextRun;
		period = o.period;
		action = o.action;
	}
	
	public PeriodicSchedulable clone() {
		return new PeriodicSchedulable(this);
	}
	
	public PeriodicSchedulable(int period) {
		this.period = period;
		nextRun = System.currentTimeMillis();
		
	}
	public PeriodicSchedulable(int period, Runnable action) {
		this(period);
		this.action = action;
	}
	
	public int getPeriod() {
		synchronized (period) {
			return period;
		}
	}
	public void setPeriod(int p) {
		synchronized (period) {
			period = p;
		}
		setNextRun(System.currentTimeMillis() + getPeriod());
	}

	@Override
	public void execute() {
		if (this.action != null) {
			this.action.run();
		}
		
		
		//The strategy for getting the next run time:
		//If the scheduler was paused for a long while, it could be
		//our next run is far in the past.  If we add the period to
		//it then that could still be in the past.  In this case we
		//add the period to the current time.  Otherwise, we add the
		//period to the previous next run time - the effect of this
		//is that the periods between the run times are constant,
		//regardless of how long a schedulee's action takes to run.
		long oldNextRun = getNextRun();
		long period = getPeriod();
		long curTime = System.currentTimeMillis();
		if ((curTime - oldNextRun) < 2 * period) {
			setNextRun(oldNextRun + period);
		} else {
			setNextRun(curTime + period);
		}
	}
	
	public void setAction(Runnable action) {
		this.action = action;
	}

	@Override
	public int compareTo(Schedulable other) {
		return (int)(this.getNextRun() - other.getNextRun());
	}
	@Override
	public long getNextRun() {
		synchronized (nextRun) {
			return nextRun;
		}
	}
	
	private void setNextRun(long nr) {
		synchronized (nextRun) {
			nextRun = nr;
		}
	}
}
