/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

#define CPU_FPU (1)
#define CPU_TSC (1<<4)
#define CPU_CMOV (1<<15)
#define CPU_MMX (1<<23)
#define CPU_SSE (1<<25)
#define CPU_SSE2 (1<<26)
#define CPU_AMD ((__int64)1<<59)
#define CPU_MMXEX ((__int64)1<<60)
#define CPU_3DNOWEX ((__int64)1<<62)
#define CPU_3DNOW ((__int64)1<<63)

#define CPU_AMD_L (1<<(59-32))
#define CPU_3DNOWEX_L (1<<(62-32))
#define CPU_3DNOW_L (1<<(63-32))

#define CPU_L2SIZE_SHIFT 32
#define CPU_L2SIZE_UNIT (32*1024)
#define CPU_L2SIZE 1023
#define CPU_L1DATASIZE_SHIFT 42
#define CPU_L1DATASIZE_UNIT (8*1024)
#define CPU_L1DATASIZE 255
#define CPU_FAMILY_SHIFT 50
#define CPU_FAMILY 15

#define OUT_OF_MEMORY \
	env->ThrowNew(env->FindClass("java/lang/OutOfMemoryError"),\
		"Out of memory in ArraysNative, C++ or Assembler code");\


#define SINGLE_PREFIX(TYPE,TYPEARRAY) \
(JNIEnv *env, jclass, jlong CpuInfo, TYPEARRAY A, jint BeginIndex, jint EndIndex, TYPE V) {\
	jint Len= EndIndex-BeginIndex;\
SINGLE_PREFIX_NO_ARGUMENTS(TYPE)\

#define SINGLE_PREFIX_NO_ARGUMENTS(TYPE) \
	try {\
		TYPE *a= (TYPE*)env->GetPrimitiveArrayCritical((jarray)A, NULL); if (a==NULL) {OUT_OF_MEMORY; goto _FA;} {\

#define SINGLE_POSTFIX \
		} env->ReleasePrimitiveArrayCritical((jarray)A, a, 0); _FA: ;\
PAIRBUFFER_POSTFIX\

#define PAIR_PREFIX(TYPE,TYPEARRAY) \
(JNIEnv *env, jclass, jlong CpuInfo, TYPEARRAY A, jint Aofs, TYPEARRAY B, jint Bofs, jint Len) {\
PAIR_PREFIX_NO_ARGUMENTS(TYPE)\

#define PAIR_PREFIX_NO_ARGUMENTS(TYPE) \
SINGLE_PREFIX_NO_ARGUMENTS(TYPE)\
		TYPE *b= (TYPE*)env->GetPrimitiveArrayCritical((jarray)B, NULL); if (b==NULL) {OUT_OF_MEMORY; goto _FB;} {\

#define PAIR_POSTFIX \
		} env->ReleasePrimitiveArrayCritical((jarray)B, b, JNI_ABORT); _FB: ;\
SINGLE_POSTFIX\

#define PAIRBUFFER_PREFIX(TYPE,TYPEOBJECT) \
(JNIEnv *env, jclass, jlong CpuInfo, TYPEOBJECT A, jint Aofs, TYPEOBJECT B, jint Bofs, jint Len) {\
	try {\
		TYPE *a= (TYPE*)env->GetDirectBufferAddress(A); if (a==NULL) {OUT_OF_MEMORY; return;}\
		TYPE *b= (TYPE*)env->GetDirectBufferAddress(B); if (b==NULL) {OUT_OF_MEMORY; return;}\

#define PAIRBUFFER_POSTFIX \
	} catch (...) {\
		env->ThrowNew(env->FindClass("java/lang/InternalError"),\
			"Unexpected exception in ArraysNative, C++ or Assembler code");\
	}\
}

#define LOOP_PREFIX_ALIGNED(UNLOOPING) \
	jint len= Len;\
	int disp= (int)pa&31;\
	jint lenStart= 0;\
	if (disp!=0 && (disp&(sizeof(*pa)-1))==0) {\
		lenStart= (32-disp)/sizeof(*pa);\
		if (len<lenStart) lenStart= 0;\
		else              len-= lenStart;\
	}\
	jint lenEnd= len&(UNLOOPING/sizeof(*pa)-1);\
	len/= (UNLOOPING/sizeof(*pa));\
	if (len>0) {len--; lenEnd+= UNLOOPING/sizeof(*pa);}\

#define LOOP_PREFIX(UNLOOPING) \
	jint len= Len;\
	jint lenEnd= len&(UNLOOPING/sizeof(*pa)-1);\
	len/= (UNLOOPING/sizeof(*pa));\

#define MINBODY_LOOP(TYPE) \
	TYPE *pa= (TYPE*)a+Aofs, *pb= (TYPE*)b+Bofs;\
	LOOP_PREFIX(32*sizeof(TYPE))\
	for (; len>0; len--,pa+=32,pb+=32) {\
		if (pa[0]>pb[0]) pa[0]= pb[0];     if (pa[1]>pb[1]) pa[1]= pb[1];\
		if (pa[2]>pb[2]) pa[2]= pb[2];     if (pa[3]>pb[3]) pa[3]= pb[3];\
		if (pa[4]>pb[4]) pa[4]= pb[4];     if (pa[5]>pb[5]) pa[5]= pb[5];\
		if (pa[6]>pb[6]) pa[6]= pb[6];     if (pa[7]>pb[7]) pa[7]= pb[7];\
		if (pa[8]>pb[8]) pa[8]= pb[8];     if (pa[9]>pb[9]) pa[9]= pb[9];\
		if (pa[10]>pb[10]) pa[10]= pb[10]; if (pa[11]>pb[11]) pa[11]= pb[11];\
		if (pa[12]>pb[12]) pa[12]= pb[12]; if (pa[13]>pb[13]) pa[13]= pb[13];\
		if (pa[14]>pb[14]) pa[14]= pb[14]; if (pa[15]>pb[15]) pa[15]= pb[15];\
		if (pa[16]>pb[16]) pa[16]= pb[16]; if (pa[17]>pb[17]) pa[17]= pb[17];\
		if (pa[18]>pb[18]) pa[18]= pb[18]; if (pa[19]>pb[19]) pa[19]= pb[19];\
		if (pa[20]>pb[20]) pa[20]= pb[20]; if (pa[21]>pb[21]) pa[21]= pb[21];\
		if (pa[22]>pb[22]) pa[22]= pb[22]; if (pa[23]>pb[23]) pa[23]= pb[23];\
		if (pa[24]>pb[24]) pa[24]= pb[24]; if (pa[25]>pb[25]) pa[25]= pb[25];\
		if (pa[26]>pb[26]) pa[26]= pb[26]; if (pa[27]>pb[27]) pa[27]= pb[27];\
		if (pa[28]>pb[28]) pa[28]= pb[28]; if (pa[29]>pb[29]) pa[29]= pb[29];\
		if (pa[30]>pb[30]) pa[30]= pb[30]; if (pa[31]>pb[31]) pa[31]= pb[31];\
	}\
	for (; lenEnd>0; lenEnd--,pa++,pb++) if (*pa>*pb) *pa= *pb;\

#define MAXBODY_LOOP(TYPE) \
	TYPE *pa= (TYPE*)a+Aofs, *pb= (TYPE*)b+Bofs;\
	LOOP_PREFIX(32*sizeof(TYPE))\
	for (; len>0; len--,pa+=32,pb+=32) {\
		if (pa[0]<pb[0]) pa[0]= pb[0];     if (pa[1]<pb[1]) pa[1]= pb[1];\
		if (pa[2]<pb[2]) pa[2]= pb[2];     if (pa[3]<pb[3]) pa[3]= pb[3];\
		if (pa[4]<pb[4]) pa[4]= pb[4];     if (pa[5]<pb[5]) pa[5]= pb[5];\
		if (pa[6]<pb[6]) pa[6]= pb[6];     if (pa[7]<pb[7]) pa[7]= pb[7];\
		if (pa[8]<pb[8]) pa[8]= pb[8];     if (pa[9]<pb[9]) pa[9]= pb[9];\
		if (pa[10]<pb[10]) pa[10]= pb[10]; if (pa[11]<pb[11]) pa[11]= pb[11];\
		if (pa[12]<pb[12]) pa[12]= pb[12]; if (pa[13]<pb[13]) pa[13]= pb[13];\
		if (pa[14]<pb[14]) pa[14]= pb[14]; if (pa[15]<pb[15]) pa[15]= pb[15];\
		if (pa[16]<pb[16]) pa[16]= pb[16]; if (pa[17]<pb[17]) pa[17]= pb[17];\
		if (pa[18]<pb[18]) pa[18]= pb[18]; if (pa[19]<pb[19]) pa[19]= pb[19];\
		if (pa[20]<pb[20]) pa[20]= pb[20]; if (pa[21]<pb[21]) pa[21]= pb[21];\
		if (pa[22]<pb[22]) pa[22]= pb[22]; if (pa[23]<pb[23]) pa[23]= pb[23];\
		if (pa[24]<pb[24]) pa[24]= pb[24]; if (pa[25]<pb[25]) pa[25]= pb[25];\
		if (pa[26]<pb[26]) pa[26]= pb[26]; if (pa[27]<pb[27]) pa[27]= pb[27];\
		if (pa[28]<pb[28]) pa[28]= pb[28]; if (pa[29]<pb[29]) pa[29]= pb[29];\
		if (pa[30]<pb[30]) pa[30]= pb[30]; if (pa[31]<pb[31]) pa[31]= pb[31];\
	}\
	for (; lenEnd>0; lenEnd--,pa++,pb++) if (*pa<*pb) *pa= *pb;\

