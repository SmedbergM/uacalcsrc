/* Malcev.java 2001/09/02  */

package org.uacalc.alg;

import java.util.*;

import org.uacalc.ui.tm.ProgressReport;
import org.uacalc.util.*;
import org.uacalc.terms.*;
import org.uacalc.alg.conlat.*;
import org.uacalc.alg.sublat.*;
import org.uacalc.alg.op.*;
//import org.apache.log4j.*;
import java.util.logging.*;

/**
 * A class with static methods for Mal'cev conditions such as finding
 * Jonsson terms, etc. It also has methods for related things such as
 * finding a near unamimity term of a given arity, finding a near 
 * majority term, etc.
 *
 * @version $Id$
 */
public class Malcev {

  static Logger logger = Logger.getLogger("org.uacalc.alg.Malcev");
  static {
    logger.setLevel(Level.FINER);
  }
  static ProgressReport monitor;

  // make sure the class cannot be instantiated.
  private Malcev() {}

  //public static Monitor getMonitor() { return monitor; }
  public static void setMonitor(ProgressReport m) { monitor = m; }
  
  /**
   * Gives a Kearnes-Kiss join term. See their monograph, chapter 3.
   * 
   */
  public static Term joinTerm(SmallAlgebra alg) {
    return joinTerm(alg, null);
  }
  
  /**
   * Gives a Kearnes-Kiss join term. See their monograph, chapter 3.
   * 
   */
  public static Term joinTerm(SmallAlgebra alg, ProgressReport report) {
    if (alg.cardinality() == 1)  return Variable.x;
    final Term taylor = markovicMcKenzieSiggersTaylorTerm(alg, report);
    final Map<Variable,Term> map = new HashMap<Variable,Term>(4);
    final Variable x0 = new VariableImp("x0");
    final Variable x1 = new VariableImp("x1");
    final Variable x2 = new VariableImp("x2");
    final Variable x3 = new VariableImp("x3");
    map.put(x0, Variable.x);
    map.put(x1, Variable.x);
    map.put(x2, Variable.y);
    map.put(x3, Variable.y);
    final Term t0 = taylor.substitute(map);
    
    map.put(x0, Variable.x);
    map.put(x1, Variable.x);
    map.put(x2, Variable.y);
    map.put(x3, Variable.x);
    final Term t1 = taylor.substitute(map);
    
    map.put(x0, Variable.y);
    map.put(x1, Variable.x);
    map.put(x2, Variable.x);
    map.put(x3, Variable.x);
    final Term t2 = taylor.substitute(map);
    
    final Term t3 = t2;
    
    map.put(x0, t0);
    map.put(x1, t1);
    map.put(x2, t2);
    map.put(x3, t3);
    // TODO: need to simplify this term !!!!!!!!!!!!!
    return taylor.substitute(map);
  }
  
  public List<Term> findWNUTerms(SmallAlgebra alg, int arity, ProgressReport report) {
    // TODO: write code
    return null;
  }
  
  private static int involutionAuto(int i, FreeAlgebra f2, Map<Integer,Integer> auto) {
    if (auto.get(i) == null) {
      Map<Term,Integer> substMap = new HashMap<Term,Integer>(2);
      substMap.put(Variable.x, 1); // 1 is the y generator
      substMap.put(Variable.y, 0);
      Term t = f2.getTerm((IntArray)f2.getElement(i));
      auto.put(i, t.intEval(f2, substMap));
    }
    return auto.get(i); 
  }
  
  public static List<Term> sdmeetTerms(SmallAlgebra alg) {
    return  sdmeetTerms(alg, null);
  }
  
  public static List<Term> sdmeetTerms(SmallAlgebra alg, ProgressReport report) {
    if (alg.cardinality() == 1) {
      List<Term> ans = new ArrayList<Term>(3);
      ans.add(Variable.x);
      ans.add(Variable.x);
      ans.add(Variable.x);
      return ans;
    }
    List<Term> ans = new ArrayList<Term>(3);
    final boolean isIdempotent = alg.isIdempotent();
    // if idempotent add the polynomial times test.
    FreeAlgebra f2 = new FreeAlgebra(alg, 2, report);
    Operation autoXY = f2.switchXandYAutomorphism();
    // ** Need to put in a test if the tables will fit in memory. **
    f2.makeOperationTables();
    IntArray g0 = new IntArray(new int[] {0,0,1,0});
    IntArray g1 = new IntArray(new int[] {0,1,0,0});
    IntArray g2 = new IntArray(new int[] {1,0,0,0});
    List<IntArray> gens = new ArrayList<IntArray>(3);
    gens.add(g0);
    gens.add(g1);
    gens.add(g2);
    final Map<IntArray,Term> termMap = new HashMap<IntArray,Term>(3);
    termMap.put(g0, new VariableImp("x"));
    termMap.put(g1, new VariableImp("y"));
    termMap.put(g2, new VariableImp("z"));
    BigProductAlgebra f2pow = new BigProductAlgebra(f2, 4);
    Closer closer = new Closer(f2pow, gens, termMap);
    closer.setProgressReport(report);  
    List<IntArray> univ = closer.sgClosePower();
    List<IntArray> univX; // members of univ with last coordinate x
    if (isIdempotent) {
      univX = univ;
    }
    else {
      univX = new ArrayList<IntArray>();
      for (IntArray ia : univ) {
        if (ia.get(3) == 0) univX.add(ia);  // 0 means it's x
      }
    }
    // a TreeMap might be a better choice
    Map<Integer,IntArray> aaaMap = new HashMap<Integer,IntArray>();
    Map<Integer,Map<Integer,IntArray>> aabMap = new HashMap<Integer,Map<Integer,IntArray>>();
    Map<Integer,Map<Integer,IntArray>> caaMap = new HashMap<Integer,Map<Integer,IntArray>>();
    for (IntArray ia : univX) {
      if (ia.get(0) == ia.get(1)) {
        if (ia.get(1) == ia.get(2)) { // so ia looks like aaax
          if (ia.get(0) == autoXY.intValueAt(ia.get(0))) { // if a is invariant under x <--> y on F(x,y)
            ans.add(termMap.get(ia));
            return ans; 
          }
          aaaMap.put(ia.get(0), ia);
        }
        Map<Integer,IntArray> bMap = aabMap.get(ia.get(0));
        if (bMap == null) {
          bMap = new HashMap<Integer,IntArray>();
          aabMap.put(ia.get(0), bMap);
        }
        bMap.put(ia.get(2), ia);
      }
      if (ia.get(1) == ia.get(2)) {
        Map<Integer,IntArray> cMap = caaMap.get(ia.get(2));
        if (cMap == null) {
          cMap = new HashMap<Integer,IntArray>();
          caaMap.put(ia.get(2), cMap);
        }
        cMap.put(ia.get(0), ia);
      }
    }
    for (Integer a : aaaMap.keySet()) {
      for (int b : aabMap.get(a).keySet()) {
        int c = autoXY.intValueAt(b);  // do we need to use an array [b] ?
        if (caaMap.get(a) != null && caaMap.get(a).get(c) != null) {
          System.out.println("a: " + a + " b: " + b + " c: " + c);
          Map<IntArray,Term> f2TM = f2.getTermMap();
          System.out.println("a: " + f2TM.get(f2.getElement(a)));
          System.out.println("b: " + f2TM.get(f2.getElement(b)));
          System.out.println("c: " + f2TM.get(f2.getElement(c)));
          System.out.println("a: " + aaaMap.get(a));
          System.out.println("aab: " + aabMap.get(a).get(b));
          System.out.println("caa: " + caaMap.get(a).get(c));
          ans.add(termMap.get(aabMap.get(a).get(b)));
          ans.add(termMap.get(aaaMap.get(a)));
          ans.add(termMap.get(caaMap.get(a).get(c)));
          return ans;
        }
      }
    }
    return null;
  }
  
  
  /**
   * Gives a term t(x,y,z,u) satisfying t(y,x,x,x) = t(x,x,y,y) and
   * t(x,x,y,x) = t(x,y,x,x).
   * 
   */
  public static Term markovicMcKenzieSiggersTaylorTerm(SmallAlgebra alg) {
    return  markovicMcKenzieSiggersTaylorTerm(alg, null);
  }
  
