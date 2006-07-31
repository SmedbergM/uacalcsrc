/* ProductAlgebra.java (c) 2001/07/22  Ralph Freese and Emil Kiss */

package org.uacalc.alg;

import java.util.*;
import java.util.logging.*;
import java.math.BigInteger;
import org.uacalc.util.*;

import org.uacalc.alg.conlat.*;
import org.uacalc.alg.sublat.*;

/**
 * This class represents the direct product of <tt>SmallAlgebra</tt>s.
 *
 * @author Ralph Freese
 *
 * @version $Id$
 */
public class ProductAlgebra extends GeneralAlgebra implements SmallAlgebra {

  static Logger logger = Logger.getLogger("org.uacalc.alg.ProductAlgebra");
  static {
    logger.setLevel(Level.FINER);
  }

  protected List algebras;
  protected int[] sizes;
  protected int numberOfProducts;

  protected ProductAlgebra() {
    super(null);
  }

  protected ProductAlgebra(String name) {
    super(name);
  }

  public ProductAlgebra(List algs) {
    this("", algs);
  }

  /**
   * Construct the direct product of a List of SmallAlgebra's.
   */
  public ProductAlgebra(String name, List algs) {
    super(name);
    algebras = algs;
    numberOfProducts = algs.size();
    sizes = new int[numberOfProducts];
    int k = 0;
    for (Iterator it = algs.iterator(); it.hasNext(); k++) {
      sizes[k] = ((SmallAlgebra)it.next()).cardinality();
    }
    int n = 1;
    for (int i = 0; i < numberOfProducts; i++) {
      n *= sizes[i];
    }
    size = n;
    universe = makeCartesianProduct(algs);
    makeOperations();
  }

  protected void makeOperations() {
    final int k = ((Algebra)algebras.get(0)).operations().size();
    operations = new ArrayList(k);
    for (int i = 0; i < k; i++) {
      final int arity = 
           ((Operation)((Algebra)algebras.get(0)).operations().get(i)).arity();
      final List opList = new ArrayList(numberOfProducts);
      for (int j = 0; j < numberOfProducts; j++) {
        opList.add(((Algebra)algebras.get(j)).operations().get(i));
      }
      final int[][] argsExpanded = new int[arity][numberOfProducts];
      final int[] arg = new int[arity];
      Operation op = new AbstractOperation(
                              ((Operation)opList.get(0)).symbol(), size) {
          // this is not tested yet
          public Object valueAt(List args) {
            List ans = new ArrayList();
            for (int i = 0; i < opList.size(); i++) {
              Operation opx = (Operation)opList.get(i);
              List arg = new ArrayList();
              for (Iterator it = args.iterator(); it.hasNext(); ) {
                arg.add(((List)it.next()).get(i));
              }
              ans.add(opx.valueAt(arg));
            }
            return ans;
          }
          //  **********  Note  ***********
          //  This could be sped up not creating ansArr and instead do
          //  horner inline.
          //  *****************************
          //public int intValueAt(int[] args) {
          //  int[] ansArr = new int[numberOfProducts];
          //  for (int i = 0; i < opList.size(); i++) {
          //    final Operation opx = (Operation)opList.get(i);
          //    final int[] arg = new int[opx.arity()];
          //    for (int j = 0; j < arg.length; j++) {
          //      arg[j] = args[i];
          //    }
          //    ansArr[i] = opx.intValueAt(arg);
          //  }
          //  return Horner.horner(ansArr, sizes);
          //}
          //
          //  ********************************
          //  **** The inline horner version ***
          //  ********************************
          //final int[][] argsExpandedx = new int[arity()][];
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
          public int intValueAt(int[] args) {
            if (tableOp != null) return tableOp.intValueAt(args);
            int ans = 0;
            //final int[][] argsExpanded = new int[args.length][];
            for (int i = 0; i < args.length; i++) {
              //argsExpanded[i] = Horner.hornerInv(args[i], sizes);
              Horner.hornerInv(args[i], sizes, argsExpanded[i]);
            }
            int s = 0;
            for (int i = numberOfProducts - 1; i >= 0; i--) {
              final Operation opx = (Operation)opList.get(i);
              //final int[] arg = new int[opx.arity()];
              for (int j = 0; j < arg.length; j++) {
                arg[j] = argsExpanded[j][i];
              }
              ans = s * ans + opx.intValueAt(arg);
              s = sizes[i];
            }
            return ans;
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

  public List factors() {
    return algebras;
  }

  public SmallAlgebra projection(int k) {
    return (SmallAlgebra)algebras.get(k);
  }

  public BasicPartition projectionKernel(int k) {
    final int projectionSize = sizes[k];
    final int blockSize = cardinality() / projectionSize;
    final int[] arr = new int[cardinality()];
    final int[] tmp = new int[cardinality()];
    for (int i = 0; i < projectionSize; i++) {
    }
    // finish this later.
    return null;
  }

  public CongruenceLattice con() {
    if (con == null) con = new CongruenceLattice(this);
    return con;
  }

  public SubalgebraLattice sub() {
    if (sub == null) sub = new SubalgebraLattice(this);
    return sub;
  }

// fix these!!!!!!!!!!!!!!
  public int elementIndex(Object obj) {
    throw new UnsupportedOperationException();
  }

  public Object getElement(int index) {
    return Horner.hornerInv(index, sizes);
  }

  // do something ??
  public List getUniverseList() { return null; }
  public Map getUniverseOrder() { return null; }

  protected Set makeCartesianProduct(final List algs) {
    return new AbstractSet() {
        public int size() { return size; }
        public boolean contains(Object obj) {
          if (!(obj instanceof List)) return false;
          Iterator itAlgs = algs.iterator();
          for (Iterator it = ((List)obj).iterator(); it.hasNext() ; ) {
            if (!itAlgs.hasNext()) return false;
            if (!((Algebra)itAlgs.next()).universe().contains(it.next())) {
              return false;
            }
          }
          return true;
        }
        // should check if each has an iterator and make one
        public Iterator iterator() {
          throw new UnsupportedOperationException();
        }
      };
  }

  /**
   * The subalgebra generated by elems given as arrays of int's.
   */
  public Subalgebra sg(List elems) {
    Subalgebra ans = new Subalgebra(this, new int[0]);
    return ans;
  }

  public List sgClose(List elems) {
    List ans = new ArrayList();
    return ans;
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


