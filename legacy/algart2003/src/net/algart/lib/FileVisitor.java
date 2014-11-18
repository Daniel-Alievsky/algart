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
import java.util.*;

/**
 * <p>Implementation of <i>visitor</i> pattern for scanning
 * all files in some directory.
 *
 * @author  Daniel Alievsky
 * @version 1.0
 */

public abstract class FileVisitor {

  public static final int DEBUG_LEVEL = GlobalProperties.getClassDebugLevel(FileVisitor.class);

  private final File root;
  private Directory parent;
  private List levelsPrivate = new ArrayList();

  /**
   * Inheritors should call this constructor with the root of the file tree
   * that should be scanned.
   *
   * @param root  the root of file tree that should be scanned
   * @throws NullPointerException if the argument is null
   */
  protected FileVisitor(File root) {
    if (root == null) throw new NullPointerException("Null root argument in " + FileVisitor.class.getName() + " constructor");
    if (root.getPath().equals("")) root = new Directory(".");
    this.root = Directory.valueOf(root);
    this.parent = null;
  }

  /**
   * Returns the root of file tree that is scanned by this visitor.
   *
   * @return   the root of file tree that is scanned by this visitor
   */
  public final File root() {
    return this.root;
  }
  /**
   * Returns the current subdirectory that is scanned by this visitor now.
   * It is changed while the visitor scans subdirectories recursively.
   *
   * @return   the current subdirectory that is scanned by this visitor now.
   */
  protected final Directory parent() {
    return this.parent;
  }

  private int depth = 0;
  protected final int depth() {
    return depth;
  }
  protected final Directory[] getLevels() {
    return (Directory[])levelsPrivate.toArray(new Directory[0]);
  }

  FileFilter fileFilter = null;

  protected abstract void visitFile(File f) throws IOException;
  protected abstract Object visitDirectoryBefore(Directory d) throws IOException;
  protected abstract void visitDirectoryAfter(Directory d, Object savedContext) throws IOException;
  protected void visitRootAfter() throws IOException {
  }
  protected boolean executeAlternate() throws IOException {
    return false;
  }

  private boolean executeWasCalled = false;
  private boolean executeInProgress = false;
  public final void execute() throws IOException {
    checkWhetherExecuteWasCalled();
    executeWasCalled = true;
    executeInProgress = true;
    try {
      if (!root.exists()) return;
      if (executeAlternate()) return;
      executePrivate(root);
      visitRootAfter();
    } finally {
      executeInProgress = false;
    }
  }
  void checkWhetherExecuteWasCalled() {
    if (executeWasCalled) throw new IllegalStateException(getClass().getName() + " method can be called only once for the given instance \"" + toString() + "\"");
  }
  public final boolean executeWasCalled() {
    return executeWasCalled;
  }
  public final boolean executeInProgress() {
    return executeInProgress;
  }
  private void executePrivate(File f) throws IOException {
    if (fileFilter != null && !fileFilter.accept(f))
      return;
    if (!(f instanceof Directory)) {
      visitFile(f);
    } else {
      Object stateSave = visitDirectoryBefore((Directory)f);
      Directory parentSave = parent;
      parent = (Directory)f;
      levelsPrivate.add(parent);
      depth++;
      try {
        File[] files = parent.listFiles();
        if (files != null)
          for (int k = 0; k < files.length; k++) {
            executePrivate(Directory.valueOf(files[k]));
          }
      } finally {
        depth--;
        levelsPrivate.remove(depth);
        parent = parentSave;
        if (levelsPrivate.size() != depth) throw new AssertionError("Internal problem in " + FileVisitor.class.getName() + ".execute method");
      }
      visitDirectoryAfter((Directory)f,stateSave);
    }
  }


  /**
   * Returns a string representation of this object.
   * This method is intended to be used only for debugging purposes,
   * and the content and format of the returned string may vary between
   * implementations.
   * The returned string may be empty but may not be <code>null</code>.
   *
   * @return  A string representation of this object
   */
  public String toString() {
    return "File visitor \"" + getClass().getName() + "\" for root " + root;
  }

  /**
   * Returns the hash code for this object.
   * @return  A hash code for this object
   */
  public int hashCode() {
    return (root.hashCode()) ^ CLASS_HASH_CODE;
  }

