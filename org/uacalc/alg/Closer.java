package org.uacalc.alg;

import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;
import java.math.BigInteger;

import org.uacalc.ui.tm.ProgressReport;
import org.uacalc.util.*;
import org.uacalc.terms.*;
import org.uacalc.eq.*;

import org.uacalc.alg.conlat.*;
import org.uacalc.alg.op.AbstractOperation;
import org.uacalc.alg.op.Operation;
import org.uacalc.alg.op.OperationWithDefaultValue;
import org.uacalc.alg.op.OperationSymbol;
import org.uacalc.alg.op.Operations;

/**
 * A class for finding the closure with configurations for several options
 * and fields to hold side results.
 * It only deals with BigProduct's now. We might call it BigProductCloser
 * and have Closer as an interface.
 * 
 * 
 * @author ralph
 *
 */
public class Closer {
  
  BigProductAlgebra algebra;
  List<IntArray> ans;
  boolean completed = false; // sometimes we end early
  List<IntArray> generators;
  Map<IntArray,Term> termMap; // initially a map from generators to variables.
  // this is double used: it is set in the closing if we are looking
  // for a pattern but set before is it an element we are looking for.
  // probably should have separate fields for these two things.
  IntArray eltToFind;
  List<IntArray> eltsToFind;  // find all
  Map<IntArray,Integer> indecesMapOfFoundElts;
  boolean allEltsFound = false;
  int specialEltsFound = 0;
  final static Integer minusOne = new Integer(-1);
  SmallAlgebra imageAlgebra;
  Map<IntArray,Integer> homomorphism; // actually a partial homo into imageAlg.
  Equation failingEquation = null; // a list of two terms
  SmallAlgebra rootAlgebra; // the root of a power algebra
  // an operation on the set of the root algebra; to test if it is in the clone. 
  Operation operation;
  Term termForOperation;
  // a blocks and values constraint as in IntArray.
  int[][] blocks;
  int[][] values;
  
  ProgressReport report;
  
  static final int nCPUs = Runtime.getRuntime().availableProcessors(); 
  
  public Closer(BigProductAlgebra alg, List<IntArray> gens) {
    this.algebra = alg;
    this.generators = gens;
  }
  
  public Closer(BigProductAlgebra alg, List<IntArray> gens, Map<IntArray,Term> termMap) {
    this(alg, gens);
    this.termMap = termMap;
  }
  
  public Closer(BigProductAlgebra alg, List<IntArray> gens, boolean makeTermMap) {
    this(alg, gens);
    if (makeTermMap) setupTermMap();
  }
  
  private void setupTermMap() {
    termMap = new HashMap<IntArray,Term>();
    if (generators.size() == 1) termMap.put(generators.get(0), Variable.x);
    if (generators.size() == 2) {
      termMap.put(generators.get(0), Variable.x);
      termMap.put(generators.get(1), Variable.y);
    }
    if (generators.size() == 3) {
      termMap.put(generators.get(0), Variable.x);
      termMap.put(generators.get(1), Variable.y);
      termMap.put(generators.get(2), Variable.z);
    }
    int k = 0;
    if (generators.size() > 3) {
      for (Iterator<IntArray> it = generators.iterator(); it.hasNext(); k++) {
        Variable var = new VariableImp("x_" + k);
        termMap.put(it.next(), var);
      }
    }
  }
  
  public List<IntArray> getAnswer() { return ans; }
  
  public Equation getFailingEquation() { return failingEquation; }
  
  // stuff for finding a term of a given operation
  public Term getTermForOperation() { return termForOperation; }
  
  public void setRootAlgebra(SmallAlgebra alg) { rootAlgebra = alg; }
  
  public void setOperation(Operation op) { operation = op; }
  
  
  public List<IntArray> getGenerators() { return generators; }
  
  public void setGenerators(List<IntArray> generators) { 
    this.generators = generators;
  }
  
  public Map<IntArray,Term> getTermMap() { return termMap; }
  
  public void setTermMap(Map<IntArray,Term> termMap) {
    this.termMap = termMap;
  }
  
  public SmallAlgebra getImageAlgebra() { return imageAlgebra; }
  
  public void setImageAlgebra(SmallAlgebra alg) {
    if (!alg.similarityType().equals(algebra.similarityType())) {
      throw new IllegalArgumentException("the algebras must be similar");
    }
    imageAlgebra = alg;
  }
  
  // TODO add convenience methods to build these maps.
  public Map<IntArray,Integer> getHomomorphism() { return homomorphism; }
  
