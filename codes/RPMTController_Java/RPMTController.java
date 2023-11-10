// RPMTController.java
//  100% pure java program for Java9
//
// ver. 0.5
// Oct 24, 2019
//   M. Kitaguchi

import java.awt.Graphics;
import java.awt.Image;
import java.awt.Color;
import java.awt.image.*;
import java.awt.Toolkit;
import java.awt.event.*;
import java.awt.BorderLayout;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Robot;
import java.awt.Graphics2D;
import java.io.*;
import javax.imageio.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.util.*;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.Instant;
import java.net.*;

////////// Application main method //////////
public class RPMTController {
	
	public static final String VERSION = "0.5";
	public static final String DEVELOPPER = "Oct 24, 2019  M. Kitaguchi";
//	public static final String SUPPORTED  = "A-STEP, OPERA from JST";
	public static final String INFO = "Developped by M. Kitaguchi, 2019";
	
	public static void main(String[] args) {
		System.out.printf("-----------------------------------\n");
		System.out.printf(" RPMTController ver.%s\n", RPMTController.VERSION);
		System.out.printf("       %s\n", RPMTController.DEVELOPPER);
//		System.out.printf("            %s\n", RPMTController.SUPPORTED);		
		System.out.printf("-----------------------------------\n");

		RPMTControllerController  rcc = new RPMTControllerController();
		RPMTControllerWindow      rcw = new RPMTControllerWindow(
									String.format("RPMTController ver.%s",
									RPMTController.VERSION), 900,680);
		rcc.setView(rcw);
		rcw.setVisible(true);
		
		// for shutdown
		Runtime.getRuntime().addShutdownHook(new Thread(
			() -> System.out.printf("-----------------------------------\n") ));
	}
} // end of main

////////// Application controller class //////////
class RPMTControllerController implements ActionListener,MouseListener { 

	Gatenet   gatenet    = null;
	RPMT      rpmt       = null;
	EDRReader reader     = null;
	long startTimeMillis = 0;
	int  currentRunNo    = 1;
	RPMTControllerWindow controllerWindow = null;
	DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss");

	public RPMTControllerController() {
		gatenet = new Gatenet();
		rpmt    = new RPMT();
		reader  = new EDRReader();
	}
	public void setView(RPMTControllerWindow v) {
		controllerWindow = v;
		controllerWindow.setActionListener(this);
		controllerWindow.setMouseListener(this);
		controllerWindow.setRunNo(currentRunNo);
		controllerWindow.setWorkingDir(new File(".").getAbsoluteFile().getParent());
		controllerWindow.setImage2D(rpmt.get2DImage());
		controllerWindow.setImagePH(rpmt.getPulseHeightImage());
		controllerWindow.setImageTOF(rpmt.getTOFImage());
		controllerWindow.setEDRImage2D(reader.get2DImage());
		controllerWindow.setEDRImagePH(reader.getPulseHeightImage());
		controllerWindow.setEDRImageTOF(reader.getTOFImage());
	}	
	// actions event performing method
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if (cmd.equals("readInfo")) {			// Gatenet read info
			gatenet.setError(0);				// reset Gatenet error
			String infoStr = gatenet.getInfoString(controllerWindow.getGatenetAddress(), 
													controllerWindow.getGatenetPort());
			controllerWindow.setInfoText(infoStr);
			if (gatenet.getError() >0) {		// if error, show message
				JOptionPane.showMessageDialog(controllerWindow, 
												"Network error. \nPlease check network connection.");
			}
		}
		else if (cmd.equals("setInfo")) {		// Gatenet set info
			gatenet.setError(0);				// reset Gatenet error
			int kpreset = 0;	if (controllerWindow.getKPReset()) { kpreset = 1; }
			gatenet.setInfo(controllerWindow.getGatenetAddress(), 
										controllerWindow.getGatenetPort(), kpreset, 0);
			String infoStr = gatenet.getInfoString(controllerWindow.getGatenetAddress(), 
													controllerWindow.getGatenetPort());
			controllerWindow.setInfoText(infoStr);
			if (gatenet.getError() >0) {		// if error, show message
				JOptionPane.showMessageDialog(controllerWindow, 
												"Network error. \nPlease check network connection.");
			}
		}
		else if (cmd.equals("setlldtime")) {	// RPMT set LLD&Time
			rpmt.setAddressPort(controllerWindow.getRPMTSetLLDAddress(), controllerWindow.getRPMTSetLLDPort());
			rpmt.setLLDTime(controllerWindow.getRPMTSetLLDTime());
		}
		else if (cmd.equals("range")) {			// change 2D Z range
			if (controllerWindow.isAutoRange()) {
				rpmt.setRangeAuto();
			} else {
				rpmt.setRangeMin(controllerWindow.getRangeMin());
				rpmt.setRangeMax(controllerWindow.getRangeMax());
			}
			if (rpmt.getRunningStatus() == false) {		// if not running, re-display 2D image
				controllerWindow.setImage2D(rpmt.get2DImage());
			}
		}
		else if (cmd.equals("startstoprun")) {	// RPMT run start/stop
			if (rpmt.getRunningStatus() == false) {
				currentRunNo = controllerWindow.getRunNo();	// check filename
				if (currentRunNo>0) {
					while (true) {
						File f = new File(controllerWindow.getWorkingDir(), String.format("rpmt%04d.edr", currentRunNo));
						if (!f.exists()) { break; }
						f = null;	currentRunNo++;
					}
					controllerWindow.setRunNo(currentRunNo);	// set run No
					rpmt.setRunNo(currentRunNo);
				} else {
					currentRunNo = -1;	rpmt.setRunNo(-1);
				}				
				controllerWindow.setRunStatus(true);		// change run state
				controllerWindow.setTOFEnd( controllerWindow.getTOFEnd() );		// TOF end on plot
				// RPMT setting
				rpmt.setWorkingDir(controllerWindow.getWorkingDir());
				rpmt.setAddressPort(controllerWindow.getRPMTAddress(), controllerWindow.getRPMTPort());
				rpmt.setLLD(controllerWindow.getLLD());		rpmt.setXYLimit(controllerWindow.getXYLimit());
				rpmt.setTOFEnd(controllerWindow.getTOFEnd());
				if (controllerWindow.isLimitByTime())  { rpmt.setLimitTime(controllerWindow.getLimitTime()); }
					else { rpmt.setLimitTime(0); }
				if (controllerWindow.isLimitByKP())    { rpmt.setLimitKP(controllerWindow.getLimitKP()); }
					else { rpmt.setLimitKP(0); }
				if (controllerWindow.isLimitByCount()) { rpmt.setLimitCount(controllerWindow.getLimitCount()); }
					else { rpmt.setLimitCount(0); }
				rpmt.startRun();									// start RPMT run thread
				startTimeMillis = System.currentTimeMillis();		// start time [ms]				
				new Thread( new RPMTControllerUpdater()).start();	// RPMT status monitor thread start
				controllerWindow.setStartTime(LocalDateTime.now().format(dtf));	// display start time
				controllerWindow.setEndTime("");								// clear end time
				System.out.printf("Run No.%d start : %s\n",currentRunNo,LocalDateTime.now().format(dtf));
			} else {
				controllerWindow.setRunStatus(false);
				rpmt.stopRun();
				controllerWindow.setEndTime(LocalDateTime.now().format(dtf));	// display end time
				System.out.printf("           end : %s\n",LocalDateTime.now().format(dtf));
				if (currentRunNo >0) { currentRunNo++; controllerWindow.setRunNo(currentRunNo); }
			}
		}
		else if (cmd.equals("selectedr")) {		// reader select file
			JFileChooser filechooser = new JFileChooser();		 	// open file chooser
			filechooser.setFileSelectionMode(JFileChooser.FILES_ONLY);			
			if(controllerWindow.getWorkingDir() != null && controllerWindow.getWorkingDir() != "") {
				filechooser.setCurrentDirectory(new File(controllerWindow.getWorkingDir()));
			}
			int selected = filechooser.showOpenDialog(controllerWindow);
			if (selected == JFileChooser.APPROVE_OPTION) {
				File file = filechooser.getSelectedFile();
				controllerWindow.setEDRFilename(file.getAbsolutePath());
				loadEDRFile();					// read	EDR file		
			} else if (selected == JFileChooser.CANCEL_OPTION || selected == JFileChooser.ERROR_OPTION) { }
		}
		else if (cmd.equals("loadfile")) {		// load file
			if (controllerWindow.getEDRFilename() != "") { loadEDRFile(); }
		}
		else if (cmd.equals("readrange")) {		// reader change 2D Z range
			if (controllerWindow.isEDRAutoRange()) {
				reader.setRangeMin(0);	reader.setRangeMax(0);	reader.setRangeAuto();
			} else {
				reader.setRangeMin(controllerWindow.getEDRRangeMin());
				reader.setRangeMax(controllerWindow.getEDRRangeMax());
			}
			controllerWindow.setEDRImage2D(reader.get2DImage());
			controllerWindow.setEDRRangeMin(reader.getRangeMin());
			controllerWindow.setEDRRangeMax(reader.getRangeMax());
		}
	} // end of actionPerform
	// mouse listener for EDR reader
	public void mouseEntered(MouseEvent e) {}
	public void mouseExited(MouseEvent e)  {}
	public void mousePressed(MouseEvent e) {}
	public void mouseReleased(MouseEvent e){
		if(controllerWindow.isEDRSelected()){
			controllerWindow.setEDRSelectedNeutronCount(reader.getCountIn(controllerWindow.getEDRSelectedArea()));
			reader.setSelectedArea(controllerWindow.getEDRSelectedArea());
			reader.setSelected(true);
		}
	}
	public void mouseClicked(MouseEvent e) {
		reader.setSelected(false);
		controllerWindow.setEDRSelectedNeutronCount(0);
	}

	public void loadEDRFile() {			// read	EDR file
		reader.setLLD(controllerWindow.getEDRLLD());
		reader.setXYLimit(controllerWindow.getEDRXYLimit());
		reader.setTOFEnd(controllerWindow.getEDRTOFEnd());
		reader.readEDRFile(controllerWindow.getEDRFilename());
		controllerWindow.setEDRImage2D(reader.get2DImage());
		controllerWindow.setEDRImagePH(reader.getPulseHeightImage());
		controllerWindow.setEDRImageTOF(reader.getTOFImage());
		controllerWindow.setEDRNeutronCount(reader.getNeutronCount());
		controllerWindow.setEDRKPCount(reader.getKPCount());
		controllerWindow.setEDRTimeIDCount(reader.getTimeIDCount());
		long st = reader.getTimeIDStart(); long et = reader.getTimeIDEnd();
		Instant is = Instant.ofEpochSecond(st+1199145600);
		Instant ie = Instant.ofEpochSecond(et+1199145600);
		LocalDateTime lts = LocalDateTime.ofInstant(is, ZoneId.systemDefault());
		LocalDateTime lte = LocalDateTime.ofInstant(ie, ZoneId.systemDefault());
		controllerWindow.setEDRStartTime(lts.format(dtf));
		controllerWindow.setEDREndTime(lte.format(dtf));
		controllerWindow.setEDRDAQTime((int)(et-st));
		controllerWindow.setEDRRangeMin(reader.getRangeMin());
		controllerWindow.setEDRRangeMax(reader.getRangeMax());
		controllerWindow.setEDRTOFEnd(controllerWindow.getEDRTOFEnd());
	}
	// RPMTControllerUpdater inner class
	class RPMTControllerUpdater implements Runnable {
		int c0 = 0;
		long ct0 = startTimeMillis;
		public void run() {
			try {
				while (rpmt.getRunningStatus()) {
					long ct = System.currentTimeMillis();	// Elasped time counter
					controllerWindow.setDAQTime( (int)((ct - startTimeMillis)/1000L) );
					if (controllerWindow.isAutoRange()) {					// update range
						rpmt.setRangeAuto(); 
						controllerWindow.setRangeMin(rpmt.getRangeMin());
						controllerWindow.setRangeMax(rpmt.getRangeMax());
					} else {
						rpmt.setRangeMin(controllerWindow.getRangeMin());
						rpmt.setRangeMax(controllerWindow.getRangeMax());
					}
					controllerWindow.setImage2D(rpmt.get2DImage());			// update images
					controllerWindow.setImagePH(rpmt.getPulseHeightImage());
					controllerWindow.setImageTOF(rpmt.getTOFImage());
					int c = rpmt.getNeutronCount();
					controllerWindow.setNeutronCount(c);// update counts
					double rate = ((double)(c-c0)/(double)(ct-ct0))*1000.;
					controllerWindow.setNeutronRate(rate);// update rate
					c0 = c;	ct0 = ct;
					controllerWindow.setKPCount(rpmt.getKPCount());
					controllerWindow.setTimeIDCount(rpmt.getTimeIDCount());
					Thread.sleep(500);	// sleep 0.5 s
				}
			} catch (Exception e) {}
			controllerWindow.setRunStatus(false);	// reset 'start/stop' button
			controllerWindow.setEndTime(LocalDateTime.now().format(dtf));
			long ct = System.currentTimeMillis();
			controllerWindow.setDAQTime( (int)((ct - startTimeMillis)/1000L) );
		}
	} // end of RPMTControllerUpdater
} // end of RPMTControllerController