  /**
   * Gives a term t(x,y,z,u) satisfying t(y,x,x,x) = t(x,x,y,y) and
   * t(x,x,y,x) = t(x,y,x,x).
   * 
   */
  public static Term markovicMcKenzieSiggersTaylorTerm(SmallAlgebra alg, 
                                                       ProgressReport report) {
    
    if (alg.cardinality() == 1)  return Variable.x;
    final boolean isIdempotent = alg.isIdempotent();
    final int[][] blocks = new int[][] {{0,1},{2,3}};
    final int[][] values = new int[][] {{4,0}};
    FreeAlgebra f2 = new FreeAlgebra(alg, 2, report);
    // ** Need to put in a test if the tables will fit in memory. **
    f2.makeOperationTables();
    //logger.info("f2 size is " + f2.cardinality());
    IntArray g0;
    IntArray g1;
    IntArray g2;
    IntArray g3;
    if (isIdempotent) {
      g0 = new IntArray(new int[] {1,0,0,0});
      g1 = new IntArray(new int[] {0,0,0,1});
      g2 = new IntArray(new int[] {0,1,1,0});
      g3 = new IntArray(new int[] {0,1,0,0});
    }
    else {
      g0 = new IntArray(new int[] {1,0,0,0,0});
      g1 = new IntArray(new int[] {0,0,0,1,0});
      g2 = new IntArray(new int[] {0,1,1,0,0});
      g3 = new IntArray(new int[] {0,1,0,0,0});
    }
    List<IntArray> gens = new ArrayList<IntArray>(4);
    gens.add(g0);
    gens.add(g1);
    gens.add(g2);
    gens.add(g3);
    final Map<IntArray,Term> termMap = new HashMap<IntArray,Term>(4);
    termMap.put(g0, new VariableImp("x0"));
    termMap.put(g1, new VariableImp("x1"));
    termMap.put(g2, new VariableImp("x2"));
    termMap.put(g3, new VariableImp("x3"));
    //final IntArray yx = new IntArray(new int[] {1,0});
    BigProductAlgebra f2pow;
    if (isIdempotent) {
      f2pow = new BigProductAlgebra(f2, 4);
    }
    else {
      f2pow = new BigProductAlgebra(f2, 5);
    }
    Closer closer = new Closer(f2pow, gens, termMap);
    closer.setProgressReport(report);
    closer.setBlocks(blocks);
    if (!isIdempotent) closer.setValues(values);
    closer.sgClosePower();
    if (closer.getElementToFind() != null) {
      return termMap.get(closer.getElementToFind());
    }
    
    /*
    List sub = f2pow.sgClose(gens, termMap);
    //logger.info("sub alg of the " + (isIdempotent ? "third" : "fourth")
    //   + " power of f2 size " + sub.size());
    IntArray ia = null;
    for (Iterator it = sub.iterator(); it.hasNext(); ) {
      ia = (IntArray)it.next();
      if (ia.get(0) == ia.get(1) && ia.get(2) == ia.get(3)) {
        if (isIdempotent) break;
        if (ia.get(4) == 0) break; // last coord is x
      }
      ia = null;
    }
    if (ia != null) return (Term)termMap.get(ia);
    */
    return null;
  }

    
  
  
  /**
   * This will find a near unamimity term of the given arity
   * if one exits; otherwise it return <tt>null</tt>.
   */
  public static Term findNUF(SmallAlgebra alg, int arity) {
    return findNUF(alg, arity, null);
  }
  
  /**
   * This will find a near unanimity term of the given arity
   * if one exits; otherwise it return <tt>null</tt>.
   */
  public static Term findNUF(SmallAlgebra alg, int arity, ProgressReport report) {
    if (alg.cardinality() == 1) return new VariableImp("x0");
    FreeAlgebra f2 = new FreeAlgebra(alg, 2, report);
 // ** Need to put in a test if the tables will fit in memory. **
    f2.makeOperationTables();
    final List<IntArray> gens = new ArrayList<IntArray>(arity);
    final Map<IntArray,Term> termMap = new HashMap<IntArray,Term>();
    for (int i = 0; i < arity ; i++) {
      final int[] arr = new int[arity];
      arr[i] = 1;
      IntArray ia = new IntArray(arr);
      gens.add(ia);
      Variable var = arity > 3 ? new VariableImp("x" + i) : null;
      if (var == null) {
        if (i == 0) var = Variable.x;
        else if (i == 1) var = Variable.y;
        else var = Variable.z;
      }
      termMap.put(ia, var);
    }
    BigProductAlgebra f2power = new BigProductAlgebra(f2, arity);
    final IntArray zero = new IntArray(new int[arity]);
    if (report != null) report.addStartLine("looking for (x,x,...x) in Sg_F(2)^" + arity 
        + " generated by (y,x,x...,x), (x,y,x,...,x), ...");
    List<IntArray> sub = f2power.sgClose(gens, termMap, zero, report);
    if (sub.contains(zero)) {
      if (report != null) report.addEndingLine("found (x,x,...x)");
      return termMap.get(zero);
    }
    if (report != null) report.addEndingLine("could not find (x,x,...x)");
    return null;
  }

  /**
   * If \alpha = Cg(a,c) \meet Cg(a,b) 
   * and \beta = Cg(a,c) \meet Cg(b,c) this gives number of alteration
   * of \alpha and \beta to get from a to c in the join of \alpha and
   * \beta. It returns -1 if (a,c) is not in the join.
   */
  public static int localDistributivityLevel(int a, int b, int c, 
                                             SmallAlgebra alg) {
    final Partition cgab = alg.con().Cg(a,b);
    final Partition cgac = alg.con().Cg(a,c);
    final Partition cgbc = alg.con().Cg(b,c);
    return BasicPartition.permutabilityLevel(a, c, 
                                       cgac.meet(cgab), cgac.meet(cgbc));
  }

  /**
   * Find the max level  over all triples a, b, c, where,
   * if \alpha = Cg(a,c) \meet Cg(a,b) 
   * and \beta = Cg(a,c) \meet Cg(b,c) the level is the number of alteration
   * of \alpha and \beta to get from a to c in the join of \alpha and
   * \beta. It return -1 if for some triple, (a,c) is not in the join.
   */
  public static int localDistributivityLevel(SmallAlgebra alg) {
    final int size = alg.cardinality();
    int maxLevel = -1;
    SmallAlgebra maxLevelAlg = null;
    for (int a = 0; a < size; a++) {
      for (int b = 0; b < size; b++) {
        for (int c = a + 1; c < size; c++) {  // a,c symmetry
          if (a == b || b == c) continue; // a == c is impossible
          BasicSet bs = alg.sub().sg(new int [] {a, b, c});
          Subalgebra sub = new Subalgebra(alg, bs);
          int level = localDistributivityLevel(sub.index(a), 
                                           sub.index(b), sub.index(c), sub);
          if (level == -1) return -1; // not distributive
          if (level > maxLevel) {
            maxLevel = level;
            maxLevelAlg = sub;
            logger.info("max level now is " + maxLevel);
          }
        }
      }
    }
/*
System.out.println("the card of maxLevelAlg is " + maxLevelAlg.cardinality() +
                   ", its con size is " + maxLevelAlg.con().cardinality());
try {
org.uacalc.io.AlgebraIO.writeAlgebraFile(maxLevelAlg, "/tmp/baker4.xml");
}
catch (java.io.IOException e) {}
org.uacalc.ui.LatDrawer.drawLattice(new org.uacalc.lat.BasicLattice("", maxLevelAlg.con(), true));
*/
    return maxLevel;
  }


  /**
   * Find a weak majority term for (the variety generated by) this 
   * algebra or <tt>null</tt> if there is none. 
   * A <i>weak majority term</i> is one satifying
   * <tt>m(x,x,x) = x</tt> and <tt>m(x,x,y) = m(x,y,x) = m(y,x,x)</tt>.
   * Any finite algebra that has relational width 3 (in the sense 
   * of the constraint satisfaction problem) must have a 
   * weak-majority term. Questions (that Matt told me about):
   * <br>
   * 1. is there a finite algebra with Jonsson terms but no weak 
   * majority term?
   * <br>
   * 2. is there a finite algebra with a weak unamimity term but no 
   * weak majority term?
   * <br>
   * The method looks in the subalgebra generated by (x,x,y,x),
   * (x,y,x,x), (y,x,x,x) for an element of the form (a,a,a,x).
   */
  public static Term weakMajorityTerm(SmallAlgebra alg) {
    return weakMajorityTerm(alg, false);
  }

