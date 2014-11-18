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

package net.algart.lib.tests;

class SomeClass {
    int intValue;
    void pause() {
        for (int k = 0; k < 10; k++) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {}
            System.out.println(intValue++);
        }
    }
}

public class SynchronizedAccess {

    public static void main(String[] args) {
        final SomeClass someClass = new SomeClass();
        new Thread() {
            public void run() {
                someClass.pause();
            }
        }.start();
        try {
            Thread.sleep(1000);
        } catch (Exception e) {}
        synchronized (someClass) { // Both "synchronized" are necessary!
            for (int k = 0; k < 4; k++) {
                try {
                    Thread.sleep(1500);
                } catch (Exception e) {}
                System.out.println(" " + k + ": we see " + someClass.intValue);
            }
        }
    }
}
