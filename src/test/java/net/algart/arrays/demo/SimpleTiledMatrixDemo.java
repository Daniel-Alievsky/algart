/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.arrays.demo;

import net.algart.arrays.Arrays;
import net.algart.arrays.Matrix;
import net.algart.arrays.NotTiledMatrixException;
import net.algart.arrays.UpdatablePArray;

public class SimpleTiledMatrixDemo {
    public static void main(String[] args) {
        Matrix<? extends UpdatablePArray> m = Arrays.SMM.newByteMatrix(1024, 1024);
        Matrix<? extends UpdatablePArray> tiled = m.tile(100, 100);
        System.out.println(m);
        System.out.println(tiled);
        if (!tiled.isTiled()) throw new AssertionError();
        if (m.isTiled()) throw new AssertionError();
        if (tiled.tileParent() != m) throw new AssertionError();
        if (!java.util.Arrays.equals(tiled.tileDimensions(), new long[]{100, 100})) throw new AssertionError();
        try {
            m.tileParent();
            throw new AssertionError();
        } catch (NotTiledMatrixException e) {
            System.out.println(e);
        }
        try {
            m.tileDimensions();
            throw new AssertionError();
        } catch (NotTiledMatrixException e) {
            System.out.println(e);
        }
    }
}