  /**
   * Find a weak majority term for (the variety generated by) this 
   * algebra or <tt>null</tt> if there is none using a faster algorithm
   * if the algebra is idempotent. 
   */
  public static Term weakMajorityTerm(SmallAlgebra alg, boolean isIdempotent) {
    if (alg.cardinality() == 1)  return Variable.x;
    FreeAlgebra f2 = new FreeAlgebra(alg, 2);
    // ** Need to put in a test if the tables will fit in memory. **
    f2.makeOperationTables();
    logger.info("f2 size is " + f2.cardinality());
    IntArray g0;
    IntArray g1;
    IntArray g2;
    if (isIdempotent) {
      g0 = new IntArray(new int[] {0,0,1});
      g1 = new IntArray(new int[] {0,1,0});
      g2 = new IntArray(new int[] {1,0,0});
    }
    else {
      g0 = new IntArray(new int[] {0,0,1,0});
      g1 = new IntArray(new int[] {0,1,0,0});
      g2 = new IntArray(new int[] {1,0,0,0});
    }
    List gens = new ArrayList(3);
    gens.add(g0);
    gens.add(g1);
    gens.add(g2);
    final HashMap termMap = new HashMap(3);
    termMap.put(g0, Variable.x);
    termMap.put(g1, Variable.y);
    termMap.put(g2, Variable.z);
    //final IntArray yx = new IntArray(new int[] {1,0});
    BigProductAlgebra f2pow;
    if (isIdempotent) {
      f2pow = new BigProductAlgebra(f2, 3);
    }
    else {
      f2pow = new BigProductAlgebra(f2, 4);
    }
    // the yx argument means stop if yx is found. 
    // of course we want something different if we want the shortest term.
    List sub = f2pow.sgClose(gens, termMap);
    logger.info("sub alg of the " + (isIdempotent ? "third" : "fourth")
       + " power of f2 size " + sub.size());
    //if (sub.contains(yx)) return (Term)termMap.get(yx);
    IntArray ia = null;
    for (Iterator it = sub.iterator(); it.hasNext(); ) {
      ia = (IntArray)it.next();
      if (ia.get(0) == ia.get(1) && ia.get(1) == ia.get(2)) {
        if (isIdempotent) break;
        if (ia.get(3) == 0) break; // last coord is x
      }
      ia = null;
    }
    if (ia != null) return (Term)termMap.get(ia);
    return null;
  }

  /**
   * This returns a list of Jonsson terms witnessing distributivity, or 
   * null if the algebra does generate a congruence distributive variety.
   * It is guarenteed to be the least number of terms possible.
   */
  public static List<Term> jonssonTerms(SmallAlgebra alg) {
    return jonssonTerms(alg, false);
  }
  
  public static List<Term> jonssonTerms(SmallAlgebra alg, boolean alvinVariant) {
    return jonssonTerms(alg, alvinVariant, null);
  }

  /**
   * This returns a list of Jonsson terms witnessing distributivity, or 
   * null if the algebra does generate a congruence distributive variety.
   * It is guaranteed to be the least number of terms possible.
   *
   * @param alvinVariant interchange even and odd in Jonsson's equations
   */
  public static List<Term> jonssonTerms(SmallAlgebra alg, 
                                        boolean alvinVariant, ProgressReport report) {
    if (report != null) report.addStartLine("finding Jonsson terms " 
    		                          + (alvinVariant ? " (ALV variant)" : ""));
    if (alg.isIdempotent()) {
      if (findDayQuadrupleInSquare(alg, report) != null) {
        if (report != null) {
          report.addEndingLine("done finding Jonsson terms; there are none");
        }
        return null;
      }
    }
    List<Term> ans = new ArrayList<Term>();
    if (alg.cardinality() == 1) {
      ans.add(Variable.x);
      ans.add(Variable.z);
      return ans;
    }
    FreeAlgebra f2 = new FreeAlgebra(alg, 2, report);
    // ** Need to put in a test if the tables will fit in memory. **
    f2.makeOperationTables();
    //logger.info("f2 size is " + f2.cardinality());
    IntArray g0 = new IntArray(new int[] {0,0,1});
    IntArray g1 = new IntArray(new int[] {0,1,0});
    IntArray g2 = new IntArray(new int[] {1,0,0});
    List<IntArray> gens = new ArrayList<IntArray>(3);
    gens.add(g0);
    gens.add(g1);
    gens.add(g2);
    final HashMap<IntArray, Term> termMap = new HashMap<IntArray, Term>(3);
    termMap.put(g0, Variable.x);
    termMap.put(g1, Variable.y);
    termMap.put(g2, Variable.z);
    BigProductAlgebra f2cubed = new BigProductAlgebra(f2, 3);
    final IntArray zero = new IntArray(new int[] {0,0,0});
    final IntArray pixley = new IntArray(new int[] {1,0,1});
    //List sub;
    if (report != null)
      report.addStartLine("computing the subalgebra of F(2)^3 generated by (x,x,y) (x,y,x), (y,x,x).");
    List<IntArray> sub = f2cubed.sgClose(gens, termMap, null, report);
    if (report != null)
      report.addEndingLine("done finding subalgebra of F(2)^3.");
//System.out.println("sub = " + sub);
    if (report != null) report.addLine("sub alg of f2 cubed size is " + sub.size());
    if (sub.contains(pixley)) {
      if (report != null) report.addLine("this variety has a pixley term: " 
                                          + termMap.get(pixley));
    }
    else if (sub.contains(zero)) {
      if (report != null) report.addLine("this variety has a majority term");
    }
    if (alvinVariant) {
      if (sub.contains(pixley)) {
        //if (report != null) report.addLine("this variety has a pixley term");
        //logger.info("this variety has a pixley term");
        ans.add(Variable.x);
        ans.add(termMap.get(pixley));
        ans.add(Variable.z);
        return ans;
      }
    }
    else {
      if (sub.contains(zero)) {
        //if (report != null) report.addLine("this variety has a majority term");
        //logger.info("this variety has a ternary majority term");
        ans.add(Variable.x);
        ans.add(termMap.get(zero));
        ans.add(Variable.z);
        return ans;
      }
    }

    List<IntArray> middleZero = new ArrayList<IntArray>();
    for (Iterator<IntArray> it = sub.iterator(); it.hasNext(); ) {
      IntArray ia = it.next();
      if (ia.get(1) == 0) middleZero.add(ia);
    }
    Comparator<IntArray> c = new Comparator<IntArray>() {
        public int compare(IntArray ia1, IntArray ia2) {
          for (int i = 0; i < ia1.universeSize(); i++) {
            if (ia1.get(i) < ia2.get(i)) return -1;
            if (ia1.get(i) > ia2.get(i)) return 1;
          }
          return 0;
        }
        public boolean equals(Object o) { return false; }
    };
    Collections.sort(middleZero, c);
    for (IntArray ia : middleZero) {  
      logger.finer("" + ia);
    }
    final List path = jonssonLevelPath(middleZero, g0, g2, false);
    final List path2 = jonssonLevelPath(middleZero, g0, g2, true);
    if (path == null) {
      if (path2 == null) return null;
      ans = path2TermList(path2, termMap);
      if (!alvinVariant) {
        ans.add(0, ans.get(0));
        if (report != null) report.addLine("The ALVIN variant is shorter than Jonsson.");
        // add to info that the alv variant Jon terms are shorter
      }
      return ans;
    }
    if (path2 == null || path.size() < path2.size()) {
      ans = path2TermList(path, termMap);
      if (alvinVariant) {
        ans.add(0, ans.get(0));
        if (report != null) report.addLine("Jonsson is shorter than the ALVIN variant.");
        // add to info that the Jon is shorter than alv variant 
      }
      return ans;
    }
    if (alvinVariant) return path2TermList(path2, termMap);
    if (path2.size() < path.size()) {
      ans = path2TermList(path2, termMap);
      ans.add(0, ans.get(0));
      return ans;
    }
    return path2TermList(path, termMap);
  }

  private static List<Term> path2TermList(List path, 
                                          HashMap<IntArray, Term> termMap) {
    List<Term> ans = new ArrayList<Term>();
    for (Iterator it = path.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      ans.add(termMap.get(ia));
    }
    return ans;
  }

