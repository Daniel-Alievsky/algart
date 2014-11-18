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

package net.algart.syntax.java;

import java.lang.reflect.*;
import net.algart.lib.*;

public class Types {

    public static Class forJvmName(String className, ClassLoader classLoader)
    throws ClassNotFoundException
    {
    // Important: classLoader==null corresponds to "Class.forName(className)",
    // not to "Class.forName(className,true,null)"
        try {
            if (classLoader==null)
                return Class.forName(className);
            return Class.forName(className,true,classLoader);
        } catch(ClassNotFoundException e) {
            Class result= (Class)primitiveTypes.get(className);
            if (result!=null) return result;
            throw e;
        }
    }

    public static String toJavaSignature(Method method, boolean toDots) {
        StringBuffer sb= new StringBuffer(method.getName());
        sb.append("(");
        Class[] parameterTypes= method.getParameterTypes();
        for (int k=0; k<parameterTypes.length; k++) {
            if (k>0) sb.append(",");
            sb.append(JVM.toJavaName(parameterTypes[k],toDots));
        }
        sb.append(")");
        return sb.toString();
    }

    public static Class forNameNoExceptions(String className, ClassLoader classLoader) {
    // Important: classLoader==null corresponds to "Class.forName(className)",
    // not to "Class.forName(className,true,null)"
        try {
            if (classLoader==null)
                return Class.forName(className);
            return Class.forName(className,true,classLoader);
        } catch(ClassNotFoundException e) {
            return null;
        } catch(NoClassDefFoundError e) {
            return null;
        } catch(NullPointerException e) {
            return null;
        }
    }
    public static Class forDotsNameNoExceptions(String className, ClassLoader classLoader) {
    // Unlike forNameNoExceptions(), understands dots (not only '$')
    // in a nested static class name
        Class result= forNameNoExceptions(className,classLoader);
        if (result!=null) return result;
        StringBuffer sb= new StringBuffer(className);
        for (int j= sb.length()-1; j>=0; j--) {
            if (sb.charAt(j)=='.') {
                sb.setCharAt(j,'$');
                result= forNameNoExceptions(sb.toString(),classLoader);
                if (result!=null) return result;
            }
        }
        return null;
    }

    public static String[] correctImports(String[] imports, ClassLoader classLoader) {
    // Changes . to \ after package names, to / after local class names (instead $)
        String[] result= new String[imports.length];
        for (int k=0; k<imports.length; k++) result[k]= correctImport(imports[k],classLoader);
        return result;
    }

    private static final java.util.HashMap alreadyExpandedImports= new java.util.HashMap();
    public static String correctImport(String s, ClassLoader classLoader) {
    // Changes . to / after package names, to \ after local class names (instead $)
        if (s.indexOf(".")==-1) return s;
        String key= s+"\n"+System.identityHashCode(classLoader);
        String result= (String)alreadyExpandedImports.get(key);
        if (result!=null) return result;
        s= s.replace('\\','.').replace('/','.');
        int p= s.lastIndexOf(".");
        if (p==-1) return s;
        Class clazz= forDotsNameNoExceptions(s.substring(0,p),classLoader);
        if (clazz==null) return s.replace('.','/');
        String className= clazz.getName();
        p= className.lastIndexOf(".");
        if (p==-1) return s.replace('.','\\');
        result= s.substring(0,p+1).replace('.','/')+s.substring(p+1).replace('.','\\');
        alreadyExpandedImports.put(key,result);
        return result;
    }

