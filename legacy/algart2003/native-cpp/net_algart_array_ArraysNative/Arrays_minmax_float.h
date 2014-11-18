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
if (CpuInfo & CPU_SSE) {
	TYPE *pa= (TYPE*)a+Aofs, *pb= (TYPE*)b+Bofs;
	LOOP_PREFIX_ALIGNED(64)
	for (; lenStart>0; lenStart--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;
	__asm {
		mov eax,pa;
		mov edx,pb;
		mov ecx,len;
		jecxz _EndLoopAligned;
		test eax,0xF;
		jnz _Loop;
		test edx,0xF;
		jnz _Loop;
	_LoopAligned:
		prefetcht0 [eax+64];
		movaps xmm0,[eax];
		movaps xmm1,[eax+16];
		prefetcht0 [edx+64];
		movaps xmm2,[eax+32];
		movaps xmm3,[eax+48];
		prefetcht0 [eax+96];
		MINMAX_SSE xmm0,[edx];
		MINMAX_SSE xmm1,[edx+16];
		prefetcht0 [edx+96];
		MINMAX_SSE xmm2,[edx+32];
		MINMAX_SSE xmm3,[edx+48];
		movaps [eax],xmm0;
		movaps [eax+16],xmm1;
		movaps [eax+32],xmm2;
		movaps [eax+48],xmm3;
		add eax,64;
		add edx,64;
		dec ecx;
		jnz _LoopAligned;
	_EndLoopAligned:
		jmp _EndLoop;
	_Loop:
		prefetcht0 [eax+64];
		movups xmm0,[eax];
		movups xmm1,[eax+16];
		prefetcht0 [edx+64];
		movups xmm2,[eax+32];
		movups xmm3,[eax+48];
		prefetcht0 [eax+96];
		movups xmm4,[edx];
		movups xmm5,[edx+16];
		prefetcht0 [edx+96];
		movups xmm6,[edx+32];
		movups xmm7,[edx+48];
		MINMAX_SSE xmm0,xmm4;
		MINMAX_SSE xmm1,xmm5;
		MINMAX_SSE xmm2,xmm6;
		MINMAX_SSE xmm3,xmm7;
		movups [eax],xmm0;
		movups [eax+16],xmm1;
		movups [eax+32],xmm2;
		movups [eax+48],xmm3;
		add eax,64;
		add edx,64;
		dec ecx;
		jnz _Loop;
	_EndLoop:
		mov pa,eax;
		mov pb,edx;
	}
	for (; lenEnd>0; lenEnd--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;
} else
#endif //SSEASM_SUPPORTED
{
	C_LOOP
}
#undef TYPE
#undef C_LOOP
#undef CMP
#undef MINMAX_SSE
