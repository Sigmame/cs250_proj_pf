#include "hsOptFlow.h"
#include <assert.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

#include <iostream>
#include <fstream>
#include <sstream>
using namespace std;

double gaussianKernel5x5[] = \
  { 2.0,  4.0,  5.0,  4.0, 2.0, \
    4.0,  9.0, 12.0,  9.0, 4.0, \
    5.0, 12.0, 15.0, 12.0, 5.0, \
    4.0,  9.0, 12.0,  9.0, 4.0, \
    2.0,  4.0,  5.0,  4.0, 2.0 };

double* filterKernel = gaussianKernel5x5;
double  filterScale  = 115.0;
int32_t intKernel[WINDOW_SIZE];

uint32_t imageWidth, imageHeight, imageSize = 0;
uint8_t  *inputImages = NULL;
int64_t  *outputImages = NULL;

int64_t *Ex, *Ey, *Et, *D = NULL; // intermediate variable arrays
int64_t *u, *v, *uAvg, *vAvg, *P = NULL; // iteration variable arrays
double  *u_out, *v_out = NULL; // output

double divParam = 0.01; //divider parameter

void allocateImageBuffers(int width, int height)
{
  imageSize = width*height;

  inputImages  = (uint8_t*) malloc(sizeof(uint8_t)*imageSize*2); assert(inputImages);//two images
  outputImages = (int64_t*) malloc(sizeof(int64_t)*imageSize*2); assert(outputImages);//two images after gaussian filter (int64_t)
  
  memset(inputImages,  0, sizeof(uint8_t)*imageSize*2);
  memset(outputImages, 0, sizeof(int64_t)*imageSize*2);
}

void allocateIntermediateVariables(int width, int height)
{
  Ex = (int64_t*) malloc(sizeof(int64_t)*imageSize); assert(Ex); //x-gradient
  Ey = (int64_t*) malloc(sizeof(int64_t)*imageSize); assert(Ey); //y-gradient
  Et = (int64_t*) malloc(sizeof(int64_t)*imageSize); assert(Et); //time-difference
  D  = (int64_t*) malloc(sizeof(int64_t)*imageSize); assert(D); //divider term: alpha+Ex^2+Ey^2

  u    = (int64_t*) malloc(sizeof(int64_t)*imageSize); assert(u); // u motion vector
  v    = (int64_t*) malloc(sizeof(int64_t)*imageSize); assert(v); // v motion vector
  uAvg = (int64_t*) malloc(sizeof(int64_t)*imageSize); assert(uAvg); // u avg
  vAvg = (int64_t*) malloc(sizeof(int64_t)*imageSize); assert(vAvg); // v avg
  P    = (int64_t*) malloc(sizeof(int64_t)*imageSize); assert(P); //product term: Ex*uAvg+Ey*vAvg+Et

  u_out = (double*) malloc(sizeof(double)*imageSize); assert(u); // u motion vector
  v_out = (double*) malloc(sizeof(double)*imageSize); assert(v); // v motion vector

  memset(Ex, 0, sizeof(int64_t)*imageSize);
  memset(Ey, 0, sizeof(int64_t)*imageSize);
  memset(Et, 0, sizeof(int64_t)*imageSize);  
  memset(D,  0, sizeof(int64_t)*imageSize);

  memset(u,    0, sizeof(int64_t)*imageSize);
  memset(v,    0, sizeof(int64_t)*imageSize);
  memset(uAvg, 0, sizeof(int64_t)*imageSize);
  memset(vAvg, 0, sizeof(int64_t)*imageSize);
  memset(P,    0, sizeof(int64_t)*imageSize);

  memset(u_out, 0, sizeof(double)*imageSize);
  memset(v_out, 0, sizeof(double)*imageSize);
}

void importInputImage(istream &input, uint8_t *buf, int len)
{
	int i=0;
	string csvLine;
	while (getline(input, csvLine))
	{
		istringstream csvStream(csvLine);
		string csvElement;
		while(getline(csvStream, csvElement, ','))
		{
			int tmpInt = 0;
			istringstream(csvElement) >> tmpInt;
			buf[i++] = (uint8_t)tmpInt;
			if (i == len)
				return;
		}
	}
}

void generateFilterKernel(double *doubleInputKernel, double kernelScaleFactor, int32_t *intOutputKernel)
{
  double dtmp;
  for (int i=0;i<WINDOW_SIZE;i++)
  {
    dtmp = doubleInputKernel[i] / kernelScaleFactor;
    intOutputKernel[i] = (int32_t) (dtmp * FP_SCALE_FACTOR);
  }
}

void convolutionFilter(int32_t *kernel, int radius, uint8_t *in, int64_t *outInt, int width, int height)
{
  // copy top and bottom lines from input to output unchanged
  for (int y=0; y<radius; y++)
    for (int x=0; x<width; x++)
    {
	  outInt[x+y*width] = ((int64_t)in[x+y*width]) << FP_FRACT;
      int offset = (height-1-y)*width;
	  outInt[x+offset] = ((int64_t)in[x+offset]) << FP_FRACT;
    }

  for (int y=radius; y<height-radius; y++)
  {
    // copy first and last pixels in each line from input to output unchanged
    int offset=y*width;
    for (int i=0;i<radius;i++) {
	  outInt[offset+i] = ((int64_t)in[offset+i]) << FP_FRACT;
	  outInt[offset+width-(i+1)] = ((int64_t)in[offset+width-(i+1)]) << FP_FRACT;
    }

    for (int x=radius; x<width-radius; x++)
    {
      int64_t outputInt = 0;
      int pos = 0;
      int diameter = 2*radius+1;
      for (int yy=0;yy<diameter;yy++)
        for (int xx=0;xx<diameter;xx++)
	      outputInt += (int64_t)in[(x+xx-radius)+((y+yy-radius)*width)] * (int64_t)kernel[pos++];

	  outInt[x+y*width] = outputInt;
    }
  }
}

