/*********************************************************************
     readRPMT
       read RPMT data and convert .edr to .root tree 

     How to use on ROOT
       root[] .L readRPMT.C+
			 
     to display summary
       root[] readRPMT(<edr_filename>, <bin # of tof>);
       
     to change the parameters for display :
       root[] lld(<LLD>, <bin # of tof>);             // change LLD
       root[] ph(<LLD>, <HLD>, <bin # of tof>);       // change Pulse Height range 
       root[] tof(<t1>, <t2>, <bin # of tof>;        // change TOF range
       root[] xy(<xylimit>, <bin # of tof>);          // change XY coincidence limit
       root[] reset(<bin # of tof>);                // reset to default value

     and to save TTree object into .root file :
       root[] save();

	 to save TOF spectrum to txt file :
	   root[] saveTOF(<txt_filename>);

     only LLD and XYLIMIT are used to make TTree object. 

     to convert .edr to .root (without display)
       root[] convertRPMT(<edb_filename>);
     then, root file "rpmtXXXX.root" is created.
     By additional arguments, LLD and XYLIMIT can be set.
       root[] convertRPMT(<edb_filename>, <LLD>);
       root[] convertRPMT(<edb_filename>, <LLD>, <XYLIMIT>);

*********************************************************************/

#include <iostream>
#include <fstream>
#include <sys/types.h>
#include <unistd.h>
#include <time.h>
/* ROOT headers */
#include <TH1D.h>
#include <TH2D.h>
#include <TTree.h>
#include <TEnv.h>
#include <TCanvas.h>
#include <TPad.h>
#include <TFrame.h>
#include <TStyle.h>
#include <TSystem.h>
#include <TText.h>
#include <TPaveText.h>
#include <TFile.h>
/* memory size */
#define XSIZE 128
#define YSIZE 128
#define XYSIZE 16384
#define PHSIZE 256
#define TOFSIZE 256

#define RPMTPHA 1024
#define LLDLOW 128
#define XYLIMIT 32
#define BINNUM 1024

// display class for global variables ////////////////////
class DisplayRPMT
{
	private:
		static char filename[128];
		static unsigned int    lld;		static unsigned int hld;	static unsigned int    xylimit;
		static unsigned int x_start, x_end, y_start, y_end;
		static double tof1;		static double tof2;
		static TTree* tree;		static TEnv*  env;
		static TCanvas* c;		static TH2D*  h_2d;
		static TH1D*  h_ph;		static TH1D*  h_tof;
		static TH1D* h_1dx;		static TH1D* h_1dy;
	public:
		static void  SetFilename(const char* s) { strcpy(DisplayRPMT::filename, s); }
		static char* GetFilename()    { return filename; }
		static void  SetLLD(unsigned int l)    { lld = l; }
		static unsigned int   GetLLD()         { return lld; }
		static void  SetHLD(unsigned int l)    { hld = l; }
		static unsigned int   GetHLD()         { return hld; }
		static void  SetTOF1(double t1) { tof1 = t1; }
		static double GetTOF1()         { return tof1; }
		static void  SetTOF2(double t2) { tof2 = t2; }
		static double GetTOF2()         { return tof2; }
		static void  SetXYLimit(unsigned int xy) {xylimit = xy; }
		static unsigned int   GetXYLimit()     { return xylimit; }
		static void  SetXStart(unsigned int xs) {x_start = xs; }
		static unsigned int   GetXStart()     { return x_start; }
		static void  SetXEnd(unsigned int xe) {x_end = xe; }
		static unsigned int   GetXEnd()     { return x_end; }		
		static void  SetYStart(unsigned int ys) {y_start = ys; }
		static unsigned int   GetYStart()     { return y_start; }
		static void  SetYEnd(unsigned int ye) {y_end = ye; }
		static unsigned int   GetYEnd()     { return y_end; }		

