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

public class CostOverrunHandler extends AsyncEventHandler {
	RealtimeThread rth;
	CostOverruner co;

	public CostOverrunHandler() {

		super(new PriorityParameters(PriorityScheduler.getMaxPriority(null)), null, null, null, null, false);
	}

	public void setThread(RealtimeThread rt) {
		this.rth = rt;
	}

	public void setCostOverrunnerMisser(CostOverruner co) {
		this.co = co;
	}

	public void handleAsyncEvent() {
		ReleaseParameters rp = rth.getReleaseParameters();
		RelativeTime cost = rp.getCost();
		RelativeTime newCost = new RelativeTime(cost);
		newCost = cost.add(0, 200000, newCost);
		rp.setCost(newCost);
		rth.schedulePeriodic();

		System.out.println("Cost of " + cost.toString() + " Overrun!");

		int costOverruns = co.getnumberOfOverruns() + 1;
		double currentCost = newCost.getMilliseconds() + ((double) newCost.getNanoseconds()) / 1000000.0;
		double currentTime = Simulator.getRealTimeSeconds();
		co.storeOverrunInfo(costOverruns, currentTime, currentCost);
	}

}
