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