		static void  SetTree(TTree* t){ if (tree!=NULL) { delete tree; } tree = t; }
		static TTree* GetTree()       { return tree; }
		static void  SetEnv(TEnv* e)  { if (env!=NULL)  { delete env; }  env = e; }
		static TEnv* GetEnv()         { return env; }
		static void  SetCanvas(TCanvas* c1) { if (c!=NULL)  { delete c; }  c = c1; }
		static TCanvas* GetCanvas()   { return c; }
		static void  SetH2D(TH2D* h)  { if (h_2d!=NULL) { delete h_2d; } h_2d = h; }
		static TH2D* GetH2D()         { return h_2d; }
		static void  SetHPH(TH1D* h)  { if (h_ph!=NULL) { delete h_ph; } h_ph = h; }
		static TH1D* GetHPH()         { return h_ph; }
		static void  SetH1DX(TH1D* h)  { if (h_1dx!=NULL) { delete h_1dx; } h_1dx = h; }
		static TH1D* GetH1DX()         { return h_1dx; }		
		static void  SetH1DY(TH1D* h)  { if (h_1dy!=NULL) { delete h_1dy; } h_1dy = h; }
		static TH1D* GetH1DY()         { return h_1dy; }				
		static void  SetHTOF(TH1D* h) { if (h_tof!=NULL){ delete h_tof;} h_tof = h; }
		static TH1D* GetHTOF()        { return h_tof; }
		static int   Save();
};

char   DisplayRPMT::filename[128];
unsigned int    DisplayRPMT::lld     = 128;
unsigned int    DisplayRPMT::hld     = 1024;
unsigned int    DisplayRPMT::xylimit = 32;
unsigned int	DisplayRPMT::x_start = 0;
unsigned int	DisplayRPMT::x_end   = 1024;
unsigned int	DisplayRPMT::y_start = 0;
unsigned int	DisplayRPMT::y_end   = 1024;

double DisplayRPMT::tof1    = 0;
double DisplayRPMT::tof2    = 8; // was 40 before
TTree* DisplayRPMT::tree    = NULL;
TEnv*  DisplayRPMT::env     = NULL;
TCanvas* DisplayRPMT::c     = NULL;
TH2D*  DisplayRPMT::h_2d    = NULL;
TH1D*  DisplayRPMT::h_1dx   = NULL;
TH1D*  DisplayRPMT::h_1dy   = NULL;
TH1D*  DisplayRPMT::h_ph    = NULL;
TH1D*  DisplayRPMT::h_tof   = NULL;

// save tree to .root file
int DisplayRPMT::Save() {
	// make .root filename
	std::string filenameString( DisplayRPMT::GetFilename() );
	int l = filenameString.size();
	std::string rootfilestring = filenameString.substr(0, l-4) + ".root";
	// open rootfile
	TFile* rootfile = new TFile(rootfilestring.c_str(), "RECREATE");
	// write to file
	tree->Write();
	env->Write();
	rootfile->Close();
	return 0;
}



