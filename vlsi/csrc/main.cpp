#include <DirectC.h>

#include <stdio.h>
#include <sys/stat.h>
#include <fcntl.h>
#include <stdint.h>
#include <assert.h>
#include <stdlib.h>
#include <string.h>
#include <ctype.h>
#include <errno.h>
#include <unistd.h>

#include <iostream>
#include <fstream>
#include <sstream>
using namespace std;

#include "hsOptFlow.h"

extern "C" {
  extern double* filterKernel;
  extern double  filterScale;
  extern int32_t intKernel[WINDOW_SIZE];
  
  extern uint8_t *inputImages;
  extern int64_t *outputImages;
  extern uint32_t imageWidth, imageHeight, imageSize;

  extern int64_t *Ex, *Ey, *Et, *D;
  extern int64_t *u, *v, *uAvg, *vAvg, *P;
  extern double  *u_out, *v_out;

  extern double divParam;

  void initialize_image_buffers(const vec32* width, const vec32* height)
  {
    assert((width->c == 0) && (height->c == 0));
    imageWidth = width->d;
    imageHeight = height->d;
    // generate coefficient values (built-in)
    generateFilterKernel(filterKernel, filterScale, intKernel);

    // allocate input image and intermediate variable buffers
    allocateImageBuffers(imageWidth, imageHeight);
    allocateIntermediateVariables(imageWidth, imageHeight);
  }

  void generate_input_image()
  {
    assert(imageSize > 0);
    // open input/output files [PATH fix]   
    fstream img1("./testbench/im1_in.txt", ios::in); // test(10*8)|img(388*584) (two different set)
    fstream img2("./testbench/im2_in.txt", ios::in);
    if(!img1.is_open() || !img2.is_open())
    {
      cout << "Image file(s) not found!\n";
      return -1;
    }
    importInputImage(img1, inputImages, imageSize); img1.close();
    importInputImage(img2, &inputImages[imageSize], imageSize); img2.close();
  }

  void generate_output_image()
  {
    assert(imageSize > 0);
    // generated expected output image for current input
    convolutionFilter(intKernel, 2, inputImages, outputImages, imageWidth, imageHeight);
    convolutionFilter(intKernel, 2, &inputImages[imageSize], &outputImages[imageSize], imageWidth, imageHeight); 
    //printf("[STATUS] Images gaussian convolution done\n");
    partialDerivative(outputImages, &outputImages[imageSize], divParam, imageWidth, imageHeight);
    //printf("[STATUS] Images partial derivative calculation done\n");
    motionVectorInteration(ITERATION_NUM, imageWidth, imageHeight);
    //printf("[STATUS] Motion vector iteration done\n");

    fstream uFile("u.txt", ios::out);
    fstream vFile("v.txt", ios::out);
    if(!uFile.is_open() || !vFile.is_open())
    {
	cout << "No file(s) open for write!\n";
	return -1;
    }
    exportOutputVector(uFile, u_out, imageWidth, imageHeight); uFile.close();
    exportOutputVector(vFile, v_out, imageWidth, imageHeight); vFile.close();
  }

  void get_input_pixel(const vec32* offset, /* OUTPUT*/ vec32 *dout)
  {
    if (offset->c != 0) // x's or z's on the input signal
    {
      dout->c = 0xFFFFFFFF;
      dout->d = 0;
    }
    else
    {
      assert(offset->d < imageSize);
      dout->c = 0;
      dout->d = inputImages[offset->d];
    }
  }

  void get_output_pixel(const vec32* offset, /* OUTPUT*/ vec32 *dout)
  {
    if (offset->c != 0) // x's or z's on the input signal
    {
      dout->c = 0xFFFFFFFF;
      dout->d = 0;
    }
    else
    {
      assert(offset->d < imageSize*2);
      dout->c = 0;
      if (offset < imageSize)
	 dout->d = u[offset->d];
      else
	 dout->d = v[offset->d - imageSize];
    }
  }

}
