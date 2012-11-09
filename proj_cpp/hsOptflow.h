#ifndef _CONVOLUTION_H
#define _CONVOLUTION_H
#include <stdint.h>
#include <stdlib.h>

#include <iostream>
#include <fstream>
#include <sstream>
using namespace std;

#define WINDOW_SIZE (25)
#define COEFF_WIDTH (16)
#define COEFF_FRACT (12)
#define COEFF_SCALE_FACTOR (1 << COEFF_FRACT)
#define ITERATION_NUM (8)

void allocateImageBuffers(int width, int height);
void allocateIntermediateVariables(int width, int height);
void importInputImage(istream &input, uint8_t *buf, int len);
void generateFilterKernel(double *doubleInputKernel, double kernelScaleFactor, double *doubleOutputKernel, int32_t *intOutputKernel);
void convolutionFilter(double *kernel, int radius, uint8_t *in, double *out, int width, int height); // double output image
void partialDerivative(double *img1, double *img2, int width, int height); // intermediate E's and D-term
void motionVectorInteration(int iter, int width, int height); // intermediate E's and D-term
void exportOutputVector(ostream &output, double *buf, int width, int height);

#endif
