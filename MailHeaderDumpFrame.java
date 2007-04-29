// MailHeaderDumpFrame.java -- Java class MailHeaderDumpFrame
// Project MailHeaderDumpFrame
//
// Created by John Thywissen on 12 Mar 2006.
//
// Copyright (c) 2006 John Thywissen. All rights reserved.
//
//             - - -  PROPRIETARY  - - -
// This work, and the information within, are 
// proprietary property of John Thywissen.  Use
// or duplication requires prior written permission.
//

package org.thywissen.john.mailheaderdump;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.prefs.Preferences;

public class MailHeaderDumpFrame extends JFrame implements Runnable, ActionListener {
	
	public SimpleAuthenticator ourAuthenticator = null;
	protected JLabel headerLabel;
	protected JTextField urlField;
	protected JLabel urlFieldLabel;
	protected JTextField fileField;
	protected JLabel fileFieldLabel;
	protected JCheckBox hashAddressesCheckBox;
	protected JCheckBox hashSubjectsCheckBox;
	protected JCheckBox hashMessageIdsCheckBox;
	protected JButton startButton;
	protected JLabel statusLine;
	protected static Preferences prefs;
	public String urlText;
	public String filePrefixText;
	public boolean hashAddresses = true;
	public boolean hashSubjects = true;
	public boolean hashMessageIds = true;
	public String statusText;
	public boolean startButtonPressed = false;
	protected Object startButtonCondition = new Object();
	
	public static MailHeaderDumpFrame initUI() {
		MailHeaderDump.logger.entering("MailHeaderDumpFrame", "initUI");
		System.setProperty("apple.laf.useScreenMenuBar", "true");
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (ClassNotFoundException e) {
			/* Disregard, fall back to default LAF */
		} catch (InstantiationException e) {
			/* Disregard, fall back to default LAF */
		} catch (IllegalAccessException e) {
			/* Disregard, fall back to default LAF */
		} catch (UnsupportedLookAndFeelException e) {
			/* Disregard, fall back to default LAF */
		}
		JFrame.setDefaultLookAndFeelDecorated(false);
		prefs = Preferences.userNodeForPackage(MailHeaderDumpFrame.class);
		MailHeaderDump.logger.exiting("MailHeaderDumpFrame", "initUI");
		return new MailHeaderDumpFrame();
	}

	public MailHeaderDumpFrame() {
		super("Mail Header Dump");
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		getContentPane().setLayout(new GridBagLayout());
		GridBagConstraints ourGridBagConstr = new GridBagConstraints();
		ourGridBagConstr.insets = new Insets(4, 4, 4, 4);
		
		urlText = prefs.get("mailServerUrl", "imap://user@mail.example.com");  //jthywissen@imap.aol.com//xz4tgr@usplm235.amer.corp.eds.com
		filePrefixText = prefs.get("dumpFilePrefix", "MHD-");
		hashAddresses = prefs.getBoolean("hashAddresses", true);
		hashSubjects = prefs.getBoolean("hashSubjects", true);
		hashMessageIds = prefs.getBoolean("hashMessageIds", true);
		
		headerLabel = new JLabel("MailHeaderDump v1.0 by J^T    (c) 2006");
		ourGridBagConstr.gridx = GridBagConstraints.RELATIVE;
		ourGridBagConstr.gridwidth = GridBagConstraints.REMAINDER;
		ourGridBagConstr.weightx = 50;
		ourGridBagConstr.weighty = 0;
		ourGridBagConstr.anchor = GridBagConstraints.PAGE_START;
		ourGridBagConstr.fill = GridBagConstraints.HORIZONTAL;
		getContentPane().add(headerLabel, ourGridBagConstr);
		
		urlField = new JTextField(urlText);
		urlFieldLabel = new JLabel("Mail server URL:");
		urlFieldLabel.setLabelFor(urlField);
		ourGridBagConstr.gridx = GridBagConstraints.RELATIVE;
		ourGridBagConstr.gridwidth = 1;
		ourGridBagConstr.weightx = 0;
		ourGridBagConstr.weighty = 0;
		ourGridBagConstr.anchor = GridBagConstraints.LINE_START;
		ourGridBagConstr.fill = GridBagConstraints.NONE;
		getContentPane().add(urlFieldLabel, ourGridBagConstr);
		ourGridBagConstr.gridx = GridBagConstraints.RELATIVE;
		ourGridBagConstr.gridwidth = GridBagConstraints.REMAINDER;
		ourGridBagConstr.weightx = 50;
		ourGridBagConstr.weighty = 0;
		ourGridBagConstr.anchor = GridBagConstraints.LINE_END;
		ourGridBagConstr.fill = GridBagConstraints.HORIZONTAL;
		getContentPane().add(urlField, ourGridBagConstr);
		
		fileField = new JTextField(filePrefixText);
		fileFieldLabel = new JLabel("Output file prefix:");
		fileFieldLabel.setLabelFor(fileField);
		ourGridBagConstr.gridx = GridBagConstraints.RELATIVE;
		ourGridBagConstr.gridwidth = 1;
		ourGridBagConstr.weightx = 0;
		ourGridBagConstr.weighty = 0;
		ourGridBagConstr.anchor = GridBagConstraints.LINE_START;
		ourGridBagConstr.fill = GridBagConstraints.NONE;
		getContentPane().add(fileFieldLabel, ourGridBagConstr);
		ourGridBagConstr.gridx = GridBagConstraints.RELATIVE;
		ourGridBagConstr.gridwidth = GridBagConstraints.REMAINDER;
		ourGridBagConstr.weightx = 50;
		ourGridBagConstr.weighty = 0;
		ourGridBagConstr.anchor = GridBagConstraints.LINE_END;
		ourGridBagConstr.fill = GridBagConstraints.HORIZONTAL;
		getContentPane().add(fileField, ourGridBagConstr);
		
		hashAddressesCheckBox = new JCheckBox("Hash addresses", hashAddresses);
		ourGridBagConstr.gridx = 1;
		ourGridBagConstr.gridwidth = GridBagConstraints.REMAINDER;
		ourGridBagConstr.weightx = 50;
		ourGridBagConstr.weighty = 0;
		ourGridBagConstr.anchor = GridBagConstraints.LINE_END;
		ourGridBagConstr.fill = GridBagConstraints.HORIZONTAL;
		getContentPane().add(hashAddressesCheckBox, ourGridBagConstr);
		
		hashSubjectsCheckBox = new JCheckBox("Hash subjects", hashSubjects);
		ourGridBagConstr.gridx = 1;
		ourGridBagConstr.gridwidth = GridBagConstraints.REMAINDER;
		ourGridBagConstr.weightx = 50;
		ourGridBagConstr.weighty = 0;
		ourGridBagConstr.anchor = GridBagConstraints.LINE_END;
		ourGridBagConstr.fill = GridBagConstraints.HORIZONTAL;
		getContentPane().add(hashSubjectsCheckBox, ourGridBagConstr);
		
		hashMessageIdsCheckBox = new JCheckBox("Hash message IDs", hashMessageIds);
		ourGridBagConstr.gridx = 1;
		ourGridBagConstr.gridwidth = GridBagConstraints.REMAINDER;
		ourGridBagConstr.weightx = 50;
		ourGridBagConstr.weighty = 0;
		ourGridBagConstr.anchor = GridBagConstraints.LINE_END;
		ourGridBagConstr.fill = GridBagConstraints.HORIZONTAL;
		getContentPane().add(hashMessageIdsCheckBox, ourGridBagConstr);
		
		startButton = new JButton("Start");
		ourGridBagConstr.gridx = GridBagConstraints.RELATIVE;
		ourGridBagConstr.gridwidth = GridBagConstraints.REMAINDER;
		ourGridBagConstr.weightx = 0;
		ourGridBagConstr.weighty = 0;
		ourGridBagConstr.anchor = GridBagConstraints.LINE_END;
		ourGridBagConstr.fill = GridBagConstraints.NONE;
		getContentPane().add(startButton, ourGridBagConstr);
		
		statusLine = new JLabel(" ");
		ourGridBagConstr.gridx = GridBagConstraints.RELATIVE;
		ourGridBagConstr.gridwidth = GridBagConstraints.REMAINDER;
		ourGridBagConstr.weightx = 50;
		ourGridBagConstr.weighty = 0;
		ourGridBagConstr.anchor = GridBagConstraints.PAGE_END;
		ourGridBagConstr.fill = GridBagConstraints.HORIZONTAL;
		getContentPane().add(statusLine, ourGridBagConstr);
		
		getRootPane().setDefaultButton(startButton);
		
		startButton.addActionListener(this);
		startButtonPressed = false;
		
		//setResizable(false);
	    pack();
	    setVisible(true);
		
		ourAuthenticator = new SimpleAuthenticator(this);
	}
	