  /**
   * This finds a path from g0 to g2 in <tt>middleZero</tt>, a list
   * of triples, where two triples are connected by an edge if either
   * their first or third coordinates agree. When alvinVariant is 
   * true, it starts with changing the third coordinate; otherwise
   * with the first.
   */
  public static List< IntArray> jonssonLevelPath(List<IntArray> middleZero, 
                                      IntArray g0, IntArray g2,
                                      boolean alvinVariant) {
    // We need to allow for the possibility that we can't
    // get started, especially for the alvinVariant.
    // I suspect, but haven't been able prove, that if 
    // terms exist in the alvinVariant, they exist (and
    // with the same number) in the original Jonsson condition.
    // It is easy when the "level" odd (each implies the other)
    // and it is also true when the level is 2 (a pixley term
    // implies a majority term).

    // a list of lists of IntArray's
    final List<List<IntArray>> levels = new ArrayList<List<IntArray>>();
    final Map parentMap = new HashMap();
    List currentLevel = new ArrayList();
    //final HashSet visited = new HashSet();
    currentLevel.add(g0);
    parentMap.put(g0, null);
    levels.add(currentLevel);
    Comparator c0 = new Comparator() {
        public int compare(Object o1, Object o2) {
          return ((IntArray)o2).get(0) - ((IntArray)o1).get(0);
        }
    };
    Comparator c2 = new Comparator() {
        public int compare(Object o1, Object o2) {
          return ((IntArray)o2).get(2) - ((IntArray)o1).get(2);
        }
    };
    HashMap classes0 = new HashMap();
    HashMap classes2 = new HashMap();
    for (Iterator it = middleZero.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      Integer ia_0 = new Integer(ia.get(0));
      Integer ia_2 = new Integer(ia.get(2));
      List eqclass = (List)classes0.get(ia_0);
      if (eqclass == null) {
        eqclass = new ArrayList();
        classes0.put(ia_0, eqclass);
      }
      eqclass.add(ia);
      eqclass = (List)classes2.get(ia_2);
      if (eqclass == null) {
        eqclass = new ArrayList();
        classes2.put(ia_2, eqclass);
      }
      eqclass.add(ia);
    } 
    boolean even = alvinVariant;
    while (true) {
      even = !even;
      final List nextLevel = new ArrayList();
//System.out.println("current level size = " + currentLevel.size());
      //System.out.println("even = " + even);
      //System.out.println("current level size = " + currentLevel.size());
      for (Iterator it = currentLevel.iterator(); it.hasNext(); ) {
        IntArray ia = (IntArray)it.next();
        List eqclass;
        if (even) eqclass = (List)classes0.get(new Integer(ia.get(0)));
        else eqclass = (List)classes2.get(new Integer(ia.get(2)));
        //eqclass.remove(ia);
        for (Iterator it2 = eqclass.iterator(); it2.hasNext(); ) {
          IntArray ia2 = (IntArray)it2.next();
          //if (ia2.equals(ia)) continue;
          if (!parentMap.containsKey(ia2)) {
            parentMap.put(ia2, ia);
            nextLevel.add(ia2);
          }
          if (ia2.equals(g2)) {
            final List path = new ArrayList(levels.size() + 1);
            path.add(g2);
            ia2 = (IntArray)parentMap.get(ia2);
            while (ia2 != null) {
              path.add(ia2);
              ia2 = (IntArray)parentMap.get(ia2);
            }
            Collections.reverse(path);
            logger.fine("path");
            for (Iterator iter = path.iterator(); iter.hasNext(); ) {
              logger.fine("" + iter.next());
            }
            return path;
          }
        }
      }
      if (nextLevel.isEmpty()) break;    
      levels.add(nextLevel);
      currentLevel = nextLevel;
    }
    return null;
  }

  /**
   * If this algebra generates a distributive variety, this returns
   * the minimal number of Jonsson terms minus 1; otherwise it returns -1,
   * but it is probably better to use <tt>jonssonTerms</tt> and get
   * get the actual terms.
   * So congruence distributivity can be tested by seeing if this 
   * this is positive. If the algebra has only one element, it returns 1.
   * For a lattice it returns 2. For Miklos Marioti's 5-ary near unanimity
   * operation on a 4 element set, it returns 6 (the maximum possible).
   */
  public static int jonssonLevel(SmallAlgebra alg) {
    if (alg.cardinality() == 1) return 1;
    FreeAlgebra f2 = new FreeAlgebra(alg, 2);
    f2.makeOperationTables();
    logger.info("f2 size is " + f2.cardinality());
    IntArray g0 = new IntArray(new int[] {0,0,1});
    IntArray g1 = new IntArray(new int[] {0,1,0});
    IntArray g2 = new IntArray(new int[] {1,0,0});
    List gens = new ArrayList(3);
    gens.add(g0);
    gens.add(g1);
    gens.add(g2);
    final HashMap termMap = new HashMap(3);
    termMap.put(g0, Variable.x);
    termMap.put(g1, Variable.y);
    termMap.put(g2, Variable.z);
    BigProductAlgebra f2cubed = new BigProductAlgebra(f2, 3);
    List sub = f2cubed.sgClose(gens, termMap);
    logger.info("sub alg of f2 cubed size is " + sub.size());
    final IntArray zero = new IntArray(new int[] {0,0,0});
    if (sub.contains(zero)) {
      logger.info("this variety has a ternary majority function");
      return 2;
    }
    List middleZero = new ArrayList();
    for (Iterator it = sub.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      if (ia.get(1) == 0) middleZero.add(ia);
    }
    Comparator c = new Comparator() {
        public int compare(Object o1, Object o2) {
          IntArray ia1 = (IntArray)o1;
          IntArray ia2 = (IntArray)o2;
          for (int i = 0; i < ia1.universeSize(); i++) {
            if (ia1.get(i) < ia2.get(i)) return -1;
            if (ia1.get(i) > ia2.get(i)) return 1;
          }
          return 0;
        }
        public boolean equals(Object o) { return false; }
    };
    Collections.sort(middleZero, c);
    return jonssonLevelAux(middleZero, g0, g2);
  }

  public static int jonssonLevelAux(List middleZero, IntArray g0,
                                              IntArray g2) {
    // a list of lists of IntArray's
    final List levels = new ArrayList();
    List currentLevel = new ArrayList();
    final HashSet visited = new HashSet();
    currentLevel.add(new IntArray[] {g0, null});
    visited.add(g0);
    levels.add(currentLevel);
    Comparator c0 = new Comparator() {
        public int compare(Object o1, Object o2) {
          return ((IntArray)o2).get(0) - ((IntArray)o1).get(0);
        }
    };
    Comparator c2 = new Comparator() {
        public int compare(Object o1, Object o2) {
          return ((IntArray)o2).get(2) - ((IntArray)o1).get(2);
        }
    };
    HashMap classes0 = new HashMap();
    HashMap classes2 = new HashMap();
    for (Iterator it = middleZero.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      Integer ia_0 = new Integer(ia.get(0));
      Integer ia_2 = new Integer(ia.get(2));
      List eqclass = (List)classes0.get(ia_0);
      if (eqclass == null) {
        eqclass = new ArrayList();
        classes0.put(ia_0, eqclass);
      }
      eqclass.add(ia);
      eqclass = (List)classes2.get(ia_2);
      if (eqclass == null) {
        eqclass = new ArrayList();
        classes2.put(ia_2, eqclass);
      }
      eqclass.add(ia);
    } 
    boolean even = false;
    while (true) {
      if (even) even = false;
      else even = true;
      final List nextLevel = new ArrayList();
//System.out.println("current level size = " + currentLevel.size());
      for (Iterator it = currentLevel.iterator(); it.hasNext(); ) {
        IntArray ia = ((IntArray[])it.next())[0];
        List eqclass;
        if (even) eqclass = (List)classes0.get(new Integer(ia.get(0)));
        else eqclass = (List)classes2.get(new Integer(ia.get(2)));
        //eqclass.remove(ia);
        for (Iterator it2 = eqclass.iterator(); it2.hasNext(); ) {
          IntArray ia2 = (IntArray)it2.next();
          //if (ia2.equals(ia)) continue;
          if (ia2.equals(g2)) {
//System.out.println("returning " + (levels.size() + 1));

            return levels.size();
          }
          if (!visited.contains(ia2)) {
            visited.add(ia2);
            nextLevel.add(new IntArray[] {ia2, ia});
          }
        }
      }
      if (nextLevel.isEmpty()) break;    
      levels.add(nextLevel);
      currentLevel = nextLevel;
//System.out.println("levels size is " + levels.size());
    }
    return -1;
  }

