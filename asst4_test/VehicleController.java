/**
 *
 * @author balvisio
 * @author abachrac
 * @author sachih
 * 
 * Solutions created by Bruno Alvisio (balvisio@mit.edu), modified by Abe Bachrach(abachrac@mit.edu) 4/23/2010, Modified by Sachi Hemachandra 4/20/2011
 *
 */


import java.util.ArrayList;
import javax.realtime.RealtimeThread;

public class VehicleController implements Runnable, CostOverruner, DeadlineMisser, inSimulator {

    public Simulator sim;
    private GroundVehicle gv;
    private ArrayList<DoublePair> costList;
    private ArrayList<Triplet> deadlineMisscounterList;
    private ArrayList<Triplet> overruncounterList;
    private double startTime = 0;
    // Hard-coded constraints come from documentation. Min translation
    // speed of the vehicle is 5 m/s, max translation speed is 10 m/s,
    // max rotational speed is PI. Radius of outer circle is 50

    private double avoidWallDist = 10;

    //no of points in the star
    private int _numSides = 5;
    //how the star is drawn - every _q th point after is connected
    private int _q = 2;

    private double minTransSpeed = 5;
    private double maxTransSpeed = 10;
    private double maxRotSpeed = Math.PI/4;
    private double minRotSpeed = -Math.PI/4;
    private double circumCircleRadius = 25.0;

    private boolean isTurning = false; //is the vehicle turning 
    private boolean isAtOuter = false; //flag indicating the vehicle is at outer turn

    protected static int count = 0;
    protected int ID = 0;
    
    private boolean isSpecialMode = false; 

    private boolean controllerInitialized = false;

    private double turnDurationOuter = 0;//turn time in the outer point
    private double turnDurationInner = 0;//turn duration in the inner point

    private double edgeTravelDuration = 0;//this is the same for either direction

    private double timeOfManoeuverStart = 0; 

    public VehicleController(Simulator s, GroundVehicle v) {
	//the ground vehicle position will already be initialized 
	if(s == null)
	    throw new IllegalArgumentException("No simulator specified ");  
	if(v == null)
	    throw new IllegalArgumentException("No vehicle specified");
	
	this.ID = count;
	count++;
	
	this.sim = s;
	this.gv = v;
	gv.setVehicleController(this);
	costList = new ArrayList<DoublePair>();
	deadlineMisscounterList = new ArrayList<Triplet>();
	overruncounterList = new ArrayList<Triplet>();
    }

    /**
     * This method sets the VehicleController to work. It: 1. Gets the time from
     * the simulator. 2. Checks if the simulation time is greater than 100 sec.
     * 3. Checks if the last time the clock was checked is the same. 4. Get the
     * GroundVehicle state. 5. Calls the "getControl" method and applies to the
     * GroundVehicle 6. Sets the "isUpdated" variable to true.
     */

    public double getStartTime(){
	return startTime;
    }

    public void run() {
	startTime = Simulator.getRealTimeSeconds();

	while (Simulator.getRealTimeSeconds()-startTime <= Simulator.simTime) {
	    //System.out.printf(".");
	    double loopStartTime = Simulator.getRealTimeSeconds(); // Store time
	    double timeNow = Simulator.getRealTimeSeconds();
	    //convert time for control
	    int currentSec = (int) timeNow;
	    int currentUSec = (int) ((timeNow - currentSec) * 1e6);
	    Control c = this.getControl(currentSec, currentUSec);
	    if (c != null) {
		gv.controlVehicle(c);
	    }
	    //			System.out.println("control!");
			
	    costList.add(new DoublePair(Simulator.getRealTimeSeconds() - startTime, Simulator.getRealTimeSeconds() - loopStartTime));
			
	    //wait for the next period
	    if (RtTest.isRealTime) {
		RealtimeThread.waitForNextPeriod();
	    } else {
		// wait for 100ms
		while (Simulator.getRealTimeSeconds() - loopStartTime < RtTest.controlPeriod) {
		    try {
			Thread.sleep(1);
		    } catch (InterruptedException e) {
		    }
		    ;
		}

	    }
	}
	System.out.println("Controller Thread " + this.gv.getVehicleId() + " is done");
    }

    //controller methods
    public int setNumPoints(int n) {
	if (n >= 3 || n <= 10){
	    _numSides = n;
	}
	return _numSides;
    }

