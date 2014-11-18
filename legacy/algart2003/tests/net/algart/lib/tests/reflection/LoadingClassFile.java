/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.lib.tests.reflection;

import java.net.*;
import java.io.*;
import java.lang.reflect.*;
import net.algart.lib.*;

public class LoadingClassFile {

  public static void main(String[] args) throws
  ClassNotFoundException, MalformedURLException, IOException {
    if (args.length==0) {
      Out.println("Usage: LoadingClassFile someclass\n"
        +"For example: LoadingClassFile net.algart.array.Arrays");
      return;
    }
    String className= args[0];
    String classFileName= className.substring(className.lastIndexOf(".")+1)+".class";
    Out.println("Loading "+className+" ("+classFileName+")...");
    Class clazz;
    Method[] methods;
    URL url= null;
    File f= null;
    byte[] buf;
    for (int k=0; k<3; k++) {
      long t1= Timing.timemcs();
      clazz= Class.forName(className);
      long t2= Timing.timemcs();
      methods= clazz.getMethods();
      long t3= Timing.timemcs();
      url= clazz.getResource(classFileName);
      long t4= Timing.timemcs();
      f= new File(url.getFile());
      long t5= Timing.timemcs();
      buf= BinaryIO.read(f);
      long t6= Timing.timemcs();
      Out.println("Loading class (forName):        "+(t2-t1)+" mcs");
      Out.println(Out.pad(methods.length,3)
                +" methods (getMethods):       "+(t3-t2)+" mcs");
      Out.println("url (getResource):              "+url+" "+(t4-t3)+" mcs");
      Out.println("new File(url.getFile()):        "+f+" "+(t5-t4)+" mcs");
      Out.println("Loading file ("+Out.rightPad(buf.length+"b):",18)+(t6-t5)+" mcs\n");
    }
    Out.println("url.getFile():         "+url.getFile());
    Out.println("File size f.length():  "+f.length());
    Out.println("f.toURL():             "+f.toURL());
    Out.println("f.getPath():           "+f.getPath());
    Out.println("f:                     "+f);
    Out.println("File.separatorChar.:   "+File.separatorChar);
  }
}