  public void setHomomorphism(Map<IntArray,Integer> homomorphism) {
    this.homomorphism = homomorphism;
  }
  
  public void setHomomorphism(int[] algGens) {
    if (algGens.length != generators.size()) {
      throw new IllegalArgumentException("wrong number of generators");
    }
    Map<IntArray,Integer> homo = new HashMap<IntArray,Integer>(generators.size());
    int k = 0;
    for (IntArray g : generators) {
      homo.put(g, algGens[k++]);
    }
    this.homomorphism = homo;
  }
  
  public IntArray getElementToFind() { return eltToFind; }
  
  public void setElementToFind(IntArray e) { eltToFind = e; }
  
  public List<IntArray> getElementsToFind() { return eltsToFind; }
  
  public void setElementsToFind(List<IntArray> e, List<IntArray> gens) {
    eltsToFind = e;
    indecesMapOfFoundElts = new HashMap<IntArray,Integer>(e.size());
    for (IntArray ia : e) {
      indecesMapOfFoundElts.put(ia, minusOne);
    }
    for (int i = 0; i < gens.size(); i++) {
      if (minusOne.equals(indecesMapOfFoundElts.get(gens.get(i)))) {
        indecesMapOfFoundElts.put(gens.get(i), i);
        specialEltsFound++;
      }
    }
  }
  
  public boolean allElementsFound() { return allEltsFound; }
  
  public int[][] getBlocks() { return blocks; }
  
  public void setBlocks(int[][] blocks) { this.blocks = blocks; }
  
  public int[][] getValues() { return values; }
  
  public void setValues(int[][] values) { this.values = values; }
  
  //protected static ProgressReport monitor;
  
  /*
  public boolean monitoring() {
    return monitor != null;
  }
  
  public static final void setMonitor(ProgressReport m) { monitor = m; }
  public static final ProgressReport getMonitor() { return monitor; }
  */
  
  public void setProgressReport(ProgressReport report) {
    this.report = report;
  }
  
  public List<IntArray> close() {
    // TODO fix this
    if (!algebra.isPower()) {
      throw new IllegalArgumentException("only implemented for powers");
    }
    ans = new ArrayList<IntArray>(generators);
    return ans;
  }
  
  public List<IntArray> sgClose() {
    System.out.println("nCPU = " + nCPUs);
    //System.out.println("termMap = " + termMap);
    //return sgCloseParallel(generators, 0, termMap);
    return sgClose(generators, 0, termMap);
  }
  
