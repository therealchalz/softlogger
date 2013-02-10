package ca.brood.softlogger.scheduler;

public class PeriodicSchedulee implements Schedulee, Comparable<Schedulee> {

	private long nextRun;
	private int period;
	private Runnable action;
	
	public PeriodicSchedulee(int period) {
		this.period = period;
		nextRun = System.currentTimeMillis() + period;
		
	}
	public PeriodicSchedulee(int period, Runnable action) {
		this(period);
		this.action = action;
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
	public int compareTo(Schedulee other) {
		return (int)(this.getNextRun() - other.getNextRun());
	}
	@Override
	public long getNextRun() {
		return nextRun;
	}

}
