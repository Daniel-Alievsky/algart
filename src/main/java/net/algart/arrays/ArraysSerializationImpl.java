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

package net.algart.arrays;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.*;
import java.util.Objects;

/**
 * <p>Implementations of {@link Arrays#arrayToBytes(byte[], PArray, ByteOrder)} and
 * {@link Arrays#bytesToArray(UpdatablePArray, byte[], ByteOrder)} methods.</p>
 *
 * @author Daniel Alievsky
 */
class ArraysSerializationImpl {

    public static void write(OutputStream outputStream, PArray array, ByteOrder byteOrder) throws IOException {
        Objects.requireNonNull(outputStream, "Null outputStream array");
        Objects.requireNonNull(array, "Null array argument");
        Objects.requireNonNull(byteOrder, "Null byteOrder argument");
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
                    int len = (int) Math.min(n - p, (long) bits.length * 64L);
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
                    ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
                    // - in old Java versions we used allocateDirect for maximal performance;
                    // but in new versions put() method for asXxxBuffer works quickly also for non-direct buffers
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
                    } else {
                        throw new AssertionError("Unallowed type of passed argument: " + array.getClass());
                    }
                }
            } finally {
                Arrays.dispose(buf);
            }
        }
    }

    public static void read(InputStream inputStream, UpdatablePArray array, ByteOrder byteOrder) throws IOException {
        Objects.requireNonNull(inputStream, "Null inputStream array");
        Objects.requireNonNull(array, "Null array argument");
        Objects.requireNonNull(byteOrder, "Null byteOrder argument");
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
                    int len = (int) Math.min(n - p, (long) bits.length * 64L);
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
                    ByteBuffer byteBuffer = ByteBuffer.allocate(bytes.length);
                    // - in old Java versions we used allocateDirect for maximal performance;
                    // but in new versions get() method for asXxxBuffer works quickly also for non-direct buffers
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
                    } else {
                        throw new AssertionError("Unallowed type of passed argument: " + array.getClass());
                    }
                }
            } finally {
                Arrays.dispose(buf);
            }
        }
    }
}
