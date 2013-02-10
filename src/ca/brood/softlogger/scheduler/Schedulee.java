package ca.brood.softlogger.scheduler;

public interface Schedulee {
	public long getNextRun();
	public void execute();
}
