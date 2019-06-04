/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.finalizing.demo;

import java.io.*;
import java.util.*;

/**
 * <p><i>TempFiles</i>: a tool for creating temporary files and deleting them before exiting program.</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.6
 */
class TempFiles {
    private TempFiles() {}

    public static void deleteOnExit(File file) {
        if (cleaner == null)
            cleaner = new Cleaner();
        cleaner.deleteOnShutdown(file);
    }

    public static boolean deleteRecursive(File fileOrDir) {
        String[] fileNames = fileOrDir.list();
        if (fileNames != null) {
            for (String name : fileNames) {
                deleteRecursive(new File(fileOrDir.getPath(), name));
            }
        }
        return fileOrDir.delete();
    }

    public static String tempDirectory() throws SecurityException {
        return System.getProperty("java.io.tmpdir");
    }

    private static Cleaner cleaner = null;
    private static class Cleaner {
        private Set<File> filesSet = new HashSet<File>();
        private List<File> filesList = new ArrayList<File>();
        private synchronized void deleteOnShutdown(File file) {
            if (!filesSet.contains(file)) {
                filesList.add(file);
                filesSet.add(file);
            }
        }

        {
            Runtime.getRuntime().addShutdownHook(new Thread() {
                public void run() {
                    cleanup();
                }
            });
        }

        private synchronized void cleanup() {
            if (filesList == null)
                return;
            try {
                for (int k = filesList.size() - 1; k >= 0; k--) {
                    File file = filesList.get(k);
                    if (!file.exists()) {
                        System.out.println("No file " + file.getAbsolutePath() + " for deleting");
                    } else if (deleteRecursive(file)) {
                        System.out.println("File " + file.getAbsolutePath()
                            + " is successfully deleted");
                    } else {
                        System.out.println("Cannot delete file: " + file.getAbsolutePath());
                    }
                }
            } finally {
                filesList = null;
            }
        }
    }
}
