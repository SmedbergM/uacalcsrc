/* ProductAlgebra.java (c) 2001/07/22  Ralph Freese and Emil Kiss */

package org.uacalc.alg;

import java.util.*;
import java.math.BigInteger;
import org.uacalc.util.*;
import org.uacalc.terms.*;

import org.uacalc.alg.conlat.*;
import org.uacalc.alg.sublat.*;

/**
 * This class represents a subalgebra of a direct product 
 * of <tt>SmallAlgebra</tt>s. It allows one to construct such an
 * algebra even though the direct product may be too big to be a
 * <tt>SmallAlgebra</tt>.
 *
 * @author Ralph Freese
 *
 * @version $Id$
 */
public class SubProductAlgebra extends GeneralAlgebra implements SmallAlgebra {

  protected BigProductAlgebra productAlgebra;
  protected List gens; // a list of IntArray's
  protected List univ; // a list of IntArray's
  protected HashMap univHashMap; // a map from IntArray's of elements of the 
                                 // univ to Integers (the index).
  protected Term[] terms; // term[i] is a term for the ith element


  protected SubProductAlgebra() {
    super(null);
  }

  protected SubProductAlgebra(String name) {
    super(name);
  }

  /**
   * Construct the direct product of a List of SmallAlgebra's.
   * gens is a list of IntArray's.
   */
  public SubProductAlgebra(String name, BigProductAlgebra prod, List gens) {
    this(name, prod, gens, false);
  }

  /**
   * Construct the direct product of a List of SmallAlgebra's.
   * gens is a list of IntArray's.
   */
  public SubProductAlgebra(String name, BigProductAlgebra prod, 
                                        List gens, boolean findTerms) {
    super(name);
    productAlgebra = prod;
    // some gyrations to eliminate duplicates but keep the order the same.
    HashSet hs = new HashSet(gens.size());
    List gens2 = new ArrayList(gens.size());
    for (Iterator it = gens.iterator(); it.hasNext(); ) {
      Object elem = it.next();
      if (!hs.contains(elem)) {
        hs.add(elem);
        gens2.add(elem);
      }
    }
    gens = gens2;
    this.gens = gens2;
    if (findTerms) {
      HashMap termMap = new HashMap();
      int k = 0;
      for (Iterator it = gens.iterator(); it.hasNext(); k++) {
        Variable var = new VariableImp("x_" + k);
        termMap.put(k, var);
      }
      univ = productAlgebra.sgClose(gens, termMap);
      terms = new Term[univ.size()];
      for (int i = 0; i < univ.size(); i++) {
        terms[i] = (Term)termMap.get(i);
      }
    }
    else univ = productAlgebra.sgClose(gens);
    size = univ.size();
    univHashMap = new HashMap(size);
    int k = 0;
    for (Iterator it = univ.iterator(); it.hasNext(); k++) {
      univHashMap.put(it.next(), new Integer(k));
    }
    universe = new HashSet(univ);
    makeOperations();
  }

  /**
   * Construct a SubProductAlgebra when the gens and univ are already
   * given. Useful for reading back from a file without calculating 
   * the universe again.
  */
  public SubProductAlgebra(String name, BigProductAlgebra prod, 
                                        List gens, List univList) {
    super(name);
    setup(prod, gens, univList);
  }

  private void setup(BigProductAlgebra prod, List gens, List univList) {
    productAlgebra = prod;
    this.gens = gens;
    univ = univList;
    size = univ.size();
    univHashMap = new HashMap(size);
    int k = 0;
    for (Iterator it = univ.iterator(); it.hasNext(); k++) {
      univHashMap.put(it.next(), new Integer(k));
    }
    universe = new HashSet(univ);
    makeOperations();
  }

