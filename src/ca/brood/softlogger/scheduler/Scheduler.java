package ca.brood.softlogger.scheduler;

import java.util.PriorityQueue;

import ca.brood.softlogger.modbus.Device;
import ca.brood.softlogger.util.ThreadPerformanceMonitor;

public class Scheduler {
	private SchedulerRunner runner;
	
	public Scheduler () {
		runner = new SchedulerRunner();
	}
	
	public void addSchedulee(Schedulee sch) {
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
		private ScheduleeQueue scheduleeQueue;
		private Boolean shouldRun;
		
		public SchedulerRunner() {
			scheduleeQueue = new ScheduleeQueue();
			shouldRun = new Boolean(false);
		}
		public void addSchedulee(Schedulee sch) {
			scheduleeQueue.add(sch);
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
			ThreadPerformanceMonitor.threadStarting();
			//log.info("Running");

			long currentTime;
			
			while (getShouldRun()) {
				//TODO: look for improvements
				currentTime = System.currentTimeMillis();
				
				for (Schedulee s : scheduleeQueue) {
					if (s.getNextRun() <= currentTime) {
						s.execute();
					}
				}
				
				long wakeTime = Long.MAX_VALUE;
				
				for (Schedulee s: scheduleeQueue) {
					if (s.getNextRun() < wakeTime) {
						wakeTime = s.getNextRun();
					}
				}
				
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
