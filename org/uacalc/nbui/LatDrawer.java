package org.uacalc.nbui;

import java.util.*;
import org.latdraw.orderedset.*;
import org.latdraw.diagram.*;
import org.latdraw.beans.*;

import org.uacalc.lat.*;
import org.uacalc.alg.*;
import org.uacalc.alg.sublat.*;
import org.uacalc.alg.conlat.*;

import javax.swing.*;
import java.awt.Color;
import java.awt.event.*;
import java.awt.Dimension;
import java.awt.BorderLayout;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;


public class LatDrawer extends JPanel {
  
  public static enum RadioButtonType {
    OFF, JI, MI, UC, LC, ID, FIL, JDECOMP, MDECOMP
  }
  
  private RadioButtonType aboveType = RadioButtonType.OFF;
  private RadioButtonType belowType= RadioButtonType.OFF;

  //public static final Color BELOW_COLOR = new Color(204, 255, 204); //light green
  //public static final Color ABOVE_COLOR = new Color(204, 204, 255); //light blue
 
  public static final Color BELOW_COLOR = Color.PINK;
  public static final Color ABOVE_COLOR = Color.CYAN;
  public static final Color BOTH_COLOR = Color.WHITE;
  public static final Color SELECTED_LIST_COLOR = Color.GREEN;
  public static final Color GENERATED_ELEMS_COLOR = Color.WHITE;


  private org.latdraw.beans.DrawPanel drawPanel;

  private JPanel mainPanel;
  //private JPanel appPanel;
  private JToolBar toolBar;

  private UACalc uacalc;
  
  private BasicLattice lattice;
  
  private Vertex selectedElem;
  
  private List<Vertex> selectedElemList;
  
