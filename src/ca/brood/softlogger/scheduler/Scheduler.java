package ca.brood.softlogger.scheduler;


import org.apache.log4j.Logger;
import ca.brood.softlogger.util.ThreadPerformanceMonitor;

public class Scheduler {
	private SchedulerRunner runner;
	
	public Scheduler () {
		runner = new SchedulerRunner();
	}
	
	public void addSchedulee(Schedulable sch) {
		runner.addSchedulee(sch);
	}
	
	public void start() {
		runner.beginRunner();
	}
	
	public void stop() {
		runner.stopRunner();
	}
	
	public void setThreadName(String name) {
		runner.setName(name);
	}
	
	private class SchedulerRunner extends Thread {
		private SchedulerQueue schedulerQueue;
		private Boolean shouldRun;
		private Logger log;
		
		public SchedulerRunner() {
			schedulerQueue = new SchedulerQueue();
			shouldRun = new Boolean(false);
			log = Logger.getLogger(SchedulerRunner.class);
		}
		public void addSchedulee(Schedulable sch) {
			schedulerQueue.add(sch);
		}
		public void beginRunner() {
			setShouldRun(true);
			if (!this.isAlive()) {
				this.start();
			}
		}
		
		public void stopRunner() {
			setShouldRun(false);
			if (this.isAlive()) {
				this.interrupt();
			}
		}
		
		private boolean getShouldRun() {
			synchronized (shouldRun) {
				return shouldRun;
			}
		}
		
		private void setShouldRun(boolean should) {
			synchronized (shouldRun) {
				shouldRun = should;
			}
		}
		@Override
		public void run() {
			
			if (schedulerQueue.peek() == null) {
				log.error("No devices configured.  Exiting thread.");
				this.setShouldRun(false);
				return;
			}
			
			ThreadPerformanceMonitor.threadStarting();
			//log.info("Running");

			long currentTime;
			
			while (getShouldRun()) {
				currentTime = System.currentTimeMillis();
				
				while (schedulerQueue.peek().getNextRun() <= currentTime) {
					 Schedulable s = schedulerQueue.poll();
					 s.execute();
					 schedulerQueue.add(s);
				}
				
				long wakeTime = schedulerQueue.peek().getNextRun();
				long sleepTime = wakeTime - System.currentTimeMillis();
				if (sleepTime > 10) {
					try {
						//log.trace("Sleeping for "+sleepTime+" milliseconds.");
						ThreadPerformanceMonitor.threadStopping();
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
					}
				}
				ThreadPerformanceMonitor.threadStarting();
			}
		}
		
	}
}
