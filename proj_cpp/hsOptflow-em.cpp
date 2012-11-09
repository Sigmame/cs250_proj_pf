#include "hsOptflow.h"

#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <assert.h>
#include <ctype.h>
#include <errno.h>
//#include <unistd.h> //unix

#include <iostream>
#include <fstream>
#include <sstream>
using namespace std;

#define DEFAULT_IMAGE_HEIGHT (10)//(388)
#define DEFAULT_IMAGE_WIDTH (8)//(584)
#define DEFAULT_NUM_IMAGES (1)

extern double *filterKernel;
extern double  filterScale;
extern uint8_t *inputImagesBuffer;
extern int32_t *outputImagesBuffer;
extern uint32_t imageWidth, imageHeight, imageBufferSize;
extern double doubleKernel[WINDOW_SIZE];
extern int32_t intKernel[WINDOW_SIZE];


int main (int argc, char* argv[]) {
  // set default values
  imageHeight = DEFAULT_IMAGE_HEIGHT;
  imageWidth  = DEFAULT_IMAGE_WIDTH;
  int numImages = DEFAULT_NUM_IMAGES;

  printf("Number of images to simulate: %d\n", numImages);

  printf("[STATUS] Loading filter coefficients...\n");
  // generate coefficient values
  generateFilterKernel(filterKernel, filterScale, doubleKernel, intKernel);
  allocateImageBuffers(imageWidth, imageHeight);

  // import input image file
  fstream img1("../im1.txt", ios::in);
  fstream img2("../im2.txt", ios::in);
  if(!img1.is_open() || !img2.is_open())
  {
    cout << "Image file(s) not found!\n";
    return 1;
  }
  importInputImage(img1, inputImagesBuffer, imageBufferSize); img1.close();
  importInputImage(img2, &inputImagesBuffer[imageBufferSize], imageBufferSize); img2.close();
  printf("[STATUS] Loading images, dimensions : %d x %d\n", imageWidth, imageHeight);
  
  // generated expected output image for current input
  convolutionFilter(doubleKernel, 2, inputImagesBuffer, outputImagesBuffer, imageWidth, imageHeight);
  convolutionFilter(doubleKernel, 2, &inputImagesBuffer[imageBufferSize], &outputImagesBuffer[imageBufferSize], imageWidth, imageHeight);
  printf("[STATUS] Images gaussian convolution done\n");

  fstream u("../u.txt", ios::out);
  fstream v("../v.txt", ios::out);
  if(!u.is_open() || !v.is_open())
  {
	  cout << "No file(s) open for write!\n";
	  return 1;
  }
  exportOutputVector(u,outputImagesBuffer, imageWidth, imageHeight); u.close();
  exportOutputVector(v,&outputImagesBuffer[imageBufferSize], imageWidth, imageHeight); v.close();

  printf("Image 1\n");
  for (int i=0;i<imageBufferSize;i++)
	  printf("input=%5d  output=%15d\n",inputImagesBuffer[i],outputImagesBuffer[i]);
  printf("Image 2\n");
  for (int i=0;i<imageBufferSize;i++)
	  printf("input=%5d  output=%15d\n",inputImagesBuffer[imageBufferSize+i],outputImagesBuffer[imageBufferSize+i]);

  free(inputImagesBuffer);
  free(outputImagesBuffer);

  return 0;
}
