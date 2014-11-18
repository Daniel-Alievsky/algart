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

import net.algart.lib.*;
import java.io.*;
import java.lang.reflect.*;

public class ClassFromFileTest {

  public static void main(final String[] args) throws Exception {
    if (args.length<2) {
      Out.println("Usage: ClassFromFileTest classPathDefault classPath2 ... classPathN className");
      return;
    }
    final int extraClassPathCount= args.length-2;

    Reflection.setClassLoaderFactory(new Reflection.ClassLoaderFactory() {
      public ClassLoader createClassLoader(Object loaderId) {
        return new Reflection.DynamicClassOverloader(
          Reflection.splitClassPath(args[loaderId==null?0:Integer.parseInt(loaderId+"")]),
          java.util.Collections.EMPTY_SET,
          loaderId, null, true)
        {
          protected boolean shouldNotBeOverloaded(String name) {
            return false;
          }
          protected boolean shouldNotBeOverloaded(Class clazz) {
            return false;
          }
        };
      }
    });
    Reflection.invalidateClasses();
    for (int k=0;;k++) {
      Object loaderId = new Integer(k%(extraClassPathCount+1));
      Class clazz= Reflection.loadClass(args[args.length-1],false,loaderId);
      Out.println("Class loaded: id = "+loaderId);
      Out.println("Creating an instance...");
      Object inst= clazz.newInstance();
      Out.println("toString:\n"+inst);
      Out.println("Now you can try to change this class file; then press:\n"
        +"  ENTER to continue,\n"
        +"  newENTER to continue with new DynamicClassOverloader,\n"
        +"  Ctrl+C to break...");
      String st= new BufferedReader(new InputStreamReader(System.in)).readLine();
      if (st==null) st= "";
      st= st.trim().toLowerCase();
      if (st.equals("new")) {
        Out.println("\nReflection.invalidateClasses()");
        Reflection.invalidateClasses();
      }
    }
  }
}
