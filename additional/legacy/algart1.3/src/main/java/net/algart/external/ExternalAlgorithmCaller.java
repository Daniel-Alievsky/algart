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
import net.algart.arrays.Arrays;
import net.algart.math.IPoint;
import net.algart.math.IRectangularArea;
import net.algart.matrices.ApertureProcessor;
import net.algart.matrices.TiledApertureProcessorFactory;

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.*;
import java.lang.reflect.InvocationTargetException;
import java.nio.ByteOrder;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

public abstract class ExternalAlgorithmCaller {
    public static enum SerializationMode {
        JAVA_BASED,
        BYTE_BUFFER
    }

    private static final int SERIALIZATION_BUFFER_SIZE = 65536; // must be divisible by 8

    public static final String SYS_DIM_COUNT = "dimCount";                                 // default: 2
    public static final String SYS_COMPONENTWISE = "componentwise";                        // default: false
    public static final String SYS_TILING = "tiling";                                      // default: false
    public static final String SYS_TILE_DIM = "tileDim";                                   // default: from AlgART
    public static final String SYS_TILE_OVERLAP = "tileOverlap";                           // default: 0
    public static final String SYS_MULTITHREADING = "multithreading";                      // default: true
    public static final String SYS_NUMBER_OF_THREADS = "numberOfThreads";                  // default: 0 (i.e. auto)
    public static final String SYS_NOT_DELETE_TEMPORARY_FILES = "notDeleteTemporaryFiles"; // default: false
    public static final String SYS_SHOW_ALL_OUTPUT = "showAllOutput";                      // default: false
    public static final String SYS_CLEANUP_AT_FINISH = "cleanup";                          // default: false

    private static final Logger LOGGER = Logger.getLogger(ExternalAlgorithmCaller.class.getName());
    private static final int NUMBER_OF_ATTEMPTS_TO_RECOVER_UNSTABLE_ERROR = 5;

    // the following static fields are set by the first successful call of setParametersFromJSON
    static volatile Class<?> jsonClass;
    static volatile java.lang.reflect.Method jsonHas;
    static volatile java.lang.reflect.Method jsonOptBoolean;
    static volatile java.lang.reflect.Method jsonGetInt;
    static volatile java.lang.reflect.Method jsonGetLong;
    static volatile java.lang.reflect.Method jsonOptString;

    private volatile ArrayContext context;

    private volatile int dimCount = 2;
    private volatile boolean componentwise = false;
    private volatile boolean tiling = false;
    private volatile long tileDimensions[] = null;
    private volatile IRectangularArea tileOverlapAperture; // initialized in the constructor
    private volatile Matrix.ContinuationMode tilingContinuationMode = Matrix.ContinuationMode.MIRROR_CYCLIC;
    private volatile double[] tilingContinuationNormalizedValues = new double[0];
    // - if non-empty, then it is used instead of tilingContinuationMode
    private volatile boolean multithreading = true;
    private volatile int numberOfThreads = 0;
    private volatile boolean notDeleteTemporaryFiles = false;
    private volatile boolean showAllOutput = false;
    private volatile boolean cleanupAtFinish = false;
    private volatile String algorithmCode = "";

    protected ExternalAlgorithmCaller(ArrayContext context) {
        this.context = context;
        setTileOverlap(0);
    }

    public static Map<String, List<Matrix<? extends PArray>>> newImageMap() {
        return new LinkedHashMap<String, List<Matrix<? extends PArray>>>();
    }

    public static Map<String, List<Matrix<? extends PArray>>> newImageMap(
        String key,
        List<Matrix<? extends PArray>> image)
    {
        Map<String, List<Matrix<? extends PArray>>> result = newImageMap();
        result.put(key, image);
        return result;
    }

    public static String appendFileSeparator(String dirName) {
        return dirName.endsWith("/") || dirName.endsWith(File.separator) ?
            dirName :
            dirName + File.separator;
    }

    public static String replaceDollarWithWorkDirectory(ExternalProcessor processor, String s) {
        String workDir = processor.getWorkDirectory().getPath();
        return s.replace("$/", appendFileSeparator(workDir).replace("\\", "\\\\"));
    }

    public static List<Matrix<? extends PArray>> cloneImage(MemoryModel mm, List<Matrix<? extends PArray>> image) {
        if (mm == null) {
            throw new NullPointerException("Null memory model");
        }
        List<Matrix<? extends PArray>> result = new ArrayList<Matrix<? extends PArray>>();
        for (Matrix<? extends PArray> m : image) {
            Matrix<UpdatablePArray> clone = mm.newMatrix(UpdatablePArray.class, m);
            clone.array().copy(m.array());
            m.freeResources(); // allows possible deletion
            result.add(clone);
        }
        return result;
    }

    public static String getFileExtension(File file) {
        return getFileExtension(file.getName());
    }

