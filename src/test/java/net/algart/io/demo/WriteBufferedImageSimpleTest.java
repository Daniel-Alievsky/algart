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

import javax.imageio.IIOException;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Path;

public class WriteBufferedImageSimpleTest {
    public static void main(String[] args) throws IOException {
        if (args.length < 2) {
            System.out.printf("Usage: %s target_image_1.jpg/png/bmp target_image_2.jpg/png/bmp%n",
                    WriteBufferedImageSimpleTest.class.getName());
            System.out.println("Please try also incorrect extension or .jp2 extension (JPEG-2000) " +
                    "to compare ImageIO.write and MatrixIO.writeBufferedImage.");
            System.out.println("To test .jp2, you must also enable jai-imageio-jpeg2000 in pom.xml.");
            return;
        }
        final Path targetFile1 = Path.of(args[0]);
        final Path targetFile2 = Path.of(args[1]);
        BufferedImage bi = new BufferedImage(1000, 1000, BufferedImage.TYPE_3BYTE_BGR);
        // - note: TYPE_INT_BGR is not processed correctly by jai-imageio-jpeg2000, version 1.4.0
        AWT2MatrixTest.drawTextOnImage(bi);

        System.out.println("Writing buffered image to " + targetFile1 + ": AWT way...");
        if (!ImageIO.write(bi, MatrixIO.extension(targetFile1), targetFile1.toFile())) {
            throw new IIOException("Cannot write " + targetFile1);
        }

        System.out.println("Writing buffered image to " + targetFile2 + ": AlgART way...");
        MatrixIO.writeBufferedImage(targetFile2, bi, p -> MatrixIO.setQuality(p, 0.05));
        // MatrixIO.writeBufferedImage(targetFile2, bi); // - such a call is also possible
        System.out.printf("Writing %s%n", targetFile2);
    }
}
