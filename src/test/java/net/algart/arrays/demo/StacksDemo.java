/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.arrays.*;

import java.util.Locale;

/**
 * <p>Simple test for Stack interface</p>
 *
 * @author Daniel Alievsky
 */
public class StacksDemo {
    private static final MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();

    public static void main(String[] args) {
        if (args.length < 2) {
            System.out.println("Usage: " + StacksDemo.class.getName() + " int|CombinedSingle stackLength");
            return;
        }
        int n = Integer.parseInt(args[1]);
        Stack stack1 = args[0].equals("int") ? Arrays.SystemSettings.globalMemoryModel().newEmptyIntArray() :
            CombinedMemoryModel.getInstance(new CombinedArraysDemo.MyCombinerSingle(mm)).newEmptyArray(
                CombinedArraysDemo.Circle.class);
        Stack stack2 = ((MutableArray) stack1).mutableClone(Arrays.SystemSettings.globalMemoryModel());
        Stack clone1, clone2;
        for (int count = 1; count <= 5; count++) {
            System.out.println("*** ITERATION #" + count);
            long t1, t2, t3, t4, t5, t6;
            if (stack1 instanceof IntStack) {
                IntStack is1 = (IntStack) stack1;
                IntStack is2 = (IntStack) stack2;
                t1 = System.nanoTime();
                for (int k = 0; k < n; k++)
                    is1.pushInt(k);
                t2 = System.nanoTime();
                clone1 = ((MutableArray) stack1).mutableClone(Arrays.SystemSettings.globalMemoryModel());
                t3 = System.nanoTime();
                for (int k = 0; k < n; k++)
                    is2.pushInt(is1.popInt());
                t4 = System.nanoTime();
                clone2 = ((MutableArray) stack2).mutableClone(Arrays.SystemSettings.globalMemoryModel());
                t5 = System.nanoTime();
                for (int k = 0; k < n; k++)
                    is2.popInt();
                t6 = System.nanoTime();
            } else {
                t1 = System.nanoTime();
                for (int k = 0; k < n; k++)
                    stack1.pushElement(new CombinedArraysDemo.Circle(-k, -k, k));
                t2 = System.nanoTime();
                clone1 = ((MutableArray) stack1).mutableClone(Arrays.SystemSettings.globalMemoryModel());
                t3 = System.nanoTime();
                for (int k = 0; k < n; k++)
                    stack2.pushElement(stack1.popElement());
                t4 = System.nanoTime();
                clone2 = ((MutableArray) stack2).mutableClone(Arrays.SystemSettings.globalMemoryModel());
                t5 = System.nanoTime();
                for (int k = 0; k < n; k++)
                    stack2.popElement();
                t6 = System.nanoTime();
            }
            System.out.printf(Locale.US, "stack1.pushInt:                 %d ns, %.2f ns/element%n",
                t2 - t1, (t2 - t1) * 1.0 / n);
            System.out.printf(Locale.US, "stack1.clone:                   %d ns, %.2f ns/element%n",
                t3 - t2, (t3 - t2) * 1.0 / n);
            System.out.printf(Locale.US, "stack2.pushInt(stack1.popInt):  %d ns, %.2f ns/element%n",
                t4 - t3, (t4 - t3) * 1.0 / n);
            System.out.printf(Locale.US, "stack2.clone:                   %d ns, %.2f ns/element%n",
                t5 - t4, (t5 - t4) * 1.0 / n);
            System.out.printf(Locale.US, "stack2.popInt:                  %d ns, %.2f ns/element%n",
                t6 - t5, (t6 - t5) * 1.0 / n);
            System.out.println("clone1: " + clone1);
            if (clone1 instanceof Array)
                System.out.println("    " + Arrays.toString((Array) clone1, "; ", 100));
            System.out.println("clone2: " + clone2);
            if (clone2 instanceof Array)
                System.out.println("    " + Arrays.toString((Array) clone2, "; ", 100));
            System.out.println("stack1: " + stack1);
            System.out.println("stack2: " + stack2);
            System.out.println();
        }
    }
}
