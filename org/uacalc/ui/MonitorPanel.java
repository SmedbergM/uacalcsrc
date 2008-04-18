package org.uacalc.ui;

import javax.swing.JPanel;
import javax.swing.JTextArea;
import java.awt.Insets;
import javax.swing.*;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.util.*;
import org.uacalc.ui.tm.*;

public class MonitorPanel extends JPanel {
  
  private UACalculator uacalc;
  private ProgressReport monitor;
  //private TaskRunner runner;
  
  // the one that is currently displayed
  BackgroundTask task;
  // a list of all
  List<BackgroundTask> tasks = new ArrayList<BackgroundTask> ();
  
  private final JTextArea logArea;
  private final JTextField passField;
  private final JTextField sizeField;
  private final JTextField descField;
  private final JTextField passSizeField;

  public MonitorPanel(UACalculator uacalc) {
    this.uacalc = uacalc;
    setLayout(new BorderLayout());
    logArea = new JTextArea(10, 50);
    logArea.setMargin(new Insets(5, 5, 5, 5));
    logArea.setEditable(false);
    logArea.setAutoscrolls(true);
    JLabel passLabel = new JLabel("Pass: ");
    JLabel sizeLabel = new JLabel("Size: ");
    JLabel currentSizeLabel = new JLabel("Now: ");
    passField = new JTextField(6);
    sizeField = new JTextField(12);
    passSizeField = new JTextField(12);
    descField = new JTextField(30);
    JButton cancelButton  = new JButton("Cancel");
    cancelButton.setActionCommand("cancel");
    cancelButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          System.out.println("getTask(): " + getTask());
          if (getTask() != null) {
            //System.out.println("cancelling ...");
            getTask().cancel(true);
            //runner.cancel(true);
            //monitor.setCancelled(true);
            //monitor.printlnToLog("...cancelling...");
          }
        }
      });
    JButton clearButton = new JButton("Clear");
    clearButton.addActionListener(new ActionListener() {
        public void actionPerformed(ActionEvent e) {
          logArea.setText(null);
        }
      });
    
    JPanel topPanel = new JPanel();
    topPanel.add(descField);
    topPanel.add(passLabel);
    topPanel.add(passField);
    topPanel.add(sizeLabel);
    topPanel.add(passSizeField);
    topPanel.add(currentSizeLabel);
    topPanel.add(sizeField);
    add(topPanel, BorderLayout.NORTH);
    add(new JScrollPane(logArea), BorderLayout.CENTER);
    JPanel botPanel = new JPanel();
    botPanel.setLayout(new BoxLayout(botPanel, BoxLayout.X_AXIS));
    botPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    botPanel.add(Box.createHorizontalGlue());
    botPanel.add(cancelButton);
    botPanel.add(Box.createRigidArea(new Dimension(10, 0)));
    botPanel.add(clearButton);
    
    add(botPanel, BorderLayout.SOUTH);
    setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
    monitor = new ProgressReport(this);
    //uacalc.setMonitor(monitor);
  }
  
  public JTextField getDescriptionField() { return descField; }
  public JTextField getPassField() { return passField; }
  public JTextField getPassSizeField() { return passSizeField; }
  public JTextField getSizeField() { return sizeField; }
  public JTextArea getLogArea() { return logArea; }
  
  
  public ProgressReport getProgressModel() { return monitor; }
  
  public void setProgressModel(ProgressReport m) {
    monitor = m;
    descField.setText(m.getDescription());
    passField.setText(String.valueOf(m.getPass()));
    passSizeField.setText(String.valueOf(m.getPassSize()));
    sizeField.setText(String.valueOf(m.getSize()));
    logArea.setText(null);
    for (String s : m.getLogLines()) {
      logArea.append(s);
    }
  }
  
  
  public BackgroundTask getTask() { return task; }
  public void setTask(BackgroundTask v) { task = v; }
  
  public void addTask(BackgroundTask task) {
    addTask(task, true);
  }
  
  public void addTask(BackgroundTask task, boolean makecurrent) {
    tasks.add(task);
    setTask(task);
  }
  
}
