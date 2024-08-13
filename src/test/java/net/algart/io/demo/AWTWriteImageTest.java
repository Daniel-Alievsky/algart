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

package net.algart.io.demo;

import net.algart.io.MatrixIO;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageOutputStream;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Iterator;

public class AWTWriteImageTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 1) {
            System.out.printf("Usage: " +
                            "%s target_image.jpg/png/bmp%n",
                    AWTWriteImageTest.class);
            return;
        }
        File targetFile = new File(args[0]);

        BufferedImage bi = new BufferedImage(1000, 1000, BufferedImage.TYPE_INT_BGR);
        AWT2MatrixTest.drawTextOnImage(bi);

        final Iterator<ImageWriter> writers = ImageIO.getImageWritersBySuffix(MatrixIO.extension(targetFile));
        System.out.println("Writers:");
        ImageWriter writer = null;
        while (writers.hasNext()) {
            writer = writers.next();
            System.out.println(writer);
            // - we will use the last one
        }
        if (writer == null) {
            throw new IOException("Cannot write " + targetFile);
        }
        System.out.println();

        //noinspection ResultOfMethodCallIgnored
        targetFile.delete();
        try (ImageOutputStream ios = ImageIO.createImageOutputStream(targetFile)) {
            writer.setOutput(ios);
            final ImageWriteParam writeParam = writer.getDefaultWriteParam();
            writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            String[] types = writeParam.getCompressionTypes();
            System.out.printf("Compression types: %s%n", Arrays.toString(types));
            System.out.printf("Default compression type: %s%n", writeParam.getCompressionType());
            if (writeParam.getCompressionType() == null && types.length > 0) {
                writeParam.setCompressionType(types[0]);
                System.out.printf("Try to set to the 1st one: %s%n", writeParam.getCompressionType());
            }
            writeParam.setCompressionQuality(0.1f);
            final IIOImage iioImage = new IIOImage(bi, null, null);
            writer.write(null, iioImage, writeParam);
        }
        System.out.printf("Written to %s%n", targetFile);
    }
}
