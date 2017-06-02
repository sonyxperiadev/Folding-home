/* -*- mode: c; tab-width: 4; indent-tabs-mode: t; c-basic-offset: 4 -*- 
 *
 * $Id$
 *
 * Copyright (c) 2009-2014, Erik Lindahl & David van der Spoel
 * All rights reserved.
 * 
 * C++ API by Yutong Zhao <proteneer@gmail.com>
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

#include "XTCWriter.h"
#include <cmath>
#include <cstdlib>
#include <stdexcept>
#include <cstdio>
#include <climits>

#ifdef _WIN32
#include <cstdint>
#endif

using std::ostream;
using std::vector;

static int sizeofint(int size) {
    unsigned int num = 1;
    int num_of_bits = 0;
    while (size >= num && num_of_bits < 32) {
		num_of_bits++;
		num <<= 1;
    }
    return num_of_bits;
}

static int sizeofints(int num_of_ints, unsigned int sizes[]) {
    int i, num;
    unsigned int num_of_bytes, num_of_bits, bytes[32], bytecnt, tmp;
    num_of_bytes = 1;
    bytes[0] = 1;
    num_of_bits = 0;
    for (i=0; i < num_of_ints; i++) {	
		tmp = 0;
		for (bytecnt = 0; bytecnt < num_of_bytes; bytecnt++) {
			tmp = bytes[bytecnt] * sizes[i] + tmp;
			bytes[bytecnt] = tmp & 0xff;
			tmp >>= 8;
		}
		while (tmp != 0) {
			bytes[bytecnt++] = tmp & 0xff;
			tmp >>= 8;
		}
		num_of_bytes = bytecnt;
    }
    num = 1;
    num_of_bytes--;
    while (bytes[num_of_bytes] >= num) {
		num_of_bits++;
		num *= 2;
    }
    return num_of_bits + num_of_bytes * 8;
}

static int32_t xdr_swapbytes(int32_t x) {
	int32_t y,i;
	char *px=(char *)&x;
	char *py=(char *)&y;
	for(i=0;i<4;i++)
		py[i]=px[3-i];
	return y;
}

static int32_t xdr_htonl(int32_t x) {
	int s=0x1234;
	if( *((char *)&s)==(char)0x34) {
		/* smallendian,swap bytes */
		return xdr_swapbytes(x);
	} else {
		/* bigendian, do nothing */
		return x;
	}
}

static int xdr_putbytes(ostream &stream, char *addr, unsigned int len) {
	stream.write(addr,int(len));
	return 1;
}

static int xdr_putlong(ostream &stream, int32_t *lp) {
	int32_t mycopy = xdr_htonl (*lp);
	lp = &mycopy;
	stream.write(reinterpret_cast<char *>(lp),4);
	return 1;
}

struct XDRSTREAM {
	ostream  &stream;
    int *    buf1;     /**< Buffer for internal use                   */
    int      buf1size; /**< Current allocated length of buf1          */    
    int *    buf2;     /**< Buffer for internal use                   */
    int      buf2size; /**< Current allocated length of buf2          */ 
};

static int xdr_int(ostream &stream, int *ip) {
	int32_t i32;
	i32 = (int32_t) *ip;
	return xdr_putlong (stream, &i32);
}

static int xdr_float(ostream &stream, float *fp) {
	if (sizeof(float) == sizeof(int32_t))
		return (xdr_putlong(stream, (int32_t *)fp));
	else if (sizeof(float) == sizeof(int)) {
		int32_t tmp = *(int *)fp;
		return (xdr_putlong(stream, &tmp));
	}
}

static int xdrfile_write_int(int *ptr, int ndata, ostream &stream) {
	int i=0;
  	while(i<ndata && xdr_int(stream,ptr+i))
		i++;
	return i;
}

static int xdrfile_write_float(float *ptr, int ndata, ostream &stream) {
	int i=0;
	/* read write is encoded in the XDR struct */  
	while(i<ndata && xdr_float(stream,ptr+i))
		i++;
	return i;
}

static void encodebits(int buf[], int num_of_bits, int num) {
    unsigned int cnt, lastbyte;
    int lastbits;
    unsigned char * cbuf;
    cbuf = ((unsigned char *)buf) + 3 * sizeof(*buf);
    cnt = (unsigned int) buf[0];
    lastbits = buf[1];
    lastbyte =(unsigned int) buf[2];
    while (num_of_bits >= 8) {
		lastbyte = (lastbyte << 8) | ((num >> (num_of_bits -8)) /* & 0xff*/);
		cbuf[cnt++] = lastbyte >> lastbits;
		num_of_bits -= 8;
    }
    if (num_of_bits > 0) {
		lastbyte = (lastbyte << num_of_bits) | num;
		lastbits += num_of_bits;
		if (lastbits >= 8) 
        {
			lastbits -= 8;
			cbuf[cnt++] = lastbyte >> lastbits;
		}
    }
    buf[0] = cnt;
    buf[1] = lastbits;
    buf[2] = lastbyte;
    if (lastbits>0) {
		cbuf[cnt] = lastbyte << (8 - lastbits);
    }
}

