package net.algart.arrays.demo;

import net.algart.arrays.*;

import java.io.*;
import java.nio.ByteOrder;

/**
 * <p>Test for {@link Arrays#read(InputStream, UpdatablePArray, ByteOrder)} and
 * {@link Arrays#write(OutputStream, PArray, ByteOrder)} methods,
 * that copies one file to another.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class ReadWritePArrayByIOStreamsTest {
    public static void main(String[] args) throws IOException, InterruptedException {
        int startArgIndex = 0;
        boolean readByMapping = false;
        if (startArgIndex < args.length && args[startArgIndex].equalsIgnoreCase("-readByMapping")) {
            readByMapping = true; startArgIndex++;
        }
        if (args.length < startArgIndex + 5) {
            System.out.println("Usage: " + ReadWritePArrayByIOStreamsTest.class.getName()
                + " [-readByMapping] BE|LE primitiveType srcFile destFile [arrayLength]");
            return;
        }
        DemoUtils.initializeClass();

        LargeMemoryModel<File> lmm = LargeMemoryModel.getInstance();
        MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
        System.out.println("Current memory model:");
        System.out.println(mm);
        System.out.println("Current large memory model:");
        System.out.println(lmm);
        System.out.println();

        ByteOrder byteOrder;
        if (args[startArgIndex].equalsIgnoreCase("BE")) {
            byteOrder = ByteOrder.BIG_ENDIAN;
        } else if (args[startArgIndex].equalsIgnoreCase("LE")) {
            byteOrder = ByteOrder.LITTLE_ENDIAN;
        } else
            throw new IllegalArgumentException("Unknown byte order: " + args[startArgIndex]);

        String elementTypeName = args[startArgIndex + 1];
        Class<?> elementType =
            elementTypeName.equals("boolean") ? boolean.class :
            elementTypeName.equals("char") ? char.class :
            elementTypeName.equals("byte") ? byte.class :
            elementTypeName.equals("short") ? short.class :
            elementTypeName.equals("int") ? int.class :
            elementTypeName.equals("long") ? long.class :
            elementTypeName.equals("float") ? float.class :
            elementTypeName.equals("double") ? double.class :
            null;
        if (elementType == null)
            throw new IllegalArgumentException("Unknown element type: " + elementTypeName);
        File fSrc = new File(args[startArgIndex + 2]);
        File fDest = new File(args[startArgIndex + 3]);
        long arrayLen = Long.parseLong(args[startArgIndex + 4]);
        PArray array;
        long t1 = System.nanoTime();
        if (readByMapping) {
            array = lmm.asArray(fSrc, elementType, 0, LargeMemoryModel.ALL_FILE, byteOrder);
            if (array.length() < arrayLen)
                throw new EOFException("The source file does not contain enough data");
            array = (PArray)array.subArr(0, arrayLen);
        } else {
            array = (UpdatablePArray)mm.newUnresizableArray(elementType, arrayLen);
            InputStream inputStream = new FileInputStream(fSrc);
            Arrays.read(inputStream, (UpdatablePArray)array, byteOrder);
            inputStream.close();
        }
        long t2 = System.nanoTime();
        System.out.printf("Reading %d elements: %.3f sec, %.5f MB/sec%n", arrayLen,
            (t2 - t1) * 1e-9, Arrays.sizeOf(array) / 1048576.0 / ((t2 - t1) * 1e-9));
        OutputStream outputStream = new FileOutputStream(fDest);
        t1 = System.nanoTime();
        Arrays.write(outputStream, array, byteOrder);
        outputStream.close();
        t2 = System.nanoTime();
        System.out.printf("Writing %d elements: %.3f sec, %.5f MB/sec%n", arrayLen,
            (t2 - t1) * 1e-9, Arrays.sizeOf(array) / 1048576.0 / ((t2 - t1) * 1e-9));
    }
}