  /**
   * Determines whether or not two objects are equal.
   * Two instances are equal if all arguments passed to their
   * constructors were equal.
   *
   * @param obj   An object to be compared with this instance
   * @return      <code>true</code> if the argument has the same type FileVisitor and is equal to this instance
   */
  public boolean equals(Object obj) {
    if (obj == null || obj.getClass() != getClass()) return false;
    FileVisitor o = (FileVisitor)obj;
    return root.equals(o.root);
  }

  private static final int CLASS_HASH_CODE = FileVisitor.class.getName().hashCode();


  public static abstract class Filtered extends FileVisitor {
    protected Filtered(File root) {
      super(root);
    }
    protected final FileFilter fileFilter() {
      return this.fileFilter;
    }
    public final void execute(FileFilter fileFilter) throws IOException {
      checkWhetherExecuteWasCalled();
      this.fileFilter = fileFilter;
      execute();
    }
  }

  public interface Listable {
    public List getFiles();
  }
  public static class Lister extends Filtered implements Listable {
    private List files = new ArrayList();
    public Lister(File root) {
      super(root);
    }
    public List getFiles() {
      if (!executeWasCalled()) throw new IllegalStateException(Lister.class.getName() + ".getFiles() method should not be called before the first execute() call");
      return Collections.unmodifiableList(files);
    }
    protected final void addToList(File f) {
      if (!executeInProgress()) throw new IllegalStateException(Lister.class.getName() + ".addToList() method should be called only while performing execute()");
      files.add(f);
    }
    protected void visitFile(File f) throws IOException {
      addToList(f);
    }
    protected Object visitDirectoryBefore(Directory d) throws IOException {
      addToList(d); return null;
    }
    protected void visitDirectoryAfter(Directory d, Object savedContext) throws IOException {
    }
  }

  static class FilePair {
    final File source;
    final File target;
    FilePair(File source, File target) {
      this.source = source;
      this.target = target;
    }
  }

  public static class Copier extends Lister {
    private List pairs = new ArrayList();
    private final File rootTarget;
    private File parentTarget;
    public Copier(File rootSource, File rootTarget) {
      super(rootSource);
      if (rootTarget == null) throw new NullPointerException("Null rootTarget argument in " + Copier.class.getName() + " constructor");
      if (rootTarget.getPath().equals("")) rootTarget = new Directory(".");
      this.rootTarget = rootTarget;
      this.parentTarget = null;
    }
    public final File rootSource() {
      return root();
    }
    public final File rootTarget() {
      return rootTarget;
    }

    private boolean overwrite = false;
    public final void setOverwrite(boolean value) {
      overwrite = value;
    }
    public final boolean getOverwrite() {
      return overwrite;
    }

    protected void visitFile(File f) throws IOException {
      addToList(f);
      File target = depth() == 0? rootTarget: new File(parentTarget,f.getName());
      pairs.add(new FilePair(f,target));
      if (DEBUG_LEVEL >= 2) Out.println("Scanned file " + f);
    }
    protected final Object visitDirectoryBefore(Directory d) throws IOException {
      addToList(d);
      File parentTargetSave = parentTarget;
      parentTarget = depth() == 0? rootTarget: new File(parentTarget,d.getName());
      pairs.add(new FilePair(d,parentTarget));
      if (DEBUG_LEVEL >= 2) Out.println("Scanned directory " +d);
      return parentTargetSave;
    }
    protected final void visitDirectoryAfter(Directory d, Object savedContext) throws IOException {
      parentTarget = (File)savedContext;
    }
    protected final void visitRootAfter() throws IOException {
      List written = new ArrayList();
      try {
        for (int k = 0, n = pairs.size(); k < n; k++) {
          FilePair p = (FilePair)pairs.get(k);
          if (p.source instanceof Directory) {
            if (DEBUG_LEVEL >= 1) Out.println("Creating directory " + p.target);
            if (p.target.mkdir())
              written.add(p.target);
            else
              if (!overwrite) throw new IOException("Cannot create " + p.target + " subdirectory while copying file tree \"" + root() + "\"");
          } else {
            if (overwrite || !p.target.exists()) {
              if (DEBUG_LEVEL >= 1) Out.println("Copying file " + p.source + " to " + p.target);
              BinaryIO.copy(p.source,p.target);
              written.add(p.target);
            }
          }
        }
      } catch (IOException e) {
        try {
          for (int n = written.size(), k = n-1; k >= 0; k--) {
            File f = (File)written.get(k);
            if (DEBUG_LEVEL >= 1) Out.println("Back removing " + f);
            if (!f.delete())
              if (DEBUG_LEVEL >= 1) Out.println("Cannot remove " + f);
          }
        } catch (Exception e1) {
          // ignoring removing errors: they are not so important as copying errors
        }
        throw e;
      }
    }
  }

