#ifndef _CONVOLUTION_H
#define _CONVOLUTION_H
#include <stdint.h>
#include <stdlib.h>

#include <iostream>
#include <fstream>
#include <sstream>
using namespace std;

#define WINDOW_SIZE (25)
//#define FP_WIDTH (30)
#define FP_FRACT (20) //(16)
#define FP_SCALE_FACTOR (1 << FP_FRACT)
#define ITERATION_NUM (8) //(8)
#define DISPLAY_DIGITS (40)

void allocateImageBuffers(int width, int height);
void allocateIntermediateVariables(int width, int height);
void importInputImage(istream &input, uint8_t *buf, int len);
void generateFilterKernel(double *doubleInputKernel, double kernelScaleFactor, int32_t *intOutputKernel);
void convolutionFilter(int32_t *kernel, int radius, uint8_t *in, int64_t *outInt, int width, int height); // double & int64_t output image
void partialDerivative(int64_t *img1, int64_t *img2, double alpha, int width, int height); // intermediate E's and D-term
void motionVectorInteration(int iter, int width, int height); // intermediate E's and D-term
void exportOutputVector(ostream &output, double *buf, int width, int height);

#endif