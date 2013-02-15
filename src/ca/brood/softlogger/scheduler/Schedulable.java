package ca.brood.softlogger.scheduler;

public interface Schedulable {
	public long getNextRun();
	public void execute();
}
