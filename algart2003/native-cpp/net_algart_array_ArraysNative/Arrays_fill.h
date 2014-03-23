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
if ((CpuInfo & CPU_SSE) && (Len*sizeof(TYPE)>32)) {
	int cacheSize= (int)((CpuInfo>>CPU_L2SIZE_SHIFT)&CPU_L2SIZE)*CPU_L2SIZE_UNIT;
	if (cacheSize<65536) cacheSize= 65536;
	int cacheSizeIn128ByteBlocks= cacheSize/128;
	
	TYPE *pa= (TYPE*)a+BeginIndex;

	__int8 filler[32];
	for (int j=0; j<32/sizeof(*pa); j++) ((TYPE*)filler)[j]= V;
	__int32 fillerDisp= 0;

	jint len= Len*sizeof(*pa);
	int disp= (int)pa&31;
	__int32 lenStart= 0;
	if (disp!=0) {
		lenStart= 32-disp;
		fillerDisp= lenStart&15;
		len-= lenStart;
	}
	jint lenEnd= len&15;
	len/= 16;
	
	__asm {
		push esi;
		push edi;
		lea esi,filler;
		mov edi,pa;
		mov ecx,lenStart;
		cld;
		rep movsb;

		lea esi,filler;
		add esi,fillerDisp;
		movups xmm0,[esi];
		mov ecx,len;
		shr ecx,3;
		jz _EndLoop;
		cmp ecx,cacheSizeIn128ByteBlocks;
		jb _Loop;
	_Loop_ntps:
		movntps [edi],xmm0;
		movntps [edi+16],xmm0;
		movntps [edi+32],xmm0;
		movntps [edi+48],xmm0;
		movntps [edi+64],xmm0;
		movntps [edi+80],xmm0;
		movntps [edi+96],xmm0;
		movntps [edi+112],xmm0;
		add edi,128;
		dec ecx;
		jnz _Loop_ntps;
		jmp _EndLoop;
	_Loop:
		movaps [edi],xmm0;
		movaps [edi+16],xmm0;
		movaps [edi+32],xmm0;
		movaps [edi+48],xmm0;
		movaps [edi+64],xmm0;
		movaps [edi+80],xmm0;
		movaps [edi+96],xmm0;
		movaps [edi+112],xmm0;
		add edi,128;
		dec ecx;
		jnz _Loop;
	_EndLoop:
		mov ecx,len;
		and ecx,7;
		jz _EndLoop2;
	_Loop2:
		movaps [edi],xmm0;
		add edi,16;
		dec ecx;
		jnz _Loop2;
	_EndLoop2:
		mov pa,edi;

		mov ecx,lenEnd;
		rep movsb;
		pop edi;
		pop esi;
	}
} else
#endif //SSEASM_SUPPORTED
{
	TYPE *pa= (TYPE*)a+BeginIndex;
	if (sizeof(TYPE)==1) {
		memset(pa,(int)V,Len);
	} else {
		LOOP_PREFIX(8*sizeof(TYPE))
		for (; len>0; len--,pa+=8) {
			pa[0]= V; pa[1]= V; pa[2]= V; pa[3]= V;
			pa[4]= V; pa[5]= V; pa[6]= V; pa[7]= V;
		}\
		for (; lenEnd>0; lenEnd--,pa++) *pa= V;
	}
}
#undef TYPE
