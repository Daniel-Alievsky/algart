/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2018 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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
import net.algart.contexts.Context;
import net.algart.math.Point;
import net.algart.math.patterns.Patterns;
import net.algart.matrices.morphology.BasicRankMorphology;
import net.algart.matrices.morphology.RankMorphology;
import net.algart.matrices.morphology.RankPrecision;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.image.BufferedImageOp;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class ExternalJavaUtilityCaller extends ExternalUtilityCaller {
    private final String mainClassName;
    private final List<String> specificJVMOptions; // added to customJVMOptions
    private volatile File customJREHome;
    private volatile File javaExecutable;
    private volatile List<String> customJVMOptions;

    public ExternalJavaUtilityCaller(
        Context context,
        String mainClassName,
        List<String> startingArguments,
        List<String> specificJVMOptions,
        String settings,
        List<String> inputArgNames,
        List<String> outputArgNames,
        Object jsonSys)
        throws IOException
    {
        super(context,
            "java", // not used
            startingArguments,
            settings,
            inputArgNames,
            outputArgNames,
            jsonSys);
        if (mainClassName == null)
            throw new NullPointerException("Main class name is not specified");
        if (specificJVMOptions == null)
            throw new NullPointerException("JVM options are not specified. "
                + "At least, -classpath or -jar should be specified");
        if (inputArgNames == null)
            throw new NullPointerException("Input argument names are not specified");
        if (outputArgNames == null)
            throw new NullPointerException("Output argument names are not specified");
        this.mainClassName = mainClassName.trim();
        this.specificJVMOptions = new ArrayList<String>(specificJVMOptions);
        setCustomJRE(null); // initializes customJREHome, javaExecutable, customJVMOptions
        setAlgorithmCode("jeu");
    }

    public final File getCustomJREHome() {
        return customJREHome;
    }

    public final File getJavaExecutable() {
        return javaExecutable;
    }

    public final void setCustomJREHome(File customJREHome) throws FileNotFoundException {
        this.customJREHome = customJREHome;
        this.javaExecutable = ExternalProcessor.getJavaExecutable(customJREHome);
    }

    public final List<String> getCustomJVMOptions() {
        return new ArrayList<String>(customJVMOptions);
    }

    public final void setCustomJVMOptions(List<String> customJVMOptions) {
        this.customJVMOptions = customJVMOptions == null ? null : new ArrayList<String>(customJVMOptions);
    }

    public final void setCustomJRE(String jreName) throws FileNotFoundException {
        setCustomJREHome(ExternalProcessor.getCustomJREHome(jreName));
        setCustomJVMOptions(ExternalProcessor.getCustomJVMOptions(jreName));
    }

    @Override
    public void execute(ExternalProcessor processor) throws IOException {
        List<String> command = new ArrayList<String>();
        command.add(javaExecutable.getAbsolutePath());
        if (customJVMOptions != null) {
            command.addAll(customJVMOptions);
        }
        command.addAll(specificJVMOptions);
        command.add(mainClassName);
        command.addAll(getStartingArguments());
        command.addAll(getMainArguments(processor));
        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processor.execute(processBuilder);
    }

    static class Test {
        public static void main(String[] args) throws IOException {
            if (args.length < 6) {
                System.err.println("Test Java utility for calling via " + ExternalJavaUtilityCaller.class);
                System.err.println("This utility can be called from SIMAGIS table "
                    + " via simagis.ExternalUtility plugin");
                System.err.println("Usage:");
                System.err.println("    " + Test.class.getName()
                    + " algart|awt N I settings.cfg arg1 arg2 ... argN result");
                System.err.println("N is the number of arguments, I is the index of processed argument (1..N)");
                System.err.println("settings.cfg must contain the property:");
                System.err.println("aperture=xx - the size of aperture");
                System.exit(1);
            }
            int numberOfInputArguments = Integer.parseInt(args[1]);
            int index = Integer.parseInt(args[2]) - 1;
            if (index < 0 || index >= numberOfInputArguments)
                throw new IndexOutOfBoundsException(index + " is out of range 1.." + numberOfInputArguments);
            Properties properties = new Properties();
            String settingsFile = args[3];
            properties.load(new FileInputStream(settingsFile));
            int aperture = Integer.parseInt(properties.getProperty("aperture"));
            if (aperture < 0)
                throw new IllegalArgumentException("Negative or zero aperture");
            File inputFile = new File(args[4 + index]);
            File outputFile = new File(args[4 + numberOfInputArguments]);

            System.out.println("Current directory: " + new File("").getAbsolutePath());
            if (args[0].equals("algart")) {
                System.out.println("Recommended number of processor units: " + Arrays.SystemSettings.cpuCount());
                List<Matrix<? extends PArray>> image = ExternalAlgorithmCaller.readAlgARTImage(inputFile);
                RankMorphology morph = BasicRankMorphology.getInstance(null, 0.5, RankPrecision.BITS_16);
                List<Matrix<? extends PArray>> result = new ArrayList<Matrix<? extends PArray>>();
                // simple processing: median
                for (Matrix<? extends PArray> m : image) {
                    Matrix<? extends UpdatablePArray> r = morph.dilation(
                        m, Patterns.newSphereIntegerPattern(Point.origin(m.dimCount()), 0.5 * aperture));
                    result.add(r);
                }
                ExternalAlgorithmCaller.clearAlgARTImageTemporaryStatus(result);
                ExternalAlgorithmCaller.writeAlgARTImage(outputFile, result, true);

            } else if (args[0].equals("awt")) {
                int p = outputFile.getName().lastIndexOf('.');
                if (p == -1)
                    throw new IllegalArgumentException("Cannot write image in a file without extension");
                String outputFormat = outputFile.getName().substring(p + 1);
                float[] kernel = new float[aperture * aperture];
                java.util.Arrays.fill(kernel, 1.0f / (float) aperture / (float) aperture);
                BufferedImage image = ImageIO.read(inputFile);
                // simple processing: blur
                BufferedImageOp op = new ConvolveOp(new Kernel(aperture, aperture, kernel));
                BufferedImage result = op.filter(image, null);
                ImageIO.write(result, outputFormat, outputFile);
            } else
                throw new IllegalArgumentException("Unknown command " + args[0]);
        }
    }
}
