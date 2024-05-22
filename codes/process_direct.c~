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
#include readRPMT.C


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

void processs(int runin, int idx_s, int idx_e, int tbin){
    readRPMT(Form("../../data/20231024/rpmt_run%i.edr", runin), tbin);
    xRange(idx_s, idx_e, tbin)
    c1->SaveAs(Form("tof/rpmt_run%d.edr", runin))
    saveTOF(Form("tof/rpmt_run%d_%d_%d_%d.csv", runid, idx_s, idx_e, tbin))
}
