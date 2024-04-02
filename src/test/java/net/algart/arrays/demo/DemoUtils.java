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

import java.io.*;
import java.util.Locale;
import java.util.Set;

class DemoUtils {
    final static long tStart = System.currentTimeMillis();
    private static long tFix = tStart;

    private static final File TEMP_LIST = new File(System.getProperty("java.io.tmpdir"), "arrayfiles.txt");

    static boolean freeResourcesBeforeFilling = true;

    static long tCleanupStart;

    static {
        deleteSavedTemporaryFiles();
        Arrays.addShutdownTask(new Runnable() {
            public void run() {
                System.out.println();
                System.out.println(" *** Cleanup procedure started... *** ");
                tCleanupStart = System.currentTimeMillis();
            }
        }, Arrays.TaskExecutionOrder.BEFORE_STANDARD);
        Arrays.addShutdownTask(new Runnable() {
            public void run() {
                System.out.println(" *** Cleanup procedure successfully performed in "
                    + (System.currentTimeMillis() - tCleanupStart) + " ms *** ");
                saveTemporaryFilesList();
            }
        }, Arrays.TaskExecutionOrder.AFTER_STANDARD);
    }

    static void initializeClass() {
    }

    static void showProgress(int testIndex, int numberOfTests) {
        long t = System.currentTimeMillis();
        if (t - tFix > 500 || testIndex >= numberOfTests - 1) {
            tFix = t;
            System.out.print("\r" + testIndex + (testIndex >= numberOfTests - 1 ? " \r          \r" : " "));
        }
    }

    static String[] allElementTypes = {
        "boolean",
        "char",
        "byte",
        "short",
        "int",
        "long",
        "float",
        "double",
        "String",
        "CombinedMulti",
        "CombinedSingle",
        "CombinedPacked",
    };

    static String possibleArg(boolean forUnresizableOnly) {
        return "primitiveType|String|Combined{Multi|Single" + (forUnresizableOnly ? "|Packed}" : "}");
    }

    static String extractInternalStorageRef(Array a) {
        if (CombinedMemoryModel.isCombinedArray(a)) {
            String result = "";
            String[] stor = CombinedMemoryModel.getStorageToStrings(a);
            for (String s : stor) {
                result += extractInternalStorageRef(s);
            }
            return result;
        }
        return extractInternalStorageRef(a.toString());
    }

    static String extractInternalStorageRef(String s) {
        int p = s.indexOf("@");
        int q = p + 1;
        if (q < s.length() && s.charAt(q) == '<') {
            q = s.indexOf(">", q + 1);
            if (q == -1)
                throw new AssertionError("Unknown toString format");
            q++;
        } else {
            while (q < s.length() && Character.isLetterOrDigit(s.charAt(q)))
                q++;
        }
        return s.substring(p + 1, q);
    }

    static MutableArray createTestArray(String elementTypeName, long len) {
        return (MutableArray) createTestArray(elementTypeName, len, false, true);
    }

    static UpdatableArray createTestUnresizableArray(String elementTypeName, long len) {
        return createTestArray(elementTypeName, len, true, true);
    }

    static UpdatableArray createTestResizableIfPossibleArray(String elementTypeName, long len) {
        return createTestArray(elementTypeName, len, elementTypeName.indexOf("Packed") != -1, true);
    }

    private static MemoryModel cmmcm = null, cmmcp = null, cmmcs = null;

    static MemoryModel memoryModel(String elementTypeName) {
        MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
        if (elementTypeName.startsWith("Combined")) {
            if (elementTypeName.equals("CombinedMulti")) {
                return cmmcm != null ? cmmcm :
                    (cmmcm = CombinedMemoryModel.getInstance(new CombinedArraysDemo.MyCombinerMulti(mm)));
                // returning the same instance is useful for testing newLazyCopy
            } else if (elementTypeName.equals("CombinedPacked")) {
                return cmmcp != null ? cmmcp :
                    (cmmcp = CombinedMemoryModel.getInstance(new CombinedArraysDemo.MyCombinerPacked(mm)));
            } else {
                return cmmcs != null ? cmmcs :
                    (cmmcs = CombinedMemoryModel.getInstance(new CombinedArraysDemo.MyCombinerSingle(mm)));
            }
        }
        return mm;
    }

