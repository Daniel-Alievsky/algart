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

package net.algart.lib.tests;

import java.util.*;
import java.awt.*;
import java.awt.event.*;
import java.applet.*;
import javax.swing.*;
import net.algart.lib.*;

public class AppletTestSecurity extends Applet {
  boolean testField = false;
  TextArea textArea = new TextArea();
  public static final boolean TEST_FOR_JAVA2 = false;
  static {
    GlobalProperties.setBooleanProperty(GlobalProperties.JAVA_NATIVE_ENABLED_PROPERTY_NAME,false);
  }

  //Construct the applet
  public AppletTestSecurity() {
    String s = "";
    try {
      s+= "NNJava Version: "+System.getProperty("java.version");
    } catch (Throwable e) {
      s+= "NNJava Version: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNClass.forName: "+Class.forName(getClass().getName());
    } catch (Throwable e) {
      s+= "NNClass.forName: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNClass.forName: "+Class.forName("java.lang.String");
    } catch (Throwable e) {
      s+= "NNClass.forName: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNgetDeclaredFields:NN  "+Out.join(getClass().getDeclaredFields(),"NN  ");
    } catch (Throwable e) {
      s+= "NNgetDeclaredFields: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNgetDeclaredFields (AbstractException):NN  "+Out.join(AbstractException.class.getDeclaredFields(),"NN  ");
    } catch (Throwable e) {
      s+= "NNgetDeclaredFields (AbstractException): "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNgetDeclaredField(testField): ";
      java.lang.reflect.Field f = getClass().getDeclaredField("testField");
      s+= f + " = ";
      s+= f.getBoolean(this);
      f.setBoolean(this,true);
      s+= ", then "+testField;
      if (TEST_FOR_JAVA2) f.setAccessible(true);
    } catch (Throwable e) {
      s+= "NNgetDeclaredField(testField): "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNgetDeclaredField(AbstractException.isTrueStaticFlag): ";
      Class c = AbstractException.class;
      s += c;
      java.lang.reflect.Field f = AbstractException.class.getDeclaredField("isTrueStaticFlag");
      s+= f + " = ";
      s+= f.getBoolean(this);
    } catch (Throwable e) {
      s+= "NNgetDeclaredField(AbstractException.isTrueStaticFlag): "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNJAVA_CLASS_PATH: "+Out.join(Reflection.JAVA_CLASS_PATH,java.io.File.pathSeparator);
    } catch (Throwable e) {
      s+= "NNJAVA_CLASS_PATH: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNgetTempClassesDirectory(): "+Reflection.getTempClassesDirectory();
    } catch (Throwable e) {
      s+= "NNgetTempClassesDirectory(): "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNGlobalProperties:NN  "+Out.join(GlobalProperties.getProperties(),"NN  ");
    } catch (Throwable e) {
      s+= "NNGlobalProperties: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    if (TEST_FOR_JAVA2) {
      try {
        s+= "NNSystem.getProperties():NN  "+Out.join(new TreeMap(System.getProperties()),"NN  ");
      } catch (Throwable e) {
        s+= "NNSystem.getProperties(): "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
      }
    }
    try {
      s+= "NNSystem path.separator: "+ System.getProperty("path.separator");
    } catch (Throwable e) {
      s+= "NNpath.separator: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNSystem line.separator: "+ System.getProperty("line.separator");
    } catch (Throwable e) {
      s+= "NNline.separator: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNSystem net.algart.lib.ExtendedFriendly.DEBUG_LEVEL: "+ System.getProperty("net.algart.lib.ExtendedFriendly.DEBUG_LEVEL");
    } catch (Throwable e) {
      s+= "NNSystem net.algart.lib.ExtendedFriendly.DEBUG_LEVEL: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNSystem java.io.tmpdir: "+ System.getProperty("java.io.tmpdir");
    } catch (Throwable e) {
      s+= "NNjava.io.tmpdir: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNtempDirectory"+ Directory.tempDirectory();
    } catch (Throwable e) {
      s+= "NNtempDirectory(): "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
    try {
      s+= "NNcreateTempDirectory: "+ Directory.createTempDirectory("aplettemp","");
    } catch (Throwable e) {
      s+= "NNcreateTempDirectory(): "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }

    try {
      s+= "NNtimsmsDouble: "+Timing.timemsDouble()+" ["+Timing.initializationExceptionMessage()+"]";
    } catch (Throwable e) {
      s+= "NNtimsmsDouble: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
    }
//    try {
//      s+= "NNKinds:NN  "+net.algart.immutableimage.Kind.values().toString("NN  ");
//    } catch (Throwable e) {
//      s+= "NNKinds: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
//    }
//    try {
//      s+= "NNImmImageToolsNative: ["+net.algart.immutableimage.ImmImageToolsNative.initializationExceptionMessage()+"]";
//    } catch (Throwable e) {
//      s+= "NNImmImageToolsNative: "+Out.replace(e.toString(),"\n","NN"); e.printStackTrace();
//    }
    s = s.substring(2);
    s = Out.replace(s,"\n","\\n");
    s = Out.replace(s,"\r","\\r");
    s = Out.replace(s,"NN","\n");
    textArea.setColumns(90);
    textArea.setRows(30);
    textArea.setText(s);
    this.add(textArea, null);
  }
}