/*
 * encodeints - encode a small set of small integers in compressed format
 *
 * this routine is used internally by xdr3dfcoord, to encode a set of
 * small integers to the buffer for writing to a file.
 * Multiplication with fixed (specified maximum) sizes is used to get
 * to one big, multibyte integer. Allthough the routine could be
 * modified to handle sizes bigger than 16777216, or more than just
 * a few integers, this is not done because the gain in compression
 * isn't worth the effort. Note that overflowing the multiplication
 * or the byte buffer (32 bytes) is unchecked and whould cause bad results.
 * THese things are checked in the calling routines, so make sure not
 * to remove those checks...
 */
 
static void encodeints(int buf[], int num_of_ints, int num_of_bits,
		   unsigned int sizes[], unsigned int nums[]) {

    int i;
    unsigned int bytes[32], num_of_bytes, bytecnt, tmp;

    tmp = nums[0];
    num_of_bytes = 0;
    do 
    {
		bytes[num_of_bytes++] = tmp & 0xff;
		tmp >>= 8;
    } while (tmp != 0);

    for (i = 1; i < num_of_ints; i++) 
    {
		if (nums[i] >= sizes[i])
        {
			fprintf(stderr,"major breakdown in encodeints - num %u doesn't "
					"match size %u\n", nums[i], sizes[i]);
			abort();
		}
		/* use one step multiply */    
		tmp = nums[i];
		for (bytecnt = 0; bytecnt < num_of_bytes; bytecnt++) 
        {
			tmp = bytes[bytecnt] * sizes[i] + tmp;
			bytes[bytecnt] = tmp & 0xff;
			tmp >>= 8;
		}
		while (tmp != 0)
        {
			bytes[bytecnt++] = tmp & 0xff;
			tmp >>= 8;
		}
		num_of_bytes = bytecnt;
    }
    if (num_of_bits >= num_of_bytes * 8) 
    {
		for (i = 0; i < num_of_bytes; i++) 
        {
			encodebits(buf, 8, bytes[i]);
		}
		encodebits(buf, num_of_bits - num_of_bytes * 8, 0);
    } 
    else
    {
		for (i = 0; i < num_of_bytes-1; i++)
        {
			encodebits(buf, 8, bytes[i]);
		}
		encodebits(buf, num_of_bits- (num_of_bytes -1) * 8, bytes[i]);
    }
}

#define BYTES_PER_XDR_UNIT 4 
static char xdr_zero[BYTES_PER_XDR_UNIT] = {0, 0, 0, 0};

static const int magicints[] = 
{
    0, 0, 0, 0, 0, 0, 0, 0, 0, 8, 10, 12, 16, 20, 25, 32, 40, 50, 64,
    80, 101, 128, 161, 203, 256, 322, 406, 512, 645, 812, 1024, 1290,
    1625, 2048, 2580, 3250, 4096, 5060, 6501, 8192, 10321, 13003, 
    16384, 20642, 26007, 32768, 41285, 52015, 65536,82570, 104031, 
    131072, 165140, 208063, 262144, 330280, 416127, 524287, 660561, 
    832255, 1048576, 1321122, 1664510, 2097152, 2642245, 3329021, 
    4194304, 5284491, 6658042, 8388607, 10568983, 13316085, 16777216 
};

#define FIRSTIDX 9
/* note that magicints[FIRSTIDX-1] == 0 */
#define LASTIDX (sizeof(magicints) / sizeof(*magicints))

static int xdr_opaque(ostream &stream, char *cp, unsigned int cnt) {
	unsigned int rndup;
	static char crud[BYTES_PER_XDR_UNIT];

	/*
	 * if no data we are done
	 */
	if (cnt == 0)
		return 1;

	rndup = cnt % BYTES_PER_XDR_UNIT;
	if (rndup > 0)
		rndup = BYTES_PER_XDR_UNIT - rndup;
	if (!xdr_putbytes (stream, cp, cnt))
		return 0;
	if (rndup == 0)
		return 1;
	return xdr_putbytes (stream, xdr_zero, rndup);

}

