/* Algebra.java (c) 2001/07/22  Ralph Freese and Emil Kiss */

package org.uacalc.lat;

import java.util.List;
import java.util.Iterator;
import java.util.Set;

import org.uacalc.alg.*;

public interface Order<E> {

  /**
   * The order relation.
   */
  public boolean leq(E a, E b);


}




