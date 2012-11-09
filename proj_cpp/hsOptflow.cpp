#include "hsOptflow.h"
#include <assert.h>

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

double doubleKernel[WINDOW_SIZE];
int32_t intKernel[WINDOW_SIZE];

uint32_t imageWidth, imageHeight, imageBufferSize = 0;
uint8_t *inputImagesBuffer = NULL;
double *outputImagesBuffer = NULL;

double alpha = 0.01; //divider parameter
double *Ex, *Ey, *Et, *D = NULL; // intermediate variable arrays
double *u, *v, *uAvg, *vAvg, *P = NULL; // iteration variable arrays

void allocateImageBuffers(int width, int height)
{
  imageBufferSize = width*height;

  inputImagesBuffer = (uint8_t*) malloc(sizeof(uint8_t)*imageBufferSize*2); //two images
  assert(inputImagesBuffer);
  outputImagesBuffer = (double*) malloc(sizeof(double)*imageBufferSize*2); //two images after gaussian filter
  assert(outputImagesBuffer);

  memset(inputImagesBuffer, 0, sizeof(uint8_t)*imageBufferSize*2);
  memset(outputImagesBuffer, 0, sizeof(double)*imageBufferSize*2);
}

void allocateIntermediateVariables(int width, int height)
{
  Ex = (double*) malloc(sizeof(double)*imageBufferSize); assert(Ex); //x-gradient
  Ey = (double*) malloc(sizeof(double)*imageBufferSize); assert(Ey); //y-gradient
  Et = (double*) malloc(sizeof(double)*imageBufferSize); assert(Et); //time-difference
  D = (double*) malloc(sizeof(double)*imageBufferSize); assert(D); //divider term: alpha+Ex^2+Ey^2

  u = (double*) malloc(sizeof(double)*imageBufferSize); assert(u); // u motion vector
  v = (double*) malloc(sizeof(double)*imageBufferSize); assert(v); // v motion vector
  uAvg = (double*) malloc(sizeof(double)*imageBufferSize); assert(uAvg); // u avg
  vAvg = (double*) malloc(sizeof(double)*imageBufferSize); assert(vAvg); // v avg
  P = (double*) malloc(sizeof(double)*imageBufferSize); assert(P); //product term: Ex*uAvg+Ey*vAvg+Et

  memset(Ex, 0, sizeof(double)*imageBufferSize);
  memset(Ey, 0, sizeof(double)*imageBufferSize);
  memset(Et, 0, sizeof(double)*imageBufferSize);  
  memset(D, 0, sizeof(double)*imageBufferSize);

  memset(u, 0, sizeof(double)*imageBufferSize);
  memset(v, 0, sizeof(double)*imageBufferSize);
  memset(uAvg, 0, sizeof(double)*imageBufferSize);
  memset(vAvg, 0, sizeof(double)*imageBufferSize);
  memset(P, 0, sizeof(double)*imageBufferSize);
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

void generateFilterKernel(double *doubleInputKernel, double kernelScaleFactor, double *doubleOutputKernel, int32_t *intOutputKernel)
{
  double ftmp;
  for (int i=0;i<WINDOW_SIZE;i++)
  {
    ftmp = doubleInputKernel[i] / kernelScaleFactor;
    intOutputKernel[i] = (int32_t) (ftmp * COEFF_SCALE_FACTOR);
    doubleOutputKernel[i] = ((double)intOutputKernel[i]) / COEFF_SCALE_FACTOR;
  }
}

void convolutionFilter(double *kernel, int radius, uint8_t *in, double *out, int width, int height)
{
  // copy top and bottom lines from input to output unchanged
  for (int y=0; y<radius; y++)
    for (int x=0; x<width; x++)
    {
      out[x+y*width] = (int32_t)in[x+y*width];
      int offset = (height-1-y)*width;
      out[x+offset] = (int32_t)in[x+offset];
    }

  for (int y=radius; y<height-radius; y++)
  {
    // copy first and last pixels in each line from input to output unchanged
    int offset=y*width;
    for (int i=0;i<radius;i++) {
      out[offset+i] = (int32_t)in[offset+i];
      out[offset+width-(i+1)] = (int32_t)in[offset+width-(i+1)];
    }

    for (int x=radius; x<width-radius; x++)
    {
      double outputDouble = 0;
      int pos = 0;
      int diameter = 2*radius+1;
      for (int yy=0;yy<diameter;yy++)
        for (int xx=0;xx<diameter;xx++)
	      outputDouble += ((double)(in[(x+xx-radius) + ((y+yy-radius)*width)])) * kernel[pos++];

      out[x+y*width] = outputDouble;
    }
  }
}

void partialDerivative(double *img1, double *img2, int width, int height)
{
	double E1, E2 = 0;
	for (int y=0;y<height-1;y++)
	{
		for (int x=0;x<width-1;x++)
		{
			E1 = img1[x+1 + y*width] - img1[x + y*width] + img1[x+1 + (y+1)*width] - img1[x + (y+1)*width];
			E2 = img2[x+1 + y*width] - img2[x + y*width] + img2[x+1 + (y+1)*width] - img2[x + (y+1)*width];
			Ex[x + y*width] = (E1 + E2)/4.0; //calculate X gradient
			
			E1 = img1[x + (y+1)*width] - img1[x + y*width] + img1[x+1 + (y+1)*width] - img1[x+1 + y*width];
			E2 = img2[x + (y+1)*width] - img2[x + y*width] + img2[x+1 + (y+1)*width] - img2[x+1 + y*width];
			Ey[x + y*width] = (E1 + E2)/4.0; //calculate Y gradient
			
			E1 = img2[x + y*width] - img1[x + y*width] + img2[x + (y+1)*width] - img1[x + (y+1)*width];
			E2 = img2[x+1 + y*width] - img1[x+1 + y*width] + img2[x+1 + (y+1)*width] - img1[x+1 + (y+1)*width];
			Et[x + y*width] = (E1 + E2)/4.0; //calculate T gradient

			D[x + y*width] = alpha + Ex[x + y*width]*Ex[x + y*width] + Ey[x + y*width]*Ey[x + y*width];
		}
	}
}


void motionVectorInteration(int iter, int width, int height)
{
	double avg1, avg2 = 0;
	for (int t=0;t<iter;t++) // simply iteration
	{
		for (int y=1;y<height-1;y++)
		{
			for (int x=1;x<width-1;x++)
			{
				avg1 = u[x + (y-1)*width] + u[x+1 + y*width] + u[x + (y+1)*width] + u[x-1 + y*width]; //delta 1
				avg2 = u[x-1 + (y-1)*width] + u[x+1 + (y-1)*width] + u[x+1 + (y+1)*width] + u[x-1 + (y+1)*width]; //delta 2
				uAvg[x + y*width] = avg1/6.0 + avg2/12.0; // calculate average u vector

				avg1 = v[x + (y-1)*width] + v[x+1 + y*width] + v[x + (y+1)*width] + v[x-1 + y*width]; 
				avg2 = v[x-1 + (y-1)*width] + v[x+1 + (y-1)*width] + v[x+1 + (y+1)*width] + v[x-1 + (y+1)*width]; 
				vAvg[x + y*width] = avg1/6.0 + avg2/12.0; // calculate average v vector
			}
		}
		for (int y=0;y<height-1;y++)
			for (int x=0;x<width-1;x++)
				P[x+y*width] = Ex[x+y*width]*uAvg[x+y*width] + Ey[x+y*width]*vAvg[x+y*width] + Et[x+y*width]; // calculate P term

		for (int y=1;y<height-1;y++)
		{
			for (int x=1;x<width-1;x++)
			{	
				u[x+y*width] = uAvg[x+y*width] - P[x+y*width]*Ex[x+y*width]/D[x+y*width];
				v[x+y*width] = vAvg[x+y*width] - P[x+y*width]*Ey[x+y*width]/D[x+y*width];
			}
		}
	}
}

void exportOutputVector(ostream &output, double *buf, int width, int height)
{
	for (int y=0;y<height;y++)
	{
		for (int x=0;x<width;x++)
		{
			ostringstream a;
			a << (double)buf[x+y*width];
			output << a.str();
			if (x != width-1)
				output << ",";
		}
		output << "\n";
	}
}