////////// UDP process class /////////////
class UDPProcess {
	static final int READ  = 0;		static final int WRITE = 1;
	byte[] buffer = new byte[2048];			// data buffer	
	int error = 0;
	public UDPProcess() { error = 0; }			// constructor
	// process UDP
	public byte[] execute(int mode, String addr, int port, int memaddr, byte[] command, int length)
	{
		int i = 0;	int len = 0;	// total data length
		int num = buffer.length;	byte[] result;
		buffer[0] = (byte)0xff;
		if (mode == WRITE) { buffer[1] = (byte)0x80; }				// write mode
		else {                   buffer[1] = (byte)0xC0; }			// read mode
		buffer[2] = (byte)0x01;	 buffer[3] = (byte)(length&0xff);	// length, address
		buffer[4] = (byte)((memaddr>>24)&0xff);		buffer[5] = (byte)((memaddr>>16)&0xff);
		buffer[6] = (byte)((memaddr>>8)&0xff);		buffer[7] = (byte)(memaddr&0xff);
		// copy command to buffer
		if (mode == WRITE) {
			i = 0; while (i < length) { buffer[i+8] = command[i]; i++; }
			len = length + 8;
		} else { len = 8; }
		// open UDP socket and send packet
		try {
			DatagramSocket dss = new DatagramSocket();	// UDP socket, send packet, and recieve packet
			DatagramPacket dps = new DatagramPacket(buffer, len, new InetSocketAddress(addr,port));
			DatagramPacket dpr = new DatagramPacket(buffer, length+8);
			dss.send(dps);									// send packet
			i = 0; while (i < num) { buffer[i] = 0; i++; }	// clear buffer
			dss.receive(dpr);								// recieve packet
			if (dpr.getLength() < 0) { System.out.printf("recieve error\n"); }
			dss.close();									// close socket			
		} catch (Exception e) {	error = 1; System.out.println(e.toString()); }
		result = new byte[length+8];						// copy to result
		i = 0;	while (i < length) { result[i] = buffer[i+8]; i++; }
		return result;
	} // end of processUDP
	public void setError(int e) { error = e;    }	// error accessor
	public int  getError()      { return error; }	
} // end of UDPProcess class

////////// Gatenet module class //////////
class Gatenet {
	int error = 0;	UDPProcess udp;
	public Gatenet() { error = 0; udp = new UDPProcess(); }		// constructor
	// get information of Gatenet
	byte[] getInfo(String addr, int port) {
		byte[] info1 = new byte[160];	byte[] info2  = new byte[48];
		byte[] command = new byte[1];	byte[] info  = new byte[208];	int i = 0;
		try {
			command[0] = (byte)0x5a;	// write command to get information, (0x100, 0x5a, 1)
			udp.execute(UDPProcess.WRITE, addr, port, 0x100, command, 1);
			Thread.sleep(10); 			// sleep 10ms
			command[0] = (byte)0x0;		// read command to info1, from address 0x00, length 160
			info1 = udp.execute(UDPProcess.READ, addr, port, 0x00, command, 160);
			Thread.sleep(10); 		// sleep 10ms, read to info2, from address 0x180, length 48
			info2 = udp.execute(UDPProcess.READ, addr, port, 0x180, command, 48);
			error = udp.getError();
		} catch (Exception e) { error = 1; }
		i = 0;	while (i < 160) {	info[i] = info1[i];	    i++; }	// copy data
		i = 0;	while (i < 48)  {	info[i+160] = info2[i]; i++; }
		return info;
	} // end of getInfo
	// get String of Gatenet informations
	String getInfoString(String addr, int port) {
		String resultStr = "";	int i = 0;	byte[] info;
		info = getInfo(addr, port);				// get GATENET information
		int crate = (int)(info[170]&0xff);		// make values
		long kpid = ((long)(info[171]&0xff))*4294967296L + ((long)(info[172]&0xff))*(long)16777216L 
					+ ((long)(info[173]&0xff))*(long)65536L + ((long)(info[174]&0xff))*(long)256L 
					+ ((long)(info[175]&0xff));
		long tmck = ((long)(info[192]&0xff))*4294967296L + ((long)(info[193]&0xff))*16777216L 
					+ ((long)(info[194]&0xff))*65536L + ((long)(info[195]&0xff))*256L 
					+ ((long)(info[196]&0xff));
		long usedhour  = ((long)(info[124]&0xff))*16777216L + ((long)(info[125]&0xff))*65536L
					+ ((long)(info[126]&0xff))*256L + ((long)(info[127]&0xff));
		int overflow   = (int)(info[156]&0xff);
		long restevent = (long)(info[156]&0xff)*16777216L + (long)(info[157]&0xff)*65536L
					+ (long)(info[158]&0xff)*256L + (long)(info[159]&0xff);
		    restevent  = restevent&0x7fffff;
		byte[] version = new byte[42];
		i = 0; while (i < 42) {
			if (info[i+82] >= 0x20 && info[i+82] <= 0x7F) { version[i] = info[i+82]; }
			else { version[i] = 0x20; }
			i++;
		}
		String versionStr = "";
		try {	versionStr = new String(version, "US-ASCII");	// version string byte -> ascii
		} catch (Exception e) {}
		long instrumenttime = (long)(info[176]&0xff)*281474976710656L 
							+ (long)(info[177]&0xff)*1099511627776L + (long)(info[178]&0xff)*4294967296L 
							+ (long)(info[179]&0xff)*16777216L + (long)(info[180]&0xff)*65536L
							+ (long)(info[181]&0xff)*256 + (long)(info[182]&0xff);
		long readinstrumenttime = instrumenttime/67108864L;
		long t = readinstrumenttime*1000 + 1199145600000L;	// make time 
		Date d = new Date(t);	Calendar cal = Calendar.getInstance();	cal.setTime(d); // local time
		int year = cal.get(Calendar.YEAR);		int mon  = cal.get(Calendar.MONTH)+1;
		int mday = cal.get(Calendar.DATE);		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int min  = cal.get(Calendar.MINUTE);	int sec  = cal.get(Calendar.SECOND);
		// result string
		resultStr += String.format("%s\n", versionStr);
		resultStr += String.format("MAC address  = %02x:%02x:%02x:%02x:%02x:%02x\n", 
						(int)(info[128]&0xff),(int)(info[129]&0xff),(int)(info[130]&0xff),
						(int)(info[131]&0xff),(int)(info[132]&0xff),(int)(info[133]&0xff));
		resultStr += String.format("IP address   = %d.%d.%d.%d\n",(int)(info[146]&0xff),
						(int)(info[147]&0xff),(int)(info[148]&0xff),(int)(info[149]&0xff));
		resultStr += String.format("TCP port = %d, ", 
						((int)(info[150]&0xff)*256+(int)(info[151]&0xff)));
		resultStr += String.format("mss = %d, ",((int)(info[152]&0xff)*256+(int)(info[153]&0xff)));
		resultStr += String.format("UDP port = %d\n\n", ((int)(info[154]&0xff)*256+(int)(info[155]&0xff)));
		resultStr += String.format("KP ID                = %d\n\n", kpid);
		resultStr += String.format("read instrument time = %d.%02d.%02d %02d:%02d:%02d\n\n", 
													year, mon, mday, hour, min, sec);
		resultStr += String.format("instrment time = %d\n", instrumenttime);
		resultStr += String.format("used hour      = %d\n", usedhour);
		resultStr += String.format("over flow      = %d, rest events    = %d\n", overflow, restevent);
		resultStr += String.format("crate          = %d, tmck           = %d\n", crate, tmck);
		System.out.printf("Gatenet time   : %d.%02d.%02d %02d:%02d:%02d\n", year, mon, mday, hour, min, sec);
		return resultStr;
	} // end of getInfoString
	// set informations to Gatenet
	int  setInfo(String addr, int port, int setKP, long kpid) {
		int i = 0;						byte[] info  = new byte[208];
		byte[] info1 = new byte[128];	byte[] info2 = new byte[48];
		try {
			info = getInfo(addr, port);	// get current information
			Thread.sleep(10); 			// sleep 10ms			// current KPID
			long kpid0 = ((long)info[171]&0xff)*4294967296L + ((long)info[172]&0xff)*16777216L 
				+ ((long)info[173]&0xff)*65536L + ((long)info[174]&0xff)*256L + ((long)info[175]&0xff);	
			long t0 = 1199145600L;
			long t1 = System.currentTimeMillis()/1000;	// current unix time
			long dt = t1 - t0;	dt = dt*4;
			// replace info[] with new data, from address 160 , begin is ON 
			info[160] = (byte)0x00;	info[161] = (byte)0x80;	info[162] = (byte)0x00;	info[163] = (byte)0x80;
			if (setKP <=0) { kpid = kpid0; } 			// set KPID or not
			info[171] = (byte)((kpid/4294967296L)&0xff);	info[172] = (byte)((kpid/16777216L)&0xff);
			info[173] = (byte)((kpid/65536L)&0xff);			info[174] = (byte)((kpid/256L)&0xff);	
			info[175] = (byte)(kpid&0xff);				// from info[176], time from 2008/1/1 9:00
			info[176] = (byte)((dt/16777216L)&0xff);		info[177] = (byte)((dt/65536L)&0xff);
			info[178] = (byte)((dt/256L)&0xff);				info[179] = (byte)(dt&0xff);
			info[180] = (byte)0x00;		info[181] = (byte)0x00;		info[182] = (byte)0x00;
			//	info[192] = (tmck/4294967296)&0xff;	info[193] = (tmck/16777216)&0xff; // tmck
			//	info[194] = (tmck/65536)&0xff;	info[195] = (tmck/256)&0xff;	info[196] = tmck&0xff;
			i = 0; while (i < 128) { info1[i] = info[i];     i++; } // copy data */
			i = 0; while (i < 48)  { info2[i] = info[i+160]; i++; }
			udp.execute(UDPProcess.WRITE, addr, port, 0x180, info2, 48);	// write command to info2, 
			Thread.sleep(10); 			// sleep 10ms					from address 0x180, length 48
			udp.execute(UDPProcess.WRITE, addr, port, 0x00, info1, 128);	// read command to info1, 
			Thread.sleep(10); 			// sleep 10ms					from address 0x00, length 128
			byte[] command = new byte[1];	command[0] = (byte)0x5b;	// write command
			udp.execute(UDPProcess.WRITE, addr, port, 0x100, command, 1);
			Thread.sleep(250); 			// sleep 250ms
			error = udp.getError();
		} catch (Exception e) { error = 1; }
		return 0;
	} // end of setInfo
	public void setError(int e) { error = e; udp.setError(e); }	// error accessor
	public int  getError()      { return error; }	
} // end of Gatenet class