  /**
   * Find a Day quadruple of the form a = (x0, x1), b = (x0, y1),
   * c = y0, x1), d = (y0, y1). Since Day quadruples are invariant
   * undere the permutation (ab)(cd), we can take x1 &lt; y1.
   *
   */
  public static IntArray findDayQuadrupleInSquare(final SmallAlgebra alg,
                                                  final ProgressReport report) {
    if (report != null) report.addStartLine("Looking for a Day quadruple in A^2");
    final int n = alg.cardinality();
    final BigProductAlgebra sq = new BigProductAlgebra(alg, 2);
    final IntArray a = new IntArray(2);
    final IntArray b = new IntArray(2);
    final IntArray c = new IntArray(2);
    final IntArray d = new IntArray(2);
    final int[] avec = a.toArray();
    final int[] bvec = b.toArray();
    final int[] cvec = c.toArray();
    final int[] dvec = d.toArray();
    final List gens = new ArrayList(4);
    gens.add(a);
    gens.add(b);
    gens.add(c);
    gens.add(d);
    for (int x0 = 0; x0 < n; x0++) {
      for (int x1 = 0; x1 < n; x1++) {
        for (int y0 = 0; y0 < n; y0++) {
          for (int y1 = x1 + 1; y1 < n; y1++) {
            avec[0] = x0;
            avec[1] = x1;
            bvec[0] = x0;
            bvec[1] = y1;
            cvec[0] = y0;
            cvec[1] = x1;
            dvec[0] = y0;
            dvec[1] = y1;
//System.out.println("x0 = " + x0 + ", x1 = " + x1 + 
//                  ", y0 = " + y0 + ", y1 = " + y1);
            final SmallAlgebra sub = new SubProductAlgebra("", sq, gens);
//TypeFinder tf = new TypeFinder(sub);
//System.out.println("types = " + tf.findTypeSet());

            final int aIndex = sub.elementIndex(a);
            final int bIndex = sub.elementIndex(b);
            final int cIndex = sub.elementIndex(c);
            final int dIndex = sub.elementIndex(d);
            
            if (dayQuadruple(aIndex, bIndex, cIndex, dIndex, sub)) {
              if (report != null) {
                report.setWitnessAlgebra(new AlgebraWithGeneratingVector(sub, 
                                         new int[] {aIndex, bIndex, cIndex, dIndex}));
                report.addLine("Found a Day quadruple in the subalgebra of A^2");
                report.addLine("generated by a = (" + x0 + "," + x1 + "), b = ("
                               + x0 + "," + y1 + "), c = ("
                               + y0 + "," + x1 + "), d = ("
                               + y0 + "," + y1 + ")");
                report.addEndingLine("the congruences Cg(c,d), Cg(a,b)(c,d) and Cg(a,c)(b,d) generate " 
                               + " a nonmodular lattice");
              }
              return new IntArray(new int[] {x0, x1, y0, y1});
            }
          }
        }
      }
    }
    if (report != null) {
      report.addLine("There are no Day quadruples in the subalgebras of A^2.");
      report.addEndingLine("So this algebra lies in a CM variety.");
    }
    return null;
  }


  public static boolean dayQuadruple(int a, int b, int c, int d,
                                                     SmallAlgebra alg) {
    final Partition cgcd = alg.con().Cg(c,d);
    final Partition cgab_cd = alg.con().Cg(a,b).join(cgcd);
    final Partition cgac_bd = alg.con().Cg(a,c).join(alg.con().Cg(b, d));
    return !cgcd.join(cgab_cd.meet(cgac_bd)).isRelated(a,b);
  }

  public static boolean congruenceModularVariety(SmallAlgebra alg) {
    if (alg.isIdempotent()) return congruenceModularForIdempotent(alg);
    FreeAlgebra f2 = new FreeAlgebra(alg, 2);
    // ** Need to put in a test if the tables will fit in memory. **
    f2.makeOperationTables();
    //List gens = f2.generators();
    IntArray a = new IntArray(new int[] {0,0});
    IntArray b = new IntArray(new int[] {0,1});
    IntArray c = new IntArray(new int[] {1,0});
    IntArray d = new IntArray(new int[] {1,1});
    List gens = new ArrayList(4);
    gens.add(a);
    gens.add(b);
    gens.add(c);
    gens.add(d);
    BigProductAlgebra f2squared = new BigProductAlgebra(f2, 2);
    //List sub = f2squared.sgClose(gens, termMap);
    SmallAlgebra sub = new SubProductAlgebra("", f2squared, gens);
    // ** Need to put in a test if the tables will fit in memory. **
    sub.makeOperationTables();
    logger.info("sub alg of f2 square size is " + sub.cardinality());
    Partition cgcd = sub.con().Cg(c, d);
    return cgcd.isRelated(sub.elementIndex(a), sub.elementIndex(b));
  }

  public static boolean congruenceModularForIdempotent(SmallAlgebra alg) {
System.out.println("got to idempotent");
    if (findDayQuadrupleInSquare(alg, null) != null) return false;
    return true;
/* old code for directly finding the pentagon that is in Freese-Valeriote
    final int n = alg.cardinality();
    final BigProductAlgebra sq = new BigProductAlgebra(alg, 2);
    final IntArray zero = new IntArray(2);
    final IntArray one = new IntArray(2);
    final IntArray t = new IntArray(2);
    final int[] zerovec = zero.toArray();
    final int[] onevec = one.toArray();
    final int[] tvec = t.toArray();
    final List gens = new ArrayList(3);
    gens.add(zero);
    gens.add(one);
    gens.add(t);
    for (int x0 = 0; x0 < n; x0++) {
      for (int x1 = 0; x1 < n; x1++) {
        for (int y0 = 0; y0 < n; y0++) {
          for (int y1 = 0; y1 < n; y1++) {
            zerovec[0] = x0;
            zerovec[1] = x1;
            onevec[0] = x0;
            onevec[1] = y1;
            tvec[0] = y0;
            tvec[1] = y1;
            final SubProductAlgebra sub = new SubProductAlgebra("", sq, gens);
            final BasicPartition beta = sub.con().Cg(zero, one);
            final Partition alpha = (sub.con()).lowerStar(beta);
            if (alpha == null) continue;
            final Partition rho2 = sub.projectionKernel(1);
            if (rho2.join(alpha).isRelated(sub.elementIndex(zero), 
                                          sub.elementIndex(one))) {
              return false;
            }
          }
        }
      }
    }
    return true;
*/
  }


  /**
   * This returns a list of Gumm terms witnessing modularity, or null if 
   * the algebra does generate a congruence modular variety.
   * It is guarenteed to be the least number of terms possible.
   */
  public static List gummTerms(SmallAlgebra alg) {
    return gummTerms(alg, null);
  }
  
  /**
   * This returns a list of Gumm terms witnessing modularity, or null if 
   * the algebra does generate a congruence modular variety.
   * It is guarenteed to be the least number of terms possible.
   */
  public static List<Term> gummTerms(SmallAlgebra alg,  ProgressReport report) {
    return gummTerms(alg, null, report);
  }
  
