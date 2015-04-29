/**
 *
 * @author balvisio
 * @author abachrac
 * 
 * Solutions created by Bruno Alvisio (balvisio@mit.edu), modified by Abe Bachrach(abachrac@mit.edu) 4/23/2010
 *
 */

import java.util.ArrayList;

public interface CostOverruner {

	public ArrayList<DoublePair> getCostList();

	public int getnumberOfOverruns();

	public void storeOverrunInfo(int overrunNumber, double time, double currentCost);

	public ArrayList<Triplet> getOverrunList();
}