  /**
   * Closure of <tt>elems</tt> under the operations. (Worry about
   * nullary ops later.)
   *
   * @param elems a List of IntArray's
   *
   * @param termMap a Map from the element to the corresponding term
   *                used to generated it. The generators should be 
   *                already in the Map. In other words the termMap
   *                should have the same number of entries as elems.
   *
   * @return a List of IntArray's.
   */
  public List<IntArray> sgClose(List<IntArray> elems, int closedMark, 
                                     final Map<IntArray,Term> termMap) {
    if (algebra.isPower()) {
      SmallAlgebra alg = algebra.rootFactors().get(0);
      alg.makeOperationTables();
      return sgClosePower(elems, closedMark, termMap);
      //List<Operation> ops = alg.operations();
      //if (ops.size() > 0 && ops.get(0).getTable() != null) {
        //return sgClosePower(alg.cardinality(), ops, elems,
        //                                       closedMark, termMap, elt);
        // TODO restore above
      //  return null;
      //}
    }
    
    if (report != null) report.addStartLine("subpower closing ...");

    final int numOfOps = algebra.operations().size();  
    List<Operation> imgOps = null;
    if (homomorphism != null) {
      imgOps = new ArrayList<Operation>(numOfOps);
      for (Operation op : algebra.operations()) {
        imgOps.add(imageAlgebra.getOperation(op.symbol()));
      }
    }
    
    // these final boolean are meant to help the jit compiler.
    final boolean reportNotNull = report == null ? false : true;
    final boolean imgAlgNull = imgOps == null ? true : false;
    final boolean eltToFindNotNull = eltToFind == null ? false : true;
    final boolean eltsToFindNotNull = eltsToFind == null ? false : true;
    final boolean operationNotNull = operation == null ? false : true;

    ans = new ArrayList<IntArray>(elems);// IntArrays
    final List<int[]> rawList = new ArrayList<int[]>(); // the corr raw int[]
    for (Iterator<IntArray> it = elems.iterator(); it.hasNext(); ) {
      rawList.add(it.next().getArray());
    }
    final HashSet<IntArray> su = new HashSet<IntArray>(ans);
    int currentMark = ans.size();
    int pass = 0;
    CloserTiming timing = null; 
    if (reportNotNull) timing = new CloserTiming(algebra, report);
    //int currPassSize = 0; // for time left
    //int lastPassSize = 0; // for time left
    //final int numberProjs = algebra.getNumberOfFactors(); // for time left
    while (closedMark < currentMark) {
      if (reportNotNull) timing.updatePass(ans.size());
      //lastPassSize = currPassSize;
      //currPassSize = ans.size();
      //long funcAppsNeeded = countFuncApplications(lastPassSize, currPassSize);
      //long appsSoFar = 0;
      String str = "pass: " + pass + ", size: " + ans.size();
      if (reportNotNull) {
        report.setPass(pass);
        report.setPassSize(ans.size());
        report.addLine(str);
      }
      else {
        System.out.println(str);
      }
      pass++;
      if (Thread.currentThread().isInterrupted()) {
        if (reportNotNull) report.addEndingLine("cancelled ...");
        return null;
      }
//if (lst.size() > 100000) return lst;
      // close the elements in current
      for (int i = 0; i < numOfOps; i++) {
      //for (Iterator<Operation> it = algebra.operations().iterator(); it.hasNext(); ) {
        //Operation f = it.next();
        Operation f = algebra.operations().get(i);
        final int arity = f.arity();
        if (arity == 0) continue;  // worry about constansts later
        int[] argIndeces = new int[arity];
        for (int j = 0; j < arity - 1; j++) {
          argIndeces[j] = 0;
        }
        argIndeces[arity - 1] = closedMark;
        ArrayIncrementor inc =
                    SequenceGenerator.sequenceIncrementor(
                                  argIndeces, currentMark - 1, closedMark);

        final int[][] arg = new int[arity][];
        while (true) {
          if (Thread.currentThread().isInterrupted()) {
            if (reportNotNull) {
              report.addEndingLine("cancelled ...");
              report.setSize(ans.size());
            }
            return null;
          }
          for (int j = 0; j < arity; j++) {
            arg[j] = rawList.get(argIndeces[j]);
          }
          
          int[] vRaw = f.valueAt(arg);
          //int[] test = f.valueAt(arg);
          IntArray v = new IntArray(vRaw);
          //IntArray testV = new IntArray(test);
           
          //System.out.println("vRaw == test is " + v.equals(new IntArray(test)));
          if (reportNotNull) timing.incrementApps();
          //appsSoFar = appsSoFar + numberProjs;
          if (su.add(v)) {
            ans.add(v);
            rawList.add(vRaw);
            if (reportNotNull) timing.incrementNextPassSize();
            if (reportNotNull) report.setSize(ans.size());
            if (Thread.currentThread().isInterrupted()) return null;
            if (termMap != null) {
              List<Term> children = new ArrayList<Term>(arity);
              for (int j = 0; j < arity; j++) {
                //children.set(i, termMap.get(arg.get(i)));
                children.add(termMap.get(ans.get(argIndeces[j])));
              }
              termMap.put(v, new NonVariableTerm(f.symbol(), children));
              //logger.fine("" + v + " from " + f.symbol() + " on " + arg);
              if (operationNotNull) {
                Term term = termMap.get(v);
                // why are recreating vars each time ???
                List<Variable> vars = new ArrayList<Variable>(generators.size());
                for (IntArray ia : generators) {
                  vars.add((Variable)termMap.get(ia));
                }
                Operation termOp = term.interpretation(rootAlgebra, vars, true);
                if (Operations.equalValues(termOp, operation)) {
                  termForOperation = term;
                  return ans;
                }
              }
            }
            if (eltToFindNotNull && v.equals(eltToFind)) {
              if (reportNotNull) report.addEndingLine("closing done, found "
                                               + eltToFind + ", at " + ans.size());
              return ans;
            }
            // the get is  likely to be null so do it in this order.
            if (eltsToFindNotNull  && minusOne.equals(indecesMapOfFoundElts.get(v))) {
              final int index = ans.size() - 1;
              indecesMapOfFoundElts.put(v, index);
              specialEltsFound++;
              if (reportNotNull) report.addLine("found " + v + ", at " + index);
              if (specialEltsFound == eltsToFind.size()) {
                if (reportNotNull) report.addEndingLine("closing done, found all "
                    + eltsToFind.size() + " elems ");
                allEltsFound = true;
                return ans;
              }
            }
            
            // cannot do this exit if we are searching  for an equation !!!!!
            if (imgAlgNull) {
              if (algebra.cardinality() > 0 && ans.size() == algebra.cardinality()) {
                if (reportNotNull) {
                  report.addEndingLine("found all " + ans.size() + " elements");
                  report.setSize(ans.size());
                }
                return ans;
              }
            }
            else {
              final int[] args = new int[arity];
              for (int t = 0; t < arity; t++) {
                args[t] = homomorphism.get(ans.get(argIndeces[t]));
              }
              homomorphism.put(v, imgOps.get(i).intValueAt(args));
            }
            
            
          }
          else {
            if (imgOps != null) {
              final int[] args = new int[arity];
              for (int t = 0; t < arity; t++) {
                args[t] = homomorphism.get(ans.get(argIndeces[t]));
              }
              if (homomorphism.get(v).intValue() != imgOps.get(i).intValueAt(args)) {
                List<Term> children = new ArrayList<Term>(arity);
                for (int r = 0; r < arity; r++) {
                  //children.set(i, termMap.get(arg.get(i)));
                  children.add(termMap.get(ans.get(argIndeces[r])));
                }
                failingEquation = new Equation(termMap.get(v),
                    new NonVariableTerm(imgOps.get(i).symbol(), children));
                final String line = "failing equation:\n" + failingEquation;
                if (reportNotNull) {
                  report.setSize(ans.size());
                  report.addEndingLine(line);
                }
                else {
                  System.out.println("failing equation:\n" + failingEquation);
                  System.out.println("size so far: " + ans.size());
                }
                return ans;
              }
            }
          }
          if (!inc.increment()) break;
        }
if (false) {
/*
  List middleZero = new ArrayList();
    for (Iterator it2 = lst.iterator(); it2.hasNext(); ) {
      IntArray ia = (IntArray)it2.next();
      if (ia.get(1) == 0) middleZero.add(ia);
    }
  System.out.println("jonsson level so far: "
     + Algebras.jonssonLevelAux(middleZero, 
                             (IntArray)lst.get(0),  (IntArray)lst.get(2)));
*/
}
      }
      closedMark = currentMark;
      currentMark = ans.size();
      if (imgAlgNull && algebra.cardinality() > 0 && currentMark >= algebra.cardinality()) break;
    }
    if (reportNotNull) report.addEndingLine("closing done, size = " + ans.size());
    completed = true;
    return ans;
  }
  
