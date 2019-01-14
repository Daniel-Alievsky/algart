/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.math.geom;

/**
 * <p>Service class that quickly returns all integer points of Bresenham circles
 * with integer radii. Uses cache for optimization (don't calculate points
 * of not too large circles twice).</p>
 *
 * @author Daniel Alievsky
 * @version 1.0
 * @since JDK 1.0
 */
public class IntegerCirclePointsGenerator {

    private IntegerCirclePointsGenerator() {  }

    public static IntegerCirclePointsGenerator getInstance() {
        return new IntegerCirclePointsGenerator();
    }

    private static final int DEBUG_LEVEL = 0;

    /**
     * Returns all integer points of the circle with given radius <tt>r</tt>
     * and center at (0,0). Points are calculated by Bresenham algorithm.
     * Result array consists of (x,y) pairs: [0] element is x0, [1] element is y0,
     * [2] element is x1, [3] element is y1, ... The number of returned points
     * if <tt>(result_array).length/2</tt>.
     * In a special case <tt>r==0</tt>, returns {0,0}.
     * <p>Warning: the result array <i>must not be modified</i>!
     */
    public int[] getCirclePoints(int r) {
        if (r < 0 || r > 10000000)
            throw new IllegalArgumentException("All radii should be in range 0..10000000");
        if (r == 0)
            return new int[] {0,0}; // do not return 4 identical points
        int[] result = circlePointsReference.getCirclePoints(r,0);
        if (result == null) {
            int[] xyBuff = new int[2 * 4 * ((int)(SQRT2 * r) + 5)]; // 5 is a little gap, really required 1 or 2
            int quarterArrayLen = getCircleQuarter1Points(r, xyBuff);
            int arrayLen = appendCircleQuarters234Points(quarterArrayLen, xyBuff);
            result = new int[arrayLen];
            System.arraycopy(xyBuff, 0, result, 0, arrayLen);
            circlePointsReference.putCirclePoints(r, 0, result);
        }
        return result;
    }

    /**
     * Analog of {@link #getCirclePoints(int)} method, but returned points
     * are shifted by <tt>delta</tt> pixels from (0,0). The number of returned points
     * is the same as in <tt>getCirclePoints</tt> method with the same <tt>r</tt>.
     * In a special case <tt>r==0</tt>, returns {0,0}.
     * <p>Warning: the result array <i>must not be modified</i>!
     */
    public int[] getExternalCirclePoints(int r, int delta) {
        if (r < 0 || r > 10000000)
            throw new IllegalArgumentException("All radii should be in range 0..10000000");
        if (delta < 0)
            throw new IllegalArgumentException("Delta argument should be >=0");
        if (delta == 0)
            return getCirclePoints(r);
        if (r == 0)
            return new int[] {0,0};
        int[] result = circlePointsReference.getCirclePoints(r,delta);
        if (result == null) {
            int[] xyBuff = new int[2*4*((int)(SQRT2*r)+5)]; // 5 is a little gap, really required 1 or 2
            int quarterArrayLen = getCircleQuarter1Points(r,xyBuff);
            int rHalf = r/2;
            for (int disp = 0; disp < quarterArrayLen; disp += 2) {
                int x = xyBuff[disp], y = xyBuff[disp+1];
                xyBuff[disp] = (x * (r+delta) + rHalf) / r;
                xyBuff[disp+1] = (y * (r+delta) + rHalf) / r;
                // That is round((x,y) * (r+delta)/r), because x,y >= 0
                // This point is never equal to x,y if delta > 0
            }
            int arrayLen = appendCircleQuarters234Points(quarterArrayLen,xyBuff);
            result = new int[arrayLen];
            System.arraycopy(xyBuff,0,result,0,arrayLen);
            circlePointsReference.putCirclePoints(r,delta,result);
        }
        return result;
    }

    /**
     * Frees all cache memory. Recommended when ceasing using this class for a long time.
     * <tt>System.gc()</tt> may be called after this to real freeing memory.
     */
    public void dispose() {
        circlePointsReference.dispose();
    }

    private static final int MAX_CACHED_RADIUS = 300; // ~2 MB / every deltaR, maximum ~20 MB
    private static final int MAX_NUMBER_OF_CACHED_DELTAR = 10;

