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

package net.algart.syntax.java.tests;

import java.util.*;
import java.io.*;
import net.algart.lib.*;
import net.algart.syntax.java.*;

public class ParserTest {

  public static void main(String[] args) throws Exception {
    if (args.length<1) {
      Out.println("Usage: JavaParser some_java_file [EXPAND]");
      return;
    }

    String src= TextIO.SYSTEM_DEFAULT.read(new File(args[0]));
    Parser parser= new Parser(src,true);
    parser.activeLocalClassSeparator= '\\';
    StringBuffer sb= new StringBuffer();
/*
    println("Tokens:");
    while (jp.et.tokenType!=JavaParser.TT_EOF) {
      jp.et.nextToken();
      sb.append(jp.et+"\n");
    }
    println(sb);
    jp.et.reset();
*/
    long t1= Timing.timens();
    parser.parse();
    long t2= Timing.timens(),t3=t2,t4=t2;

    String[] imports= null;
    if (args.length>=2 && args[1].equals("EXPAND")) {
      imports= Types.correctImports(parser.getAllImports(),null);
      t3= Timing.timens();
      parser.expandMethodArgumentTypes(null,imports);
      t4= Timing.timens();
    }

    Out.println(parser.memberSources.size()+" methods parsed:");
    for (Iterator it= parser.memberSources.iterator(); it.hasNext(); ) {
      Out.println("!"+((Parser.MemberSource)it.next()).toString());
    }
    Out.println("\nAll fields:\n  "+Out.join(parser.getFieldsMap().keySet(),"\n  "));
    Out.println("\nAll methods:\n  "+Out.join(parser.getMethodsMap().keySet(),"\n  "));
    Out.println("\nAll classes:\n  "+Out.join(parser.getClassesMap().keySet(),"\n  "));
    Out.println("\nAll interfaces:\n  "+Out.join(parser.getInterfacesMap().keySet(),"\n  "));
    if (t4!=t2) Out.println("\nCorrected imports:\n  "+Out.join(imports,"\n  "));
    Out.println("\nParsing time: "+(t2-t1+0.)/1000000+" ms");
    if (t4!=t2) Out.println("Expanding imports time: "+(t3-t2+0.)/1000000+" ms");
    if (t4!=t2) Out.println("Expanding time: "+(t4-t3+0.)/1000000+" ms");
  }
}