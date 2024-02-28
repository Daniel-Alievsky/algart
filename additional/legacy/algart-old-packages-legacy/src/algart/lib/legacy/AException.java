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

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>Title:     <b>Algart Exception</b><br>
 * Description:  Implements an original idea of xml-based easily localized exceptions<br>
 * @author       Daniel Alievsky
 * @version      1.1
 * @deprecated
 */

public class AException extends Exception implements Cloneable,RepresentableAsXml {
  private static boolean isTrueStaticFlag;
  // AException (but not its children) should be TrueStatic:
  // we sometimes need to throw AException in one ClassLoader
  // and then check it in another
  // Classes extending AException cannot be TrueStatic: they
  // are sometimes local non-static classes of usual dynamic classes,
  // so they contains a link to dynamic content

  private String message;
  public AException() {
    super();
  }
  public AException(String s) {
    super(s);
    this.message= s;
  }
  public Object clone() {
    try {
      return super.clone();
    } catch (CloneNotSupportedException e) {
      throw new InternalError();
    }
  }
  public String getMessage() {
    return this.message==null? "": this.message;
  }
  public void setMessage(String message) {
    this.message= message;
  }
  public String getOriginalMessage() {
    return super.getMessage();
  }

  public int toStringMode= TO_STRING_MODE_FULL_INFO;
  public static final int TO_STRING_MODE_FULL_INFO= 0;
  public static final int TO_STRING_MODE_FULL_INFO_XML= 1;
  public static final int TO_STRING_MODE_STACK_ONLY= 2;
  public static final int TO_STRING_MODE_STANDARD= 3;
  public static final int TO_STRING_MODE_ORIGINAL= 4;
  public String toString() {
    String s= "Exception "+getClass().getName()+" occurred\n";
    switch (toStringMode) {
      case TO_STRING_MODE_FULL_INFO:
        return s+AStrings.chomp(AStrings.addLFIfNotEmpty(message)+getAdditionalInfo().toString(true));
      case TO_STRING_MODE_FULL_INFO_XML:
        return s+AStrings.chomp(AStrings.addLFIfNotEmpty(message)+getAdditionalInfo().toXmlTags(true));
      case TO_STRING_MODE_STACK_ONLY:
        return "";
      case TO_STRING_MODE_STANDARD:
        return s+getMessage();
    }
    return getOriginalMessage();
  }

  public static class Info {
    private static boolean isTrueStaticFlag;
    public static final String PREFIX= "info";
    public static final int PREFIX_LENGTH= PREFIX.length();
    private Object[] pairs;
    private Info(Object[] pairs) {
      this.pairs= pairs;
    }
    private Info(List pairs) {
      this.pairs= pairs.toArray();
    }
    public String toString() {
      return toString(false);
    }
    public String toString(boolean addLFAfterLastElement) {
      StringBuffer sb= new StringBuffer();
      for (int k=0; k<this.pairs.length; k+=2) {
        sb.append(this.pairs[k]+":\t"
          +this.pairs[k+1]
          +(addLFAfterLastElement || k<this.pairs.length-2?"\n":""));
      }
      return sb.toString();
    }
    public String toXmlTags() {
      return toXmlTags(false);
    }
    public String toXmlTags(boolean addLFAfterLastElement) {
      StringBuffer sb= new StringBuffer();
      for (int k=0; k<this.pairs.length; k+=2) {
        String name= String.valueOf(this.pairs[k]);
        Object valueObj= this.pairs[k+1];
        boolean valueIsRepresentableAsXml= valueObj instanceof RepresentableAsXml;
        String value= valueIsRepresentableAsXml?
          "\n  "+AStrings.replace(
            AStrings.chomp(((RepresentableAsXml) valueObj).toXmlString()),
            "\n","\n  ")+"\n":
          String.valueOf(valueObj);
        sb.append(AStrings.xmlTag(
          AStrings.javaNameToTagName(name.substring(PREFIX_LENGTH)),
          value,!valueIsRepresentableAsXml)
          +(addLFAfterLastElement || k<this.pairs.length-2?"\n":""));
      }
      return sb.toString();
    }
  }

  public Info getAdditionalInfo() {
    List info= new ArrayList();
    for (Class clazz= getClass();
      clazz!=null && clazz!=Throwable.class && clazz!=Object.class;
      clazz=clazz.getSuperclass()) {
      Field[] fields= clazz.getDeclaredFields();
      for (int k=0; k<fields.length; k++) {
        String name= fields[k].getName();
        if (name.startsWith(Info.PREFIX)) {
          try {
            try {
              fields[k].setAccessible(true);
            } catch (SecurityException e) {
            }
            Object value= fields[k].get(this);
            if (value!=null) {
              info.add(name);
              info.add(value);
            }
          } catch (IllegalAccessException e) {
            System.err.println("Internal error in AException: cannot access to "
              +name+" field of "+this.getClass().getName()+" exception");
          }
        }
      }
    }
    return new Info(info);
  }
  public String toXmlString() {
    return AStrings.chomp(
      AStrings.addLFIfNotEmpty(AStrings.xmlTag("message",getMessage()))
      +getAdditionalInfo().toXmlTags(true));
  }

    public static String[] getSuperclasses(Throwable e, boolean fullClassNames) {
        return getSuperclasses(e.getClass(), fullClassNames);
    }

    public static String[] getSuperclasses(Class<? extends Throwable> eClass, boolean fullClassNames) {
        int count = 0;
        for (Class clazz = eClass;
             clazz != Throwable.class && clazz != Object.class;
             clazz = clazz.getSuperclass())
            count++;
        String[] result = new String[count + 1];
        for (Class clazz = eClass;
             count >= 0;
             clazz = clazz.getSuperclass(), count--) {
            String name = clazz.getName();
            if (!fullClassNames) name = name.substring(name.lastIndexOf(".") + 1);
            result[count] = name;
        }
        return result;
    }


    public String[] getSuperclasses(boolean fullClassNames) {
        return getSuperclasses(this, fullClassNames);
    }
}