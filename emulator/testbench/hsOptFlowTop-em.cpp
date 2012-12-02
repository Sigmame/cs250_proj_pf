#include "hsOptFlow.h"
#include "hsOptFlowTop-em.h"
#include "hsOptFlowTop.h" //scala generated-src

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <ctype.h>
#include <errno.h>
#include <unistd.h> //unix

#include <iostream>
#include <fstream>
#include <sstream>
using namespace std;

#define DEFAULT_IMAGE_HEIGHT (256)//(388)
#define DEFAULT_IMAGE_WIDTH (512)//(584)
#define DEFAULT_NUM_IMAGES (1)

extern double *filterKernel;
extern double  filterScale;
extern int32_t intKernel[WINDOW_SIZE];

extern uint8_t *inputImages;
extern int64_t *outputImages;
extern uint32_t imageWidth, imageHeight, imageSize;

extern int64_t *Ex, *Ey, *Et, *D;
extern int64_t *u, *v, *uAvg, *vAvg, *P;
extern double *u_out, *v_out;

extern double divParam;

int main (int argc, char* argv[]) {
  int cycle;
  int lim = 0;
  int done = 0;
  int fail = 0;
  int imageFailed = 0;

  int64_t img1_expected = 0;
  int64_t u_expected = 0;
  int64_t v_expected = 0;
  int64_t Ex_expected = 0;
  int64_t Ey_expected = 0;
  int64_t Et_expected = 0;
  int64_t P_expected = 0;
  int64_t D_expected = 0;

  int dout_mismatch = 0;

  // set default values
  imageHeight = DEFAULT_IMAGE_HEIGHT;
  imageWidth  = DEFAULT_IMAGE_WIDTH;
  int numImages = DEFAULT_NUM_IMAGES;
  int print_trace = 0; // enable text trace output    case 't':print_trace = 1;
  int generate_vcd = 1; // enable vcd file generation      case 'v':generate_vcd = 1;
  FILE *vcdFile = NULL;
  const char *vcdFileName = "trace.vcd";

  /* parse command line options
  int ch;
  opterr = 0;
  while ((ch = getopt(argc,argv,"t:v:")) != -1)
  {
    switch(ch)
    {
      // enable text trace output
      case 't':
        print_trace = 1;
        break;
      // enable vcd file generation
      case 'v':
        generate_vcd = 1;
        break;
      default:
        return -1;
    }
  }
  */

  // for loading of input images
  int loadImage = 1;
  int loadImageOffset = 0;
  int loadImageCount = 0;

  // for checking of output images 
  int checkOutput = 0;
  int checkOutputOffset = 0;
  int checkOutputCount = 0;
  
  // number of clock cycles to simulate before exiting
  // if not specified on command line, set based on number
  // of images in simulation
  if (lim == 0)
	lim = numImages*(imageWidth*imageHeight + WINDOW_SIZE + imageWidth*10);
 
  // generate coefficient values (built-in)
  generateFilterKernel(filterKernel, filterScale, intKernel);
  
  // allocate input image and intermediate variable buffers
  allocateImageBuffers(imageWidth, imageHeight);
  allocateIntermediateVariables(imageWidth, imageHeight);

  // open input/output files [PATH fix]   
  fstream img1("./testbench/im1_in.txt", ios::in); 
  fstream img2("./testbench/im2_in.txt", ios::in);
  if(!img1.is_open() || !img2.is_open())
  {
    cout << "Image file(s) not found!\n";
    return -1;
  }
  fstream uFile("u.txt", ios::out);
  fstream vFile("v.txt", ios::out);
  if(!uFile.is_open() || !vFile.is_open())
  {
	cout << "No file(s) open for write!\n";
	return -1;
  }

  // Instantiate and initialize top level Chisel module
  hsOptFlowTop_t* dut = new hsOptFlowTop_t();
  dut->init();

  printf("Image dimensions : %d x %d\n", imageWidth, imageHeight);
  printf("Number of image pairs to be simulated: %d\n", numImages);
  printf("Simulation will timeout after %d clock cycles.\n", lim);
  if (generate_vcd)
    printf("VCD generation enabled, output filename = %s\n", vcdFileName);


  // Start simulation
  // Every loop iteration simulates one clock cycle
  for (cycle = 0; lim < 0 || cycle < lim && !done ; cycle++) {
    // assert reset for 1 cycle at start of simulation
    dat_t<1> reset = LIT<1>(cycle==0);

    // open VCD trace dump file, if option enabled
    if (cycle == 0 && generate_vcd)
    {
      vcdFile = fopen(vcdFileName, "w");
      if (vcdFile == NULL)
      {
        printf("Error opening trace output file %s.\n", vcdFileName);
        perror("fopen: ");
        return -1;
      }
      // add some extra signals to the vcd file to help debugging
      fprintf(vcdFile, "$scope module hsOptFlowTopTestHarness $end\n");
      fprintf(vcdFile, "$var reg 32 NCYCLE cycle $end\n");
      fprintf(vcdFile, "$var reg 26 IMG1_EXP img1_expected $end\n");
      fprintf(vcdFile, "$var reg 32 U_EXP u_expected $end\n");
      fprintf(vcdFile, "$var reg 32 V_EXP v_expected $end\n");
      fprintf(vcdFile, "$var reg 26 EX_EXP Ex_expected $end\n");
      fprintf(vcdFile, "$var reg 26 EY_EXP Ey_expected $end\n");
      fprintf(vcdFile, "$var reg 26 ET_EXP Et_expected $end\n");
      fprintf(vcdFile, "$var reg 32 P_EXP P_expected $end\n");
      fprintf(vcdFile, "$var reg 32 D_EXP D_expected $end\n");
      fprintf(vcdFile, "$var reg 32 MISMATCH dout_mismatch $end\n");
      fprintf(vcdFile, "$upscope $end\n");
    }


    // Set input port values
    // handle input image loading
    uint32_t io_frame_sync_in = 0;
    uint8_t  io_data_in1 = 0;
    uint8_t  io_data_in2 = 0;

    if (cycle != 0 && loadImage)
    {
      if (loadImageOffset == 0)
      { 
        importInputImage(img1, inputImages, imageSize); img1.close();
	 importInputImage(img2, &inputImages[imageSize], imageSize); img2.close();
        printf("[STATUS] Loading image pair %d, dimensions : %d x %d\n", loadImageCount+1, imageWidth, imageHeight);
        io_frame_sync_in = 1;
      }
      io_data_in1 = inputImages[loadImageOffset++];
      io_data_in2 = inputImages[imageSize+loadImageOffset-1];
      if (loadImageOffset == imageSize)
      {
        loadImageCount++;
        loadImageOffset = 0;
        if (loadImageCount == numImages)
          loadImage = 0;
        else
          loadImage = 1;
      }
    }

    dut->hsOptFlowTop__io_data_in1   = LIT<8>(io_data_in1);
    dut->hsOptFlowTop__io_data_in2   = LIT<8>(io_data_in2);
    dut->hsOptFlowTop__io_frame_sync_in = LIT<1>(io_frame_sync_in);

    // handle image dimension setup
//    dut->hsOptFlowTop__io_image_width = LIT<10>(imageWidth-1);
//    dut->hsOptFlowTop__io_image_height = LIT<10>(imageHeight-1);

    // advance simulation
    dut->clock_lo(reset);
 
    // examine output port values
    // if frame_sync_out is asserted, start verification
    if (dut->hsOptFlowTop__io_frame_sync_out.lo_word())
    {
         // generated expected output image for current input
         convolutionFilter(intKernel, 2, inputImages, outputImages, imageWidth, imageHeight);
	  convolutionFilter(intKernel, 2, &inputImages[imageSize], &outputImages[imageSize], imageWidth, imageHeight); 
	  //printf("[STATUS] Images gaussian convolution done\n");
	  partialDerivative(outputImages, &outputImages[imageSize], divParam, imageWidth, imageHeight);
	  //printf("[STATUS] Images partial derivative calculation done\n");
	  motionVectorInteration(ITERATION_NUM, imageWidth, imageHeight);
	  //printf("[STATUS] Motion vector iteration done\n");
      checkOutput = 1;
      checkOutputOffset = 0;
    }

    if (checkOutput)
    {
      int64_t data_out_u = (int64_t) dut->hsOptFlowTop__io_data_out_u.lo_word();
      int64_t data_out_v = (int64_t) dut->hsOptFlowTop__io_data_out_v.lo_word();
      int64_t out_P = (int64_t) dut->hsOptFlowTop_iterCalc_uvCalculation__io_P.lo_word();
      int64_t out_D = (int64_t) dut->hsOptFlowTop_iterCalc_uvCalculation__io_D.lo_word();

      img1_expected = outputImages[checkOutputOffset];
      u_expected = u[checkOutputOffset]; // expected
      v_expected = v[checkOutputOffset]; 
      Ex_expected = Ex[checkOutputOffset];
      Ey_expected = Ey[checkOutputOffset];
      Et_expected = Et[checkOutputOffset];
      P_expected = P[checkOutputOffset];
      D_expected = D[checkOutputOffset];

       int mask = (1 << 26) - 1;
      dout_mismatch = 0;
      if ((data_out_u & mask) != (u_expected & mask) || (data_out_v & mask) != (v_expected & mask))
      {
        printf("Verification failed at cycle %6d! pixel at offset %5d u_expected: %02x u_actual: %02x \n", cycle, checkOutputOffset, u_expected, data_out_u);
        printf("Verification failed at cycle %6d! pixel at offset %5d v_expected: %02x v_actual: %02x \n", cycle, checkOutputOffset, v_expected, data_out_v);
        fail = 1;
        imageFailed = 1;
        dout_mismatch = 1;
      }

      // end of image
      if(checkOutputOffset == imageSize-1)
      {
        checkOutputCount++;
        if (!imageFailed)
          printf("[PASSED] Image pair %d processed succesfully.\n", checkOutputCount);
        else
          printf("[FAILED] HS_optflow output for input image pair %d was incorrect!\n", checkOutputCount);
  
        imageFailed = 0;
        checkOutputOffset = 0;
        checkOutput = 1;
        
        if (checkOutputCount == numImages)
          done=1;

      }
      else
        checkOutputOffset++;
    }

    if (print_trace) {
      // Print the values of the input and output signals
      uint32_t frame_sync_in  = dut->hsOptFlowTop__io_frame_sync_in.lo_word();
      uint8_t data_in1        = dut->hsOptFlowTop__io_data_in1.lo_word();
      uint8_t data_in2        = dut->hsOptFlowTop__io_data_in2.lo_word();

      // outputs
      uint32_t frame_sync_out = dut->hsOptFlowTop__io_frame_sync_out.lo_word();
      int64_t data_out_u        = dut->hsOptFlowTop__io_data_out_u.lo_word();
      int64_t data_out_v        = dut->hsOptFlowTop__io_data_out_v.lo_word();

      printf("cycle: %04d frame_sync_in: %d data_in1: %02x data_in2: %02x frame_sync_out: %d data_out_u: %02x data_out_v: %02x\n", \
          cycle, frame_sync_in, data_in1, data_in1, frame_sync_out, data_out_u, data_out_v);
    }

    // write trace output to VCD file
    if (generate_vcd)
    {
      dut->dump(vcdFile, cycle);
      // add extra signals for debugging
      // cycle count
      dat_dump(vcdFile, dat_t<32>(cycle), "NCYCLE");
      // expected value (for verification)
      dat_dump(vcdFile, dat_t<26>(img1_expected), "IMG1_EXP");
      dat_dump(vcdFile, dat_t<32>(u_expected), "U_EXP");
      dat_dump(vcdFile, dat_t<32>(v_expected), "V_EXP");
      dat_dump(vcdFile, dat_t<26>(Ex_expected), "EX_EXP");
      dat_dump(vcdFile, dat_t<26>(Ey_expected), "EY_EXP");
      dat_dump(vcdFile, dat_t<26>(Et_expected), "ET_EXP");
      dat_dump(vcdFile, dat_t<32>(P_expected), "P_EXP");
      dat_dump(vcdFile, dat_t<32>(D_expected), "D_EXP");

      // mismatch signal (high when output doesn't match expected output)
      dat_dump(vcdFile, dat_t<32>(dout_mismatch), "MISMATCH");
      
    }

    // advance simulation
    dut->clock_hi(reset);
  }

  if (generate_vcd)
    fclose(vcdFile);

  exportOutputVector(uFile, u_out, imageWidth, imageHeight); uFile.close();
  exportOutputVector(vFile, v_out, imageWidth, imageHeight); vFile.close();
  free(inputImages);
  free(outputImages);
  free(Ex); free(Ey); free(Et); free(D);
  free(u); free(v); free(uAvg); free(vAvg); free(P);
  free(u_out); free(v_out);

  if(fail)
  {
     printf("Failed test! Simulation ended after %d cycles.\n", cycle);
     return -1;
  }

  if(done)
  {
     printf("All tests passed, simulation finished after %d cycles.\n", cycle);
     return 0;
  }

  printf("[Failed] Timed out after %d cycles!\n", cycle);
  return -1;
}
