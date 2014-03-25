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

package net.algart.lib.tests.performance;

import net.algart.lib.Timing;
import net.algart.lib.Out;

class SomeClass {
    int inta, intb;
    byte bytea, byteb;
    float floata, floatb;
    double doublea, doubleb;
}

public class CommonSpeed {
    static final double MULT = 1000000.0;
    static final String UNITS = " ns";

    static final int NUM = 1 * 1024 * 1024;
    static final int NUMd4 = NUM / 4; // must be integer
    static int NUMe;
    static int TOTAL;

    public CommonSpeed(int NUMe) {
        this.NUMe = NUMe;
        this.TOTAL = NUM * NUMe;
    }

    public CommonSpeed() {
        this(32);
    }

    public static String memInfo() {
        Runtime rt = Runtime.getRuntime();
        return "Memory: " + Out.dec(rt.totalMemory() / 1024.0 / 1024, 4) + " Mb total = "
            + Out.dec((rt.totalMemory() - rt.freeMemory()) / 1024.0 / 1024, 4) + " Mb used + "
            + Out.dec(rt.freeMemory() / 1024.0 / 1024, 4) + " Mb free; "
            + Out.dec(rt.maxMemory() / 1024.0 / 1024, 4) + " Mb max";
    }

    {
        Out.println(memInfo());
        Out.print("Allocating memory - " + (NUM * 2 * 4 + NUM * 2 * 1 + NUM * 2 * 8 + NUM * 2 * 4 +
            NUM * 2 * 8) / 1024.0 / 1024 +
            " Mb...");
    }

    int[] inta = new int[NUM];
    int[] intb = new int[NUM];
    byte[] bytea = new byte[NUM];
    byte[] byteb = new byte[NUM];
    long[] longa = new long[NUM];
    long[] longb = new long[NUM];
    float[] floata = new float[NUM];
    float[] floatb = new float[NUM];
    double[] doublea = new double[NUM];
    double[] doubleb = new double[NUM];
    {
        Out.println(" Done");
        Out.println(memInfo());
    }

    static final int testProcedure65536NUM = 64; //must be <=NUM
    static final int testProcedure1024NUM = 1024; //must be <=NUM
    static final int testProcedure64NUM = 16 * 1024; //must be <=NUM

    public void emptyMethod() {
    }

    synchronized public void emptyMethodSynchronized() {
    }

