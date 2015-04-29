/**
 *
 * @author balvisio
 * @author abachrac
 * @author sachih
 * 
 * Solutions created by Bruno Alvisio (balvisio@mit.edu), modified by Abe Bachrach(abachrac@mit.edu) 4/23/2010, Modified by Sachi Hemachandra 4/20/2011
 *
 */

import java.util.*;

import javax.realtime.*;

public class Simulator implements Runnable, CostOverruner, DeadlineMisser, inSimulator {

	public ArrayList<GroundVehicle> gvlist;
	private static DisplayClient dc;
    public static double simTime = 10;//100;
	private double lastTimeUpdate;
        private double startTime = 0;
	public ArrayList<Triplet> deadlineMisscounterList;
	public ArrayList<Triplet> overruncounterList;
	public ArrayList<DoublePair> costList;
    
	public Simulator() {
		gvlist = new ArrayList<GroundVehicle>();
		costList = new ArrayList<DoublePair>();
		deadlineMisscounterList = new ArrayList<Triplet>();
		overruncounterList = new ArrayList<Triplet>();
	}
	
	/**
	 * Convenience function to return the current wallclock time with double precision
	 * 
	 * @return time in seconds.
	 */
	static public double getRealTimeSeconds() {
		double timeNowInMilliseconds = Clock.getRealtimeClock().getTime().getMilliseconds()
				+ ((double) Clock.getRealtimeClock().getTime().getNanoseconds()) / 1000000.0;
		return timeNowInMilliseconds / 1000.0;
	}

	/**
	 * Adds a groundVehicle to the ArrayList of GroundVehicles
	 * 
	 * @param gv
	 *            GroundVehicle to be added to the ArrayList
	 */
	public void addGroundVehicle(GroundVehicle gv) {
		synchronized (this) {
			this.gvlist.add(gv);
		}
	}

	public void setDisplayClient(DisplayClient d) {
		dc = d;
	}

    public double getStartTime(){
	return startTime;
    }

	public void run() {
		
	        startTime = Simulator.getRealTimeSeconds();
		lastTimeUpdate = Simulator.getRealTimeSeconds();
		while (Simulator.getRealTimeSeconds()-startTime < this.simTime) {
			double loopStartTime = Simulator.getRealTimeSeconds();
			double timeNow = Simulator.getRealTimeSeconds();

			double deltaT = timeNow - lastTimeUpdate; // Microseconds

			lastTimeUpdate = timeNow;
			
			/*for (GroundVehicle gv : this.gvlist) {
			    gv.(deltaT);
			    }*/
			this.drawVehicles(); // Display Vehicles in VisualServer
			costList.add(new DoublePair(Simulator.getRealTimeSeconds() - -startTime, Simulator.getRealTimeSeconds() - loopStartTime));
			
			//wait for the next period
			if (RtTest.isRealTime) {
				RealtimeThread.waitForNextPeriod();
			} else {
				//wait for 100ms
				while (Simulator.getRealTimeSeconds() - loopStartTime < RtTest.simPeriod) {
					try {
					    //System.out.printf("Sleeping : %f\n", );
						Thread.sleep(1); 
					} catch (InterruptedException e) {
					}
					;
				}
			}
			//System.out.printf("Time : %f\n", Simulator.getRealTimeSeconds()-startTime);
		}
		System.out.println("Sim Thread Done\n");
	}

	/**
	 * This method allows us to plot the vehicle positions on the DisplayServer
	 * screen.
	 * 
	 */
	public void drawVehicles() {
	    
		double[] gvX;
		double[] gvY;
		double[] gvTheta;
		gvX = new double[gvlist.size()];
		gvY = new double[gvlist.size()];
		gvTheta = new double[gvlist.size()];

		for (int i = 0; i < gvlist.size(); i++) {
			synchronized (gvlist.get(i)) {
				gvX[i] = gvlist.get(i).getPosition()[0]; // GroundVehicle
				// Position: Shared
				// Resource
				gvY[i] = gvlist.get(i).getPosition()[1]; // GroundVehicle
				// Position: Shared
				// Resource
				gvTheta[i] = gvlist.get(i).getPosition()[2]; // GroundVehicle
				// Position:
				// Shared
				// Resource
			}
		}
		dc.update(gvlist.size(), gvX, gvY, gvTheta);
	}

	public void printError(double xPos, double yPos) {
		double distance = Math.sqrt(Math.pow((50 - xPos), 2) + Math.pow((50 - yPos), 2));
		double error = distance - 25;
		System.out.println(Simulator.getRealTimeSeconds() + " " + error);
	}

	public ArrayList<DoublePair> getCostList() {
		return costList;
	}

	public ArrayList<Triplet> getDealineMissedList() {
		return this.deadlineMisscounterList;
	}

	public int getMissedDeadlines() {
		return deadlineMisscounterList.size();
	}

	public void storeMissDeadlineInfo(int deadlineNumber, double time, double currentDeadLine) {
		this.deadlineMisscounterList.add(new Triplet(time, deadlineNumber, currentDeadLine));
	}

	public int getnumberOfOverruns() {
		return this.overruncounterList.size();
	}

	public void storeOverrunInfo(int deadlineNumber, double time, double currentCostTime) {
		this.overruncounterList.add(new Triplet(time, deadlineNumber, currentCostTime));
	}

	public ArrayList<Triplet> getOverrunList() {
		return overruncounterList;
	}

	public Simulator getMySimulator() {
		return this;
	}

}