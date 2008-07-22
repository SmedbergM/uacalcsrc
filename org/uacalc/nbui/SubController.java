package org.uacalc.nbui;

import java.awt.BorderLayout;
import java.beans.PropertyChangeSupport;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeEvent;

import org.uacalc.alg.SmallAlgebra;
import org.uacalc.alg.sublat.SubalgebraLattice;

public class SubController {

  private final UACalculatorUI uacalcUI;
  private LatDrawer subLatDrawer;
  
  public SubController(UACalculatorUI uacalcUI, PropertyChangeSupport cs) {
    this.uacalcUI = uacalcUI;
    subLatDrawer = new LatDrawer(uacalcUI);
    uacalcUI.getSubMainPanel().setLayout(new BorderLayout());
    uacalcUI.getSubMainPanel().add(subLatDrawer, BorderLayout.CENTER);
    addPropertyChangeListener(cs);
  }
  
  private void addPropertyChangeListener(PropertyChangeSupport cs) {
    cs.addPropertyChangeListener(
        MainController.ALGEBRA_CHANGED,
        new PropertyChangeListener() {
          public void propertyChange(PropertyChangeEvent evt) {
            getSubLatDrawer().setBasicLattice(null);  
            final SmallAlgebra alg = uacalcUI.getMainController().getCurrentAlgebra().getAlgebra();
            alg.resetConAndSub();
          }
        }
    );
  }
  
  public LatDrawer getSubLatDrawer() { return subLatDrawer; }
  
  public void drawSub() {
    SmallAlgebra alg = uacalcUI.getMainController().getCurrentAlgebra().getAlgebra();
    if (alg == null) return;
    final int subTabIndex = 4;
    if (uacalcUI.getTabbedPane().getSelectedIndex() != subTabIndex) {
      uacalcUI.getTabbedPane().setSelectedIndex(subTabIndex);
      uacalcUI.repaint();
    }
    drawSub(alg, true);
  }
  
  public void drawSub(SmallAlgebra alg, boolean makeIfNull) {
    if (!alg.isTotal()) {
      uacalcUI.getMainController().beep();
      uacalcUI.getMainController().setUserWarning(
          "Not all the operations of this algebra are total.", false);
      getSubLatDrawer().setBasicLattice(null);
      return;
    }
    final int maxSize = SubalgebraLattice.MAX_DRAWABLE_SIZE;
    if (!alg.sub().isDrawable()) {
      uacalcUI.getMainController().beep();
      uacalcUI.getMainController().setUserWarning(
          "Too many elements in the subalgebra lattice. More than " + maxSize + ".", false);
      getSubLatDrawer().setBasicLattice(null);
      return;
    }
    getSubLatDrawer().setBasicLattice(alg.sub().getBasicLattice(makeIfNull));
    //getConLatDrawer().setDiagram(alg.con().getDiagram());
    getSubLatDrawer().repaint();
  }
  
  
}
