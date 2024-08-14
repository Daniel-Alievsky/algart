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

package net.algart.io;

import net.algart.arrays.*;
import net.algart.io.awt.ImageToMatrix;
import net.algart.io.awt.MatrixToImage;

import javax.imageio.*;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

/**
 * <p>Tools for simple reading/writing images, represented by <code>BufferedImage</code> or
 * {@link Matrix AlgART matrices}.</p>
 *
 * @author Daniel Alievsky
 */
public class MatrixIO {
    /**
     * Equivalent to {@link #extension(String) extension(file.getFileName().toString())}.
     *
     * @param file some path.
     * @return the ending file extension (suffix) like "txt", "jpeg" etc.
     * @throws NullPointerException     if <code>file</code> is {@code null}.
     * @throws IllegalArgumentException if <code>file.getFileName()</code> is {@code null}
     *                                  (the path has zero elements).
     */
    public static String extension(Path file) {
        Objects.requireNonNull(file, "Null file");
        final Path fileName = file.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Path without file name is not allowed: \"" + file + "\"");
        }
        return extension(fileName.toString());
    }

    /**
     * Equivalent to {@link #extension(String, String) extension(file.getFileName().toString(), defaultExtension)}.
     *
     * @param file             some path.
     * @param defaultExtension default extension; can be {@code null}.
     * @return the ending file extension (suffix) like "txt", "jpeg" etc.
     * @throws NullPointerException     if <code>file</code> is {@code null}.
     * @throws IllegalArgumentException if <code>file.getFileName()</code> is {@code null}
     *                                  (the path has zero elements).
     */
    public static String extension(Path file, String defaultExtension) {
        Objects.requireNonNull(file, "Null file");
        final Path fileName = file.getFileName();
        if (fileName == null) {
            throw new IllegalArgumentException("Path without file name is not allowed: \"" + file + "\"");
        }
        return extension(fileName.toString(), defaultExtension);
    }

    /**
     * Equivalent to {@link #extension(String) extension(file.getName())}.
     *
     * @param file some file.
     * @return the ending file extension (suffix) like "txt", "jpeg" etc.
     * @throws NullPointerException     if <code>file</code> is {@code null}.
     * @throws IllegalArgumentException if <code>file.getName()</code> is an empty string.
     */
    public static String extension(File file) {
        Objects.requireNonNull(file, "Null file");
        return extension(file.getName());
    }

    /**
     * Equivalent to {@link #extension(String, String) extension(file.getName(), defaultExtension)}.
     *
     * @param file             some file.
     * @param defaultExtension default extension; can be {@code null}.
     * @return the ending file extension (suffix) like "txt", "jpeg" etc.
     * @throws NullPointerException     if <code>file</code> is {@code null}.
     * @throws IllegalArgumentException if <code>file.getName()</code> is an empty string.
     */
    public static String extension(File file, String defaultExtension) {
        Objects.requireNonNull(file, "Null file");
        return extension(file.getName(), defaultExtension);
    }

    /**
     * Analog of {@link #extension(String, String)}, but in a case when the file name does not contain a dot ".",
     * throws <code>IllegalArgumentException</code> instead of returning the default extension.
     * This method never returns {@code null}.
     *
     * @param fileName some file name.
     * @return the ending file extension (suffix) like "txt", "jpeg" etc.
     * @throws NullPointerException     if <code>fileName</code> is {@code null}.
     * @throws IllegalArgumentException if <code>fileName</code> is an empty string.
     */
    public static String extension(String fileName) {
        final String extension = extension(fileName, null);
        if (extension == null) {
            throw new IllegalArgumentException("File name without extension is not allowed: \"" + fileName + "\"");
        }
        return extension;
    }

    /**
     * Returns the extension (suffix) of the specified file name.
     * If the file name contains a dot ".", returns the substring after the last dot;
     * otherwise, returns <code>defaultExtension</code>.
     * If the substring after the last dot is empty, also returns <code>defaultExtension</code>.
     *
     * <p>The <code>fileName</code> argument must not be {@code null} or an empty string.</p>
     *
     * <p>For example:</p>
     * <ul>
     *     <li>for "c:\tmp\test.bmp", returns "bmp"</li>
     *     <li>for "c:\tmp\test.20.06.2023.txt", returns "txt"</li>
     *     <li>for "some_string", returns <code>defaultExtension</code></li>
     *     <li>for "some_string.", returns <code>defaultExtension</code></li>
     *     <li>for "some_string. ", returns " "</li>
     * </ul>
     *
     * <p>This method can be used together with {@link #writeBufferedImageByExtension}</p>.
     *
     * @param fileName         some file name.
     * @param defaultExtension default extension; can be {@code null}.
     * @return the ending file extension (suffix) like "txt", "jpeg" etc.
     * @throws NullPointerException     if <code>fileName</code> is {@code null}
     *                                  (but <code>defaultExtension</code> is allowed to be {@code null}).
     * @throws IllegalArgumentException if <code>fileName</code> is an empty string.
     */
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

    /**
     * If the given file name contains a dot ".", removes the substring before the last dot.
     * Otherwise, returns the argument. If the argument is {@code null}, returns {@code null}.
     *
     * @param fileName some file name.
     * @return the same string without ending file extension (suffix) like ".txt", ".jpeg" etc.
     */
    public static String removeExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        final int p = fileName.lastIndexOf('.');
        return p == -1 ? fileName : fileName.substring(0, p);
    }

    /**
     * Sets the specified compression by calling two methods:
     * <code>{@link ImageWriteParam#setCompressionMode(int)
     * parameters.setCompressionMode}(ImageWriteParam.MODE_EXPLICIT)</code>
     * and <code>{@link ImageWriteParam#setCompressionQuality(float)
     * parameters.setCompressionQuality}(quality.floatValue())</code>.
     * The second method is not called if there is no currently set compression type,
     * that is if {@link ImageWriteParam#getCompressionType() parameters.getCompressionType()} is <code>null</code>.
     * Returns <code>true</code> if both methods <code>setCompressionMode</code> and
     * <code>setCompressionQuality</code> have been successfully called.
     *
     * <p>The <code>quality</code> argument can be <code>null</code>, then this method does nothing
     * and simply returns <code>false</code>.</p>
     *
     * <p>This method can be used inside a customizer passed to
     * {@link #writeBufferedImage(Path, BufferedImage, Consumer)} and similar methods.</p>
     *
     * @param parameters parameters for writing image.
     * @param quality    quality in range 0.0..1.0; can be <code>null</code>, then this method does nothing.
     * @return whether the quality was set.
     * @throws NullPointerException if <code>parameters</code> is <code>null</code>.
     */
    public static boolean setQuality(ImageWriteParam parameters, Double quality) {
        Objects.requireNonNull(parameters, "Null parameters (ImageWriteParam)");
        if (quality == null) {
            return false;
        }
        parameters.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        if (parameters.getCompressionType() != null) {
            parameters.setCompressionQuality(quality.floatValue());
            return true;
        } else {
            return false;
        }
    }

    public static void writeBufferedImage(Path file, BufferedImage image) throws IOException {
        writeBufferedImage(file, image, null);
    }

    public static void writeBufferedImage(Path file, BufferedImage image, Consumer<ImageWriteParam> customizer)
            throws IOException {
        writeBufferedImageByExtension(file, image, extension(file), customizer);
    }

    public static void writeBufferedImageByExtension(
            Path file,
            BufferedImage image,
            String fileExtension) throws IOException {
        writeBufferedImageByExtension(file, image, fileExtension, null);
    }

    public static void writeBufferedImageByExtension(
            Path file,
            BufferedImage image,
            String fileExtension,
            Consumer<ImageWriteParam> customizer) throws IOException {
        Objects.requireNonNull(file, "Null file");
        Objects.requireNonNull(image, "Null image");
        Objects.requireNonNull(fileExtension, "Null fileExtension");
        // Note that the following call would be incorrect!
        //      ImageIO.write(image, fileExtension, file.toFile())
        // ImageIO.write method uses "String formatName" argument, which can differ from
        // file extension, for example, for JPEG-2000 in com.github.jaiimageio:
        // the format names are "jpeg 2000", "JPEG 2000", "jpeg2000", "JPEG2000",
        // but the file suffixes are only "jp2".

        final Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(fileExtension);
        final ImageWriter writer = writers.hasNext() ? writers.next() : null;
        if (writer == null) {
            throw new UnsupportedImageFormatException("Cannot write " + file +
                    ": no writers found for file suffix \"" + fileExtension + "\"");
        }
        writeBufferedImage(file, image, writer, customizer);
    }

    public static void writeBufferedImageByFormatName(
            Path file,
            BufferedImage image,
            String formatName) throws IOException {
        writeBufferedImageByFormatName(file, image, formatName, null);
    }

    public static void writeBufferedImageByFormatName(
            Path file,
            BufferedImage image,
            String formatName,
            Consumer<ImageWriteParam> customizer) throws IOException {
        Objects.requireNonNull(file, "Null file");
        Objects.requireNonNull(image, "Null image");
        Objects.requireNonNull(formatName, "Null formatName");
        ImageTypeSpecifier type = ImageTypeSpecifier.createFromRenderedImage(image);
        final Iterator<ImageWriter> writers = ImageIO.getImageWriters(type, formatName);
        final ImageWriter writer = writers.hasNext() ? writers.next() : null;
        if (writer == null) {
            throw new UnsupportedImageFormatException("Cannot write " + file +
                    ": no writers found for format name \"" + formatName +
                    "\" for writing " + image);
        }
        writeBufferedImage(file, image, writer, customizer);
    }

    public static void writeBufferedImage(
            Path file,
            BufferedImage image,
            ImageWriter writer,
            Consumer<ImageWriteParam> customizer) throws IOException {
        Objects.requireNonNull(file, "Null file");
        Objects.requireNonNull(image, "Null image");
        Objects.requireNonNull(writer, "Null writer");
        final File output = file.toFile();
        //noinspection ResultOfMethodCallIgnored
        output.delete();
        // - the same operation is performed in ImageIO class;
        // without this, an existing file will not be truncated: only its start part will be overwritten
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(output)) {
            writer.setOutput(ios);
            final ImageWriteParam writeParam = writer.getDefaultWriteParam();
            if (customizer != null) {
                customizer.accept(writeParam);
            }
            final IIOImage iioImage = new IIOImage(image, null, null);
            writer.write(null, iioImage, writeParam);
        }
    }

    public static BufferedImage readBufferedImage(Path file) throws IOException {
        Objects.requireNonNull(file, "Null file");
        if (!Files.exists(file)) {
            throw new FileNotFoundException("Image file " + file + " does not exist");
        }
        BufferedImage image = ImageIO.read(file.toFile());
        if (image == null) {
            throw new UnsupportedImageFormatException("Cannot read " + file + ": no suitable reader");
        }
        return image;
    }

    public static void writeImage(Path file, List<? extends Matrix<? extends PArray>> image) throws IOException {
        writeImage(file, image, null);
    }

    public static void writeImage(
            Path file,
            List<? extends Matrix<? extends PArray>> image,
            Consumer<ImageWriteParam> customizer) throws IOException {
        Objects.requireNonNull(file, "Null file");
        Objects.requireNonNull(image, "Null image");
        final Matrix<PArray> matrix = Matrices.interleave(image);
        final BufferedImage bi = new MatrixToImage.InterleavedRGBToInterleaved().toBufferedImage(matrix);
        writeBufferedImage(file, bi, customizer);
    }

    public static List<Matrix<UpdatablePArray>> readImage(Path file) throws IOException {
        final BufferedImage bi = readBufferedImage(file);
        final Matrix<UpdatablePArray> matrix = new ImageToMatrix.ToInterleavedRGB().toMatrix(bi);
        return Matrices.separate(matrix);
    }

    /**
     * Should be called if you are going to call {@link #writeAlgARTImage(Path, List, boolean)}
     * with <code>allowReferencesToStandardLargeFiles=true</code> from an external algorithm before its finishing
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
     * Saves the multichannel <code>image</code> (list of matrices) in the specified folder.
     * Matrices are saved in several files in very simple format without any compression.
     * If this folder already contains an image, saved by previous call of this method,
     * it is automatically deleted (replaced with the new one).
     *
     * @param folder                              folder to save the image.
     * @param image                               some multichannel image
     * @param allowReferencesToStandardLargeFiles if <code>true</code>, and if one of passed matrices is
     *                                            mapped to some file F by {@link LargeMemoryModel},
     *                                            this method does not write the matrix content and
     *                                            saves a little text "reference" file with information about
     *                                            the path to this file F.
     * @throws IOException          in a case of I/O error.
     * @throws NullPointerException if one of the arguments or elements of <code>image</code> list is {@code null}.
     */
    public static void writeAlgARTImage(
            Path folder,
            List<? extends Matrix<? extends PArray>> image,
            boolean allowReferencesToStandardLargeFiles) throws IOException {
        Objects.requireNonNull(folder, "Null folder");
        Objects.requireNonNull(image, "Null image");
        image = new ArrayList<Matrix<? extends PArray>>(image);
        // cloning before checking guarantees correct check while multithreading
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Empty list of image bands");
        }
        if (!Files.exists(folder)) {
            Files.createDirectory(folder);
        }
        File f = folder.toFile();
        Files.writeString(new File(f, "version").toPath(), "1.0");
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
                Files.writeString(infFile.toPath(), mi.toChars());
                Files.writeString(refFile.toPath(), lmm.getDataFilePath(raw).toString());
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
                Files.writeString(infFile.toPath(), mi.toChars());
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
            //noinspection ResultOfMethodCallIgnored
            infFile.delete();
            //noinspection ResultOfMethodCallIgnored
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
     * @throws NullPointerException if the argument is {@code null}.
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
        List<Matrix<? extends PArray>> result = new ArrayList<>();
        int index = 0;
        for (; ; index++) {
            final File infFile = new File(f, index + ".inf");
            final File refFile = new File(f, index + ".ref");
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
                rawFile = new File(Files.readString(refFile.toPath()).trim());
            }
            LargeMemoryModel<File> mm = LargeMemoryModel.getInstance(new StandardIODataFileModel());
            try {
                MatrixInfo matrixInfo = MatrixInfo.valueOf(Files.readString(infFile.toPath()));
                result.add(mm.asMatrix(rawFile, matrixInfo));
            } catch (IllegalInfoSyntaxException e) {
                throw new IOException("Invalid meta-information file " + infFile + ": " + e.getMessage());
            }
        }
        return result;
    }
}
