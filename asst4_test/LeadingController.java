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

public class LeadingController extends VehicleController {

    private ArrayList<GroundVehicle> gvList;

    public LeadingController(Simulator s, GroundVehicle gv) {
	super(s, gv);
	gvList = new ArrayList<GroundVehicle>();
    }

    /**
     * Adds a GroundVehicle to the list of chasers considered by this
     * groundVehicle.
     * 
     * @param gv
     */
    public void addFollower(GroundVehicle gv) {
	gvList.add(gv);
    }

    /**
     * Returns a control negating the output for the FollowingControler. Added
     * special controls when the GroundVehicle is close to the walls.
     */
    public Control getControl(int sec, int usec) {
	GroundVehicle closestVehicle = this.getClosestVehicle();

	//if no closest vehicle return null
	if(closestVehicle == null){
	    return null;
	}

	double desiredOmega = 0;
	double[] chaserPos;
	double[] myPos;

	chaserPos = closestVehicle.getPosition(); /* Shared Resource */
	
	myPos = getGroundVehicle().getPosition(); /* Shared Resource */

	/*Attempt to get more than one lock - uncomment to see exception thrown*/

	/*
	GroundVehicle leadVehicle = getGroundVehicle();

	synchronized(closestVehicle){
	    synchronized(leadVehicle){//should throw exception 
		System.out.printf("-------------------Called-------------\n");
		closestVehicle.getPosition();
		leadVehicle.getPosition();
	    }
	}
	*/	    

	double xDiff = chaserPos[0] - myPos[0];
	double yDiff = chaserPos[1] - myPos[1];
	double desiredTheta = Math.atan2(yDiff, xDiff);
	
	double gain = 5;
	desiredTheta = convertAngle(desiredTheta);
	desiredOmega = convertAngle(desiredTheta - myPos[2]);

	/*works without this part*/
	
	//rot velocity is 0 if there is PI angle difference between the two vehicles 
	//otherwise it is propotinal 
	if(desiredOmega >=0){
	    desiredOmega = Math.min(desiredOmega, Math.PI);
	    desiredOmega = Math.PI - desiredOmega;
	}
	else{
	    desiredOmega = Math.max(desiredOmega, -Math.PI);
	    desiredOmega = -Math.PI - desiredOmega;
	}

	desiredOmega *= gain;
	if (desiredOmega > Math.PI/4) {
	    desiredOmega = Math.PI/4;
	}
	if (desiredOmega < -Math.PI/4) {
	    desiredOmega = -Math.PI/4;
	}

	double desiredSpeed = 10;

	// Wall cases and corner cases.
	Control a = avoidWalls(myPos);
	if (a != null)
	    return a;

	Control c = new Control(-desiredOmega, desiredSpeed);
	return c;
    }

    /**
     * @return the closest GroundVehicle to the leader
     */
    public GroundVehicle getClosestVehicle() {

	/* this avoids deadlock by not holding more than one lock at a time.  
	*  ideally to be accurate - the leading controller needs to hold all follower 
	*  vehicle locks and get the positions at that instance 
	*/

	double[] leaderPosition = this.getGroundVehicle().getPosition();
	double closestDistance = Double.MAX_VALUE;
	GroundVehicle closestVehicle = null;
	for (GroundVehicle v : gvList) {
	    double[] followerPosition = v.getPosition();
	    double xDistance = leaderPosition[0] - followerPosition[0];
	    double yDistance = leaderPosition[1] - followerPosition[1];
	    double totalDistance = Math.hypot(xDistance, yDistance);
	    if (totalDistance < closestDistance) {
		closestDistance = totalDistance;
		closestVehicle = v;
	    }
	}
	assert (closestVehicle != null);
	return closestVehicle;
    }

    public ArrayList<GroundVehicle> getFollowersList() {
	return gvList;
    }
}