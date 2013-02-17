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
		setNextRun(System.currentTimeMillis() + getPeriod());
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
