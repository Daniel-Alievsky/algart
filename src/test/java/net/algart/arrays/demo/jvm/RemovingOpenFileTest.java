/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2026 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.arrays.demo.jvm;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;

public class RemovingOpenFileTest {
    public static void main(String[] args) throws IOException {
        if (args.length == 0) {
             System.out.printf("Usage: %s some_temp_tile%n", RemovingOpenFileTest.class.getName());
             return;
        }
        Path file = Paths.get(args[0]);
        RandomAccessFile randomAccessFile = new RandomAccessFile(file.toFile(), "rwd");
        System.out.printf("Opened; file existence: %s%n", Files.exists(file));
        System.out.printf("Removing file %s%n", file);

        try {
            Files.delete(file);
            System.out.printf("File %s was deleted%n", file);
        } catch (IOException e) {
            System.out.printf("Cannot delete file %s: %s%n", file, e);
        }
        randomAccessFile.close();
        System.out.println("Closed");
        Files.delete(file);
        System.out.printf("File %s was deleted%n", file);
        System.out.printf("File existence: %s%n", Files.exists(file));
        System.out.println();

        FileChannel channel = FileChannel.open(file,
                StandardOpenOption.READ,
                StandardOpenOption.WRITE,
                StandardOpenOption.CREATE,
//                ExtendedOpenOption.NOSHARE_DELETE, // - does not help
                StandardOpenOption.DELETE_ON_CLOSE);
        System.out.printf("Opened; file existence: %s%n", Files.exists(file));

        try {
            Files.delete(file);
            System.out.printf("File %s was deleted%n", file);
        } catch (IOException e) {
            System.out.printf("Cannot delete file %s: %s%n", file, e);
        }
        System.out.printf("File existence: %s%n", Files.exists(file));
        channel.close();
        System.out.printf("Closed; file existence: %s%n", Files.exists(file));
    }

}