// convert .edr to .root /////////////////////////////////
int makeTreeRPMT(TTree* tree, TEnv* env, const char *filename, 
                 const unsigned int lld, const unsigned int xylimit) {
	// make Tree
	int stored_flag = 0;
	int header;
	unsigned char bytedata[8];
  	int psd_num = 0;     int psd_num0 = 0;
  	int module_num = 0;  int module_num0 = 0;
  	unsigned int time = 0;  unsigned int time0 = 0;
	unsigned int pha_left;	unsigned int pha_left0;
	unsigned int pha_right;	unsigned int pha_right0;
	unsigned int pha_total;	unsigned int pha_total0;
	double pos = 0;			double pos0 = 0;
	double pos_x = 0;		double pos_y = 0;
	double x, y, tof;
	unsigned int ph   = 0;			unsigned long int t0id = 0;
	unsigned long int timeid = 0;	int timeid_started = 0; // flag
	unsigned long int timeid_start = 0;	unsigned long int timeid_end = 0;
	int total_num = 0;	int t0_num = 0;	int time_num = 0;

	// branch
	tree->Branch("x", &x, "x/D");
	tree->Branch("y", &y, "y/D");
	tree->Branch("tof", &tof, "tof/D");
	tree->Branch("ph", &ph, "ph/i");
	tree->Branch("t0id", &t0id, "t0id/l");
	tree->Branch("timeid", &timeid, "timeid/l");

	// open edr file
	std::ifstream infile(filename, ios::binary);

	// read data loop
	while(infile.read((char *) bytedata, 8*sizeof(char))){
		header = bytedata[0];	// header
		if (header == 0x5A){	//neutron event
			time = bytedata[1] * (0x10000) + bytedata[2] * (0x100) + bytedata[3];	// tof
			psd_num = bytedata[4] & 0x7;	// psd number		lower 3 byte 0x07 = 0000111;
			module_num = bytedata[4] >> 3;	// module number 
			pha_left = bytedata[5] * 0x10 + (bytedata[6] >> 4);		// pulse height of left side
			pha_right = (bytedata[6] & 0xf) * 0x100 + bytedata[7];	// pulse height of right side
			pha_total = (pha_left + pha_right)*RPMTPHA/4096;		// total pulse height
			pos = (double)1024*pha_left/(pha_right+pha_left);		// position = L/(L+R)
			//discrimination
			if ((psd_num == 0 || psd_num == 1) && (pha_total >= lld && pha_total <= RPMTPHA)){
				// if data stored, reconstruct informations
				if (stored_flag == 1){
					if (((psd_num0 == 0 && psd_num == 1) 
						|| (psd_num0 == 1 && psd_num == 0))
						&& (fabs((double)time - (double)time0) < xylimit)){ //coincidence

						if(psd_num0 == 0 && psd_num == 1){	// x axis first, y axis second
							pos_x = pos0;	pos_y = pos;
						}
						if(psd_num0 == 1 && psd_num == 0){	// y axis first, x axis second
							pos_x = pos;	pos_y = pos0;
						}
						// fill data into tree
						tof = (double)time*25.e-6;	// TOF
						x = pos_x;	y = pos_y;		// position (x,y)
						ph = pha_total;				// Pulse height
						tree->Fill();
						// reset flag
						stored_flag = 0;
						total_num++;
					}
				} // end of stored_flag == 1
				// if not stored, 
				time0		= time;
				psd_num0	= psd_num;		module_num0 = module_num;
				pha_left0	= pha_left;		pha_right0	= pha_right;
				pha_total0	= pha_total;	pos0 = pos;
				stored_flag = 1;
			} // end of discrimination

		} // end of neutron event 0x5A

		else {	// KP event
			if (header == 0x5B){
				t0id = ((unsigned long int)(256*256))*((unsigned long int)(256*256))*((unsigned long int)bytedata[3])
				+ ((unsigned long int)(256))*((unsigned long int)(256*256))*((unsigned long int)bytedata[4])
				+ ((unsigned long int)(256*256))*((unsigned long int)bytedata[5])
				+ ((unsigned long int)(256))*((unsigned long int)bytedata[6])
				+ ((unsigned long int)bytedata[7]);
				t0_num++;
			} else if (header == 0x5C){
				timeid = (bytedata[1]/4)*256*256*256
				+ ( ((bytedata[1])%4)*64 + ((bytedata[2])/4) )*256*256
				+ ( ((bytedata[2])%4)*64 + ((bytedata[3])/4) )*256
				+ ( (bytedata[3])%4)*64 + ((bytedata[4])/4);
				if (timeid_started <=0) {
					timeid_start = timeid;
					timeid_started = 1;
				}
				time_num++;
			}
		}
	} // end of read data loop

	timeid_end = timeid;

	// close edr file
	infile.close();
	// save TTree to file
	env->SetValue("StartTime1", (int)(timeid_start/(256*256)) );
	env->SetValue("StartTime2", (int)(timeid_start%(256*256)) );
	env->SetValue("EndTime1", (int)(timeid_end/(256*256)) );
	env->SetValue("EndTime2", (int)(timeid_end%(256*256)) );
	env->SetValue("T0Num", (int)t0_num );
	env->SetValue("TimeNum", (int)(time_num) );

	DisplayRPMT::SetFilename(filename);
	DisplayRPMT::SetTree(tree);
	DisplayRPMT::SetEnv(env);
	DisplayRPMT::SetLLD(lld);
	DisplayRPMT::SetXYLimit(xylimit);


	return 0;
// end of function
}

