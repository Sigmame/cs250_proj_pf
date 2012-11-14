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

#define DEFAULT_IMAGE_HEIGHT (388)//(388)
#define DEFAULT_IMAGE_WIDTH (584)//(584)
#define DEFAULT_NUM_IMAGES (1)

extern double *filterKernel;
extern double  filterScale;
extern int32_t intKernel[WINDOW_SIZE];

extern uint8_t *inputImagesBuffer;
extern int64_t *outputImagesBuffer;
extern uint32_t imageWidth, imageHeight, imageBufferSize;

extern int64_t *Ex, *Ey, *Et, *D;
extern int64_t *u, *v, *uAvg, *vAvg, *P;
extern double *u_out, *v_out;

extern double divParam;

int main (int argc, char* argv[]) {
  // set default values
  imageHeight = DEFAULT_IMAGE_HEIGHT;
  imageWidth  = DEFAULT_IMAGE_WIDTH;
  int numImages = DEFAULT_NUM_IMAGES;

  printf("Number of images to simulate: %d\n", numImages);

  printf("[STATUS] Loading filter coefficients...\n");
  // generate coefficient values
  generateFilterKernel(filterKernel, filterScale, intKernel);
  allocateImageBuffers(imageWidth, imageHeight);

  // import input image file
  fstream img1("../img/im1_in.txt", ios::in); // test(10*8)|img(388*584) (two different set)
  fstream img2("../img/im2_in.txt", ios::in);
  if(!img1.is_open() || !img2.is_open())
  {
    cout << "Image file(s) not found!\n";
    return 1;
  }
  importInputImage(img1, inputImagesBuffer, imageBufferSize); img1.close();
  importInputImage(img2, &inputImagesBuffer[imageBufferSize], imageBufferSize); img2.close();
  printf("[STATUS] Loading images, dimensions : %d x %d\n", imageWidth, imageHeight);
  
  // generated expected output image for current input
  convolutionFilter(intKernel, 2, inputImagesBuffer, outputImagesBuffer, imageWidth, imageHeight);
  convolutionFilter(intKernel, 2, &inputImagesBuffer[imageBufferSize], &outputImagesBuffer[imageBufferSize], imageWidth, imageHeight);
  printf("[STATUS] Images gaussian convolution done\n");

  allocateIntermediateVariables(imageWidth, imageHeight);
  partialDerivative(outputImagesBuffer, &outputImagesBuffer[imageBufferSize], divParam, imageWidth, imageHeight);
  printf("[STATUS] Images partial derivative calculation done\n");
  motionVectorInteration(ITERATION_NUM, imageWidth, imageHeight);
  printf("[STATUS] Motion vector iteration done\n");

  /*
  fstream ExFile("../Ex.txt", ios::out);
  fstream EyFile("../Ey.txt", ios::out);
  fstream EtFile("../Et.txt", ios::out);
  exportOutputVector(ExFile, Ex, imageWidth, imageHeight); ExFile.close();
  exportOutputVector(EyFile, Ey, imageWidth, imageHeight); EyFile.close();
  exportOutputVector(EtFile, Et, imageWidth, imageHeight); EtFile.close();
  */
  
  fstream uFile("../u.txt", ios::out);
  fstream vFile("../v.txt", ios::out);
  if(!uFile.is_open() || !vFile.is_open())
  {
	  cout << "No file(s) open for write!\n";
	  return 1;
  }
  exportOutputVector(uFile, u_out, imageWidth, imageHeight); uFile.close();
  exportOutputVector(vFile, v_out, imageWidth, imageHeight); vFile.close();

  /*
  printf("Ex\tEy\tEt\tD\tP\n");
  for (int i=0;i<imageBufferSize;i++)
	  printf("Ex=%I64d  Ey=%I64d  Et=%I64d  D=%I64d  P=%I64d  u=%I64d  v=%I64d\n",Ex[i],Ey[i],Et[i],D[i],P[i],u[i],v[i]);
  */

  free(inputImagesBuffer);
  free(outputImagesBuffer);
  free(Ex); free(Ey); free(Et); free(D);
  free(u); free(v); free(uAvg); free(vAvg); free(P);
  free(u_out); free(v_out);

  return 0;
}
