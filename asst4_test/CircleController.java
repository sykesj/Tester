/**
 *
 * @author gomezs
 * @author abachrac
 * @author sachih
 * 
 * Controller Solution created by Steven Gomez (gomezs@mit.edu), modified by Abe Bachrach(abachrac@mit.edu) 4/23/2010,  Modified by Sachi Hemachandra 4/20/2011
 *
 */

public class CircleController extends VehicleController{
	private static final double RADIUS = 25;
	private static final double VELOCITY = 10;
	private static final double eps = 1E-3;


	public CircleController(Simulator s, GroundVehicle v) {
		super(s, v);
		// TODO Auto-generated constructor stub
	}

	public Control getControl(int sec,int usec){
		/*
		 * Strategy is to give the controller a constant speed and angular velocity and adjust based on the error in the circle
		 * for vehicles outside the circle the point that is tangent to the circle and goes through the vehicle is set at the target point,
		 * for vehicles inside the circle a mirror point along the same radial direction and the same distance from the circle but on the outside is
		 * used as the target point.
		 *  
		 * The controller then uses the same logic as the following controller to follow this tangent point around the circle
		 * except that the change in angular velocities are added to the nominal angular velocity required to make a vehicle draw this
		 * circle, instead of zero.
		 */
	    
		double pose[] = this.getGroundVehicle().getPosition();;
		double targetPose[] = new double[2];
		double deltheta = 0;
		double myRad = radius();
		double dr = RADIUS - myRad;
		//if vehcile is within circle calculate mirror point radius
		if(myRad < RADIUS){
			myRad = 2*RADIUS - radius();

		}

		//calculate the angular displacement from the vehicle to the tangent point
		deltheta = Math.acos(RADIUS/myRad);

		//compute the position of the tangent point
		targetPose[0] = RADIUS*Math.cos(angle()+deltheta)+ 50;
		targetPose[1] = RADIUS*Math.sin(angle()+deltheta)+ 50;

		//calculate displacement from target point
		double dx = targetPose[0]-pose[0];
		double dy = targetPose[1]-pose[1];

		//calculate angle change necessary (deltheta variable is reused)
		
		if(Math.sqrt(dx*dx+dy*dy) < eps){
			deltheta = 0;
		}
		else{
			deltheta = Math.atan2(dy, dx) - pose[2];
		}
		//normalize angle between -pi and pi
		while(deltheta > Math.PI) deltheta -= 2*Math.PI;
		while(deltheta < -Math.PI) deltheta += 2*Math.PI;
		
		double desiredTheta = VELOCITY/RADIUS + 2*deltheta - dr;
		double desiredVelocity = VELOCITY;
		
		//normalize angle between -pi and pi
		while(deltheta > Math.PI) desiredTheta -= 2*Math.PI;
		while(deltheta < -Math.PI) desiredTheta += 2*Math.PI;
		
		
		//return control adjust angular velocity by proportional to the angle change necessary and proportional to the error in radius
		return new Control(desiredTheta,desiredVelocity);
	}
	//calculate distance between the vehicle and the center of the circle
	public double radius(){
		double pose[] = this.getGroundVehicle().getPosition();;
		double dist = Math.sqrt((pose[0]-50)*(pose[0]-50)+(pose[1]-50)*(pose[1]-50));
		return dist;
	}
	//calculate angle made between the line connecting the vehicle and the center of the circle and the x axis
	public double angle(){
		double pose[] = this.getGroundVehicle().getPosition();;
		double angle = Math.atan2(pose[1]-50, pose[0]-50);
		return angle;

	}
}