  /**
   * Closure of <tt>elems</tt> under the operations. (Worry about
   * nullary ops later.)
   *
   * @param elems a List of IntArray's
   *
   * @param termMap a Map from the element to the corresponding term
   *                used to generated it. The generators should be 
   *                already in the Map. In other words the termMap
   *                should have the same number of entries as elems.
   *
   * @return a List of IntArray's.
   */
  public List<IntArray> sgCloseParallel(List<IntArray> elems, int closedMark, 
                                        Map<IntArray,Term> termMap) {
    
    if (algebra.isPower()) {
      SmallAlgebra alg = algebra.rootFactors().get(0);
      alg.makeOperationTables();
      return sgClosePower(elems, closedMark, termMap);
    }
    
    if (report != null) report.addStartLine("parallel subpower closing ...");

    final int numOfOps = algebra.operations().size();  
    List<Operation> imgOps = null;
    if (homomorphism != null) {
      imgOps = new ArrayList<Operation>(numOfOps);
      for (Operation op : algebra.operations()) {
        imgOps.add(imageAlgebra.getOperation(op.symbol()));
      }
    }
    
    // these final boolean are meant to help the jit compiler.
    final boolean reportNotNull = report == null ? false : true;
    final boolean imgAlgNull = imgOps == null ? true : false;
    final boolean eltToFindNotNull = eltToFind == null ? false : true;
    final boolean eltsToFindNotNull = eltsToFind == null ? false : true;
    final boolean operationNotNull = operation == null ? false : true;

    elems = Collections.synchronizedList(elems);
    ans = Collections.synchronizedList(new ArrayList<IntArray>(elems));// IntArrays
    final List<int[]> rawList = Collections.synchronizedList(new ArrayList<int[]>()); // the corr raw int[]
    synchronized(elems) {
      for (Iterator<IntArray> it = elems.iterator(); it.hasNext(); ) {
        rawList.add(it.next().getArray());
      }
    }
    termMap = new ConcurrentHashMap<IntArray,Term>(termMap);
    //final HashSet<IntArray> su = new HashSet<IntArray>(ans);
    int currentMark = ans.size();
    int pass = 0;
    CloserTiming timing = null; 
    if (reportNotNull) timing = new CloserTiming(algebra, report);
    //int currPassSize = 0; // for time left
    //int lastPassSize = 0; // for time left
    //final int numberProjs = algebra.getNumberOfFactors(); // for time left
    while (closedMark < currentMark) {
      if (reportNotNull) timing.updatePass(ans.size());
      //lastPassSize = currPassSize;
      //currPassSize = ans.size();
      //long funcAppsNeeded = countFuncApplications(lastPassSize, currPassSize);
      //long appsSoFar = 0;
      String str = "pass: " + pass + ", size: " + ans.size();
      if (reportNotNull) {
        report.setPass(pass);
        report.setPassSize(ans.size());
        report.addLine(str);
      }
      else {
        System.out.println(str);
      }
      pass++;
      if (Thread.currentThread().isInterrupted()) {
        if (reportNotNull) report.addEndingLine("cancelled ...");
        return null;
      }
      CountDownLatch latch = new CountDownLatch(nCPUs);
      ConcurrentHashMap<IntArray,Term> map = new ConcurrentHashMap<IntArray,Term>();
      for (int i = 0; i < nCPUs; ++i) {// create and start threads
        new Thread(new ParallelWorker(latch, i, map, numOfOps, closedMark, currentMark, reportNotNull, rawList, timing)).start();
      }
      try {
        latch.await();
      }
      catch (InterruptedException e) { return null; }  // also write to the report.
      
      
      System.out.println("map: " + map);
      for (IntArray ia : map.keySet()) {
        System.out.println("ia: " + ia + ", term: " + map.get(ia));
        ans.add(ia);
        rawList.add(ia.getArray());
        termMap.put(ia, map.get(ia));
      }
      
      
      
      closedMark = currentMark;
      currentMark = ans.size();
      if (imgAlgNull && algebra.cardinality() > 0 && currentMark >= algebra.cardinality()) break;
    }
    if (reportNotNull) report.addEndingLine("closing done, size = " + ans.size());
    completed = true;
    return ans;
  }
  