    public Types(String[] imports) {
    // imports[] should be corrected by correctImports()
        this.imports = imports.clone();
    }
    public Class forJavaName(final String className, ClassLoader classLoader) {
    // this.imports should start from "(current package)/*" and "java/lang/*"
        Class result= (Class)alreadyExpandedTypes.get(className);
        if (result!=null || alreadyExpandedTypes.containsKey(className))
            return result;
        try {
            if ((result= (Class)primitiveTypes.get(className))!=null)
                return result;
            if (className.endsWith("[]")) {
                Class componentClass= this.forJavaName(
                    className.substring(0,className.length()-2),classLoader);
                if (componentClass==null) return result= null;
                return result= java.lang.reflect.Array.newInstance(
                    componentClass,0).getClass();
            }
            // Maybe, className is full package + nested static class name?
            if ((result= forDotsNameNoExceptions(className,classLoader))!=null)
                return result;
            // No, className should be expanded with imports[]
            String classNameNoDots= className.replace('.','$');
            for (int k=0; k<this.imports.length; k++) {
                String s= this.imports[k].replace('/','.').replace('\\','.');
                if (!s.endsWith(".*")) {
                    int q= s.lastIndexOf(".")+1;
                    if ((classNameNoDots+"$").startsWith(s.substring(q)+"$")
                        && ((result= forNameNoExceptions(
                            this.imports[k].substring(0,q).replace('/','.').replace('\\','$')
                                +classNameNoDots,classLoader))!=null))
                        return result;
                }
            }
            for (int k=0; k<this.imports.length; k++) {
                String s= this.imports[k].replace('/','.').replace('\\','.');
                if (s.endsWith(".*")) {
                    int q= this.imports[k].length()-1;
                    if ((result= forNameNoExceptions(
                        this.imports[k].substring(0,q).replace('/','.').replace('\\','$')
                            +classNameNoDots,classLoader))!=null)
                        return result;
                }
            }
            return result= null;
        } finally {
            alreadyExpandedTypes.put(className,result);
        }
    }
    public boolean classNameMatches(Class clazz, String className) {
        return classNameMatches(clazz,className,this.imports);
    }

    private static final java.util.HashMap importsToInstances= new java.util.HashMap();
    public static Class forJavaName(final String className, ClassLoader classLoader, String[] imports) {
        String key= Out.join(imports,"\n")+"\n"+System.identityHashCode(classLoader);
        Types types= (Types)importsToInstances.get(key);
        if (types==null) {
            types= new Types(imports);
            importsToInstances.put(key,types);
        }
        return types.forJavaName(className,classLoader);
    }

    public static boolean classNameMatches(Class clazz, String className, String[] imports) {
    // imports should always start from "(current package).*" and "java.lang.*"
        if (clazz==boolean.class) return className.equals("boolean");
        if (clazz==char.class)    return className.equals("char");
        if (clazz==byte.class)    return className.equals("byte");
        if (clazz==short.class)   return className.equals("short");
        if (clazz==int.class)     return className.equals("int");
        if (clazz==long.class)    return className.equals("long");
        if (clazz==float.class)   return className.equals("float");
        if (clazz==double.class)  return className.equals("double");
        if (clazz==void.class)    return className.equals("void");
        Class componentClass= clazz.getComponentType();
        if (componentClass!=null) {
            return className.endsWith("[]")
                && classNameMatches(componentClass,
                    className.substring(0,className.length()-2),imports);
        }
        String fullClassName= clazz.getName();
        int p= fullClassName.lastIndexOf('.')+1;
        String packageName= fullClassName.substring(0,p);
        String fullClassNameDots= fullClassName.replace('$','.');
        if (className.equals(fullClassNameDots))
            return true;
        if (!(fullClassNameDots.endsWith("."+className)
            && fullClassNameDots.length()-className.length()>=p))
            return false;
        for (int k=0; k<imports.length; k++) {
            if (!imports[k].endsWith(".*")) {
                int q= imports[k].lastIndexOf(".")+1;
                if ((className+".").startsWith(imports[k].substring(q)+".")
                    && (imports[k].substring(0,q)+className).equals(fullClassNameDots))
                    return true;
            }
        }
        for (int k=0; k<imports.length; k++) {
            if (imports[k].endsWith(".*")) {
                int q= imports[k].length()-1;
                if ((imports[k].substring(0,q)+className).equals(fullClassNameDots))
                    return true;
            }
        }
        return false;
    }

    private static final java.util.HashMap primitiveTypes= new java.util.HashMap(9);
    static {
        primitiveTypes.put("boolean",boolean.class);
        primitiveTypes.put("char",   char.class);
        primitiveTypes.put("byte",   byte.class);
        primitiveTypes.put("short",  short.class);
        primitiveTypes.put("int",    int.class);
        primitiveTypes.put("long",   long.class);
        primitiveTypes.put("float",  float.class);
        primitiveTypes.put("double", double.class);
        primitiveTypes.put("void",   void.class);
    }
    private final java.util.HashMap alreadyExpandedTypes= new java.util.HashMap();
    private final String[] imports;
}