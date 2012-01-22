package ca.brood;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;


public class Softlogger {
	private Logger log;
	public Softlogger() {
		log = Logger.getLogger(Softlogger.class);
		PropertyConfigurator.configure("logger.config");
		log.fatal("Testing");
	}
	public void run() {

	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		System.out.println("Starting...");
		Softlogger s = new Softlogger();
		s.run();
	}

}