  //private static final Dimension scrollDim = new Dimension(200, 250);

// was ConceptLattice, leave it in case we need it as an example.
  public LatDrawer(UACalc uacalc) {
    this.uacalc = uacalc;
    drawPanel = new org.latdraw.beans.DrawPanel();
    PropertyChangeListener changeListener = new PropertyChangeListener() {
        public void propertyChange(PropertyChangeEvent e) {
          if (e.getPropertyName().equals(ChangeSupport.VERTEX_PRESSED)) {
            Vertex v = (Vertex)e.getNewValue();
            setSelectedElem(v);
            System.out.println("v: " + v);
            return;
          }
          if (e.getPropertyName().equals(ChangeSupport.VERTEX_PRESSED_CTRL)) {
            Vertex v = (Vertex)e.getNewValue();
            Vertex sel = getSelectedElem();
            if (sel != null && !v.equals(sel)) {
              Diagram diag = drawPanel.getDiagram();
              addToSelectedElemList(v);
              repaint();
              return;
            }  
          }
          if (e.getPropertyName().equals(ChangeSupport.VERTEX_PRESSED_SHIFT)) {
            Vertex v = (Vertex)e.getNewValue();
            Vertex sel = getSelectedElem();
            if (sel != null && !v.equals(sel)) {
              Diagram diag = drawPanel.getDiagram();
              if (diag.leq(v, sel)) {
                resetSelectedElemList();
                resetVertexColors();
                for (Vertex vert : diag.ideal(sel)) {
                  if (diag.leq(v, vert) && !vert.equals(sel)) {
                    addToSelectedElemList(vert);
                  }
                }
                repaint();
                return;
              }
              if (diag.leq(sel, v)) {
                resetSelectedElemList();
                resetVertexColors();
                for (Vertex vert : diag.ideal(v)) {
                  if (diag.leq(sel, vert) && !vert.equals(sel)) {
                    addToSelectedElemList(vert);
                  }
                }
                repaint();
                return;
              }
              // incomparable case
              resetSelectedElemList();
              resetVertexColors();
            }
          }

          if (e.getPropertyName().equals(ChangeSupport.VERTEX_MIDDLE_PRESSED)) {
            Vertex v = (Vertex)e.getNewValue();
            Diagram diag = drawPanel.getDiagram();
            Set<Vertex> drawnVerts = new HashSet<Vertex>();
            if (getSelectedElem() == null) setSelectedElem(v);
            Vertex sel = getSelectedElem();
            System.out.println("sel equals v is " + sel.equals(v));
            System.out.println("(under elem) sel equals v is " 
                + sel.getUnderlyingElem().equals(v.getUnderlyingElem()));
            if (v.equals(getSelectedElem())) {
              List<Vertex> fil = diag.filter(v);
              List<Vertex> idl = diag.ideal(v);
              for (Vertex vert : fil) {
                drawnVerts.add(vert);
              }
              for (Vertex vert : idl) {
                drawnVerts.add(vert);
              }
              drawPanel.setAllowedVertices(drawnVerts);
              repaint();
              return;
            }
            if (diag.leq(sel, v)) {
              for (Vertex vert : diag.ideal(v)) {
                if (diag.leq(sel, vert)) drawnVerts.add(vert);
              }
              drawPanel.setAllowedVertices(drawnVerts);
              repaint();
              return;
            }
            if (diag.leq(v, sel)) {
              for (Vertex vert : diag.ideal(sel)) {
                if (diag.leq(v, vert)) drawnVerts.add(vert);
              }
              drawPanel.setAllowedVertices(drawnVerts);
              repaint();
              return;
            }
          }
          if (e.getPropertyName().equals(ChangeSupport.NOTHING_PRESSED)) {
            setSelectedElem(null);
            drawPanel.setAllowedVertices(null);
            
            //diag.resetVertices();
            //setSelectedElem(null);
            //resetVertexColors();
            //repaint();
            return;
          }
        }
    };
    drawPanel.getChangeSupport().addPropertyChangeListener(changeListener);
    drawPanel.setFocusable(true);
    drawPanel.addMouseListener(new MouseAdapter() {
        public void mouseEntered(MouseEvent e) {
          requestFocus();
        }
      });
    addKeyListener(new KeyAdapter() {
        public void keyTyped(KeyEvent e) {
          if (drawPanel.getDiagram() == null) return;
          char key = e.getKeyChar();
          if (key == 'r' || key == 'R') {
            if (drawPanel.isRotating()) drawPanel.stopRotation();
            else drawPanel.startRotation();
          }
          //if (key == 's' || key == 'S') drawPanel.stopRotation();
          if (key == 's' || key == 'S') showSublattice();
          if (key == 'c') showClosure(false);
          if (key == 'C') showClosure(true);
          if (key == '.' || key == '>') drawPanel.rotateOnce();
          if (key == ',' || key == '<') drawPanel.rotateLeft();
          if (key == 'd' || key == 'D') drawPanel.setDraggingAllowed(
                                          !drawPanel.isDraggingAllowed());
          if (key == 'h' || key == 'H') drawPanel.setDraggingHorizontal(
                                          !drawPanel.isDraggingHorizontal());
          if (key == 'i') drawPanel.improveWithDelay(40);
          if (key == 'I') drawPanel.improve();
          if (key == 'p') drawPanel.printDialog();
          if (key == 'P') drawPanel.writeRSFDiagram("outx.ps");
          if (key == '+') {
            drawPanel.increaseReplusion();
            drawPanel.decreaseAttraction();
          }
          if (key == '-') {
            drawPanel.increaseAttraction();
            drawPanel.decreaseReplusion();
          }
        }
      });


    setLayout(new BorderLayout());
    ButtonGroup labelGroup = new ButtonGroup();
    JRadioButtonMenuItem noLabel = new JRadioButtonMenuItem("No");
    JRadioButtonMenuItem yesLabel = new JRadioButtonMenuItem("Yes", true);
    JRadioButtonMenuItem numsLabel = new JRadioButtonMenuItem("Nums");
    JMenuBar mb = new JMenuBar();
    JMenu labelsMenu = mb.add(new JMenu("Labels"));
    labelsMenu.add(noLabel);
    labelsMenu.add(yesLabel);
    labelsMenu.add(numsLabel);
    labelGroup.add(noLabel);
    labelGroup.add(yesLabel);
    labelGroup.add(numsLabel);
    noLabel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (drawPanel.getDiagram() != null) {
          drawPanel.getDiagram().setPaintLabels(false);
          repaint();
        }
      }
    });
    yesLabel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (getDiagram() != null) {
          setLabels(false);
          getDiagram().setPaintLabels(true);
          repaint();
        }
      }
    });
    numsLabel.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (getDiagram() != null) {
          setLabels(true);
          getDiagram().setPaintLabels(true);
          repaint();
        }
      }
    });
    
    ButtonGroup dragGroup = new ButtonGroup();
    JRadioButtonMenuItem noDrag = new JRadioButtonMenuItem("No");
    JRadioButtonMenuItem horizDrag = new JRadioButtonMenuItem("Horiz");
    JRadioButtonMenuItem allDrag = new JRadioButtonMenuItem("All", true);
    JMenu dragMenu = mb.add(new JMenu("Dragging"));
    dragMenu.add(noDrag);
    dragMenu.add(horizDrag);
    dragMenu.add(allDrag);
    dragGroup.add(noDrag);
    dragGroup.add(horizDrag);
    dragGroup.add(allDrag);
    noDrag.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        drawPanel.setDraggingAllowed(false);
      }
    });
    horizDrag.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        drawPanel.setDraggingAllowed(true);
        drawPanel.setDraggingHorizontal(true);
      }
    });
    allDrag.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        drawPanel.setDraggingAllowed(true);
        drawPanel.setDraggingHorizontal(false);
      }
    });

    ButtonGroup speedGroup = new ButtonGroup();
    JRadioButtonMenuItem fast = new JRadioButtonMenuItem("Fast", true);
    JRadioButtonMenuItem medium = new JRadioButtonMenuItem("Medium");
    JRadioButtonMenuItem slow = new JRadioButtonMenuItem("Slow");
    JMenu speedMenu = mb.add(new JMenu("Improve Speed"));
    speedMenu.add(fast);
    speedMenu.add(medium);
    speedMenu.add(slow);
    speedGroup.add(fast);
    speedGroup.add(medium);
    speedGroup.add(slow);
    drawPanel.setUseImproveDelay(false);
    fast.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        drawPanel.setUseImproveDelay(false);
      }
    });
    medium.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        drawPanel.setUseImproveDelay(true);
        drawPanel.setImproveDelay(50);
      }
    });
    slow.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        drawPanel.setUseImproveDelay(true);
        drawPanel.setImproveDelay(50);
      }
    });
    
    toolBar = makeToolBar();
    //appPanel = new JPanel();
    //appPanel.setLayout(new BorderLayout());
    mainPanel = new JPanel();
    mainPanel.setLayout(new BorderLayout());
    mainPanel.add(drawPanel, BorderLayout.CENTER);
    //mb.add(new JMenu("FOO"));
    JPanel top = new JPanel();
    top.setLayout(new BorderLayout());
    top.add(mb, BorderLayout.NORTH);
    top.add(toolBar, BorderLayout.CENTER);
    add(top, BorderLayout.NORTH);
    //add(mb, BorderLayout.NORTH);
    //add(toolBar, BorderLayout.NORTH);
    add(mainPanel, BorderLayout.CENTER);
    validate();
    repaint();
  }
  
 
  
  

  public UACalc getUACalc() { return uacalc; }

  public Diagram getDiagram() { return drawPanel.getDiagram(); }

  public void setDiagram(Diagram d) { 
    if (true || d != null) { // fix this !!!!!!!!!!!
      drawPanel.setDiagram(d);
      //drawPanel = new DrawPanel(d);
      repaint();
    }
  }

  public BasicLattice getBasicLattice() { return lattice; }
  
  public void setBasicLattice(BasicLattice lat) {
    drawPanel.setAllowedVertices(null); 
    lattice = lat;
    if (lat != null) setDiagram(lat.getDiagram());
    else setDiagram(null);
  }
  
  public org.latdraw.beans.DrawPanel getDrawPanel() { return drawPanel; }

  public JToolBar getToolBar() { return toolBar; }

  private Set<Vertex> getSublattice(List<Vertex> gens) {
    Set<Vertex> ans = new HashSet<Vertex>();
    final int[] arr = new int[gens.size()];
    for (int k = 0; k < arr.length; k++) {
      arr[k] = gens.get(k).getUnderlyingElem().index();
    }
    int[] sub = getBasicLattice().sub().sg(arr).getArray();
    for (int k = 0; k < sub.length; k++) {
      ans.add(getDiagram().getVertices()[sub[k]]);
    }
    return ans;
  }
  
  private Set<Vertex> getClosure(List<Vertex> gens, boolean keepAlg) {
    Set<Vertex> ans = new HashSet<Vertex>();
    if (gens == null || gens.size() == 0) return ans;
    Object foo = gens.get(0).getUnderlyingObject();
    if (!(foo instanceof BasicPartition)) return getSublattice(gens);
    List<Partition> pars = new ArrayList<Partition>(gens.size());
    for (Vertex v : gens) {
      pars.add((Partition)v.getUnderlyingObject());
    }
    SmallAlgebra alg = BasicPartition.unaryCloneAlgebra(pars);
    if (keepAlg) {
      alg.convertToDefaultValueOps();
      uacalc.getMainController().addAlgebra(alg, false);
    }
    Set<Partition> closure = alg.con().universe();
    for (Vertex v : getDiagram().getVertices()) {
      Partition par = (Partition)v.getUnderlyingObject();
      if (closure.contains(par)) ans.add(v);
    }
    return ans;
  }
  
  public void showSublattice() {
    if (getSelectedElemList() != null) {
      List<Vertex> elems = new ArrayList<Vertex>(getSelectedElemList());
      elems.add(getSelectedElem());
      Set<Vertex> sub = getSublattice(elems);
      Set<Vertex> elemsSet = new HashSet<Vertex>(elems);
      for (Vertex v : sub) {
        if (!elemsSet.contains(v)) {
          v.setColor(GENERATED_ELEMS_COLOR);
          v.setFilled(true);
        }
      }
      repaint();
      System.out.println(sub.size());
    }
  }
  
  /**
   * In Congruence panel this will show the closure (all congruences
   * respected by all operations respecting the given ones; just the 
   * sublattice generated otherwise.
   */
  public void showClosure(boolean keepAlg) {
    if (getSelectedElemList() != null) {
      List<Vertex> elems = new ArrayList<Vertex>(getSelectedElemList());
      elems.add(getSelectedElem());
      Set<Vertex> sub = getClosure(elems, keepAlg);
      Set<Vertex> elemsSet = new HashSet<Vertex>(elems);
      for (Vertex v : sub) {
        if (!elemsSet.contains(v)) {
          v.setColor(GENERATED_ELEMS_COLOR);
          v.setFilled(true);
        }
      }
      repaint();
      System.out.println(sub.size());
    }
  }
  