    static UpdatableArray createTestArray(String elementTypeName, long len, boolean unresizable, boolean fill) {
        MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
        Class<?> clazz = elementType(elementTypeName);
        UpdatableArray a;
        MutableArray ma = null;
        if (elementTypeName.startsWith("Combined")) {
            MemoryModel cmm = memoryModel(elementTypeName);
            if (unresizable) {
                a = cmm.newUnresizableArray(CombinedArraysDemo.Circle.class, len);
            } else {
                a = ma = cmm.newEmptyArray(CombinedArraysDemo.Circle.class);
            }
            CombinedArraysDemo.Circle circle = new CombinedArraysDemo.Circle();
            if (unresizable) {
                if (fill) {
                    for (int k = 0; k < len; k++) {
                        circle.x = 2 * k;
                        circle.y = 2 * k;
                        circle.r = k - 5;
                        a.setElement(k, circle);
                    }
                }
            } else {
                if (fill) {
                    for (int k = 0; k < len; k++) {
                        circle.x = 2 * k;
                        circle.y = 2 * k;
                        circle.r = k - 5;
                        ma.pushElement(circle);
                    }
                } else {
                    ma.length(len);
                }
            }
        } else {
            if (unresizable) {
                a = mm.newUnresizableArray(clazz, len);
            } else {
                a = ma = mm.newEmptyArray(clazz);
            }
            if (!unresizable)
                ma.length(len);
            if (a instanceof UpdatablePArray) {
                if (fill) {
                    a.freeResources(null); // for testing empty banks
                    int blockLen = (int) Math.min(len, 1000000);
                    if (a instanceof BitArray) {
                        for (int k = 0; k < blockLen; k++)
                            ((UpdatableBitArray) a).setBit(k, (k + 1) % 3 == 1);
                    } else {
                        for (int k = 0; k < blockLen; k++)
                            ((UpdatablePArray) a).setLong(k, k - 5);
                    }
                    for (long k = blockLen; k < len; k += blockLen) {
                        a.copy(k, 0, Math.min(blockLen, len - k));
                    }
                }
            } else {
                UpdatableObjectArray<String> oa = ((UpdatableObjectArray<?>) a).cast(String.class);
                if (fill) {
                    for (int k = 0; k < len; k++)
                        oa.set(k, (k - 5) + "s");
                } else {
                    if (freeResourcesBeforeFilling)
                        oa.freeResources(null); // for testing empty banks
                    for (int k = 0; k < len; k++)
                        oa.set(k, "");
                }
            }
        }
        return a;
    }

    static Class<?> elementType(String elementTypeName) {
        Class<?> clazz = elementTypeName.equals("boolean") ? boolean.class
            : elementTypeName.equals("char") ? char.class
            : elementTypeName.equals("byte") ? byte.class
            : elementTypeName.equals("short") ? short.class
            : elementTypeName.equals("int") ? int.class
            : elementTypeName.equals("long") ? long.class
            : elementTypeName.equals("float") ? float.class
            : elementTypeName.equals("double") ? double.class
            : elementTypeName.equals("String") ? String.class
            : elementTypeName.equals("CombinedMulti") ||
            elementTypeName.equals("CombinedSingle") ||
            elementTypeName.equals("CombinedPacked") ? CombinedArraysDemo.Circle.class
            : null;
        if (clazz == null)
            throw new IllegalArgumentException("Unknown element type: " + elementTypeName);
        return clazz;
    }

    static void changeTestArray(UpdatableArray a, long index, int value) {
        if (a instanceof UpdatablePArray) {
            ((UpdatablePArray) a).setLong(index, value);
        } else if (a.elementType() == CombinedArraysDemo.Circle.class) {
            CombinedArraysDemo.Circle circ = (CombinedArraysDemo.Circle) a.getElement(index);
            circ.r = value;
            a.setElement(index, circ);
        } else {
            a.setElement(index, String.format(Locale.US, "%010d", value));
        }
    }

