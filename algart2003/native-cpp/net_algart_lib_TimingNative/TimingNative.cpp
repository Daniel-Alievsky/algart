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
#include <windows.h>
#include "net_algart_lib_TimingNative.h"

/*
 * Class:     net_algart_lib_TimingNative
 * Method:    timens
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_algart_lib_TimingNative_timens
(JNIEnv *, jclass) {
	static bool firstCall= true;
	static LARGE_INTEGER frequency;
	if (firstCall) {
		::QueryPerformanceFrequency(&frequency);
		firstCall= false;
	}
	LARGE_INTEGER counter;
	::QueryPerformanceCounter(&counter);
	return (jlong)(counter.QuadPart*1000000000.0/frequency.QuadPart+0.5);
}

/*
 * Class:     net_algart_lib_TimingNative
 * Method:    getTimecpuSupportedInternal
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_net_algart_lib_TimingNative_getTimecpuSupportedInternal
(JNIEnv *, jclass) {
	static int _timecpuSupported= -1;
	if (_timecpuSupported>=0) return _timecpuSupported;
	_timecpuSupported= 0;
	try {
		__asm {
			mov eax,1;
			_emit 0Fh;
			_emit 0A2h; //cpuid;
			test edx,1<<4;
			jz _TSC_No;
				xor eax,eax;
				xor edx,edx;
				_emit 0Fh;
				_emit 31h; //rdtsc;
				or eax,edx;
				jz _TSC_No;
				mov _timecpuSupported,1;
_TSC_No:
		}
	} catch (...) {
	}
	return _timecpuSupported;
}

/*
 * Class:     net_algart_lib_TimingNative
 * Method:    timecpuInternal
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_net_algart_lib_TimingNative_timecpuInternal
(JNIEnv *, jclass) {
	jlong result= 0;
	__asm {
		_emit 0Fh;
		_emit 31h; //rdtsc;
		mov dword ptr result,eax;
		mov dword ptr result+4,edx;
	}
	return result;
}
