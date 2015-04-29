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

public class DeadLockAvoider {

	/**
	 * Checks that the current thread that is trying to access GroundVehicle
	 * 'gv' method does not hold a different lock except the built-in lock of
	 * 'gv'
	 * 
	 * @param gv
	 *            groundVehicle that is calling this method
	 * @param s
	 *            simulator in which this GroundVehicle is added.
	 * @return true if it doesn't hold any lock (different from built-in lock of
	 *         'gv')
	 * @throws DeadLockAvoiderException
	 */
	public static boolean hasNoLock(GroundVehicle gv, Simulator s) throws DeadLockAvoiderException {
		ArrayList<GroundVehicle> gvList = s.gvlist;
		for (int i = 0; i < gvList.size(); i++) {
			if (Thread.holdsLock(gvList.get(i))) {
				if (!gvList.get(i).equals(gv)) {
					throw new DeadLockAvoiderException();
				}
			}
		}
		return true;
	}

	/**
	 * This method avoids hold and wait deadlock. Each vehicle has a distinct
	 * Id. The order to get the locks is from lowest to highest. If this ground
	 * vehicle has a lower id than the leader, it gets its own lock first and
	 * then the leaders lock. Else the order to get the locks will be reversed.
	 */
	public static void lockgvLocks(ArrayList<GroundVehicle> gvList) {

		ArrayList<GroundVehicle> orderedList = sortVehiclesById(gvList);
		for (int i = 0; i < orderedList.size(); i++) {
			orderedList.get(i).getVehicleLock().lock();
		}
	}

	/**
	 * @return an arrayList<GroundVehicle> with the same GroundVehicles of
	 *         gvList but ordered by the 'Id' variable of GroundVehicle. (From
	 *         lowest to highest).
	 */
	public static ArrayList<GroundVehicle> sortVehiclesById(ArrayList<GroundVehicle> gvList) {

		GroundVehicle key;
		int i, j;
		for (j = 0 + 1; j <= gvList.size() - 1; j++) {
			key = gvList.get(j);
			for (i = j - 1; i >= 0 && key.compareId(gvList.get(i)) < 0; i--) {
				gvList.set(i + 1, gvList.get(i));
			}
			gvList.set(i + 1, key);
		}
		return gvList;
	}

	/**
	 * Unlocks all the 'mygvLocks' lock of each GroundVehicle object in the
	 * gvList.
	 */
	public static void unlockgvLocks(ArrayList<GroundVehicle> gvList) {
		for (int i = 0; i < gvList.size(); i++) {
			gvList.get(i).getVehicleLock().unlock();
		}
	}

}
