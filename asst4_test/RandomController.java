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

public class RandomController extends VehicleController
{
    private double minTransSpeed = 5;
    private double maxTransSpeed = 8;
    private double maxRotSpeed = Math.toRadians(10);
    private boolean controllerInitialized = false;
   

    public RandomController(Simulator s, GroundVehicle v){
	super(s,v);	
    }

    public Control getControl(int sec, int usec) {
	// avoid walls if we're too close
	Control a = avoidWalls(this.getGroundVehicle().getPosition());
	if (a != null)
	    return a;
		
	// otherwise generate a random control
	Random rng = new Random();
	Control c = new Control(rng.nextDouble() * Math.PI / 2.0 - Math.PI / 4.0, rng.nextDouble() * 5 + 5);

	return c;
    }   
}