  public static final class Mover extends FileVisitor {
    private List pairs = new ArrayList();
    private final File rootTarget;
    private File parentTarget;
    public Mover(File rootSource, File rootTarget) {
      super(rootSource);
      if (rootTarget == null) throw new NullPointerException("Null rootTarget argument in " + Mover.class.getName() + " constructor");
      if (rootTarget.getPath().equals("")) rootTarget = new Directory(".");
      this.rootTarget = rootTarget;
      this.parentTarget = null;
    }
    public final File rootSource() {
      return root();
    }
    public final File rootTarget() {
      return rootTarget;
    }
    protected void visitFile(File f) throws IOException {
      File target = depth() == 0? rootTarget: new File(parentTarget,f.getName());
      pairs.add(new FilePair(f,target));
      if (DEBUG_LEVEL >= 2) Out.println("Scanned file " + f);
    }
    protected final Object visitDirectoryBefore(Directory d) throws IOException {
      File parentTargetSave = parentTarget;
      parentTarget = depth() == 0? rootTarget: new File(parentTarget,d.getName());
      pairs.add(new FilePair(d,parentTarget));
      if (DEBUG_LEVEL >= 2) Out.println("Scanned directory " +d);
      return parentTargetSave;
    }
    protected final void visitDirectoryAfter(Directory d, Object savedContext) throws IOException {
      parentTarget = (File)savedContext;
    }
    protected final void visitRootAfter() throws IOException {
      int n = pairs.size();
      for (int k = 0; k < n; k++) {
        FilePair p = (FilePair)pairs.get(k);
        if (p.source instanceof Directory) {
          if (DEBUG_LEVEL >= 1) Out.println("Moving directory " + p.source + " to " + p.target);
          if (!p.target.mkdir()) throw new IOException("Cannot create " + p.target + " subdirectory while moving file tree \"" + root() + "\"");
        } else {
          if (!p.source.renameTo(p.target)) {
            if (DEBUG_LEVEL >= 1) Out.println("Moving file " + p.source + " to " + p.target);
            BinaryIO.copy(p.source,p.target);
            if (!p.source.delete()) throw new IOException("Cannot remove " + p.source + " file while moving file tree \"" + root() + "\"");
          } else {
            if (DEBUG_LEVEL >= 1) Out.println("Quick moving file " + p.source + " to " + p.target);
          }
        }
      }
      for (int k = n-1; k >= 0; k--) {
        FilePair p = (FilePair)pairs.get(k);
        if (p.source instanceof Directory) {
          if (!p.source.delete()) throw new IOException("Cannot remove empty " + p.source + " subdirectory while moving file tree \"" + root() + "\"");
        }
      }
    }
    protected final boolean executeAlternate() throws IOException {
      return rootSource().renameTo(rootTarget());
    }
  }

  public static final class Remover extends Lister {
    public Remover(File root) {
      super(root);
    }
    protected final void visitFile(File f) throws IOException {
      addToList(f);
      if (DEBUG_LEVEL >= 1) Out.println("Removing " + f);
      if (!f.delete()) throw new IOException("Cannot remove " + f + " while removing file tree \"" + root() + "\"");
    }
    protected final Object visitDirectoryBefore(Directory d) throws IOException {
      return null;
    }
    protected final void visitDirectoryAfter(Directory d, Object savedContext) throws IOException {
      addToList(d);
      visitFile(d);
    }
  }

