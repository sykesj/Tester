/**
 *
 * @author balvisio
 * @author abachrac
 * @author sachih
 * 
 * Solutions created by Bruno Alvisio (balvisio@mit.edu), modified by Abe Bachrach(abachrac@mit.edu) 4/23/2010, Modified by Sachi Hemachandra 4/20/2011
 *
 */

import javax.realtime.*;

public class DeadlineMissHandler extends AsyncEventHandler {
	RealtimeThread myrt;
	DeadlineMisser dm;

	public DeadlineMissHandler() {
		super(new PriorityParameters(PriorityScheduler.getMaxPriority(null) - 1), null, null, null, null, null);
	}

	public void setThread(RealtimeThread rt) {
		myrt = rt;
	}

	public void setDeadlineMisser(DeadlineMisser dm) {
		this.dm = dm;
	}

	public void handleAsyncEvent() {
		ReleaseParameters rp = myrt.getReleaseParameters();
		RelativeTime deadline = rp.getDeadline();
		RelativeTime newDeadline = new RelativeTime(deadline);
		newDeadline.add(0, RtTest.dt,newDeadline);
		rp.setDeadline(newDeadline);
		myrt.schedulePeriodic();
		//System.out.println("Deadline of "+ deadline.toString() + " Missed!");
	

		int missedDealines = dm.getMissedDeadlines() + 1;
		double currentDeadline = newDeadline.getMilliseconds() + ((double) newDeadline.getNanoseconds()) / 1.0e6;
		double currentTime = Simulator.getRealTimeSeconds() - this.dm.getStartTime();
		dm.storeMissDeadlineInfo(missedDealines, currentTime, currentDeadline);
	}
}