  /**
   * This returns a list of Gumm terms witnessing modularity, or null if 
   * the algebra does generate a congruence modular variety.
   * It is guarenteed to be the least number of terms possible.
   */
  public static List<Term> gummTerms(SmallAlgebra alg, FreeAlgebra free2, ProgressReport report) { 
    if (report != null) report.addStartLine("Finding Gumm terms.");
    if (alg.isIdempotent()) {
      if (findDayQuadrupleInSquare(alg, report) != null) {
        if (report != null) {
          report.addEndingLine("done finding Gumm terms; there are none");
        }
        return null;
      }
    }
    List<Term> ans = new ArrayList<Term>();
    if (alg.cardinality() == 1) {
      ans.add(Variable.x);
      ans.add(Variable.z);
      return ans;
    }
    FreeAlgebra f2 = free2;
    if (f2 == null) f2 = new FreeAlgebra(alg, 2, report);
    // ** If the tables don't fit in memory, this just doesn't do anything. **
    f2.makeOperationTables();
    logger.info("f2 size is " + f2.cardinality());
    IntArray g0 = new IntArray(new int[] {0,0,1});
    IntArray g1 = new IntArray(new int[] {0,1,0});
    IntArray g2 = new IntArray(new int[] {1,0,0});
    List<IntArray> gens = new ArrayList<IntArray>(3);
    gens.add(g0);
    gens.add(g1);
    gens.add(g2);
    final HashMap<IntArray,Term> termMap = new HashMap<IntArray,Term>(3);
    termMap.put(g0, Variable.x);
    termMap.put(g1, Variable.y);
    termMap.put(g2, Variable.z);
    BigProductAlgebra f2cubed = new BigProductAlgebra(f2, 3);
    
    report.addStartLine("computing the subalgebra of F(2)^3 generated by (x,x,y) (x,y,x), (y,x,x).");
    List sub = f2cubed.sgClose(gens, termMap, null, report);
    report.addEndingLine("done finding subalgebra of F(2)^3.");
    
    
    //logger.info("sub alg of f2 cubed size is " + sub.size());
    final IntArray zero = new IntArray(new int[] {0,0,0});
    if (sub.contains(zero)) {
      //logger.info("this variety has a ternary majority function");
      if (report != null) report.addLine("this variety has a majority term: " + termMap.get(zero));
      ans.add(Variable.x);
      ans.add(termMap.get(zero));
      ans.add(Variable.z);
      return ans;
    }
    List middleZero = new ArrayList();
    for (Iterator it = sub.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      if (ia.get(1) == 0) middleZero.add(ia);
    }
    List firstOne = new ArrayList();
    for (Iterator it = sub.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      if (ia.get(0) == 1) firstOne.add(ia);
    }
    final Comparator c = new Comparator() {
        public int compare(Object o1, Object o2) {
          IntArray ia1 = (IntArray)o1;
          IntArray ia2 = (IntArray)o2;
          for (int i = 0; i < ia1.universeSize(); i++) {
            if (ia1.get(i) < ia2.get(i)) return -1;
            if (ia1.get(i) > ia2.get(i)) return 1;
          }
          return 0;
        }
        public boolean equals(Object o) { return false; }
    };
    Collections.sort(middleZero, c);
    //Collections.sort(firstOne, c);
    for (Iterator it = middleZero.iterator(); it.hasNext(); ) {
      logger.finer("" + it.next());
    }
    final List path = gummLevelPath(middleZero, firstOne, g0, g2);
    if (path == null) {
      if (report != null) report.addEndingLine("this variety is not congruence modular");
      return null;
    }
    for (Iterator it = path.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      ans.add(termMap.get(ia));
    }
    if (report != null) report.addEndingLine("done finding Gumm terms.");
    return ans;
  }

  /**
   * Change this a little:
   * This finds a path from g0 to u, then v. The path from g0 to u is 
   * in <tt>middleZero</tt>, and u and v agree on the third coordinate
   * and v has 1 (the second free generator of F_2) in its first 
   * coordinate. The path from g0 to u is a a list
   * of triples, where two triples are connected by an edge if either
   * their first or third coordinates agree.
   */
  public static List gummLevelPath(List middleZero, List firstOne, 
                                             IntArray g0, IntArray g2) {
    // a list of lists of IntArray's
    final List levels = new ArrayList();
    final Map parentMap = new HashMap();
    List currentLevel = new ArrayList();
    //final HashSet visited = new HashSet();
    currentLevel.add(g0);
    parentMap.put(g0, null);
    levels.add(currentLevel);
    Comparator c0 = new Comparator() {
        public int compare(Object o1, Object o2) {
          return ((IntArray)o2).get(0) - ((IntArray)o1).get(0);
        }
    };
    Comparator c2 = new Comparator() {
        public int compare(Object o1, Object o2) {
          return ((IntArray)o2).get(2) - ((IntArray)o1).get(2);
        }
    };
    HashMap classes0 = new HashMap();
    HashMap classes2 = new HashMap();
    for (Iterator it = middleZero.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      Integer ia_0 = new Integer(ia.get(0));
      Integer ia_2 = new Integer(ia.get(2));
      List eqclass = (List)classes0.get(ia_0);
      if (eqclass == null) {
        eqclass = new ArrayList();
        classes0.put(ia_0, eqclass);
      }
      eqclass.add(ia);
      eqclass = (List)classes2.get(ia_2);
      if (eqclass == null) {
        eqclass = new ArrayList();
        classes2.put(ia_2, eqclass);
      }
      eqclass.add(ia);
    }
    // classes1 is for the firstOne
    HashMap classes1 = new HashMap();
    for (Iterator it = firstOne.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      Integer ia_2 = new Integer(ia.get(2));
      List eqclass = (List)classes1.get(ia_2);
      if (eqclass == null) {
        eqclass = new ArrayList();
        classes1.put(ia_2, eqclass);
      }
      eqclass.add(ia);
    }
    boolean even = false;
    while (true) {
      even = !even;
      final List nextLevel = new ArrayList();
//System.out.println("current level size = " + currentLevel.size());
      for (Iterator it = currentLevel.iterator(); it.hasNext(); ) {
        IntArray ia = (IntArray)it.next();
        List eqclass;
        if (even) eqclass = (List)classes0.get(new Integer(ia.get(0)));
        else eqclass = (List)classes2.get(new Integer(ia.get(2)));
        //eqclass.remove(ia);
        for (Iterator it2 = eqclass.iterator(); it2.hasNext(); ) {
          IntArray ia2 = (IntArray)it2.next();
          //if (ia2.equals(ia)) continue;
          if (!parentMap.containsKey(ia2)) {
            parentMap.put(ia2, ia);
            nextLevel.add(ia2);
          }
          // here
          eqclass = (List)classes1.get(new Integer(ia2.get(2)));
          if (eqclass != null) {
            IntArray p = (IntArray)eqclass.get(0);
            if (eqclass.contains(g2)) p = g2;
            final List path = new ArrayList(levels.size() + 1);
            if (!eqclass.contains(ia2)) path.add(p);
            path.add(ia2);
            ia2 = (IntArray)parentMap.get(ia2);
            while (ia2 != null) {
              path.add(ia2);
              ia2 = (IntArray)parentMap.get(ia2);
            }
            Collections.reverse(path);
            logger.fine("path");
            for (Iterator iter = path.iterator(); iter.hasNext(); ) {
              logger.fine("" + iter.next());
            }
            return path;
          }
        }
      }
      if (nextLevel.isEmpty()) break;    
      levels.add(nextLevel);
      currentLevel = nextLevel;
    }
    return null;
  }

  public static Term malcevTerm(SmallAlgebra alg) {
    return malcevTerm(alg, null);
  }
  
  /**
   * Find a Mal'cev term for (the variety generated by) this algebra or
   * null if there is it is not permutable.
   */
  public static Term malcevTerm(SmallAlgebra alg, ProgressReport report) {
    if (alg.cardinality() == 1)  return Variable.x;
    if (alg.isIdempotent()) {
      if (findDayQuadrupleInSquare(alg, report) != null) {
        if (report != null) {
          report.addLine("there is no Maltsev term");
        }
        return null;
      }
    }
    FreeAlgebra f2 = new FreeAlgebra(alg, 2, report);
    // ** Need to put in a test if the tables will fit in memory. **
    f2.makeOperationTables();
    //logger.info("f2 size is " + f2.cardinality());
    IntArray g0 = new IntArray(new int[] {0,0});
    IntArray g1 = new IntArray(new int[] {0,1});
    IntArray g2 = new IntArray(new int[] {1,1});
    List<IntArray> gens = new ArrayList<IntArray>(3);
    gens.add(g0);
    gens.add(g1);
    gens.add(g2);
    final HashMap<IntArray,Term> termMap = new HashMap<IntArray,Term>(3);
    termMap.put(g0, Variable.x);
    termMap.put(g1, Variable.y);
    termMap.put(g2, Variable.z);
    final IntArray yx = new IntArray(new int[] {1,0});
    BigProductAlgebra f2squared = new BigProductAlgebra(f2, 2);
    // the yx argument means stop if yx is found. 
    // of course we want something different if we want the shortest term.
    report.addStartLine("looking for (y,x) in the subalgebra of F(2)^2 generated by (x,x) (x,y), (y,y).");
    List<IntArray> sub = f2squared.sgClose(gens, termMap, yx, report);
    report.addEndingLine("done finding subalgebra of F(2)^2.");
    //logger.info("sub alg of f2 squared size is " + sub.size());
    if (sub.contains(yx)) return (Term)termMap.get(yx);
    return null;
  }
  
  public static Term majorityTerm(SmallAlgebra alg) {
    return majorityTerm(alg, null);
  }
  
