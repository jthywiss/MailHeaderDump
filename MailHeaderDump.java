// MailHeaderDump.java -- Java class MailHeaderDump
// Project MailHeaderDump
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
import java.util.logging.Logger;
import java.text.SimpleDateFormat;
import java.math.BigInteger;
import java.io.*;
import javax.mail.*;
import javax.mail.internet.InternetAddress;
import com.sun.mail.imap.*;
import java.security.MessageDigest;

public class MailHeaderDump {
	
	public static boolean debug = false;
	public static boolean hashAddresses = true;
	public static boolean hashSubjects = true;
	public static boolean hashMessageIds = true;
	private static MessageDigest hasher;
	private static FetchProfile ourFetchProfile = null;
	static MailHeaderDumpFrame mainFrame = null;
	static Logger logger = Logger.getLogger("mailheaderdump");
	
    public static void main(String[] args) throws FileNotFoundException, NoSuchProviderException, MessagingException, java.lang.reflect.InvocationTargetException, UnsupportedEncodingException, java.security.NoSuchAlgorithmException {
		logger.entering("MailHeaderDump", "main", args);
		mainFrame = MailHeaderDumpFrame.initUI();
		try {
			mainFrame.getOptions();
			URLName mailboxUrl = new URLName(mainFrame.urlText);
			if (hashAddresses || hashSubjects || hashMessageIds) {
				hasher = MessageDigest.getInstance("MD5");
			}
			hashAddresses = mainFrame.hashAddresses;
			hashSubjects = mainFrame.hashSubjects;
			hashMessageIds = mainFrame.hashMessageIds;
			
			Session session = Session.getInstance(System.getProperties(), mainFrame.ourAuthenticator);
			session.setDebug(debug);
			
			PrintStream dumpOut = new PrintStream(new FileOutputStream(mainFrame.filePrefixText+mailboxUrl.getUsername()+"-"+new SimpleDateFormat("yyyyMMddHHmmss").format(new Date())+".txt"));
			
			try {
				Store store = session.getStore(mailboxUrl);
				mainFrame.setStatus("Connecting to "+store);
				store.connect();
				
				try {
					//Folder[] personalFolders = store.getPersonalNamespaces();
					Folder[] personalFolders = { store.getDefaultFolder() };
					
					dumpOut.println("Message-ID\tSent Date\tReceived Date\tFrom Count\tFrom Int\tFrom Addrs\tTo Count\tTo Int\tTo Addrs\tCc Count\tCc Int\tCc Addrs\tReply-To Count\tReply-To Int\tReply-To Addrs\tSubject\tHost\tUsername\tFolder\tSeen\tAnswered\tFlagged\tDraft\tSize\tImportance\tHas-Attach\t");
					
					dumpFolders(personalFolders, dumpOut);
					
				} finally {
					mainFrame.setStatus("Closing "+store);
					store.close();
				}
			} finally {
				dumpOut.close();
			}
			
			mainFrame.setStatus("Complete "+new Date());
		} finally {
			mainFrame.terminateUI();
		}
		logger.exiting("MailHeaderDump", "main");
    }
	
	private static SimpleDateFormat outputDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	
    public static void dumpFolders(Folder[] folders, PrintStream out) throws MessagingException, UnsupportedEncodingException {
		logger.entering("MailHeaderDump", "dumpFolders");
		outputDateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
		if (ourFetchProfile == null) {
			initFetchProfile();
		}
		for (int i = 0; i < folders.length; i++) {
			if (folders[i].getFullName().equalsIgnoreCase("Calendar") || folders[i].getFullName().equalsIgnoreCase("Contacts") || folders[i].getFullName().equalsIgnoreCase("Tasks")) {
				/* Skip Exchange's non-mail folders */
				continue;
			}
			String ourSLD = secondLevelDomain(folders[i].getURLName().getHost());
			mainFrame.setStatus("Dumping \""+folders[i].getFullName()+"\" on "+folders[i].getURLName().getHost());
			
			if ((folders[i].getType() & Folder.HOLDS_MESSAGES) != 0) {
				folders[i].open(Folder.READ_ONLY);
				try {
					Message[] messages = folders[i].getMessages();
					folders[i].fetch(messages, ourFetchProfile);
					for (int j = 0; j < messages.length; j++) {
						out.print(analyzeMessageId(firstString(messages[j].getHeader("Message-ID")))+"\t");
						out.print(outputDateFormat.format(messages[j].getSentDate())+"\t");
						out.print(outputDateFormat.format(messages[j].getReceivedDate())+"\t");
						out.print(analyzeAddresses(messages[j].getFrom(), ourSLD, "\t")+"\t");
						out.print(analyzeAddresses(messages[j].getRecipients(Message.RecipientType.TO), ourSLD, "\t")+"\t");
						out.print(analyzeAddresses(messages[j].getRecipients(Message.RecipientType.CC), ourSLD, "\t")+"\t");
						out.print(analyzeAddresses(messages[j].getReplyTo(), ourSLD, "\t")+"\t");
						out.print(analyzeSubject(messages[j].getSubject())+"\t");
						out.print(messages[j].getFolder().getURLName().getHost()+"\t");
						out.print(messages[j].getFolder().getURLName().getUsername()+"\t");
						out.print(messages[j].getFolder().getFullName()+"\t");
						out.print((messages[j].getFlags().contains(Flags.Flag.SEEN)?"Seen":"")+"\t");
						out.print((messages[j].getFlags().contains(Flags.Flag.ANSWERED)?"Answered":"")+"\t");
						out.print((messages[j].getFlags().contains(Flags.Flag.FLAGGED)?"Flagged":"")+"\t");
						out.print((messages[j].getFlags().contains(Flags.Flag.DRAFT)?"Draft":"")+"\t");
						out.print(messages[j].getSize()+"\t");
						out.print(firstString(messages[j].getHeader("Importance"))+"\t");
						out.print(firstString(messages[j].getHeader("X-MS-Has-Attach"))+"\t");
						//out.print(messages[j].getLineCount()+"\t");
						//out.print(messages[j].getContentType()+"\t");
						out.print("\n");
					}
				} finally {
					folders[i].close(false);
				}
			}

			dumpFolders(folders[i].list(), out);
		}
		logger.exiting("MailHeaderDump", "dumpFolders");
	}
		