  public List<IntArray> sgClosePower() {
    //System.out.println("gens = " + generators);
    //System.out.println("termMap = " + termMap);
    return sgClosePower(generators, 0, termMap);
  }
  
  /**
   * A fast version for powers to compute the 
   * closure of <tt>elems</tt> under the operations. (Worry about
   * nullary ops later.)
   *
   * @param elems a List of IntArray's
   *
   * @param termMap a Map from the element to the corresponding term
   *                used to generated it. The generators should be 
   *                already in the Map. In other words the termMap
   *                should have the same number of entries as elems.
   *
   * @return a List of IntArray's.
   */
  private final List<IntArray> sgClosePower(
                     List<IntArray> elems, int closedMark, 
                     final Map<IntArray,Term> termMap) {
    
    if (report != null) report.addStartLine("subpower closing ...");
    final int algSize = algebra.factors().get(0).cardinality();
    final List<Operation> ops = algebra.factors().get(0).operations();
    final int k = ops.size();
    final int[][] opTables = new int[k][];
    final int[] arities = new int[k];
    final OperationSymbol[] symbols = new OperationSymbol[k];
    final Operation[] imgOps = 
      (homomorphism != null && imageAlgebra != null  && termMap != null) 
          ? new Operation[k] : null;
    boolean nullTable = false;
    for (int i = 0; i < k; i++) {
      Operation op = ops.get(i);
      if (op instanceof OperationWithDefaultValue) {
        opTables[i] = ((OperationWithDefaultValue)op).getTotalTable();
      }
      else {
        opTables[i] = op.getTable();
        if (opTables[i] == null) nullTable = true;
      }
      System.out.println("nullTable = " + nullTable);
      arities[i] = op.arity();
      symbols[i] = op.symbol();
      if (imgOps != null) imgOps[i] = imageAlgebra.getOperation(op.symbol());
    }
    //if (k > 0) System.out.println("opTable[0] = " + opTables[0]);
 //   List<Operation> imgOps = null;
//    Operation[] imgOps;
 //   if (homomorphism != null && imageAlgebra != null) {
 //     imgOps = new ArrayList<Operation>(imageAlgebra.operations().size());
      
  //  }
    
    // these final boolean are meant to help the jit compiler.
    final boolean reportNotNull = report == null ? false : true;
    final boolean imgAlgNull = imgOps == null ? true : false;
    final boolean eltToFindNotNull = eltToFind == null ? false : true;
    final boolean eltsToFindNotNull = eltsToFind == null ? false : true;
    final boolean operationNotNull = operation == null ? false : true;
    final boolean blocksNotNull = blocks == null ? false : true;
    final boolean valuesNotNull = values == null ? false : true;
    
    System.out.println("got to sgClosePower, reportNotNull: " + reportNotNull);
    final int power = algebra.getNumberOfFactors();
    ans = new ArrayList<IntArray>(elems);// IntArrays
    final List<int[]> rawList = new ArrayList<int[]>(); // the corr raw int[]
    for (Iterator<IntArray> it = elems.iterator(); it.hasNext(); ) {
      rawList.add(it.next().getArray());
    }
    final HashSet<IntArray> su = new HashSet<IntArray>(ans);
    int currentMark = ans.size();
    int pass = 0;
    final CloserTiming timing = reportNotNull ?  new CloserTiming(algebra, report) : null;
    //vvv;
    while (closedMark < currentMark) {
      String str = "pass: " + pass + ", size: " + ans.size();
      if (reportNotNull) {
        timing.updatePass(ans.size());
        report.setPass(pass);
        report.setPassSize(ans.size());
        report.addLine(str);
      }
      else {
        System.out.println(str);
      }
      pass++;
      // close the elements in current
      for (int i = 0; i < k; i++) {
        final int arity = arities[i];
        if (arity == 0) continue;  // worry about constansts later
        final int[] opTable = opTables[i];
        final int[] argIndeces = new int[arity];
        for (int r = 0; r < arity - 1; r++) {
          argIndeces[r] = 0;
        }
        argIndeces[arity - 1] = closedMark;
        ArrayIncrementor inc =
                    SequenceGenerator.sequenceIncrementor(
                                  argIndeces, currentMark - 1, closedMark);
        while (true) {
          if (Thread.currentThread().isInterrupted()) {
            if (reportNotNull) {
              report.setSize(ans.size());
              report.addEndingLine("cancelled ...");
            }
            return null;
          }
          //for (int i = 0; i < arity; i++) {
          //  arg[i] = rawList.get(argIndeces[i]);
          //}
          final int[] vRaw = new int[power];

//yyy;
          if (opTable != null) {
            for (int j = 0; j < power; j++) {
              int factor = algSize;
              int index = rawList.get(argIndeces[0])[j];
              for (int r = 1; r < arity; r++) {
                index += factor * rawList.get(argIndeces[r])[j];
                factor = factor * algSize;
              }
              vRaw[j] = opTable[index];
            }
          }
            
          else {
            final Operation f = ops.get(i);
            for (int j = 0; j < power; j++) {
              final int[] arg = new int[f.arity()];
              for (int r = 0; r < arity; r++) {
                arg[r] = rawList.get(argIndeces[r])[j];
              }
              vRaw[j] = f.intValueAt(arg);
            }
          }
          IntArray v = new IntArray(vRaw);
          if (reportNotNull) timing.incrementApps();
          if (su.add(v)) {
            ans.add(v);
            rawList.add(vRaw);
            if (reportNotNull) timing.incrementNextPassSize();
            if (reportNotNull) report.setSize(ans.size());
            if (Thread.currentThread().isInterrupted()) return null;
            if (termMap != null) {
              List<Term> children = new ArrayList<Term>(arity);
              for (int r = 0; r < arity; r++) {
                //children.set(i, termMap.get(arg.get(i)));
                children.add(termMap.get(ans.get(argIndeces[r])));
              }
              termMap.put(v, new NonVariableTerm(symbols[i], children));
              //logger.fine("" + v + " from " + f.symbol() + " on " + arg);
              if (operationNotNull) {
                Term term = termMap.get(v);
                // why are recreating vars each time ???
                List<Variable> vars = new ArrayList<Variable>(generators.size());
                for (IntArray ia : generators) {
                  vars.add((Variable)termMap.get(ia));
                }
                Operation termOp = term.interpretation(rootAlgebra, vars, true);
                if (Operations.equalValues(termOp, operation)) {
                  termForOperation = term;
                  if (reportNotNull) report.addEndingLine("found operation, term = " + term);
                  return ans;
                }
              }
            }
            if (eltToFindNotNull && v.equals(eltToFind)) {
              if (reportNotNull) {
                report.setSize(ans.size());
                report.addEndingLine("closing done, found "
                    + eltToFind + ", at " + ans.size());
              }
              return ans;
            }
            // the get is  likely to be null so do it in this order.
            if (eltsToFindNotNull  && minusOne.equals(indecesMapOfFoundElts.get(v))) {
              final int index = ans.size() - 1;
              indecesMapOfFoundElts.put(v, index);
              specialEltsFound++;
              System.out.println("found " + v);
              if (reportNotNull) report.addLine("found " + v + ", at " + index);
              if (specialEltsFound == eltsToFind.size()) {
                if (reportNotNull) report.addEndingLine("closing done, found all "
                    + eltsToFind.size() + " elems ");
                allEltsFound = true;
                return ans;
              }
            }
            if (blocksNotNull) {  // this assumes that if values != null then so is blocks
              boolean found = false;
              if (valuesNotNull) {  
                if (v.satisfiesConstraint(blocks, values)) found = true;
              }
              else {
                if (v.satisfiesConstraint(blocks)) found = true;
              }
              if (found) {
                eltToFind = v;
                if (reportNotNull) {
                  report.setSize(ans.size());
                  report.addEndingLine("closing done, found "
                      + eltToFind + ", at " + ans.size());
                }
                return ans;
              }
            }
            
            // can't quit early if we are looking for a homomorphism
            if (imgOps == null) {
              final int size = ans.size();
              if (imgAlgNull && algebra.cardinality() > 0 && size == algebra.cardinality()) {  
                if (reportNotNull) {
                  report.addEndingLine("found all " + size + " elements");
                  report.setSize(ans.size());
                }
                return ans;
              }
            }
            else {
              final int[] args = new int[arity];
              for (int t = 0; t < arity; t++) {
                args[t] = homomorphism.get(ans.get(argIndeces[t]));
              }
              homomorphism.put(v, imgOps[i].intValueAt(args));
            }
            if (Thread.currentThread().isInterrupted()) {
              if (reportNotNull) {
                report.setSize(ans.size());
                report.addEndingLine("cancelled ...");
              }
              return null;
            }
            
          }
          else {
            if (!imgAlgNull) {
              // here
              final int[] args = new int[arity];
              for (int t = 0; t < arity; t++) {
                args[t] = homomorphism.get(ans.get(argIndeces[t]));
              }
              if (homomorphism.get(v).intValue() != imgOps[i].intValueAt(args)) {
                List<Term> children = new ArrayList<Term>(arity);
                for (int r = 0; r < arity; r++) {
                  //children.set(i, termMap.get(arg.get(i)));
                  children.add(termMap.get(ans.get(argIndeces[r])));
                }
                failingEquation = new Equation(termMap.get(v),
                    new NonVariableTerm(symbols[i], children));
                final String line = "failing equation:\n" + failingEquation;
                if (reportNotNull) {
                  report.setSize(ans.size());
                  report.addEndingLine(line);
                }
                else {
                  System.out.println("failing equation:\n" + failingEquation);
                  System.out.println("size so far: " + ans.size());
                }
                return ans;
              }
            }
          }
          if (!inc.increment()) break;
        }

if (false) {
/*
  List middleZero = new ArrayList();
    for (Iterator it2 = ans.iterator(); it2.hasNext(); ) {
      IntArray ia = (IntArray)it2.next();
      if (ia.get(1) == 0) middleZero.add(ia);
    }
  System.out.println("jonsson level so far: "
     + Algebras.jonssonLevelAux(middleZero, 
                             (IntArray)ans.get(0),  (IntArray)ans.get(2)));
*/
}
      }
      closedMark = currentMark;
      currentMark = ans.size();
      if (imgAlgNull && algebra.cardinality() > 0 && currentMark >= algebra.cardinality()) break;
System.out.println("so far: " + currentMark);
//if (currentMark > 7) return ans;
    }
    final String str = "done closing, size = " + ans.size();
    if (reportNotNull) {
      report.setSize(ans.size());
      report.addEndingLine(str);
    }
    else System.out.println(str);
    completed = true;
    return ans;
  }
  
