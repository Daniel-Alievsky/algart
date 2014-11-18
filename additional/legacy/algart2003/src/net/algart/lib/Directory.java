/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001-2003 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.lib;

import java.io.*;

/**
 * <p>A simple inheritor of standard <code>java.io.File</code>
 * for representing directories. Also contains some static methods
 * for manipulating directories in addition to standard
 * <code>java.io.File</code> methods.
 *
 * @author  Daniel Alievsky
 * @version 1.0
 */


public final class Directory extends File implements TrueStatic {
// TrueStatic is useful to save time because the returned tempDirectory() should be global

  /**
   * The global property returned by {@link GlobalProperties#getClassDebugLevel(Class)
   * GlobalProperties.getClassDebugLevel(Directory.class)} call:
   * if it contains positive value, this class will print some debug information
   * (by {@link Out#println(String)} calls).
   */
  public static final int DEBUG_LEVEL = GlobalProperties.getClassDebugLevel(Directory.class);

  /**
   *
   * @param path
   * @throws IllegalArgumentException
   */
  public Directory(String path) throws IllegalArgumentException {
    super(path);
    if (!isDirectory()) throw new IllegalArgumentException(Directory.class + " can be instantiated for existing directories only");
  }

  public static File valueOf(File file) {
    if (file == null) throw new NullPointerException("Null file argument in " + Directory.class.getName() + ".valueOf method");
    if (file instanceof Directory) return file;
    if (!file.isDirectory()) return file;
    return new Directory(file.getPath());
  }
  public static File valueOf(String path) {
    if (path == null) throw new NullPointerException("Null path argument in " + Directory.class.getName() + ".valueOf method");
    File file = new File(path);
    if (!file.isDirectory()) return file;
    return new Directory(path);
  }

  private static Directory tempDirectory = null;
  public static File tempDirectory() {
    if (tempDirectory != null) return tempDirectory;
    try {
      String s = (String)java.security.AccessController.doPrivileged(
      // doPriviliged allows building more flexible security control:
      // the clients of this class can have less permissions
        new java.security.PrivilegedAction() {
          public Object run() {
            return System.getProperty("java.io.tmpdir");
          }
       });
      if (s != null) return tempDirectory = new Directory(s);
    } catch (Exception e) {
      // if any problems occurred, including in Directory constructor, try to use the alternate branch
      if (DEBUG_LEVEL >= 1) Out.println(" **Directory** Warning: cannot determine temp directory on the base of java.io.tmpdir system property: " + e);
    }

    // The following branch is necessary for a case of future change of
    // the name of undocumented system property "java.io.tmpdir"
    try {
      File testTempFile = File.createTempFile("gettempdir",null);
      try {
        if (DEBUG_LEVEL >= 1) Out.println(" **Directory** Detecting temporary directory on the base of temporary file " + testTempFile);
        return tempDirectory = new Directory(testTempFile.getParent());
      } finally {
        boolean deleted = false;
        try {
          deleted = testTempFile.delete();
        } catch (Exception e) {
        }
        if (DEBUG_LEVEL >= 1 && !deleted) Out.println(" **Directory** Warning: cannot delete temporary file " + testTempFile);
      }
    } catch (IOException e) {
      throw (IllegalStateException)new IllegalStateException(Directory.class.getName() + ".tempDirectory() cannot determine temporary directory: " + e).initCause(e);
    }
  }

  public static Directory createTempDirectory(String prefix, String suffix) throws IOException {
    return createTempDirectory(prefix,suffix,null);
  }
  public static Directory createTempDirectory(String prefix, String suffix, File directory) throws IOException {
    if (prefix == null) throw new NullPointerException("Null prefix argument in " + Directory.class.getName() + ".createTempDirectory method");
    if (prefix.length() < 3) throw new IllegalArgumentException("Too short prefix argument in " + Directory.class.getName() + ".createTempDirectory method: it's length must be >=3");
    String s = suffix == null ? ".tmp" : suffix;
    synchronized(tmpDirectoryLock) {
      if (directory == null) directory = tempDirectory();
      File f = null;
      for (int m = 0; m < 20; m++) {
        for (int k = 0; k < 1000; k++) {
          f = generateDir(prefix,suffix,directory);
          if (!f.exists())
            if (f.mkdir()) {
              File d = Directory.valueOf(f);
              if (d instanceof Directory) { // - to be on the safe side
                if (DEBUG_LEVEL >= 1) Out.println(" **Directory** " + f + " temp directory created");
                return (Directory)d;
              }
            }
            break;
        }
        if (DEBUG_LEVEL >= 2) Out.println(" **Directory** Attempt #" + m + " to create temp directory failed (" + f + ")");
        counter = new java.util.Random(counter).nextInt() & 0xFFFFFF;
      }
      throw new IOException("Unable to create temporary directory");
    }
  }
  private static final Object tmpDirectoryLock = new Object();
  private static int counter = -1; /* Protected by tmpFileLock */
  private static File generateDir(String prefix, String suffix, File dir) {
    if (counter == -1) counter = new java.util.Random().nextInt() & 0xFFFFFF;
    counter++;
    return new File(dir,prefix+counter+suffix);
  }
}