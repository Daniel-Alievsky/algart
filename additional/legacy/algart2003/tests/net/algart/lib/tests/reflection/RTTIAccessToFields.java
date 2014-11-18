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

import net.algart.lib.tests.reflection.otherpackage.*;
import java.lang.reflect.*;

class Friend {
  public int a_public= 23;
  int b_friend= 334;
  private int c_private= 542;
}
class FriendSubclass extends Friend {
  private int c_private= -542;
  int d_friend= 3;
  public int e_public= 4;
  private int f_private= 5;
  public String toString() {return "!!!"+d_friend;}
  { e_public= 123; }
}

public class RTTIAccessToFields {
  public static boolean ACCESSIBLE= true;
  public static void print(String s) {System.out.print(s);}
  public static void println(String s) {System.out.println(s);}

  public static void showFields (Field[] fields, Object inst) {
    for (int k=0; k<fields.length; k++) {
      print(fields[k].getName()+" \t");
      try {
        if (ACCESSIBLE)
          fields[k].setAccessible(true);
        print(fields[k].get(inst)+"");
      } catch (Exception e) {
        print("cannot read ("+e+")");
      }
      println("");
    }
  }
  public static void showConstructors (Constructor[] constructors, Object inst) {
    for (int k=0; k<constructors.length; k++) {
      print(constructors[k].getName()+", "
        +constructors[k].getParameterTypes().length+" arguments \t");
      try {
        if (ACCESSIBLE)
          constructors[k].setAccessible(true);
        Object newInst= constructors[k].newInstance(new Object[0]);
        print(newInst+"");
      } catch (Exception e) {
        print("cannot create\n ("+e+")");
      }
      println("");
    }
    print("Using newInstance \t");
    try {
      Object newInst= inst.getClass().newInstance();
      print(newInst+"");
    } catch (Exception e) {
      print("cannot create\n ("+e+")");
    }
    println("");
  }
  public static void main (String[] args) {
    Field[] fields;
    Constructor[] constructors;
    Object inst;

    for (int k=0; k<2; k++) {
      RTTIAccessToFields.ACCESSIBLE= k>0;
      println(RTTIAccessToFields.ACCESSIBLE?
        "\n*** Using setAccessible(true) ***":
        "\n*** Not using setAccessible(true) ***");

      inst= new FriendSubclass();
      fields= inst.getClass().getDeclaredFields();
      println("\nLocal FriendSubclass, declared fields:");
      showFields(fields,inst);

      fields= inst.getClass().getSuperclass().getDeclaredFields();
      println("\nLocal FriendSubclass, declared fields of superclass:");
      showFields(fields,inst);

      fields= inst.getClass().getFields();
      println("\nLocal FriendSubclass, fields:");
      showFields(fields,inst);

      constructors= inst.getClass().getConstructors();
      println("\nLocal FriendSubclass, constructors:");
      showConstructors(constructors,inst);

      inst= RTTIFriends.getFriendSubclass();
      fields= inst.getClass().getDeclaredFields();
      println("\nExternal FriendSubclass, declared fields:");
      showFields(fields,inst);

      fields= inst.getClass().getSuperclass().getDeclaredFields();
      println("\nExternal FriendSubclass, declared fields of superclass:");
      showFields(fields,inst);

      fields= inst.getClass().getFields();
      println("\nExternal FriendSubclass, fields:");
      showFields(fields,inst);

      constructors= inst.getClass().getDeclaredConstructors();
      println("\nExternal FriendSubclass, declared constructors:");
      showConstructors(constructors,inst);
    }
  }
}
