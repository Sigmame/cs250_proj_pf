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

void allocateImageBuffers(int width, int height);
void importInputImage(istream &input, uint8_t *buf, int len);
void generateFilterKernel(double *doubleInputKernel, double kernelScaleFactor, double *doubleOutputKernel, int32_t *intOutputKernel);
void convolutionFilter(double *kernel, int radius, uint8_t *in, int32_t *out, int width, int height);
void partialDerivative(double *img1, double *img2, int width, int height);
void exportOutputVector(ostream &output, int32_t *buf, int width, int height);

#endif
