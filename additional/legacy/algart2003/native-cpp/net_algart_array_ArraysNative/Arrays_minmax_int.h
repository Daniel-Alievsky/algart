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

#ifdef SSEASM_SUPPORTED

#ifdef MINMAX_SSE2

if (CpuInfo & CPU_SSE2) {
	TYPE *pa= (TYPE*)a+Aofs, *pb= (TYPE*)b+Bofs;
#ifdef BIT_MASK_SSE
	static struct {__int64 bitMask1= BIT_MASK_SSE; __int64 bitMask2= BIT_MASK_SSE;} bitMask;
	LOOP_PREFIX_ALIGNED(32)
	for (; lenStart>0; lenStart--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;
	__asm {
		mov eax,pa;
		mov edx,pb;
		mov ecx,len;
		jecxz _EndLoopSSE2;
		movups xmm7,bitMask;
	_LoopSSE2:
		prefetcht0 [eax+32];
		movups xmm0,[eax];
		pxor xmm0,xmm7;
		movups xmm1,[eax+8];
		pxor xmm1,xmm7;
		movups xmm2,[edx];
		pxor xmm2,xmm7;
		movups xmm3,[edx+8];
		pxor xmm3,xmm7;
		MINMAX_SSE2 xmm0,xmm2;
		MINMAX_SSE2 xmm1,xmm3;
		pxor xmm0,xmm7;
		pxor xmm1,xmm7;
		movups [eax],xmm0;
		movups [eax+8],xmm1;
		prefetcht0 [edx+32];
		movups xmm0,[eax+16];
		pxor xmm0,xmm7;
		movups xmm1,[eax+24];
		pxor xmm1,xmm7;
		movups xmm2,[edx+16];
		pxor xmm2,xmm7;
		movups xmm3,[edx+24];
		pxor xmm3,xmm7;
		MINMAX_SSE2 xmm0,xmm2;
		MINMAX_SSE2 xmm1,xmm3;
		pxor xmm0,xmm7;
		pxor xmm1,xmm7;
		movups [eax+16],xmm0;
		movups [eax+24],xmm1;
		add eax,32;
		add edx,32;
		dec ecx;
		jnz _LoopSSE2;
	_EndLoopSSE2:
		mov pa,eax;
		mov pb,edx;
	}
#else
	LOOP_PREFIX_ALIGNED(64)
	for (; lenStart>0; lenStart--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;
	__asm {
		mov eax,pa;
		mov edx,pb;
		mov ecx,len;
		jecxz _EndLoopSSE2;
	_LoopSSE2:
		prefetcht0 [eax+64];
		movups xmm0,[eax];
		movups xmm1,[eax+8];
		prefetcht0 [edx+64];
		movups xmm2,[eax+16];
		movups xmm3,[eax+24];
		movups xmm4,[eax+32];
		movups xmm5,[eax+40];
		movups xmm6,[eax+48];
		movups xmm7,[eax+56];
		prefetcht0 [eax+96];
		MINMAX_SSE2 xmm0,[edx];
		MINMAX_SSE2 xmm1,[edx+8];
		prefetcht0 [edx+96];
		MINMAX_SSE2 xmm2,[edx+16];
		MINMAX_SSE2 xmm3,[edx+24];
		MINMAX_SSE2 xmm4,[edx+32];
		MINMAX_SSE2 xmm5,[edx+40];
		MINMAX_SSE2 xmm6,[edx+48];
		MINMAX_SSE2 xmm7,[edx+56];
		movups [eax],xmm0;
		movups [eax+8],xmm1;
		movups [eax+16],xmm2;
		movups [eax+24],xmm3;
		movups [eax+32],xmm4;
		movups [eax+40],xmm5;
		movups [eax+48],xmm6;
		movups [eax+56],xmm7;
		add eax,64;
		add edx,64;
		dec ecx;
		jnz _LoopSSE2;
	_EndLoopSSE2:
		mov pa,eax;
		mov pb,edx;
	}
#endif
	for (; lenEnd>0; lenEnd--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;

} else 
#endif //MINMAX_SSE2

#ifdef MINMAX_MMXEX
if (CpuInfo & CPU_MMXEX) {
	TYPE *pa= (TYPE*)a+Aofs, *pb= (TYPE*)b+Bofs;
#ifdef BIT_MASK_SSE
	static __int64 bitMask= BIT_MASK_SSE;
	LOOP_PREFIX_ALIGNED(32)
	for (; lenStart>0; lenStart--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;
	__asm {
		mov eax,pa;
		mov edx,pb;
		mov ecx,len;
		jecxz _EndLoopSSE;
		movq mm7,bitMask;
	_LoopSSE:
		prefetcht0 [eax+32];
		movq mm0,[eax];
		pxor mm0,mm7;
		movq mm1,[eax+8];
		pxor mm1,mm7;
		movq mm2,[edx];
		pxor mm2,mm7;
		movq mm3,[edx+8];
		pxor mm3,mm7;
		MINMAX_MMXEX mm0,mm2;
		MINMAX_MMXEX mm1,mm3;
		pxor mm0,mm7;
		pxor mm1,mm7;
		movq [eax],mm0;
		movq [eax+8],mm1;
		prefetcht0 [edx+32];
		movq mm0,[eax+16];
		pxor mm0,mm7;
		movq mm1,[eax+24];
		pxor mm1,mm7;
		movq mm2,[edx+16];
		pxor mm2,mm7;
		movq mm3,[edx+24];
		pxor mm3,mm7;
		MINMAX_MMXEX mm0,mm2;
		MINMAX_MMXEX mm1,mm3;
		pxor mm0,mm7;
		pxor mm1,mm7;
		movq [eax+16],mm0;
		movq [eax+24],mm1;
		add eax,32;
		add edx,32;
		dec ecx;
		jnz _LoopSSE;
	_EndLoopSSE:
		emms;
		mov pa,eax;
		mov pb,edx;
	}
#else
	LOOP_PREFIX_ALIGNED(64)
	for (; lenStart>0; lenStart--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;
	__asm {
		mov eax,pa;
		mov edx,pb;
		mov ecx,len;
		jecxz _EndLoopSSE;
	_LoopSSE:
		prefetcht0 [eax+64];
		movq mm0,[eax];
		movq mm1,[eax+8];
		prefetcht0 [edx+64];
		movq mm2,[eax+16];
		movq mm3,[eax+24];
		movq mm4,[eax+32];
		movq mm5,[eax+40];
		movq mm6,[eax+48];
		movq mm7,[eax+56];
		prefetcht0 [eax+96];
		MINMAX_MMXEX mm0,[edx];
		MINMAX_MMXEX mm1,[edx+8];
		prefetcht0 [edx+96];
		MINMAX_MMXEX mm2,[edx+16];
		MINMAX_MMXEX mm3,[edx+24];
		MINMAX_MMXEX mm4,[edx+32];
		MINMAX_MMXEX mm5,[edx+40];
		MINMAX_MMXEX mm6,[edx+48];
		MINMAX_MMXEX mm7,[edx+56];
		movq [eax],mm0;
		movq [eax+8],mm1;
		movq [eax+16],mm2;
		movq [eax+24],mm3;
		movq [eax+32],mm4;
		movq [eax+40],mm5;
		movq [eax+48],mm6;
		movq [eax+56],mm7;
		add eax,64;
		add edx,64;
		dec ecx;
		jnz _LoopSSE;
	_EndLoopSSE:
		emms;
		mov pa,eax;
		mov pb,edx;
	}
#endif
	for (; lenEnd>0; lenEnd--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;

} else 
#endif //MINMAX_MMXEX

#endif //SSEASM_SUPPORTED

#ifdef MMXASM_SUPPORTED
if (CpuInfo & CPU_MMX) {
	TYPE *pa= (TYPE*)a+Aofs, *pb= (TYPE*)b+Bofs;
#ifdef BIT_MASK_MMX
	static __int64 bitMask= BIT_MASK_MMX;
#endif
	LOOP_PREFIX_ALIGNED(32)
	for (; lenStart>0; lenStart--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;
	__asm {
		mov eax,pa;
		mov edx,pb;
		mov ecx,len;
		or ecx,ecx;
		jz _EndLoopMMX;
#ifdef BIT_MASK_MMX
		movq mm7,bitMask;
	_LoopMMX:
		movq mm0,[eax];
		movq mm1,[edx];
		movq mm2,MM0_FOR_MIN;
		pxor mm2,mm7;
		movq mm3,MM1_FOR_MIN;
		pxor mm3,mm7;
		PCMPGT mm2,mm3;
		pand mm1,mm2;
		pandn mm2,mm0;
		por mm1,mm2;
		movq [eax],mm1;
		movq mm0,[eax+8];
		movq mm1,[edx+8];
		movq mm2,MM0_FOR_MIN;
		pxor mm2,mm7;
		movq mm3,MM1_FOR_MIN;
		pxor mm3,mm7;
		PCMPGT mm2,mm3;
		pand mm1,mm2;
		pandn mm2,mm0;
		por mm1,mm2;
		movq [eax+8],mm1;
		movq mm0,[eax+16];
		movq mm1,[edx+16];
		movq mm2,MM0_FOR_MIN;
		pxor mm2,mm7;
		movq mm3,MM1_FOR_MIN;
		pxor mm3,mm7;
		PCMPGT mm2,mm3;
		pand mm1,mm2;
		pandn mm2,mm0;
		por mm1,mm2;
		movq [eax+16],mm1;
		movq mm0,[eax+24];
		movq mm1,[edx+24];
		movq mm2,MM0_FOR_MIN;
		pxor mm2,mm7;
		movq mm3,MM1_FOR_MIN;
		pxor mm3,mm7;
		PCMPGT mm2,mm3;
		pand mm1,mm2;
		pandn mm2,mm0;
		por mm1,mm2;
		movq [eax+24],mm1;
#else
	_LoopMMX:
		movq mm0,[eax];
		movq mm1,[edx];
		movq mm2,MM0_FOR_MIN;
		PCMPGT mm2,MM1_FOR_MIN;
		pand mm1,mm2;
		pandn mm2,mm0;
		por mm1,mm2;
		movq [eax],mm1;
		movq mm0,[eax+8];
		movq mm1,[edx+8];
		movq mm2,MM0_FOR_MIN;
		PCMPGT mm2,MM1_FOR_MIN;
		pand mm1,mm2;
		pandn mm2,mm0;
		por mm1,mm2;
		movq [eax+8],mm1;
		movq mm0,[eax+16];
		movq mm1,[edx+16];
		movq mm2,MM0_FOR_MIN;
		PCMPGT mm2,MM1_FOR_MIN;
		pand mm1,mm2;
		pandn mm2,mm0;
		por mm1,mm2;
		movq [eax+16],mm1;
		movq mm0,[eax+24];
		movq mm1,[edx+24];
		movq mm2,MM0_FOR_MIN;
		PCMPGT mm2,MM1_FOR_MIN;
		pand mm1,mm2;
		pandn mm2,mm0;
		por mm1,mm2;
		movq [eax+24],mm1;
#endif
		add eax,32;
		add edx,32;
		dec ecx;
		jnz _LoopMMX;
	_EndLoopMMX:
		emms;
		mov pa,eax;
		mov pb,edx;
	}
	for (; lenEnd>0; lenEnd--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;
} else
#endif //MMXASM_SUPPORTED

{
	C_LOOP
}

#undef TYPE
#undef C_LOOP
#undef CMP
#undef BIT_MASK_SSE
#undef MINMAX_SSE2
#undef MINMAX_MMXEX
#undef BIT_MASK_MMX
#undef PCMPGT
#undef MM0_FOR_MIN
#undef MM1_FOR_MIN
