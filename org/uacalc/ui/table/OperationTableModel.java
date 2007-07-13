package org.uacalc.ui.table;

import javax.swing.table.AbstractTableModel;
import java.lang.System;
import org.uacalc.alg.Operation;
import org.uacalc.alg.OperationSymbol;
import org.uacalc.alg.Operations;
import org.uacalc.util.Horner;
import org.uacalc.util.SequenceGenerator;
import org.uacalc.util.ArrayIncrementor;
import org.uacalc.util.ArrayString;

public class OperationTableModel extends AbstractTableModel {

  Operation op = null;
  OperationSymbol opSym;
  
  int defaultValue = -1;
  int[] valueTable;
  int[] diagIndices;
  int[][] leftArgsTable;// all but the last arg
  String[] rowNames;
  int arity;
  int setSize;

  static final String x = "x";
  static final String y = "y";
  static final String z = "z";
  
  public OperationTableModel(Operation op) {
    this.op = op;
    this.opSym = op.symbol();
  }
  
  public OperationTableModel(int arity, int setSize) {
    this(OperationSymbol.getOperationSymbol(arity), setSize, false, -1);
    //this.op = makeUndefinedOp(OperationSymbol.getOperationSymbol(arity), 
    //                                                     setSize, -1, false);
  }
  
  public OperationTableModel(int arity, int setSize, boolean idempotent, 
                                                         int defaultValue) {
    this(OperationSymbol.getOperationSymbol(arity), setSize, 
                                                    idempotent, defaultValue);
  }
  
  public OperationTableModel (OperationSymbol sym, int setSize,
                              boolean idempotent, int defaultValue) {
    this.defaultValue = defaultValue;
    this.setSize = setSize;
    this.arity = sym.arity();
    op = makeUndefinedOp(sym, setSize, idempotent);
    setRowNames();
  }
  /**
   * Make an int operation which returns the defaultValue. 
   * It might be the invalid value -1.
   * Also set up leftArgsTable.
   * 
   * @param sym
   * @param setSize
   * @return
   */
  Operation makeUndefinedOp(OperationSymbol sym, int setSize, 
                                                 boolean idempotent) {
    final int arity = sym.arity();
    int n = 1;
    for (int k = 0; k < arity; k++) {
      n = n * setSize;
    }
    final int k = n / setSize;
    valueTable = new int[n];
    leftArgsTable = new int[k][];
    final int[] seq = new int[arity - 1];
    for (int i = 0; i < arity - 1; i++) {
      seq[i] = 0;
    }
    rowNames = new String[k];
    ArrayIncrementor inc 
             = SequenceGenerator.sequenceIncrementor(seq, setSize - 1);
    for (int j = 0 ; j < k; j++) {
      int[] dest = new int[arity];
      System.arraycopy(seq, 0, dest, 0, arity - 1);
      leftArgsTable[j] = dest;
      //setColName(k, dest);
      inc.increment();
    }
    for (int i = 0; i < n; i++) {
      valueTable[i] = defaultValue;
    }
    if (idempotent) makeIdempotent();
    return Operations.makeIntOperation(sym, setSize, valueTable);
  }
  
  public void makeIdempotent() {
    if (diagIndices == null) {
      diagIndices = new int[setSize];
      for (int i = 0; i < setSize; i++) {
        final int[] diag = new int[arity];
        for (int j = 0; j < arity; j++) {
          diag[j] = i;
        }
        diagIndices[i] = Horner.horner(diag, setSize);
      }
    }
    for (int i = 0; i < setSize; i++) {
      valueTable[diagIndices[i]] = i;
    }
  }
  
  public int getDefaultValue() {
    return defaultValue;
  }
  
  public void setDefaultValue(int v) {
    defaultValue = v;
  }

  public String getLastVariable() {
    if (op.arity() == 1) return x;
    if (op.arity() == 2) return y;
    return z;
  }

  public void setRowNames() {
    for (int i = 0; i < leftArgsTable.length; i++) {
      setRowName(i, leftArgsTable[i]);
    }
  }
  
  public void setRowName(int k, int[] arg) {
    final String left = "(";
    final String right = ")";
    final String comma = ",";
    String free = getLastVariable();
    StringBuffer sb = new StringBuffer(op.symbol().name());
    sb.append(left);
    for (int i = 0; i < arg.length - 1; i++) {
      sb.append(arg[i]);
      sb.append(comma);
    }
    sb.append(free);
    sb.append(right);
    rowNames[k] = sb.toString();
  }
  
  public OperationSymbol getOperationSymbol() { return opSym; }
  
  public int[] getValueTable() { return valueTable; }
  
  public Operation getOperation() {
    return op;
  }
  
  /**
   * The column of index 0 is a vector of all but the last arg.
   */
  public int getColumnCount() {
    return op.getSetSize() + 1;
  }

  public int getRowCount() {
    return leftArgsTable.length;
  }

  public String getColumnName(int col) {
    if (col == 0) return getLastVariable();
    return "" + (col -1);
  }
  
  public Class getColumnClass(int col) {
    if (col == 0) return String.class;
    return Integer.class;
  }
  
  public boolean isCellEditable(int row, int col) {
    if (col == 0) return false;
    return true;
  }
  
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (columnIndex == 0) {
      return rowNames[rowIndex];
    }
    int val = op.intValueAt(rowColToArg(rowIndex, columnIndex));
    if (val == -1) {
      if (defaultValue == -1) return null;
      return defaultValue;
    }
    if (val >= setSize) return null;
    return val;
  }

  public void setValueAt(Object val, int row, int col) {
    if (val == null) return;
    if (col == 0) return;
    int value = ((Integer)val).intValue();
    if (value < 0 || value >= setSize) return;  // issue a warning
    valueTable[Horner.horner(rowColToArg(row, col), op.getSetSize())] = value;
  }
  
  private int[] rowColToArg(int row, int col) {
    if (col == 0) return null;
    int[] left = leftArgsTable[row];
    int[] arg = new int[op.arity()];
    System.arraycopy(left, 0, arg, 0, left.length);
    arg[arg.length - 1] = col - 1;
//System.out.println("row: " + row + " col: " + col);
//System.out.println("left: " + ArrayString.toString(left));
//System.out.println("arg: " + ArrayString.toString(arg));
//System.out.println("op.getSetSize(): " + op.getSetSize());
//System.out.println("op.arity(): " + op.arity());
    //return Horner.leftRightReverse(arg, op.getSetSize(), op.arity());
    return arg;
  }
}




