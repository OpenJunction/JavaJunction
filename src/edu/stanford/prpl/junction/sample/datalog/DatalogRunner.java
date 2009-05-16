package edu.stanford.prpl.junction.sample.datalog;

public class DatalogRunner {
	public static void main(final String[] argv) {
		try {
			
			System.out.println("Starting the datalog activity.");
			
			new Thread() {
				@Override
				public void run() {
					DatalogActorDB.main(argv);
				}
			}.start();
			
			
			Thread.sleep(1000);
			
			new Thread() {
				@Override
				public void run() {
					DatalogActorQuerier.main(argv);
				}
			}.start();
			
			Thread.sleep(10*60*1000);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