////////// RPMT module class //////////
class RPMT {
	static final int pixelNum2D = 65536;	static final int pixelNum1D = 256;
	String RPMTAddress  = "192.168.0.16";	// default IP address for RPMT
	int    RPMTPort     = 23;				// default port for RPMT
	int MAX_DATA_LENGTH = 100000;			// Maximum data length
	byte[] reqcommand   = new byte[8];		// Request command
	int    eventdatalength = 0;				// Total data length
	byte[] evdlength  = new byte[4];		// Return value for data length request
	byte[] eventdata  = new byte[MAX_DATA_LENGTH];	// Return event data 
	int ret = 0;        int retsum = 0;    int retsumtmp = 0;	// Return data size
	int num_n = 0;      int num_t0 = 0;    int num_ti = 0;
	int j = 0;          int k = 0;         int num = 0;
	long startTime = 0; long endTime = 0;  long daqTime = 10000;
	boolean running   = false;				// running flag
	int     runNo     = 0;					// run number for filename
	String  workingDir = "";				// working directory
	int limitTime = 0;	 int limitKP = 0;  int limitCount = 0; // Limit time, KP, count
	int lld       = 128; int xylimit = 32; int RPMTPHA = 4096; //int RPMTPHA = 1024; // LLD, XYLimit, PHA Max
	int tofe      = 40;						// tof end time
	int[] imageArray = new int[pixelNum2D];	// 256*256 2D histogram
	int[] phArray    = new int[pixelNum1D];	// 1D histogram
	int[] tofArray   = new int[pixelNum1D];	// 1D histogram
	boolean range2Dauto = true;				// 2D auto range
	int max2D = 1; int min2D = 65536; int maxTOF = 1; int maxPH = 1;
	BufferedImage bufferefImage2D  = null;
	BufferedImage bufferefImagePH  = null;
	BufferedImage bufferefImageTOF = null;
	int[] col = {0x263EA8,0x1652CD,0x1064DC,0x1372D9,0x1581D6,0x0D8FD2,0x099DCC,0x0DA7C3,0x1EAFB3,0x2EB7A4,
				 0x53BA92,0x74BD81,0x95BE70,0xB3BD65,0xD1BB59,0xE2C04B,0xF4C63B,0xFDD22C,0xFBE61E,0xF9F911};
	// constructor
	public RPMT() {
		// initialize request command
		reqcommand[0] = (byte)0xa3; reqcommand[1] = 0; reqcommand[2] = 0;
		reqcommand[3] = 0;          reqcommand[4] = 0; reqcommand[5] = 0;    
		reqcommand[6] = (byte)((MAX_DATA_LENGTH/512)&0xff);
		reqcommand[7] = (byte)((MAX_DATA_LENGTH/2)&0xff);
		running = false;
		// images
		bufferefImage2D  = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		bufferefImagePH  = new BufferedImage(280, 130, BufferedImage.TYPE_INT_RGB);
		bufferefImageTOF = new BufferedImage(280, 130, BufferedImage.TYPE_INT_RGB);
	}
	// Accessor methods
	public void setAddressPort(String addr, int p) { RPMTAddress = addr; RPMTPort = p; }
	public void setRunNo(int n)         { runNo      = n;  }
	public void setWorkingDir(String d) { workingDir = d;  }
	public void setLimitTime(int t)     { limitTime  = t;  }
	public void setLimitKP(int kp)      { limitKP    = kp; }
	public void setLimitCount(int c)    { limitCount = c;  }
	public void setXYLimit(int xy)      { xylimit    = xy; }	
	public void setLLD(int l)           { lld        = l;  }
	public void setTOFEnd(int t)        { tofe       = t;  }
	public boolean getRunningStatus()   { return running;  }	// running status
	public int  getNeutronCount()       { return num_n;    }
	public int  getKPCount()            { return num_t0;   }
	public int  getTimeIDCount()        { return num_ti;   }
	public void setRangeAuto()          { range2Dauto = true;             }
	public void setRangeMin(int n)      { range2Dauto = false; min2D = n; }
	public void setRangeMax(int n)      { range2Dauto = false; max2D = n; }
	public int  getRangeMin()           { return min2D;                   }
	public int  getRangeMax()           { return max2D;                   }
	// set LLD and time 
	public void setLLDTime(int[] lldtime) {
		UDPProcess udp = new UDPProcess();				byte[] command = new byte[8];
		command[0] = (byte)((lldtime[0]/256)&0xff);		command[1] = (byte)(lldtime[0]&0xff);	// LLD
		command[2] = (byte)((lldtime[2]/65536)&0xff);	// Time High
		command[3] = (byte)((lldtime[2]/256)&0xff);		command[4] = (byte)((lldtime[2])&0xff);
		command[5] = (byte)((lldtime[1]/65536)&0xff); 	// Time Low
		command[6] = (byte)((lldtime[1]/256)&0xff);		command[7] = (byte)((lldtime[1])&0xff);
		udp.execute(1, RPMTAddress, RPMTPort, 0x198, command, 8);// process UDP, 1 for WRITE, 8 for length
	}
	// run control
	public void startRun() {	// start run
		if (running == false) { // if not running
			running = true; num_n = 0; num_t0 = 0; num_ti = 0;	// reset number of events
			maxPH = 1; maxTOF = 1; max2D = 1; min2D = 65536;
			int i = 0;	while (i<pixelNum1D) { phArray[i]=0; tofArray[i]=0; i++; }
			    i = 0;	while (i<pixelNum2D) { imageArray[i]=0; i++; }
			new Thread(new RPMTRunner()).start();	// Thread start
		}
	}
	public void stopRun() {		// stop run
		if (running == true) { running = false; }	// if running, Thread stop
	}
	public BufferedImage get2DImage() {				// make and get 2D image
		int i = 0; int num = 65536; int c = 0;
		if (range2Dauto == true) {			// make max and min
			while (i < num) {
				if (imageArray[i] > max2D) { max2D = imageArray[i]; }
				if (imageArray[i] < min2D) { min2D = imageArray[i]; }
				i++;
			}
		}
		i = 0;
		while (i < num) {
			if (imageArray[i] <= min2D) { bufferefImage2D.setRGB(i%pixelNum1D, 255-i/pixelNum1D, col[0]); }
			else { c = 20*(imageArray[i]-min2D)/(max2D-min2D); if (c >= 20) { c = 19; } 
				bufferefImage2D.setRGB(i%pixelNum1D, 255-i/pixelNum1D, col[c]); // flip Y-axis
			}
			i++;
		}
		return bufferefImage2D;
	}
	public BufferedImage getPulseHeightImage() {	// make and get PH image
		int i = 0; int num = pixelNum1D;	maxPH = 1;
		while (i < num) { if (phArray[i] > maxPH) { maxPH = phArray[i]; } i++; } // make max
		Graphics g=bufferefImagePH.createGraphics();
		g.setColor(Color.white); g.fillRect(0,0,280,130);
		g.setColor(Color.LIGHT_GRAY);	g.drawLine(64, 0, 64, 130);
		g.drawLine(128, 0, 128, 130);	g.drawLine(192, 0, 192, 130);	g.drawLine(256, 0, 256, 130);
		g.setColor(Color.black); 		g.drawRect(0,0,279,129);
		g.setColor(Color.blue);
		i = 0;
		while (i < num-1) {
			g.drawLine(i,   130-128*phArray[i]/maxPH, i+1, 130-128*phArray[i]/maxPH);
			g.drawLine(i+1, 130-128*phArray[i]/maxPH, i+1, 130-128*phArray[i+1]/maxPH);
			i++;
		}
		g.drawLine(i, 130-128*phArray[i]/maxPH, i+1, 130);
		g.dispose();
		return bufferefImagePH;
	}
	public BufferedImage getTOFImage() {	// make and get TOF image
		int i = 0; int num = pixelNum1D;	maxTOF = 1;
		while (i < num) { if (tofArray[i] > maxTOF) { maxTOF = tofArray[i]; } i++; } // make max
		Graphics g=bufferefImageTOF.createGraphics();
		g.setColor(Color.white); g.fillRect(0,0,280,130);
		g.setColor(Color.LIGHT_GRAY);	g.drawLine(64, 0, 64, 130);
		g.drawLine(128, 0, 128, 130);	g.drawLine(192, 0, 192, 130);	g.drawLine(256, 0, 256, 130);
		g.setColor(Color.black); g.drawRect(0,0,279,129);
		g.setColor(Color.blue);
		i = 0;
		while (i < num-1) {
			g.drawLine(i,   130-128*tofArray[i]/maxTOF, i+1, 130-128*tofArray[i+1]/maxTOF);
			g.drawLine(i+1, 130-128*tofArray[i]/maxTOF, i+1, 130-128*tofArray[i+1]/maxTOF);
			i++;
		}
		g.drawLine(i, 130-128*tofArray[i]/maxTOF, i+1, 130);
		g.dispose();
		return bufferefImageTOF;
	}
	// RPMTRunner inner class for threading
	class RPMTRunner implements Runnable {
		int stored_flag = 0;	// for analysis
		int time0;    int time00;    int psd_num;   int psd_num0;   int module_num; int module_num0;
		int pha_left; int pha_left0; int pha_right; int pha_right0; int pha_total;  int pha_total0;
		double pos;   double pos0;   double pos_x;  double pos_y;
		int x, y, tof; int ph = 0;   int kp = 0;

