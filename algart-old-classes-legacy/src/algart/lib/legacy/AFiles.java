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

package algart.lib.legacy;

import java.io.*;
import java.util.*;

/**
 * <p>Title:     <b>Algart Files</b><br>
 * Description:  Library of tools for working with files<br>
 * @author       Daniel Alievsky
 * @version      1.1
 * @deprecated
 */

public class AFiles {

  public static class TrueStaticSection {
    public static int debugLevel= 0;
    public static File tempDirectory= null;

    public static synchronized void deleteOnShutdown(String fileName) {
      if (deleterOnShutdown==null) deleterOnShutdown= new DeleterOnShutdown();
      deleterOnShutdown.deleteOnShutdown(fileName);
    }
    private static DeleterOnShutdown deleterOnShutdown= null;
    private static class DeleterOnShutdown {
      private Set filesSet= new HashSet();
      private List filesList= new ArrayList();
      private void deleteOnShutdown(String fileName) {
        if (!filesSet.contains(fileName)) {
          filesList.add(fileName);
          filesSet.add(fileName);
        }
      }
      {
        if (debugLevel>=1) ATools.println("AFiles.DeleterOnShutdown: created");
        Runtime.getRuntime().addShutdownHook(new Thread() {
          public void run() {
            cleanup();
          }
        });
      }
      private synchronized void cleanup() {
        if (filesList==null) return;
        try {
          if (debugLevel>=1) ATools.println("AFiles.DeleterOnShutdown: deleting " + filesList.size() + " files");
          for (int k= filesList.size()-1; k>=0; k--) {
            String fileName= (String)filesList.get(k);
            if (debugLevel>=1) ATools.print("AFiles.DeleterOnShutdown: deleting file "+fileName+"... ");
            if (deleteRecursive(fileName)) {
              if (debugLevel>=1) ATools.println("OK");
            } else {
              if ((new File(fileName)).exists())
                ATools.println("Cannot delete "+fileName+" on shutdown");
            }
          }
        } finally {
          filesList= null;
        }
      }
    }
  }

