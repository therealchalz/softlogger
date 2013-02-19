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