// convert GateNet Time to local time array YYYY/MM/DD HH:MM:SS
int timeToText(int t1, int t2, int* a) {
	// GateNet Time is second from 2008/1/1 09:00:00 with 30 bits
	// t1 = upper 14 bits -> 2 bytes, t2 = lower 16 bits = 2 bytes
	time_t t;
	t = ((time_t)(256*256))*((time_t)t1)+((time_t)t2);
	t += 1199145600; // unixtime of 2008/1/1 09:00:00
	struct tm *tms;	tms = localtime(&t);
	a[0] = tms->tm_year+1900; a[1] = tms->tm_mon+1; a[2] = tms->tm_mday;
	a[3] = tms->tm_hour; a[4] = tms->tm_min; a[5] = tms->tm_sec;
	return 0;
}


// read .edr file and display image and PH, TOF /////////////////////////////////
int readRPMT(const char *filename, const int nbin) {

	/* Remove histograms in DisplayRPMT global variables */
	DisplayRPMT::SetCanvas(NULL);
	DisplayRPMT::SetTree(NULL);
	DisplayRPMT::SetH2D(NULL);
	DisplayRPMT::SetHPH(NULL);
	DisplayRPMT::SetHTOF(NULL);

    /* define histograms */
    TH2D* h_2d  = new TH2D("h_2d", "2D image", 128,0,1024,128,0,1024);  /* 2D image */
   	TH1D* h_1dx = new TH1D("h_1dx", "", 256,0,1024);  /* 2D image */	
	TH1D* h_1dy = new TH1D("h_1dy", "", 256,0,1024);  /* 2D image */		

    TH1D* h_ph  = new TH1D("h_ph", "", 256,0,1024);         /* Pulse height */
    // TH1D* h_tof = new TH1D("h_tof", "", 256, DisplayRPMT::GetTOF1(), DisplayRPMT::GetTOF2());   /* TOF */
    TH1D* h_tof = new TH1D("h_tof", "", nbin, DisplayRPMT::GetTOF1(), DisplayRPMT::GetTOF2());   /* TOF */	
    /* make canvas and pads */
    TCanvas* c1 = new TCanvas("c1","RPMT reader", 900, 600);
   	TPad* pad1 = new TPad("pad1", "pad1", 0.01, 0.05, 0.61, 1.);       
	// TPad* pad2 = new TPad("pad2", "pad2", 0.63, 0.67, 1., 1.);
	// TPad* pad3 = new TPad("pad3", "pad3", 0.63, 0.34, 1., 0.67);
	// TPad* pad4 = new TPad("pad4", "pad4", 0.63, 0., 1., 0.34);
	TPad* pad2 = new TPad("pad2", "pad2", 0.65, 0.75, 1., 1.);
	TPad* pad3x = new TPad("pad3x", "pad3x", 0.65, 0.50, 0.82, 0.75);	
	TPad* pad3y = new TPad("pad3y", "pad3y", 0.83, 0.50, 1., 0.75);
	TPad* pad4 = new TPad("pad4", "pad4", 0.65, 0.25, 1., 0.50);
	TPad* pad5 = new TPad("pad5", "pad5", 0.65, 0.00, 1., 0.25);
    c1->SetFillColor(18);
    pad1->SetFillColor(18); pad1->SetFrameFillColor(10);
    pad2->SetFillColor(18); pad2->SetFrameFillColor(10);
    pad3x->SetFillColor(18); pad3x->SetFrameFillColor(10);
    pad3y->SetFillColor(18); pad3y->SetFrameFillColor(10);	
    pad4->SetFillColor(18); pad4->SetFrameFillColor(10);
	pad5->SetFillColor(18); 	

    pad1->Draw();   pad2->Draw();   pad3x->Draw();  pad3y->Draw();   pad4->Draw(); pad5->Draw();
    gStyle->SetOptStat(kFALSE); /* No statistics box */

	// make tree
	TTree* tree = new TTree("tree", "tree");
	TEnv* env = new TEnv();
	makeTreeRPMT(tree, env, filename, 
				DisplayRPMT::GetLLD(), DisplayRPMT::GetXYLimit() );
	// tree, env, LLD, xylimit   are used in DisplayRPMT global variables

	pad1->cd();
	tree->Draw("y:x>>h_2d",
				Form("ph>=%d && ph <= %d && tof>=%f && tof<=%f && x>=%d && x<%d && y>=%d && y<%d",
					DisplayRPMT::GetLLD(),  DisplayRPMT::GetHLD(),
					DisplayRPMT::GetTOF1(), DisplayRPMT::GetTOF2(),
					DisplayRPMT::GetXStart(),DisplayRPMT::GetXEnd(),
					DisplayRPMT::GetYStart(),DisplayRPMT::GetYEnd()), "colz");
		h_2d->SetMinimum(-0.001);
	TBox* box = new TBox(DisplayRPMT::GetXStart(), DisplayRPMT::GetYStart(), DisplayRPMT::GetXEnd(), DisplayRPMT::GetYEnd());
	box->Draw();
	box->SetFillStyle(0); 
	box->SetLineColor(kRed);


	pad2->cd();
	tree->Draw("ph>>h_ph",
				Form("ph>=%d && ph <= %d && tof>=%f && tof<=%f && x>=%d && x<%d && y>=%d && y<%d",
					DisplayRPMT::GetLLD(),  DisplayRPMT::GetHLD(),
					DisplayRPMT::GetTOF1(), DisplayRPMT::GetTOF2(),
					DisplayRPMT::GetXStart(),DisplayRPMT::GetXEnd(),
					DisplayRPMT::GetYStart(),DisplayRPMT::GetYEnd() ));
        TText* phTitleText = new TText(0.3,0.92, "Pulse Hight");
        phTitleText->SetTextSize(0.08); phTitleText->SetNDC(1); phTitleText->Draw();
	
	pad3x->cd();
	tree->Draw("x>>h_1dx",
				Form("ph>=%d && ph <= %d && tof>=%f && tof<=%f && x>=%d && x<%d && y>=%d && y<%d",
					DisplayRPMT::GetLLD(),  DisplayRPMT::GetHLD(),
					DisplayRPMT::GetTOF1(), DisplayRPMT::GetTOF2(),
					DisplayRPMT::GetXStart(),DisplayRPMT::GetXEnd(),
					DisplayRPMT::GetYStart(),DisplayRPMT::GetYEnd() ));
         TText* h1dxTitleText = new TText(0.3,0.92, "X profile");
        h1dxTitleText->SetTextSize(0.08); h1dxTitleText->SetNDC(1); h1dxTitleText->Draw();

	pad3y->cd();
	tree->Draw("y>>h_1dy",
				Form("ph>=%d && ph <= %d && tof>=%f && tof<=%f && x>=%d && x<%d && y>=%d && y<%d",
					DisplayRPMT::GetLLD(),  DisplayRPMT::GetHLD(),
					DisplayRPMT::GetTOF1(), DisplayRPMT::GetTOF2(),
					DisplayRPMT::GetXStart(),DisplayRPMT::GetXEnd(),
					DisplayRPMT::GetYStart(),DisplayRPMT::GetYEnd() ));
         TText* h1dyTitleText = new TText(0.3,0.92, "Y profile");
        h1dyTitleText->SetTextSize(0.08); h1dyTitleText->SetNDC(1); h1dyTitleText->Draw();

	pad4->cd();
	tree->Draw("tof>>h_tof",
				Form("ph>=%d && ph <= %d && tof>=%f && tof<=%f && x>=%d && x<%d && y>=%d && y<%d",
					DisplayRPMT::GetLLD(),  DisplayRPMT::GetHLD(),
					DisplayRPMT::GetTOF1(), DisplayRPMT::GetTOF2(),
					DisplayRPMT::GetXStart(),DisplayRPMT::GetXEnd(),
					DisplayRPMT::GetYStart(),DisplayRPMT::GetYEnd()));
    	TText* tofTitleText = new TText(0.4,0.92, "TOF");
        tofTitleText->SetTextSize(0.08); tofTitleText->SetNDC(1); tofTitleText->Draw();

    /* histogram label font size*/
    h_ph->GetXaxis()->SetLabelSize(0.07);   h_ph->GetYaxis()->SetLabelSize(0.07);
	h_1dx->GetXaxis()->SetLabelSize(0.07);  h_1dx->GetYaxis()->SetLabelSize(0.07);	
	h_1dy->GetXaxis()->SetLabelSize(0.07);  h_1dy->GetYaxis()->SetLabelSize(0.07);	
    h_tof->GetXaxis()->SetLabelSize(0.07);  h_tof->GetYaxis()->SetLabelSize(0.07);

	pad5->cd();
		/* filename */
        TText* runNoText = new TText(0.,0.8, filename);
        runNoText->SetTextSize(0.15);    runNoText->Draw();
        /* start time */
        int st[6];
        int st1 = DisplayRPMT::GetEnv()->GetValue("StartTime1", 0);
        int st2 = DisplayRPMT::GetEnv()->GetValue("StartTime2", 0);
        timeToText(st1, st2, st);
        TText* startTimeText = new TText(0.1,0.7, 
    	    Form("Start  %d.%02d.%02d %02d:%02d:%02d",st[0],st[1],st[2],st[3],st[4],st[5]));
        startTimeText->SetTextSize(0.1); startTimeText->Draw();
		/* end time */
        int et[6];
        int et1 = DisplayRPMT::GetEnv()->GetValue("EndTime1", 0);
        int et2 = DisplayRPMT::GetEnv()->GetValue("EndTime2", 0);
        timeToText(et1, et2, et);
        TText* endTimeText = new TText(0.106,0.6, 
	        Form("End   %d.%02d.%02d %02d:%02d:%02d",et[0],et[1],et[2],et[3],et[4],et[5]));
        endTimeText->SetTextSize(0.1); endTimeText->Draw();
	    /* total neutron counts */
    	TText* totalText = new TText(0.05,0.45, Form("Total : %d, Time : %.2f s", (int)(h_ph->GetEntries()), (double)(et2-st2)));
    	totalText->SetTextSize(0.1);     totalText->Draw();
    	/* KP counts */
	    TText* t0Text = new TText(0.05,0.32, Form("T0 : %d", (int)(env->GetValue("T0Num", 0)) ));
    	t0Text->SetTextSize(0.13);     t0Text->Draw();
    	/* LLD counts */
	    TText* lldText = new TText(0.1,0.21, Form("LLD : %d", (int)(DisplayRPMT::GetLLD()) ));
    	lldText->SetTextSize(0.1);     lldText->Draw();
    	/* XYLimit counts */
	    TText* xyText = new TText(0.1,0.11, Form("XY coincidence limit : %d", (int)(DisplayRPMT::GetXYLimit()) ));
    	xyText->SetTextSize(0.1);     xyText->Draw();

	/* set histograms into DisplayRPMT global variables */
	DisplayRPMT::SetCanvas(c1);
	DisplayRPMT::SetH2D(h_2d);
	DisplayRPMT::SetHPH(h_ph);
	DisplayRPMT::SetH1DX(h_1dx);
	DisplayRPMT::SetH1DY(h_1dy);	
	DisplayRPMT::SetHTOF(h_tof);
	c1->Update();



	return 1;
}

