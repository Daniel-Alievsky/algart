package net.algart.finalizing.demo;

import java.io.*;
import java.util.*;

/**
 * <p><i>TempFiles</i>: a tool for creating temporary files and deleting them before exiting program.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
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
