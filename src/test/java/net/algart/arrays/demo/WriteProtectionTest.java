/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2022 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import net.algart.finalizing.Finalizer;
import net.algart.arrays.*;

/**
 * <p>The main 5 ways of protecting AlgART arrays against illegal changes.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
public class WriteProtectionTest {
    private static final int SYNTAX = 0;
    private static final int CLONE = 1;
    private static final int IMMUTABLE = 2;
    private static final int TRUSTED = 3;
    private static final int CNW = 4;
    private static final int TRUSTED_CNW = 5;
    private static final int CNW_TRUSTED = 6;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Usage: " + WriteProtectionTest.class.getName()
                + " " + DemoUtils.possibleArg(true) + " [gc [waitAfterGc]]");
            return;
        }
        System.out.println(Arrays.SystemSettings.globalMemoryModel());
        System.out.println();
        Finalizer fin = new Finalizer();
        fin.setPriority(Thread.MIN_PRIORITY);
        for (int m = 0; m < 7; m++) {
            UpdatableArray aSource = DemoUtils.createTestUnresizableArray(args[0],
                args[0].equals("boolean") ? 80 : 20);
            UpdatableArray aSourceClone = aSource.updatableClone(Arrays.SystemSettings.globalMemoryModel());
            final int identityHashCode = System.identityHashCode(aSource);
            System.out.println("Protection method #" + (m + 1) + ": "
                + (m == SYNTAX ? "SYNTACTICAL ONLY"
                : m == CLONE ? "UPDATABLE CLONE"
                : m == IMMUTABLE ? "IMMUTABLE VIEW"
                : m == TRUSTED ? "TRUSTED IMMUTABLE VIEW"
                : m == CNW ? "COPY-ON-NEXT-WRITE"
                : m == TRUSTED_CNW ? "TRUSTED IMMUTABLE + COPY-ON-NEXT-WRITE"
                : m == CNW_TRUSTED ?
                "COPY-ON-NEXT-WRITE + TRUSTED IMMUTABLE (should be equivalent to the previous method)"
                : "IMPOSSIBLE")
                + " (array @" + Integer.toHexString(identityHashCode) + ")");
            Array aR = m == 0 ? aSource
                : m == CLONE ? aSource.updatableClone(Arrays.SystemSettings.globalMemoryModel())
                : m == IMMUTABLE ? aSource.asImmutable()
                : m == TRUSTED ? aSource.asTrustedImmutable()
                : m == CNW ? aSource.asCopyOnNextWrite()
                : m == TRUSTED_CNW ? aSource.asTrustedImmutable().asCopyOnNextWrite()
                : m == CNW_TRUSTED ? aSource.asCopyOnNextWrite().asTrustedImmutable()
                : null;
            final Array dup = aR.shallowClone();
            // - must be here, not inside the following inner class, to allow deallocation of aR
            fin.invokeOnDeallocation(aR, new Runnable() {
                public void run() {
                    System.out.println("~~~ Checking array @"
                        + Integer.toHexString(identityHashCode) + " while finalization...");
                    long t = System.currentTimeMillis();
                    while (System.currentTimeMillis() - t < 750) ; //emulation of long calculations
                    try {
                        dup.checkUnallowedMutation();
                        System.out.println("~~~ Finalization OK");
                    } catch (UnallowedMutationError ex) {
                        System.out.println("~~~ UnallowedMutationError CAUGHT while finalization:");
                        System.out.println("    \"" + ex.getMessage() + "\"");
                    }
                }
            });
            DemoUtils.showArray("The original array:  ", aSource);
            DemoUtils.showArray("The protected array: ", aR);
            System.out.println("Attempt to get element #3");
            System.out.println("The element #3 is " + aR.getElement(3));

            if (aR instanceof UpdatableArray) {
                System.out.println("It is updatable!");
                System.out.println("Attempt to change element #3");
                DemoUtils.changeTestArray((UpdatableArray) aR, 3, 0);
                DemoUtils.showArray("The changed array:   ", aR);
            }

            if (aR instanceof DirectAccessible) {
                if (((DirectAccessible) aR).hasJavaArray()) {
                    System.out.println("It can be accessible as array");
                    System.out.println("Attempt to quickly read element #4");
                    Object array = ((DirectAccessible) aR).javaArray();
                    if (array instanceof boolean[])
                        System.out.println("The element #4 is " + ((boolean[]) array)[4]);
                    else if (array instanceof char[])
                        System.out.println("The element #4 is " + ((char[]) array)[4]);
                    else if (array instanceof byte[])
                        System.out.println("The element #4 is " + ((byte[]) array)[4]);
                    else if (array instanceof short[])
                        System.out.println("The element #4 is " + ((short[]) array)[4]);
                    else if (array instanceof int[])
                        System.out.println("The element #4 is " + ((int[]) array)[4]);
                    else if (array instanceof long[])
                        System.out.println("The element #4 is " + ((long[]) array)[4]);
                    else if (array instanceof float[])
                        System.out.println("The element #4 is " + ((float[]) array)[4]);
                    else if (array instanceof double[])
                        System.out.println("The element #4 is " + ((double[]) array)[4]);
                    else
                        System.out.println("The element #4 is " + ((Object[]) array)[4]);
                    aR.checkUnallowedMutation();
                    System.out.println("Attempt to quickly write element #4");
                    if (array instanceof boolean[])
                        ((boolean[]) array)[4] = false;
                    else if (array instanceof char[])
                        ((char[]) array)[4] = 0;
                    else if (array instanceof byte[])
                        ((byte[]) array)[4] = (byte) 0;
                    else if (array instanceof short[])
                        ((short[]) array)[4] = (short) 0;
                    else if (array instanceof int[])
                        ((int[]) array)[4] = 0;
                    else if (array instanceof long[])
                        ((long[]) array)[4] = 0;
                    else if (array instanceof float[])
                        ((float[]) array)[4] = 0;
                    else if (array instanceof double[])
                        ((double[]) array)[4] = 0;
                    else
                        ((Object[]) array)[4] = null;
                    DemoUtils.showArray("The changed array: ", aR);
                    try {
                        aR.checkUnallowedMutation();
                        if (m == TRUSTED || m == TRUSTED_CNW || m == CNW_TRUSTED)
                            throw new RuntimeException("Internal error: unallowed array change was not detected!");
                    } catch (UnallowedMutationError ex) {
                        System.out.println("UnallowedMutationError CAUGHT:");
                        System.out.println("    \"" + ex.getMessage() + "\"");
                    }
                    try {
                        dup.checkUnallowedMutation();
                        if (m == TRUSTED)
                            throw new RuntimeException("Internal error: unallowed array change was not detected "
                                + "in a duplicate!");
                        // changes in copy-on-write arrays cannot be caught here:
                        // duplicate will cease to be a view after array() call
                    } catch (UnallowedMutationError ex) {
                        System.out.println("UnallowedMutationError CAUGHT in a duplicate:");
                        System.out.println("    \"" + ex.getMessage() + "\"");
                    }
                } else {
                    System.out.println("It implements DirectAccessible, but cannot be accessible as array");
                }
            } else {
                System.out.println("It doesn't implement DirectAccessible");
            }
            if (!aSource.equals(aSourceClone)) {
                System.out.println("UNFORTUNATELY, the original array was changed");
                DemoUtils.showArray("The original array: ", aSource);
                if (m == CLONE || m == IMMUTABLE || m == CNW || m == TRUSTED_CNW || m == CNW_TRUSTED)
                    throw new RuntimeException("Internal error: array should not be changed here!");
            } else {
                System.out.println("The original array IS NOT MODIFIED!");
                if (m == SYNTAX || (m == TRUSTED && SimpleMemoryModel.isSimpleArray(aSource)
                    && !(aSource instanceof BitArray)))
                    throw new RuntimeException("Internal error: array should be changed here!");
            }
            System.out.println();
            System.out.println();
        }
        System.out.println("Number of finalization tasks: " + fin.activeTasksCount());
        if (args.length >= 2 && args[1].equalsIgnoreCase("gc")) {
            System.out.println("Calling System.gc()");
            System.gc();
//            fin.shutdownNow(); // uncomment this to cancel finalization
            if (args.length >= 3 && args[2].equalsIgnoreCase("waitaftergc")) {
                System.out.println("Waiting for all finalization tasks...");
                while (true) {
                    int count = fin.activeTasksCount();
                    System.out.println("Number of finalization tasks: " + count);
                    if (count == 0)
                        break;
                    try {
                        Thread.sleep(300);
                    } catch (InterruptedException ex) {
                    }
                }
            }
        }
        try {
            Arrays.gcAndAwaitFinalization(1000);
        } catch (InterruptedException ex) {
            // Nothing to do here
        }
    }
}