  public long countFuncApplications(int size0, int size1) {
    BigInteger ans = BigInteger.ZERO;
    final BigInteger s0 = BigInteger.valueOf(size0);
    final BigInteger s1 = BigInteger.valueOf(size1);
    for (Operation op : algebra.operations()) {
      final int r = op.arity();
      ans = ans.add(s1.pow(r).subtract(s0.pow(r)));
    }
    if (ans.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) > 0) return -1;
    return ans.longValue() * algebra.getNumberOfFactors();
  }
  
  /**
   * Note this assumes termMap is not null !!!!!!!!
   * 
   * @param map
   * @param indexInCPUs
   * @param numOfOps
   * @param closedMark
   * @param currentMark
   * @param reportNotNull
   * @param rawList
   * @param timing
   */
  public void doOneStagePartialClosure(final ConcurrentHashMap<IntArray,Term> map, 
                                       final int indexInCPUs,
                                       final int numOfOps, 
                                       final int closedMark, final int currentMark,
                                       final boolean reportNotNull,
                                       final List<int[]> rawList,
                                       final CloserTiming timing
                                       ) {
    for (int i = 0; i < numOfOps; i++) {
      //for (Iterator<Operation> it = algebra.operations().iterator(); it.hasNext(); ) {
      //Operation f = it.next();
      Operation f = algebra.operations().get(i);
      final int arity = f.arity();
      if (arity == 0) continue;  // worry about constansts later
      int[] argIndeces = new int[arity];
      for (int j = 0; j < arity - 1; j++) {
        argIndeces[j] = 0;
      }
      argIndeces[arity - 1] = closedMark;
      ArrayIncrementor inc =
        SequenceGenerator.sequenceIncrementor(
            argIndeces, currentMark - 1, closedMark, nCPUs);
      for (int j = 1; j < indexInCPUs; j++) {
        if (!inc.increment()) continue;
      }

      final int[][] arg = new int[arity][];
      while (true) {
        if (Thread.currentThread().isInterrupted()) {
          if (reportNotNull) {
            report.addEndingLine("cancelled ...");
            report.setSize(ans.size());
          }
          return;
        }
        for (int j = 0; j < arity; j++) {
          arg[j] = rawList.get(argIndeces[j]);
        }

        int[] vRaw = f.valueAt(arg);
        //int[] test = f.valueAt(arg);
        IntArray v = new IntArray(vRaw);
        //IntArray testV = new IntArray(test);

        //System.out.println("vRaw == test is " + v.equals(new IntArray(test)));
        if (reportNotNull) timing.incrementApps();
        //appsSoFar = appsSoFar + numberProjs;
        if (!termMap.containsKey(v) && !map.containsKey(v)) {
          List<Term> children = new ArrayList<Term>(arity);
          for (int j = 0; j < arity; j++) {
            //children.set(i, termMap.get(arg.get(i)));
            children.add(termMap.get(ans.get(argIndeces[j])));
          }
          Term term = new NonVariableTerm(f.symbol(), children);
          
          Term old = map.putIfAbsent(v, term);  // make sure it is still not there
          //System.out.println("v: " + v + ", term: " + term + ", old term:" + old + ", thread: " + indexInCPUs);
          System.out.println("v: " + v  + ", old term:" + old + ", thread: " + indexInCPUs);
        }
        if (reportNotNull) timing.incrementNextPassSize();
        if (reportNotNull) report.setSize(ans.size() + map.size());
        if (Thread.currentThread().isInterrupted()) return;

        if (!inc.increment()) break;
      }
    }
  }

 
  
  class ParallelWorker implements Runnable {
    
    CountDownLatch latch;
    int indexInCPUs;
    ConcurrentHashMap<IntArray,Term> map;
    final int numOfOps;
    final int closedMark; 
    final int currentMark;
    final boolean reportNotNull;
    final List<int[]> rawList;
    final CloserTiming timing;
    
    public ParallelWorker(CountDownLatch latch, int indexInCPUs, 
                          ConcurrentHashMap<IntArray,Term> map,
                          int numOfOps, int closedMark, int currentMark, boolean reportNotNull,
                          final List<int[]> rawList,
                          final CloserTiming timing) {
      this.latch =  latch;
      this.indexInCPUs = indexInCPUs;
      this.map = map;
      this.numOfOps = numOfOps;
      this.closedMark = closedMark;
      this.currentMark = currentMark;
      this.rawList = rawList;
      this.timing = timing;
      this.reportNotNull = reportNotNull;
    }
    
    public void run() {
      doOneStagePartialClosure(map, indexInCPUs, numOfOps, closedMark, currentMark, reportNotNull, rawList, timing);
      latch.countDown();
    }
    
  }
  
  
}
