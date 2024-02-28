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
import java.net.*;
import java.awt.*;
import java.awt.image.*;
import javax.swing.*;

/**
 * <p>A very simple image object that can be loaded from the file with maximal speed.
 *
 * @author  Daniel Alievsky
 * @version 1.0
 */

public class QuickLoadableImage {

  public static final int DEBUG_LEVEL = GlobalProperties.getClassDebugLevel(QuickLoadableImage.class);;
  public static final String FILE_MARKER= "AQLI 1.0"; //<=8 chars

  public abstract class LocalException extends AbstractException {
    String infoResourceName= resourceName;
  }
  public class NoDataException extends LocalException {
    public NoDataException() {
      setMessage("No any data in "+resourceName);
    }
  }
  public class IllegalFormatException extends LocalException {
    public IllegalFormatException() {
      setMessage("Invalid format of "+resourceName);
    }
  }

  private int sx= -1;
  private int sy= -1;
  private int[] rgb;
  private String resourceName= "[some resource]"; //used in exceptions
  public QuickLoadableImage(int sx, int sy, int[] rgb) {
    this.sx= sx;
    this.sy= sy;
    this.rgb= rgb;
  }
  public QuickLoadableImage(BufferedImage bi) {
    this.sx= bi.getWidth();
    this.sy= bi.getHeight();
    this.rgb= bi.getRGB(0,0,sx,sy,null,0,sx);
  }
  public QuickLoadableImage(Class clazz, String resourceName) throws IOException,LocalException {
    this(clazz.getResourceAsStream(resourceName),resourceName);
  }
  public QuickLoadableImage(java.net.URL url) throws IOException,LocalException {
    this(url==null? null: BinaryIO.read(url.openStream(),true), url+"");
  }
  public QuickLoadableImage(InputStream stream) throws IOException,LocalException {
    this(stream,null);
  }
  private QuickLoadableImage(InputStream stream, String resourceName) throws IOException,LocalException {
    this(stream==null? null: BinaryIO.read(stream,true), resourceName);
  }
  public QuickLoadableImage(File file) throws IOException,LocalException {
    this(file==null? null: BinaryIO.read(file), file+"");
  }
  public QuickLoadableImage(byte data[]) throws LocalException {
    this(data,null);
  }
  private QuickLoadableImage(byte data[], String resourceName) throws LocalException {
    if (resourceName!=null) this.resourceName= resourceName;
    if (data==null || data.length==0)
      throw new NoDataException();
    if (DEBUG_LEVEL>=1) Out.println(" **QuickLoadableImage** "+data.length+" bytes loaded from "+resourceName);
    try {
      if (data.length<=16
        || !FILE_MARKER.equals(new String(data,0,8,"UTF-8")))
        throw new IllegalFormatException();
    } catch (UnsupportedEncodingException e) {
    }
    this.sx= Ma.max(net.algart.array.Arrays.coerciveCopyInts(data,8,4)[0],0);
    this.sy= Ma.max(net.algart.array.Arrays.coerciveCopyInts(data,12,4)[0],0);
    this.rgb= new int[Ma.min(sx*sy,(data.length-16+3)/4)];
    net.algart.array.Arrays.coerciveCopy(data,16,rgb,0,rgb.length*4);
  }

  public int getWidth() {
    return sx;
  }
  public int getHeight() {
    return sy;
  }
  public Image getImage() {
    return Toolkit.getDefaultToolkit().createImage(
      new MemoryImageSource(sx,sy,rgb,0,sx));
  }
  public BufferedImage getBufferedImage() {
    return ColorBuffers.rgbToBufferedImage(rgb,sx,sy,false);
  }
  public byte[] getData() {
    byte[] result= new byte[16+rgb.length*4];
    try {
      net.algart.array.Arrays.copy(FILE_MARKER.getBytes("UTF-8"),result);
    } catch (UnsupportedEncodingException e) {
    }
    net.algart.array.Arrays.coerciveCopy(new int[]{sx,sy},0,result,8,8);
    net.algart.array.Arrays.coerciveCopy(rgb,0,result,16,rgb.length*4);
    return result;
  }
  public void write(File f) throws IOException {
    BinaryIO.write(f,getData());
  }

  public static ImageIcon getImageIconAndPrintException(Class clazz, String resourceName) {
    Image image= getImageAndPrintException(clazz,resourceName);
    if (image==null) return null;
    return new ImageIcon(image);
  }
  public static Image getImageAndPrintException(Class clazz, String resourceName) {
    try {
      return getImage(clazz,resourceName);
    } catch (Exception e) {
      Out.printlnFlush("Class "+clazz.getName()+" cannot load an icon from "+resourceName);
      e.printStackTrace(System.out);
      return null;
    }
  }
  public static Image getImage(Class clazz, String resourceName) throws IOException,LocalException {
    return (new QuickLoadableImage(clazz,resourceName)).getImage();
  }
  public static Image getImage(java.net.URL url) throws IOException,LocalException {
    return (new QuickLoadableImage(url)).getImage();
  }
  public static Image getImage(InputStream stream) throws IOException,LocalException {
    return (new QuickLoadableImage(stream)).getImage();
  }
  public static Image getImage(File file) throws IOException,LocalException {
    return (new QuickLoadableImage(file)).getImage();
  }
  public static Image getImage(byte data[]) throws LocalException {
    return (new QuickLoadableImage(data)).getImage();
  }

