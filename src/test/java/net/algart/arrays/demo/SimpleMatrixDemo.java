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

import java.io.File;
import java.io.IOException;
import java.nio.ByteOrder;
import java.util.Map;

/**
 * <p>Simple demo of using {@link Matrix matrices}.</p>
 *
 * @author Daniel Alievsky
 */
public class SimpleMatrixDemo {
//    private abstract static class MyIntArray extends AbstractIntArray implements IntArray {
//        protected MyIntArray(MemoryModel mm, long initialCapacity, long initialLength) {
//            super(mm, initialCapacity, initialLength, true);
//        }
//    }

    private static long mySum(Matrix<? extends PArray> m) {
        PArray a = m.array();
        long sum = 0;
        for (long k = 0, n = a.length(); k < n; k++)
            sum += (int) a.getDouble(k);
        return sum;
    }

    public static void main(String[] args) throws IOException, IllegalInfoSyntaxException {
        int startArgIndex = 0;
        boolean tile = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-tile")) {
            tile = true;
            startArgIndex++;
        }
        if (args.length < startArgIndex + 2) {
            System.out.println("Usage:");
            System.out.println("    " + SimpleMatrixDemo.class.getName()
                + " [-tile] newMatrixFile value bit|byte|short|int N [version [additionalProperty]]");
            System.out.println("to create matrix Nx(N+1)x(N+2) or");
            System.out.println("    " + SimpleMatrixDemo.class.getName() + " oldMatrixFile value ");
            return;
        }

        final boolean fromFile = args.length < startArgIndex + 4;
        final File file = new File(args[startArgIndex]);
        final int value = Integer.parseInt(args[startArgIndex + 1]);
        Matrix<? extends UpdatablePArray> m;
        final LargeMemoryModel<File> lmm;
        final Map<String, String> additionalProperties;
        final String version;
        if (fromFile) {
            lmm = LargeMemoryModel.getInstance();
            System.out.println(lmm);
            System.out.println();
            ByteArray ba = lmm.asByteArray(file, 0,
                Math.min(file.length(), MatrixInfo.MAX_SERIALIZED_MATRIX_INFO_LENGTH),
                ByteOrder.nativeOrder());
            MatrixInfo mi = MatrixInfo.valueOf(Arrays.toJavaArray(ba));
            System.out.println(lmm.asMatrix(file, mi));
            System.out.println("    - read-only version");
            m = lmm.asUpdatableMatrix(file, mi);
            additionalProperties = mi.additionalProperties();
            version = mi.version();
        } else {
            DataFileModel<File> dfm;
            if (LargeMemoryModel.getInstance().getDataFileModel() instanceof DefaultDataFileModel) {
                dfm = new DefaultDataFileModel(file, MatrixInfo.MAX_SERIALIZED_MATRIX_INFO_LENGTH,
                    DefaultDataFileModel.defaultLazyWriting());
            } else {
                dfm = new StandardIODataFileModel(file, MatrixInfo.MAX_SERIALIZED_MATRIX_INFO_LENGTH,
                    true, StandardIODataFileModel.defaultDirectBuffers());
            }
            lmm = LargeMemoryModel.getInstance(dfm);
            System.out.println(lmm);
            System.out.println();
            Class<?> elementType = switch (args[startArgIndex + 2]) {
                case "bit" -> boolean.class;
                case "byte" -> byte.class;
                case "short" -> short.class;
                case "int" -> int.class;
                default -> throw new IllegalArgumentException("Unknown element type");
            };
            long n = Long.parseLong(args[startArgIndex + 3]);
            m = lmm.newMatrix(UpdatablePArray.class, elementType, n, n + 1, n + 2);
            if (tile) {
                m = m.tile();
            }
            version = args.length > startArgIndex + 4 ? args[startArgIndex + 4] : MatrixInfo.DEFAULT_VERSION;
            MatrixInfo mi = !version.equals(MatrixInfo.DEFAULT_VERSION) ?
                MatrixInfo.valueOf(m, MatrixInfo.MAX_SERIALIZED_MATRIX_INFO_LENGTH, version) :
                LargeMemoryModel.getMatrixInfoForSavingInFile(m, MatrixInfo.MAX_SERIALIZED_MATRIX_INFO_LENGTH);
            additionalProperties = mi.additionalProperties();
            if (args.length > startArgIndex + 5) {
                // ((Map)additionalProperties).put(new StringBuilder("additional"), new StringBuilder(args[startArgIndex + 5]));
                // - this variant must lead to ClassCastException
                additionalProperties.put("additional", args[startArgIndex + 5]);
                mi = mi.cloneWithOtherAdditionalProperties(additionalProperties);
            }
            UpdatableByteArray ba = lmm.asUpdatableByteArray(
                lmm.getDataFilePath(LargeMemoryModel.getRawArrayForSavingInFile(m)),
                0, MatrixInfo.MAX_SERIALIZED_MATRIX_INFO_LENGTH, false, ByteOrder.nativeOrder());
            ba.setData(0, mi.toBytes());
            ba.flushResources(null, false);
            LargeMemoryModel.setTemporary(LargeMemoryModel.getRawArrayForSavingInFile(m), false);
        }