static int xdrfile_write_opaque(char *ptr, int cnt, ostream &stream) {
	if(xdr_opaque(stream,ptr,cnt))
		return cnt;
	else
		return 0;
}

static int xdrfile_compress_coord_float(float   *ptr,
							 int      size,
							 float    precision,
							 ostream &stream) {
	int minint[3], maxint[3], mindiff, *lip, diff;
	int lint1, lint2, lint3, oldlint1, oldlint2, oldlint3, smallidx;
	int minidx, maxidx;
	unsigned sizeint[3], sizesmall[3], bitsizeint[3], size3, *luip;
	int k, *buf1, *buf2;
	int smallnum, smaller, larger, i, j, is_small, is_smaller, run, prevrun;
	float *lfp, lf;
	int tmp, tmpsum, *thiscoord,  prevcoord[3];
	unsigned int tmpcoord[30];
	int errval=1;
	unsigned int bitsize;
  
	size3=3*size;
    
    bitsizeint[0] = 0;
    bitsizeint[1] = 0;
    bitsizeint[2] = 0;

	vector<int> xbuf1(size3,0);
	vector<int> xbuf2(size3*1.2,0);

/*
	if(size3>xfp->buf1size) {
		if((xfp->buf1=(int *)malloc(sizeof(int)*size3))==NULL) {
			fprintf(stderr,"Cannot allocate memory for compressing coordinates.\n");
			return -1;
		}
		xfp->buf1size=size3;
		xfp->buf2size=size3*1.2;
		if((xfp->buf2=(int *)malloc(sizeof(int)*xfp->buf2size))==NULL) {
			fprintf(stderr,"Cannot allocate memory for compressing coordinates.\n");
			return -1;
		}	
	}
	*/
	if(xdrfile_write_int(&size,1,stream)==0)
		return -1; /* return if we could not write size */
	/* Dont bother with compression for three atoms or less */
	if(size<=9) 
    {
		return xdrfile_write_float(ptr,size3,stream)/3;
		/* return number of coords, not floats */
	}
	/* Compression-time if we got here. Write precision first */
	if (precision <= 0)
		precision = 1000;
	xdrfile_write_float(&precision,1,stream);
	/* avoid repeated pointer dereferencing. */
	/*
	buf1=xfp->buf1; 
	buf2=xfp->buf2;
	*/
	buf1=&xbuf1[0];
	buf2=&xbuf2[0];
	/* buf2[0-2] are special and do not contain actual data */
	buf2[0] = buf2[1] = buf2[2] = 0;
	minint[0] = minint[1] = minint[2] = INT_MAX;
	maxint[0] = maxint[1] = maxint[2] = INT_MIN;
	prevrun = -1;
	lfp = ptr;
	lip = buf1;
	mindiff = INT_MAX;
	oldlint1 = oldlint2 = oldlint3 = 0;
	while(lfp < ptr + size3 )
    {
		/* find nearest integer */
		if (*lfp >= 0.0)
			lf = *lfp * precision + 0.5;
		else
			lf = *lfp * precision - 0.5;
		if (fabs(lf) > INT_MAX-2) 
        {
			/* scaling would cause overflow */
			fprintf(stderr,"Internal overflow compressing coordinates.\n");
			errval=0;
		}
		lint1 = lf;
		if (lint1 < minint[0]) minint[0] = lint1;
		if (lint1 > maxint[0]) maxint[0] = lint1;
		*lip++ = lint1;
		lfp++;
		if (*lfp >= 0.0)
			lf = *lfp * precision + 0.5;
		else
			lf = *lfp * precision - 0.5;
		if (fabs(lf) > INT_MAX-2)
        {
			/* scaling would cause overflow */
			fprintf(stderr,"Internal overflow compressing coordinates.\n");
			errval=0;
		}
		lint2 = lf;
		if (lint2 < minint[1]) minint[1] = lint2;
		if (lint2 > maxint[1]) maxint[1] = lint2;
		*lip++ = lint2;
		lfp++;
		if (*lfp >= 0.0)
			lf = *lfp * precision + 0.5;
		else
			lf = *lfp * precision - 0.5;
		if (fabs(lf) > INT_MAX-2) 
        {
			errval=0;      
		}
		lint3 = lf;
		if (lint3 < minint[2]) minint[2] = lint3;
		if (lint3 > maxint[2]) maxint[2] = lint3;
		*lip++ = lint3;
		lfp++;
		diff = abs(oldlint1-lint1)+abs(oldlint2-lint2)+abs(oldlint3-lint3);
		if (diff < mindiff && lfp > ptr + 3)
			mindiff = diff;
		oldlint1 = lint1;
		oldlint2 = lint2;
		oldlint3 = lint3;
	}  
	xdrfile_write_int(minint,3,stream);
	xdrfile_write_int(maxint,3,stream);
  
	if ((float)maxint[0] - (float)minint[0] >= INT_MAX-2 ||
		(float)maxint[1] - (float)minint[1] >= INT_MAX-2 ||
		(float)maxint[2] - (float)minint[2] >= INT_MAX-2) {
		/* turning value in unsigned by subtracting minint
		 * would cause overflow
		 */
		fprintf(stderr,"Internal overflow compressing coordinates.\n");
		errval=0;
	}
	sizeint[0] = maxint[0] - minint[0]+1;
	sizeint[1] = maxint[1] - minint[1]+1;
	sizeint[2] = maxint[2] - minint[2]+1;
  
	/* check if one of the sizes is to big to be multiplied */
	if ((sizeint[0] | sizeint[1] | sizeint[2] ) > 0xffffff)
    {
		bitsizeint[0] = sizeofint(sizeint[0]);
		bitsizeint[1] = sizeofint(sizeint[1]);
		bitsizeint[2] = sizeofint(sizeint[2]);
		bitsize = 0; /* flag the use of large sizes */
	}
    else
    {
		bitsize = sizeofints(3, sizeint);
	}
	lip = buf1;
	luip = (unsigned int *) buf1;
	smallidx = FIRSTIDX;
	while (smallidx < LASTIDX && magicints[smallidx] < mindiff)
    {
		smallidx++;
	}
	xdrfile_write_int(&smallidx,1,stream);
	tmp=smallidx+8;
	maxidx = (LASTIDX<tmp) ? LASTIDX : tmp;
	minidx = maxidx - 8; /* often this equal smallidx */
	tmp=smallidx-1;
	tmp= (FIRSTIDX>tmp) ? FIRSTIDX : tmp;
	smaller = magicints[tmp] / 2;
	smallnum = magicints[smallidx] / 2;
	sizesmall[0] = sizesmall[1] = sizesmall[2] = magicints[smallidx];
	larger = magicints[maxidx] / 2;
	i = 0;
	while (i < size) 
    {
		is_small = 0;
		thiscoord = (int *)(luip) + i * 3;
		if (smallidx < maxidx && i >= 1 &&
			abs(thiscoord[0] - prevcoord[0]) < larger &&
			abs(thiscoord[1] - prevcoord[1]) < larger &&
			abs(thiscoord[2] - prevcoord[2]) < larger) {
			is_smaller = 1;
		} 
        else if (smallidx > minidx) 
        {
			is_smaller = -1;
		}
        else
        {
			is_smaller = 0;
		}
		if (i + 1 < size) 
        {
			if (abs(thiscoord[0] - thiscoord[3]) < smallnum &&
				abs(thiscoord[1] - thiscoord[4]) < smallnum &&
				abs(thiscoord[2] - thiscoord[5]) < smallnum) 
            {
				/* interchange first with second atom for better
				 * compression of water molecules
				 */
				tmp = thiscoord[0]; thiscoord[0] = thiscoord[3];
				thiscoord[3] = tmp;
				tmp = thiscoord[1]; thiscoord[1] = thiscoord[4];
				thiscoord[4] = tmp;
				tmp = thiscoord[2]; thiscoord[2] = thiscoord[5];
				thiscoord[5] = tmp;
				is_small = 1;
			} 
		}
		tmpcoord[0] = thiscoord[0] - minint[0];
		tmpcoord[1] = thiscoord[1] - minint[1];
		tmpcoord[2] = thiscoord[2] - minint[2];
		if (bitsize == 0) 
        {
			encodebits(buf2, bitsizeint[0], tmpcoord[0]);
			encodebits(buf2, bitsizeint[1], tmpcoord[1]);
			encodebits(buf2, bitsizeint[2], tmpcoord[2]);
		} 
        else
        {
			encodeints(buf2, 3, bitsize, sizeint, tmpcoord);
		}
		prevcoord[0] = thiscoord[0];
		prevcoord[1] = thiscoord[1];
		prevcoord[2] = thiscoord[2];
		thiscoord = thiscoord + 3;
		i++;

		run = 0;
		if (is_small == 0 && is_smaller == -1)
			is_smaller = 0;
		while (is_small && run < 8*3)
        {
			tmpsum=0;
			for(j=0;j<3;j++) 
            {
				tmp=thiscoord[j] - prevcoord[j];
				tmpsum+=tmp*tmp;
			}
			if (is_smaller == -1 && tmpsum >= smaller * smaller)
            {
				is_smaller = 0;
			}
      
			tmpcoord[run++] = thiscoord[0] - prevcoord[0] + smallnum;
			tmpcoord[run++] = thiscoord[1] - prevcoord[1] + smallnum;
			tmpcoord[run++] = thiscoord[2] - prevcoord[2] + smallnum;
      
			prevcoord[0] = thiscoord[0];
			prevcoord[1] = thiscoord[1];
			prevcoord[2] = thiscoord[2];
      
			i++;
			thiscoord = thiscoord + 3;
			is_small = 0;
			if (i < size &&
				abs(thiscoord[0] - prevcoord[0]) < smallnum &&
				abs(thiscoord[1] - prevcoord[1]) < smallnum &&
				abs(thiscoord[2] - prevcoord[2]) < smallnum)
            {
				is_small = 1;
			}
		}
		if (run != prevrun || is_smaller != 0) 
        {
			prevrun = run;
			encodebits(buf2, 1, 1); /* flag the change in run-length */
			encodebits(buf2, 5, run+is_smaller+1);
		} 
        else 
        {
			encodebits(buf2, 1, 0); /* flag the fact that runlength did not change */
		}
		for (k=0; k < run; k+=3) 
        {
			encodeints(buf2, 3, smallidx, sizesmall, &tmpcoord[k]);	
		}
		if (is_smaller != 0) 
        {
			smallidx += is_smaller;
			if (is_smaller < 0) 
            {
				smallnum = smaller;
				smaller = magicints[smallidx-1] / 2;
			} 
            else 
            {
				smaller = smallnum;
				smallnum = magicints[smallidx] / 2;
			}
			sizesmall[0] = sizesmall[1] = sizesmall[2] = magicints[smallidx];
		}   
	}
	if (buf2[1] != 0) buf2[0]++;
	xdrfile_write_int(buf2,1,stream); /* buf2[0] holds the length in bytes */
	tmp=xdrfile_write_opaque((char *)&(buf2[3]),(unsigned int)buf2[0],stream);
	if(tmp==(unsigned int)buf2[0])
		return size;
	else
		return -1;
}

