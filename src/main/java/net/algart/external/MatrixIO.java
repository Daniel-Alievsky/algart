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

package net.algart.external;

import net.algart.arrays.*;
import net.algart.external.awt.BufferedImageToMatrix;
import net.algart.external.awt.MatrixToBufferedImage;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MatrixIO {
    private static final boolean USE_DEPRECATED_READ_WRITE = false;

    public enum SerializationMode {
        JAVA_BASED,
        BYTE_BUFFER
    }

    private static final int SERIALIZATION_BUFFER_SIZE = 65536; // must be divisible by 8

    public static String extension(Path file) {
        Objects.requireNonNull(file, "Null file");
        final Path fileName = file.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Path without file name is not allowed: " + file);
        }
        return extension(fileName.toString());
    }

    public static String extension(Path file, String defaultExtension) {
        Objects.requireNonNull(file, "Null file");
        final Path fileName = file.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Path without file name is not allowed: " + file);
        }
        return extension(fileName.toString(), defaultExtension);
    }

    public static String extension(File file) {
        Objects.requireNonNull(file, "Null file");
        return extension(file.getName());
    }

    public static String extension(File file, String defaultExtension) {
        Objects.requireNonNull(file, "Null file");
        return extension(file.getName(), defaultExtension);
    }

    public static String extension(String fileName) {
        final String extension = extension(fileName, null);
        if (extension == null) {
            throw new IllegalArgumentException("File name without extension is not allowed: " + fileName);
        }
        return extension;
    }

    public static String extension(String fileName, String defaultExtension) {
        Objects.requireNonNull(fileName, "Null fileName");
        if (fileName.isEmpty()) {
            throw new IllegalArgumentException("Empty file name is not allowed");
        }
        int p = fileName.lastIndexOf('.');
        if (p == -1) {
            return defaultExtension;
        }
        final String result = fileName.substring(p + 1);
        return result.isEmpty() ? defaultExtension : result;
    }

    public static String removeExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        final int p = fileName.lastIndexOf('.');
        return p == -1 ? fileName : fileName.substring(0, p);
    }

    public static void writeBufferedImage(Path file, BufferedImage image) throws IOException {
        Objects.requireNonNull(file, "Null file");
        Objects.requireNonNull(image, "Null image");
        String formatName = extension(file);
        if (!ImageIO.write(image, formatName, file.toFile())) {
            throw new IOException("Cannot write " + file + ": no \"" + formatName +
                    "\" format writer for this image type (" + image + ")");
        }
    }

    public static BufferedImage readBufferedImage(Path file) throws IOException {
        Objects.requireNonNull(file, "Null file");
        if (!Files.exists(file)) {
            throw new FileNotFoundException("Image file " + file + " does not exist");
        }
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new IIOException("Cannot read " + file + ": no suitable reader");
        }
        return image;
    }

    public static void writeImage(Path file, List<? extends Matrix<? extends PArray>> image) throws IOException {
        Objects.requireNonNull(file, "Null file");
        Objects.requireNonNull(image, "Null image");
        BufferedImage bufferedImage;
        if (USE_DEPRECATED_READ_WRITE) {
            writeBufferedImage(file, (new ColorImageFormatter.Simple()).toBufferedImage(image));
            return;
        }
        final Matrix<PArray> matrix = Matrices.interleave(null, image);
        writeBufferedImage(file, new MatrixToBufferedImage.InterleavedRGBToInterleaved().toBufferedImage(matrix));
    }

    public static List<Matrix<UpdatablePArray>> readImage(Path file) throws IOException {
        BufferedImage bufferedImage = readBufferedImage(file);
        if (USE_DEPRECATED_READ_WRITE) {
            return ((ColorImageFormatter) new ColorImageFormatter.Simple()).toImage(bufferedImage);
        }
        final Matrix<UpdatablePArray> matrix = new BufferedImageToMatrix.ToInterleaved().toMatrix(bufferedImage);
        return Matrices.separate(null, matrix);
    }

    /**
     * Should be called if you are going to call {@link #writeAlgARTImage(Path, List, boolean)}
     * with <tt>allowReferencesToStandardLargeFiles=true</tt> from an external algorithm before its finishing
     * (to return its results).
     *
     * @param image matrices, for built-in arrays of which you want to clear the temporary status
     */
    public static void clearAlgARTImageTemporaryStatus(
            List<Matrix<? extends PArray>> image) {
        for (Matrix<? extends PArray> m : image) {
            PArray a = m.array();
            if (LargeMemoryModel.isLargeArray(a)) {
                LargeMemoryModel.setTemporary(a, false);
                a.flushResources(null, true);
            }
        }
    }

    public static void writeAlgARTImage(Path folder, List<? extends Matrix<? extends PArray>> image)
            throws IOException {
        writeAlgARTImage(folder, image, false);
    }

    /**
     * Saves the multichannel <tt>image</tt> (list of matrices) in the specified folder.
     * Matrices are saved in several files in very simple format without any compression.
     * If this folder already contain an image, saved by previous call of this method,
     * it is automatically deleted (replaced with the new one).
     *
     * @param folder                              folder to save the image.
     * @param image                               some multichannel image
     * @param allowReferencesToStandardLargeFiles if <tt>true</tt>, and if one of passed matrices is
     *                                            mapped to some file F by {@link LargeMemoryModel},
     *                                            this method does not write the matrix content and
     *                                            saves a little text "reference" file with information about
     *                                            the path to this file F.
     * @throws IOException          in a case of I/O error.
     * @throws NullPointerException if one of the arguments or elements of <tt>image</tt> list is <tt>null</tt>.
     */
    public static void writeAlgARTImage(
            Path folder,
            List<? extends Matrix<? extends PArray>> image,
            boolean allowReferencesToStandardLargeFiles) throws IOException {
        Objects.requireNonNull(folder, "Null folder");
        Objects.requireNonNull(image, "Null image");
        File f = folder.toFile();
        image = new ArrayList<Matrix<? extends PArray>>(image);
        // cloning before checking guarantees correct check while multithreading
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Empty list of image bands");
        }
        f.mkdir();
        ExternalProcessor.writeUTF8(new File(f, "version"), "1.0");
        int index = 0;
        for (Matrix<? extends PArray> m : image) {
            DataFileModel<?> dataFileModel;
            if (allowReferencesToStandardLargeFiles
                    && LargeMemoryModel.isLargeArray(m.array())
                    && ((dataFileModel = LargeMemoryModel.getDataFileModel(m.array())) instanceof DefaultDataFileModel
                    || dataFileModel instanceof StandardIODataFileModel)) {
                File infFile = new File(f, index + ".inf");
                File refFile = new File(f, index + ".ref");
                LargeMemoryModel<File> lmm = LargeMemoryModel.getInstance(dataFileModel).cast(File.class);
                MatrixInfo mi = LargeMemoryModel.getMatrixInfoForSavingInFile(m, 0);
                PArray raw = LargeMemoryModel.getRawArrayForSavingInFile(m);
                assert raw != null : "Null raw array for LargeMemoryModel";
                ExternalProcessor.writeUTF8(infFile, mi.toChars());
                ExternalProcessor.writeUTF8(refFile, lmm.getDataFilePath(raw).toString());
                raw.flushResources(null, true);
            } else {
                File infFile = new File(f, index + ".inf");
                File rawFile = new File(f, String.valueOf(index));
                LargeMemoryModel<File> mm = LargeMemoryModel.getInstance(
                        new StandardIODataFileModel(rawFile, false, false));
                Matrix<? extends UpdatablePArray> clone = mm.newMatrix(UpdatablePArray.class, m);
                LargeMemoryModel.setTemporary(clone.array(), false);
                clone = clone.structureLike(m);
                MatrixInfo mi = LargeMemoryModel.getMatrixInfoForSavingInFile(clone, 0);
                ExternalProcessor.writeUTF8(infFile, mi.toChars());
                clone.array().copy(m.array());
                clone.array().freeResources(null, true);
                // - close file to allow possible deletion
            }
            index++;
        }
        for (; ; index++) {
            // - remove all further channels if they were previously saved here
            File infFile = new File(f, index + ".inf");
            File rawFile = new File(f, String.valueOf(index));
            if (!infFile.exists()) {
                break;
            }
            infFile.delete();
            rawFile.delete();
        }
    }


    /**
     * Loads the multichannel image (list of matrices), saved in the specified folder
     * by {@link #writeAlgARTImage(Path, List)} call.
     *
     * <p>Note: the files containing the matrices retain open, and any access to the returned
     * matrices will lead to operations with these files (mapping).
     * Usually you should copy the returned matrices to some other memory model
     * and call {@link Matrix#freeResources()} for them to close these files.
     *
     * @param folder folder with multichannel image, stored in a set of files.
     * @return all channels of this image.
     * @throws IOException          in a case of I/O error.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public static List<Matrix<? extends PArray>> readAlgARTImage(Path folder) throws IOException {
        Objects.requireNonNull(folder, "Null folder");
        File f = folder.toFile();
        if (!f.exists()) {
            throw new FileNotFoundException("Image subdirectory " + f + " does not exist");
        }
        if (!f.isDirectory()) {
            throw new FileNotFoundException("Image subdirectory " + f + " is not a directory");
        }
        List<Matrix<? extends PArray>> result = new ArrayList<Matrix<? extends PArray>>();
        int index = 0;
        for (; ; index++) {
            File infFile = new File(f, index + ".inf");
            File refFile = new File(f, index + ".ref");
            File rawFile = new File(f, String.valueOf(index));
            if (!infFile.exists()) {
                if (index > 0) {
                    break;
                }
                throw new FileNotFoundException("Image subdirectory " + f
                        + " does not contain 0.inf file (meta-information of the 1st image component)");
                // so, we do not allow reading empty band lists
            }
            if (refFile.exists()) {
                rawFile = new File(ExternalProcessor.readUTF8(refFile).trim());
            }
            LargeMemoryModel<File> mm = LargeMemoryModel.getInstance(new StandardIODataFileModel());
            try {
                MatrixInfo matrixInfo = MatrixInfo.valueOf(ExternalProcessor.readUTF8(infFile));
                result.add(mm.asMatrix(rawFile, matrixInfo));
            } catch (IllegalInfoSyntaxException e) {
                throw new IOException("Invalid meta-information file " + infFile + ": " + e.getMessage());
            }
        }
        return result;
    }

    public static void writeAlgARTMatrix(Path folder, Matrix<? extends PArray> matrix)
            throws IOException {
        Objects.requireNonNull(folder, "Null folder");
        Objects.requireNonNull(matrix, "Null matrix");
        File f = folder.toFile();
        if (!f.mkdir()) {
            if (!f.isDirectory()) {
                // i.e. if doesn't really exist
                throw new IOException("Cannot create matrix directory " + f);
            }
            // Important note: we must attempt to create the directory BEFORE checking its existence;
            // in other case, some parallel threads can attempt to create this directory twice,
            // that will lead to illegal messages about "errors" while creation
        }
        final PArray array = LargeMemoryModel.getRawArrayForSavingInFile(matrix);
        MatrixInfo mi = LargeMemoryModel.getMatrixInfoForSavingInFile(matrix, 0);
        ExternalProcessor.writeUTF8(new File(f, "version"), "1.0");
        File matrixFile = new File(f, matrix.dimCount() == 1 ? "vector" : "matrix");
        File indexFile = new File(f, "index");
        final LargeMemoryModel<File> mm = LargeMemoryModel.getInstance(
                new StandardIODataFileModel(matrixFile, false, false));
        final UpdatablePArray dest = (UpdatablePArray) mm.newUnresizableArray(matrix.elementType(), array.length());
        LargeMemoryModel.setTemporary(dest, false);
        mi = mi.cloneWithOtherByteOrder(dest.byteOrder());
        ExternalProcessor.writeUTF8(indexFile, mi.toChars());
        Arrays.copy(null, dest, array, 1, false);
        dest.freeResources(null);
        // - actually saves possible cached data to the file
        array.freeResources(null);
        // - necessary to avoid overflowing 2 GB limit in 32-bit JVM
    }

    public static Matrix<? extends PArray> readAlgARTMatrix(Path folder) throws IOException {
        Objects.requireNonNull(folder, "Null folder");
        File f = folder.toFile();
        File indexFile = new File(f, "index");
        try {
            MatrixInfo mi = MatrixInfo.valueOf(ExternalProcessor.readUTF8(indexFile));
            LargeMemoryModel<File> mm = LargeMemoryModel.getInstance(new StandardIODataFileModel());
            File matrixFile = new File(f, mi.dimCount() == 1 ? "vector" : "matrix");
            return mm.asMatrix(matrixFile, mi);
        } catch (IllegalInfoSyntaxException e) {
            IOException ex = new IOException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public static void serializeAlgARTMatrix(
            Matrix<? extends PArray> matrix,
            OutputStream outputStream,
            SerializationMode serializationMode,
            ByteOrder byteOrder)
            throws IOException {
        if (serializationMode == null) {
            throw new NullPointerException("Null serialization mode");
        }
        if (byteOrder == null) {
            throw new NullPointerException("Null byteOrder");
        }
        MatrixInfo matrixInfo = LargeMemoryModel.getMatrixInfoForSavingInFile(matrix, 0);
        // - we shall use CONSTANT_PROPERTY_NAME here
        matrixInfo = matrixInfo.cloneWithOtherByteOrder(byteOrder);
        String serializedMatrixInfo = matrixInfo.toChars();
        PArray array = matrix.array();
        DataOutputStream dataOutputStream = new DataOutputStream(outputStream);
        dataOutputStream.writeUTF(serializedMatrixInfo);
        if (Arrays.isNCopies(matrix.array())) {
            assert matrixInfo.additionalProperties().containsKey(LargeMemoryModel.CONSTANT_PROPERTY_NAME);
        } else {
            switch (serializationMode) {
                case JAVA_BASED: {
                    byte[] bytes = null;
                    for (long p = 0, n = array.length(); p < n; ) {
                        int len = (int) Math.min(n - p, SERIALIZATION_BUFFER_SIZE);
                        PArray subArray = (PArray) array.subArr(p, len);
                        int numberOfBytes = (int) Arrays.sizeOf(subArray);
                        // Using sizeOf instead of sizeOfBytesForCopying provides compatibility with ByteBuffer mode
                        if (bytes == null) {
                            bytes = new byte[numberOfBytes];
                        }
                        bytes = Arrays.copyArrayToBytes(bytes, subArray, byteOrder);
                        dataOutputStream.write(bytes, 0, numberOfBytes);
                        p += len;
                    }
                    break;
                }
                case BYTE_BUFFER: {
                    Arrays.write(dataOutputStream, array, byteOrder);
                    break;
                }
                default:
                    throw new UnsupportedOperationException("Unsupported " + serializationMode);
            }
        }
        dataOutputStream.flush();
    }

    public static Matrix<? extends PArray> deserializeAlgARTMatrix(
            InputStream inputStream,
            SerializationMode serializationMode,
            MemoryModel memoryModel)
            throws IOException {
        if (serializationMode == null) {
            throw new NullPointerException("Null serialization mode");
        }
        MemoryModel mm = memoryModel == null ? Arrays.SMM : memoryModel;
        DataInputStream dataInputStream = new DataInputStream(inputStream);
        String serializedMatrixInfo = dataInputStream.readUTF();
        final MatrixInfo matrixInfo;
        try {
            matrixInfo = MatrixInfo.valueOf(serializedMatrixInfo);
            final Matrix<? extends PArray> constant = LargeMemoryModel.asConstantMatrix(matrixInfo);
            if (constant != null) {
                return constant;
            }
        } catch (IllegalInfoSyntaxException e) {
            final IOException exception = new IOException(e.getMessage());
            exception.initCause(e);
            throw exception;
        }
        Matrix<? extends UpdatablePArray> matrix = mm.newMatrix(
                UpdatablePArray.class, matrixInfo.elementType(), matrixInfo.dimensions());
        UpdatablePArray array = matrix.array();
        switch (serializationMode) {
            case JAVA_BASED: {
                final long n = array.length();
                byte[] bytes = null;
                for (long p = 0; p < n; ) {
                    int len = (int) Math.min(n - p, SERIALIZATION_BUFFER_SIZE);
                    UpdatablePArray subArray = array.subArr(p, len);
                    int numberOfBytes = Arrays.sizeOfBytesForCopying(subArray);
                    if (bytes == null) {
                        bytes = new byte[numberOfBytes];
                    }
                    dataInputStream.readFully(bytes, 0, numberOfBytes);
                    Arrays.copyBytesToArray(subArray, bytes, matrixInfo.byteOrder());
                    p += len;
                }
                break;
            }
            case BYTE_BUFFER: {
                Arrays.read(dataInputStream, array, matrixInfo.byteOrder());
                break;
            }
            default:
                throw new UnsupportedOperationException("Unsupported " + serializationMode);
        }
        return matrix;
    }
}
