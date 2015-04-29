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
import java.lang.IllegalArgumentException;

public class FollowingController extends VehicleController
{
    private GroundVehicle leadergv;
    public FollowingController(Simulator s, GroundVehicle targetVehicle,  GroundVehicle followingVehicle){
	super(s,followingVehicle);
	this.leadergv = targetVehicle;
    }

    public Control getControl(int sec, int usec) {
	double desiredOmega = 5;
	double[] leaderPos;
	double[] myPos;

	leaderPos = leadergv.getPosition();
	myPos = getGroundVehicle().getPosition();

	double xDiff = leaderPos[0] - myPos[0];
	double yDiff = leaderPos[1] - myPos[1];
	double desiredTheta = Math.atan2(yDiff, xDiff);

	double gain = 5;
	desiredTheta = convertAngle(desiredTheta);
	desiredOmega = convertAngle(desiredTheta - myPos[2]);

	desiredOmega *= gain;
	if (desiredOmega > Math.PI/4) {
	    desiredOmega = Math.PI/4;
	}
	if (desiredOmega < -Math.PI/4) {
	    desiredOmega = -Math.PI/4;
	}

	double distance = Math.sqrt(xDiff * xDiff + yDiff * yDiff);
	double desiredSpeed = 10;
	if (distance < 10) {
	    desiredSpeed = 10 - distance;
	}
	if (desiredSpeed < 5) {
	    desiredSpeed = 5;
	}

	Control c = new Control(desiredOmega, desiredSpeed);
	return c;
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
}