    private void initializeController() {

	double minTurningRadius = minTransSpeed / maxRotSpeed;

	/* The bulk of this method is to determine how long to spend turning at
	 * each corner of the star, and how long to spend driving along each
	 * edge. We calculate turnDurationOuter and turnDurationInner
	 * and edgeTravelDuration, and then use
	 * these inside getControl to decide when to switch from turning to
	 * travelling straight and back again. */ 

	if(_numSides < 5){ //for stars with less than 5 points the path needs to do something different

	    isSpecialMode = true;
	  
	    double turningAngle = Math.PI - Math.PI *(_numSides - 2) / _numSides;
	    double arcLengthOuter = turningAngle * minTurningRadius;

	    //in special mode we only use the outer turning angle 
	    turnDurationOuter = arcLengthOuter / minTransSpeed;

	    double alpha = Math.PI *(_numSides - 2) / _numSides/ 2;
	    double beta = turningAngle / 2;   	

	    double l1 = minTurningRadius / Math.tan(alpha);

	    double R_act = l1 * Math.cos(alpha) + 
		circumCircleRadius - (minTurningRadius -  minTurningRadius * Math.cos(beta));

	    //Size of a single edge on the star - see diagram 

	    double gamma = Math.PI / _numSides;
	    double d = 2 * R_act * Math.sin(gamma);
	
	    //Length of the edge travelled 
	    double edgeLength = d - 2*l1;
	
	    //duration of the straight travel 
	    edgeTravelDuration = edgeLength/maxTransSpeed;

	}

	else{
	    /*Refer to the pdf for the defined angles 
	     * alpha, beta, theta, gamma, R_act
	     */

	    //----------Outer Turnining angle--------------//

	    /* Firstly, we know we need to turn the vehicle by PI - the internal angle's at the outer edge of the star*/

	    //Internal angle of the star is given by Math.PI*(_numSides-2*_q)/_numSides

	    double turningAngleOuter = Math.PI - Math.PI*(_numSides-2*_q)/_numSides;
    
	    /* And we're going to turn the vehicle along the circumference of the
	     * smallest circle we can make. */ 
	

	    /* The distance we have to travel along that smallest circle is a function
	     * of the angle and the radius, and is an arc along that small circle. */

	    double arcLengthOuter = turningAngleOuter * minTurningRadius;

	    /* We can work out how long each outer turn will take based on the arcLength and
	     * how fast we are travelling. Of course, we could also work it out based
	     * on the angle and our maximum angular velocity. */

	    turnDurationOuter = arcLengthOuter / minTransSpeed;


	    //-------Inner Turning angle---------//

	    //Simmilar scenario for the inner turning angle

	    double alpha = (Math.PI*(_numSides-2*_q)/_numSides)/2;                                                                                                                                                             
	    double gamma = Math.PI/_numSides;
	    double theta = alpha + gamma;          
                                        
	    double turningAngleInner = Math.PI - 2 * theta;

	    /* The distance we have to travel along that smallest circle is a function
	     * of the angle and the radius, and is an arc along that small circle. */
	    double arcLengthInner = turningAngleInner * minTurningRadius;

	    /* We can work out how long each inner turn will take based on the arcLength and
	     * how fast we are travelling. Of course, we could also work it out based	 
	     * on the angle and our maximum angular velocity. */

	    turnDurationInner = arcLengthInner / minTransSpeed;

	    //---------Edge Length Calculation-------------//
	
	    double beta = turningAngleOuter / 2;   	

	    //Distances not traveled on the star edge because of the curved trajectories

	    double l1 = minTurningRadius / Math.tan(alpha);

	    double l2 = minTurningRadius / Math.tan(theta);

	    /* R_act (Actual Radius) - distance from the center to any outer corner of the star 
	     * (Larger than circumCircleRadius because of the curved trajectories)
	     */
	
	    double R_act = l1 * Math.cos(alpha) + 
		circumCircleRadius - (minTurningRadius -  minTurningRadius * Math.cos(beta));

	    //Size of a single edge on the star - see diagram 
	
	    double d = R_act * Math.cos(alpha)/(1 + Math.sin(alpha));
	
	    //Length of the edge travelled 
	
	    double edgeLength = d - l1 - l2;
	
	    //duration of the straight travel 

	    edgeTravelDuration = edgeLength/maxTransSpeed;

	    /* Also in method, we initialize the controller state. It's a little ugly,
	     * but we'll start as if we're half-way through an outer turn, and tangent to the
	     * outer circle. This makes it easy to put the vehicle on a legal part of
	     * the star, rather than having to drive to it. */ 
	}

	isTurning = true;
	timeOfManoeuverStart = -turnDurationOuter/2.0; 
	isAtOuter = true;
	
	controllerInitialized = true;
	
    }

