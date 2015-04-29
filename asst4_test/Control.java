/**
 *
 * @author balvisio
 * @author abachrac
 * @author sachih
 * 
 * Solutions created by Bruno Alvisio (balvisio@mit.edu), modified by Abe Bachrach(abachrac@mit.edu) 4/23/2010, Modified by Sachi Hemachandra 4/20/2011
 *
 */

import java.lang.IllegalArgumentException;

public class Control
{
  private double _s;
  private double _omega;
  
  public Control (double omega, double s){
    if (s < 5 || s > 10) //Check to make sure s is in range.
      throw new IllegalArgumentException("S out of range");
    if (omega < -Math.PI || omega >= Math.PI) //Check to make sure theta is in range.
      throw new IllegalArgumentException("Omega out of range");
    
    _s = s;
    _omega = omega;    
  }
  
  public double getTransVel() {
    return _s;
  }

  public double getRotVel() {
    return _omega;
  }  
}