// convert .edr file to .root file /////////////////////////////////
int convertRPMT(const char *filename1, const unsigned int lld1, const unsigned int xylimit1) {

	// make .root filename
	std::string filestring(filename1);
	int l = filestring.size();
	std::string rootfilestring = filestring.substr(0, l-4) + ".root";
	// open rootfile
	TFile* rootfile1 = new TFile(rootfilestring.c_str(), "RECREATE");

	// make Tree
	int stored_flag = 0;
	int header;
	unsigned char bytedata[8];
  	int psd_num = 0;     int psd_num0 = 0;
  	int module_num = 0;  int module_num0 = 0;
  	unsigned int time = 0;  unsigned int time0 = 0;
	unsigned int pha_left;	unsigned int pha_left0;
	unsigned int pha_right;	unsigned int pha_right0;
	unsigned int pha_total;	unsigned int pha_total0;
	double pos = 0;			double pos0 = 0;
	double pos_x = 0;		double pos_y = 0;
	double x, y, tof;
	unsigned int ph   = 0;			unsigned long int t0id = 0;
	unsigned long int timeid = 0;	int timeid_started = 0; // flag
	unsigned long int timeid_start = 0;	unsigned long int timeid_end = 0;
	int total_num = 0;	int t0_num = 0;	int time_num = 0;

	// make Tree
	TTree* tree1 = new TTree("tree", "tree");
	TEnv* env1 = new TEnv();
	// branch
	tree1->Branch("x", &x, "x/D");
	tree1->Branch("y", &y, "y/D");
	tree1->Branch("tof", &tof, "tof/D");
	tree1->Branch("ph", &ph, "ph/i");
	tree1->Branch("t0id", &t0id, "t0id/l");
	tree1->Branch("timeid", &timeid, "timeid/l");

	// open edr file
	std::ifstream infile(filename1, ios::binary);

	// read data loop
	while(infile.read((char *) bytedata, 8*sizeof(char))){
		header = bytedata[0];	// header
		if (header == 0x5A){	//neutron event
			time = bytedata[1] * (0x10000) + bytedata[2] * (0x100) + bytedata[3];	// tof
			psd_num = bytedata[4] & 0x7;	// psd number		lower 3 byte 0x07 = 0000111;
			module_num = bytedata[4] >> 3;	// module number 
			pha_left = bytedata[5] * 0x10 + (bytedata[6] >> 4);		// pulse height of left side
			pha_right = (bytedata[6] & 0xf) * 0x100 + bytedata[7];	// pulse height of right side
			pha_total = (pha_left + pha_right)*RPMTPHA/4096;		// total pulse height
			pos = (double)1024*pha_left/(pha_right+pha_left);		// position = L/(L+R)
			//discrimination
			if ((psd_num == 0 || psd_num == 1) && (pha_total >= lld1 && pha_total <= RPMTPHA)){
				// if data stored, reconstruct informations
				if (stored_flag == 1){
					if (((psd_num0 == 0 && psd_num == 1) 
						|| (psd_num0 == 1 && psd_num == 0))
						&& (fabs((double)time - (double)time0) < xylimit1)){ //coincidence

						if(psd_num0 == 0 && psd_num == 1){	// x axis first, y axis second
							pos_x = pos0;	pos_y = pos;
						}
						if(psd_num0 == 1 && psd_num == 0){	// y axis first, x axis second
							pos_x = pos;	pos_y = pos0;
						}
						// fill data into tree
						tof = (double)time*25.e-6;	// TOF
						x = pos_x;	y = pos_y;		// position (x,y)
						ph = pha_total;				// Pulse height
						tree1->Fill();
						// reset flag
						stored_flag = 0;
						total_num++;
					}
				} // end of stored_flag == 1
				// if not stored, 
				time0		= time;
				psd_num0	= psd_num;		module_num0 = module_num;
				pha_left0	= pha_left;		pha_right0	= pha_right;
				pha_total0	= pha_total;	pos0 = pos;
				stored_flag = 1;
			} // end of discrimination

		} // end of neutron event 0x5A

		else {	// KP event
			if (header == 0x5B){
				t0id = ((unsigned long int)(256*256))*((unsigned long int)(256*256))*((unsigned long int)bytedata[3])
				+ ((unsigned long int)(256))*((unsigned long int)(256*256))*((unsigned long int)bytedata[4])
				+ ((unsigned long int)(256*256))*((unsigned long int)bytedata[5])
				+ ((unsigned long int)(256))*((unsigned long int)bytedata[6])
				+ ((unsigned long int)bytedata[7]);
				t0_num++;
			} else if (header == 0x5C){
				timeid = (bytedata[1]/4)*256*256*256
				+ ( ((bytedata[1])%4)*64 + ((bytedata[2])/4) )*256*256
				+ ( ((bytedata[2])%4)*64 + ((bytedata[3])/4) )*256
				+ ( (bytedata[3])%4)*64 + ((bytedata[4])/4);
				if (timeid_started <=0) {
					timeid_start = timeid;
					timeid_started = 1;
				}
				time_num++;
			}
		}
	} // end of read data loop

	timeid_end = timeid;

	// close edr file
	infile.close();
	// save TTree to file
	env1->SetValue("StartTime1", (int)(timeid_start/(256*256)) );
	env1->SetValue("StartTime2", (int)(timeid_start%(256*256)) );
	env1->SetValue("EndTime1", (int)(timeid_end/(256*256)) );
	env1->SetValue("EndTime2", (int)(timeid_end%(256*256)) );
	env1->SetValue("T0Num", (int)t0_num );
	env1->SetValue("TimeNum", (int)(time_num) );

	// write to file
	tree1->Write();
	env1->Write();
	rootfile1->Close();	

	return 1;
}

