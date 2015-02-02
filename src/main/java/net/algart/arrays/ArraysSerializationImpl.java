/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2015 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.*;

/**
 * <p>Implementations of {@link Arrays#copyArrayToBytes(byte[], PArray, ByteOrder)} and
 * {@link Arrays#copyBytesToArray(UpdatablePArray, byte[], ByteOrder)} methods.</p>
 *
 * <p>AlgART Laboratory 2007&ndash;2015</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
class ArraysSerializationImpl {

    public static byte[] copyArrayToBytes(byte[] bytes, PArray array, ByteOrder byteOrder) {
        if (array == null)
            throw new NullPointerException("Null array");
        if (byteOrder == null)
            throw new NullPointerException("Null byteOrder");
        final long requiredLength = Arrays.sizeOfBytesForCopying(array);
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
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                    char v = data[k];
                    bytes[disp] = char0(v);
                    bytes[disp + 1] = char1(v);
                }
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                    char v = data[k];
                    bytes[disp] = char1(v);
                    bytes[disp + 1] = char0(v);
                }
            }
        } else if (array instanceof ShortArray) {
            final short[] data = Arrays.toJavaArray((ShortArray) array);
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                    short v = data[k];
                    bytes[disp] = short0(v);
                    bytes[disp + 1] = short1(v);
                }
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                    short v = data[k];
                    bytes[disp] = short1(v);
                    bytes[disp + 1] = short0(v);
                }
            }
        } else if (array instanceof IntArray) {
            final int[] data = Arrays.toJavaArray((IntArray) array);
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                    int v = data[k];
                    bytes[disp] = int0(v);
                    bytes[disp + 1] = int1(v);
                    bytes[disp + 2] = int2(v);
                    bytes[disp + 3] = int3(v);
                }
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                    int v = data[k];
                    bytes[disp] = int3(v);
                    bytes[disp + 1] = int2(v);
                    bytes[disp + 2] = int1(v);
                    bytes[disp + 3] = int0(v);
                }
            }
        } else if (array instanceof LongArray) {
            final long[] data = Arrays.toJavaArray((LongArray) array);
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
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
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 8) {
                    long v = data[k];
                    bytes[disp] = long7(v);
                    bytes[disp + 1] = long6(v);
                    bytes[disp + 2] = long5(v);
                    bytes[disp + 3] = long4(v);
                    bytes[disp + 4] = long3(v);
                    bytes[disp + 5] = long2(v);
                    bytes[disp + 6] = long1(v);
                    bytes[disp + 7] = long0(v);
                }
            }
        } else if (array instanceof FloatArray) {
            final float[] data = Arrays.toJavaArray((FloatArray) array);
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                    int v = Float.floatToRawIntBits(data[k]);
                    bytes[disp] = int0(v);
                    bytes[disp + 1] = int1(v);
                    bytes[disp + 2] = int2(v);
                    bytes[disp + 3] = int3(v);
                }
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                    int v = Float.floatToRawIntBits(data[k]);
                    bytes[disp] = int3(v);
                    bytes[disp + 1] = int2(v);
                    bytes[disp + 2] = int1(v);
                    bytes[disp + 3] = int0(v);
                }
            }
        } else if (array instanceof DoubleArray) {
            final double[] data = Arrays.toJavaArray((DoubleArray) array);
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
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
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 8) {
                    long v = Double.doubleToRawLongBits(data[k]);
                    bytes[disp] = long7(v);
                    bytes[disp + 1] = long6(v);
                    bytes[disp + 2] = long5(v);
                    bytes[disp + 3] = long4(v);
                    bytes[disp + 4] = long3(v);
                    bytes[disp + 5] = long2(v);
                    bytes[disp + 6] = long1(v);
                    bytes[disp + 7] = long0(v);
                }
            }
        } else
            throw new AssertionError("Unallowed type of passed array: " + array.getClass());
        return bytes;
    }

    public static void copyBytesToArray(UpdatablePArray array, byte[] bytes, ByteOrder byteOrder) {
        if (array == null)
            throw new NullPointerException("Null array");
        if (bytes == null)
            throw new NullPointerException("Null bytes Java array");
        if (byteOrder == null)
            throw new NullPointerException("Null byteOrder");
        final long requiredLength = Arrays.sizeOfBytesForCopying(array);
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
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                    data[k] = makeChar(
                        bytes[disp],
                        bytes[disp + 1]);
                }
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                    data[k] = makeCharBE(
                        bytes[disp],
                        bytes[disp + 1]);
                }
            }
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableShortArray) {
            final short[] data = new short[(int) array.length()];
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                    data[k] = makeShort(
                        bytes[disp],
                        bytes[disp + 1]);
                }
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 2) {
                    data[k] = makeShortBE(
                        bytes[disp],
                        bytes[disp + 1]);
                }
            }
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableIntArray) {
            final int[] data = new int[(int) array.length()];
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                    data[k] = makeInt(
                        bytes[disp],
                        bytes[disp + 1],
                        bytes[disp + 2],
                        bytes[disp + 3]);
                }
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                    data[k] = makeIntBE(
                        bytes[disp],
                        bytes[disp + 1],
                        bytes[disp + 2],
                        bytes[disp + 3]);
                }
            }
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableLongArray) {
            final long[] data = new long[(int) array.length()];
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
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
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 8) {
                    data[k] = makeLongBE(
                        bytes[disp],
                        bytes[disp + 1],
                        bytes[disp + 2],
                        bytes[disp + 3],
                        bytes[disp + 4],
                        bytes[disp + 5],
                        bytes[disp + 6],
                        bytes[disp + 7]);
                }
            }
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableFloatArray) {
            final float[] data = new float[(int) array.length()];
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                    data[k] = Float.intBitsToFloat(makeInt(
                        bytes[disp],
                        bytes[disp + 1],
                        bytes[disp + 2],
                        bytes[disp + 3]));
                }
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 4) {
                    data[k] = Float.intBitsToFloat(makeIntBE(
                        bytes[disp],
                        bytes[disp + 1],
                        bytes[disp + 2],
                        bytes[disp + 3]));
                }
            }
            array.setData(0, data, 0, data.length);
        } else if (array instanceof UpdatableDoubleArray) {
            final double[] data = new double[(int) array.length()];
            if (byteOrder == ByteOrder.LITTLE_ENDIAN) {
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
            } else {
                for (int k = 0, disp = 0; k < data.length; k++, disp += 8) {
                    data[k] = Double.longBitsToDouble(makeLongBE(
                        bytes[disp],
                        bytes[disp + 1],
                        bytes[disp + 2],
                        bytes[disp + 3],
                        bytes[disp + 4],
                        bytes[disp + 5],
                        bytes[disp + 6],
                        bytes[disp + 7]));
                }
            }
            array.setData(0, data, 0, data.length);
        } else
            throw new AssertionError("Unallowed type of passed array: " + array.getClass());
    }

    public static void write(OutputStream outputStream, PArray array, ByteOrder byteOrder) throws IOException {
        if (outputStream == null)
            throw new NullPointerException("Null outputStream array");
        if (array == null)
            throw new NullPointerException("Null array argument");
        if (byteOrder == null)
            throw new NullPointerException("Null byteOrder argument");
        final long n = array.length();
        if (array instanceof BitArray) {
            long[] bits = (long[]) DataBuffersImpl.LONG_BUFFERS.requestArray();
            try {
                BitArray bitArray = (BitArray) array;
                ByteBuffer byteBuffer = ByteBuffer.allocate(8 * (int) Math.min(bits.length, (n + 63) >>> 6));
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                byte[] bytes = byteBuffer.array();
                LongBuffer longBuffer = byteBuffer.asLongBuffer();
                for (long p = 0; p < n; ) {
                    int len = (int) Math.min(n - p, bits.length * 64);
                    int packedLen = (len + 63) >>> 6;
                    bitArray.getBits(p, bits, 0, len);
                    if ((len & 63) != 0) {
                        PackedBitArrays.fillBits(bits, len, 64 - (len & 63), false);
                        // clearing extra bits in the last byte
                    }
                    longBuffer.rewind();
                    longBuffer.put(bits, 0, packedLen);
                    outputStream.write(bytes, 0, packedLen * 8);
                    p += len;
                }
            } finally {
                DataBuffersImpl.LONG_BUFFERS.releaseArray(bits);
            }

        } else {
            DataBuffer buf = Arrays.bufferInternal(array, DataBuffer.AccessMode.READ,
                AbstractArray.largeBufferCapacity(array), true);
            Arrays.enableCaching(buf);
            try {
                if (array instanceof ByteArray) {
                    for (buf.map(0); buf.hasData(); buf.mapNext()) {
                        outputStream.write((byte[]) buf.data(), buf.from(), buf.cnt());
                    }
                } else {
                    int sizeOfElement = (int) array.bitsPerElement() >>> 3;
                    byte[] bytes = new byte[sizeOfElement * (int) Math.min(buf.capacity(), n)];
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
                    byteBuffer.order(byteOrder);
                    //[[Repeat() Char ==> Short,,Int,,Long,,Float,,Double;;
                    //           char ==> short,,int,,long,,float,,double;;
                    //           if\s+\(array ==> } else if (array,,... ]]
                    if (array instanceof CharArray) {
                        CharBuffer charBuffer = byteBuffer.asCharBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            charBuffer.rewind();
                            charBuffer.put((char[]) buf.data(), buf.from(), buf.cnt());
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            byteBuffer.rewind();
                            byteBuffer.get(bytes, 0, numberOfBytes);
                            outputStream.write(bytes, 0, numberOfBytes);
                        }
                        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    } else if (array instanceof ShortArray) {
                        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            shortBuffer.rewind();
                            shortBuffer.put((short[]) buf.data(), buf.from(), buf.cnt());
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            byteBuffer.rewind();
                            byteBuffer.get(bytes, 0, numberOfBytes);
                            outputStream.write(bytes, 0, numberOfBytes);
                        }
                    } else if (array instanceof IntArray) {
                        IntBuffer intBuffer = byteBuffer.asIntBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            intBuffer.rewind();
                            intBuffer.put((int[]) buf.data(), buf.from(), buf.cnt());
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            byteBuffer.rewind();
                            byteBuffer.get(bytes, 0, numberOfBytes);
                            outputStream.write(bytes, 0, numberOfBytes);
                        }
                    } else if (array instanceof LongArray) {
                        LongBuffer longBuffer = byteBuffer.asLongBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            longBuffer.rewind();
                            longBuffer.put((long[]) buf.data(), buf.from(), buf.cnt());
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            byteBuffer.rewind();
                            byteBuffer.get(bytes, 0, numberOfBytes);
                            outputStream.write(bytes, 0, numberOfBytes);
                        }
                    } else if (array instanceof FloatArray) {
                        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            floatBuffer.rewind();
                            floatBuffer.put((float[]) buf.data(), buf.from(), buf.cnt());
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            byteBuffer.rewind();
                            byteBuffer.get(bytes, 0, numberOfBytes);
                            outputStream.write(bytes, 0, numberOfBytes);
                        }
                    } else if (array instanceof DoubleArray) {
                        DoubleBuffer doubleBuffer = byteBuffer.asDoubleBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            doubleBuffer.rewind();
                            doubleBuffer.put((double[]) buf.data(), buf.from(), buf.cnt());
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            byteBuffer.rewind();
                            byteBuffer.get(bytes, 0, numberOfBytes);
                            outputStream.write(bytes, 0, numberOfBytes);
                        }
                        //[[Repeat.AutoGeneratedEnd]]
                    } else
                        throw new AssertionError("Unallowed type of passed argument: " + array.getClass());
                }
            } finally {
                Arrays.dispose(buf);
            }
        }
    }

    public static void read(InputStream inputStream, UpdatablePArray array, ByteOrder byteOrder) throws IOException {
        if (inputStream == null)
            throw new NullPointerException("Null inputStream array");
        if (array == null)
            throw new NullPointerException("Null array argument");
        if (byteOrder == null)
            throw new NullPointerException("Null byteOrder argument");
        final long n = array.length();
        DataInputStream dataInputStream = new DataInputStream(inputStream); // DataInputStream - for readFully feature
        if (array instanceof BitArray) {
            long[] bits = (long[]) DataBuffersImpl.LONG_BUFFERS.requestArray();
            try {
                UpdatableBitArray bitArray = (UpdatableBitArray) array;
                ByteBuffer byteBuffer = ByteBuffer.allocate(8 * (int) Math.min(bits.length, (n + 63) >>> 6));
                byteBuffer.order(ByteOrder.LITTLE_ENDIAN);
                byte[] bytes = byteBuffer.array();
                LongBuffer longBuffer = byteBuffer.asLongBuffer();
                for (long p = 0; p < n; ) {
                    int len = (int) Math.min(n - p, bits.length * 64);
                    int packedLen = (len + 63) >>> 6;
                    dataInputStream.readFully(bytes, 0, packedLen * 8);
                    longBuffer.rewind();
                    longBuffer.get(bits, 0, packedLen);
                    bitArray.setBits(p, bits, 0, len);
                    p += len;
                }
            } finally {
                DataBuffersImpl.LONG_BUFFERS.releaseArray(bits);
            }

        } else {
            DataBuffer buf = Arrays.bufferInternal(array, DataBuffer.AccessMode.READ_WRITE,
                AbstractArray.largeBufferCapacity(array), true);
            Arrays.enableCaching(buf);
            try {
                if (array instanceof ByteArray) {
                    for (buf.map(0); buf.hasData(); buf.mapNext()) {
                        dataInputStream.readFully((byte[]) buf.data(), buf.from(), buf.cnt());
                        buf.force();
                    }
                } else {
                    int sizeOfElement = (int) array.bitsPerElement() >>> 3;
                    byte[] bytes = new byte[sizeOfElement * (int) Math.min(buf.capacity(), n)];
                    ByteBuffer byteBuffer = ByteBuffer.allocateDirect(bytes.length);
                    byteBuffer.order(byteOrder);
                    //[[Repeat() Char ==> Short,,Int,,Long,,Float,,Double;;
                    //           char ==> short,,int,,long,,float,,double;;
                    //           if\s+\(array ==> } else if (array,,... ]]
                    if (array instanceof CharArray) {
                        CharBuffer charBuffer = byteBuffer.asCharBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            dataInputStream.readFully(bytes, 0, numberOfBytes);
                            byteBuffer.rewind();
                            byteBuffer.put(bytes, 0, numberOfBytes);
                            charBuffer.rewind();
                            charBuffer.get((char[]) buf.data(), buf.from(), buf.cnt());
                            buf.force();
                        }
                        //[[Repeat.AutoGeneratedStart !! Auto-generated: NOT EDIT !! ]]
                    } else if (array instanceof ShortArray) {
                        ShortBuffer shortBuffer = byteBuffer.asShortBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            dataInputStream.readFully(bytes, 0, numberOfBytes);
                            byteBuffer.rewind();
                            byteBuffer.put(bytes, 0, numberOfBytes);
                            shortBuffer.rewind();
                            shortBuffer.get((short[]) buf.data(), buf.from(), buf.cnt());
                            buf.force();
                        }
                    } else if (array instanceof IntArray) {
                        IntBuffer intBuffer = byteBuffer.asIntBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            dataInputStream.readFully(bytes, 0, numberOfBytes);
                            byteBuffer.rewind();
                            byteBuffer.put(bytes, 0, numberOfBytes);
                            intBuffer.rewind();
                            intBuffer.get((int[]) buf.data(), buf.from(), buf.cnt());
                            buf.force();
                        }
                    } else if (array instanceof LongArray) {
                        LongBuffer longBuffer = byteBuffer.asLongBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            dataInputStream.readFully(bytes, 0, numberOfBytes);
                            byteBuffer.rewind();
                            byteBuffer.put(bytes, 0, numberOfBytes);
                            longBuffer.rewind();
                            longBuffer.get((long[]) buf.data(), buf.from(), buf.cnt());
                            buf.force();
                        }
                    } else if (array instanceof FloatArray) {
                        FloatBuffer floatBuffer = byteBuffer.asFloatBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            dataInputStream.readFully(bytes, 0, numberOfBytes);
                            byteBuffer.rewind();
                            byteBuffer.put(bytes, 0, numberOfBytes);
                            floatBuffer.rewind();
                            floatBuffer.get((float[]) buf.data(), buf.from(), buf.cnt());
                            buf.force();
                        }
                    } else if (array instanceof DoubleArray) {
                        DoubleBuffer doubleBuffer = byteBuffer.asDoubleBuffer();
                        for (buf.map(0); buf.hasData(); buf.mapNext()) {
                            int numberOfBytes = buf.cnt() * sizeOfElement;
                            dataInputStream.readFully(bytes, 0, numberOfBytes);
                            byteBuffer.rewind();
                            byteBuffer.put(bytes, 0, numberOfBytes);
                            doubleBuffer.rewind();
                            doubleBuffer.get((double[]) buf.data(), buf.from(), buf.cnt());
                            buf.force();
                        }
                        //[[Repeat.AutoGeneratedEnd]]
                    } else
                        throw new AssertionError("Unallowed type of passed argument: " + array.getClass());
                }
            } finally {
                Arrays.dispose(buf);
            }
        }
    }

    private static char makeChar(byte b0, byte b1) {
        return (char) ((b1 << 8) | (b0 & 0xff));
    }

    private static short makeShort(byte b0, byte b1) {
        return (short) ((b1 << 8) | (b0 & 0xff));
    }

    private static int makeInt(byte b0, byte b1, byte b2, byte b3) {
        return (((b3 & 0xff) << 24) |
            ((b2 & 0xff) << 16) |
            ((b1 & 0xff) << 8) |
            (b0 & 0xff));
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

    private static char makeCharBE(byte b1, byte b0) {
        return (char) ((b1 << 8) | (b0 & 0xff));
    }

    private static short makeShortBE(byte b1, byte b0) {
        return (short) ((b1 << 8) | (b0 & 0xff));
    }

    private static int makeIntBE(byte b3, byte b2, byte b1, byte b0) {
        return (((b3 & 0xff) << 24) |
            ((b2 & 0xff) << 16) |
            ((b1 & 0xff) << 8) |
            (b0 & 0xff));
    }

    private static long makeLongBE(
        byte b7, byte b6, byte b5, byte b4,
        byte b3, byte b2, byte b1, byte b0)
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

    private static byte char0(char x) {
        return (byte) x;
    }

    private static byte char1(char x) {
        return (byte) (x >> 8);
    }

    private static byte short0(short x) {
        return (byte) x;
    }

    private static byte short1(short x) {
        return (byte) (x >> 8);
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
