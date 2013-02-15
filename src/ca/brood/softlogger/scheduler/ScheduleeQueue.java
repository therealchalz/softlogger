package ca.brood.softlogger.scheduler;

import java.util.AbstractQueue;
import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.PriorityQueue;
import java.util.Queue;

public class ScheduleeQueue extends AbstractQueue<Schedulee> implements Queue<Schedulee> {

	PriorityQueue<Schedulee> internalQueue;
	
	public ScheduleeQueue() {
		super();
		internalQueue = new PriorityQueue<Schedulee>(5, new ScheduleeComparator());
	}
	
	@Override
	public boolean offer(Schedulee arg0) {
		if (arg0 instanceof Schedulee)
			return internalQueue.offer((Schedulee) arg0);
		return false;
	}

	@Override
	public Schedulee peek() {
		return internalQueue.peek();
	}

	@Override
	public Schedulee poll() {
		return internalQueue.poll();
	}

	@Override
	public Iterator<Schedulee> iterator() {
		return internalQueue.iterator();
	}

	@Override
	public int size() {
		return internalQueue.size();
	}

	private class ScheduleeComparator implements Comparator<Schedulee> {
		@Override
		public int compare(Schedulee arg0, Schedulee arg1) {
			return (int) (arg0.getNextRun() - arg1.getNextRun());
		}
	}

}