int convertRPMT(const char *filename, const unsigned int lld) {
	// use DisplayRPMT global variables
	return convertRPMT(filename, lld, DisplayRPMT::GetXYLimit() );
}

int convertRPMT(const char *filename) {
	// use DisplayRPMT global variables
	return convertRPMT(filename, DisplayRPMT::GetLLD(), DisplayRPMT::GetXYLimit() );
}

// Some sweet functions /////////////////////////////////

int save() {
	DisplayRPMT::Save();
	return 0;
}

int xRange(const int xs, const int xe,  int nbin){
	cout << "X range [" << DisplayRPMT::GetXStart() << ", " << DisplayRPMT::GetXEnd() <<"]" << endl;
	DisplayRPMT::SetXStart(xs);
	DisplayRPMT::SetXEnd(xe);		
	cout << " =>     [" << DisplayRPMT::GetXStart() << ", " << DisplayRPMT::GetXEnd() <<"]" << endl;	
	readRPMT( DisplayRPMT::GetFilename(), nbin );
	return 0;
}

int yRange(const int ys, const int ye,  int nbin){
	cout << "Y range [" << DisplayRPMT::GetYStart() << ", " << DisplayRPMT::GetYEnd() <<"]" << endl;
	DisplayRPMT::SetYStart(ys);
	DisplayRPMT::SetYEnd(ye);		
	cout << " =>     [" << DisplayRPMT::GetYStart() << ", " << DisplayRPMT::GetYEnd() <<"]" << endl;	
	readRPMT( DisplayRPMT::GetFilename(), nbin);
	return 0;
}

