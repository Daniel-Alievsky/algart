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

#include <jni.h>
#include "net_algart_array_ArraysNative.h"
#include "ArraysMacro.h"
#include "ArraysFunctions.h"

#include <string.h> // memmove()

#define MMXASM_SUPPORTED
#define SSEASM_SUPPORTED
#ifdef _MSC_VER
	#if _MSC_VER<1200
		#undef SSEASM_SUPPORTED
	#endif
#endif //_MSC_VER

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    detectImplementedFlags
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_detectImplementedFlags
(JNIEnv *env, jclass clazz) {
	env->SetStaticBooleanField(clazz,
		env->GetStaticFieldID(clazz,"copyBytesImplemented","Z"),
		JNI_TRUE);
	env->SetStaticBooleanField(clazz,
		env->GetStaticFieldID(clazz,"fillImplemented","Z"),
		JNI_TRUE);
	env->SetStaticBooleanField(clazz,
		env->GetStaticFieldID(clazz,"minmaxImplemented","Z"),
		JNI_TRUE);
	env->SetStaticBooleanField(clazz,
		env->GetStaticFieldID(clazz,"minmaxuImplemented","Z"),
		JNI_TRUE);
}

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    getCpuInfoInternal
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_algart_array_ArraysNative_getCpuInfoInternal
(JNIEnv *, jclass) {
	return _cpuInfo();
}

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    ptrOfs
 * Signature: (Ljava/lang/Object;)I
 */
JNIEXPORT jint JNICALL Java_net_algart_array_ArraysNative_ptrOfs
(JNIEnv *env, jclass, jobject A) {
	try {\
		void *a= env->GetPrimitiveArrayCritical((jarray)A, NULL);
		if (a==NULL) return 0;
		env->ReleasePrimitiveArrayCritical((jarray)A, a, 0);
		return (jint)a;
	} catch (...) {\
		return 0;
	}\
}

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    copyBytes
 * Signature: (JLjava/lang/Object;ILjava/lang/Object;II)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_copyBytes