/*
  public void paint(java.awt.Graphics g) {
    if (drawPanel.getDiagram() != null) super.paint(g);
    toolBar.repaint();
  }
*/

/*
  public void setContent(DrawPanel dp, JToolBar tb) {
    drawPanel = dp;
    //conceptLattice = cl;
    JPanel rightPanel = new JPanel();
    rightPanel.setLayout(new BorderLayout());
    rightPanel.add(tb, BorderLayout.NORTH);
    rightPanel.add(drawPanel, BorderLayout.CENTER);
    mainPanel.add(rightPanel, BorderLayout.CENTER);
    validate();
  }
*/

// add what we need (like the dragging menu) to the tool bar
/*
  private void buildMenuBar() {
    JMenuBar menuBar = new JMenuBar();

    // the file menu
    JMenu file = (JMenu) menuBar.add(new JMenu("File"));
    file.setMnemonic(KeyEvent.VK_F);

    ClassLoader cl = this.getClass().getClassLoader();
    ImageIcon icon = new ImageIcon(cl.getResource(
                              "org/latdraw/sample/images/Open16.gif"));

    JMenuItem openMI = (JMenuItem)file.add(new JMenuItem("Open", icon));
    openMI.setMnemonic(KeyEvent.VK_O);
    KeyStroke cntrlO = KeyStroke.getKeyStroke(KeyEvent.VK_O,Event.CTRL_MASK);
    openMI.setAccelerator(cntrlO);

    file.add(new JSeparator());

    JMenuItem exitMI = (JMenuItem)file.add(new JMenuItem("Exit"));
    exitMI.setMnemonic(KeyEvent.VK_X);
    KeyStroke cntrlQ = KeyStroke.getKeyStroke(KeyEvent.VK_Q,Event.CTRL_MASK);
    exitMI.setAccelerator(cntrlQ);

    openMI.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          try {
            open();
          }
          catch (IOException err) {
            err.printStackTrace();
          }
        }
      });

    exitMI.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.exit(0);
        }
      });

    JMenu dragging = (JMenu)menuBar.add(new JMenu("Dragging"));
    ButtonGroup draggingGroup = new ButtonGroup();
    JRadioButtonMenuItem noDrag = new JRadioButtonMenuItem("None");
    JRadioButtonMenuItem horizDrag = new JRadioButtonMenuItem("Horizontal");
    JRadioButtonMenuItem allDrag = new JRadioButtonMenuItem("All");
    noDrag.setMnemonic(KeyEvent.VK_N);
    horizDrag.setMnemonic(KeyEvent.VK_H);
    allDrag.setMnemonic(KeyEvent.VK_A);
    KeyStroke key = 
        KeyStroke.getKeyStroke(KeyEvent.VK_N,Event.SHIFT_MASK+Event.CTRL_MASK);
    noDrag.setAccelerator(key);
    key=KeyStroke.getKeyStroke(KeyEvent.VK_H,Event.SHIFT_MASK+Event.CTRL_MASK);
    horizDrag.setAccelerator(key);
    key=KeyStroke.getKeyStroke(KeyEvent.VK_A,Event.SHIFT_MASK+Event.CTRL_MASK);
    allDrag.setAccelerator(key);
    noDrag.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          drawPanel.setDraggingAllowed(false);
        }
    });
    horizDrag.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          drawPanel.setDraggingAllowed(true);
          drawPanel.setDraggingHorizontal(true);
        }
    });
    allDrag.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          drawPanel.setDraggingAllowed(true);
          drawPanel.setDraggingHorizontal(false);
        }
    });
    // start with horizontal only
    horizDrag.setSelected(true);
    drawPanel.setDraggingAllowed(true);
    drawPanel.setDraggingHorizontal(true);
    draggingGroup.add(noDrag);
    dragging.add(noDrag);
    draggingGroup.add(horizDrag);
    dragging.add(horizDrag);
    draggingGroup.add(allDrag);
    dragging.add(allDrag);

    JMenu labels = (JMenu)menuBar.add(new JMenu("Labeling"));
    ButtonGroup labelsGroup = new ButtonGroup();
    JRadioButtonMenuItem noLabels = new JRadioButtonMenuItem("No");
    JRadioButtonMenuItem someLabels = new JRadioButtonMenuItem("Yes");
    //JRadioButtonMenuItem allLabels = new JRadioButtonMenuItem("All");
    //noLabels.setMnemonic(KeyEvent.VK_N);
    //someLabels.setMnemonic(KeyEvent.VK_H);
    //allLabels.setMnemonic(KeyEvent.VK_A);
    //KeyStroke key = 
      //KeyStroke.getKeyStroke(KeyEvent.VK_N,Event.SHIFT_MASK+Event.CTRL_MASK);
    //noDrag.setAccelerator(key);
    //key=KeyStroke.getKeyStroke(KeyEvent.VK_H,Event.SHIFT_MASK+Event.CTRL_MASK);
    //horizDrag.setAccelerator(key);
    //key=KeyStroke.getKeyStroke(KeyEvent.VK_A,Event.SHIFT_MASK+Event.CTRL_MASK);
    //allDrag.setAccelerator(key);
    noLabels.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          drawPanel.getDiagram().setPaintLabels(false);
          repaint();
        }
    });
    someLabels.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          drawPanel.getDiagram().setPaintLabels(true);
          repaint();
        }
    });
    // start without labels
    noLabels.setSelected(true);
    drawPanel.getDiagram().setPaintLabels(false);
    labelsGroup.add(noLabels);
    labels.add(noLabels);
    labelsGroup.add(someLabels);
    labels.add(someLabels);
    //labelsGroup.add(allLabels);
    //labels.add(allLabels);

    setJMenuBar(menuBar);
  }
*/

  // Will probably use a JPopupMenu instead
  private String getRightClickOption(String[] opts, int defaultIndex) {
    String opt = (String)JOptionPane.showInputDialog(uacalc.getFrame(),
        "Options",
        "Options",
        JOptionPane.QUESTION_MESSAGE, null,
        opts, defaultIndex);    
    return opt;
  }
  
  /**
   * Find the Vertex which has obj as its underlying object.
   * This assumes getDiagram() is not null.
   * 
   * @param obj
   * @return
   */
  public Vertex vertexOfObject(Object obj) {
    final Vertex[] verts = getDiagram().getVertices();
    for (int i = 0; i < verts.length; i++) {
      if (obj.equals(verts[i].getUnderlyingObject())) return verts[i];
    }
    return null;
  }
  
  public void addToSelectedElemList(Vertex v) {
    if (selectedElemList == null) selectedElemList = new ArrayList<Vertex>();
    selectedElemList.add(v);
    v.setColor(SELECTED_LIST_COLOR);
    v.setFilled(true);
  }
  
  public void resetSelectedElemList() {
    selectedElemList = null;
    resetVertexColors();
  }
  
  public List<Vertex> getSelectedElemList() {
    return selectedElemList;
  }
  
  public void setSelectedElemList(List<Vertex> vertexList) {
    selectedElemList = vertexList;
  }
  
  public void setSelectedElem(Vertex selectedElem) {
    this.selectedElem = selectedElem;
    resetSelectedElemList();
  }

  public Vertex getSelectedElem() {
    return selectedElem;
  }
  
  public void resetVertexColors() {
    if (getDiagram() == null) return;
    getDiagram().resetVertices();
    getDiagram().hideLabels();
    if (selectedElem != null) selectedElem.setHighlighted(true);
    boolean selectedFilledAbove = false;
    boolean selectedFilledBelow = false;
    switch (getAboveType()) {
      case UC:
        showUpperCovers();
        break;
      case MI:
        if (selectedElem != null)  selectedFilledAbove = selectedElem.isMeetIrreducible();
        showMIsAbove();
        break;
      case FIL:
        if (selectedElem != null)  selectedFilledAbove = true;
        showFilter();
        break;
      case MDECOMP:
        if (selectedElem != null)  selectedFilledAbove = selectedElem.isMeetIrreducible();
        showMeetIrredundantDecomp();
        break;
    }
    switch (getBelowType()) {
      case LC:
        showLowerCovers();
        break;
      case JI:
        if (selectedElem != null)  selectedFilledBelow = selectedElem.isJoinIrreducible();
        showJIsBelow();
        break;
      case ID:
        if (selectedElem != null)  selectedFilledBelow = true;
        showIdeal();
        break;
      case JDECOMP:
        if (selectedElem != null)  selectedFilledBelow = selectedElem.isJoinIrreducible();
        showJoinIrredundantDecomp();
        break;
    }
    if (selectedElem != null && selectedFilledAbove && selectedFilledBelow) {
      //selectedElem.setFilled(true);
      selectedElem.setColor(BOTH_COLOR);
      selectedElem.setFilled(true);
    }
    repaint();
  }

  public void setBelowType(RadioButtonType belowType) {
    this.belowType = belowType;
  }

  public RadioButtonType getBelowType() {
    return belowType;
  }

  public void setAboveType(RadioButtonType aboveType) {
    this.aboveType = aboveType;
  }

  public RadioButtonType getAboveType() {
    return aboveType;
  }

  public List<Vertex> joinIrredsBelow(Vertex v) {
    List<Vertex> ans = new ArrayList<Vertex>();
    final Diagram diag = getDiagram();
    final Vertex[] verts = diag.getVertices();
    for (int i = 0; i < verts.length; i++) {
      Vertex u = verts[i];
      if (u.isJoinIrreducible() && diag.leq(u, v)) {
        ans.add(u);
      }
    }
    return ans;
  }
  
  public List<Vertex> meetIrredsAbove(Vertex v) {
    List<Vertex> ans = new ArrayList<Vertex>();
    final Diagram diag = getDiagram();
    final Vertex[] verts = diag.getVertices();
    for (int i = 0; i < verts.length; i++) {
      Vertex u = verts[i];
      if (u.isMeetIrreducible() && diag.leq(v, u)) {
        ans.add(u);
      }
    }
    return ans;
  }
  
  public List<Vertex> ideal(Vertex v) {
    List<Vertex> ans = new ArrayList<Vertex>();
    final Diagram diag = getDiagram();
    final Vertex[] verts = diag.getVertices();
    for (int i = 0; i < verts.length; i++) {
      Vertex u = verts[i];
      if (diag.leq(u, v)) {
        ans.add(u);
      }
    }
    return ans;
  }
  
  public List<Vertex> filter(Vertex v) {
    List<Vertex> ans = new ArrayList<Vertex>();
    final Diagram diag = getDiagram();
    final Vertex[] verts = diag.getVertices();
    for (int i = 0; i < verts.length; i++) {
      Vertex u = verts[i];
      if (diag.leq(v, u)) {
        ans.add(u);
      }
    }
    return ans;
  }
  
  public List<Vertex> lowerCovers(Vertex v) {
    final List<Vertex> ans = new ArrayList<Vertex>();
    final Diagram diag = getDiagram();
    List<POElem> lc = (List<POElem>)v.getUnderlyingElem().lowerCovers();
    for (POElem elt : lc) {
      ans.add(diag.vertexForPOElem(elt));
    }
    return ans;
  }
  
  public List<Vertex> upperCovers(Vertex v) {
    final List<Vertex> ans = new ArrayList<Vertex>();
    final Diagram diag = getDiagram();
    List<POElem> lc = (List<POElem>)v.getUnderlyingElem().upperCovers();
    for (POElem elt : lc) {
      ans.add(diag.vertexForPOElem(elt));
    }
    return ans;
  }
  
  // change all the ones above to use the method in BasicLattice.
  
  public List<Vertex> irredundantMeetDecomposition(Vertex v) {
    if (lattice == null || getDiagram() == null) return null;
    POElem e = v.getUnderlyingElem();
    return lattice.getVertices(lattice.irredundantMeetDecomposition(e));
  }
  
  public List<Vertex> irredundantJoinDecomposition(Vertex v) {
    if (lattice == null || getDiagram() == null) return null;
    POElem e = v.getUnderlyingElem();
    return lattice.getVertices(lattice.irredundantJoinDecomposition(e));
  }

  public void setLabels(boolean numbers) {
    Vertex[] verts = getDiagram().getVertices();
    final int n = verts.length;
    for (int i = 0; i < n; i++) {
      verts[i].setUseOrderForLabel(numbers);      
      //if (numbers) verts[i].setLabel(Integer.toString(i));
      //else verts[i].setLabel(null);
    }
  }

  // move the tool bar stuff into DrawPanel
  public JToolBar makeToolBar() {
    //JToolBar toolBar = new JToolBar(JToolBar.VERTICAL);
    JToolBar toolBar = new JToolBar();
    ClassLoader cl = this.getClass().getClassLoader();
    JButton goButton = new JButton("Go");
    goButton.setPreferredSize(new Dimension(32, 32));
    goButton.setToolTipText("Draw the lattice and table");
    goButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        if (getDiagram() == null) {
          final int idx = getUACalc().getTabbedPane().getSelectedIndex();
          if (idx == 3) {
            getUACalc().getMainController().getConController().drawCon();
          }
          if (idx == 4) {
            getUACalc().getMainController().getSubController().drawSub();
          }
          if (idx == 5) {
            getUACalc().getMainController().getDrawingController().drawAlg();
          }
        }
      }
    });
    
    toolBar.add(goButton);
    
    ImageIcon icon = new ImageIcon
      (cl.getResource("org/latdraw/sample/images/Forward16.gif"));
    JButton forwardButton = new JButton(icon);
    forwardButton.setPreferredSize(new Dimension(32, 32));
    forwardButton.setToolTipText("Rotate forward");
    forwardButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getDiagram() != null) {
            drawPanel.rotateOnce();
          }
        }
      });
    toolBar.add(forwardButton);
    icon = new ImageIcon(
                 cl.getResource("org/latdraw/sample/images/Back16.gif"));
    JButton backButton = new JButton(icon);
    backButton.setPreferredSize(new Dimension(32, 32));
    backButton.setToolTipText("Rotate back");
    backButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getDiagram() != null) {
            drawPanel.rotateLeft();
          }
        }
      });
    toolBar.add(backButton);
    icon = new ImageIcon(
                 cl.getResource("org/latdraw/sample/images/Redo16.gif"));
    JButton rotButton = new JButton(icon);
    rotButton.setPreferredSize(new Dimension(32, 32));
    rotButton.setToolTipText("Rotate");
    rotButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getDiagram() != null) {
            if (drawPanel.isRotating()) drawPanel.stopRotation();
            else drawPanel.startRotation();
          }
        }
      });
    toolBar.add(rotButton);
    JButton inButton = new JButton("><");
    inButton.setPreferredSize(new Dimension(32, 32));
    inButton.setToolTipText("Push in horizontally");
    inButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getDiagram() != null) {
            drawPanel.increaseAttraction();
            drawPanel.decreaseReplusion();
            drawPanel.improveWithoutDelay(40);
          }
        }
      });
    toolBar.add(inButton);
    JButton outButton = new JButton("<>");
    outButton.setPreferredSize(new Dimension(32, 32));
    outButton.setToolTipText("Push out horizontally");
    outButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getDiagram() != null) {
            drawPanel.increaseReplusion();
            drawPanel.decreaseAttraction();
            drawPanel.improveWithoutDelay(40);
          }
        }
      });
    toolBar.add(outButton);
    JButton improveButton = new JButton("++");
    improveButton.setPreferredSize(new Dimension(32, 32));
    improveButton.setToolTipText("Improve the diagram");
    improveButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          if (getDiagram() != null) {
            drawPanel.improve();
          }
        }
      });
    toolBar.add(improveButton);
    
    toolBar.addSeparator();
    ButtonGroup lowerBG = new ButtonGroup();
    JRadioButton jis = (JRadioButton)toolBar.add(new JRadioButton("JI"));
    JRadioButton lcovs = (JRadioButton)toolBar.add(new JRadioButton("L C"));
    JRadioButton ideals = (JRadioButton)toolBar.add(new JRadioButton("Id"));
    JRadioButton jdecomp = (JRadioButton)toolBar.add(new JRadioButton("J D"));
    JRadioButton lnone = (JRadioButton)toolBar.add(new JRadioButton("Off", true));
    lowerBG.add(jis);
    lowerBG.add(lcovs);
    lowerBG.add(ideals);
    lowerBG.add(jdecomp);
    lowerBG.add(lnone);
    jis.setToolTipText("join irreds below highlighted elem or 1");
    lcovs.setToolTipText("lower covers of highlighted elem or 1");
    ideals.setToolTipText("ideal of highlighted elem");
    jdecomp.setToolTipText("an irredundant join decomposition of highlighted elem or 1");
    
    
    toolBar.addSeparator();
    ButtonGroup upperBG = new ButtonGroup();
    JRadioButton mis = (JRadioButton)toolBar.add(new JRadioButton("MI"));
    JRadioButton ucovs = (JRadioButton)toolBar.add(new JRadioButton("U C"));
    JRadioButton filters = (JRadioButton)toolBar.add(new JRadioButton("Fil"));
    JRadioButton mdecomp = (JRadioButton)toolBar.add(new JRadioButton("M D"));
    JRadioButton unone = (JRadioButton)toolBar.add(new JRadioButton("Off", true));
    upperBG.add(mis);
    upperBG.add(ucovs);
    upperBG.add(filters);
    upperBG.add(mdecomp);
    upperBG.add(unone);
    mis.setToolTipText("meet irreds above highlighted elem or 0");
    ucovs.setToolTipText("upper covers of highlighted elem or 0");
    filters.setToolTipText("filters of highlighted elem");
    mdecomp.setToolTipText("an irredundant meet decomposition of highlighted elem or 0");
    
    jis.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setBelowType(RadioButtonType.JI);
        resetVertexColors();
      }
    });
    
    lcovs.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setBelowType(RadioButtonType.LC);
        resetVertexColors();
      }
    });
    
    ideals.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setBelowType(RadioButtonType.ID);
        resetVertexColors();
      }
    });
    
    jdecomp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setBelowType(RadioButtonType.JDECOMP);
        resetVertexColors();
      }
    });
    
    lnone.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setBelowType(RadioButtonType.OFF);
        resetVertexColors();
      }
    });
    
    mis.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setAboveType(RadioButtonType.MI);
        resetVertexColors();
      }
    });
    
    ucovs.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        //showUpperCovers();
        setAboveType(RadioButtonType.UC);
        resetVertexColors();
      }
    });
    
    filters.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setAboveType(RadioButtonType.FIL);
        resetVertexColors();
      }
    });
    
    mdecomp.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setAboveType(RadioButtonType.MDECOMP);
        resetVertexColors();
      }
    });
    
    unone.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {
        setAboveType(RadioButtonType.OFF);
        resetVertexColors();
      }
    });

    return toolBar;
  }
  
  public Vertex zero() {
    return getDiagram().vertexForPOElem(getBasicLattice().zero());
  }
  
  public Vertex one() {
    return getDiagram().vertexForPOElem(getBasicLattice().one());
  }

  private void showUpperCovers() {
    Vertex v = getSelectedElem();
    if (v == null) v = getDiagram().vertexForPOElem(getBasicLattice().zero());
    for (Vertex u : upperCovers(v)) {
      u.setColor(ABOVE_COLOR);
      u.setFilled(true);
    }
  }
  
  private void showLowerCovers() {
    Vertex v = getSelectedElem();
    if (v == null) v = getDiagram().vertexForPOElem(getBasicLattice().one());
    for (Vertex u : lowerCovers(v)) {
      u.setColor(BELOW_COLOR);
      u.setFilled(true);
    }
  }
  
  private void showMIsAbove() {
    Vertex v = getSelectedElem();
    if (v == null) v = getDiagram().vertexForPOElem(getBasicLattice().zero());
    for (Vertex u : filter(v)) {
      if (u.isMeetIrreducible()) {
        u.setColor(ABOVE_COLOR);
        u.setFilled(true);
      }
    }
  }
  
  private void showJIsBelow() {
    Vertex v = getSelectedElem();
    if (v == null) v = getDiagram().vertexForPOElem(getBasicLattice().one());
    for (Vertex u : ideal(v)) {
      if (u.isJoinIrreducible()) {
        u.setColor(BELOW_COLOR);
        u.setFilled(true);
      }
    }
  }
  
  private void showFilter() {
    Vertex v = getSelectedElem();
    if (v == null) return;
    for (Vertex u : filter(v)) {
      u.setColor(ABOVE_COLOR);
      u.setFilled(true);
    }
  }
  
  private void showIdeal() {
    Vertex v = getSelectedElem();
    if (v == null) return;
    for (Vertex u : ideal(v)) {
      u.setColor(BELOW_COLOR);
      u.setFilled(true);
    }
  }
  
  private void showMeetIrredundantDecomp() {
    Vertex v = getSelectedElem();
    if (v == null) v = getDiagram().vertexForPOElem(getBasicLattice().zero());
    for (Vertex u : irredundantMeetDecomposition(v)) {
      u.setColor(ABOVE_COLOR);
      u.setFilled(true);
    }
  }
  
  private void showJoinIrredundantDecomp() {
    Vertex v = getSelectedElem();
    if (v == null) v = getDiagram().vertexForPOElem(getBasicLattice().one());
    for (Vertex u : irredundantJoinDecomposition(v)) {
      u.setColor(BELOW_COLOR);
      u.setFilled(true);
    }
  }
  
  
  /**
   * Make a sample lattice.
   */
  public static org.latdraw.orderedset.OrderedSet makeExampleLat() 
                                          throws NonOrderedSetException {
    String n = "Test";
    List l = new ArrayList();
    l.add("0"); 
    l.add("a");
    l.add("b");
    l.add("c");
    l.add("topelt bigname");

    List c = new ArrayList();
    List tmp;

    // covers of 0
    tmp = new ArrayList();
    tmp.add("b");
    tmp.add("c");
    c.add(tmp);

    // covers of a
    tmp = new ArrayList();
    tmp.add("topelt bigname");
    c.add(tmp);

    // covers of b
    tmp = new ArrayList();
    tmp.add("topelt bigname");
    c.add(tmp);


    // covers of c
    tmp = new ArrayList();
    tmp.add("a");
    c.add(tmp);

    // covers of 1
    tmp = new ArrayList();
    c.add(tmp);

    InputLattice i = new InputLattice(n, l, c);
    return new org.latdraw.orderedset.OrderedSet(i);
  }

  
  
}