  protected void makeOperations() {
    final int k = productAlgebra.operations().size();
    operations = new ArrayList(k);
    for (int i = 0; i < k; i++) {
      final Operation opx = (Operation)productAlgebra.operations().get(i);
      final int arity = opx.arity();
      Operation op = new AbstractOperation(opx.symbol(), size) {
          // this is not tested yet
          public Object valueAt(List args) {
            return opx.valueAt(args);
          }
          Operation tableOp = null;
          public void makeTable() {
            int h = 1;
            for (int i = 0; i < arity; i++) {
              h = h * size;
            }
            int[] values = new int[h];
            for (int i = 0; i < h; i++) {
              values[i] = intValueAt(Horner.hornerInv(i, size, arity));
            }
            tableOp = Operations.makeIntOperation(symbol(), size, values);
          }
          public int intValueAt(final int[] args) {
            if (tableOp != null) return tableOp.intValueAt(args);
            final List lst = new ArrayList(arity);
            for (int i = 0; i < arity; i++) {
              lst.add(getElement(args[i]));
            }
            return elementIndex(opx.valueAt(lst));
          }
      };
      operations.add(op);
    }
  }

  public void makeOperationTables() {
    for (Iterator it = operations().iterator(); it.hasNext(); ) {
      ((Operation)it.next()).makeTable();
    }
  }

  public Term[] getTerms() {
    return terms;
  }

  public BigProductAlgebra getProductAlgebra() {
    return productAlgebra;
  }

  public BigProductAlgebra superAlgebra() {
    return productAlgebra;
  }

  public List generators() {
    return gens;
  }

  public List universeList() {
    return univ;
  }

  public CongruenceLattice con() {
    if (con == null) con = new CongruenceLattice(this);
    return con;
  }

  public SubalgebraLattice sub() {
    if (sub == null) sub = new SubalgebraLattice(this);
    return sub;
  }

  public int elementIndex(Object obj) {
    IntArray elem = (IntArray)obj;
    return ((Integer)univHashMap.get(elem)).intValue();
  }

  public Object getElement(int index) {
    return univ.get(index);
  }

  public static void main(String[] args) throws java.io.IOException,
                                   org.uacalc.io.BadAlgebraFileException {
    if (args.length == 0) return;
    System.out.println("reading " + args[0]);
    Algebra alg = org.uacalc.io.AlgebraIO.readAlgebraFile(args[0]);
    System.out.println("The alg \n" + alg);
    ArrayList lst = new ArrayList();
    lst.add(alg);
    lst.add(alg);
//    lst.add(alg);
//    lst.add(alg);
    System.out.println("prod of " + lst.size() + " algebras");
    SmallAlgebra alg2 = new ProductAlgebra(lst);

    org.uacalc.io.AlgebraWriter writer 
         = new org.uacalc.io.AlgebraWriter((SmallAlgebra)alg2, "/tmp/goo.xml");
    writer.writeAlgebraXML();
    

    SmallAlgebra alg3 = new ProductAlgebra(lst);
    alg2.makeOperationTables();
    TypeFinder tf = new TypeFinder(alg2);
    long t = System.currentTimeMillis();
    int k = alg2.con().joinIrreducibles().size();
    t = System.currentTimeMillis() - t;
    System.out.println("number of jis is " + k);
    System.out.println("to find the jis it took " + t);
    t = System.currentTimeMillis();
    //System.out.println("size " + alg.con().universe().size());
    HashSet types = tf.findTypeSet();
    t = System.currentTimeMillis() - t;
    System.out.println("type set = " + types);
    System.out.println("it took " + t);
    System.out.println("--- Without tables ---");
    tf = new TypeFinder(alg3);
    t = System.currentTimeMillis();
    k = alg3.con().joinIrreducibles().size();
    t = System.currentTimeMillis() - t;
    System.out.println("number of jis is " + k);
    System.out.println("to find the jis it took " + t);
    t = System.currentTimeMillis();
    //System.out.println("size " + alg.con().universe().size());
    types = tf.findTypeSet();
    t = System.currentTimeMillis() - t;
    System.out.println("type set = " + types);
    System.out.println("it took " + t);


  }

}