		public void run() {	// run RPMT DAQ
			try {
				Socket socket = new Socket(RPMTAddress, RPMTPort);
				OutputStream os = socket.getOutputStream();
				InputStream  is = socket.getInputStream();
				FileOutputStream fos;
				if (runNo >0) { fos = new FileOutputStream(new File(workingDir, String.format("rpmt%04d.edr", runNo))); }
				else { fos = new FileOutputStream(new File(workingDir, "rpmt_tmp.edr")); }
				startTime = System.currentTimeMillis();	// get start UNIX time
				retsum = 0; j = 0;
				while (true) {
					os.write(reqcommand, 0, 8); 		// request command
					ret = is.read(evdlength, 0, 4); 	// read data length
					if (ret != 4) { continue; }
					eventdatalength = (int)(evdlength[2]&0xff)*256 + (int)(evdlength[3]&0xff);
					if (ret <= 0 || eventdatalength > MAX_DATA_LENGTH) { continue; }
					if (eventdatalength > 0) {
						eventdatalength *= 2;
						retsumtmp = 0;
						while (retsumtmp < eventdatalength) {
							ret = is.read(eventdata, retsum, eventdatalength);
							retsumtmp += ret;
						} // end of reading eventdata
						retsum += retsumtmp;
					} // end of if eventdatalength <0
					num = retsum/8;		// event number in read data
					j = num - retsumtmp/8;	if (j<0) { j=0; } // event analysis loop
					while (j < num) {
						if (eventdata[j*8] == (byte)0x5a) {	// neutron event
							time0 = (int)(eventdata[j*8+1]&0xff)*(0x10000) + 
									(int)(eventdata[j*8+2]&0xff)*(0x100) + (int)(eventdata[j*8+3]&0xff); // tof
							psd_num    = (int)(eventdata[j*8+4]&0x7); // psd number lower 3 byte 0x07 = 0000111
							module_num = (int)((eventdata[j*8+4]&0xff)>>>3);  // module number
							pha_left   = (int)(eventdata[j*8+5]&0xff)*0x10 + 
										(int)((eventdata[j*8+6]&0xff)>>>4);	 // pulse height of left
							pha_right  = (int)(eventdata[j*8+6]&0xf)*0x100 + 
										(int)(eventdata[j*8+7]&0xff);		 // pulse height of right
							pha_total = (pha_left + pha_right); // total pulse height 0-4096 //*RPMTPHA/4096; 
							pos = (double)pixelNum1D*pha_left/(pha_right+pha_left); // position = L/(L+R)  0-256
	      					// discrimination 
							if ((psd_num==0 || psd_num==1) && (pha_total>=lld && pha_total<=RPMTPHA)){
				        		if (stored_flag == 1){	// if data stored, reconstruct informations
		        					if (((psd_num0==0&&psd_num==1)  || (psd_num0==1&&psd_num==0)) 
				            			&& (Math.abs(time0 - time00) < xylimit)){ // coincidence
							            if(psd_num0==0 && psd_num==1){ pos_x = pos0; pos_y = pos;  } // x->y
			            				if(psd_num0==1 && psd_num==0){ pos_x = pos;  pos_y = pos0; } // y->x
							            num_n++; // fill data into tree
			    				        x   = (int)pos_x; y = (int)pos_y; 				// fill 2D histo
			    				        if (x >=0 && x<pixelNum1D && y>=0 && y<pixelNum1D) {imageArray[x+y*pixelNum1D]++;}
							            ph  = (int)(pha_total/16); // ph array 0-256 // (int)(pha_total/4);
							            if (ph>=0 && ph<pixelNum1D) {phArray[ph]++;}	// fill PH histogram
    			        				tof = (int)(255.*((double)time0*25.e-6)/((double)tofe)); // [ms]->256
										if (tof >=0 && tof<pixelNum1D) {tofArray[tof]++;}		// fill TOF histogram
			    				        stored_flag = 0;		// reset flag
          							}
				        		} // end of stored_flag == 1
								pos0 = pos;	stored_flag = 1;	// if not stored, rise flag
								time00    = time0;    psd_num0   = psd_num;   module_num0 = module_num;
								pha_left0 = pha_left; pha_right0 = pha_right; pha_total0  = pha_total;
      						} // end of discrimination
						} // end of neutron event 0x5a
						else if (eventdata[j*8] == (byte)0x5b) { num_t0++; } // KP event
						else if (eventdata[j*8] == (byte)0x5c) { num_ti++; } // time_id event
						j++;
					} // end of event analysis loop
					if (k >=100) {	// output evry 0.5s
						fos.write(eventdata,0,retsum);			// file output
						k = 0;	retsum = 0;
						endTime = System.currentTimeMillis();	// get current time
						if (running == false) { break; }
						if (limitTime  > 0 && endTime - startTime >= limitTime*1000) { break; }
						if (limitKP    > 0 && num_t0 >= limitKP)   { break; }
						if (limitCount > 0 && num_n >= limitCount) { break; }
					}
					Thread.sleep(1);	// wait 1ms, about 4ms??
					k++;
				} // end of while loop
				fos.close(); os.close(); is.close(); socket.close();	// close stream and socket
			} catch (Exception e) {
				System.out.printf("%s\n", e.getClass().getName() + ": " + e.getMessage());
			} // end of try-catch
			running = false; // turn off running flag			
		}	// end of run
	} // end of RPMTRunner inner class
} // end of RPMT class

////////// EDRReader class //////////
class EDRReader {
	static final int pixelNum2D = 65536;	static final int pixelNum1D = 256;
	int stored_flag = 0;	// for analysis
	int eventdatalength = 0;				// Total data length
	int num_n = 0;      int num_t0 = 0;    int num_ti = 0;
	long startTime = 0; long endTime = 0;  long daqTime = 10000;
	long timeid = 0; long timeid_start = 0; long timeid_end = 0; int timeid_started = 0;
	int lld       = 128; int xylimit = 32; int RPMTPHA = 4096; // LLD, XYLimit, PHA Max
	int tofe      = 40;						// tof end time
	int[] imageArray = new int[pixelNum2D];	// 256*256 2D histogram
	int[] phArray    = new int[pixelNum1D];	// 1D histogram
	int[] tofArray   = new int[pixelNum1D];	// 1D histogram
	boolean range2Dauto = true;				// 2D auto range
	int max2D = 1; int min2D = 65536; int maxTOF = 1; int maxPH = 1;
	BufferedImage bufferefImage2D  = null;
	BufferedImage bufferefImagePH  = null;
	BufferedImage bufferefImageTOF = null;
	// for analysis
	int time0;    int time00;    int psd_num;   int psd_num0;   int module_num; int module_num0;
	int pha_left; int pha_left0; int pha_right; int pha_right0; int pha_total;  int pha_total0;
	double pos;   double pos0;   double pos_x;  double pos_y;
	int x, y, tof; int ph = 0;   int kp = 0;
	int[] col = {0x263EA8,0x1652CD,0x1064DC,0x1372D9,0x1581D6,0x0D8FD2,0x099DCC,0x0DA7C3,0x1EAFB3,0x2EB7A4,
				 0x53BA92,0x74BD81,0x95BE70,0xB3BD65,0xD1BB59,0xE2C04B,0xF4C63B,0xFDD22C,0xFBE61E,0xF9F911};
	byte[] buf = new byte[8];	int readByte = 0;
	boolean selected = false;	int[] selectedArea = new int[4];	// selected area 
	int[] phSelectedArray    = new int[pixelNum1D];		// 1D histogram
	int[] tofSelectedArray   = new int[pixelNum1D];		// 1D histogram

	// constructor
	public EDRReader() {
		// images
		bufferefImage2D  = new BufferedImage(256, 256, BufferedImage.TYPE_INT_RGB);
		bufferefImagePH  = new BufferedImage(280, 130, BufferedImage.TYPE_INT_RGB);
		bufferefImageTOF = new BufferedImage(280, 130, BufferedImage.TYPE_INT_RGB);
	}
	public void readEDRFile(String f) {		// edr file reader
		maxPH = 1; maxTOF = 1; if (range2Dauto) {max2D = 1; min2D = 0;}
		try{
			File file = new File(f);
			DataInputStream dataInStream = new DataInputStream( new BufferedInputStream( new FileInputStream(f)));
			int i=0; while (i < 65536) {imageArray[i]=0;i++;}
			i=0; while (i < 256) {phArray[i]=0; tofArray[i]=0; phSelectedArray[i]=0; tofSelectedArray[i]=0; i++;}
			num_n=0;	num_t0=0;	num_ti=0;	timeid_start = 0; timeid_end = 0; timeid_started = 0;
			while (0 < (readByte = dataInStream.read(buf)) ) {
				if (buf[0] == (byte)0x5a) {	// neutron event
					time0 = (int)(buf[1]&0xff)*(0x10000) + 
							(int)(buf[2]&0xff)*(0x100) + (int)(buf[3]&0xff); // tof
					psd_num    = (int)(buf[4]&0x7); // psd number lower 3 byte 0x07 = 0000111
					module_num = (int)((buf[4]&0xff)>>>3);  // module number
					pha_left   = (int)(buf[5]&0xff)*0x10 +(int)((buf[6]&0xff)>>>4); // pulse height of left
					pha_right  = (int)(buf[6]&0xf)*0x100 +(int)(buf[7]&0xff);		// pulse height of right
					pha_total = (pha_left + pha_right); // total pulse height 0-4096 // *RPMTPHA/4096;
					pos = (double)pixelNum1D*pha_left/(pha_right+pha_left); // position = L/(L+R)  0-256
	      			// discrimination 
					if ((psd_num==0 || psd_num==1) && (pha_total>=lld && pha_total<=RPMTPHA)){
						if (stored_flag == 1){	// if data stored, reconstruct informations
		       				if (((psd_num0==0&&psd_num==1)  || (psd_num0==1&&psd_num==0)) 
		            			&& (Math.abs(time0 - time00) < xylimit)){ // coincidence
								if(psd_num0==0 && psd_num==1){ pos_x = pos0; pos_y = pos;  } // x->y
			            		if(psd_num0==1 && psd_num==0){ pos_x = pos;  pos_y = pos0; } // y->x
			    				x   = (int)pos_x; y = (int)pos_y; num_n++;		// fill data into tree
			    				if (x >=0 && x<pixelNum1D && y>=0 && y<pixelNum1D) {imageArray[x+y*pixelNum1D]++;} // fill 2D histo
							    ph  = (int)(pha_total/16);	// ph array 0-256 //(pha_total/4);
							    if (ph>=0 && ph<pixelNum1D) { phArray[ph]++;			// fill PH histogram
							    	if (selectedArea[0]<=x&&selectedArea[2]>x&&selectedArea[1]<=y&&selectedArea[3]>y) {
							    		phSelectedArray[ph]++; }							    
							    }
    			        		tof = (int)(255.*((double)time0*25.e-6)/((double)tofe)); // [ms]->256
								if (tof >=0 && tof <pixelNum1D) { tofArray[tof]++;		// fill TOF histogram
									if (selectedArea[0]<=x&&selectedArea[2]>x&&selectedArea[1]<=y&&selectedArea[3]>y) {
							    		tofSelectedArray[tof]++; }
								}
			    				stored_flag = 0;		// reset flag
          					}
				        } // end of stored_flag == 1
						pos0 = pos;	stored_flag = 1;	// if not stored, rise flag
						time00    = time0;    psd_num0   = psd_num;   module_num0 = module_num;
						pha_left0 = pha_left; pha_right0 = pha_right; pha_total0  = pha_total;
      				} // end of discrimination
				} // end of neutron event 0x5a
				else if (buf[0] == (byte)0x5b) { num_t0++; } // KP event
				else if (buf[0] == (byte)0x5c) {			 // time_id event
					timeid = ((int)(buf[1]&0xff))/4*256*256*256
							+ (((int)(buf[1]&0xff))%4*64 + ((int)(buf[2]&0xff))/4 )*256*256
							+ (((int)(buf[2]&0xff))%4*64 + ((int)(buf[3]&0xff))/4 )*256
							+ (((int)(buf[3]&0xff))%4*64 + ((int)(buf[4]&0xff))/4 );
					if (timeid_started <=0) { timeid_start = timeid; timeid_started = 1; }
					num_ti++;
				} // end of time_id event
			} // end of file read while loop
			timeid_end = timeid;
			dataInStream.close();
			System.out.printf("Open EDR file  : %s\n", file.getAbsolutePath());
		} catch (IOException e) { }
	} // end of readEDRFile
	// accessor methods
	public void setXYLimit(int xy)    { xylimit    = xy;     }	
	public void setLLD(int l)         { lld        = l;      }
	public void setTOFEnd(int t)      { tofe       = t;      }
	public int  getNeutronCount()     { return num_n;        }
	public int  getKPCount()          { return num_t0;       }
	public int  getTimeIDCount()      { return num_ti;       }
	public long getTimeIDStart()      { return timeid_start; }
	public long getTimeIDEnd()        { return timeid_end;   }
	public void setRangeAuto()        { range2Dauto = true;             }
	public void setRangeMin(int n)    { range2Dauto = false; min2D = n; }
	public void setRangeMax(int n)    { range2Dauto = false; max2D = n; }
	public int  getRangeMin()         { return min2D;                   }
	public int  getRangeMax()         { return max2D;                   }
	public BufferedImage get2DImage() {		
		int i = 0; int num = pixelNum2D; int c = 0;
		if (range2Dauto == true) {			// make max and min
			while (i < num) {
				if (imageArray[i] > max2D) { max2D = imageArray[i]; }
				if (imageArray[i] < min2D) { min2D = imageArray[i]; }
				i++;
			}
		}
		i = 0;
		while (i < num) {
			if (imageArray[i] <= min2D) { bufferefImage2D.setRGB(i%256, 255-i/256, col[0]); }
			else { c = 20*(imageArray[i]-min2D)/(max2D-min2D); if (c >= 20) { c = 19; }
				bufferefImage2D.setRGB(i%256, 255-i/256, col[c]); // flip Y-axis
			}
			i++;
		}
		return bufferefImage2D;
	}
	public BufferedImage getPulseHeightImage() {
		int i = 0; int num = pixelNum1D;
		while (i < num) { if (phArray[i] > maxPH) { maxPH = phArray[i]; } i++; } // make max
		Graphics g=bufferefImagePH.createGraphics();
		g.setColor(Color.white);		g.fillRect(0,0,280,130);
		g.setColor(Color.LIGHT_GRAY);	g.drawLine(64, 0, 64, 130);
		g.drawLine(128, 0, 128, 130);	g.drawLine(192, 0, 192, 130);	g.drawLine(256, 0, 256, 130);
		g.setColor(Color.black);		g.drawRect(0,0,279,129);
		g.setColor(Color.blue);
		i = 0;
		while (i < num-1) {
			g.drawLine(i,   130-128*phArray[i]/maxPH, i+1, 130-128*phArray[i]/maxPH);
			g.drawLine(i+1, 130-128*phArray[i]/maxPH, i+1, 130-128*phArray[i+1]/maxPH);
			i++;
		}
		g.drawLine(i, 130-128*phArray[i]/maxPH, i+1, 130);
		if (selected) {
			g.setColor(Color.red);
			i = 0;
			while (i < num-1) {
				g.drawLine(i,   130-128*phSelectedArray[i]/maxPH, i+1, 130-128*phSelectedArray[i]/maxPH);
				g.drawLine(i+1, 130-128*phSelectedArray[i]/maxPH, i+1, 130-128*phSelectedArray[i+1]/maxPH);
				i++;
			}
			g.drawLine(i, 130-128*phSelectedArray[i]/maxPH, i+1, 130);
		}
		g.dispose();
		return bufferefImagePH;
	}
	public BufferedImage getTOFImage() {
		int i = 0; int num = pixelNum1D;
		while (i < num) { if (tofArray[i] > maxTOF) { maxTOF = tofArray[i]; } i++; } // make max
		Graphics g=bufferefImageTOF.createGraphics();
		g.setColor(Color.white); g.fillRect(0,0,280,130);
		g.setColor(Color.LIGHT_GRAY);	g.drawLine(64, 0, 64, 130);
		g.drawLine(128, 0, 128, 130);	g.drawLine(192, 0, 192, 130);	g.drawLine(256, 0, 256, 130);
		g.setColor(Color.black); g.drawRect(0,0,279,129);
		g.setColor(Color.blue);
		i = 0;
		while (i < num-1) {
			g.drawLine(i,   130-128*tofArray[i]/maxTOF, i+1, 130-128*tofArray[i+1]/maxTOF);
			g.drawLine(i+1, 130-128*tofArray[i]/maxTOF, i+1, 130-128*tofArray[i+1]/maxTOF);
			i++;
		}		
		g.drawLine(i, 130-128*tofArray[i]/maxTOF, i+1, 130);
		if (selected) {
			g.setColor(Color.red);
			i = 0;
			while (i < num-1) {
				g.drawLine(i,   130-128*tofSelectedArray[i]/maxTOF, i+1, 130-128*tofSelectedArray[i]/maxTOF);
				g.drawLine(i+1, 130-128*tofSelectedArray[i]/maxTOF, i+1, 130-128*tofSelectedArray[i+1]/maxTOF);
				i++;
			}
			g.drawLine(i, 130-128*tofSelectedArray[i]/maxTOF, i+1, 130);
		}
		g.dispose();
		return bufferefImageTOF;
	}
	public int getCountIn(int[] area) { // neutron count in selected area 
		int i = 0; int sx=area[0]/2; int sy=256-area[3]/2; int ex=area[2]/2; int ey=256-area[1]/2; int snum = 0;
		while (i < 65536) {
			if (i%256>=sx&&i%256<ex&&i/256>=sy&&i/256<ey) { snum +=imageArray[i]; }
			i++;
		}
		return snum;
	}
	public void setSelected(boolean b) { selected  = b; }
	public void setSelectedArea(int[] s) {
		selectedArea[0] = s[0]/2;	selectedArea[1] = 256-s[3]/2;
		selectedArea[2] = s[2]/2;	selectedArea[3] = 256-s[1]/2;
	}
} // end of EDRReader

