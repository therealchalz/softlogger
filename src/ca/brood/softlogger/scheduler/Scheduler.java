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
		private String description;
		
		public SchedulerRunner() {
			schedulerQueue = new SchedulerQueue();
			shouldRun = new Boolean(false);
			log = Logger.getLogger(SchedulerRunner.class);
			description = "Unnamed";
		}
		public void addSchedulee(Schedulable sch) {
			schedulerQueue.add(sch);
		}
		public void beginRunner() {
			log.info(description+"- Starting");
			boolean start = !getShouldRun();
			setShouldRun(true);
			if (start) {
				this.start();
			}
		}
		
		public void stopRunner() {
			log.info(description+"- Stopping");
			boolean interrupt = getShouldRun() | this.isAlive();
			setShouldRun(false);
			if (interrupt) {
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
			
			description = Thread.currentThread().getName();
			
			ThreadPerformanceMonitor.threadStarting();
			log.info("Running");

			long currentTime;
			
			while (getShouldRun()) {
				currentTime = System.currentTimeMillis();
				
				while (schedulerQueue.peek().getNextRun() <= currentTime) {
					 Schedulable s = schedulerQueue.poll();
					 s.execute();
					 schedulerQueue.add(s);
				}
				
				//The schedulable's action could take a while, so it's possible
				//that we get stopped during the action.  If so, we want to bail
				//as soon as the action is finished:
				if (!getShouldRun()) {
					break;
				}
				
				long wakeTime = schedulerQueue.peek().getNextRun();
				long sleepTime = wakeTime - System.currentTimeMillis();
				if (sleepTime > 10) {
					try {
						log.trace("Sleeping for "+sleepTime+" milliseconds.");
						ThreadPerformanceMonitor.threadStopping();
						Thread.sleep(sleepTime);
					} catch (InterruptedException e) {
					}
				}
				ThreadPerformanceMonitor.threadStarting();
			}
			log.info("Thread exiting");
			ThreadPerformanceMonitor.threadStopping();
		}
		
	}
}
