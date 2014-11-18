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

#ifndef A_ARRAYSFUNCTIONS_H__INCLUDED_
#define A_ARRAYSFUNCTIONS_H__INCLUDED_

#pragma warning(disable: 4731)
__int64 _cpuInfo() {
	static bool cpuInfoCalculated= false;
	static __int64 cpuInfoLast= 0;
	if (cpuInfoCalculated) return cpuInfoLast;
	cpuInfoCalculated= true;
	static __int32 maxArgForCpuId;
	unsigned __int32 cpuid1regEAX;
	unsigned __int32 amdFeatures= 0;
	unsigned __int32 l1d= 0, l2= 0; // in KB
	signed __int32 cpuid2regs[4];
	unsigned __int8 *cpuid2info= (unsigned __int8*)cpuid2regs;
	try {
		__asm {
			pushad;
			xor eax,eax;
			_emit 0Fh;
			_emit 0A2h; //cpuid;
			or eax,eax;
			jz _Exit;
			mov maxArgForCpuId,eax;

			mov eax,1;
			_emit 0Fh;
			_emit 0A2h; //cpuid;
			mov cpuid1regEAX,eax;
			test edx,CPU_FPU;
			jnz _FPU_Ok;
				and edx,~CPU_CMOV;
_FPU_Ok:
			test edx,CPU_MMX;
			jnz _MMX_Ok;
				and edx,~(CPU_SSE|CPU_SSE2)
_MMX_Ok:
			test edx,CPU_SSE;
			jnz _SSE_Ok;
				and edx,~CPU_SSE2
_SSE_Ok:
			mov dword ptr cpuInfoLast,edx;

			mov eax,0x80000000;
			_emit 0Fh;
			_emit 0A2h; //cpuid;
			cmp eax,0x80000005;
			jb _NoAMD_K6IIIPLUS;
			
			push eax;
			mov eax,0x80000001;
			_emit 0Fh;
			_emit 0A2h; //cpuid;
			or edx,CPU_AMD_L;
			test edx,CPU_3DNOW_L;
			jnz _3DNOW_Ok;
				and edx,~CPU_3DNOWEX_L;
_3DNOW_Ok:
			mov amdFeatures,edx;
			mov eax,0x80000005;
			_emit 0Fh;
			_emit 0A2h; //cpuid;
			shr ecx,24;
			mov l1d,ecx;
			pop eax;

			cmp eax,0x80000006;
			jb _Exit;
			mov eax,0x80000006;
			_emit 0Fh;
			_emit 0A2h; //cpuid;
			shr ecx,16;
			mov l2,ecx;
			jmp _Exit;

_NoAMD_K6IIIPLUS:

			cmp maxArgForCpuId,2;
			jb _Exit;
			mov eax,2;
			_emit 0Fh;
			_emit 0A2h; //cpuid;
			mov cpuid2regs[0],eax;
			mov cpuid2regs[4],ebx;
			mov cpuid2regs[8],ecx;
			mov cpuid2regs[12],edx;
_Exit:
			popad;
		}
	} catch (...) {
		return cpuInfoLast= 0;
	}
	if (cpuInfoLast&CPU_SSE) cpuInfoLast|=CPU_MMXEX;
		// Under construction: checking AMD Athlon should be added here
	int cpuFamily= (cpuid1regEAX>>8)&15;
	if (cpuFamily==15) cpuFamily= (cpuid1regEAX>>20)&15; //Pentium 4+
	cpuInfoLast|= (__int64)(cpuFamily&CPU_FAMILY)<<CPU_FAMILY_SHIFT;
	cpuInfoLast|= (__int64)(amdFeatures&(CPU_AMD_L|CPU_3DNOW_L|CPU_3DNOWEX_L))<<32;
	if (amdFeatures==0 && maxArgForCpuId>=2) {
		for (int j=0; j<4; j++) if (cpuid2regs[j]<0) cpuid2regs[j]= 0;
		for (int k=1 /*skipping AL*/; k<16; k++) {
			unsigned __int8 v= cpuid2info[k];
			int vl= v&0xF;
			switch(v>>4) {
			case 0:
				switch(vl) {
				case 10: l1d+= 8; break;
				case 12: l1d+= 16; break;
				}
				break;
			case 4:
				if (cpuFamily>6) break; //P4+, L3 cache; else same as 8
			case 8:
				if (vl) l2+= 128<<(vl-1);
				break;
			case 6:
				if (vl>=6) l1d+= 8<<(vl-6);
				break;
			case 7:
				if (vl>=8) l2+= 64<<(vl-8);
				break;
			}
		}
	}
	l1d/= CPU_L1DATASIZE_UNIT/1024;
	l2/= CPU_L2SIZE_UNIT/1024;
	cpuInfoLast|= ((__int64)(l1d&CPU_L1DATASIZE))<<CPU_L1DATASIZE_SHIFT;
	cpuInfoLast|= ((__int64)(l2&CPU_L2SIZE))<<CPU_L2SIZE_SHIFT;
	return cpuInfoLast;
}
#pragma warning(default: 4731)

#endif //A_ARRAYSFUNCTIONS_H__INCLUDED_