  public static void prepareAQLI(File f) {
    try {
      System.out.print("Processing "+f+"... ");
      BufferedImage bi= javax.imageio.ImageIO.read(f);
      if (bi==null) {
        System.out.println("unknown format");
      } else {
        QuickLoadableImage aqli= new QuickLoadableImage(bi);
        aqli.write(new File(
          Strings.beforeLastSubstring(f.getPath(),".")+".aqli"));
        System.out.println("OK");
      }
    } catch(Exception e) {
      System.err.println("\nCannot process "+f);
      e.printStackTrace();
    }
  }
  public static void prepareAQLI(InputStream f, File target) {
    try {
      System.out.print("Processing "+target+"... ");
      BufferedImage bi= javax.imageio.ImageIO.read(f);
      if (bi==null) {
        System.out.println("unknown format");
      } else {
        QuickLoadableImage aqli= new QuickLoadableImage(bi);
        aqli.write(target);
        System.out.println("OK");
      }
    } catch(Exception e) {
      System.err.println("\nCannot process "+target);
      e.printStackTrace();
    }
  }
  static String[] prepareImagesList(String[] pathsWithMask, String removedPrefix) {
    try {
      ArrayList result= new ArrayList();
      for (int m=0; m<pathsWithMask.length; m++) {
        File path= new File(pathsWithMask[m]);
        final String name= path.getName();
        File[] files= null;
        if (!name.startsWith("*.")) {
          files= new File[] {path};
        } else {
          FileVisitor.Lister lister = new FileVisitor.Lister(path.getParentFile());
          lister.execute(new FileFilter() {
            public boolean accept(File f) {
              return f.getName().endsWith("."+name.substring(2));
            }
          });
          files= (File[])lister.getFiles().toArray(new File[0]);
        }
        for (int k=0; k<files.length; k++) {
          String s= files[k].getCanonicalPath().replace(File.separatorChar,'/');
          if (removedPrefix!=null && s.startsWith(removedPrefix))
            s= s.substring(removedPrefix.length());
          result.add(s);
        }
      }
      return (String[])result.toArray(new String[0]);
    } catch(Exception e) {
      e.printStackTrace();
      return new String[0];
    }
  }
  static String prepareImagesListAsString(String[] pathsWithMask, String removedPrefix) {
    return Out.join(prepareImagesList(pathsWithMask,removedPrefix),"\n");
  }
  static String prepareImagesListAsString(String[] pathsWithMask) {
    return prepareImagesListAsString(pathsWithMask,
      Reflection.getClassFile(net.algart.lib.QuickLoadableImage.class)
        .getParentFile().getParentFile().getParentFile().getParentFile()
        .getAbsolutePath().replace(File.separatorChar,'/')+"/");
  }
  public static void main(String[] args) {
    if (args.length==0) {
      System.out.println("Usage:\n"
        +"  QuickLoadableImage pictureFile\n"
        +"or\n"
        +"  QuickLoadableImage [-printlistonly] path/*.someExt\n"
        +"or\n"
        +"  QuickLoadableImage resourceWithListOfResources.txt targetDir/"
      );
      return;
    }
    if (args.length==2 && args[0].endsWith(".txt") && args[1].endsWith("/")) {
      ClassLoader classLoader= QuickLoadableImage.class.getClassLoader();
      try {
        StringTokenizer stok = new StringTokenizer(TextIO.UTF8.read(
          classLoader.getResourceAsStream(args[0]),true));
        File lastTargetDir= null;
        for (; stok.hasMoreTokens(); ) {
          String token = stok.nextToken();
          File target= new File(args[1]+
            Strings.beforeLastSubstring(token,".")+".aqli");
          if (!target.getParent().equals(lastTargetDir+"")) {
            lastTargetDir= target.getParentFile();
            if (!lastTargetDir.exists()) {
              System.out.println("Creating directory "+lastTargetDir);
              lastTargetDir.mkdirs();
            }
          }
          prepareAQLI(classLoader.getResourceAsStream(token),target);
        }
      } catch(Exception e) {
        e.printStackTrace();
      }
    } else if (args[0].equals("-printlistonly")) {
      System.out.println(prepareImagesListAsString((String[])net.algart.array.Arrays.deleteOne(args,0)));
    } else {
      String[] files= prepareImagesList(args,null);
      for (int k=0; k<files.length; k++)
        prepareAQLI(new File(files[k]));
    }
  }

}