////////// View class //////////
class RPMTControllerWindow extends JFrame implements ActionListener {//, ChangeListener {
	// tab 1
	JTextField gatenetAddressTextField = null;	JTextField gatenetPortTextField = null;
	JTextArea  gatenetInfoTextArea     = null;	JButton    readInfoButton       = null;
	JCheckBox  kpResetCheckBox         = null;	JButton    setInfoButton        = null;
	JTextField rpmtSetLLDAddressTextField = null; JTextField rpmtSetLLDPortTextField = null;
	JTextField rpmtSetLLDTextField     = null;	JTextField rpmtSetTimeHighTextField  = null;
	JTextField rpmtSetTimeLowTextField = null;	JButton    setLLDTimeButton          = null;
	// tab2
	JTextField rpmtAddressTextField    = null;	JTextField rpmtPortTextField    = null;
	JCheckBox  limitTimeCheckBox       = null;	JTextField limitTimeTextField   = null;
	JCheckBox  limitKPCheckBox         = null;	JTextField limitKPTextField     = null;
	JCheckBox  limitCountCheckBox      = null;	JTextField limitCountTextField  = null;
	JCheckBox  runNoCheckBox           = null;	JTextField runNoTextField       = null;
	JButton    dirButton               = null;	JTextArea  dirTextArea          = null;
	JButton    runButton               = null;	JPanel     runIndicator         = null; // ver. 0.4
	JLabel     startTimeLabel          = null;	JLabel     endTimeLabel         = null;
	JLabel     daqTimeLabel            = null;	JLabel     neutronCountLabel    = null;
	JLabel     neutronRateLabel        = null;
	JLabel     kpCountLabel            = null;	JLabel     tiCountLabel         = null;
	JTextField lldTextField            = null;	JTextField xyTextField          = null;
	JTextField tofEndTextField         = null;	JLabel tofEndPlotLabel          = null;
	JRadioButton rangeAuto             = null;	JRadioButton rangeManual        = null;
	JTextField rangeMinTextField       = null;	JTextField rangeMaxTextField    = null;
	ImageCanvas image2D  = null; ImageCanvas imagePH  = null; ImageCanvas imageTOF = null;
	// tab3
	JPanel     tabPanel3               = null;
	JTextField readFileTextField       = null;	JButton    fileSelectButton     = null;
	JButton    loadButton              = null;
	JLabel     readStartTimeLabel      = null;	JLabel     readEndTimeLabel     = null;
	JLabel     readDAQTimeLabel        = null;	JLabel     readNeutronCountLabel= null;
	JLabel     readSelectedNeutronCountLabel= null;
	JLabel     readKPCountLabel        = null;	JLabel     readTiCountLabel     = null;
	JTextField readLLDTextField        = null;	JTextField readXYTextField      = null;
	JTextField readTOFEndTextField     = null;	JLabel     readTOFEndPlotLabel  = null;
	JRadioButton readRangeAuto         = null;	JRadioButton readRangeManual    = null;
	JTextField readRangeMinTextField   = null;	JTextField readRangeMaxTextField= null;
	SelectableImageCanvas readImage2D  = null;	JButton    saveEDRButton        = null;
	ImageCanvas readImagePH  = null; ImageCanvas readImageTOF = null;