    class BitwiseTest {
        /* Optimized block in one class cannot be too large! */
        int[] inta = CommonSpeed.this.inta;
        int[] intb = CommonSpeed.this.intb;
        byte[] bytea = CommonSpeed.this.bytea;
        byte[] byteb = CommonSpeed.this.byteb;
        long[] longa = CommonSpeed.this.longa;
        long[] longb = CommonSpeed.this.longb;
        float[] floata = CommonSpeed.this.floata;
        float[] floatb = CommonSpeed.this.floatb;
        double[] doublea = CommonSpeed.this.doublea;
        double[] doubleb = CommonSpeed.this.doubleb;
        void test() {
            System.gc();
            long t1, t2;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[k] |= inta[k];
            t2 = Timing.timems();
            Out.println("int|=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int disp = 0; disp < NUM; disp += 8) {
                    intb[disp] |= inta[disp]; intb[disp + 1] |= inta[disp + 1];
                    intb[disp + 2] |= inta[disp + 2]; intb[disp + 3] |= inta[disp + 3];
                    intb[disp + 4] |= inta[disp + 4]; intb[disp + 5] |= inta[disp + 5];
                    intb[disp + 6] |= inta[disp + 6]; intb[disp + 7] |= inta[disp + 7];
                }

            t2 = Timing.timems();
            Out.println("int|=int per 8:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0, sh = 1; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[k] = inta[k] >>> sh;
            t2 = Timing.timems();
            Out.println("int=int>>>sh(=1):         " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0, sh = 1; n < NUMe; n++)
                for (int disp = 0; disp < NUM; disp += 8) {
                    intb[disp] = inta[disp] >>> sh; intb[disp + 1] = inta[disp + 1] >>> sh;
                    intb[disp + 2] = inta[disp + 2] >>> sh; intb[disp + 3] = inta[disp + 3] >>> sh;
                    intb[disp + 4] = inta[disp + 4] >>> sh; intb[disp + 5] = inta[disp + 5] >>> sh;
                    intb[disp + 6] = inta[disp + 6] >>> sh; intb[disp + 7] = inta[disp + 7] >>> sh;
                }
            t2 = Timing.timems();
            Out.println("int=int>>>sh(=1) per 8:   " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0, sh = 23; n < NUMe; n++)
                for (int disp = 0; disp < NUM; disp += 8) {
                    intb[disp] = inta[disp] >>> sh; intb[disp + 1] = inta[disp + 1] >>> sh;
                    intb[disp + 2] = inta[disp + 2] >>> sh; intb[disp + 3] = inta[disp + 3] >>> sh;
                    intb[disp + 4] = inta[disp + 4] >>> sh; intb[disp + 5] = inta[disp + 5] >>> sh;
                    intb[disp + 6] = inta[disp + 6] >>> sh; intb[disp + 7] = inta[disp + 7] >>> sh;
                }
            t2 = Timing.timems();
            Out.println("int=int>>>sh(=23) per 8:  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    longb[k] |= longa[k];
            t2 = Timing.timems();
            Out.println("long|=long:               " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int disp = 0; disp < NUM; disp += 8) {
                    longb[disp] |= longa[disp]; longb[disp + 1] |= longa[disp + 1];
                    longb[disp + 2] |= longa[disp + 2]; longb[disp + 3] |= longa[disp + 3];
                    longb[disp + 4] |= longa[disp + 4]; longb[disp + 5] |= longa[disp + 5];
                    longb[disp + 6] |= longa[disp + 6]; longb[disp + 7] |= longa[disp + 7];
                }
            t2 = Timing.timems();
            Out.println("long|=long per 8:         " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0, sh = 1; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    longb[k] = longa[k] >>> sh;
            t2 = Timing.timems();
            Out.println("long=long>>>sh(=1):       " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0, sh = 1; n < NUMe; n++)
                for (int disp = 0; disp < NUM; disp += 8) {
                    longb[disp] = longa[disp] >>> sh; longb[disp + 1] = longa[disp + 1] >>> sh;
                    longb[disp + 2] = longa[disp + 2] >>> sh; longb[disp + 3] = longa[disp + 3] >>> sh;
                    longb[disp + 4] = longa[disp + 4] >>> sh; longb[disp + 5] = longa[disp + 5] >>> sh;
                    longb[disp + 6] = longa[disp + 6] >>> sh; longb[disp + 7] = longa[disp + 7] >>> sh;
                }
            t2 = Timing.timems();
            Out.println("long=long>>>sh(=1) per 8: " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0, sh = 23; n < NUMe; n++)
                for (int disp = 0; disp < NUM; disp += 8) {
                    longb[disp] = longa[disp] >>> sh; longb[disp + 1] = longa[disp + 1] >>> sh;
                    longb[disp + 2] = longa[disp + 2] >>> sh; longb[disp + 3] = longa[disp + 3] >>> sh;
                    longb[disp + 4] = longa[disp + 4] >>> sh; longb[disp + 5] = longa[disp + 5] >>> sh;
                    longb[disp + 6] = longa[disp + 6] >>> sh; longb[disp + 7] = longa[disp + 7] >>> sh;
                }
            t2 = Timing.timems();
            Out.println("long=long>>>sh(=23) per 8:" + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");
        }
    }

    class FieldTest {
        int inta0;
        int intb0;
        byte bytea0;
        byte byteb0;
        long longa0;
        long longb0;
        float floata0;
        float floatb0;
        double doublea0;
        double doubleb0;
        boolean booleana0;
        String stringa0 = "Hello!";
        String stringb0 = "Hello?";
        java.util.Map imap = new java.util.HashMap(100); {
            imap.put(new Integer(157), "Hello157");
            imap.put(new Integer(294), "Hello294");
        }

        Integer i157 = new Integer(157);
        Integer i294 = new Integer(294);
        {
            if (!imap.get(i157).equals("Hello157"))
                System.err.println("HashMap error 157!");
            if (!imap.get(i294).equals("Hello294"))
                System.err.println("HashMap error 294!");
        }

        SomeClass c = new SomeClass();
        void test() {
            System.gc();

            long t1, t2;
            Out.println("");
            Out.println("Number operation:");
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta0 = 157;
            t2 = Timing.timems();
            Out.println("int=157:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb0 = inta0;
            t2 = Timing.timems();
            Out.println("int=int:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    bytea0 = 15;
            t2 = Timing.timems();
            Out.println("byte=15:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    byteb0 = bytea0;
            t2 = Timing.timems();
            Out.println("byte=byte:                " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doublea0 = 157.0;
            t2 = Timing.timems();
            Out.println("double=157.0:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb0 = doublea0;
            t2 = Timing.timems();
            Out.println("double=double:            " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floata0 = (float)157.0;
            t2 = Timing.timems();
            Out.println("float=157.0:              " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb0 = floata0;
            t2 = Timing.timems();
            Out.println("float=float:              " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    emptyMethod();
            t2 = Timing.timems();
            Out.println("calling method:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    emptyMethodSynchronized();
            t2 = Timing.timems();
            Out.println("calling synchronized:     " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta0 = intb0 = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb0 += inta0;
            t2 = Timing.timems();
            Out.println("int+=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                bytea0 = byteb0 = 0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    byteb0 += bytea0;
            t2 = Timing.timems();
            Out.println("byte+=byte:               " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea0 = doubleb0 = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb0 += doublea0;
            t2 = Timing.timems();
            Out.println("double+=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata0 = floatb0 = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb0 += floata0;
            t2 = Timing.timems();
            Out.println("float+=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta0 = intb0 = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb0 -= inta0;
            t2 = Timing.timems();
            Out.println("int-=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea0 = doubleb0 = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb0 -= doublea0;
            t2 = Timing.timems();
            Out.println("double-=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata0 = floatb0 = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb0 -= floata0;
            t2 = Timing.timems();
            Out.println("float-=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb0 = Math.max(inta0, intb0);
            t2 = Timing.timems();
            Out.println("int=max(int,int):         " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb0 = Math.max(doublea0, doubleb0);
            t2 = Timing.timems();
            Out.println("double=max(double,double):" + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    if (inta0 > intb0)
                        intb0 = inta0;
            t2 = Timing.timems();
            Out.println("if (int>int) int=int:     " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    if (doubleb0 > doublea0)
                        doubleb0 = doubleb0;
            t2 = Timing.timems();
            Out.println("if (dbl>dbl) dbl=dbl:     " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta0 = intb0 = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb0 *= inta0;
            t2 = Timing.timems();
            Out.println("int*=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea0 = doubleb0 = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb0 *= doublea0;
            t2 = Timing.timems();
            Out.println("double*=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata0 = floatb0 = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb0 *= floata0;
            t2 = Timing.timems();
            Out.println("float*=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta0 = intb0 = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb0 /= inta0;
            t2 = Timing.timems();
            Out.println("int/=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea0 = doubleb0 = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb0 /= doublea0;
            t2 = Timing.timems();
            Out.println("double/=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata0 = floatb0 = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb0 /= floata0;
            t2 = Timing.timems();
            Out.println("float/=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    testProcedure(inta0);
            t2 = Timing.timems();
            Out.println("void testProcedure(int):  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUMd4; k++)
                    inta0 = (int)Math.round(doublea0);
            t2 = Timing.timems();
            Out.println("int=Math.round(double):   " + (t2 - t1) * MULT / (TOTAL / 4) + UNITS + "/element   \t(" + (t2 -
                t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUMd4; k++)
                    inta0 = Math.round(floata0);
            t2 = Timing.timems();
            Out.println("int=Math.round(float):    " + (t2 - t1) * MULT / (TOTAL / 4) + UNITS + "/element   \t(" + (t2 -
                t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floata0 = (float)doublea0;
            t2 = Timing.timems();
            Out.println("float=(float)double:      " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta0 = (int)floata0;
            t2 = Timing.timems();
            Out.println("int=(int)float:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta0 = (int)doublea0;
            t2 = Timing.timems();
            Out.println("int=(int)double:          " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta0 = (int)(doublea0 + 0.5);
            t2 = Timing.timems();
            Out.println("int=(int)(double+0.5):    " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea0 = 100000.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb0 = Math.sqrt(doublea0);
            t2 = Timing.timems();
            Out.println("double=Math.sqrt(double): " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb0 = Math.sin(doublea0);
            t2 = Timing.timems();
            Out.println("double=Math.sin(double):  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    imap.get(i157);
            t2 = Timing.timems();
            Out.println("HashMap.get(157):         " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    imap.get(i294);
            t2 = Timing.timems();
            Out.println("HashMap.get(294):         " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    booleana0 = stringa0.equals(stringa0);
            t2 = Timing.timems();
            Out.println("\"Hello!\".equals(\"Hello!\"):" + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 -
                t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    booleana0 = stringa0.equals(stringb0);
            t2 = Timing.timems();
            Out.println("\"Hello!\".equals(\"Hello?\"):" + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 -
                t1) +
                " ms total)");
        }
    }

    class FieldOtherClassTest {
        SomeClass c = new SomeClass();
        void test() {
            long t1, t2;

            Out.println("");
            Out.println("Number operation from other class:");
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.inta = 157;
            t2 = Timing.timems();
            Out.println("int=157:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.inta = "1234567890".length();
            t2 = Timing.timems();
            Out.println("int=\"1234567890\".length():" + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.intb = c.inta;
            t2 = Timing.timems();
            Out.println("int=int:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.bytea = 15;
            t2 = Timing.timems();
            Out.println("byte=15:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.byteb = c.bytea;
            t2 = Timing.timems();
            Out.println("byte=byte:                " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.doublea = 157.0;
            t2 = Timing.timems();
            Out.println("double=157.0:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.doubleb = c.doublea;
            t2 = Timing.timems();
            Out.println("double=double:            " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.floata = (float)157.0;
            t2 = Timing.timems();
            Out.println("float=157.0:              " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.floatb = c.floata;
            t2 = Timing.timems();
            Out.println("float=float:              " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.inta = c.intb = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.intb += c.inta;
            t2 = Timing.timems();
            Out.println("int+=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.bytea = c.byteb = 0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.byteb += c.bytea;
            t2 = Timing.timems();
            Out.println("byte+=byte:               " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.doublea = c.doubleb = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.doubleb += c.doublea;
            t2 = Timing.timems();
            Out.println("double+=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.floata = c.floatb = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.floatb += c.floata;
            t2 = Timing.timems();
            Out.println("float+=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.inta = c.intb = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.intb -= c.inta;
            t2 = Timing.timems();
            Out.println("int-=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.doublea = c.doubleb = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.doubleb -= c.doublea;
            t2 = Timing.timems();
            Out.println("double-=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.floata = c.floatb = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.floatb -= c.floata;
            t2 = Timing.timems();
            Out.println("float-=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.intb = Math.max(c.inta, c.intb);
            t2 = Timing.timems();
            Out.println("int=max(int,int):         " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.doubleb = Math.max(c.doublea, c.doubleb);
            t2 = Timing.timems();
            Out.println("double=max(double,double):" + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    if (c.inta > c.intb)
                        c.intb = c.inta;
            t2 = Timing.timems();
            Out.println("if (int>int) int=int:     " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    if (c.doubleb > c.doublea)
                        c.doubleb = c.doubleb;
            t2 = Timing.timems();
            Out.println("if (dbl>dbl) dbl=dbl:     " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.inta = c.intb = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.intb *= c.inta;
            t2 = Timing.timems();
            Out.println("int*=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.doublea = c.doubleb = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.doubleb *= c.doublea;
            t2 = Timing.timems();
            Out.println("double*=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.floata = c.floatb = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.floatb *= c.floata;
            t2 = Timing.timems();
            Out.println("float*=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.inta = c.intb = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.intb /= c.inta;
            t2 = Timing.timems();
            Out.println("int/=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.doublea = c.doubleb = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.doubleb /= c.doublea;
            t2 = Timing.timems();
            Out.println("double/=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.floata = c.floatb = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.floatb /= c.floata;
            t2 = Timing.timems();
            Out.println("float/=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    testProcedure(c.inta);
            t2 = Timing.timems();
            Out.println("void testProcedure(int):  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUMd4; k++)
                    c.inta = (int)Math.round(c.doublea);
            t2 = Timing.timems();
            Out.println("int=Math.round(double):   " + (t2 - t1) * MULT / (TOTAL / 4) + UNITS + "/element   \t(" + (t2 -
                t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUMd4; k++)
                    c.inta = Math.round(c.floata);
            t2 = Timing.timems();
            Out.println("int=Math.round(float):    " + (t2 - t1) * MULT / (TOTAL / 4) + UNITS + "/element   \t(" + (t2 -
                t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.inta = (int)c.doublea;
            t2 = Timing.timems();
            Out.println("int=(int)double:          " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.inta = (int)(c.doublea + 0.5);
            t2 = Timing.timems();
            Out.println("int=(int)(double+0.5):    " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                c.doublea = 100000.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.doubleb = Math.sqrt(c.doublea);
            t2 = Timing.timems();
            Out.println("double=Math.sqrt(double): " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    c.doubleb = Math.sin(c.doublea);
            t2 = Timing.timems();
            Out.println("double=Math.sin(double):  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");
        }
    }

    private int testProcedure65536(boolean needFill) {
        int[] v = new int[65536];
        v[32768] = 0;
        if (needFill)
            for (int k = 0; k < 65536; k++)
                v[k] = k;
        return v[32768];
    }

    private int testProcedure1024(boolean needFill) {
        int[] v = new int[1024];
        v[512] = 0;
        if (needFill)
            for (int k = 0; k < 1024; k++)
                v[k] = k;
        return v[512];
    }

    private int testProcedure64(boolean needFill) {
        int[] v = new int[64];
        v[32] = 0;
        if (needFill)
            for (int k = 0; k < 64; k++)
                v[k] = k;
        return v[32];
    }

    private int testProcedure(int v) {
        return v;
    }

    class ArrayTest {
        int[] inta = CommonSpeed.this.inta;
        int[] intb = CommonSpeed.this.intb;
        byte[] bytea = CommonSpeed.this.bytea;
        byte[] byteb = CommonSpeed.this.byteb;
        long[] longa = CommonSpeed.this.longa;
        long[] longb = CommonSpeed.this.longb;
        float[] floata = CommonSpeed.this.floata;
        float[] floatb = CommonSpeed.this.floatb;
        double[] doublea = CommonSpeed.this.doublea;
        double[] doubleb = CommonSpeed.this.doubleb;
        void test() {
            long t1, t2;
            Out.print("Filling " + NUM * 4 / 1024.0 / 1024 + " Mb...");
            for (int k = 0; k < NUM; k++)
                inta[k] = 1;
            Out.println();
            Out.println(memInfo());

            Out.println();
            Out.println("Array operation:");
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    longa[k] = System.currentTimeMillis();
            t2 = Timing.timems();
            Out.println("long=currentTimeMillis(): " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta[k] = 157;
            t2 = Timing.timems();
            Out.println("int=157:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[k] = inta[k];
            t2 = Timing.timems();
            Out.println("int=int:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                System.arraycopy(inta, 0, intb, 0, NUM);
            t2 = Timing.timems();
            Out.println("int=int (arraycopy):      " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    bytea[k] = 15;
            t2 = Timing.timems();
            Out.println("byte=15:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    byteb[k] = bytea[k];
            t2 = Timing.timems();
            Out.println("byte=byte:                " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                System.arraycopy(bytea, 0, byteb, 0, NUM);
            t2 = Timing.timems();
            Out.println("byte=byte (arraycopy):    " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doublea[k] = 157.0;
            t2 = Timing.timems();
            Out.println("double=157.0:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[k] = doublea[k];
            t2 = Timing.timems();
            Out.println("double=double:            " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                System.arraycopy(doublea, 0, doubleb, 0, NUM);
            t2 = Timing.timems();
            Out.println("double=double (arraycopy):" + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floata[k] = (float)157.0;
            t2 = Timing.timems();
            Out.println("float=157.0:              " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb[k] = floata[k];
            t2 = Timing.timems();
            Out.println("float=float:              " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                System.arraycopy(floata, 0, floatb, 0, NUM);
            t2 = Timing.timems();
            Out.println("float=float (arraycopy):  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            (new BitwiseTest()).test();

            for (int k = 0; k < NUM; k++)
                inta[k] = intb[k] = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[k] += inta[k];
            t2 = Timing.timems();
            Out.println("int+=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                bytea[k] = byteb[k] = 0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    byteb[k] += bytea[k];
            t2 = Timing.timems();
            Out.println("byte+=byte:               " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea[k] = doubleb[k] = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[k] += doublea[k];
            t2 = Timing.timems();
            Out.println("double+=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata[k] = floatb[k] = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb[k] += floata[k];
            t2 = Timing.timems();
            Out.println("float+=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta[k] = intb[k] = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[k] -= inta[k];
            t2 = Timing.timems();
            Out.println("int-=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea[k] = doubleb[k] = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[k] -= doublea[k];
            t2 = Timing.timems();
            Out.println("double-=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata[k] = floatb[k] = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb[k] -= floata[k];
            t2 = Timing.timems();
            Out.println("float-=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[k] = Math.max(inta[k], intb[k]);
            t2 = Timing.timems();
            Out.println("int=max(int,int):         " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[k] = Math.max(doublea[k], doubleb[k]);
            t2 = Timing.timems();
            Out.println("double=max(double,double):" + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    if (inta[k] > intb[k])
                        intb[k] = inta[k];
            t2 = Timing.timems();
            Out.println("if (int>int) int=int:     " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    if (doubleb[k] > doublea[k])
                        doubleb[k] = doubleb[k];
            t2 = Timing.timems();
            Out.println("if (dbl>dbl) dbl=dbl:     " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta[k] = intb[k] = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[k] *= inta[k];
            t2 = Timing.timems();
            Out.println("int*=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea[k] = doubleb[k] = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[k] *= doublea[k];
            t2 = Timing.timems();
            Out.println("double*=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata[k] = floatb[k] = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb[k] *= floata[k];
            t2 = Timing.timems();
            Out.println("float*=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta[k] = intb[k] = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[k] /= inta[k];
            t2 = Timing.timems();
            Out.println("int/=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea[k] = doubleb[k] = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[k] /= doublea[k];
            t2 = Timing.timems();
            Out.println("double/=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata[k] = floatb[k] = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb[k] /= floata[k];
            t2 = Timing.timems();
            Out.println("float/=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    testProcedure(inta[k]);
            t2 = Timing.timems();
            Out.println("void testProcedure(int):  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUMd4; k++)
                    inta[k] = (int)Math.round(doublea[k]);
            t2 = Timing.timems();
            Out.println("int=Math.round(double):   " + (t2 - t1) * MULT / (TOTAL / 4) + UNITS + "/element   \t(" + (t2 -
                t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUMd4; k++)
                    inta[k] = Math.round(floata[k]);
            t2 = Timing.timems();
            Out.println("int=Math.round(float):    " + (t2 - t1) * MULT / (TOTAL / 4) + UNITS + "/element   \t(" + (t2 -
                t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floata[k] = (float)doublea[k];
            t2 = Timing.timems();
            Out.println("float=(float)double:      " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta[k] = (int)floata[k];
            t2 = Timing.timems();
            Out.println("int=(int)float:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta[k] = (int)doublea[k];
            t2 = Timing.timems();
            Out.println("int=(int)double:          " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta[k] = (int)(doublea[k] + 0.5);
            t2 = Timing.timems();
            Out.println("int=(int)(double+0.5):    " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea[k] = 100000.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUMd4; k++)
                    doubleb[k] = Math.sqrt(doublea[k]);
            t2 = Timing.timems();
            Out.println("double=Math.sqrt(double): " + (t2 - t1) * MULT / (TOTAL / 4) + UNITS + "/element   \t(" + (t2 -
                t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUMd4; k++)
                    doubleb[k] = Math.sin(doublea[k]);
            t2 = Timing.timems();
            Out.println("double=Math.sin(double):  " + (t2 - t1) * MULT / (TOTAL / 4) + UNITS + "/element   \t(" + (t2 -
                t1) +
                " ms total)");

            System.gc();
            t1 = Timing.timems();
            for (int k = 0; k < testProcedure65536NUM; k++)
                inta[k] = testProcedure65536(false);
            t2 = Timing.timems();
            Out.println("allocate 256K:            " + (t2 - t1) * MULT / testProcedure65536NUM / 65536 + UNITS +
                "/4 bytes   \t(" + (t2 - t1) + " ms total)");

            t1 = Timing.timems();
            for (int k = 0; k < testProcedure65536NUM; k++)
                inta[k] = testProcedure65536(true);
            t2 = Timing.timems();
            Out.println("allocate+fill 256K:       " + (t2 - t1) * MULT / testProcedure65536NUM / 65536 + UNITS +
                "/4 bytes   \t(" + (t2 - t1) + " ms total)");

            t1 = Timing.timems();
            for (int k = 0; k < testProcedure1024NUM; k++)
                inta[k] = testProcedure1024(false);
            t2 = Timing.timems();
            Out.println("allocate 4K:              " + (t2 - t1) * MULT / testProcedure1024NUM / 1024 + UNITS +
                "/4 bytes   \t(" +
                (t2 - t1) + " ms total)");

            t1 = Timing.timems();
            for (int k = 0; k < testProcedure1024NUM; k++)
                inta[k] = testProcedure1024(true);
            t2 = Timing.timems();
            Out.println("allocate+fill 4K:         " + (t2 - t1) * MULT / testProcedure1024NUM / 1024 + UNITS +
                "/4 bytes   \t(" +
                (t2 - t1) + " ms total)");

            t1 = Timing.timems();
            for (int k = 0; k < testProcedure64NUM; k++)
                inta[k] = testProcedure64(false);
            t2 = Timing.timems();
            Out.println("allocate 512b:            " + (t2 - t1) * MULT / testProcedure64NUM / 64 + UNITS +
                "/4 bytes   \t(" +
                (t2 - t1) + " ms total)");

            t1 = Timing.timems();
            for (int k = 0; k < testProcedure64NUM; k++)
                inta[k] = testProcedure64(true);
            t2 = Timing.timems();
            Out.println("allocate+fill 512b:       " + (t2 - t1) * MULT / testProcedure64NUM / 64 + UNITS +
                "/4 bytes   \t(" +
                (t2 - t1) + " ms total)");

        }
    }

    class Array0Test {
        int[] inta = CommonSpeed.this.inta;
        int[] intb = CommonSpeed.this.intb;
        byte[] bytea = CommonSpeed.this.bytea;
        byte[] byteb = CommonSpeed.this.byteb;
        long[] longa = CommonSpeed.this.longa;
        long[] longb = CommonSpeed.this.longb;
        float[] floata = CommonSpeed.this.floata;
        float[] floatb = CommonSpeed.this.floatb;
        double[] doublea = CommonSpeed.this.doublea;
        double[] doubleb = CommonSpeed.this.doubleb;
        void test() {
            long t1, t2;
            Out.println();
            Out.println("Array operation (fixed index 0):");
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta[0] = 157;
            t2 = Timing.timems();
            Out.println("int=157:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[0] = inta[0];
            t2 = Timing.timems();
            Out.println("int=int:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    bytea[0] = 15;
            t2 = Timing.timems();
            Out.println("byte=15:                  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    byteb[0] = bytea[0];
            t2 = Timing.timems();
            Out.println("byte=byte:                " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doublea[0] = 157.0;
            t2 = Timing.timems();
            Out.println("double=157.0:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[0] = doublea[0];
            t2 = Timing.timems();
            Out.println("double=double:            " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floata[0] = (float)157.0;
            t2 = Timing.timems();
            Out.println("float=157.0:              " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb[0] = floata[0];
            t2 = Timing.timems();
            Out.println("float=float:              " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta[0] = intb[0] = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[0] += inta[0];
            t2 = Timing.timems();
            Out.println("int+=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                bytea[0] = byteb[0] = 0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    byteb[0] += bytea[0];
            t2 = Timing.timems();
            Out.println("byte+=byte:               " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea[0] = doubleb[0] = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[0] += doublea[0];
            t2 = Timing.timems();
            Out.println("double+=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata[0] = floatb[0] = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb[0] += floata[0];
            t2 = Timing.timems();
            Out.println("float+=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta[0] = intb[0] = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[0] -= inta[0];
            t2 = Timing.timems();
            Out.println("int-=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea[0] = doubleb[0] = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[0] -= doublea[0];
            t2 = Timing.timems();
            Out.println("double-=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata[0] = floatb[0] = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb[0] -= floata[0];
            t2 = Timing.timems();
            Out.println("float-=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[0] = Math.max(inta[0], intb[0]);
            t2 = Timing.timems();
            Out.println("int=max(int,int):         " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[0] = Math.max(doublea[0], doubleb[0]);
            t2 = Timing.timems();
            Out.println("double=max(double,double):" + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    if (inta[0] > intb[0])
                        intb[0] = inta[0];
            t2 = Timing.timems();
            Out.println("if (int>int) int=int:     " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    if (doubleb[0] > doublea[0])
                        doubleb[0] = doubleb[0];
            t2 = Timing.timems();
            Out.println("if (dbl>dbl) dbl=dbl:     " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta[0] = intb[0] = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[0] *= inta[0];
            t2 = Timing.timems();
            Out.println("int*=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea[0] = doubleb[0] = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[0] *= doublea[0];
            t2 = Timing.timems();
            Out.println("double*=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata[0] = floatb[0] = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb[0] *= floata[0];
            t2 = Timing.timems();
            Out.println("float*=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                inta[0] = intb[0] = 1;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    intb[0] /= inta[0];
            t2 = Timing.timems();
            Out.println("int/=int:                 " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea[0] = doubleb[0] = 1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[0] /= doublea[0];
            t2 = Timing.timems();
            Out.println("double/=double:           " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                floata[0] = floatb[0] = (float)1.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    floatb[0] /= floata[0];
            t2 = Timing.timems();
            Out.println("float/=float:             " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    testProcedure(inta[0]);
            t2 = Timing.timems();
            Out.println("void testProcedure(int):  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUMd4; k++)
                    inta[0] = (int)Math.round(doublea[0]);
            t2 = Timing.timems();
            Out.println("int=Math.round(double):   " + (t2 - t1) * MULT / (TOTAL / 4) + UNITS + "/element   \t(" + (t2 -
                t1) + " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUMd4; k++)
                    inta[0] = Math.round(floata[0]);
            t2 = Timing.timems();
            Out.println("int=Math.round(float):    " + (t2 - t1) * MULT / (TOTAL / 4) + UNITS + "/element   \t(" + (t2 -
                t1) + " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta[0] = (int)doublea[0];
            t2 = Timing.timems();
            Out.println("int=(int)double:          " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    inta[0] = (int)(doublea[0] + 0.5);
            t2 = Timing.timems();
            Out.println("int=(int)(double+0.5):    " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            for (int k = 0; k < NUM; k++)
                doublea[0] = 100000.0;
            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[0] = Math.sqrt(doublea[0]);
            t2 = Timing.timems();
            Out.println("double=Math.sqrt(double): " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");

            t1 = Timing.timems();
            for (int n = 0; n < NUMe; n++)
                for (int k = 0; k < NUM; k++)
                    doubleb[0] = Math.sin(doublea[0]);
            t2 = Timing.timems();
            Out.println("double=Math.sin(double):  " + (t2 - t1) * MULT / TOTAL + UNITS + "/element   \t(" + (t2 - t1) +
                " ms total)");
        }
    }

    public void exec() {
        Out.print("Garbage collection...");
        System.gc();
        Out.println();
        Out.println(memInfo());

        (new ArrayTest()).test();
        (new Array0Test()).test();
        (new FieldTest()).test();
        (new FieldOtherClassTest()).test();

        Out.println("O'k");
    }

    public static void main(String[] args) {
        (args.length == 0 ? new CommonSpeed() : new CommonSpeed(Integer.parseInt(args[0]))).exec();
    }
}
