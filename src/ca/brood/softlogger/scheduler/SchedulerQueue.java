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

import java.util.AbstractQueue;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

public class SchedulerQueue extends AbstractQueue<Schedulable> implements Queue<Schedulable> {

	PriorityQueue<Schedulable> internalQueue;
	
	public SchedulerQueue() {
		super();
		internalQueue = new PriorityQueue<Schedulable>(5, new ScheduleeComparator());
	}
	
	@Override
	public boolean offer(Schedulable arg0) {
		if (arg0 instanceof Schedulable)
			return internalQueue.offer((Schedulable) arg0);
		return false;
	}

	@Override
	public Schedulable peek() {
		return internalQueue.peek();
	}

	@Override
	public Schedulable poll() {
		return internalQueue.poll();
	}

	@Override
	public Iterator<Schedulable> iterator() {
		return internalQueue.iterator();
	}

	@Override
	public int size() {
		return internalQueue.size();
	}

	private class ScheduleeComparator implements Comparator<Schedulable> {
		@Override
		public int compare(Schedulable arg0, Schedulable arg1) {
			return (int) (arg0.getNextRun() - arg1.getNextRun());
		}
	}

}
