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
import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.ArrayList;

import javax.realtime.*;

public class RtTest {

    private static DisplayClient dc;
    public static boolean isRealTime = false;
    private static boolean runFollower = true; //set this to true to run 
	
    //period is in seconds

    public static double simPeriod = 0.1;
    public static double controlPeriod = 0.1;
    public static double gvPeriod = 0.1;
    public static int dt = 20000;//100;


    public static void main(String argv[]) {
	String displayServerIP = "127.0.0.1";
	if (argv.length < 1 || argv.length > 2) {
	    System.out.println("USAGE:");
	    System.out.println("RtTest <DisplayServer IP> [realtime]");
	}

	if (argv.length >= 1)
	    displayServerIP = argv[0];

	if (argv.length >= 2 && argv[1].equals("realtime")) {
	    System.out.println("Running in Realtime Mode!");
	    isRealTime = true;
	}	

	dc = new DisplayClient(displayServerIP);
	dc.clear();
	dc.traceOn();

	Simulator sim = new Simulator();
	sim.setDisplayClient(dc);

	// GroundVehicles
	double[] initialPos = { 50, 25, 0 };
	GroundVehicle gv = new GroundVehicle(initialPos, 0, 0, 0);

	//
	GroundVehicle gvLeader = null;
	GroundVehicle gvFollower = null;

	// Controllers
	CircleController controller = new CircleController(sim, gv);

	LeadingController leader = null;
	FollowingController follower = null;

	if (runFollower){
	    Random r = new Random();
	    
	    double[] initialPosLeader = { r.nextDouble() * 100, r.nextDouble() * 100,
				    r.nextDouble() * 2 * Math.PI - Math.PI };
	    double initialdx = r.nextDouble() * 10;
	    double initialdy = r.nextDouble() * 10;
	    double initialOmega = r.nextDouble() * Math.PI / 2.0 - Math.PI / 4;
	    
	    gvLeader = new GroundVehicle(initialPosLeader, initialdx, initialdy, initialOmega);

	    double[] initialPosFollower = { r.nextDouble() * 100, r.nextDouble() * 100,
			   r.nextDouble() * 2 * Math.PI - Math.PI };
	    initialdx = r.nextDouble() * 10;
	    initialdy = r.nextDouble() * 10;
	    initialOmega = r.nextDouble() * Math.PI / 2.0 - Math.PI / 4;
	    
	    gvFollower = new GroundVehicle(initialPosFollower, initialdx, initialdy, initialOmega);
	    sim.addGroundVehicle(gvLeader);
	    sim.addGroundVehicle(gvFollower);
	    
	    //gvLeader.addSimulator(sim);
	    //gvFollower.addSimulator(sim);
	    //create leader/follower pair 
	    leader = new LeadingController(sim, gvLeader);
	    follower = new FollowingController(sim, gvLeader, gvFollower);	    

	    ((LeadingController)leader).addFollower(gvFollower);
	}
		
	// Add to simulator
	sim.addGroundVehicle(gv);
		
	// define thread objects
	Thread simThread;
	Thread controlThread;
	Thread leadControlThread = null;
	Thread followControlThread= null;
	Thread gvThread;
	Thread gvLeaderThread = null;
	Thread gvFollowerThread = null;
		
	if (!isRealTime) {
	    simThread = new Thread(sim);
	    controlThread = new Thread(controller);
	    gvThread = new Thread(gv);

	    if (runFollower){
		leadControlThread = new Thread(leader);
		followControlThread  = new Thread(follower);
		gvLeaderThread = new Thread(gvLeader);
		gvFollowerThread =  new Thread(gvFollower);
	    }

	    
	} else {
	    // Create Handlers
	    //the cost overrun handlers are not supported by the JVM
			
	    DeadlineMissHandler simDeadlineHandler = new DeadlineMissHandler();
	    DeadlineMissHandler controlDeadlineHandler = new DeadlineMissHandler();	    
	    DeadlineMissHandler gvDeadlineHandler = new DeadlineMissHandler();

	    SchedulingParameters scheduling = new PriorityParameters(PriorityScheduler.getMinPriority(null) + 10);


	    ReleaseParameters simRelease = new PeriodicParameters(new RelativeTime(), 
								  new RelativeTime((int)(simPeriod *1000), 0),
								  new RelativeTime(0, 1), 
								  new RelativeTime(0, 1), 
								  null,
								  (AsyncEventHandler)simDeadlineHandler);
	    //int controlPeriod = (int) (1.0 / controlFreq * 1000.0);
	    ReleaseParameters controllerRelease = new PeriodicParameters(new RelativeTime(), 
									 new RelativeTime((int)(controlPeriod *1000), 0), 
									 new RelativeTime(0, 1), 
									 new RelativeTime(0, 1), 
									 null,
									 (AsyncEventHandler)controlDeadlineHandler);

	    
	    
	    ReleaseParameters gvRelease = new PeriodicParameters(new RelativeTime(), 
								 new RelativeTime((int)(gvPeriod *1000), 0), 
								 new RelativeTime(0, 1), 
								 new RelativeTime(0, 1), 
								 null,
								 (AsyncEventHandler)gvDeadlineHandler);

	    simThread = new RealtimeThread(scheduling, simRelease, null, null, null, sim);
	    controlThread = new RealtimeThread(scheduling, controllerRelease, null, null, null, controller);
	    gvThread = new RealtimeThread(scheduling, gvRelease, null, null, null, gv);	

	    // Set Up Handlers
	    simDeadlineHandler.setThread((RealtimeThread) simThread);
	    simDeadlineHandler.setDeadlineMisser(sim);
	    
	    controlDeadlineHandler.setThread((RealtimeThread) controlThread);
	    controlDeadlineHandler.setDeadlineMisser(controller);	    

	    gvDeadlineHandler.setThread((RealtimeThread) gvThread);
	    gvDeadlineHandler.setDeadlineMisser(gv);

	    if (runFollower){
		DeadlineMissHandler leaderControlDeadlineHandler = new DeadlineMissHandler();
		DeadlineMissHandler followerControlDeadlineHandler = new DeadlineMissHandler();

		ReleaseParameters leaderControllerRelease = new PeriodicParameters(new RelativeTime(), 
										   new RelativeTime((int)(controlPeriod *1000), 0), 
										   new RelativeTime(0, 1), 
										   new RelativeTime(0, 1), 
										   null,
										   (AsyncEventHandler)leaderControlDeadlineHandler);
		
		ReleaseParameters followerControllerRelease = new PeriodicParameters(new RelativeTime(), 
										     new RelativeTime((int)(controlPeriod * 1000), 0), 
										     new RelativeTime(0, 1), 
										     new RelativeTime(0, 1), 
										     null,
										     (AsyncEventHandler)followerControlDeadlineHandler);

		DeadlineMissHandler gvLeaderDeadlineHandler = new DeadlineMissHandler();
		DeadlineMissHandler gvFollowerDeadlineHandler = new DeadlineMissHandler();

		ReleaseParameters gvLeaderRelease = new PeriodicParameters(new RelativeTime(), 
									   new RelativeTime((int)(gvPeriod*1000), 0), 
									   new RelativeTime(0, 1), 
									   new RelativeTime(0, 1), 
									   null,
									   (AsyncEventHandler)gvLeaderDeadlineHandler);
		
		ReleaseParameters gvFollowerRelease = new PeriodicParameters(new RelativeTime(), 
									     new RelativeTime((int)(gvPeriod*1000), 0), 
									     new RelativeTime(0, 1), 
									     new RelativeTime(0, 1), 
									     null,
									     (AsyncEventHandler)gvFollowerDeadlineHandler);
		
		leadControlThread = new RealtimeThread(scheduling, leaderControllerRelease, null, null, null, leader);
		followControlThread  = new RealtimeThread(scheduling, followerControllerRelease, null, null, null, follower);
		gvLeaderThread = new RealtimeThread(scheduling, gvLeaderRelease, null, null, null, gvLeader);
		gvFollowerThread =  new RealtimeThread(scheduling, gvFollowerRelease, null, null, null, gvFollower);

		// Set Up Handlers
		leaderControlDeadlineHandler.setThread((RealtimeThread) leadControlThread);
		leaderControlDeadlineHandler.setDeadlineMisser(leader);

		followerControlDeadlineHandler.setThread((RealtimeThread) followControlThread);
		followerControlDeadlineHandler.setDeadlineMisser(follower);

		gvLeaderDeadlineHandler.setThread((RealtimeThread) gvLeaderThread);
		gvLeaderDeadlineHandler.setDeadlineMisser(gvLeader);

		gvFollowerDeadlineHandler.setThread((RealtimeThread) gvFollowerThread);
		gvFollowerDeadlineHandler.setDeadlineMisser(gvFollower);

		System.out.printf("Starting threads ==============\n");
	    }

	    ReleaseParameters rp = ((RealtimeThread)simThread).getReleaseParameters();
	    System.out.println("simThread release at " + ((PeriodicParameters)rp).getPeriod().toString() + " deadline is " + rp.getDeadline().toString());

	    rp = ((RealtimeThread)controlThread).getReleaseParameters();
	    System.out.println("controlThread release at " + ((PeriodicParameters)rp).getPeriod().toString() + " deadline is " + rp.getDeadline().toString());
	    rp = ((RealtimeThread)gvThread).getReleaseParameters();
	    System.out.println("gvThread release at " + ((PeriodicParameters)rp).getPeriod().toString() + " deadline is " + rp.getDeadline().toString());
	    

	}
	try {
	    System.out.println("Starting threads");
	    simThread.start();
	    controlThread.start();
	    gvThread.start();   
	   
	    if (runFollower){
		leadControlThread.start();
		followControlThread.start();
		gvLeaderThread.start();
		gvFollowerThread.start();
		
		System.out.println("--------------Done ");
	    }
	    
	    System.out.println("Joining to threads");
	    simThread.join();
	    controlThread.join();
	    gvThread.join();

	    if(runFollower){
		leadControlThread.join();
                followControlThread.join();
                gvLeaderThread.join();
                gvFollowerThread.join();
	    }
	    
	} catch (Exception e) {
	    System.out.printf("Exception starting thread\n");
	}
	;
		
	
	System.out.println("Writing stats...");
	
	//print the trajectory to a file
	printTrajectory("trajectory.txt",gv.getPoseList());
	// Printing cost Estimation in Files
	printCost("ceCircleController.txt", controller);
	printCost("ceSimulator.txt", sim);
	//can also print gvCost if needed 
	
	if (runFollower){
	    printCost("ceLeadingController.txt", leader);
	    printCost("ceFollowingController.txt", follower);
	    printHandlerInfo("dmLeadingController.txt", leader.getDealineMissedList());
	    printHandlerInfo("dmFollowingController.txt", follower.getDealineMissedList());
	    printHandlerInfo("dmGVLeading.txt", gvLeader.getDealineMissedList());
	    printHandlerInfo("dmGVFollowing.txt", gvFollower.getDealineMissedList());
	    
	}

	
	// Printing Missed Deadlines information in Files
	printHandlerInfo("dmCircleController.txt", controller.getDealineMissedList());
	printHandlerInfo("dmSimulator.txt", sim.getDealineMissedList());
	printHandlerInfo("dmGV.txt", gv.getDealineMissedList());


	System.out.println("Done Writing");	

    }