    private static class CirclePointsTable {
        int deltaR;
        int[][] circlePoints = new int[MAX_CACHED_RADIUS+1][];
    }

    private static class CirclePointsReference {
        CirclePointsTable[] tables = new CirclePointsTable[MAX_NUMBER_OF_CACHED_DELTAR];
        int tablesLen = 0;

        synchronized int[] getCirclePoints(int r, int deltaR) {
            if (r > MAX_CACHED_RADIUS)
                return null;
            for (int k = 0; k < tablesLen; k++) {
                if (tables[k].deltaR == deltaR) {
                    CirclePointsTable table = tables[k];
                    System.arraycopy(tables, 0, tables, 1, k);
                    tables[0] = table;
                    if (DEBUG_LEVEL >= 6)
                        System.out.println("Circle points found in cache at position " + k
                            + ": deltaR = " + deltaR + ", r = " + r);
                    return table.circlePoints[r];
                }
            }
            if (DEBUG_LEVEL >= 6)
                System.out.println("Circle points not found in cache: deltaR = " + deltaR + ", r = " + r);
            return null;
        }

        synchronized void putCirclePoints(int r, int deltaR, int[] circlePoints) {
            if (r > MAX_CACHED_RADIUS)
                return;
            for (int k = 0; k < tablesLen; k++) {
                if (tables[k].deltaR == deltaR) {
                    tables[k].circlePoints[r] = circlePoints;
                    if (DEBUG_LEVEL >= 5)
                        System.out.println("Saving circle points in cache at position " + k
                            + ": deltaR = " + deltaR + ", r = " + r);
                    return;
                }
            }
            tablesLen = Math.min(tablesLen + 1, tables.length);
            System.arraycopy(tables, 0, tables, 1, tablesLen - 1);
            tables[0] = new CirclePointsTable();
            tables[0].deltaR = deltaR;
            tables[0].circlePoints[r] = circlePoints;
            if (DEBUG_LEVEL >= 5)
                System.out.println("Adding circle points into cache: deltaR = " + deltaR + ", r = " + r);
        }

        synchronized void dispose() {
            for (int k = 0; k < tablesLen; k++)
                tables[k] = null;
            tablesLen = 0;
        }
    }

    private CirclePointsReference circlePointsReference = new CirclePointsReference();

    private static final double SQRT2 = Math.sqrt(2.0);
    private static int getCircleQuarter1Points(int r, int[] xyBuff) {
        // Bresenham algorithm
        int disp = 0;
        int x = r, y = 0;
        int d = 3-2*r; // 2 * (x^2+y^2-(r+eps)^2)
        while (x >= y) {
            xyBuff[disp++] = x;
            xyBuff[disp++] = y;
            if (d < 0) {
                d += 4*y+6; y++;
            } else {
                d += 4*(y-x)+10; x--; y++;
            }
        }
        int symDisp = disp-2;
        if (x == y) symDisp -= 2;
        for (; symDisp > 0; disp+=2, symDisp-=2) {
            // Here symDisp > 0, not >=0! We should include (r,0) point, but not (0,r)!
            xyBuff[disp] = xyBuff[symDisp + 1];
            xyBuff[disp + 1] = xyBuff[symDisp];
        }
        return disp;
    }

    private static int appendCircleQuarters234Points(int quarterArrayLen, int[] xyBuff) {
        for (int k = 0, disp = quarterArrayLen; k < quarterArrayLen; k+=2,disp+=2) {
            xyBuff[disp] = -xyBuff[k+1];    // x' = -y
            xyBuff[disp+1] = xyBuff[k];     // y' = x
        }
        for (int k = 0, disp = 2*quarterArrayLen; k < quarterArrayLen; k+=2,disp+=2) {
            xyBuff[disp] = -xyBuff[k];      // x' = -x
            xyBuff[disp+1] = -xyBuff[k+1];  // y' = -y
        }
        for (int k = 0, disp = 3*quarterArrayLen; k < quarterArrayLen; k+=2,disp+=2) {
            xyBuff[disp] = xyBuff[k+1];     // x' = y
            xyBuff[disp+1] = -xyBuff[k];    // y' = -x
        }
        return 4*quarterArrayLen;
    }

    private static boolean isTrueStaticFlag;
}