	private static String analyzeAddresses(Address[] addresses, String ourDomainSuffix, String seperator) throws UnsupportedEncodingException {
		String result = safeLength(addresses) + seperator;
		int fromOurDomain = 0;
		for (int i = 0; i < safeLength(addresses); i++) {
			if (addresses[i] instanceof javax.mail.internet.InternetAddress) {
				String currAddr = ((javax.mail.internet.InternetAddress)addresses[i]).getAddress();
				int endCurrAddr = currAddr.length();
				if (currAddr.endsWith(".")) {
					/* Trim trailing dot. */
					endCurrAddr--;
				}
				if (currAddr.substring(endCurrAddr-ourDomainSuffix.length(), endCurrAddr).equalsIgnoreCase(ourDomainSuffix)) {
					fromOurDomain++;
				}
			}
		}
		result += fromOurDomain + seperator;
		for (int i = 0; i < safeLength(addresses); i++) {
			String addrString;
			if (hashAddresses) {
				if (addresses[i] instanceof InternetAddress) {
					addrString = ((InternetAddress)addresses[i]).getAddress();
				} else {
					addrString = addresses[i].toString();
				}
				result += (new BigInteger(1, hasher.digest(addrString.toLowerCase().getBytes("UTF-8")))).toString(32);
			} else {
				result += addresses[i].toString();
			}
			if ((i + 1) < safeLength(addresses)) {
				result += ",";
			}
		}
		
		return result;
	}
	
	private static String analyzeSubject(String theSubject) throws UnsupportedEncodingException {
		if (theSubject == null) {
			return null;
		}
		
		int startIndex = 0;
		int lastStart = -1;
		while (lastStart != startIndex) {
			lastStart = startIndex;
			if (theSubject.regionMatches(true, startIndex, "Re: ", 0, 4)) {
				startIndex += 4;
			}
			if (theSubject.regionMatches(true, startIndex, "Fwd: ", 0, 5)) {
				startIndex += 5;
			}
			if (theSubject.regionMatches(true, startIndex, "FW: ", 0, 4)) {
				startIndex += 4;
			}
		}
		
		if (hashSubjects) {
			return (new BigInteger(1, hasher.digest(theSubject.substring(startIndex).toUpperCase().getBytes("UTF-8")))).toString(32);
		}
		return theSubject.substring(startIndex);
	}
	
	private static String analyzeMessageId(String theMessageId) throws UnsupportedEncodingException {
		if (theMessageId != null && theMessageId.length() > 0 && hashMessageIds) {
			return (new BigInteger(1, hasher.digest(theMessageId.toLowerCase().getBytes("UTF-8")))).toString(32);
		}
		return theMessageId;
	}
	
	private static int safeLength(Object[] anArray) {
		if (anArray == null) {
			return 0;
		}
		return anArray.length;
	}
	
	private static String firstString(String[] anArray) {
		if (anArray == null || anArray.length < 1) {
			return "";
		}
		return anArray[0];
	}
	
	private static String secondLevelDomain(String domainName) {
		if (domainName == null || domainName.length() < 3) {
			return null;
		}
		if (!((domainName.charAt(0) >= 'A' && domainName.charAt(0) <= 'Z') 
			|| (domainName.charAt(0) >= 'a' && domainName.charAt(0) <= 'z'))) {
			/* This is not a domain name string.  (For eample, an IP address.) */
			return null;
		}
		int endOfTld = (domainName.charAt(domainName.length()-1)=='.') ? domainName.length()-2 : domainName.length()-1;
		int dotOne = domainName.lastIndexOf('.', endOfTld);
		if (dotOne < 0) {
			/* No dots -- no secondLevelDomain */
			return null;
		}
		int dotTwo = domainName.lastIndexOf('.', dotOne-1);
		/* Note: if no dotTwo, we'll use the whole string */
		return domainName.substring(dotTwo+1, endOfTld+1);
	}
	
	private static String[] headersOfInterest = { "Message-ID", "X-MS-Has-Attach", "Importance" };
	
	private static void initFetchProfile() {
		ourFetchProfile = new FetchProfile();
		ourFetchProfile.add(FetchProfile.Item.ENVELOPE);
		//ourFetchProfile.add(FetchProfile.Item.CONTENT_INFO);
		ourFetchProfile.add(FetchProfile.Item.FLAGS);
		ourFetchProfile.add(com.sun.mail.imap.IMAPFolder.FetchProfileItem.SIZE);
		
		for (int i=0; i < headersOfInterest.length; i++) {
			ourFetchProfile.add(headersOfInterest[i]);
		}
		
		/* Possibly interesting attributes:

		 Received 
		 Return-Path 
		 Date 
		 From 
		 Sender 
		 Reply-To 
		 To 
		 Cc 
		 Message-ID 
		 Subject 
		 Content-Type
		 X-OriginalArrivalTime 
		 X-MS-Has-Attach 
		 Importance 
		 X-Mailer
		 
		 Received Date 
		 Flags: ANSWERED DRAFT FLAGGED READ 
		 Line Count 
		 Size
		 */
	}
}
