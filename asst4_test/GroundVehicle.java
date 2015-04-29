/**
 *
 * @author balvisio
 * @author abachrac
 * @author sachih
 * 
 * Solutions created by Bruno Alvisio (balvisio@mit.edu), modified by Abe Bachrach(abachrac@mit.edu) 4/23/2010, Modified by Sachi Hemachandra 4/20/2011
 *
 */

import java.util.Random;
import java.util.concurrent.locks.ReentrantLock;
import java.util.ArrayList;
import javax.realtime.RealtimeThread;

public class GroundVehicle implements Runnable, CostOverruner, DeadlineMisser
{

    private double dx;
    private double dy;
    private double dtheta;
    private double[] pose;
    private VehicleController vc;
    private Random r;
    private int vehicleId;
    private static int vehicleCount = 0;
    private ReentrantLock mygvLock;
    private ArrayList<double[]> pose_list;
    private double startTime = 0;
    private ArrayList<DoublePair> costList;
    private ArrayList<Triplet> deadlineMisscounterList;
    private ArrayList<Triplet> overruncounterList;
	
    /**
     * 
     * @param pose
     *            Current position of the vehicle. Array of length 3: {x, y,
     *            theta}
     * @param dx
     *            velocity in the x-direction
     * @param dy
     *            velocity in the y-direction
     * @param dtheta
     *            angular velocity
     * 
     * @throws IllegalArgumentException
     *             if the length of the position array is not 3.
     */
    public GroundVehicle(double[] pose, double dx, double dy, double dtheta) throws IllegalArgumentException {
	if (pose.length != 3) {
	    throw new IllegalArgumentException("Length of Position Array should be 3");
	}

	double[] vel = { dx, dy, dtheta };
	this.setVelocity(vel);
	this.pose = new double[3];
	this.setPosition(pose);

	synchronized (GroundVehicle.class) {
	    vehicleId = vehicleCount;
	    vehicleCount++;
	}

	r = new Random();

	mygvLock = new ReentrantLock();
	pose_list = new ArrayList<double[]>();
	
	costList = new ArrayList<DoublePair>();
	deadlineMisscounterList = new ArrayList<Triplet>();
	overruncounterList = new ArrayList<Triplet>();
    }

    /**
     * Returns an array of 3 doubles, corresponding to dx, dy, dtheta linear and
     * angular velocities of the vehicle.
     * 
     * @return {dx, dy, dtheta}
     */
    public double[] getVelocity() {

	double[] vel = new double[3];
	if (checkIfNoLock()) {
	    synchronized (this) {
		vel[0] = this.dx;
		vel[1] = this.dy;
		vel[2] = this.dtheta;
	    }
	}
	return vel;
    }

    /**
     * Returns an array of 3 doubles, corresponding to the x, y, theta position
     * and orientation of the vehicle
     * 
     * @return [x, y, theta]
     */
    public double[] getPosition() {
	if (checkIfNoLock()) {
	    synchronized (this) {
		return pose;
	    }
	}
	return null;
    }

    /**
     * 
     * @param newPose
     *            new vehicle position. Constraints: 0 <= x <= 100, 0 <= y <=
     *            100, -Pi <= theta <= Pi
     * 
     * @throws IllegalArgumentException
     *             if the given position array's length is not 3.
     */

    public synchronized void setPosition(double[] newPose) {
	synchronized (this) {

	    if (newPose.length != 3) {
		throw new IllegalArgumentException("Invalid parameter. The position array should be of length 3.");
	    }
	    if (newPose[0] < 0) {
		newPose[0] = 0;
	    }
	    if (newPose[0] > 100) {
		newPose[0] = 100;
	    }

	    if (newPose[1] < 0) {
		newPose[1] = 0;
	    }
	    if (newPose[1] > 100) {
		newPose[1] = 100;
	    }

	    newPose[2] = convertAngle(newPose[2]);

	    /*System.out.println("x " + newPose[0]);
	    System.out.println("y " + newPose[1]);
	    System.out.println("theta " + newPose[2]);
	    */
	    System.arraycopy(newPose, 0, pose, 0, 3);
	}

    }

    /**
     * Sets the corresponding velocity representation.
     * 
     * @param newVel
     *            new Velocity Array.
     * 
     * @throws IllegalArgumentException
     *             if the given velocity array's length is not 3
     */

    public synchronized void setVelocity(double[] newVel) throws IllegalArgumentException {

	if (newVel.length != 3) {
	    throw new IllegalArgumentException("Invalid velocity array. It should be of length 3.");
	}

	double vel = Math.sqrt(Math.pow(newVel[0], 2) + Math.pow(newVel[1], 2));
	if (vel > 10) {
	    this.dx = newVel[0] * 10 / vel;
	    this.dy = newVel[1] * 10 / vel;
	} 
	/*else if (vel < 1.0) {
	    this.dx = 1.0 * newVel[0]/vel;
	    this.dy = 1.0 * newVel[1]/vel;
	}*/
	else {
	    this.dx = newVel[0];
	    this.dy = newVel[1];
	}

	if (newVel[2] < -Math.PI / 4) {
	    newVel[2] = -Math.PI / 4;
	}

	if (newVel[2] > Math.PI / 4) {
	    newVel[2] = Math.PI / 4;
	}

	dtheta = newVel[2];
    }

