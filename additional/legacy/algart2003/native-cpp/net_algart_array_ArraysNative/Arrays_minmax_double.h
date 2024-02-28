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
if (CpuInfo & CPU_CMOV) {
	TYPE *pa= (TYPE*)a+Aofs, *pb= (TYPE*)b+Bofs;
	LOOP_PREFIX_ALIGNED(64)
	for (; lenStart>0; lenStart--,pa++,pb++) if (*pa CMP *pb) *pa= *pb;
	__asm {
		mov eax,pa;
		mov edx,pb;
		mov ecx,len;
		jecxz _EndLoop;
	_Loop:
		fld qword ptr [eax];
		fld qword ptr [edx];
		fucomi st(0),st(1);
		FCMOV st(0),st(1);
		fstp qword ptr [eax];
		fstp st(0);
		fld qword ptr [eax+8];
		fld qword ptr [edx+8];
		fucomi st(0),st(1);
		FCMOV st(0),st(1);
		fstp qword ptr [eax+8];
		fstp st(0);
		fld qword ptr [eax+16];
		fld qword ptr [edx+16];
		fucomi st(0),st(1);
		FCMOV st(0),st(1);
		fstp qword ptr [eax+16];
		fstp st(0);
		fld qword ptr [eax+24];
		fld qword ptr [edx+24];
		fucomi st(0),st(1);
		FCMOV st(0),st(1);
		fstp qword ptr [eax+24];
		fstp st(0);
		fld qword ptr [eax+32];
		fld qword ptr [edx+32];
		fucomi st(0),st(1);
		FCMOV st(0),st(1);
		fstp qword ptr [eax+32];
		fstp st(0);
		fld qword ptr [eax+40];
		fld qword ptr [edx+40];
		fucomi st(0),st(1);
		FCMOV st(0),st(1);
		fstp qword ptr [eax+40];
		fstp st(0);
		fld qword ptr [eax+48];
		fld qword ptr [edx+48];
		fucomi st(0),st(1);
		FCMOV st(0),st(1);
		fstp qword ptr [eax+48];
		fstp st(0);
		fld qword ptr [eax+56];
		fld qword ptr [edx+56];
		fucomi st(0),st(1);
		FCMOV st(0),st(1);
		fstp qword ptr [eax+56];
		fstp st(0);
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
#undef FCMOV