  public static Term majorityTerm(SmallAlgebra alg, ProgressReport report) {
    if (alg.cardinality() == 1)  return Variable.x;
    if (alg.isIdempotent()) {
      if (findDayQuadrupleInSquare(alg, report) != null) {
        if (report != null) {
          report.addLine("there is no majority term");
        }
        return null;
      }
    }
    FreeAlgebra f2 = new FreeAlgebra(alg, 2, report);
    // ** Need to put in a test if the tables will fit in memory. ** 
    f2.makeOperationTables();
    //logger.info("f2 size is " + f2.cardinality());
    IntArray g0 = new IntArray(new int[] {0,0,1});
    IntArray g1 = new IntArray(new int[] {0,1,0});
    IntArray g2 = new IntArray(new int[] {1,0,0});
    List<IntArray> gens = new ArrayList<IntArray>(3);
    gens.add(g0);
    gens.add(g1);
    gens.add(g2);
    final HashMap<IntArray,Term> termMap = new HashMap<IntArray,Term>();
    termMap.put(g0, Variable.x);
    termMap.put(g1, Variable.y);
    termMap.put(g2, Variable.z);
    final IntArray xxx = new IntArray(new int[] {0,0,0});
    BigProductAlgebra f2cubed = new BigProductAlgebra(f2, 3);
    // the yx argument means stop if yx is found. 
    // of course we want something different if we want the shortest term.
    List<IntArray> sub = f2cubed.sgClose(gens, termMap, xxx, report);
    logger.info("sub alg of f2 cubed size is " + sub.size());
    if (sub.contains(xxx)) return (Term)termMap.get(xxx);
    return null;
  }

  
  /**
   * Find a Pixley term for (the variety generated by) this algebra or
   * null if there is it is not arithmetical.
   * The algorithm looks at the subalgebra of F(x,y)^3 generated by
   * (x,x,y), (y,y,y), (y,x,x) and looks for (x,x,x).
   */
  public static Term pixleyTerm(SmallAlgebra alg) {
    return pixleyTerm(alg, null);
  }
  
  /**
   * Find a Pixley term for (the variety generated by) this algebra or
   * null if there is it is not arithmetical.
   * The algorithm looks at the subalgebra of F(x,y)^3 generated by
   * (x,x,y), (y,y,y), (y,x,x) and looks for (x,x,x).
   */
  public static Term pixleyTerm(SmallAlgebra alg, ProgressReport report) {
    if (alg.cardinality() == 1)  return Variable.x;
    if (alg.isIdempotent()) {
      if (findDayQuadrupleInSquare(alg, report) != null) {
        if (report != null) {
          report.addLine("there is no Pixley term");
        }
        return null;
      }
    }
    FreeAlgebra f2 = new FreeAlgebra(alg, 2, report);
    // ** Need to put in a test if the tables will fit in memory. **
    f2.makeOperationTables();
    logger.info("f2 size is " + f2.cardinality());
    IntArray g0 = new IntArray(new int[] {0,0,1});
    IntArray g1 = new IntArray(new int[] {1,1,1});
    IntArray g2 = new IntArray(new int[] {1,0,0});
    List gens = new ArrayList(3);
    gens.add(g0);
    gens.add(g1);
    gens.add(g2);
    final HashMap termMap = new HashMap();
    termMap.put(g0, Variable.x);
    termMap.put(g1, Variable.y);
    termMap.put(g2, Variable.z);
    final IntArray xxx = new IntArray(new int[] {0,0,0});
    BigProductAlgebra f2cubed = new BigProductAlgebra(f2, 3);
    // the yx argument means stop if yx is found. 
    // of course we want something different if we want the shortest term.
    List sub = f2cubed.sgClose(gens, termMap, xxx, report);
    //logger.info("sub alg of f2 cubed size is " + sub.size());
    if (sub.contains(xxx)) return (Term)termMap.get(xxx);
    return null;
  }

  public static List hagemannMitschkeTerms(SmallAlgebra alg) {
    return hagemannMitschkeTerms(alg, null);
  }
  
  /**
   * This returns a list of Hagemann Mitschke Terms terms witnessing 
   * <code>k</code>-permutability, or
   * null if the algebra does generate a congruence 
   * <code>k</code>-permutable variety.
   * It is guarenteed to be the least number of terms possible.
   */
  public static List hagemannMitschkeTerms(SmallAlgebra alg, ProgressReport report) {
    if (report != null) report.addStartLine("finding Hagemann-Mitschke terms.");
    List ans = new ArrayList();
    if (alg.cardinality() == 1) {
      ans.add(Variable.x);
      ans.add(Variable.z);
      return ans;
    }
    FreeAlgebra f2 = new FreeAlgebra(alg, 2, report);
    // ** Need to put in a test if the tables will fit in memory. **
    f2.makeOperationTables();
    logger.info("f2 size is " + f2.cardinality());
    IntArray g0 = new IntArray(new int[] {0,0});
    IntArray g1 = new IntArray(new int[] {0,1});
    IntArray g2 = new IntArray(new int[] {1,1});
    List gens = new ArrayList(3);
    gens.add(g0);
    gens.add(g1);
    gens.add(g2);
    final HashMap termMap = new HashMap(3);
    termMap.put(g0, Variable.x);
    termMap.put(g1, Variable.y);
    termMap.put(g2, Variable.z);
    final IntArray yx = new IntArray(new int[] {1,0});
    BigProductAlgebra f2squared = new BigProductAlgebra(f2, 2);
    // the yx argument means stop if yx is found. 
    // of course we want something different if we want the shortest term.
    if (report != null) report.addStartLine("finding the subalgebra of F(2)^2 generated by (x,x), (x,y), (y,y)");
    List sub = f2squared.sgClose(gens, termMap, yx, report);
    if (report != null) report.addEndingLine("subalgebra size = " + sub.size());
    //logger.info("sub alg of f2 squared size is " + sub.size());
    if (sub.contains(yx)) {
      if (report != null) report.addLine("The variety has a Maltsev term: " + termMap.get(yx));
      //logger.info("this variety has a Malcev term");
      ans.add(Variable.x);
      ans.add(termMap.get(yx));
      ans.add(Variable.z);
      return ans;
    }
    // Not sure we need this. Check if we need to sort this.
    Comparator c = new Comparator() {
        public int compare(Object o1, Object o2) {
          IntArray ia1 = (IntArray)o1;
          IntArray ia2 = (IntArray)o2;
          for (int i = 0; i < ia1.universeSize(); i++) {
            if (ia1.get(i) < ia2.get(i)) return -1;
            if (ia1.get(i) > ia2.get(i)) return 1;
          }
          return 0;
        }
        public boolean equals(Object o) { return false; }
    };
    Collections.sort(sub, c);
    final List path = hagemannMitschkeLevelPath(sub, g0, g2);
    if (path == null) {
      if (report != null) report.addEndingLine("this variety is not K-permutable");
      return null;
    }
    for (Iterator it = path.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      ans.add(termMap.get(ia));
    }
    if (report != null) report.addEndingLine("Found the Hagemann-Mitschke terms.");
    return ans;
  }

  /**
   * This finds a path from g0 to g2 in <tt>middleZero</tt>, a list
   * of triples, where two triples are connected by an edge if either
   * their first or third coordinates agree.
   */
  public static List hagemannMitschkeLevelPath(List sub, IntArray g0, 
                                                         IntArray g2) {
    // a list of lists of IntArray's
    final List levels = new ArrayList();
    final Map parentMap = new HashMap();
    List currentLevel = new ArrayList();
    //final HashSet visited = new HashSet();
    currentLevel.add(g0);
    parentMap.put(g0, null);
    levels.add(currentLevel);
    Comparator c0 = new Comparator() {
        public int compare(Object o1, Object o2) {
          return ((IntArray)o2).get(0) - ((IntArray)o1).get(0);
        }
    };
    Comparator c2 = new Comparator() {
        public int compare(Object o1, Object o2) {
          return ((IntArray)o2).get(1) - ((IntArray)o1).get(1);
        }
    };
    HashMap classes0 = new HashMap();
    HashMap classes1 = new HashMap();
    for (Iterator it = sub.iterator(); it.hasNext(); ) {
      IntArray ia = (IntArray)it.next();
      Integer ia_0 = new Integer(ia.get(0));
      Integer ia_1 = new Integer(ia.get(1));
      List eqclass = (List)classes0.get(ia_0);
      if (eqclass == null) {
        eqclass = new ArrayList();
        classes0.put(ia_0, eqclass);
      }
      eqclass.add(ia);
      eqclass = (List)classes1.get(ia_1);
      if (eqclass == null) {
        eqclass = new ArrayList();
        classes1.put(ia_1, eqclass);
      }
      eqclass.add(ia);
    } 
    while (true) {
      final List nextLevel = new ArrayList();
//System.out.println("current level size = " + currentLevel.size());
      for (Iterator it = currentLevel.iterator(); it.hasNext(); ) {
        IntArray ia = (IntArray)it.next();
        List eqclass;
        eqclass = (List)classes1.get(new Integer(ia.get(0)));
        for (Iterator it2 = eqclass.iterator(); it2.hasNext(); ) {
          IntArray ia2 = (IntArray)it2.next();
          if (!parentMap.containsKey(ia2)) {
            parentMap.put(ia2, ia);
            nextLevel.add(ia2);
          }
          if (ia2.equals(g2)) {
            final List path = new ArrayList(levels.size() + 1);
            path.add(g2);
            ia2 = (IntArray)parentMap.get(ia2);
            while (ia2 != null) {
              path.add(ia2);
              ia2 = (IntArray)parentMap.get(ia2);
            }
            Collections.reverse(path);
            logger.fine("Hagemann Mitschke path");
            for (Iterator iter = path.iterator(); iter.hasNext(); ) {
              logger.fine("" + iter.next());
            }
            return path;
          }
        }
      }
      if (nextLevel.isEmpty()) break;    
      levels.add(nextLevel);
      currentLevel = nextLevel;
    }
    return null;
  }
  