	// constructor
	public RPMTControllerWindow(String title, int width, int height) {
		super(title);
		setDefaultCloseOperation(EXIT_ON_CLOSE);
		setSize(width,height);
		setLocationRelativeTo(null);
		this.setResizable(false);

		// Tabbed pane
		JTabbedPane tabbedpane = new JTabbedPane();
		
		// step 1: Initialize Gatenet module
		JPanel tabPanel1 = new JPanel();		tabPanel1.setLayout(null);	// invalid layout manager
		JLabel gatenetAddressLabel = new JLabel("GATENET IP address :");
		tabPanel1.add(gatenetAddressLabel);					gatenetAddressLabel.setBounds(10,10,150,25);
		gatenetAddressTextField = new JTextField("192.168.0.15", 50);
		gatenetAddressTextField.setBounds(160,10,120,25);	tabPanel1.add(gatenetAddressTextField);
		gatenetAddressTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel gatenetPortLabel = new JLabel("port :");
		tabPanel1.add(gatenetPortLabel);					gatenetPortLabel.setBounds(300,10,40,25);
		gatenetPortTextField    = new JTextField("4660", 10);
		gatenetPortTextField.setBounds(350,10,80,25);		tabPanel1.add(gatenetPortTextField);
		gatenetPortTextField.setHorizontalAlignment(JTextField.CENTER);		
		readInfoButton = new JButton("Read GATENET Info");
		readInfoButton.setBounds(30,70,155,35);				tabPanel1.add(readInfoButton);
		readInfoButton.setActionCommand("readInfo");
		kpResetCheckBox = new JCheckBox("KP Reset", true);
		kpResetCheckBox.setBounds(230,77,90,20);			tabPanel1.add(kpResetCheckBox);
		setInfoButton = new JButton("Set GATENET Time");
		setInfoButton.setBounds(320,70,155,35);				tabPanel1.add(setInfoButton);
		setInfoButton.setActionCommand("setInfo");
		gatenetInfoTextArea     = new JTextArea(30,5);
		gatenetInfoTextArea.setBounds(20,120,450,300);		tabPanel1.add(gatenetInfoTextArea);
		gatenetInfoTextArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 15));
		gatenetInfoTextArea.setMargin(new Insets(5, 10, 5, 10));
		String msg  = "\n1. Press \"Read GATENET Info\" button.\n   Check the parameters.\n";
			   msg += "2. Turn on the checkbox to reset KP.\n";
			   msg += "3. Press \"Set GATENET Time\" button.\n   Check the local time and KP value.\n";
		gatenetInfoTextArea.setText(msg);
		// RPMT set LLD and time range 
		JLabel rpmtSetLLDLabel = new JLabel("RPMT LLD and Time range");
		tabPanel1.add(rpmtSetLLDLabel);						rpmtSetLLDLabel.setBounds(590,70,250,25);
		JLabel rpmtSetLLDAddressLabel = new JLabel("RPMT IP address :");
		tabPanel1.add(rpmtSetLLDAddressLabel);				rpmtSetLLDAddressLabel.setBounds(600,110,150,25);
		rpmtSetLLDAddressTextField = new JTextField("192.168.0.16", 50);
		rpmtSetLLDAddressTextField.setBounds(730,110,120,25);	tabPanel1.add(rpmtSetLLDAddressTextField);
		rpmtSetLLDAddressTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel rpmtSetLLDPortLabel = new JLabel("port :");
		tabPanel1.add(rpmtSetLLDPortLabel);					rpmtSetLLDPortLabel.setBounds(720,135,40,25);
		rpmtSetLLDPortTextField    = new JTextField("4660", 10);
		rpmtSetLLDPortTextField.setBounds(770,135,80,25);	tabPanel1.add(rpmtSetLLDPortTextField);
		rpmtSetLLDPortTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel rpmtLLDLabel = new JLabel("LLD");
		tabPanel1.add(rpmtLLDLabel);						rpmtLLDLabel.setBounds(620,170,80,25);
		rpmtSetLLDTextField    = new JTextField("128", 10);
		rpmtSetLLDTextField.setBounds(700,170,80,25);		tabPanel1.add(rpmtSetLLDTextField);
		rpmtSetLLDTextField.setHorizontalAlignment(JTextField.RIGHT);
		JLabel rpmtTimeLowLabel = new JLabel("Time Low");
		tabPanel1.add(rpmtTimeLowLabel);					rpmtTimeLowLabel.setBounds(620,200,80,25);
		rpmtSetTimeLowTextField    = new JTextField("0", 10);
		rpmtSetTimeLowTextField.setBounds(700,200,80,25);	tabPanel1.add(rpmtSetTimeLowTextField);
		rpmtSetTimeLowTextField.setHorizontalAlignment(JTextField.RIGHT);
		JLabel rpmtTimeHighLabel = new JLabel("Time High");
		tabPanel1.add(rpmtTimeHighLabel);					rpmtTimeHighLabel.setBounds(620,230,80,25);
		rpmtSetTimeHighTextField    = new JTextField("2000000", 10);
		rpmtSetTimeHighTextField.setBounds(700,230,80,25);	tabPanel1.add(rpmtSetTimeHighTextField);
		rpmtSetTimeHighTextField.setHorizontalAlignment(JTextField.RIGHT);
		JLabel lldinfo1Label = new JLabel("Hardware LLD 0-4096.");
		tabPanel1.add(lldinfo1Label);						lldinfo1Label.setBounds(600,275,300,25);
		JLabel lldinfo2Label = new JLabel("If LLD<128, LLD=128.");
		tabPanel1.add(lldinfo2Label);						lldinfo2Label.setBounds(620,295,300,25);
		JLabel lldinfo21Label = new JLabel("Events < LLD are not transfered to PC.");
		tabPanel1.add(lldinfo21Label);						lldinfo21Label.setBounds(620,315,300,25);
		JLabel lldinfo3Label = new JLabel("Time unit=0.025us. (40MHz)");
		tabPanel1.add(lldinfo3Label);						lldinfo3Label.setBounds(600,345,300,25);
		JLabel lldinfo4Label = new JLabel("1ms=40000, 40ms=1600000.");
		tabPanel1.add(lldinfo4Label);						lldinfo4Label.setBounds(620,365,300,25);
		JLabel lldinfo5Label = new JLabel("\'0\' means no limit.");
		tabPanel1.add(lldinfo5Label);						lldinfo5Label.setBounds(620,385,300,25);
		setLLDTimeButton = new JButton("Set LLD&Time");
		setLLDTimeButton.setBounds(650,430,155,35);				tabPanel1.add(setLLDTimeButton);
		setLLDTimeButton.setActionCommand("setlldtime");
		// info
		JLabel info1 = new JLabel(RPMTController.INFO);
		info1.setBounds(650,580,500,25);				tabPanel1.add(info1);
		
		// step 2: RPMT control
		JPanel tabPanel2 = new JPanel();		tabPanel2.setLayout(null);	// invalid layout manager
		JLabel rpmtAddressLabel = new JLabel("RPMT IP address :");
		tabPanel2.add(rpmtAddressLabel);					rpmtAddressLabel.setBounds(10,10,150,25);
		rpmtAddressTextField = new JTextField("192.168.0.16", 50);
		rpmtAddressTextField.setBounds(160,10,120,25);		tabPanel2.add(rpmtAddressTextField);
		rpmtAddressTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel rpmtPortLabel = new JLabel("port :");
		tabPanel2.add(rpmtPortLabel);						rpmtPortLabel.setBounds(300,10,40,25);
		rpmtPortTextField    = new JTextField("23", 10);
		rpmtPortTextField.setBounds(350,10,80,25);			tabPanel2.add(rpmtPortTextField);
		rpmtPortTextField.setHorizontalAlignment(JTextField.CENTER);		
		JLabel limitLabel = new JLabel("Limit");
		tabPanel2.add(limitLabel);							limitLabel.setBounds(10,38,60,25);
		limitTimeCheckBox = new JCheckBox("Time(s)", false);
		limitTimeCheckBox.setBounds(70,40,80,20);			tabPanel2.add(limitTimeCheckBox);
		limitTimeTextField = new JTextField("100", 50);
		limitTimeTextField.setBounds(150,38,80,25);			tabPanel2.add(limitTimeTextField);
		limitTimeTextField.setHorizontalAlignment(JTextField.CENTER);
		limitKPCheckBox = new JCheckBox("KP", false);
		limitKPCheckBox.setBounds(235,40,50,20);			tabPanel2.add(limitKPCheckBox);
		limitKPTextField = new JTextField("10000", 50);
		limitKPTextField.setBounds(280,38,80,25);			tabPanel2.add(limitKPTextField);
		limitKPTextField.setHorizontalAlignment(JTextField.CENTER);
		limitCountCheckBox = new JCheckBox("Count", false);
		limitCountCheckBox.setBounds(365,40,80,20);			tabPanel2.add(limitCountCheckBox);
		limitCountTextField = new JTextField("10000", 50);
		limitCountTextField.setBounds(440,38,80,25);		tabPanel2.add(limitCountTextField);
		limitCountTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel dirLabel = new JLabel("Working Dir.");
		tabPanel2.add(dirLabel);							dirLabel.setBounds(650,10,80,25);
		dirTextArea     = new JTextArea(15,3);
		dirTextArea.setBounds(650,35,200,70);				tabPanel2.add(dirTextArea);
		dirTextArea.setBorder(new EtchedBorder(EtchedBorder.LOWERED));
		dirTextArea.setLineWrap(true);
		dirButton = new JButton("select");					dirButton.setBounds(775,10,80,25);
		tabPanel2.add(dirButton);							dirButton.addActionListener(this);
		runNoCheckBox = new JCheckBox("Run No.", true);
		tabPanel2.add(runNoCheckBox);						runNoCheckBox.setBounds(530,10,110,25);
		runNoTextField = new JTextField("1", 50);
		runNoTextField.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 18));
		runNoTextField.setBounds(550,32,80,40);				tabPanel2.add(runNoTextField);
		runNoTextField.setHorizontalAlignment(JTextField.CENTER);
		runIndicator = new JPanel();						runIndicator.setLayout(null);
		tabPanel2.add(runIndicator);						runIndicator.setBounds(543,75,96,54);
		runButton = new JButton("Start RUN");
		runButton.setMargin(new Insets(2,2,2,2));	// margin ver 0.4.1