    public static String getFileExtension(String fileName) {
        int p = fileName.lastIndexOf('.');
        if (p == -1) {
            return null;
        }
        return fileName.substring(p + 1);
    }

    public static File removeFileExtension(File file) {
        String fileName = file.getName();
        int p = fileName.lastIndexOf('.');
        if (p == -1) {
            return file;
        }
        return new File(file.getParentFile(), fileName.substring(0, p));
    }

    public static void writeImage(File file, List<? extends Matrix<? extends PArray>> image) throws IOException {
        String formatName = getFileExtension(file);
        if (formatName == null) {
            throw new IllegalArgumentException("Cannot write image into a file without extension");
        }
        ColorImageFormatter formatter = new SimpleColorImageFormatter();
        BufferedImage bufferedImage = formatter.toBufferedImage(image);
        if (!ImageIO.write(bufferedImage, formatName, file)) {
            throw new IOException("Cannot write " + file + ": no writer for " + formatName);
        }
    }

    public static List<Matrix<? extends PArray>> readImage(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("Image file " + file + " does not exist");
        }
        ColorImageFormatter formatter = new SimpleColorImageFormatter();
        BufferedImage bufferedImage = ImageIO.read(file);
        return formatter.toImage(bufferedImage);
    }

    public static int[] readImageDimensions(File file) throws IOException {
        if (!file.exists()) {
            throw new FileNotFoundException("Image file " + file + " does not exist");
        }
        ImageInputStream iis = ImageIO.createImageInputStream(file);
        try {
            Iterator<ImageReader> iterator = ImageIO.getImageReaders(iis);
            if (!iterator.hasNext()) {
                throw new IIOException("Unknown image format: can't create an ImageInputStream");
            }
            ImageReader reader = iterator.next();
            try {
                reader.setInput(iis);
                return new int[] {reader.getWidth(0), reader.getHeight(0)};
            } finally {
                reader.dispose();
            }
        } finally {
            iis.close();
        }
    }

    /**
     * Should be called if you are going to call {@link #writeAlgARTImage(java.io.File, java.util.List, boolean)}
     * with <tt>allowReferencesToStandardLargeFiles=true</tt> from an external algorithm before its finishing
     * (to return its results).
     *
     * @param image matrices, for built-in arrays of which you want to clear the temporary status
     */
    public static void clearAlgARTImageTemporaryStatus(
        List<Matrix<? extends PArray>> image)
    {
        for (Matrix<? extends PArray> m : image) {
            PArray a = m.array();
            if (LargeMemoryModel.isLargeArray(a)) {
                LargeMemoryModel.setTemporary(a, false);
                a.flushResources(null, true);
            }
        }
    }

    public static void writeAlgARTImage(File folder, List<? extends Matrix<? extends PArray>> image)
            throws IOException
    {
        writeAlgARTImage(folder, image, false);
    }

    /**
     * Saves the multi-channel <tt>image</tt> (list of matrices) in the specified folder.
     * Matrices are saved in several files in very simple format without any compression.
     * If this folder already contain an image, saved by previous call of this method,
     * it is automatically deleted (replaced with the new one).
     *
     * @param folder folder to save the image.
     * @param image  some multi-channel image
     * @param allowReferencesToStandardLargeFiles if <tt>true</tt>, and if one of passed matrices is
     *                                            mapped to some file F by {@link LargeMemoryModel},
     *                                            this method does not write the matrix content and
     *                                            saves a little text "reference" file with information about
     *                                            the path to this file F.
     * @throws IOException in a case of I/O error.
     * @throws NullPointerException if one of the arguments or elements of <tt>image</tt> list is <tt>null</tt>.
     */
    public static void writeAlgARTImage(
        File folder,
        List<? extends Matrix<? extends PArray>> image,
        boolean allowReferencesToStandardLargeFiles) throws IOException
    {
        image = new ArrayList<Matrix<? extends PArray>>(image);
        // cloning before checking guarantees correct check while multithreading
        if (image.isEmpty()) {
            throw new IllegalArgumentException("Empty list of image bands");
        }
        folder.mkdir();
        ExternalProcessor.writeUTF8(new File(folder, "version"), "1.0");
        int index = 0;
        for (Matrix<? extends PArray> m : image) {
            DataFileModel<?> dataFileModel;
            if (allowReferencesToStandardLargeFiles
                && LargeMemoryModel.isLargeArray(m.array())
                && ((dataFileModel = LargeMemoryModel.getDataFileModel(m.array())) instanceof DefaultDataFileModel
                || dataFileModel instanceof StandardIODataFileModel))
            {
                File infFile = new File(folder, index + ".inf");
                File refFile = new File(folder, index + ".ref");
                LargeMemoryModel<File> lmm = LargeMemoryModel.getInstance(dataFileModel).cast(File.class);
                MatrixInfo mi = LargeMemoryModel.getMatrixInfoForSavingInFile(m, 0);
                PArray raw = LargeMemoryModel.getRawArrayForSavingInFile(m);
                assert raw != null : "Null raw array for LargeMemoryModel";
                ExternalProcessor.writeUTF8(infFile, mi.toChars());
                ExternalProcessor.writeUTF8(refFile, lmm.getDataFilePath(raw).toString());
                raw.flushResources(null, true);
            } else {
                File infFile = new File(folder, index + ".inf");
                File rawFile = new File(folder, String.valueOf(index));
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
            File infFile = new File(folder, index + ".inf");
            File rawFile = new File(folder, String.valueOf(index));
            if (!infFile.exists()) {
                break;
            }
            infFile.delete();
            rawFile.delete();
        }
    }

    /**
     * Loads the multi-channel image (list of matrices), saved in the specified folder
     * by {@link #writeAlgARTImage(File, List)} call.
     *
     * <p>Note: the files containing the matrices retain open, and any access to the returned
     * matrices will lead to operations with these files (mapping).
     * Usually you should copy the returned matrices to some other memory model
     * and call {@link Matrix#freeResources()} for them to close these files.
     *
     * @param folder folder with multi-channel image, stored in a set of files.
     * @return all channels of this image.
     * @throws IOException in a case of I/O error.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public static List<Matrix<? extends PArray>> readAlgARTImage(File folder) throws IOException {
        if (!folder.exists()) {
            throw new FileNotFoundException("Image subdirectory " + folder + " does not exist");
        }
        if (!folder.isDirectory()) {
            throw new FileNotFoundException("Image subdirectory " + folder + " is not a directory");
        }
        List<Matrix<? extends PArray>> result = new ArrayList<Matrix<? extends PArray>>();
        int index = 0;
        for (; ; index++) {
            File infFile = new File(folder, index + ".inf");
            File refFile = new File(folder, index + ".ref");
            File rawFile = new File(folder, String.valueOf(index));
            if (!infFile.exists()) {
                if (index > 0) {
                    break;
                }
                throw new FileNotFoundException("Image subdirectory " + folder
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

    // NOTE: this method was used for SIMAGIS, but it contains a bug
    public static void writeAlgARTMatrix(ArrayContext context, File dir, Matrix<? extends PArray> matrix)
        throws IOException
    {
        if (dir == null)
            throw new NullPointerException("Null directory for writing matrix");
        if (matrix == null)
            throw new NullPointerException("Null matrix");
        if (!dir.mkdir()) {
            if (!dir.isDirectory()) {
                // i.e. if doesn't really exist
                throw new IOException("Cannot create matrix directory " + dir);
            }
            // Important note: we must attempt to create the directory BEFORE checking its existence;
            // in other case, some parallel threads can attempt to create this directory twice,
            // that will lead to illegal messages about "errors" while creation
        }
        final PArray array = LargeMemoryModel.getRawArrayForSavingInFile(matrix);
        MatrixInfo mi = LargeMemoryModel.getMatrixInfoForSavingInFile(matrix, 0);
        ExternalProcessor.writeUTF8(new File(dir, "version"), "1.0");
        File matrixFile = new File(dir, matrix.dimCount() == 1 ? "vector" : "matrix");
        File indexFile = new File(dir, "index");
        final LargeMemoryModel<File> mm = LargeMemoryModel.getInstance(
            new StandardIODataFileModel(matrixFile, false, false));
        final UpdatablePArray dest = (UpdatablePArray) mm.newUnresizableArray(matrix.elementType(), array.length());
        // - BUG! array may be null when Arrays.isNCopies
        LargeMemoryModel.setTemporary(dest, false);
        mi = mi.cloneWithOtherByteOrder(dest.byteOrder());
        ExternalProcessor.writeUTF8(indexFile, mi.toChars());
        Arrays.copy(context, dest, array, 0, false);
        dest.freeResources(null);
        // - actually saves possible cached data to the file
        array.freeResources(null);
        // - necessary to avoid overflowing 2 GB limit in 32-bit JVM
    }

    public static Matrix<? extends PArray> readAlgARTMatrix(ArrayContext context, File dir) throws IOException {
        if (dir == null)
            throw new NullPointerException("Null directory for reading matrix");
        File indexFile = new File(dir, "index");
        try {
            MatrixInfo mi = MatrixInfo.valueOf(ExternalProcessor.readUTF8(indexFile));
            LargeMemoryModel<File> mm = LargeMemoryModel.getInstance(new StandardIODataFileModel());
            File matrixFile = new File(dir, mi.dimCount() == 1 ? "vector" : "matrix");
            return mm.asMatrix(matrixFile, mi);
        } catch (IllegalInfoSyntaxException e) {
            IOException ex = new IOException(e.getMessage());
            ex.initCause(e);
            throw ex;
        }
    }

    public static void serializeAlgARTMatrix(
        ArrayContext context,
        Matrix<? extends PArray> matrix,
        OutputStream outputStream,
        SerializationMode serializationMode,
        ByteOrder byteOrder)
        throws IOException
    {
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
                        if (context != null) {
                            context.checkInterruptionAndUpdateProgress(array.elementType(), p, n);
                        }
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
        ArrayContext context,
        InputStream inputStream,
        SerializationMode serializationMode)
        throws IOException
    {
        if (serializationMode == null) {
            throw new NullPointerException("Null serialization mode");
        }
        MemoryModel mm = context == null ? Arrays.SMM : context.getMemoryModel();
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
                    if (context != null) {
                        context.checkInterruptionAndUpdateProgress(array.elementType(), p, n);
                    }
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

    public final ArrayContext getContext() {
        return context;
    }

    public final void setContext(ArrayContext context) {
        this.context = context;
    }

    public final int getDimCount() {
        return dimCount;
    }

    public final void setDimCount(int dimCount) {
        if (dimCount <= 0) {
            throw new IllegalArgumentException("Zero or negative dimCount");
        }
        this.dimCount = dimCount;
    }

    public final boolean isComponentwise() {
        return componentwise;
    }

    public final void setComponentwise(boolean componentwise) {
        this.componentwise = componentwise;
    }

    public final boolean isTiling() {
        return tiling;
    }

    public final void setTiling(boolean tiling) {
        this.tiling = tiling;
    }

    public final long[] getTileDimensions() {
        return this.tileDimensions == null ? Matrices.defaultTileDimensions(dimCount) : this.tileDimensions.clone();
    }

    /**
     * Sets tileDimensions and also dimCount.
     *
     * @param tileDimensions new tile dimensions.
     */
    public final void setTileDimensions(long[] tileDimensions) {
        if (tileDimensions == null) {
            throw new NullPointerException("Null tileDimensions array");
        }
        if (tileDimensions.length == 0) {
            throw new IllegalArgumentException("Empty tileDimensions array");
        }
        tileDimensions = tileDimensions.clone();
        for (int k = 0; k < tileDimensions.length; k++) {
            if (tileDimensions[k] <= 0) {
                throw new IllegalArgumentException("Negative or zero tile dimension #"
                    + k + ": " + tileDimensions[k]);
            }
        }
        setDimCount(tileDimensions.length);
        this.tileDimensions = tileDimensions;
    }

    /**
     * Sets all dimCount tile dimensions equal to the argument.
     *
     * @param tileDimension the value of all new tile dimensions.
     */
    public final void setTileDimension(long tileDimension) {
        if (tileDimension <= 0) {
            throw new IllegalArgumentException("Zero or negative tile dimension " + tileDimension);
        }
        this.tileDimensions = new long[dimCount];
        JArrays.fillLongArray(this.tileDimensions, tileDimension);
    }

    public final void setDefaultTileDimensions() {
        this.tileDimensions = null;
    }

    public final IRectangularArea getTileOverlapAperture() {
        return tileOverlapAperture;
    }

    /**
     * Sets tileOverlapAperture and also dimCount.
     *
     * @param tileOverlapAperture new tile overlap aperture.
     */
    public final void setTileOverlapAperture(IRectangularArea tileOverlapAperture) {
        if (tileOverlapAperture == null) {
            throw new NullPointerException("Null tileOverlapAperture");
        }
        setDimCount(tileOverlapAperture.coordCount());
        this.tileOverlapAperture = tileOverlapAperture;
    }

    /**
     * Sets tileOverlapAperture equal to <tt>-tileOverlap...tileOverlap</tt> for all <tt>dimCount</tt> coordinates.
     *
     * @param tileOverlap half of overlap aperture for all coordinates.
     */
    public final void setTileOverlap(long tileOverlap) {
        if (tileOverlap < 0) {
            throw new IllegalArgumentException("Negative tileOverlap = " + tileOverlap);
        }
        setTileOverlapAperture(IRectangularArea.valueOf(
            IPoint.valueOfEqualCoordinates(dimCount, -tileOverlap),
            IPoint.valueOfEqualCoordinates(dimCount, tileOverlap)));
    }

    public final Matrix.ContinuationMode getTilingContinuationMode(
        int bandIndex,
        Matrix<? extends PArray> bandMatrix)
    {
        if (tilingContinuationNormalizedValues.length > 0) {
            return Matrix.ContinuationMode.getConstantMode(bandMatrix.array().maxPossibleValue(1.0) *
                (bandIndex < tilingContinuationNormalizedValues.length ?
                    tilingContinuationNormalizedValues[bandIndex] :
                    tilingContinuationNormalizedValues[tilingContinuationNormalizedValues.length - 1]));
        } else {
            return tilingContinuationMode;
        }
    }

    public final void setTilingContinuationMode(Matrix.ContinuationMode tilingContinuationMode) {
        if (tilingContinuationMode == null) {
            throw new NullPointerException("Null tilingContinuationMode");
        }
        this.tilingContinuationMode = tilingContinuationMode;
    }

    public final void setTilingContinuationNormalizedValues(double[] tilingContinuationNormalizedValues) {
        if (tilingContinuationNormalizedValues == null) {
            throw new NullPointerException("Null tilingContinuationNormalizedValues");
        }
        this.tilingContinuationNormalizedValues = tilingContinuationNormalizedValues.clone();
    }

    public final boolean isMultithreading() {
        return multithreading;
    }

    public final void setMultithreading(boolean multithreading) {
        this.multithreading = multithreading;
    }

    public final int getNumberOfThreads() {
        return numberOfThreads > 0 ?
            numberOfThreads :
            Arrays.getThreadPoolFactory(context).recommendedNumberOfTasks();
    }

    public final void setNumberOfThreads(int numberOfThreads) {
        if (numberOfThreads < 0) {
            throw new IllegalArgumentException("Negative numberOfThreads");
        }
        this.numberOfThreads = numberOfThreads;
    }

    public final boolean isNotDeleteTemporaryFiles() {
        return notDeleteTemporaryFiles;
    }

    public final void setNotDeleteTemporaryFiles(boolean notDeleteTemporaryFiles) {
        this.notDeleteTemporaryFiles = notDeleteTemporaryFiles;
    }

    public final boolean isShowAllOutput() {
        return showAllOutput;
    }

    public final void setShowAllOutput(boolean showAllOutput) {
        this.showAllOutput = showAllOutput;
    }

    public final boolean isCleanupAtFinish() {
        return cleanupAtFinish;
    }

    public final void setCleanupAtFinish(boolean cleanupAtFinish) {
        this.cleanupAtFinish = cleanupAtFinish;
    }

    public final String getAlgorithmCode() {
        return algorithmCode;
    }

    public final void setAlgorithmCode(String algorithmCode) {
        if (algorithmCode == null) {
            throw new NullPointerException("Null algorithm code");
        }
        this.algorithmCode = algorithmCode;
    }

    // This method does not change fields, which are not specified in JSON
    public void setParametersFromJSON(Object jsonObjectOrString) {
        try {
            jsonClass = Class.forName("org.json.JSONObject");
        } catch (ClassNotFoundException e) {
            throw new UnsupportedOperationException("Cannot set parameters from JSON: "
                + "org.json.JSONObject class is not available");
        }
        try {
            jsonHas = jsonClass.getMethod("has", String.class);
            jsonOptBoolean = jsonClass.getMethod("optBoolean", String.class, boolean.class);
            jsonGetInt = jsonClass.getMethod("getInt", String.class);
            jsonGetLong = jsonClass.getMethod("getLong", String.class);
            jsonOptString = jsonClass.getMethod("optString", String.class, String.class);
            Object json = stringToJSON(jsonObjectOrString);
            if ((Boolean) jsonHas.invoke(json, SYS_DIM_COUNT)) {
                setDimCount((Integer) jsonGetInt.invoke(json, SYS_DIM_COUNT));
            }
            setComponentwise((Boolean) jsonOptBoolean.invoke(json, SYS_COMPONENTWISE, componentwise));
            setTiling((Boolean) jsonOptBoolean.invoke(json, SYS_TILING, tiling));
            if ((Boolean) jsonHas.invoke(json, SYS_TILE_DIM)) {
                String s = ((String) jsonOptString.invoke(json, SYS_TILE_DIM, "")).trim();
                String[] d = s.length() == 0 ? new String[0] : s.split("[x, ]+");
                long[] dimensions = new long[d.length];
                for (int k = 0; k < d.length; k++) {
                    dimensions[k] = Long.parseLong(d[k]);
                }
                if (dimensions.length == 1) {
                    setTileDimension(dimensions[0]);
                } else {
                    setTileDimensions(dimensions);
                }
            }
            if ((Boolean) jsonHas.invoke(json, SYS_TILE_OVERLAP)) {
                setTileOverlap((Long) jsonGetLong.invoke(json, SYS_TILE_OVERLAP));
            }
            setMultithreading((Boolean) jsonOptBoolean.invoke(json, SYS_MULTITHREADING, multithreading));
            if ((Boolean) jsonHas.invoke(json, SYS_NUMBER_OF_THREADS)) {
                setNumberOfThreads((Integer) jsonGetInt.invoke(json, SYS_NUMBER_OF_THREADS));
            }
            setNotDeleteTemporaryFiles((Boolean) jsonOptBoolean.invoke(json,
                SYS_NOT_DELETE_TEMPORARY_FILES, notDeleteTemporaryFiles));
            setShowAllOutput((Boolean) jsonOptBoolean.invoke(json, SYS_SHOW_ALL_OUTPUT, showAllOutput));
            setCleanupAtFinish((Boolean) jsonOptBoolean.invoke(json, SYS_CLEANUP_AT_FINISH, cleanupAtFinish));
        } catch (InvocationTargetException e) {
            throw (AssertionError) new AssertionError("Unexpected error while using org.json.JSONObject").initCause(e);
        } catch (NoSuchMethodException e) {
            throw (AssertionError) new AssertionError("Unexpected error while using org.json.JSONObject").initCause(e);
        } catch (IllegalAccessException e) {
            throw (AssertionError) new AssertionError("Unexpected error while using org.json.JSONObject").initCause(e);
        }
    }

    public ExternalProcessor getProcessor() {
        ExternalProcessor processor = ExternalProcessor.getInstance(
            context, ExternalProcessor.getDefaultTempDirectory(), algorithmCode);
        if (notDeleteTemporaryFiles) {
            processor.cancelRemovingWorkDirectory();
        }
        if (showAllOutput) {
            processor.setSystemStreams();
        }
        return processor;
    }

    public TiledApertureProcessorFactory getTiler(ArrayContext context, Matrix.ContinuationMode continuationMode) {
        return TiledApertureProcessorFactory.getInstance(
            context,
            continuationMode,
            Long.MAX_VALUE,
            getTileDimensions(),
            multithreading ? numberOfThreads : 1);
    }

    public final Map<String, List<Matrix<? extends PArray>>> process(
        Map<String, List<Matrix<? extends PArray>>> source,
        Object additionalData)
        throws IOException
    {
        long t1 = System.nanoTime();
        Map<String, List<Matrix<? extends PArray>>> result = processComponentwiseIfNecessary(source, additionalData);
        long t2 = System.nanoTime();
        cleanupAfterFinish();
        long t3 = System.nanoTime();
        if (Arrays.SystemSettings.profilingMode()) {
            LOGGER.config(String.format(Locale.US,
                "%d image" + (source.size() == 1 ? "" : "s") + " processed by an external program in %.3f ms"
                    + (componentwise ? " componentwise" : "")
                    + (tiling ?
                    " with tiling " + JArrays.toString(getTileDimensions(), "x", 256)
                        + " (overlap " + getTileOverlapAperture() + ")" :
                    ""),
                source.size(),
                (t2 - t1) * 1e-6));
            if (cleanupAtFinish) {
                LOGGER.config(String.format(Locale.US,
                    "Cleanup of temporary directories performed in %.3f ms",
                    (t3 - t2) * 1e-6));
            }
        }
        return result;
    }

    /* // It seems to be extra
    public final Map<String, Image2D> process(
        Context context,
        Map<String, Image2D> source, Object additionalData)
        throws IOException
    {
        if (context == null)
            throw new NullPointerException("Null image context");
        ImageContext imageContext = context.as(ImageContext.class);
        Map<String, List<Matrix<? extends PArray>>> sourceImages = newImageMap();
        for (Map.Entry<String, Image2D> entry : source.entrySet()) {
            sourceImages.put(entry.getKey(), entry.getValue().rgbi());
        }
        Map<String, List<Matrix<? extends PArray>>> resultImages = process(sourceImages, additionalData);
        Map<String, Image2D> result = new LinkedHashMap<String, Image2D>();
        for (Map.Entry<String, List<Matrix<? extends PArray>>> entry : resultImages.entrySet()) {
            result.put(entry.getKey(), imageContext.newImage2D(context, entry.getValue()));
        }
        return result;
    }
    */

    public final void cleanupAfterFinish() {
        if (cleanupAtFinish) {
            ExternalProcessor.cleanup();
        }
    }

    protected abstract Map<String, List<Matrix<? extends PArray>>> processImpl(
        ExternalProcessor processor,
        Map<String, List<Matrix<? extends PArray>>> images,
        Object additionalData,
        boolean calledForTile)
        throws IOException, UnstableProcessingError;


    private Map<String, List<Matrix<? extends PArray>>> processComponentwiseIfNecessary(
        Map<String, List<Matrix<? extends PArray>>> source,
        Object additionalData)
        throws IOException
    {
        List<Matrix<? extends PArray>> firstImage = source.isEmpty() ?
            null :
            source.entrySet().iterator().next().getValue();
        if (componentwise) {
            if (firstImage == null) {
                throw new IllegalArgumentException("Cannot process componentwise an empty set of source matrices");
            }
            if (firstImage.isEmpty()) {
                throw new IllegalArgumentException("Cannot process componentwise an empty components set");
            }
            Map<String, List<Matrix<? extends PArray>>> result = newImageMap();
            for (int k = 0, n = firstImage.size(); k < n; k++) {
                ArrayContext ac = this.context == null ? null : this.context.part(k, k + 1, n);
                long t1 = System.nanoTime();
                Map<String, List<Matrix<? extends PArray>>> sourceMono = newImageMap();
                for (Map.Entry<String, List<Matrix<? extends PArray>>> entry : source.entrySet()) {
                    String key = entry.getKey();
                    List<Matrix<? extends PArray>> image = entry.getValue();
                    if (image.isEmpty()) {
                        throw new IllegalArgumentException("Cannot process componentwise an empty components set");
                    }
                    Matrix<? extends PArray> correspondingBand = image.get(k < image.size() ? k : 0);
                    sourceMono.put(key, Matrices.several(PArray.class, correspondingBand));
                }
                Matrix<? extends PArray> firstMatrix = firstImage.get(k);
                Matrix.ContinuationMode continuationMode = getTilingContinuationMode(k, firstMatrix);
                Map<String, List<Matrix<? extends PArray>>> resultMono = processWithTilingIfNecessary(ac,
                    continuationMode,
                    sourceMono,
                    additionalData);
                for (Map.Entry<String, List<Matrix<? extends PArray>>> entry : resultMono.entrySet()) {
                    String key = entry.getKey();
                    List<Matrix<? extends PArray>> image = entry.getValue();
                    if (image.isEmpty()) {
                        throw new IllegalArgumentException("Cannot use componentwise an empty result components set");
                    }
                    List<Matrix<? extends PArray>> resultImage = result.get(key);
                    if (resultImage == null) {
                        resultImage = new ArrayList<Matrix<? extends PArray>>();
                        result.put(key, resultImage);
                    }
                    resultImage.add(image.get(0)); // component #0 from possibly multi-component (color) result
                }
                if (ac != null) {
                    ac.checkInterruptionAndUpdateProgress(firstMatrix.elementType(),
                        firstMatrix.size(), firstMatrix.size());
                }
                long t2 = System.nanoTime();
                if (Arrays.SystemSettings.profilingMode()) {
                    LOGGER.config(String.format(Locale.US,
                        "  Component #%d processed by an external program in %.3f ms"
                            + (tiling ? " with tiling (continuation: " + continuationMode + ")" : ""),
                        k, (t2 - t1) * 1e-6));
                }
            }
            return result;
        } else {
            return processWithTilingIfNecessary(
                this.context,
                firstImage == null || firstImage.isEmpty() ?
                    this.tilingContinuationMode :
                    getTilingContinuationMode(0, firstImage.get(0)),
                source,
                additionalData);
        }
    }

    private Map<String, List<Matrix<? extends PArray>>> processWithTilingIfNecessary(
        ArrayContext context,
        Matrix.ContinuationMode continuationMode,
        Map<String, List<Matrix<? extends PArray>>> source,
        Object additionalData)
        throws IOException
    {
        if (tiling) {
            TilingExternalUtilityProcessor tiledProcessor = new TilingExternalUtilityProcessor(additionalData);
            Map<StringAndIndexPair, Matrix<?>> dest = new LinkedHashMap<StringAndIndexPair, Matrix<?>>();
            Map<StringAndIndexPair, Matrix<?>> src = new LinkedHashMap<StringAndIndexPair, Matrix<?>>();
            imagesToMatrices(src, source);
            getTiler(context, continuationMode).tile(tiledProcessor).process(dest, src);
            return matricesToImages(dest);
        } else {
            ExternalProcessor processor = getProcessor();
            try {
                return processImpl(processor, source, additionalData, false);
            } finally {
                processor.close();
            }
        }
    }

    private static void imagesToMatrices(
        Map<StringAndIndexPair, Matrix<?>> result,
        Map<String, List<Matrix<? extends PArray>>> images)
    {
        for (Map.Entry<String, List<Matrix<? extends PArray>>> entry : images.entrySet()) {
            String key = entry.getKey();
            List<Matrix<? extends PArray>> image = entry.getValue();
            for (int k = 0, n = image.size(); k < n; k++) {
                result.put(new StringAndIndexPair(key, k), image.get(k));
            }
        }
    }

    private static Map<String, List<Matrix<? extends PArray>>> matricesToImages(
        Map<StringAndIndexPair, Matrix<?>> matrices)
    {
        Map<String, List<Matrix<? extends PArray>>> result =
            new LinkedHashMap<String, List<Matrix<? extends PArray>>>();
        for (Map.Entry<StringAndIndexPair, Matrix<?>> entry : matrices.entrySet()) {
            StringAndIndexPair key = entry.getKey();
            Matrix<?> m = entry.getValue();
            if (m == null) {
                continue;
            }
            List<Matrix<? extends PArray>> image = result.get(key.s);
            if (image == null) {
                image = new ArrayList<Matrix<? extends PArray>>();
                result.put(key.s, image);
            }
            while (image.size() <= key.index) {
                image.add(null);
            }
            image.set(key.index, m.cast(PArray.class));
        }
        return result;
    }

    static Object stringToJSON(Object jsonObjectOrString) {
        if (jsonObjectOrString == null) {
            throw new NullPointerException("Null jsonObjectOrString");
        }
        try {
            if (jsonObjectOrString instanceof String) {
                return jsonClass.getConstructor(String.class).newInstance(jsonObjectOrString);
            } else {
                if (!jsonClass.isInstance(jsonObjectOrString)) {
                    throw new IllegalArgumentException("Invalid class of jsonObjectOrString: "
                        + jsonObjectOrString.getClass() + " (only String and JSONObject allowed)");
                }
                return jsonObjectOrString;
            }
        } catch (InvocationTargetException e) {
            throw (AssertionError) new AssertionError("Unexpected error while using org.json.JSONObject").initCause(e);
        } catch (NoSuchMethodException e) {
            throw (AssertionError) new AssertionError("Unexpected error while using org.json.JSONObject").initCause(e);
        } catch (InstantiationException e) {
            throw (AssertionError) new AssertionError("Unexpected error while using org.json.JSONObject").initCause(e);
        } catch (IllegalAccessException e) {
            throw (AssertionError) new AssertionError("Unexpected error while using org.json.JSONObject").initCause(e);
        }
    }

    private class TilingExternalUtilityProcessor
        extends AbstractArrayProcessorWithContextSwitching
        implements ApertureProcessor<StringAndIndexPair>
    {
        final IRectangularArea dependenceAperture;
        final Object additionalData;

        public TilingExternalUtilityProcessor(Object additionalData) {
            super(null); // will be replaced by the tiler
            this.additionalData = additionalData;
            this.dependenceAperture = getTileOverlapAperture();
        }

        public void process(Map<StringAndIndexPair, Matrix<?>> dest, Map<StringAndIndexPair, Matrix<?>> src) {
//            TiledApertureProcessorFactory.TileInformation info = (TiledApertureProcessorFactory.TileInformation)
//                context().customData();
//            System.out.printf("Processing %s (thread #%d, tile %s, extended tile %s)%n",
//                src, context().currentThreadIndex(),
//                info.getTile(), info.getExtendedTile());
            for (int attempt = 0; ; ) {
                ExternalProcessor processor = getProcessor();
                try {
                    imagesToMatrices(
                        dest,
                        ExternalAlgorithmCaller.this.processImpl(
                            processor,
                            matricesToImages(src),
                            additionalData,
                            true));
                    return; // in usual situations no, any loop
                } catch (UnstableProcessingError e) {
                    ++attempt;
                    LOGGER.log(Level.SEVERE, "Unstable error occurred while attempt #" + attempt
                        + "/" + NUMBER_OF_ATTEMPTS_TO_RECOVER_UNSTABLE_ERROR + " to process a tile; "
                        + "processed by: " + processor
                        + "; source matrices: " + src + "; destination matrices: " + src, e);
                    if (attempt >= NUMBER_OF_ATTEMPTS_TO_RECOVER_UNSTABLE_ERROR) {
                        throw new TileProcessingException(e);
                    } // else continue the loop with a new processor
                } catch (RuntimeException e) {
                    throw e; // important not to convert InterruptionException into IOError
                } catch (Exception e) {
                    throw new TileProcessingException(e);
                } finally {
                    processor.close();
                }
            }
        }

        public IRectangularArea dependenceAperture(StringAndIndexPair srcMatrixKey) {
            return dependenceAperture;
        }
    }

    private static class StringAndIndexPair {
        final String s;
        final int index;

        private StringAndIndexPair(String s, int index) {
            if (s == null) {
                throw new NullPointerException("Null string key");
            }
            this.s = s;
            this.index = index;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StringAndIndexPair that = (StringAndIndexPair) o;
            if (index != that.index) {
                return false;
            }
            if (!s.equals(that.s)) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int result = s.hashCode();
            result = 31 * result + index;
            return result;
        }

        @Override
        public String toString() {
            return "StringAndIndex{" +
                "s='" + s + '\'' +
                ", index=" + index +
                '}';
        }
    }

    /**
     * Can be thrown by {@link ExternalAlgorithmCaller#processImpl}
     * to indicate that this class should repeat an attempt to process the tile several times.
     */
    public static class UnstableProcessingError extends Error {
        public UnstableProcessingError() {
        }

        public UnstableProcessingError(String message) {
            super(message);
        }

        public UnstableProcessingError(String message, Throwable cause) {
            super(message, cause);
        }

        private static final long serialVersionUID = 7953030047937421334L;
    }

    /**
     * Thrown by this class in a case of any checked exception while processing a tile.
     */
    public static class TileProcessingException extends RuntimeException {
        public TileProcessingException(Throwable cause) {
            super(cause);
        }

        private static final long serialVersionUID = 7909205853651982341L;
    }

}
