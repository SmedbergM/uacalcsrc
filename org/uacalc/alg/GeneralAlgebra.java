/* GeneralAlgebra.java (c) 2001/07/22  Ralph Freese and Emil Kiss */

package org.uacalc.alg;

import java.util.*;

import org.uacalc.alg.conlat.*;
import org.uacalc.alg.op.Operation;
import org.uacalc.alg.op.OperationSymbol;
import org.uacalc.alg.op.Operations;
import org.uacalc.alg.op.SimilarityType;
import org.uacalc.alg.sublat.*;
import org.uacalc.util.Monitor;

/**
 * This class represents general algebras that may or may not be
 * "computer finite".
 *
 * @author Ralph Freese
 *
 * @version $Id$
 */
public class GeneralAlgebra implements Algebra {

  protected List<Operation> operations;
  protected Map operationsMap;
  protected SimilarityType similarityType;
  protected Set universe;
  //private final int[] similarityType;
  protected CongruenceLattice con;
  protected SubalgebraLattice sub;
  protected final String name;
  protected String description;
  protected int size;
  
  protected static Monitor monitor;

  protected GeneralAlgebra(String name) {
    this.name = name;
  }

  public GeneralAlgebra(String name, Set univ) {
    this.name = name;
    this.universe = univ;
    this.size = univ.size();
  }

  public GeneralAlgebra(String name, Set univ, List<Operation> operations) {
    this(name, univ);
    this.operations = operations;
    this.operationsMap = Operations.makeMap(operations);
    //similarityType = new int[operations.size()];
    //int k;
    //Iterator it;
    //for (it = operations.iterator(), k = 0; it.hasNext(); k++) {
      //similarityType[k] = ((Operation)it.next()).arity();
    //}
  }

  public static final void setMonitor(Monitor m) { monitor = m; }
  public static final Monitor getMonitor() { return monitor; }
  
  public static final boolean monitoring() {
    return monitor != null;
  }
  
  
  protected void setUniverse(Set univ) {
    this.universe = univ;
    this.size = univ.size();
  }

  protected void setOperations(List<Operation> ops) {
    this.operations = ops;
  }

  public Map getOperationsMap() {
    if (operationsMap == null) operationsMap = Operations.makeMap(operations);
    return operationsMap;
  }

  public List<Operation> operations() {
    return operations;
  }

  public Operation getOperation(OperationSymbol sym) {
    return (Operation)getOperationsMap().get(sym);
  }

  public boolean isUnary() {
    for (Iterator it = operations().iterator(); it.hasNext(); ) {
      final Operation op = (Operation)it.next();
      if (op.arity() > 1) return false;
    }
    return true;
  }

  /**
   * This gives a list of the operations of arity 0, which is
   * a little different from the constants.
   */
  public List constantOperations() {
    List ans = new ArrayList();
    for (Iterator it = operations().iterator(); it.hasNext(); ) {
      final Operation op = (Operation)it.next();
      if (op.arity() == 0) ans.add(op);
    }
    return ans;
  }

  public SimilarityType similarityType() {
    if (similarityType == null) {
      List lst = new ArrayList(operations.size());
      for (Iterator it = operations().iterator(); it.hasNext(); ) {
        lst.add(((Operation)it.next()).symbol());
      }
      similarityType = new SimilarityType(lst);
    }
    return similarityType;
  }

  public String name() {
    return name;
  }

  public String description() {
    return description;
  }

  public void setDescription(String desc) {
    this.description = desc;
  }

  public boolean isSimilarTo(Algebra alg2) {
    return alg2.similarityType().equals(similarityType());
  }

  public Iterator iterator() {
    return universe().iterator();
  }

  public int cardinality() {
    if (universe != null) return universe.size();
    return -1;
  }
  
  public Set universe() {
    return universe;
  }

  public CongruenceLattice con() {
    throw new UnsupportedOperationException();
  }

  public SubalgebraLattice sub() {
    throw new UnsupportedOperationException();
  }

  public void makeOperationTables() {}

  public SmallAlgebra parent() { return null; }
  
  public List<SmallAlgebra> parents() {
    if (parent() == null) return null;
    List<SmallAlgebra> lst = new ArrayList<SmallAlgebra>();
    lst.add(parent());
    return lst;
  }

  public boolean isIdempotent() {
    for (Iterator it = operations().iterator(); it.hasNext(); ) {
      final Operation op = (Operation)it.next();
      if (!(op.isIdempotent())) return false; 
    }
    return true;
  } 

/*
  public static void main(String[] args) throws java.io.IOException,
                                     org.uacalc.io.BadAlgebraFileException {
*/
/*
    Operation op = Operations.makeBinaryIntOperation("*", 3,
                     new int[][] {{0, 1, 2}, {1, 2, 2}, {0,2,2}});
    //Operation op = new AbstractOperation("*", 2, 3
    List ops = new ArrayList();
    ops.add(op);
    Algebra alg = new BasicAlgebra("test1", 3, ops);
    System.out.println("0*2 = " + op.intValueAt(new int[] {0, 2}));
    System.out.println("2*0 = " + op.intValueAt(new int[] {2, 0}));
*/
/*
    Operation mulModN = new AbstractOperation("*", 2, 100) {
        public Object valueAt(List args) {
          BigInteger arg0 = (BigInteger)args.get(0);
          BigInteger arg1 = (BigInteger)args.get(1);
          return arg0.multiply(arg1);
        }
      } ;
    Operation addModN = new AbstractOperation("+", 2, 100) {
        public Object valueAt(List args) {
          BigInteger arg0 = (BigInteger)args.get(0);
          BigInteger arg1 = (BigInteger)args.get(1);
          return arg0.add(arg1);
        }
      } ;
    List modOps = new ArrayList();
    modOps.add(addModN);
    modOps.add(mulModN);
    Set univ = new AbstractSet() {
       public int size() { return 100; }
       public boolean contains(Object obj) {
         BigInteger b100 = BigInteger.valueOf(100);
         if (obj instanceof BigInteger) {
           BigInteger e = (BigInteger)obj;
           if (e.compareTo(b100) < 0 
               && e.compareTo(BigInteger.ZERO) >= 0) return true;
           return false;
         }
         return false;
       }
       public Iterator iterator() { 
         throw new UnsupportedOperationException();
       }
     };
    Algebra Zmod100 = new GeneralAlgebra("Zmod100", univ, modOps);
    System.out.println("Zmod100 has size " + Zmod100.cardinality());

  }
*/

}


