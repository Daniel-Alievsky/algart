/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2013 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 * <p>Simple test for AlgART array clones and views.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class ClonesAndViewsTest {
    public static void mainImpl(String[] args) {
        DemoUtils.messageNamePadding = 44;
        MutableArray a = DemoUtils.createTestArray(args[0], 30);
        MutableArray aOrig = a;
        UpdatableArray aSub = null;
        for (int m = 0; m < 3; m++) {
            System.out.println("Step " + (m + 1));
            if (m == 1) a = aSub.mutableClone(Arrays.SystemSettings.globalMemoryModel());
            if (m == 2) a = aOrig;
            DemoUtils.showArray("a:", a);
            if (m == 0) {
                a.ensureCapacity(35);
                DemoUtils.showArray("a after ensureCapacity(35):", a);
            }
            String aStorageRef = DemoUtils.extractInternalStorageRef(a);
            MutableArray aDup = a.shallowClone();
            DemoUtils.showArray("shallow clone:", aDup);
            if (!DemoUtils.extractInternalStorageRef(aDup).equals(aStorageRef))
                throw new AssertionError("Internal error: shallow clone has another storage");
            MutableArray aCl = a.mutableClone(Arrays.SMM);
            DemoUtils.showArray("mutable clone:", aCl);
            if (DemoUtils.extractInternalStorageRef(aCl).equals(aStorageRef))
                throw new AssertionError("Internal error: clone has the same storage");
            if (m == 0) {
                a.length(40);
                DemoUtils.showArray("a after length(40):", a);
                aStorageRef = DemoUtils.extractInternalStorageRef(a);
            }
            UpdatableArray aUnr = a.asUnresizable();
            DemoUtils.showArray("unres:", aUnr);
            if (!DemoUtils.extractInternalStorageRef(aUnr).equals(aStorageRef))
                throw new AssertionError("Internal error: asUnresizable has another storage");
            aSub = a.subArray(10, a.length());
            DemoUtils.showArray("aSub (10..a.length):", aSub);
            if (!DemoUtils.extractInternalStorageRef(aSub).equals(aStorageRef))
                throw new AssertionError("Internal error: subarray has another storage");
            MutableArray aCNW = a.asCopyOnNextWrite();
            DemoUtils.showArray("a copy-on-next-write:", aCNW);
            DemoUtils.showArray("a copy-on-next-write updatable clone:",
                aCNW.updatableClone(Arrays.SystemSettings.globalMemoryModel()));
            if (!DemoUtils.extractInternalStorageRef(aCNW).equals(aStorageRef))
                throw new AssertionError("Internal error: copy-on-next-write has another storage");
            UpdatableArray aUnrCNW = aUnr.asCopyOnNextWrite();
            if (!DemoUtils.extractInternalStorageRef(aUnrCNW).equals(aStorageRef))
                throw new AssertionError("Internal error: unres copy-on-next-write has another storage");
            UpdatableArray aSubCNW = a.subArray(10, a.length()).asCopyOnNextWrite();
            if (!DemoUtils.extractInternalStorageRef(aSubCNW).equals(aStorageRef))
                throw new AssertionError("Internal error: subarray copy-on-next-write has another storage");
            DemoUtils.changeTestArray(a, 2, 22);
            DemoUtils.changeTestArray(aUnr, 13, 83);
            DemoUtils.changeTestArray(aCNW, 2, 82);
            DemoUtils.changeTestArray(aUnrCNW, 14, 84);
            DemoUtils.changeTestArray(aSubCNW, 1, 111);
            DemoUtils.showArray("a after [2]=13:", a);
            DemoUtils.showArray("unres after [13]=83:", aUnr);
            DemoUtils.showArray("copy-on-next-write after [2]=82:", aCNW);
            DemoUtils.showArray("unres copy-on-next-write after [14]=84:", aUnrCNW);
            DemoUtils.showArray("aSub copy-on-next-write after [1]=111:", aSubCNW);
            if (DemoUtils.extractInternalStorageRef(aCNW).equals(aStorageRef))
                throw new AssertionError("Internal error: copy-on-next-write hasn't reallocate storage");
            if (DemoUtils.extractInternalStorageRef(aUnrCNW).equals(aStorageRef))
                throw new AssertionError("Internal error: unres copy-on-next-write hasn't reallocate storage");
            if (DemoUtils.extractInternalStorageRef(aSubCNW).equals(aStorageRef))
                throw new AssertionError("Internal error: subarray copy-on-next-write hasn't reallocate storage");
            Array aImm = a.asImmutable();
            if (!DemoUtils.extractInternalStorageRef(aImm).equals(aStorageRef))
                throw new AssertionError("Internal error: immutable has another storage");
            Array aUnrImm = aUnr.asImmutable();
            if (!DemoUtils.extractInternalStorageRef(aUnrImm).equals(aStorageRef))
                throw new AssertionError("Internal error: unres immutable has another storage");
            MutableArray aMut = aImm.mutableClone(Arrays.SystemSettings.globalMemoryModel());
            if (DemoUtils.extractInternalStorageRef(aMut).equals(aStorageRef))
                throw new AssertionError("Internal error: mutableClone() has the same storage");
            DemoUtils.changeTestArray(aMut, 12, 122);
            aMut.length(15); // only aMut will be changed
            DemoUtils.showArray("a (changed):", a);
            DemoUtils.showArray("shallow clone (maybe unchanged):", aDup);
            DemoUtils.showArray("clone (unchanged):", aCl);
            DemoUtils.showArray("unres:", aUnr);
            DemoUtils.showArray("imm:", aImm);
            DemoUtils.showArray("unres+imm:", aUnrImm);
            DemoUtils.showArray("imm -> mutable:", aMut);
            DemoUtils.showArray("unres mutable clone:", aUnr.mutableClone(Arrays.SystemSettings.globalMemoryModel()));
            if (aImm.asCopyOnNextWrite() == aImm)
                System.out.println("imm asCopyOnNextWrite == imm");
            else
                throw new AssertionError("Internal error: imm asCopyOnNextWrite != imm");
            if (aUnr.shallowClone() != aUnr)
                System.out.println("unres shallow clone != unres");
            if (aImm.shallowClone() == aImm)
                System.out.println("imm shallow clone == imm");
            DemoUtils.showArray("aSub:                      ", aSub);
            System.out.println();
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: " + ClonesAndViewsTest.class.getName() + " " + DemoUtils.possibleArg(false));
            return;
        }
        mainImpl(args); // separate method allows to finalize all objects
        DemoUtils.fullGC();
    }
}
