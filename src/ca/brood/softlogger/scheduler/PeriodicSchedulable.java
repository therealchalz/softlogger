package ca.brood.softlogger.scheduler;

public class PeriodicSchedulable implements Schedulable, Comparable<Schedulable> {

	private long nextRun;
	private int period;
	private Runnable action;
	
	public PeriodicSchedulable(int period) {
		this.period = period;
		nextRun = System.currentTimeMillis();
		
	}
	public PeriodicSchedulable(int period, Runnable action) {
		this(period);
		this.action = action;
	}
	
	public int getPeriod() {
		return period;
	}

	@Override
	public void execute() {
		if (this.action != null) {
			this.action.run();
		}
		nextRun = System.currentTimeMillis() + period;
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
		return nextRun;
	}

}