//		runButton.setBounds(545,77,90,50);					tabPanel2.add(runButton);
		runButton.setBounds(2,2,90,50);						runIndicator.add(runButton);
		runButton.setActionCommand("startstoprun");			runButton.addActionListener(this);
		JLabel startLabel = new JLabel("Start ");
		tabPanel2.add(startLabel);							startLabel.setBounds(550,130,70,25);
		startTimeLabel = new JLabel("");
		startTimeLabel.setBounds(590,130,170,25);			tabPanel2.add(startTimeLabel);
		JLabel endLabel = new JLabel("End ");
		tabPanel2.add(endLabel);							endLabel.setBounds(550,155,70,25);
		endTimeLabel = new JLabel("");
		endTimeLabel.setBounds(590,155,170,25);				tabPanel2.add(endTimeLabel);
		JLabel daqLabel = new JLabel("Elapsed time (s)");
		tabPanel2.add(daqLabel);							daqLabel.setBounds(750,120,200,25);
		daqTimeLabel = new JLabel("0");
		daqTimeLabel.setBounds(750,145,100,25);				tabPanel2.add(daqTimeLabel);
		daqTimeLabel.setHorizontalAlignment(JTextField.RIGHT);
		daqTimeLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
		JLabel neutronLabel = new JLabel("Neutron");
		tabPanel2.add(neutronLabel);						neutronLabel.setBounds(550,183,96,25);
		neutronCountLabel = new JLabel("0");
		neutronCountLabel.setBounds(550,203,96,25);			tabPanel2.add(neutronCountLabel);
		neutronCountLabel.setHorizontalAlignment(JTextField.RIGHT);
		neutronCountLabel.setBorder( new LineBorder(Color.LIGHT_GRAY, 1, false));
		neutronRateLabel = new JLabel("0");
		neutronRateLabel.setBounds(550,230,96,25);			tabPanel2.add(neutronRateLabel);
		neutronRateLabel.setHorizontalAlignment(JTextField.RIGHT);
		neutronRateLabel.setBorder( new LineBorder(Color.LIGHT_GRAY, 1, false));
		JLabel rateLabel = new JLabel("Hz");
		tabPanel2.add(rateLabel);							rateLabel.setBounds(650,230,50,25);
		JLabel kpLabel = new JLabel("KP");
		tabPanel2.add(kpLabel);								kpLabel.setBounds(652,183,96,25);
		kpCountLabel = new JLabel("0");
		kpCountLabel.setBounds(652,203,96,25);				tabPanel2.add(kpCountLabel);
		kpCountLabel.setHorizontalAlignment(JTextField.RIGHT);
		kpCountLabel.setBorder( new LineBorder(Color.LIGHT_GRAY, 1, false));
		JLabel tiLabel = new JLabel("Time ID");
		tabPanel2.add(tiLabel);								tiLabel.setBounds(754,183,96,25);
		tiCountLabel = new JLabel("0");
		tiCountLabel.setBounds(754,203,96,25);				tabPanel2.add(tiCountLabel);
		tiCountLabel.setHorizontalAlignment(JTextField.RIGHT);
		tiCountLabel.setBorder( new LineBorder(Color.LIGHT_GRAY, 1, false));		
		JLabel lldLabel = new JLabel("LLD");
		tabPanel2.add(lldLabel);							lldLabel.setBounds(550,262,30,25);
		lldTextField = new JTextField("128", 50);
		lldTextField.setBounds(578,262,50,25);				tabPanel2.add(lldTextField);
		lldTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel xyLabel = new JLabel("XYLimit");
		tabPanel2.add(xyLabel);								xyLabel.setBounds(638,262,55,25);
		xyTextField = new JTextField("32", 50);
		xyTextField.setBounds(690,262,50,25);				tabPanel2.add(xyTextField);
		xyTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel tofEndLabel = new JLabel("TOF end");
		tabPanel2.add(tofEndLabel);							tofEndLabel.setBounds(750,262,65,25);
		tofEndTextField = new JTextField("40", 50);
		tofEndTextField.setBounds(805,262,50,25);			tabPanel2.add(tofEndTextField);
		tofEndTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel zRangeLabel = new JLabel("Z range");
		tabPanel2.add(zRangeLabel);							zRangeLabel.setBounds(70,65,60,25);
		rangeAuto   = new JRadioButton("Auto", true);		rangeAuto.setBounds(120,65,80,25);
		tabPanel2.add(rangeAuto);							rangeAuto.setActionCommand("range");
		rangeManual = new JRadioButton(" ");				rangeManual.setBounds(200,65,40,25);
		tabPanel2.add(rangeManual);							rangeManual.setActionCommand("range");
		ButtonGroup rangeGroup = new ButtonGroup();
		rangeGroup.add(rangeAuto);		rangeGroup.add(rangeManual);
		rangeMinTextField = new JTextField("0", 50);		rangeMinTextField.setBounds(225,66,50,25);
		tabPanel2.add(rangeMinTextField);					rangeMinTextField.setActionCommand("range");
		rangeMinTextField.setHorizontalAlignment(JTextField.CENTER);
		rangeMaxTextField = new JTextField("100", 50);		rangeMaxTextField.setBounds(470,66,50,25);
		tabPanel2.add(rangeMaxTextField);					rangeMaxTextField.setActionCommand("range");
		rangeMaxTextField.setHorizontalAlignment(JTextField.CENTER);
		ColorBarCanvas bar = new ColorBarCanvas();
		bar.setBounds(283,72,180,15);						tabPanel2.add(bar);
		image2D = new ImageCanvas();
		image2D.setBounds(10,90,512,512);					tabPanel2.add(image2D);
		image2D.setBorder( new LineBorder(Color.black, 1, false));
		imagePH = new ImageCanvas();
		imagePH.setBounds(570,292,280,130);					tabPanel2.add(imagePH);
		imageTOF = new ImageCanvas();
		imageTOF.setBounds(570,452,280,130);				tabPanel2.add(imageTOF);
		tofEndPlotLabel = new JLabel("40");
		tabPanel2.add(tofEndPlotLabel);						tofEndPlotLabel.setBounds(820,582,50,25);
		JLabel tofZeroPlotLabel = new JLabel("0");
		tabPanel2.add(tofZeroPlotLabel);					tofZeroPlotLabel.setBounds(568,582,20,25);
		JLabel tofPlotLabel = new JLabel("TOF (ms)");
		tabPanel2.add(tofPlotLabel);						tofPlotLabel.setBounds(672,582,100,25);
		JLabel phPlotLabel = new JLabel("Pulse Height");
		tabPanel2.add(phPlotLabel);							phPlotLabel.setBounds(665,422,120,25);
		JLabel phZeroPlotLabel = new JLabel("0");
		tabPanel2.add(phZeroPlotLabel);						phZeroPlotLabel.setBounds(568,422,20,25);
		JLabel phHighPlotLabel = new JLabel("4096");		//	JLabel phHighPlotLabel = new JLabel("1024");
		tabPanel2.add(phHighPlotLabel);						phHighPlotLabel.setBounds(810,422,40,25);

		// step 3: EDR display
		tabPanel3 = new JPanel();		tabPanel3.setLayout(null);	// invalid layout manager
		JLabel readFileLabel = new JLabel("EDR file :");
		tabPanel3.add(readFileLabel);						readFileLabel.setBounds(10,10,80,25);
		readFileTextField = new JTextField("", 50);
		readFileTextField.setBounds(90,10,600,25);			tabPanel3.add(readFileTextField);
		fileSelectButton = new JButton("select");			fileSelectButton.setBounds(700,10,80,25);
		tabPanel3.add(fileSelectButton);					fileSelectButton.setActionCommand("selectedr");
		loadButton = new JButton("load");					loadButton.setBounds(700,45,80,25);
		tabPanel3.add(loadButton);							loadButton.setActionCommand("loadfile");
		JLabel readStartLabel = new JLabel("Start ");
		tabPanel3.add(readStartLabel);						readStartLabel.setBounds(550,90,70,25);
		readStartTimeLabel = new JLabel("");
		readStartTimeLabel.setBounds(590,90,170,25);		tabPanel3.add(readStartTimeLabel);
		JLabel readEndLabel = new JLabel("End ");
		tabPanel3.add(readEndLabel);						readEndLabel.setBounds(550,115,70,25);
		readEndTimeLabel = new JLabel("");
		readEndTimeLabel.setBounds(590,115,170,25);			tabPanel3.add(readEndTimeLabel);
		JLabel readDAQLabel = new JLabel("Measured Time (s)");
		tabPanel3.add(readDAQLabel);						readDAQLabel.setBounds(750,80,200,25);
		readDAQTimeLabel = new JLabel("0");
		readDAQTimeLabel.setBounds(750,105,100,25);			tabPanel3.add(readDAQTimeLabel);
		readDAQTimeLabel.setHorizontalAlignment(JTextField.RIGHT);
		readDAQTimeLabel.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 20));
		JLabel readNeutronLabel = new JLabel("Neutron");
		tabPanel3.add(readNeutronLabel);					readNeutronLabel.setBounds(550,143,96,25);
		readNeutronCountLabel = new JLabel("0");
		readNeutronCountLabel.setBounds(550,163,96,25);		tabPanel3.add(readNeutronCountLabel);
		readNeutronCountLabel.setHorizontalAlignment(JTextField.RIGHT);
		readNeutronCountLabel.setBorder( new LineBorder(Color.LIGHT_GRAY, 1, false));
		JLabel readKPLabel = new JLabel("KP");
		tabPanel3.add(readKPLabel);							readKPLabel.setBounds(652,143,96,25);
		readKPCountLabel = new JLabel("0");
		readKPCountLabel.setBounds(652,163,96,25);			tabPanel3.add(readKPCountLabel);
		readKPCountLabel.setHorizontalAlignment(JTextField.RIGHT);
		readKPCountLabel.setBorder( new LineBorder(Color.LIGHT_GRAY, 1, false));
		JLabel readTiLabel = new JLabel("Time ID");
		tabPanel3.add(readTiLabel);							readTiLabel.setBounds(754,143,96,25);
		readTiCountLabel = new JLabel("0");
		readTiCountLabel.setBounds(754,163,96,25);			tabPanel3.add(readTiCountLabel);
		readTiCountLabel.setHorizontalAlignment(JTextField.RIGHT);
		readTiCountLabel.setBorder( new LineBorder(Color.LIGHT_GRAY, 1, false));
		JLabel readSelectedLabel = new JLabel("Selected");
		tabPanel3.add(readSelectedLabel);					readSelectedLabel.setBounds(550,190,96,25);
		readSelectedNeutronCountLabel = new JLabel("0");
		readSelectedNeutronCountLabel.setBounds(610,190,96,25);	tabPanel3.add(readSelectedNeutronCountLabel);
		readSelectedNeutronCountLabel.setHorizontalAlignment(JTextField.RIGHT);
		readSelectedNeutronCountLabel.setBorder( new LineBorder(Color.LIGHT_GRAY, 1, false));
		JLabel updateLabel = new JLabel("To update PH/TOF, push \'load\' button.");
		tabPanel3.add(updateLabel);						updateLabel.setBounds(550,235,400,25);
		JLabel readLLDLabel = new JLabel("LLD");
		tabPanel3.add(readLLDLabel);						readLLDLabel.setBounds(550,262,30,25);
		readLLDTextField = new JTextField("128", 50);		readLLDTextField.setBounds(578,262,50,25);			
		tabPanel3.add(readLLDTextField);					readLLDTextField.setActionCommand("loadfile");
		readLLDTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel readXYLabel = new JLabel("XYLimit");
		tabPanel3.add(readXYLabel);							readXYLabel.setBounds(638,262,55,25);
		readXYTextField = new JTextField("32", 50);			readXYTextField.setBounds(690,262,50,25);
		tabPanel3.add(readXYTextField);						readXYTextField.setActionCommand("loadfile");
		readXYTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel readTOFEndLabel = new JLabel("TOF end");
		tabPanel3.add(readTOFEndLabel);						readTOFEndLabel.setBounds(750,262,65,25);
		readTOFEndTextField = new JTextField("40", 50);		readTOFEndTextField.setBounds(805,262,50,25);
		tabPanel3.add(readTOFEndTextField);					readTOFEndTextField.setActionCommand("loadfile");
		readTOFEndTextField.setHorizontalAlignment(JTextField.CENTER);
		JLabel readZRangeLabel = new JLabel("Z range");
		tabPanel3.add(readZRangeLabel);						readZRangeLabel.setBounds(70,65,60,25);
		readRangeAuto   = new JRadioButton("Auto", true);	readRangeAuto.setBounds(120,65,80,25);
		tabPanel3.add(readRangeAuto);						readRangeAuto.setActionCommand("readrange");
		readRangeManual = new JRadioButton(" ");			readRangeManual.setBounds(200,65,40,25);
		tabPanel3.add(readRangeManual);						readRangeManual.setActionCommand("readrange");
		ButtonGroup readRangeGroup = new ButtonGroup();
		readRangeGroup.add(readRangeAuto);		readRangeGroup.add(readRangeManual);
		readRangeMinTextField = new JTextField("0", 50);	readRangeMinTextField.setBounds(225,66,50,25);
		tabPanel3.add(readRangeMinTextField);				readRangeMinTextField.setActionCommand("readrange");
		readRangeMinTextField.setHorizontalAlignment(JTextField.CENTER);
		readRangeMaxTextField = new JTextField("100", 50);	readRangeMaxTextField.setBounds(470,66,50,25);
		tabPanel3.add(readRangeMaxTextField);				readRangeMaxTextField.setActionCommand("readrange");
		readRangeMaxTextField.setHorizontalAlignment(JTextField.CENTER);
		ColorBarCanvas readBar = new ColorBarCanvas();
		readBar.setBounds(283,72,180,15);					tabPanel3.add(readBar);
		readImage2D = new SelectableImageCanvas();
		readImage2D.setBounds(10,90,512,512);				tabPanel3.add(readImage2D);
		readImage2D.setBorder( new LineBorder(Color.black, 1, false));
		readImagePH = new ImageCanvas();
		readImagePH.setBounds(570,292,280,130);				tabPanel3.add(readImagePH);
		readImageTOF = new ImageCanvas();
		readImageTOF.setBounds(570,452,280,130);			tabPanel3.add(readImageTOF);
		readTOFEndPlotLabel = new JLabel("40");
		tabPanel3.add(readTOFEndPlotLabel);					readTOFEndPlotLabel.setBounds(820,582,50,25);
		JLabel readTOFZeroPlotLabel = new JLabel("0");
		tabPanel3.add(readTOFZeroPlotLabel);				readTOFZeroPlotLabel.setBounds(568,582,20,25);
		JLabel readTOFPlotLabel = new JLabel("TOF (ms)");
		tabPanel3.add(readTOFPlotLabel);					readTOFPlotLabel.setBounds(672,582,100,25);
		JLabel readPHPlotLabel = new JLabel("Pulse Height");
		tabPanel3.add(readPHPlotLabel);						readPHPlotLabel.setBounds(665,422,120,25);
		JLabel readPHZeroPlotLabel = new JLabel("0");
		tabPanel3.add(readPHZeroPlotLabel);					readPHZeroPlotLabel.setBounds(568,422,20,25);
		JLabel readPHHighPlotLabel = new JLabel("4096");	// JLabel readPHHighPlotLabel = new JLabel("1024");
		tabPanel3.add(readPHHighPlotLabel);					readPHHighPlotLabel.setBounds(810,422,40,25);
		saveEDRButton = new JButton("save");				saveEDRButton.setBounds(800,45,80,25);
		tabPanel3.add(saveEDRButton);						saveEDRButton.addActionListener(this);

		// add panels to the tabbed pane 
		tabbedpane.addTab("Gatenet initialize", tabPanel1);
		tabbedpane.addTab("RPMT DAQ control", tabPanel2);
		tabbedpane.addTab("EDR display", tabPanel3);
		// add tabbed pane on the window
		getContentPane().add(tabbedpane, BorderLayout.CENTER);

	} // end of constructor
	
	// action event performing method inside View object 
	public void actionPerformed(ActionEvent e) {
		
		if (e.getSource() == dirButton) {	// file selects
		 	// open file chooser
			JFileChooser filechooser = new JFileChooser();
			filechooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
			int selected = filechooser.showOpenDialog(this);
			if (selected == JFileChooser.APPROVE_OPTION) {
				File file = filechooser.getSelectedFile();
				dirTextArea.setText(file.getAbsolutePath());
			} else if (selected == JFileChooser.CANCEL_OPTION || selected == JFileChooser.ERROR_OPTION) {
			}
		} // end of "dirButton"
		if (e.getSource() == saveEDRButton) {	// save EDR image
			JFileChooser filechooser = new JFileChooser();
			File savefile = new File(readFileTextField.getText());
			String oldfilename = savefile.getName();
			int index = oldfilename.lastIndexOf('.');
			String newfilename = oldfilename.substring(0, index) + ".png";
			filechooser.setSelectedFile(new File(savefile.getParent(), newfilename));
			int selected = filechooser.showSaveDialog(this);
			if (selected == JFileChooser.APPROVE_OPTION) {
				File file = filechooser.getSelectedFile();
				saveEDRPanelImage(file.getAbsolutePath());
			} else if (selected == JFileChooser.CANCEL_OPTION || selected == JFileChooser.ERROR_OPTION) {
			}
		} // end of "saveEDRButton"
	}  // end of actionPerformed
	
	// actionListener registration
	public void setActionListener(ActionListener al) {
		readInfoButton.addActionListener(al);			setInfoButton.addActionListener(al);
		setLLDTimeButton.addActionListener(al);			runButton.addActionListener(al);
		rangeAuto.addActionListener(al);				rangeManual.addActionListener(al);
		fileSelectButton.addActionListener(al);			loadButton.addActionListener(al);
		readLLDTextField.addActionListener(al);			readXYTextField.addActionListener(al);
		readTOFEndTextField.addActionListener(al);
		readRangeAuto.addActionListener(al);			readRangeManual.addActionListener(al);
		readRangeMinTextField.addActionListener(al);	readRangeMaxTextField.addActionListener(al);
	}
	public void setMouseListener(MouseListener ml) {
		readImage2D.setMouseListener(ml);
	}
	
	// accessor methods
	public String  getGatenetAddress()  { return gatenetAddressTextField.getText();                }
	public int     getGatenetPort()     { return Integer.parseInt(gatenetPortTextField.getText()); }
	public boolean getKPReset()         { return kpResetCheckBox.isSelected();                     }
	public void setInfoText(String str) { gatenetInfoTextArea.setText(str);                        }
	public String  getRPMTSetLLDAddress() { return rpmtSetLLDAddressTextField.getText();             }
	public int     getRPMTSetLLDPort() { return Integer.parseInt(rpmtSetLLDPortTextField.getText()); }
	public int[]   getRPMTSetLLDTime() {
		int[] r = new int[3];
		r[0] = Integer.parseInt(rpmtSetLLDTextField.getText());
		r[1] = Integer.parseInt(rpmtSetTimeLowTextField.getText());
		r[2] = Integer.parseInt(rpmtSetTimeHighTextField.getText());
		return r;
	}
	public String  getRPMTAddress()     { return rpmtAddressTextField.getText();                   }
	public int     getRPMTPort()        { return Integer.parseInt(rpmtPortTextField.getText());    }
	public int     getRunNo()           {
		if (runNoCheckBox.isSelected()) { return Integer.parseInt(runNoTextField.getText()); }
		else { return -1; }
	}
	public void    setRunNo(int n)      { runNoTextField.setText(String.format("%d", n));          }
	public String  getWorkingDir()      { return dirTextArea.getText();                            }
	public void setWorkingDir(String d) { dirTextArea.setText(d);                                  }
	public boolean isLimitByTime()      { return limitTimeCheckBox.isSelected();                   }
	public boolean isLimitByKP()        { return limitKPCheckBox.isSelected();                     }
	public boolean isLimitByCount()     { return limitCountCheckBox.isSelected();                  }
	public int     getLimitTime()       { return Integer.parseInt(limitTimeTextField.getText());   }
	public int     getLimitKP()         { return Integer.parseInt(limitKPTextField.getText());     }
	public int     getLimitCount()      { return Integer.parseInt(limitCountTextField.getText());  }
	public void    setStartTime(String ts)  { startTimeLabel.setText(ts);                          }
	public void    setEndTime(String ts)    { endTimeLabel.setText(ts);                            }
	public void    setDAQTime(int t)        { daqTimeLabel.setText(String.format("%d",t));         }
	public void    setNeutronCount(int n)   { neutronCountLabel.setText(String.format("%d",n));    }
	public void    setNeutronRate(double n) { neutronRateLabel.setText(String.format("%.2f",n));  }
	public void    setKPCount(int n)        { kpCountLabel.setText(String.format("%d",n));         }
	public void    setTimeIDCount(int n)    { tiCountLabel.setText(String.format("%d",n));         }
	public int     getLLD()             { return Integer.parseInt(lldTextField.getText());         }
	public int     getXYLimit()         { return Integer.parseInt(xyTextField.getText());          }
	public int     getTOFEnd()          { return Integer.parseInt(tofEndTextField.getText());      }
	public void    setTOFEnd(int t)     { tofEndPlotLabel.setText(String.format("%d",t));          }
	public boolean isAutoRange()        { return rangeAuto.isSelected();                           }
	public int     getRangeMin()        { return Integer.parseInt(rangeMinTextField.getText());    }
	public int     getRangeMax()        { return Integer.parseInt(rangeMaxTextField.getText());    }
	public void    setRangeMin(int n)   { rangeMinTextField.setText(String.format("%d",n));        }
	public void    setRangeMax(int n)   { rangeMaxTextField.setText(String.format("%d",n));        }
	public void    setImage2D(BufferedImage bi)  { image2D.setImage(bi);                           }
	public void    setImagePH(BufferedImage bi)  { imagePH.setImage(bi);                           }
	public void    setImageTOF(BufferedImage bi) { imageTOF.setImage(bi);                          }

	public void setRunStatus(boolean s) { 
		if (s == true) {
			runButton.setForeground(Color.RED);		runButton.setText("Stop RUN");
			runIndicator.setBackground(new Color(255,0,0,255));
			runIndicator.setOpaque(true);			runIndicator.repaint();
		} else {
			runButton.setForeground(Color.BLACK);	runButton.setText("Start RUN");	
			runIndicator.setOpaque(false);			runIndicator.repaint();
		}
	}

	public void    setEDRFilename(String s) { readFileTextField.setText(s);                         }
	public String  getEDRFilename()         { return readFileTextField.getText();                   }
	public int     getEDRLLD()              { return Integer.parseInt(readLLDTextField.getText());  }
	public int     getEDRXYLimit()          { return Integer.parseInt(readXYTextField.getText());   }
	public int     getEDRTOFEnd()           { return Integer.parseInt(readTOFEndTextField.getText());}
	public void    setEDRTOFEnd(int te)     { readTOFEndPlotLabel.setText(String.format("%d",te));  }
	public void    setEDRImage2D(BufferedImage bi)  { readImage2D.setImage(bi);                     }
	public void    setEDRImagePH(BufferedImage bi)  { readImagePH.setImage(bi);                     }
	public void    setEDRImageTOF(BufferedImage bi) { readImageTOF.setImage(bi);                    }
	public void    setEDRNeutronCount(int n){ readNeutronCountLabel.setText(String.format("%d",n)); }
	public void    setEDRKPCount(int n)     { readKPCountLabel.setText(String.format("%d",n));      }
	public void    setEDRTimeIDCount(int n) { readTiCountLabel.setText(String.format("%d",n));      }

	public void    setEDRStartTime(String ts) { readStartTimeLabel.setText(ts);                     }
	public void    setEDREndTime(String ts) { readEndTimeLabel.setText(ts);                         }
	public void    setEDRDAQTime(int t)     { readDAQTimeLabel.setText(String.format("%d",t));      }
	public boolean isEDRAutoRange()         { return readRangeAuto.isSelected();                    }
	public int     getEDRRangeMin()         { return Integer.parseInt(readRangeMinTextField.getText()); }
	public int     getEDRRangeMax()         { return Integer.parseInt(readRangeMaxTextField.getText()); }
	public void    setEDRRangeMin(int n)    { readRangeMinTextField.setText(String.format("%d",n));     }
	public void    setEDRRangeMax(int n)    { readRangeMaxTextField.setText(String.format("%d",n));     }

	public boolean isEDRSelected()          { return readImage2D.isSelected();      }
	public int[]   getEDRSelectedArea()     { return readImage2D.getSelectedArea(); }
	public void setEDRSelectedNeutronCount(int n) { readSelectedNeutronCountLabel.setText(String.format("%d",n)); }
	// save EDR Reader panel image
	public void    saveEDRPanelImage(String f) {
		BufferedImage panelImage = null;
		try { panelImage = new Robot().createScreenCapture(tabPanel3.getBounds()); }
		catch (Exception e1) { }  
		Graphics2D graphics2D = panelImage.createGraphics();
		tabPanel3.paint(graphics2D);
		try { ImageIO.write(panelImage,"png", new File(f)); }
		catch (Exception e) { }
	} // end of save EDR Reader panel image

} // end of RPMTControllerWindow