  /**
   * This gives unary terms evaluating to the characteristic
   * functions of the one element subsets of alg; a term which
   * applied to these unit vectors gives the identity function; and
   * a binary term giving a semilattice operation on {0, 1}.
   * 
   * @param alg
   * @param report
   * @return
   */
  public static List<Term> primalityTerms(SmallAlgebra alg, ProgressReport report) {
    Term semilatTerm = semilatTerm(alg, report);
    if (semilatTerm == null) return null;
    System.out.println("semilat term: " + semilatTerm);
    Term idTerm = idTerm(alg, report);
    if (idTerm == null) return null;
    System.out.println("id term: " + idTerm);
    List<Term> unitTerms = unitTerms(alg, report);
    if (unitTerms == null) return null;
    System.out.println("unit terms: " + unitTerms);
    List<Term> ans = new ArrayList<Term>(alg.cardinality() + 2);
    ans.add(semilatTerm);
    ans.add(idTerm);
    for (Term term : unitTerms) {
      ans.add(term);
    }
    return ans;
  }
  
  /**
   * Gives a binary term of alg that is a semilattice meet on 0 and 1 
   * or null if there is none.
   *  
   * @param alg
   * @param report
   * @return
   */
  private static Term semilatTerm(SmallAlgebra alg, ProgressReport report) {
    report.addStartLine("Looking for [0,0,0,1] (the meet operation) in A^4 generated by [0,0,1,1] and [0,1,0,1]."); 
    final BigProductAlgebra alg4 = new BigProductAlgebra(alg, 4);
    IntArray meet = new IntArray(new int[] {0,0,0,1});
    List<IntArray> gens = new ArrayList<IntArray>(2);
    gens.add(new IntArray(new int[] {0,0,1,1}));
    gens.add(new IntArray(new int[] {0,1,0,1}));
    Map<IntArray,Term> termMap = new HashMap<IntArray,Term>();
    termMap.put(gens.get(0), Variable.x);
    termMap.put(gens.get(1), Variable.y);
    Closer closer = new Closer(alg4, gens, termMap);
    closer.setProgressReport(report);
    closer.setElementToFind(meet);
    List<IntArray> lst = closer.sgClose();
    if (termMap.get(meet) == null) report.addEndingLine("Meet not found; the algebra is not primal.");
    else report.addEndingLine("A term for the meet is " + termMap.get(meet));
    return termMap.get(meet);
  }
  
  private static Term idTerm(SmallAlgebra alg, ProgressReport report) {
    final int n = alg.cardinality();
    report.addStartLine("Looking for the identity function on A in the subalgebra of A^" 
        + n + " generated by the unit vectors.");
    final BigProductAlgebra prod = new BigProductAlgebra(alg, n);
    List<IntArray> gens = new ArrayList<IntArray>(n);
    Map<IntArray,Term> termMap = new HashMap<IntArray,Term>();
    List<IntArray> units = unitVectors(n);
    final int[] idArr = new int[n];
    for (int i = 0; i < n; i++) {
      gens.add(units.get(i));
      termMap.put(units.get(i), new VariableImp("x_" + i));
      idArr[i] = i;
    }
    final IntArray id = new IntArray(idArr);
    Closer closer = new Closer(prod, gens, termMap);
    closer.setProgressReport(report);
    closer.setElementToFind(id);
    List<IntArray> lst = closer.sgClose();
    if (termMap.get(id) == null) report.addEndingLine("Id not found; the algebra is not primal.");
    else report.addEndingLine("A term for the Id is " + termMap.get(id));
    return termMap.get(id);
  }
  
  private static List<IntArray> unitVectors(int n) {
    List<IntArray> ans = new ArrayList<IntArray>(n);
    for (int i = 0; i < n; i++) {
      final int[] arr = new int[n];
      arr[i] = 1;
      final IntArray ia = new IntArray(arr);
      ans.add(ia);
    }
    return ans;
  }
  
  private static List<Term> unitTerms(SmallAlgebra alg, ProgressReport report) {
    final int n = alg.cardinality();
    report.addStartLine("Looking for terms giving the " + n + " units vectors in F(1).");
    List<IntArray> units = unitVectors(n);
    FreeAlgebra F = new FreeAlgebra(alg, 1, false, false, false, null, report);
    Closer closer = new Closer(F.getProductAlgebra(), F.generators(), F.getTermMap());
    closer.setProgressReport(report);
    closer.setElementsToFind(units, F.generators());
    closer.sgClose();
    if (!closer.allElementsFound()) {
      report.addEndingLine("Not all units were found. The algebra is not primal.");
      return null;
    }
    List<Term> ans = new ArrayList<Term>(n);
    for (IntArray ia : units) {
      ans.add(closer.getTermMap().get(ia));
    }
    report.addEndingLine("All units were found.");
    return ans;
  }


  public static void main(String[] args) {
    SmallAlgebra alg = null;
    try {
      if (args.length > 0) 
        alg = org.uacalc.io.AlgebraIO.readAlgebraFile(args[0]);
      else 
        alg = org.uacalc.io.AlgebraIO.readAlgebraFile(
            //"/home/ralph/Java/Algebra/algebras/wm3.ua"
            //"/home/ralph/Java/Algebra/algebras/n5.ua"
            //"/home/ralph/Java/Algebra/algebras/forkprime.ua"
            //"/home/ralph/Java/Algebra/algebras/directoidNonCom7.ua"  /////////////////////
            //"/home/ralph/Java/Algebra/algebras/FivePaper.ua"
            //"/home/ralph/Java/Algebra/algebras/linjon4.ua"  // has a wnu term with x \circ y = y \circ x
            "/home/ralph/Java/Algebra/algebras/linjon3.ua" // has no wnu term with x \circ y = y \circ x
                                                           // in fact x <-> y in F(x,y) has no fixed point 
            );
    }
    catch (Exception e) { 
      e.printStackTrace();
      return;
    }
    //Term term = markovicMcKenzieSiggersTaylorTerm(alg, null);
    //System.out.println("term = " + term);
    List<Term> sdm = sdmeetTerms(alg, null);
    System.out.println("terms = " + sdm);
    
    //IntArray ia = findDayQuadrupleInSquare(alg, null);
    //System.out.println("day quad is " + ia);
    //System.out.println("congr mod var = " + congruenceModularVariety(alg));
    //List<Term> foo = primalityTerms(alg, null);
    //List<Term> foo = jonssonTerms(alg);
    //System.out.println("terms are " + foo);
    /*
    if (args.length == 0) return;
    SmallAlgebra alg0 = null;
    int arity = 3;
    try {
      alg0 = (SmallAlgebra)org.uacalc.io.AlgebraIO.readAlgebraFile(args[0]);
      if (args.length > 1) {
        arity = Integer.parseInt(args[1]);
      }
    }
    catch (Exception e) {}
    //int level = Malcev.jonssonLevel(alg0);
    //System.out.println("level is " + level);

    //Term t = findNUF(alg0, arity);
    //if (t == null) System.out.println("there is no NUF with arity " + arity);
    //else System.out.println("the alg has a NUF of arity " + arity + ": " + t);

    //IntArray ia = findDayQuadrupleInSquare(alg0);
    //System.out.println("day quad is " + ia);
    System.out.println("congr mod var = " + congruenceModularVariety(alg0));
    */
  }



}