  /**
   * This method is an analog of the standard <code>f.deleteOnExit()</code>:
   * it plans removing <code>f</code> on the moment before the shutdown
   * of Java Virtual Machine.
   * But, unlike that method, this one compeletely removes <code>f</code>
   * also in a case when it is a directory: then it is removed recursively
   * including all files and subdirectories.
   *
   * @param f   The file or directory that will be removed before JVM shutdown
   */
  public static void removeOnShutdown(File f) {
    ShutdownActions.getInstance().removeOnShutdown(f);
  }

  private static class ShutdownActions {
    private ShutdownActions() {
      if (DEBUG_LEVEL >= 2) Out.println("ShutdownActions created");
      Runtime.getRuntime().addShutdownHook(new Thread() {
        public void run() {
          perform();
        }
      });
    }
    private static final ShutdownActions instance = new ShutdownActions();
    static ShutdownActions getInstance() {
      return instance;
    }
    private LinkedHashSet removedFiles = new LinkedHashSet();
    private void removeOnShutdown(File file) {
      removedFiles.add(file);
    }
    private synchronized void perform() {
      if (removedFiles.isEmpty()) return;
        // additional check to avoid the second call, to be on the safe side
      try {
        if (DEBUG_LEVEL >= 1) Out.println("Removing " + removedFiles.size() + " files/subdirectories on shutdown");
        File[] files = (File[])removedFiles.toArray(new File[0]);
        for (int k = files.length - 1; k >= 0; k--) {
          try {
            new Remover(files[k]).execute();
          } catch(IOException e) {
          }
          if (files[k].exists()) Out.println("Cannot remove " + files[k] + " on shutdown");
        }
      } finally {
        removedFiles.clear();
      }
    }
  }

  private static class FileFilterRE implements FileFilter {
    final java.util.regex.Pattern rePosForFile, reNeg;
    FileFilterRE(String rePosForFile, String reNeg) {
      this.rePosForFile = rePosForFile == null? null: java.util.regex.Pattern.compile(rePosForFile);
      this.reNeg = reNeg == null? null: java.util.regex.Pattern.compile(reNeg);
    }
    public boolean accept(File f) {
      if (rePosForFile != null && !(f instanceof Directory)) {
        if (!rePosForFile.matcher(f.getPath()).find()) return false;
      }
      if (reNeg != null)
        if (reNeg.matcher(f.getPath()).find()) return false;
      return true;
    }
    static FileFilterRE valueOf(String[] argv, int indexStart) {
      if (indexStart >= argv.length) return null;
      return new FileFilterRE(argv[indexStart],
        indexStart+1 < argv.length? argv[indexStart+1]: null);
    }
  }
  public static void main(String[] argv) {
    if (argv.length <= 1) {
      Out.println("Usage:\n"
        + "  FileVisitor list subdirectory [RegExpPosition [RegExpNegative]]\n"
        + "  FileVisitor copy [-overwrite] sourceSubdirectory targetSubdirectory  [RegExpPosition [RegExpNegative]]\n"
        + "  FileVisitor move sourceSubdirectory targetSubdirectory\n"
        + "  FileVisitor remove subdirectory [RegExpPosition [RegExpNegative]]");
      return;
    }
    try {
      FileFilter ff = null;
      if (argv[0].equals("list")) {
        Lister visitor = new Lister(new File(argv[1]));
        visitor.execute(FileFilterRE.valueOf(argv,2));
        List files = visitor.getFiles();
        Out.println(files.size() + " elements found\n" + Out.join(files,"\n"));
      } else if (argv[0].equals("copy")) {
        int index = argv[1].equals("-overwrite")? 2: 1;
        Copier visitor = new Copier(new File(argv[index]), new File(argv[index+1]));
        if (index == 2) visitor.setOverwrite(true);
        visitor.execute(FileFilterRE.valueOf(argv,index+2));
        Out.println(visitor.getFiles().size() + " elements copied");
      } else if (argv[0].equals("move")) {
        Mover visitor = new Mover(new File(argv[1]), new File(argv[2]));
        visitor.execute();
      } else if (argv[0].equals("remove")) {
        Remover visitor = new Remover(new File(argv[1]));
        visitor.execute(FileFilterRE.valueOf(argv,2));
        Out.println(visitor.getFiles().size() + " elements removed");
      } else {
        Out.println("Unknown command");
        return;
      }
    } catch (Exception e) {
      System.err.println(e);
    }
  }
}