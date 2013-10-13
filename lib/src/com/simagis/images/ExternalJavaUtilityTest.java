package com.simagis.images;

import net.algart.arrays.*;
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

class ExternalJavaUtilityTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 6) {
            System.err.println("Test Java utility for calling via " + ExternalJavaUtilityCaller.class);
            System.err.println("Usage:");
            System.err.println("    " + ExternalJavaUtilityTest.class.getName()
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
                Matrix<? extends UpdatablePArray> r = morph.dilation(m, Patterns.newSphereIntegerPattern(Point.origin(m.dimCount()), 0.5 * aperture));
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