        System.out.println(m);
        System.out.println("Additional properties: " + additionalProperties);
        System.out.println("The sum of all elements: " + mySum(m));
        System.out.println("Filling matrix...");

        UpdatablePArray a = m.array();
        long[] coordinates = new long[3];
        long[] foundCoordinates = new long[3];
        for (long z = 0; z < m.dimZ(); z++) {
            coordinates[2] = z;
            for (long y = 0; y < m.dimY(); y++) {
                coordinates[1] = y;
                for (long x = 0; x < m.dimX(); x++) {
                    coordinates[0] = x;
                    long index = m.index(x, y, z);
                    if (index != m.index(new long[]{x, y, z}))
                        throw new AssertionError("Bug found in index(...) method");
                    if (!m.inside(new long[]{x, y, z}))
                        throw new AssertionError("Bug found in inside(...) method");
                    if (!java.util.Arrays.equals(m.coordinates(index, foundCoordinates), coordinates))
                        throw new AssertionError("Bug found in coordinates(...) method: "
                            + JArrays.toString(foundCoordinates, ",", 100) + " instead of "
                            + JArrays.toString(coordinates, ",", 100));
                    a.setInt(index, value + (int) (x + y + z));
                }
            }
        }
        System.out.println("The result matrix has " + m.dimCount() + " dimensions");
        if (m.size() < 2000) {
            Matrix<? extends UpdatablePArray> sm = m.subMatrix(new long[m.dimCount()], m.dimensions()); // must be same
            if (sm.isSubMatrix()) {
                System.out.println("Creating identical submatrix of " + sm.subMatrixParent() + " at "
                    + JArrays.toString(sm.subMatrixFrom(), ",", 100) + ".."
                    + JArrays.toString(sm.subMatrixTo(), ",", 100));
            }
            System.out.println("The result data are:");
            for (long z = 0, disp = 0; z < m.dimZ(); z++) {
                for (long y = 0; y < m.dimY(); y++, disp += m.dimX()) {
                    System.out.println(Arrays.toString(sm.array().subArr(disp, sm.dim(0)), " ", 10000));
                }
            }
            if (!a.equals(sm.array()))
                throw new AssertionError("Bug found in subMatrix(...) method");
        }
        m.flushResources(null); // necessary for actual saving data to disk

        Matrix<PArray> mImm = m.matrix(m.array().asImmutable());
        System.out.println();
        System.out.println("Matrix type():                    " + m.type(PArray.class));
        System.out.println("Matrix updatableType():           " + m.updatableType(PArray.class));
        System.out.println("Immutable matrix type():          " + mImm.type(PArray.class));
        System.out.println("Immutable matrix updatableType(): " + mImm.updatableType(UpdatablePArray.class));
        System.out.println("Immutable matrix toString():");
        System.out.println(mImm.toString());
        // MyIntArray myInt = Arrays.asFuncArray(net.algart.math.functions.Func.IDENTITY, MyIntArray.class, m.array());
        // - BUG AA-158!
        System.out.println("The sum of all elements: " + mySum(mImm));
        System.out.println();
        MatrixInfo mi = MatrixInfo.valueOf(m, MatrixInfo.MAX_SERIALIZED_MATRIX_INFO_LENGTH, version);
        if (!additionalProperties.isEmpty()) {
            // avoiding UnsupportedOperationException if no properties specified
            mi = mi.cloneWithOtherAdditionalProperties(additionalProperties);
        }
        if (!mi.matches(mImm))
            throw new AssertionError("Bug 1 in " + MatrixInfo.class);
        System.out.println("The matrix information:");
        System.out.println("\"" + mi + "\"");
        String chars = mi.toChars();
        System.out.println("The serialized representation of the matrix structure (between <<< and >>>):");
        System.out.println("<<<" + chars + ">>>");
        MatrixInfo mi2 = MatrixInfo.valueOf(chars);
        if (!mi2.equals(mi))
            throw new AssertionError("Bug 2 in " + MatrixInfo.class);
        byte[] bytes2 = mi2.toBytes();
        System.out.println("The serialized representation of the matrix structure (again):");
        System.out.println("<<<" + new String(bytes2) + ">>>");
        MatrixInfo mi3 = mi.cloneWithOtherVersion(MatrixInfo.DEFAULT_VERSION);
        if (!mi3.equals(mi))
            throw new AssertionError("Bug 3 in " + MatrixInfo.class);
        chars = mi3.toChars();
        System.out.println("The serialized representation after conversion to the version "
            + MatrixInfo.DEFAULT_VERSION + ":");
        System.out.println("<<<" + chars + ">>>");
    }
}
