#include "hsOptflow.h"
#include <assert.h>
#include <stdio.h>

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

uint32_t imageWidth, imageHeight, imageBufferSize = 0;
uint8_t *inputImagesBuffer = NULL;
int64_t *outputImagesBuffer = NULL;

int64_t *Ex, *Ey, *Et, *D = NULL; // intermediate variable arrays
int64_t *u, *v, *uAvg, *vAvg, *P = NULL; // iteration variable arrays
double *u_out, *v_out = NULL; // output

double divParam = 0.01; //divider parameter

void allocateImageBuffers(int width, int height)
{
  imageBufferSize = width*height;

  inputImagesBuffer = (uint8_t*) malloc(sizeof(uint8_t)*imageBufferSize*2); assert(inputImagesBuffer);//two images
  outputImagesBuffer = (int64_t*) malloc(sizeof(int64_t)*imageBufferSize*2); assert(outputImagesBuffer);//two images after gaussian filter (int64_t)
  
  memset(inputImagesBuffer, 0, sizeof(uint8_t)*imageBufferSize*2);
  memset(outputImagesBuffer, 0, sizeof(int64_t)*imageBufferSize*2);
}

void allocateIntermediateVariables(int width, int height)
{
  Ex = (int64_t*) malloc(sizeof(int64_t)*imageBufferSize); assert(Ex); //x-gradient
  Ey = (int64_t*) malloc(sizeof(int64_t)*imageBufferSize); assert(Ey); //y-gradient
  Et = (int64_t*) malloc(sizeof(int64_t)*imageBufferSize); assert(Et); //time-difference
  D = (int64_t*) malloc(sizeof(int64_t)*imageBufferSize); assert(D); //divider term: alpha+Ex^2+Ey^2

  u = (int64_t*) malloc(sizeof(int64_t)*imageBufferSize); assert(u); // u motion vector
  v = (int64_t*) malloc(sizeof(int64_t)*imageBufferSize); assert(v); // v motion vector
  uAvg = (int64_t*) malloc(sizeof(int64_t)*imageBufferSize); assert(uAvg); // u avg
  vAvg = (int64_t*) malloc(sizeof(int64_t)*imageBufferSize); assert(vAvg); // v avg
  P = (int64_t*) malloc(sizeof(int64_t)*imageBufferSize); assert(P); //product term: Ex*uAvg+Ey*vAvg+Et

  u_out = (double*) malloc(sizeof(double)*imageBufferSize); assert(u); // u motion vector
  v_out = (double*) malloc(sizeof(double)*imageBufferSize); assert(v); // v motion vector

  memset(Ex, 0, sizeof(int64_t)*imageBufferSize);
  memset(Ey, 0, sizeof(int64_t)*imageBufferSize);
  memset(Et, 0, sizeof(int64_t)*imageBufferSize);  
  memset(D, 0, sizeof(int64_t)*imageBufferSize);

  memset(u, 0, sizeof(int64_t)*imageBufferSize);
  memset(v, 0, sizeof(int64_t)*imageBufferSize);
  memset(uAvg, 0, sizeof(int64_t)*imageBufferSize);
  memset(vAvg, 0, sizeof(int64_t)*imageBufferSize);
  memset(P, 0, sizeof(int64_t)*imageBufferSize);

  memset(u_out, 0, sizeof(double)*imageBufferSize);
  memset(v_out, 0, sizeof(double)*imageBufferSize);
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
	  outInt[x+y*width] = (int64_t)(in[x+y*width] * FP_SCALE_FACTOR);
      int offset = (height-1-y)*width;
	  outInt[x+offset] = (int64_t)(in[x+offset] * FP_SCALE_FACTOR);
    }

  for (int y=radius; y<height-radius; y++)
  {
    // copy first and last pixels in each line from input to output unchanged
    int offset=y*width;
    for (int i=0;i<radius;i++) {
	  outInt[offset+i] = (int64_t)(in[offset+i] * FP_SCALE_FACTOR);
	  outInt[offset+width-(i+1)] = (int64_t)(in[offset+width-(i+1)] * FP_SCALE_FACTOR);
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
	int64_t E1, E2 = 0;
	for (int y=0;y<height-1;y++)
	{
		for (int x=0;x<width-1;x++)
		{
			E1 = img1[x+1 + y*width] - img1[x + y*width] + img1[x+1 + (y+1)*width] - img1[x + (y+1)*width];
			E2 = img2[x+1 + y*width] - img2[x + y*width] + img2[x+1 + (y+1)*width] - img2[x + (y+1)*width];
			Ex[x + y*width] = (E1 + E2) / 4; //calculate X gradient
			
			E1 = img1[x + (y+1)*width] - img1[x + y*width] + img1[x+1 + (y+1)*width] - img1[x+1 + y*width];
			E2 = img2[x + (y+1)*width] - img2[x + y*width] + img2[x+1 + (y+1)*width] - img2[x+1 + y*width];
			Ey[x + y*width] = (E1 + E2) / 4; //calculate Y gradient
			
			E1 = img2[x + y*width] - img1[x + y*width] + img2[x + (y+1)*width] - img1[x + (y+1)*width];
			E2 = img2[x+1 + y*width] - img1[x+1 + y*width] + img2[x+1 + (y+1)*width] - img1[x+1 + (y+1)*width];
			Et[x + y*width] = (E1 + E2) / 4; //calculate T gradient

			D[x + y*width] = alphaInt + Ex[x + y*width]*Ex[x + y*width] + Ey[x + y*width]*Ey[x + y*width];
		}
	}
}


void motionVectorInteration(int iter, int width, int height)
{
	int64_t avg1, avg2 = 0;
	for (int t=0;t<iter;t++) // simply iteration
	{
		for (int y=1;y<height-1;y++)
		{
			for (int x=1;x<width-1;x++)
			{
				avg1 = u[x + (y-1)*width] + u[x+1 + y*width] + u[x + (y+1)*width] + u[x-1 + y*width]; //delta 1
				avg2 = u[x-1 + (y-1)*width] + u[x+1 + (y-1)*width] + u[x+1 + (y+1)*width] + u[x-1 + (y+1)*width]; //delta 2
				uAvg[x + y*width] = avg1/6 + avg2/12; // calculate average u vector

				avg1 = v[x + (y-1)*width] + v[x+1 + y*width] + v[x + (y+1)*width] + v[x-1 + y*width]; 
				avg2 = v[x-1 + (y-1)*width] + v[x+1 + (y-1)*width] + v[x+1 + (y+1)*width] + v[x-1 + (y+1)*width]; 
				vAvg[x + y*width] = avg1/6 + avg2/12; // calculate average v vector
			}
		}
		for (int y=0;y<height-1;y++)
			for (int x=0;x<width-1;x++)
				P[x+y*width] = Ex[x+y*width]*uAvg[x+y*width] + Ey[x+y*width]*vAvg[x+y*width] + Et[x+y*width]; // calculate P term

		for (int y=1;y<height-1;y++)
		{
			for (int x=1;x<width-1;x++)
			{	
				u[x+y*width] = uAvg[x+y*width] - (int64_t)(P[x+y*width]*Ex[x+y*width]*FP_SCALE_FACTOR / D[x+y*width]);
				v[x+y*width] = vAvg[x+y*width] - (int64_t)(P[x+y*width]*Ey[x+y*width]*FP_SCALE_FACTOR / D[x+y*width]);
			}
		}
	}

	for (int i=0;i<imageBufferSize;i++)
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