void partialDerivative(int64_t *img1, int64_t *img2, double alpha, int width, int height)
{
	int64_t alphaInt = (int64_t)(alpha * FP_SCALE_FACTOR * FP_SCALE_FACTOR);
	int64_t E1, E2, tmpD = 0;
	for (int y=0;y<height-1;y++)
	{
		for (int x=0;x<width-1;x++)
		{
			E1 = img1[x+1 + y*width] - img1[x + y*width] + img1[x+1 + (y+1)*width] - img1[x + (y+1)*width];
			E2 = img2[x+1 + y*width] - img2[x + y*width] + img2[x+1 + (y+1)*width] - img2[x + (y+1)*width];
			Ex[x + y*width] = (E1 + E2) >> 2; //calculate X gradient
			
			E1 = img1[x + (y+1)*width] - img1[x + y*width] + img1[x+1 + (y+1)*width] - img1[x+1 + y*width];
			E2 = img2[x + (y+1)*width] - img2[x + y*width] + img2[x+1 + (y+1)*width] - img2[x+1 + y*width];
			Ey[x + y*width] = (E1 + E2) >> 2; //calculate Y gradient
			
			E1 = img2[x + y*width] - img1[x + y*width] + img2[x + (y+1)*width] - img1[x + (y+1)*width];
			E2 = img2[x+1 + y*width] - img1[x+1 + y*width] + img2[x+1 + (y+1)*width] - img1[x+1 + (y+1)*width];
			Et[x + y*width] = (E1 + E2) >> 2; //calculate T gradient

			tmpD = alphaInt + Ex[x + y*width]*Ex[x + y*width] + Ey[x + y*width]*Ey[x + y*width];
			D[x + y*width] = tmpD >> FP_FRACT; // track the point
		}
	}
}


void motionVectorInteration(int iter, int width, int height)
{
	int64_t div06 = (int64_t)((1.0/6.0) * FP_SCALE_FACTOR); //UFix(10923,16) ~ 0.1667
	int64_t div12 = (int64_t)((1.0/12.0) * FP_SCALE_FACTOR); //UFix(5461,16) ~ 0.0833
	int64_t avg1, avg2, tmpP = 0;
	for (int t=0;t<iter;t++) // simply iteration
	{
		for (int y=1;y<height-1;y++)
		{
			for (int x=1;x<width-1;x++)
			{
				avg1 = u[x + (y-1)*width] + u[x+1 + y*width] + u[x + (y+1)*width] + u[x-1 + y*width]; //delta 1
				avg2 = u[x-1 + (y-1)*width] + u[x+1 + (y-1)*width] + u[x+1 + (y+1)*width] + u[x-1 + (y+1)*width]; //delta 2
				uAvg[x + y*width] = (avg1*div06 + avg2*div12) >> FP_FRACT; // calculate average u vector

				avg1 = v[x + (y-1)*width] + v[x+1 + y*width] + v[x + (y+1)*width] + v[x-1 + y*width]; 
				avg2 = v[x-1 + (y-1)*width] + v[x+1 + (y-1)*width] + v[x+1 + (y+1)*width] + v[x-1 + (y+1)*width]; 
				vAvg[x + y*width] = (avg1*div06 + avg2*div12) >> FP_FRACT; // calculate average v vector
			}
		}

		for (int y=0;y<height-1;y++)
			for (int x=0;x<width-1;x++)
			{
				tmpP = Ex[x+y*width]*uAvg[x+y*width] + Ey[x+y*width]*vAvg[x+y*width] + (Et[x+y*width] << FP_FRACT); // calculate P term
				P[x+y*width] = tmpP >> FP_FRACT;
			}

		for (int y=1;y<height-1;y++)
		{
			for (int x=1;x<width-1;x++)
			{	
				u[x+y*width] = uAvg[x+y*width] - (P[x+y*width]*Ex[x+y*width]/D[x+y*width]);
				v[x+y*width] = vAvg[x+y*width] - (P[x+y*width]*Ey[x+y*width]/D[x+y*width]);
			}
		}
	}

	for (int i=0;i<imageSize;i++)
	{
		u_out[i] = ((double)u[i])/FP_SCALE_FACTOR;
		v_out[i] = ((double)v[i])/FP_SCALE_FACTOR;
	}
}

void exportOutputVector(ostream &output, double *buf, int width, int height)
{
	for (int y=0;y<height;y++)
	{
		for (int x=0;x<width;x++)
		{
			char a[DISPLAY_DIGITS];
			sprintf(a, "%.22f", buf[x+y*width]);
			output << a;
			if (x != width-1)
				output << ",";
		}
		output << "\n";
	}
}