  public static boolean isRoot(File f) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().isRoot(f);
  }
  public static boolean isFileSystem(File f) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().isFileSystem(f);
  }
  public static boolean isFileSystemRoot(File dir) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().isFileSystemRoot(dir);
  }
  public static boolean isDrive(File dir) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().isDrive(dir);
  }
  public static boolean isFloppyDrive(File dir) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().isFloppyDrive(dir);
  }
  public static boolean isComputerNode(File dir) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().isComputerNode(dir);
  }
  public static boolean isHiddenFile(File f) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().isHiddenFile(f);
  }
  public static boolean isTraversable(File f) {
    Boolean result= javax.swing.filechooser.FileSystemView.getFileSystemView().isTraversable(f);
    return result!=null && result.booleanValue();
  }
  public static boolean isUsualFileOrSubdir(File f) {
    javax.swing.filechooser.FileSystemView fileSystemView=
      javax.swing.filechooser.FileSystemView.getFileSystemView();
    return fileSystemView.isFileSystem(f)
      && !fileSystemView.isRoot(f)
      && !fileSystemView.isFileSystemRoot(f);
  }
  public static boolean isUsualFileOrSubdirOrRoot(File f) {
    javax.swing.filechooser.FileSystemView fileSystemView=
      javax.swing.filechooser.FileSystemView.getFileSystemView();
    return fileSystemView.isFileSystem(f);
  }

  public static boolean isLink(File f) {
    try {
      return sun.awt.shell.ShellFolder.getShellFolder(f).isLink();
    } catch (FileNotFoundException e) {
      return false;
    }
  }
  public static File getLinkLocation(File f) throws FileNotFoundException {
    File result= sun.awt.shell.ShellFolder.getShellFolder(f).getLinkLocation();
    if (result==null || result.getPath().trim().length()==0) throw new FileNotFoundException("Incorrect link - it is empty");
    return result;
  }
  public static javax.swing.Icon getSystemIcon(File f) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().getSystemIcon(f);
  }

  public static boolean isParent(File folder, File file) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().isParent(folder,file);
  }
  public static File getChild(File parent, String fileName) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().getChild(parent,fileName);
  }
  public static File getParentDirectory(File dir) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().getParentDirectory(dir);
  }
  public static File[] getFiles(File dir) {
  // All files without hiding
    return getFiles(dir,false);
  }
  public static File[] getFiles(File dir, boolean useFileHiding) {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().getFiles(dir,useFileHiding);
  }

  private static File userDefaultDirectory;
  public static synchronized File getUserDefaultDirectory() {
  // My Documents under Windows
    if (userDefaultDirectory!=null) return userDefaultDirectory;
    String userDefaultDirectoryName = System.getProperty("s7.user.home.myDocuments", null);
    return userDefaultDirectory = userDefaultDirectoryName == null
            ? new File(javax.swing.filechooser.FileSystemView.getFileSystemView().getDefaultDirectory().getAbsolutePath())
            : new File(userDefaultDirectoryName);
  }
  public static String getUserDefaultDirectoryName() {
    return getUserDefaultDirectory().getAbsolutePath();
  }
  private static File userHomeDirectory;
  public static synchronized File getUserHomeDirectory() {
  // Desktop under Windows
    if (userHomeDirectory!=null) return userHomeDirectory;
    return userHomeDirectory = new File(javax.swing.filechooser.FileSystemView.getFileSystemView().getHomeDirectory().getAbsolutePath());
  }
  public static String getUserHomeDirectoryName() {
    return getUserHomeDirectory().getAbsolutePath();
  }
  public static File[] getRoots() {
    return javax.swing.filechooser.FileSystemView.getFileSystemView().getRoots();
  }
  public static File getMyPicturesFolder() {
    File result = new File(AFiles.getUserDefaultDirectory(), "My Pictures");
    if (!result.exists()) result = AFiles.getUserDefaultDirectory();
    return result;
  }
  public static String getMyPicturesFolderName() {
    return getMyPicturesFolder().getAbsolutePath();
  }


  public static File tempDirectory() {
    if (TrueStaticSection.tempDirectory!=null) return TrueStaticSection.tempDirectory;
    try {
      String s= System.getProperty("java.io.tmpdir");
      if (s!=null) return TrueStaticSection.tempDirectory= new File(s);
    } catch (Exception e) {
    }
    try {
      File testTempFile= File.createTempFile("gettempdir",null);
      try {
        if (TrueStaticSection.debugLevel>=1) ATools.println("AFiles.tempDirectory(): detecting temporary directory "
          +"by using temporary file "+testTempFile);
        return TrueStaticSection.tempDirectory= testTempFile.getParentFile();
      } finally {
        testTempFile.delete();
      }
    } catch(IOException e) {
      System.err.println("Error in AFiles.tempDirectory(): cannot determine temporary directory.\n"+e);
      return new File("."+File.pathSeparator);
    }
  }
  public static File createTempDirectory(String prefix, String suffix) {
    return createTempDirectory(prefix,suffix,null);
  }
  public static File createTempDirectory(String prefix, String suffix, File directory) {
    if (prefix==null) throw new NullPointerException();
    if (prefix.length()<3) throw new IllegalArgumentException("Prefix string too short");
    synchronized(tmpFileLock) {
      if (directory==null) directory= tempDirectory();
      File f;
      do {
        f= generateDir(prefix,suffix==null?".tmp":suffix,directory);
      } while (!f.mkdir());
      if (TrueStaticSection.debugLevel>=1) ATools.println("AFiles.createTempDirectory(): "+f+" created");
      return f;
    }
  }
  /*--------------------------------------------------------------------------*/
  public static class AFileExistsException extends AException {
    private String filename;
    public AFileExistsException(File f) {
      super("File \""+ f.getAbsolutePath()+ "\" is always exists");
      filename= f.getAbsolutePath();
    }
  }
  public static class ACannotMakeDirectoryException extends AException {
    private String directory;
    public ACannotMakeDirectoryException(File f) {
      super("Can not make directory \""+ f.getAbsolutePath()+ "\"");
      directory= f.getAbsolutePath();
    }
  }
  public static void mkdir(
    File fDir
    , boolean removeFiles
    , boolean recursive
  ) throws AFileExistsException, ACannotMakeDirectoryException, SecurityException {
    if (fDir.isFile())
      throw new AFileExistsException(fDir);

    if (fDir.exists()) {
      if (removeFiles) {
        File[] flist= fDir.listFiles();
        int len= (flist== null)? 0: flist.length;
        for (; len> 0; ) {
          File f= flist[--len];
          if (f.isDirectory()) {
            if (recursive) {
              mkdir(f, removeFiles, recursive);
              f.delete();
            }
          } else {
            f.delete();
          }
        }
      }
    } else {
      if (!fDir.mkdirs())
        throw new ACannotMakeDirectoryException(fDir);
    }
  }
  public static boolean moveFiles(
    File fDirSrc
  , File fDirDst
  , boolean recursive
  ) throws AFileExistsException, ACannotMakeDirectoryException, SecurityException {
    if (fDirDst.exists() && !fDirDst.isDirectory())
      throw new AFileExistsException(fDirDst);
    File[] files= fDirSrc.listFiles();
    for (int i= 0; i< files.length; ++i) {
      File f= files[i];
      String name= f.getName();
      boolean res;
      if (recursive && f.isDirectory())
        res= moveFiles(new File(fDirSrc, name), new File(fDirDst, name), recursive);
      else
        res= f.renameTo(new File(fDirDst, name));
      if (!res) return false;
    }
    return true;
  }

  private static final Object tmpFileLock = new Object();
  private static int counter= -1; /* Protected by tmpFileLock */
  private static File generateDir(String prefix, String suffix, File dir) {
    if (counter==-1) counter= new Random().nextInt()&0xFFFFFF;
    counter++;
    return new File(dir,prefix+counter+suffix);
  }

  public static void deleteOnShutdown(File f) {
    deleteOnShutdown(f.getPath());
  }
  public static void deleteOnShutdown(String fileName) {
    TrueStaticSection.deleteOnShutdown(fileName);
  }



  public static String loadStreamAsString(InputStream stream, boolean closeStream) throws IOException {
    return loadStreamAsString(stream,null,closeStream);
  }
  public static char[] loadStreamAsChars(InputStream stream, boolean closeStream) throws IOException {
    return loadStreamAsChars(stream,null,closeStream);
  }
  public static String loadStreamAsString(InputStream stream, String encoding, boolean closeStream) throws IOException {
    try {
      StringBuffer sb= new StringBuffer(stream.available());
      InputStreamReader reader= encoding==null? new InputStreamReader(stream):
        new InputStreamReader(stream,encoding);
      char[] buf= new char[32768];
      int len;
      while ((len=reader.read(buf,0,buf.length))>=0) {
        sb.append(buf,0,len);
      }
      return sb.toString();
    } finally {
      if (closeStream) {
        try {stream.close();} catch (Exception e) {};
      }
    }
  }
  public static char[] loadStreamAsChars(InputStream stream, String encoding, boolean closeStream) throws IOException {
    String buf= loadStreamAsString(stream,encoding,closeStream);
    char[] result= new char[buf.length()];
    buf.getChars(0,result.length,result,0);
    return result;
  }
  public static byte[] loadStreamAsBytes(InputStream stream, boolean closeStream) throws IOException {
    try {
      byte[] result= new byte[stream.available()];
      byte[] buf= new byte[32768];
      int len,ofs=0;
      while ((len=stream.read(buf,0,buf.length))>=0) {
        result= (byte[])AArrays.copyEnsured(buf,0,result,ofs,len);
        ofs+= len;
      }
      return result;
    } finally {
      if (closeStream) {
        try {stream.close();} catch (Exception e) {};
      }
    }
  }
  public static int[] loadStreamAsInts(InputStream stream, boolean closeStream) throws IOException {
    try {
      int[] result= new int[(stream.available()+3)/4];
      byte[] buf= new byte[32768];
      int len,ofs=0;
      while ((len=stream.read(buf,0,buf.length))>=0) {
        result= (int[]) AArrays.ensureCapacity(result, (ofs + len + 3) / 4);
        AArrays.coerciveCopy(buf,0,result,ofs,len);
        ofs+= len;
      }
      return result;
    } finally {
      if (closeStream) {
        try {stream.close();} catch (Exception e) {};
      }
    }
  }

  public static String loadFileAsStringNoExceptions(String fileName) {
    return loadFileAsStringNoExceptions(new File(fileName));
  }
  public static String loadFileAsStringNoExceptions(File file) {
    return loadFileAsStringNoExceptions(file,null);
  }
  public static String loadFileAsStringNoExceptions(String fileName, String encoding) {
    return loadFileAsStringNoExceptions(new File(fileName),encoding);
  }
  public static String loadFileAsStringNoExceptions(File file, String encoding) {
    try {
      return loadFileAsString(file,encoding);
    } catch (Exception e) {
      return null;
    }
  }

  public static String loadFileAsString(String fileName) throws IOException {
    return loadFileAsString(new File(fileName));
  }
  public static String loadFileAsString(File file) throws IOException {
    return loadFileAsString(file,null);
  }
  public static char[] loadFileAsChars(String fileName) throws IOException {
    return loadFileAsChars(new File(fileName));
  }
  public static char[] loadFileAsChars(File file) throws IOException {
    return loadFileAsChars(file,(String)null);
  }
  public static void loadFileAsChars(File file, char[] buf) throws IOException {
    loadFileAsChars(file,buf,0,buf.length);
  }
  public static void loadFileAsChars(File file, char[] buf, int off, int len)
  throws IOException {
    loadFileAsChars(file,null,buf,off,len);
  }
  public static String loadFileAsString(String fileName, String encoding) throws IOException {
    return loadFileAsString(new File(fileName),encoding);
  }
  public static String loadFileAsString(File file, String encoding) throws IOException {
    StringBuffer sb= new StringBuffer((int)Math.min(file.length(),1<<30));
    InputStreamReader f= encoding==null? new FileReader(file):
      new InputStreamReader(new FileInputStream(file),encoding);
    try {
      char[] buf= new char[32768];
      int len;
      while ((len=f.read(buf,0,buf.length))>=0) {
        sb.append(buf,0,len);
      }
      return sb.toString();
    } finally {
      try {f.close();} catch (Exception e) {};
    }
  }
  public static char[] loadFileAsChars(String fileName, String encoding) throws IOException {
    return loadFileAsChars(new File(fileName),encoding);
  }
  public static char[] loadFileAsChars(File file, String encoding) throws IOException {
    String buf= loadFileAsString(file,encoding);
    char[] result= new char[buf.length()];
    buf.getChars(0,result.length,result,0);
    return result;
  }
  public static void loadFileAsChars(File file, String encoding, char[] buf) throws IOException {
    loadFileAsChars(file,encoding,buf,0,buf.length);
  }
  public static void loadFileAsChars(File file, String encoding, char[] buf, int off, int len)
  throws IOException {
    InputStreamReader f= encoding==null? new FileReader(file):
      new InputStreamReader(new FileInputStream(file),encoding);
    try {
      f.read(buf,off,len);
    } finally {
      try {f.close();} catch (Exception e) {};
    }
  }

  public static byte[] loadFileAsBytes(String fileName) throws IOException {
    return loadFileAsBytes(new File(fileName));
  }
  public static byte[] loadFileAsBytes(File file) throws IOException {
    byte[] result= new byte[(int)file.length()];
    loadFileAsBytes(file,result);
    return result;
  }
  public static void loadFileAsBytes(File file, byte[] buf) throws IOException {
    loadFileAsBytes(file,buf,0,buf.length);
  }
  public static void loadFileAsBytes(File file, byte[] buf, int off, int len)
  throws IOException {
    FileInputStream f= new FileInputStream(file);
    try {
      f.read(buf,off,len);
    } finally {
      try {f.close();} catch (Exception e) {};
    }
  }

  // All saveFileXXX remove file if v/buf==null
  public static void saveFileFromStringNoExceptions(String fileName, String v) {
    saveFileFromStringNoExceptions(new File(fileName),v);
  }
  public static void saveFileFromStringNoExceptions(File file, String v) {
    saveFileFromStringNoExceptions(file,null,v);
  }
  public static void saveFileFromStringNoExceptions(String fileName, String encoding, String v) {
    saveFileFromStringNoExceptions(new File(fileName),encoding,v);
  }
  public static void saveFileFromStringNoExceptions(File file, String encoding, String v) {
    try {
      saveFileFromString(file,encoding,v);
    } catch (Exception e) {
    }
  }

  public static void saveFileFromString(String fileName, String v) throws IOException {
    saveFileFromString(new File(fileName),v);
  }
  public static void saveFileFromString(File file, String v) throws IOException {
    saveFileFromString(file,null,v);
  }
  public static void saveFileFromChars(String fileName, char[] buf) throws IOException {
    saveFileFromChars(new File(fileName),buf);
  }
  public static void saveFileFromChars(File file, char[] buf) throws IOException {
    saveFileFromChars(file,null,buf);
  }
  public static void saveFileFromChars(File file, char[] buf, int off, int len) throws IOException {
    saveFileFromChars(file,null,buf,off,len);
  }
  public static void saveFileFromString(String fileName, String encoding, String v) throws IOException {
    saveFileFromString(new File(fileName),encoding,v);
  }
  public static void saveFileFromString(File file, String encoding, String v) throws IOException {
    if (v==null) {
      file.delete(); return;
    }
    char[] buf= new char[v.length()];
    v.getChars(0,buf.length,buf,0);
    saveFileFromChars(file,encoding,buf);
  }
  public static void saveFileFromChars(String fileName, String encoding, char[] buf) throws IOException {
    saveFileFromChars(new File(fileName),encoding,buf);
  }
  public static void saveFileFromChars(File file, String encoding, char[] buf) throws IOException {
    if (buf==null) {
      file.delete(); return;
    }
    saveFileFromChars(file,encoding,buf,0,buf.length);
  }
  public static void saveFileFromChars(File file, String encoding, char[] buf, int off, int len) throws IOException {
    if (buf==null) {
      file.delete(); return;
    }
    OutputStreamWriter f= encoding==null? new FileWriter(file):
      new OutputStreamWriter(new FileOutputStream(file),encoding);
    try {
      f.write(buf,off,len);
    } catch (IOException e) {
      try {f.close();} catch (Exception e1) {};
      return;
    }
    f.close();
  }

  public static void saveFileFromBytes(String fileName, byte[] buf) throws IOException {
    saveFileFromBytes(new File(fileName),buf);
  }
  public static void saveFileFromBytes(File file, byte[] buf) throws IOException {
    if (buf==null) {
      file.delete(); return;
    }
    saveFileFromBytes(file,buf,0,buf.length);
  }
  public static void saveFileFromBytes(File file, byte[] buf, int off, int len) throws IOException {
    if (buf==null) {
      file.delete(); return;
    }
    FileOutputStream f= new FileOutputStream(file);
    try {
      f.write(buf,off,len);
    } catch (IOException e) {
      try {f.close();} catch (Exception e1) {};
      return;
    }
    f.close();
  }

  public static void copyFile(String source, String target) throws IOException {
    copyFile(new File(source),new File(target));
  }
  public static void copyFile(File source, File target) throws IOException {
    RandomAccessFile input= new RandomAccessFile(source,"r");
    RandomAccessFile output= new RandomAccessFile(target,"rw");
    try {
      byte[] buf = new byte[65536];
      long len= input.length();
      output.setLength(len); // - decrease disk fragmentation under NTFS
      int bytesRead;
      while ((bytesRead= input.read(buf,0,buf.length))>0)
        output.write(buf,0,bytesRead);
    } catch (IOException e) {
      try {input.close();} catch (Exception e1) {};
      try {output.close();} catch (Exception e1) {};
      return;
    }
    try {input.close();} catch (Exception e) {};
    output.close();
  }

  /** Recursively copies srcDir over destDir, overwriting files there if
   * their names collide. Subdirectories under destDir get created as
   * necessary.
   *
   * XXX.2do In order to make the behavior more flexible, an elaborate feedback
   * interface type arg (a policy) is required, with implementations ranging
   * from the strictest to the liberal. The filter would then be one of the
   * policy's properties.
   *
   * Ideally, this routine should operate on any 2 File instances, regardless
   * of their nature or existence. E.g. it would be able, under a sufficiently
   * liberal policy, to overwrite an existing directory with a file whose name
   * collides with that of the directory.
   */
  public static void copyDirectory (
    File srcDir, File destDir, FilenameFilter filter
  ) throws IOException {
    if (!srcDir.isDirectory() || destDir.isFile())
      throw new IllegalArgumentException(srcDir + " is not a directory or " + destDir + " is a file");

    destDir.mkdirs() ;
    copyDirectoryInternal(srcDir, destDir, filter) ;
  }

  private static void copyDirectoryInternal (
    File srcDir, File destDir, FilenameFilter filter
  ) throws IOException {
    destDir.mkdir() ;

    File[] files = srcDir.listFiles(filter) ;

    for (int i = 0 ; i < files.length ; ++i) {
      File f = files[i], g = new File(destDir, f.getName()) ;

      if (f.isFile())
        copyFile(f, g) ;
      else
        copyDirectoryInternal(f, g, filter) ;
    }
  }

  public interface RecursiveFilter {
    public boolean accept(File f, String[] recursiveLevels);
    // {} for top level,
    // {subdirName} for 2nd level,
    // {topSubdirName,subdirName} for 3rd level, ...
  }
  public static class RecursiveFilterFileExtension implements RecursiveFilter {
    private String fileExtension;
    public RecursiveFilterFileExtension(String fileExtension) {
      this.fileExtension= fileExtension;
    }
    public boolean accept(File f, String[] recursiveLevels) {
      return f.getName().endsWith("."+fileExtension);
    }
  }
  public static void listRecursive(String dirName,
    RecursiveFilter fileFilter,
    RecursiveFilter subdirFilter)
  // Just calls fileFilter and subdirFilter for all files/subdirectories
  {
    listFilesRecursive(new File(dirName),fileFilter,subdirFilter,null,false);
  }
  public static void listRecursive(File dir,
    RecursiveFilter fileFilter,
    RecursiveFilter subdirFilter) {
  // Just calls fileFilter and subdirFilter for all files/subdirectories
    listFilesRecursive(dir,fileFilter,subdirFilter,null,false);
  }
  public static String[] listFilesRecursive(String dirName,
  // Don't include subdirectories into result
    RecursiveFilter fileFilter,
    RecursiveFilter subdirFilter)
  {
    return listFilesRecursive(dirName,fileFilter,subdirFilter,false);
  }
  public static String[] listFilesRecursive(String dirName,
    RecursiveFilter fileFilter,
    RecursiveFilter subdirFilter,
    boolean includeSubdirectoriesInResult)
  {
    List list= new ArrayList();
    listFilesRecursive(new File(dirName),fileFilter,subdirFilter,list,includeSubdirectoriesInResult);
    return (String[])(list.toArray(new String[0]));
  }
  public static File[] listFilesRecursive(File dir,
  // Don't include subdirectories into result
    RecursiveFilter fileFilter,
    RecursiveFilter subdirFilter)
  {
    return listFilesRecursive(dir,fileFilter,subdirFilter,false);
  }
  public static File[] listFilesRecursive(File dir,
    RecursiveFilter fileFilter,
    RecursiveFilter subdirFilter,
    boolean includeSubdirectoriesInResult)
  {
    List list= new ArrayList();
    listFilesRecursive(dir,fileFilter,subdirFilter,list,includeSubdirectoriesInResult);
    File[] result= new File[list.size()];
    for (int k=0; k<result.length; k++) result[k]= new File((String)list.get(k));
    return result;
  }
  public static void listFilesRecursive(File dir,
    RecursiveFilter fileFilter,
    RecursiveFilter subdirFilter,
    List listOfAllFiles, boolean includeSubdirectoriesInList)
  {
    listFilesRecursive(dir,fileFilter,subdirFilter,
      listOfAllFiles,includeSubdirectoriesInList,new String[0]);
  }

  protected static void listFilesRecursive(File dir,
    RecursiveFilter fileFilter,
    RecursiveFilter subdirFilter,
    List listOfAllFiles,
    boolean includeSubdirectoriesInList,
    String[] levels)
  {
    if (dir==null || "".equals(dir.getPath())) dir= new File(".");
    String[] subLevels= new String[levels.length+1];
    System.arraycopy(levels,0,subLevels,0,levels.length);

    String[] fileNames = dir.list();
    if (fileNames==null) return;
    for (int k=0; k<fileNames.length; k++) {
      File f= new File(dir.getPath(), fileNames[k]);
      if (f.isFile()) {
        if (fileFilter==null || fileFilter.accept(f,levels)) {
          if (listOfAllFiles!=null) listOfAllFiles.add(f.getPath());
        }
      } else if (f.isDirectory()) {
        if (subdirFilter==null || subdirFilter.accept(f,levels)) {
          if (includeSubdirectoriesInList && listOfAllFiles!=null) listOfAllFiles.add(f.getPath());
          subLevels[levels.length]= f.getName();
          listFilesRecursive(f,fileFilter,subdirFilter,
            listOfAllFiles,includeSubdirectoriesInList,subLevels);
        }
      }
    }
  }

  public static boolean deleteRecursive(String fileOrDirName) {
    return deleteRecursive(new File(fileOrDirName));
  }
  public static boolean deleteRecursive(File fileOrDir) {
    String[] fileNames = fileOrDir.list();
    if (fileNames!=null) {
      for (int k=0; k<fileNames.length; k++) {
        deleteRecursive(new File(fileOrDir.getPath(), fileNames[k]));
      }
    }
    return fileOrDir.delete();
  }



  public static PrintStream duplicateStream(PrintStream s1, OutputStream s2) {
    return duplicateStream(s1,s2,true);
  }
  public static PrintStream duplicateStream(PrintStream s1, OutputStream s2, boolean flush2) {
    return new DuplicateStreamInternal(s1,s2,flush2);
  }
  public static PrintStream duplicateStream(PrintStream s1, OutputStream s2, OutputStream s3) {
    return duplicateStream(s1,s2,true,s3,true);
  }
  public static PrintStream duplicateStream(PrintStream s1, OutputStream s2, boolean flush2, OutputStream s3, boolean flush3) {
    return duplicateStream(
      duplicateStream(s1,s2,flush2),
      s3,flush3);
  }

  private static class DuplicateStreamInternal extends PrintStream {
    protected PrintStream source;

    private DuplicateStreamInternal(PrintStream source,
      OutputStream target,
      boolean autoFlushTarget)
    {
      super(target,autoFlushTarget);
      this.source= source;
    }

    public boolean checkError() {
      return source.checkError() || super.checkError();
    }
    public void write(int b) {
      source.write(b);
      super.write(b);
    }
    public void write(byte[] buf, int off, int len) {
      source.write(buf,off,len);
      super.write(buf,off,len);
    }
    public void close() {
      source.close();
      super.close();
    }
    public void flush() {
      source.flush();
      super.flush();
    }
  }
}