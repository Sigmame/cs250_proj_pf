#include "hsOptflow.h"
#include <assert.h>

#include <iostream>
#include <fstream>
#include <sstream>
using namespace std;

float gaussianKernel5x5[] = \
  { 2.0,  4.0,  5.0,  4.0, 2.0, \
    4.0,  9.0, 12.0,  9.0, 4.0, \
    5.0, 12.0, 15.0, 12.0, 5.0, \
    4.0,  9.0, 12.0,  9.0, 4.0, \
    2.0,  4.0,  5.0,  4.0, 2.0 };

float* filterKernel = gaussianKernel5x5;
float  filterScale  = 115.0;

float floatKernel[WINDOW_SIZE];
int32_t intKernel[WINDOW_SIZE];

uint32_t imageWidth, imageHeight, imageBufferSize = 0;
uint8_t *inputImagesBuffer = NULL;
int32_t *outputImagesBuffer = NULL;

void allocateImageBuffers(int width, int height)
{
  imageBufferSize = width*height;

  inputImagesBuffer = (uint8_t*) malloc(sizeof(uint8_t)*imageBufferSize*2); //two images
  assert(inputImagesBuffer);
  outputImagesBuffer = (int32_t*) malloc(sizeof(int32_t)*imageBufferSize*2); //two images (later u,v)
  assert(outputImagesBuffer);

  memset(inputImagesBuffer, 0, sizeof(uint8_t)*imageBufferSize*2);
  memset(outputImagesBuffer, 0, sizeof(int32_t)*imageBufferSize*2);
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

void generateFilterKernel(float *floatInputKernel, float kernelScaleFactor, float *floatOutputKernel, int32_t *intOutputKernel)
{
  float ftmp;
  for (int i=0;i<WINDOW_SIZE;i++)
  {
    ftmp = floatInputKernel[i] / kernelScaleFactor;
    intOutputKernel[i] = (int32_t) (ftmp * COEFF_SCALE_FACTOR);
    floatOutputKernel[i] = ((float)intOutputKernel[i]) / COEFF_SCALE_FACTOR;
  }
}

void convolutionFilter(int32_t *kernel, int radius, uint8_t *in, int32_t *out, int width, int height)
{
  // copy top and bottom lines from input to output unchanged
  for (int y=0; y<radius; y++)
    for (int x=0; x<width; x++)
    {
      out[x+y*width] = (int32_t)(in[x+y*width] * COEFF_SCALE_FACTOR);
      int offset = (height-1-y)*width;
      out[x+offset] = (int32_t)(in[x+offset] * COEFF_SCALE_FACTOR);
    }

  for (int y=radius; y<height-radius; y++)
  {
    // copy first and last pixels in each line from input to output unchanged
    int offset=y*width;
    for (int i=0;i<radius;i++) {
      out[offset+i] = (int32_t)(in[offset+i] * COEFF_SCALE_FACTOR);
      out[offset+width-(i+1)] = (int32_t)(in[offset+width-(i+1)] * COEFF_SCALE_FACTOR);
    }

    for (int x=radius; x<width-radius; x++)
    {
      int32_t outputInt = 0;
      int pos = 0;
      int diameter = 2*radius+1;
      for (int yy=0;yy<diameter;yy++)
        for (int xx=0;xx<diameter;xx++)
	      outputInt += in[(x+xx-radius) + ((y+yy-radius)*width)] * kernel[pos++];

      int32_t output;
      if (outputInt+0.5 > 255.0)
        output = (int32_t) (255 * COEFF_SCALE_FACTOR);
      else if (outputInt < 0)
        output = (int32_t) 0;
      else
        output = (int32_t)(outputInt); // no 0.5 cutoff

      out[x+y*width] = output;
    }
  }
}

void exportOutputVector(ostream &output, int32_t *buf, int width, int height)
{
	for (int y=0;y<height;y++)
	{
		for (int x=0;x<width;x++)
		{
			ostringstream a;
			a << ((float)buf[x+y*width]) / COEFF_SCALE_FACTOR;
			output << a.str();
			if (x != width-1)
				output << ",";
		}
		output << "\n";
	}
}
