/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.matrices.spectra;

class RootsOfUnity {
    public static final int LOG_CACHE_SIZE = 20;
    public static final int HALF_CACHE_SIZE = 1 << (LOG_CACHE_SIZE - 1);
    public static final int CACHE_SIZE = 2 * HALF_CACHE_SIZE;

    public static final double[] SINE_CACHE = new double[HALF_CACHE_SIZE + 1];
    public static final double[] LOGARITHMICAL_SINE_CACHE = new double[65];

    static {
        SINE_CACHE[0] = 0.0;
        for (int k = 1; k < HALF_CACHE_SIZE; k++) {
            SINE_CACHE[k] = StrictMath.sin(0.5 * StrictMath.PI * k / HALF_CACHE_SIZE);
        }
        SINE_CACHE[HALF_CACHE_SIZE] = 1.0;
    }

    static {
        LOGARITHMICAL_SINE_CACHE[0] = 0.0; // sin(PI)
        LOGARITHMICAL_SINE_CACHE[1] = 1.0; // sin(PI/2)
        double angle = 0.25 * StrictMath.PI;
        for (int log = 2; log <= 64; log++) {
            LOGARITHMICAL_SINE_CACHE[log] = StrictMath.sin(angle);
            angle *= 0.5;
        }
    }


    /**
     * Returns sin &phi;, &phi; = &pi;<code>*angleIndex/{@link #CACHE_SIZE}</code>.
     * The <code>angleIndex</code> must be in range <code>0&lt;=angleIndex&lt;={@link #CACHE_SIZE}</code>,
     * so 0&le;&phi;&le;&pi;.
     *
     * @param angleIndex the angle &phi;, measured in &pi;/{@link #CACHE_SIZE} units
     *                   (&phi; = &pi;<code>*angleIndex/{@link #CACHE_SIZE}</code>).
     * @return sin &phi;.
     * @throws IndexOutOfBoundsException if <code>angleIndex</code> is not
     *                                   in <code>0..{@link #CACHE_SIZE}</code> range.
     */
    public static double quickSin(int angleIndex) {
        return angleIndex < HALF_CACHE_SIZE ?
                SINE_CACHE[angleIndex] :
                SINE_CACHE[2 * HALF_CACHE_SIZE - angleIndex];
    }

    /**
     * Returns cos &phi;, &phi; = &pi;<code>*angleIndex/{@link #CACHE_SIZE}</code>.
     * The <code>angleIndex</code> must be in range <code>0&lt;=angleIndex&lt;={@link #CACHE_SIZE}</code>,
     * so 0&le;&phi;&le;&pi;.
     *
     * @param angleIndex the angle &phi;, measured in &pi;/{@link #CACHE_SIZE} units
     *                   (&phi; = &pi;<code>*angleIndex/{@link #CACHE_SIZE}</code>).
     * @return cos &phi;.
     * @throws IndexOutOfBoundsException if <code>angleIndex</code> is not
     *                                   in <code>0..{@link #CACHE_SIZE}</code> range.
     */
    public static double quickCos(int angleIndex) {
        return angleIndex < HALF_CACHE_SIZE ?
                SINE_CACHE[HALF_CACHE_SIZE - angleIndex] :
                -SINE_CACHE[angleIndex - HALF_CACHE_SIZE];
    }
}