// imageCanvas for histogram
class ImageCanvas extends JPanel {
	BufferedImage image  = null;
	public ImageCanvas() {	}
	public void setImage(BufferedImage img) {
		if (image != null) { image = null; }
		image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g=image.createGraphics();
		g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
		g.dispose();	repaint();
	}
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
	}
}

// imageCanvas for histogram
class SelectableImageCanvas extends JPanel implements MouseListener, MouseMotionListener {
	BufferedImage image  = null;	int startX, startY, currentX, currentY;
	int[] sa = new int[4];			boolean selected = false;
	public SelectableImageCanvas() {
		addMouseListener(this);	addMouseMotionListener(this);
	}
	public boolean isSelected() { return selected; }
	public int[] getSelectedArea() { return sa; }
	public void setImage(BufferedImage img) {
		if (image != null) { image = null; }
		image = new BufferedImage(getWidth(), getHeight(), BufferedImage.TYPE_INT_RGB);
		Graphics g=image.createGraphics();
		g.drawImage(img, 0, 0, getWidth(), getHeight(), null);
		g.dispose();	repaint();
	}
	public void setMouseListener(MouseListener ml) { addMouseListener(ml); }
	public void mousePressed(MouseEvent e)  { startX =currentX =e.getX(); startY =currentY =e.getY(); }
	public void mouseReleased(MouseEvent e) { }
	public void mouseClicked(MouseEvent e)  { startX=0;currentX=0;startY=0;currentY=0; selected=false; repaint(); }
	public void mouseEntered(MouseEvent e)  { }
	public void mouseExited(MouseEvent e)   { }
	public void mouseMoved(MouseEvent e)    { }
	public void mouseDragged(MouseEvent e)  {
		currentX = e.getX(); currentY = e.getY();
		if(currentX != startX || currentY != startY) { selected=true; repaint(); }
	}
	public void paintComponent(Graphics g) {
		if (currentX < startX) {sa[0]=currentX; sa[2]=startX;} else {sa[0]=startX; sa[2]=currentX;}
		if (currentY < startY) {sa[1]=currentY; sa[3]=startY;} else {sa[1]=startY; sa[3]=currentY;}
		super.paintComponent(g);
		g.drawImage(image, 0, 0, getWidth(), getHeight(), null);
		g.setColor(Color.red);
		g.drawRect(sa[0],sa[1],sa[2]-sa[0],sa[3]-sa[1]);
	}
}

// Color bar
class ColorBarCanvas extends JPanel {
	int[] col = {0x263EA8,0x1652CD,0x1064DC,0x1372D9,0x1581D6,0x0D8FD2,0x099DCC,0x0DA7C3,0x1EAFB3,0x2EB7A4,
				 0x53BA92,0x74BD81,0x95BE70,0xB3BD65,0xD1BB59,0xE2C04B,0xF4C63B,0xFDD22C,0xFBE61E,0xF9F911};
	public ColorBarCanvas() { setBorder( new LineBorder(Color.black, 1, false)); }
	public void paintComponent(Graphics g) {
		super.paintComponent(g);
		int i = 0;
		while (i < 20) {
			g.setColor(new Color(col[i]));	g.fillRect(getWidth()*i/20, 0, getWidth()/20, getHeight());
			i++;
		}
	}
}


// end of RPMTController 



