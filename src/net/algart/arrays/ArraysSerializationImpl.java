/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2014 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.arrays;

/**
 * <p>Implementations of {@link Arrays#copyArrayToBytes(byte[], PArray)} and
 * {@link Arrays#copyBytesToArray(UpdatablePArray, byte[])} methods.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2014</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
class ArraysSerializationImpl {

    public static byte[] copyArrayToBytes(byte[] bytes, PArray array) {
        if (array == null)
            throw new NullPointerException("Null array");
        final long requiredLength = Arrays.copyArrayRequiredNumberOfBytes(array);
        if (bytes == null) {
            bytes = new byte[(int) requiredLength];
        } else {
            if (bytes.length < requiredLength)
                throw new IndexOutOfBoundsException("Not enough space to copy the AlgART array into byte[] array: "
                    + requiredLength + " bytes required, but only " + bytes.length + " available");
        }
        if (array instanceof ByteArray) {
            assert requiredLength == array.length();
            array.getData(0, bytes, 0, (int) requiredLength);
        } else if (array instanceof BitArray) {
            BitArray a = (BitArray) array;
            final long packedLength = PackedBitArrays.packedLength(a.length());
            assert packedLength <= Integer.MAX_VALUE; // because requiredLength <= Integer.MAX_VALUE
            assert packedLength * 8 >= requiredLength;
            final long[] data = new long[(int) packedLength];
            a.getBits(0, data, 0, a.length());
            int disp = 0;
            for (int k = 0; k < data.length - 1; k++, disp += 8) {
                long v = data[k];
                bytes[disp] = long0(v);
                bytes[disp + 1] = long1(v);
                bytes[disp + 2] = long2(v);
                bytes[disp + 3] = long3(v);
                bytes[disp + 4] = long4(v);
                bytes[disp + 5] = long5(v);
                bytes[disp + 6] = long6(v);
                bytes[disp + 7] = long7(v);
            }
            if (data.length > 0) {
                long v = data[data.length - 1];
                for (int shift = 0; disp < requiredLength; disp++, shift += 8) {
                    bytes[disp] = (byte) (v >>> shift);
                }
            }
        } else if (array instanceof CharArray) {
            final char[] data = Arrays.toJavaArray((CharArray) array);
            for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                char v = data[k];
                bytes[disp] = char0(v);
                bytes[disp + 1] = char1(v);
            }
        } else if (array instanceof ShortArray) {
            final short[] data = Arrays.toJavaArray((ShortArray) array);
            for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                short v = data[k];
                bytes[disp] = short0(v);
                bytes[disp + 1] = short1(v);
            }
        } else if (array instanceof IntArray) {
            final int[] data = Arrays.toJavaArray((IntArray) array);
            for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                int v = data[k];
                bytes[disp] = int0(v);
                bytes[disp + 1] = int1(v);
                bytes[disp + 2] = int2(v);
                bytes[disp + 3] = int3(v);
            }
        } else if (array instanceof LongArray) {
            final long[] data = Arrays.toJavaArray((LongArray) array);
            for (int k = 0, disp = 0; k < data.length; k++, disp += 8) {
                long v = data[k];
                bytes[disp] = long0(v);
                bytes[disp + 1] = long1(v);
                bytes[disp + 2] = long2(v);
                bytes[disp + 3] = long3(v);
                bytes[disp + 4] = long4(v);
                bytes[disp + 5] = long5(v);
                bytes[disp + 6] = long6(v);
                bytes[disp + 7] = long7(v);
            }
        } else if (array instanceof FloatArray) {
            final float[] data = Arrays.toJavaArray((FloatArray) array);
            for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                int v = Float.floatToRawIntBits(data[k]);
                bytes[disp] = int0(v);
                bytes[disp + 1] = int1(v);
                bytes[disp + 2] = int2(v);
                bytes[disp + 3] = int3(v);
            }
        } else if (array instanceof DoubleArray) {
            final double[] data = Arrays.toJavaArray((DoubleArray) array);
            for (int k = 0, disp = 0; k < data.length; k++, disp += 8) {
                long v = Double.doubleToRawLongBits(data[k]);
                bytes[disp] = long0(v);
                bytes[disp + 1] = long1(v);
                bytes[disp + 2] = long2(v);
                bytes[disp + 3] = long3(v);
                bytes[disp + 4] = long4(v);
                bytes[disp + 5] = long5(v);
                bytes[disp + 6] = long6(v);
                bytes[disp + 7] = long7(v);
            }
        } else
            throw new AssertionError("Unallowed type of passed array: " + array.getClass());
        return bytes;
    }

    public static void copyBytesToArray(UpdatablePArray array, byte[] bytes) {
        if (array == null)
            throw new NullPointerException("Null array");
        if (bytes == null)
            throw new NullPointerException("Null bytes Java array");
        final long requiredLength = Arrays.copyArrayRequiredNumberOfBytes(array);
        if (bytes.length < requiredLength)
            throw new IndexOutOfBoundsException("byte[] array is too short to copy into all elements of "
                + "the AlgART array: " + requiredLength + " bytes required, but only " + bytes.length + " available");
        if (array instanceof UpdatableByteArray) {
            assert requiredLength == array.length();
            array.setData(0, bytes, 0, (int) requiredLength);
        } else if (array instanceof UpdatableBitArray) {
            UpdatableBitArray a = (UpdatableBitArray) array;
            final long packedLength = PackedBitArrays.packedLength(a.length());
            assert packedLength <= Integer.MAX_VALUE; // because requiredLength <= bytes.length <= Integer.MAX_VALUE
            assert packedLength * 8 >= requiredLength;
            final long[] data = new long[(int) packedLength];
            int disp = 0;
            for (int k = 0; k < data.length - 1; k++, disp += 8) {
                data[k] = makeLong(
                    bytes[disp],
                    bytes[disp + 1],
                    bytes[disp + 2],
                    bytes[disp + 3],
                    bytes[disp + 4],
                    bytes[disp + 5],
                    bytes[disp + 6],
                    bytes[disp + 7]);
            }
            if (data.length > 0) {
                long v = 0;
                for (int shift = 0; disp < requiredLength; disp++, shift += 8) {
                    v |= ((long) (bytes[disp] & 0xFF)) << shift;
                }
                data[data.length - 1] = v;
            }
            a.setBits(0, data, 0, a.length());
        } else if (array instanceof UpdatableCharArray) {
            final char[] data = new char[(int) array.length()];
            for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                data[k] = makeChar(
                    bytes[disp],
                    bytes[disp + 1]);
            }
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableShortArray) {
            final short[] data = new short[(int) array.length()];
            for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                data[k] = makeShort(
                    bytes[disp],
                    bytes[disp + 1]);
            }
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableIntArray) {
            final int[] data = new int[(int) array.length()];
            for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                data[k] = makeInt(
                    bytes[disp],
                    bytes[disp + 1],
                    bytes[disp + 2],
                    bytes[disp + 3]);
            }
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableLongArray) {
            final long[] data = new long[(int) array.length()];
            for (int k = 0, disp = 0; k < data.length; k++, disp += 8) {
                data[k] = makeLong(
                    bytes[disp],
                    bytes[disp + 1],
                    bytes[disp + 2],
                    bytes[disp + 3],
                    bytes[disp + 4],
                    bytes[disp + 5],
                    bytes[disp + 6],
                    bytes[disp + 7]);
            }
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableFloatArray) {
            final float[] data = new float[(int) array.length()];
            for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                data[k] = Float.intBitsToFloat(makeInt(
                    bytes[disp],
                    bytes[disp + 1],
                    bytes[disp + 2],
                    bytes[disp + 3]));
            }
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableDoubleArray) {
            final double[] data = new double[(int) array.length()];
            for (int k = 0, disp = 0; k < data.length; k++, disp += 8) {
                data[k] = Double.longBitsToDouble(makeLong(
                    bytes[disp],
                    bytes[disp + 1],
                    bytes[disp + 2],
                    bytes[disp + 3],
                    bytes[disp + 4],
                    bytes[disp + 5],
                    bytes[disp + 6],
                    bytes[disp + 7]));
            }
            array.setData(0, data, 0, data.length);
        } else
            throw new AssertionError("Unallowed type of passed array: " + array.getClass());
    }

    private static char makeChar(byte b0, byte b1) {
        return (char) ((b1 << 8) | (b0 & 0xff));
    }

    private static byte char0(char x) {
        return (byte) x;
    }

    private static byte char1(char x) {
        return (byte) (x >> 8);
    }

    private static short makeShort(byte b0, byte b1) {
        return (short) ((b1 << 8) | (b0 & 0xff));
    }

    private static byte short0(short x) {
        return (byte) x;
    }

    private static byte short1(short x) {
        return (byte) (x >> 8);
    }

    static private int makeInt(byte b0, byte b1, byte b2, byte b3) {
        return (((b3 & 0xff) << 24) |
            ((b2 & 0xff) << 16) |
            ((b1 & 0xff) << 8) |
            (b0 & 0xff));
    }

    private static byte int0(int x) {
        return (byte) x;
    }

    private static byte int1(int x) {
        return (byte) (x >> 8);
    }

    private static byte int2(int x) {
        return (byte) (x >> 16);
    }

    private static byte int3(int x) {
        return (byte) (x >> 24);
    }

    private static long makeLong(
        byte b0, byte b1, byte b2, byte b3,
        byte b4, byte b5, byte b6, byte b7)
    {
        return ((((long) b7) << 56) |
            (((long) b6 & 0xff) << 48) |
            (((long) b5 & 0xff) << 40) |
            (((long) b4 & 0xff) << 32) |
            (((long) b3 & 0xff) << 24) |
            (((long) b2 & 0xff) << 16) |
            (((long) b1 & 0xff) << 8) |
            (((long) b0 & 0xff)));
    }

    private static byte long0(long x) {
        return (byte) x;
    }

    private static byte long1(long x) {
        return (byte) (x >>> 8);
    }

    private static byte long2(long x) {
        return (byte) (x >>> 16);
    }

    private static byte long3(long x) {
        return (byte) (x >>> 24);
    }

    private static byte long4(long x) {
        return (byte) (x >>> 32);
    }

    private static byte long5(long x) {
        return (byte) (x >>> 40);
    }

    private static byte long6(long x) {
        return (byte) (x >>> 48);
    }

    private static byte long7(long x) {
        return (byte) (x >>> 56);
    }
}