    /**
     * Modifies the internal velocities according to the specified forward speed
     * and rotational velocity.
     * 
     * @param c
     *            Control Object.
     */
    public synchronized void controlVehicle(Control c) {
	double newdx = c.getTransVel() * Math.cos(pose[2]);
	double newdy = c.getTransVel() * Math.sin(pose[2]);
	double newdtheta = c.getRotVel();
	double[] newVelocity = { newdx, newdy, newdtheta };
	this.setVelocity(newVelocity);
    }

    /**
     * Computes dynamic and kinematic changes.
     * 
     * @param sec
     *            seconds
     * @param usec
     *            microseconds
     */
    public synchronized void updateState(double dt) {
	// dt is passed in seconds.microseconds

	double[] newPose = new double[3];
	double errc = 2e-2 * r.nextGaussian();
	double errd = 3e-2 * r.nextGaussian();

	newPose[0] = pose[0] + dx * dt + errd * Math.cos(pose[2]) - errc * Math.sin(pose[2]);
	newPose[1] = pose[1] + dy * dt + errd * Math.sin(pose[2]) + errc * Math.cos(pose[2]);
	newPose[2] = pose[2] + dtheta * dt;
	newPose[2] = convertAngle(newPose[2]);

	//System.out.printf("%f,%f,%f\n", dx, dy, dtheta);
	//System.out.println("x " + newPose[0]);
	//System.out.println("y " + newPose[1]);

	double[] newVel = new double[3];
	double s = Math.sqrt(Math.pow(dx, 2) + Math.pow(dy, 2));
	newVel[0] = s * Math.cos(pose[2]);
	newVel[1] = s * Math.sin(pose[2]);
	newVel[2] = dtheta;

	this.setPosition(newPose);
	this.setVelocity(newVel);
	this.pose_list.add(getPosition()); //save the pose for printing
	//System.out.printf("%f\n",dt);
    }

    /**
     * Sets an angle in the allowed range.
     * 
     * @param theta
     * @return theta in the range [-Pi, Pi]
     */
    public static double convertAngle(double theta) {
	while (theta < -Math.PI) {
	    theta += 2 * Math.PI;
	}
	while (theta > Math.PI) {
	    theta -= 2 * Math.PI;
	}
	return theta;
    }
    
    public double getStartTime(){
	return startTime;
    }

    public void run(){
	//int currentTime = 0;
	//int currentUTime = 0;

	startTime = Simulator.getRealTimeSeconds();
	double lastTimeUpdate = Simulator.getRealTimeSeconds();
	System.out.printf("Called\n"); //wierd seem to work only when this is called ??
	while (Simulator.getRealTimeSeconds()-startTime <= Simulator.simTime) {
	    //System.out.printf("Start Time : %f Last Updated Time : %f\n", startTime, lastTimeUpdate);
	    double loopStartTime = Simulator.getRealTimeSeconds();
	    double timeNow = Simulator.getRealTimeSeconds();
	    
	    double deltaT = timeNow - lastTimeUpdate; // Microseconds

	    updateState(deltaT);

	    //System.out.printf("Time Now: %f Last Updated Time : %f\n", timeNow, lastTimeUpdate);

	    lastTimeUpdate = timeNow;

	    costList.add(new DoublePair(Simulator.getRealTimeSeconds() -startTime, Simulator.getRealTimeSeconds() - loopStartTime));
	    //wait for the next period
	    if (RtTest.isRealTime) {
		RealtimeThread.waitForNextPeriod();
	    } else {
		// wait for next loop 
		while (Simulator.getRealTimeSeconds() - loopStartTime < RtTest.gvPeriod) {
		    try {
			Thread.sleep(1);
		    } catch (InterruptedException e) {
		    }
		    ;
		}

	    }
	}    

    }

    public ArrayList<DoublePair> getCostList() {
	return this.costList;
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
    



    /**
     * Sets the VehicleController of this GroundVehicle
     * 
     * @param vController
     *            : the VehicleController of this GroundVehicle
     */
    public void setVehicleController(VehicleController vController) {
	this.vc = vController;
    }

    /**
     * @return the VehicleController of this GroundVehicle
     */
    public VehicleController getVehicleController() {
	return vc;
    }

    /**
     * @return the id number of the GroundVehicle
     */
    public int getVehicleId() {
	return this.vehicleId;
    }

    /**
     * @return the object lock
     */
    public ReentrantLock getVehicleLock() {
	return this.mygvLock;
    }

    /**
     * Compares the 'Id' instance variable of 'this' with the one of the
     * GroundVehicle passed as a parameter.
     * 
     * @param gv
     *            groundVehicle to be compared with.
     * @return 1 if this vehicle's ID is lower, else -1.
     */
    public int compareId(GroundVehicle gv) {
	if (this.getVehicleId() < gv.getVehicleId())
	    return -1;
	else if (this.getVehicleId() > gv.getVehicleId())
	    return 1;
	else
	    return 0;
    }

    /**
     * This method is used on the get and set methods of Position and Velocity.
     * Before they synchronize on this object they check that the Thread holds
     * no lock.
     * 
     * @return if the thread has no lock.
     */
    private boolean checkIfNoLock() {
	if (vc != null) {
	    try {
		return DeadLockAvoider.hasNoLock(this, vc.sim);
	    } catch (DeadLockAvoiderException d) {
		d.printStackTrace();
		Runtime.getRuntime().exit(1);
	    }
	    return false;
	} else {
	    return true;
	}
    }
	
    public ArrayList<double[]> getPoseList(){
	return this.pose_list;
    }
}