PAIR_PREFIX(jbyte,jobject)
#ifdef SSEASM_SUPPORTED
if (CpuInfo & CPU_SSE) {
	int cacheSize= (int)((CpuInfo>>CPU_L2SIZE_SHIFT)&CPU_L2SIZE)*CPU_L2SIZE_UNIT;
	if (cacheSize<65536) cacheSize= 65536;
	int cacheSizeIn64ByteBlocks= cacheSize/64;
	int nonCachedCopyMinLen= cacheSizeIn64ByteBlocks/2;
	jbyte *pb= a+Aofs, *pa= b+Bofs;\
	LOOP_PREFIX_ALIGNED(64)
	__asm {
		push esi;
		push edi;
		mov esi,pb;
		mov edi,pa;
		mov ecx,lenStart;
		cld;
		rep movsb;
		mov ecx,len;
		jecxz _EndLoopAligned_ntps;
		test esi,0xF;
		jnz _Loop;
		test edi,0xF;
		jnz _Loop;
		cmp ecx,nonCachedCopyMinLen;
		jb _LoopAligned;
	_LoopAligned_ntps:
		prefetcht0 [esi+64];
		movaps xmm0,[esi];
		movaps xmm1,[esi+16];
		prefetcht0 [esi+96];
		movaps xmm2,[esi+32];
		movaps xmm3,[esi+48];
		movntps [edi],xmm0;
		movntps [edi+16],xmm1;
		movntps [edi+32],xmm2;
		movntps [edi+48],xmm3;
		add esi,64;
		add edi,64;
		dec ecx;
		jnz _LoopAligned_ntps;
	_EndLoopAligned_ntps:
		jmp _EndLoop;
	_LoopAligned:
		prefetcht0 [esi+64];
		movaps xmm0,[esi];
		movaps xmm1,[esi+16];
		prefetcht0 [esi+96];
		movaps xmm2,[esi+32];
		movaps xmm3,[esi+48];
		movaps [edi],xmm0;
		movaps [edi+16],xmm1;
		movaps [edi+32],xmm2;
		movaps [edi+48],xmm3;
		add esi,64;
		add edi,64;
		dec ecx;
		jnz _LoopAligned;
		jmp _EndLoop;
	_Loop:
		prefetcht0 [esi+64];
		movups xmm0,[esi];
		movups xmm1,[esi+16];
		prefetcht0 [esi+96];
		movups xmm2,[esi+32];
		movups xmm3,[esi+48];
		movups [edi],xmm0;
		movups [edi+16],xmm1;
		movups [edi+32],xmm2;
		movups [edi+48],xmm3;
		add esi,64;
		add edi,64;
		dec ecx;
		jnz _Loop;
	_EndLoop:
		mov ecx,lenEnd;
		cld;
		rep movsb;
		pop edi;
		pop esi;
	}
} else 
#endif
{
	memmove(b+Bofs,a+Aofs,Len);
}
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    fill
 * Signature: (J[CIIC)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_fill__J_3CIIC
SINGLE_PREFIX(jchar,jcharArray)
#define TYPE jchar
#include "Arrays_fill.h"
SINGLE_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    fill
 * Signature: (J[BIIB)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_fill__J_3BIIB
SINGLE_PREFIX(jbyte,jbyteArray)
#define TYPE jbyte
#include "Arrays_fill.h"
SINGLE_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    fill
 * Signature: (J[SIIS)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_fill__J_3SIIS
SINGLE_PREFIX(jshort,jshortArray)
#define TYPE jshort
#include "Arrays_fill.h"
SINGLE_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    fill
 * Signature: (J[IIII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_fill__J_3IIII
SINGLE_PREFIX(jint,jintArray)
#define TYPE jint
#include "Arrays_fill.h"
SINGLE_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    fill
 * Signature: (J[JIIJ)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_fill__J_3JIIJ
SINGLE_PREFIX(jlong,jlongArray)
#define TYPE jlong
#include "Arrays_fill.h"
SINGLE_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    fill
 * Signature: (J[FIIF)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_fill__J_3FIIF
SINGLE_PREFIX(jfloat,jfloatArray)
#define TYPE jfloat
#include "Arrays_fill.h"
SINGLE_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    fill
 * Signature: (J[DIID)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_fill__J_3DIID
SINGLE_PREFIX(jdouble,jdoubleArray)
#define TYPE jdouble
#include "Arrays_fill.h"
SINGLE_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    fill
 * Signature: (J[Ljava/lang/Object;IILjava/lang/Object;)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_fill__J_3Ljava_lang_Object_2IILjava_lang_Object_2
SINGLE_PREFIX(jobject,jobjectArray)
#define TYPE jobject
#include "Arrays_fill.h"
SINGLE_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    min
 * Signature: (J[BI[BII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_min__J_3BI_3BII
PAIR_PREFIX(jbyte,jbyteArray)
#define TYPE jbyte
#define C_LOOP MINBODY_LOOP(jbyte)
#define CMP >
#define BIT_MASK_SSE 0x8080808080808080
#define MINMAX_MMXEX pminub
#define PCMPGT pcmpgtb
#define MM0_FOR_MIN mm0
#define MM1_FOR_MIN mm1
#include "Arrays_minmax_int.h"
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    max
 * Signature: (J[BI[BII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_max__J_3BI_3BII
PAIR_PREFIX(jbyte,jbyteArray) 
#define TYPE jbyte
#define C_LOOP MAXBODY_LOOP(jbyte)
#define CMP <
#define BIT_MASK_SSE 0x8080808080808080
#define MINMAX_MMXEX pmaxub
#define PCMPGT pcmpgtb
#define MM0_FOR_MIN mm1
#define MM1_FOR_MIN mm0
#include "Arrays_minmax_int.h"
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    min
 * Signature: (JLjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;II)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_min__JLjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2II
PAIRBUFFER_PREFIX(jbyte,jobject)
#define TYPE jbyte
#define C_LOOP MINBODY_LOOP(jbyte)
#define CMP >
#define BIT_MASK_SSE 0x8080808080808080
#define MINMAX_MMXEX pminub
#define PCMPGT pcmpgtb
#define MM0_FOR_MIN mm0
#define MM1_FOR_MIN mm1
#include "Arrays_minmax_int.h"
PAIRBUFFER_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    max
 * Signature: (JLjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;II)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_max__JLjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2II
PAIRBUFFER_PREFIX(jbyte,jobject)
#define TYPE jbyte
#define C_LOOP MAXBODY_LOOP(jbyte)
#define CMP <
#define BIT_MASK_SSE 0x8080808080808080
#define MINMAX_MMXEX pmaxub
#define PCMPGT pcmpgtb
#define MM0_FOR_MIN mm1
#define MM1_FOR_MIN mm0
#include "Arrays_minmax_int.h"
PAIRBUFFER_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    min
 * Signature: (J[SI[SII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_min__J_3SI_3SII
PAIR_PREFIX(jshort,jshortArray)
#define TYPE jshort
#define C_LOOP MINBODY_LOOP(jshort)
#define CMP >
#define MINMAX_MMXEX pminsw
#define PCMPGT pcmpgtw
#define MM0_FOR_MIN mm0
#define MM1_FOR_MIN mm1
#include "Arrays_minmax_int.h"
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    max
 * Signature: (J[SI[SII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_max__J_3SI_3SII
PAIR_PREFIX(jshort,jshortArray)
#define TYPE jshort
#define C_LOOP MAXBODY_LOOP(jshort)
#define CMP <
#define MINMAX_MMXEX pmaxsw
#define PCMPGT pcmpgtw
#define MM0_FOR_MIN mm1
#define MM1_FOR_MIN mm0
#include "Arrays_minmax_int.h"
PAIR_POSTFIX


/*
 * Class:     net_algart_array_ArraysNative
 * Method:    min
 * Signature: (J[II[III)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_min__J_3II_3III
PAIR_PREFIX(jint,jintArray)
#define TYPE jint
#define C_LOOP MINBODY_LOOP(jint)
#define CMP >
#define PCMPGT pcmpgtd
#define MM0_FOR_MIN mm0
#define MM1_FOR_MIN mm1
#include "Arrays_minmax_int.h"
PAIR_POSTFIX


/*
 * Class:     net_algart_array_ArraysNative
 * Method:    max
 * Signature: (J[II[III)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_max__J_3II_3III
PAIR_PREFIX(jint,jintArray)
#define TYPE jint
#define C_LOOP MAXBODY_LOOP(jint)
#define CMP <
#define PCMPGT pcmpgtd
#define MM0_FOR_MIN mm1
#define MM1_FOR_MIN mm0
#include "Arrays_minmax_int.h"
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    min
 * Signature: (J[JI[JII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_min__J_3JI_3JII
PAIR_PREFIX(jlong,jlongArray)
MINBODY_LOOP(jlong)
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    max
 * Signature: (J[JI[JII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_max__J_3JI_3JII
PAIR_PREFIX(jlong,jlongArray)
MAXBODY_LOOP(jlong)
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    min
 * Signature: (J[FI[FII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_min__J_3FI_3FII
PAIR_PREFIX(jfloat,jfloatArray)
#define TYPE jfloat
#define C_LOOP MINBODY_LOOP(jfloat)
#define CMP >
#define MINMAX_SSE minps
#include "Arrays_minmax_float.h"
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    max
 * Signature: (J[FI[FII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_max__J_3FI_3FII
PAIR_PREFIX(jfloat,jfloatArray)
#define TYPE jfloat
#define C_LOOP MAXBODY_LOOP(jfloat)
#define CMP <
#define MINMAX_SSE maxps
#include "Arrays_minmax_float.h"
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    min
 * Signature: (J[DI[DII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_min__J_3DI_3DII
PAIR_PREFIX(jdouble,jdoubleArray)
#define TYPE jdouble
#define C_LOOP MINBODY_LOOP(jdouble)
#define CMP >
#define FCMOV fcmovnb
#include "Arrays_minmax_double.h"
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    max
 * Signature: (J[DI[DII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_max__J_3DI_3DII
PAIR_PREFIX(jdouble,jdoubleArray)
#define TYPE jdouble
#define C_LOOP MAXBODY_LOOP(jdouble)
#define CMP <
#define FCMOV fcmovb
#include "Arrays_minmax_double.h"
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    minu
 * Signature: (J[BI[BII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_minu__J_3BI_3BII
PAIR_PREFIX(unsigned __int8,jbyteArray) 
#include "Arrays_pminub.h"
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    maxu
 * Signature: (J[BI[BII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_maxu__J_3BI_3BII
PAIR_PREFIX(unsigned __int8,jbyteArray)
#include "Arrays_pmaxub.h"
PAIR_POSTFIX


/*
 * Class:     net_algart_array_ArraysNative
 * Method:    minu
 * Signature: (JLjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;II)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_minu__JLjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2II
PAIRBUFFER_PREFIX(unsigned __int8,jobject)
#include "Arrays_pminub.h"
PAIRBUFFER_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    maxu
 * Signature: (JLjava/nio/ByteBuffer;ILjava/nio/ByteBuffer;II)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_maxu__JLjava_nio_ByteBuffer_2ILjava_nio_ByteBuffer_2II
PAIRBUFFER_PREFIX(unsigned __int8,jobject)
#include "Arrays_pmaxub.h"
PAIRBUFFER_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    minu
 * Signature: (J[SI[SII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_minu__J_3SI_3SII
PAIR_PREFIX(unsigned __int16,jshortArray)
#define TYPE unsigned __int16
#define C_LOOP MINBODY_LOOP(unsigned __int16)
#define CMP >
#define BIT_MASK_SSE 0x8000800080008000
#define MINMAX_MMXEX pminsw
#define BIT_MASK_MMX 0x8000800080008000
#define PCMPGT pcmpgtw
#define MM0_FOR_MIN mm0
#define MM1_FOR_MIN mm1
#include "Arrays_minmax_int.h"
PAIR_POSTFIX

/*
 * Class:     net_algart_array_ArraysNative
 * Method:    maxu
 * Signature: (J[SI[SII)V
 */
JNIEXPORT void JNICALL Java_net_algart_array_ArraysNative_maxu__J_3SI_3SII
PAIR_PREFIX(unsigned __int16,jshortArray)
#define TYPE unsigned __int16
#define C_LOOP MAXBODY_LOOP(unsigned __int16)
#define CMP <
#define BIT_MASK_SSE 0x8000800080008000
#define MINMAX_MMXEX pmaxsw
#define BIT_MASK_MMX 0x8000800080008000
#define PCMPGT pcmpgtw
#define MM0_FOR_MIN mm1
#define MM1_FOR_MIN mm0
#include "Arrays_minmax_int.h"
PAIR_POSTFIX
