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

public interface DeadlineMisser {

	public int getMissedDeadlines();

	public void storeMissDeadlineInfo(int deadlineNumber, double time, double currentDeadLine);

	public ArrayList<Triplet> getDealineMissedList();
    
        public double getStartTime();

}