    static int messageNamePadding = 0;

    static void showArray(String name, Array a) {
        if (messageNamePadding > 0)
            name = String.format(Locale.US, "%-" + messageNamePadding + "s", name + ' ');
        long qp;
        System.out.println(name
            + (a instanceof BitArray ?
            Arrays.toHexString(a, "", 1000) + ", nearest 4 quick positions "
                + (qp = ((BitArray) a).nextQuickPosition(0)) + ", "
                + (qp = qp == -1 ? -1 : ((BitArray) a).nextQuickPosition(qp + 1)) + ", "
                + (qp = qp == -1 ? -1 : ((BitArray) a).nextQuickPosition(qp + 1)) + ", "
                + (qp == -1 ? -1 : ((BitArray) a).nextQuickPosition(qp + 1)) :
            a instanceof CharArray ?
                Arrays.toHexString(a, "; ", 1000) :
                a instanceof PFloatingArray ?
                    Arrays.toString(a, Locale.US, "%.0f", "; ", 1000) :
                    Arrays.toString(a, "; ", 1000)));
        if (CombinedMemoryModel.isCombinedArray(a))
            System.out.println("      (" + Arrays.toString(SimpleMemoryModel.asUpdatableObjectArray(
                CombinedMemoryModel.getStorageToStrings(a)), "; ", 1000) + ")");

        System.out.println("    - " + a
            + (a.isUnresizable() ? " <<unresizable>>" : "")
            + (a.isImmutable() ? " <<immutable>>" : ""));
        System.out.println("    - " + a.getClass() + " @" + Integer.toHexString(System.identityHashCode(a)));
    }

    private static void saveTemporaryFilesList() {
        try {
            Set<DataFileModel<?>> models = LargeMemoryModel.allUsedDataFileModelsWithAutoDeletion();
            BufferedWriter w = new BufferedWriter(new FileWriter(TEMP_LIST));
            long count = 0;
            for (DataFileModel<?> m : models) {
                Set<DataFile> files = m.allTemporaryFiles();
                for (DataFile df : files) {
                    w.write(m.getPath(df).toString());
                    w.newLine();
                    count++;
                }
            }
            w.close();
            if (count > 0) {
                System.out.printf(Locale.US, "The list of " + count
                    + " non-deleted temporary files is saved in " + TEMP_LIST + "%n%n");
            }
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    private static void deleteSavedTemporaryFiles() {
        if (!TEMP_LIST.exists())
            return;
        try {
            BufferedReader r = new BufferedReader(new FileReader(TEMP_LIST));
            for (; ; ) {
                String s = r.readLine();
                if (s == null) {
                    break;
                }
                File f = new File(s);
                if (f.exists() && !f.delete()) {
                    System.err.println("Cannot remove " + s);
                }
            }
            r.close();
        } catch (IOException ex) {
            System.err.println(ex);
        }
    }

    static void fullGC() {
        long t1 = System.nanoTime();
        boolean ok = false;
        for (int k = 1; k <= 10; k++) {
            int count = LargeMemoryModel.activeFinalizationTasksCount();
            if (count == 0) {
                ok = true;
                break;
            }
            int mapCount = LargeMemoryModel.activeMappingFinalizationTasksCount();
            System.out.print("\rWaiting for finalization: "
                + count + " tasks = "
                + (count - mapCount) + " storages + " + mapCount + " mappings, "
                + LargeMemoryModel.activeArrayFinalizationTasksCount()
                + " non-finalized AlgART arrays (attempt #" + k + ")...             ");
            try {
                Arrays.gcAndAwaitFinalization(2000);
            } catch (InterruptedException e) {
                System.err.println("Unexpected interruption: " + e);
            }
        }
        if (ok) {
            long t2 = System.nanoTime();
            System.out.printf(Locale.US, "\rFinalization complete in %.3f seconds %80s%n", (t2 - t1) * 1e-9, "");
        } else {
            System.out.println();
            System.out.println("LargeMemoryModel finalization failed");
        }
    }
}