XTCWriter::XTCWriter(ostream &output, float precision) :
	output_(output), 
	precision_(precision) {

};

#define MAGIC 1995

void XTCWriter::append(int step, float time, 
		        	   const vector<vector<float> > &box,
				       const vector<vector<float> > &positions) {
	// write headers
	int result,magic,n=1;
	
	if(box.size() != 3) 
		throw(std::runtime_error("Bad box size!"));
	if(box[0].size() != 3)
		throw(std::runtime_error("Bad box size!"));
	if(positions.size() == 0)
		throw(std::runtime_error("Empty positions"));
    for(int i=0;i<positions.size(); i++)
    	if(positions[i].size() != 3)
    		throw(std::runtime_error("One of the positions has size != 3"));
 
 	int natoms = positions.size();

	float a_box[3][3];
    a_box[0][0] = box[0][0]; a_box[0][1] = box[0][1]; a_box[0][2] = box[0][2];
    a_box[1][0] = box[1][0]; a_box[1][1] = box[1][1]; a_box[1][2] = box[1][2];
    a_box[2][0] = box[2][0]; a_box[2][1] = box[2][1]; a_box[2][2] = box[2][2];

	magic  = MAGIC;
	if ((result = xdrfile_write_int(&magic,n,output_)) != n)
		throw std::runtime_error("exdrINT");
	if (magic != MAGIC)
		throw std::runtime_error("exdrMAGIC");
	if (xdrfile_write_int(&natoms,n,output_) != n)
		throw std::runtime_error("exdrINT");
	if (xdrfile_write_int(&step,n,output_) != n)
		throw std::runtime_error("exdrINT");
	if (xdrfile_write_float(&time,n,output_) != n)
		throw std::runtime_error("exdrFLOAT");
	// write coords
	if (3*3 != xdrfile_write_float(a_box[0],3*3,output_))
		throw std::runtime_error("exdrFLOAT");
	// convert positions to expected xtc format
	typedef float rvec[3];
    rvec *coords = (rvec *) malloc(sizeof(rvec)*natoms);
    for(int i=0;i<positions.size(); i++) {
        for(int j=0;j<3;j++) {
            coords[i][j] = positions[i][j];
        }
    }
	if (xdrfile_compress_coord_float(coords[0],natoms,precision_,output_) != natoms)
		throw std::runtime_error("exdr3DX");
	free(coords);
}