int saveTOF(const char *filename) {
	TH1D* htoftemp;
	htoftemp=DisplayRPMT::GetHTOF();
	int nline = htoftemp->GetXaxis()->GetNbins();

	ofstream fileTOF;
	fileTOF.open(filename);
	for (int i=0;i<nline;i++){
		// cout << htoftemp->GetBinContent(i) << endl;
		fileTOF << htoftemp->GetBinCenter(i) << " " << htoftemp->GetBinContent(i) << "\n";		
	}
	fileTOF.close();
	return 0;

}

int lld(unsigned int l, int nbin) {
	DisplayRPMT::SetLLD(l);
	readRPMT( DisplayRPMT::GetFilename(), nbin );
	return 0;
}

int ph(unsigned int l, unsigned int h, int nbin) {
	DisplayRPMT::SetLLD(l);
	DisplayRPMT::SetHLD(h);
	readRPMT( DisplayRPMT::GetFilename(), nbin);
	return 0;
}

int xy(unsigned int x, int nbin) {
	DisplayRPMT::SetXYLimit(x);
	readRPMT( DisplayRPMT::GetFilename(), nbin);
	return 0;
}

int tof(double t1, double t2, int nbin) {
	cout << "        tof start: " << DisplayRPMT::GetTOF1() << ", end: " << DisplayRPMT::GetTOF2() <<"\n" << endl;
	DisplayRPMT::SetTOF1(t1);
	DisplayRPMT::SetTOF2(t2);
	readRPMT( DisplayRPMT::GetFilename(), nbin);
	cout << " =>     tof start: " << DisplayRPMT::GetTOF1() << ", end: " << DisplayRPMT::GetTOF2() <<"\n" << endl;
	return 0;
}

int reset(int nbin) {
	DisplayRPMT::SetLLD(128);
	DisplayRPMT::SetHLD(1024);
	DisplayRPMT::SetTOF1(0);
	DisplayRPMT::SetTOF2(8);
	DisplayRPMT::SetXYLimit(32);
	readRPMT( DisplayRPMT::GetFilename(), nbin);
	return 0;
}