    public Control getControl(int sec, int usec) {
	double controlTime = sec+usec*1E-6;
		
	Control nextControl = null;

	if (!controllerInitialized) 
	    initializeController();

	//if the vehicle is turning 
	
	if (isTurning) {
	    double currentTurnDuration = .0;

	    /*in special mode we only do the outer turn (to outline triangle and square)
	     *otherwise we do two types of turns alternating between minRotationalVelocity and 
	     *maxRotationalVelocity for the outer and inner edges
	     */

	    if(isSpecialMode){ 
		currentTurnDuration= turnDurationOuter;		
	    }
	    else{
		if(isAtOuter){
		    currentTurnDuration= turnDurationOuter;
		}
		else{
		    currentTurnDuration= turnDurationInner;
		}
	    }
	    //if the turn is not complete 
	    if (controlTime - timeOfManoeuverStart < currentTurnDuration){
		if(isSpecialMode){
		    nextControl = new Control(maxRotSpeed, 5);
		}
		else{
		    if(isAtOuter){
			nextControl = new Control(maxRotSpeed, 5);
		    }
		    else{
			//we need to turn in the other direction if performing an inner turn
			nextControl = new Control(minRotSpeed, 5);
		    }
		}
	    }

	    //we are done turning - go over to travelling in straight line 
	    else {
		isTurning = false;
		isAtOuter = !isAtOuter; //flip the parameter as we alternate between inner and outer turns
		timeOfManoeuverStart = controlTime;
		nextControl = new Control(0, 10);
	    } 
	}
	else {
	    if (controlTime - timeOfManoeuverStart < edgeTravelDuration)
		nextControl = new Control(0, 10);
	    //done with the edge travel - start making the turn 
	    else { 		
		isTurning = true;
		timeOfManoeuverStart = controlTime;
		if(isSpecialMode){
		    nextControl = new Control(maxRotSpeed, 5);
		}
		else{
		    if(isAtOuter){
			nextControl = new Control(maxRotSpeed, 5);
		    }
		    else{
			nextControl = new Control(minRotSpeed, 5);
		    }
		}
	    } 
	}
	return nextControl;
    }
    
    /**
     * @return the GroundVehicle associated with this VehicleController
     */
    public GroundVehicle getGroundVehicle() {
	return gv;
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

    public Simulator getMySimulator() {
	return this.sim;
    }

    public static double convertAngle(double theta) {
	while (theta < -Math.PI) {
	    theta += 2 * Math.PI;
	}
	while (theta > Math.PI) {
	    theta -= 2 * Math.PI;
	}
	return theta;
    }

    public Control avoidWalls(double[] pos) {
	if (pos[0] > 100 - avoidWallDist && pos[1] > 100 - avoidWallDist) {
	    if (pos[2] > -3 * Math.PI / 4.0) {
		return new Control(-Math.PI/4, 5);
	    } else {
		return new Control(+Math.PI/4, 5);
	    }
	}

	if (pos[0] > 100 - avoidWallDist && pos[1] < 0 + avoidWallDist) {
	    if (pos[2] > 3 * Math.PI / 4.0) {
		return new Control(-Math.PI/4, 5);
	    } else {
		return new Control(+Math.PI/4, 5);
	    }
	}

	if (pos[0] < 0 + avoidWallDist && pos[1] > 100 - avoidWallDist) {
	    if (pos[2] > -Math.PI / 4.0) {
		return new Control(-Math.PI/4, 5);
	    } else {
		return new Control(+Math.PI/4, 5);
	    }
	}

	if (pos[0] < 0 + avoidWallDist && pos[1] < 0 + avoidWallDist) {
	    if (pos[2] > Math.PI / 4.0) {
		return new Control(-Math.PI/4, 5);
	    } else {
		return new Control(+Math.PI/4, 5);
	    }
	}

	if (pos[0] > 100 - avoidWallDist) {
	    if (pos[2] > 0) {
		return new Control(+Math.PI/4, 5);
	    } else {
		return new Control(-Math.PI/4, 5);
	    }
	}
	if (pos[0] < 0 + avoidWallDist) {
	    if (pos[2] > 0) {
		return new Control(-Math.PI / 4, 5);
	    } else {
		return new Control(Math.PI / 4, 5);
	    }
	}

	if (pos[1] < 0 + avoidWallDist) {
	    if (pos[2] > Math.PI / 2) {
		return new Control(-Math.PI / 4, 5);
	    } else {
		return new Control(Math.PI / 4, 5);
	    }
	}

	if (pos[1] > 100- avoidWallDist) {
	    if (pos[2] > -Math.PI / 2) {
		return new Control(-Math.PI / 4, 5);
	    } else {
		return new Control(Math.PI / 4, 5);
	    }
	}
	return null;
    }    
}