	public void getOptions() throws java.lang.reflect.InvocationTargetException {
		MailHeaderDump.logger.entering("MailHeaderDumpFrame", "getOptions");
		synchronized (startButtonCondition) {
			while (!startButtonPressed) {
				try {
					startButtonCondition.wait();
				} catch (InterruptedException e) {
					/* Disregard interrupts */
				}
			}
			try {
				SwingUtilities.invokeAndWait(this);
			} catch (InterruptedException e) {
				/* Disregard interrupts */
			}
		}
		MailHeaderDump.logger.exiting("MailHeaderDumpFrame", "getOptions");
	}
	
	public void setStatus(String newStatus) {
		statusText = newStatus;
		SwingUtilities.invokeLater(this);
	}
	
	/* Sync urlText, filePrefixText, and statusLine in this class with the AWT components in a thread-safe manner.
	 * Do do so, enqueue the invocation of this method by AWT's EventDispatchThread using SwingUtilities.invokeLater.
	 */
	public void run() {
		urlText = urlField.getText();
		filePrefixText = fileField.getText();
		hashAddresses = hashAddressesCheckBox.isSelected();
		hashSubjects = hashSubjectsCheckBox.isSelected();
		hashMessageIds = hashMessageIdsCheckBox.isSelected();
		statusLine.setText(statusText);
	}
	
	public void actionPerformed(ActionEvent event) {
		MailHeaderDump.logger.entering("MailHeaderDumpFrame", "actionPerformed");
		if (event.getSource() == startButton) {
			synchronized (startButtonCondition) {
				startButtonPressed = true;
				startButtonCondition.notifyAll();
			}
			JLabel headerLabel;
			urlField.setEnabled(false);
			fileField.setEnabled(false);
			hashAddressesCheckBox.setEnabled(false);
			hashSubjectsCheckBox.setEnabled(false);
			hashMessageIdsCheckBox.setEnabled(false);
			startButton.setEnabled(false);
			run();
			prefs.put("mailServerUrl", urlText);
			prefs.put("dumpFilePrefix", filePrefixText);
			prefs.putBoolean("hashAddresses", hashAddresses);
			prefs.putBoolean("hashSubjects", hashSubjects);
			prefs.putBoolean("hashMessageIds", hashMessageIds);
		}
		MailHeaderDump.logger.exiting("MailHeaderDumpFrame", "actionPerformed");
	}
	
	public void terminateUI() {
		MailHeaderDump.logger.entering("MailHeaderDumpFrame", "terminateUI");
		dispose();
		MailHeaderDump.logger.exiting("MailHeaderDumpFrame", "terminateUI");
	}
}