    /**
     * Outputs the vehicle trajectory to a file
     * 
     * @param fileName
     *            name of the outputFile
     */
    public static void printTrajectory(String fileName, ArrayList<double[]> pose_list) {
	try {
	    // Create file
	    FileWriter fstream = new FileWriter(fileName);
	    PrintWriter outputFileWriter = new PrintWriter(fstream);
			
	    // Print info in Array format
	    for (int i = 0; i < pose_list.size(); i++) {
		outputFileWriter.println(pose_list.get(i)[0] +"," + pose_list.get(i)[1]+"," + pose_list.get(i)[2]);
				
	    }
	    // Close the output stream
	    outputFileWriter.close();
	} catch (Exception e) {// Catch exception if any
	    System.err.println("Error: " + e.getMessage());
	}
    }

	
	
    /**
     * Outputs estimated cost [s] versus time [s] for the objects that
     * implement the CostOverrunable interface
     * 
     * @param fileName
     *            name of the outputFile
     */
    public static void printCost(String fileName, CostOverruner vc) {
	try {
	    // Create file
	    FileWriter fstream = new FileWriter(fileName);
	    PrintWriter outputFileWriter = new PrintWriter(fstream);
	    ArrayList<DoublePair> costList = vc.getCostList();

	    // Print info in Array format
	    for (int i = 0; i < costList.size(); i++) {
		outputFileWriter.print(costList.get(i).number1);
		outputFileWriter.print(",");
		outputFileWriter.print(costList.get(i).number2);
		outputFileWriter.println("");
	    }
	    // Close the output stream
	    outputFileWriter.close();
	} catch (Exception e) {// Catch exception if any
	    System.err.println("Error: " + e.getMessage());
	}
    }

    /**
     * Output information about Missed Deadlines or Cost Overrrun: time[s],
     * number of miseed deadlines so far, current deadline value
     */
    public static void printHandlerInfo(String fileName, ArrayList<Triplet> missList) {
	try {
	    // Create file
	    FileWriter fstream = new FileWriter(fileName);
	    PrintWriter outputFileWriter = new PrintWriter(fstream);

	    // Print info in Array format
	    for (int i = 0; i < missList.size(); i++) {
		outputFileWriter.print(missList.get(i).number1);
		outputFileWriter.print(",");
		outputFileWriter.print(missList.get(i).number2);
		outputFileWriter.print(",");
		outputFileWriter.print(missList.get(i).number3);
		outputFileWriter.println("");
	    }

	    // Close the output stream
	    outputFileWriter.close();
	} catch (Exception e) {// Catch exception if any
	    System.err.println("Error: " + e.getMessage());
	}
    }
}