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
import net.algart.lib.*;

public class StringsParsingTest {

  public static void main(String[] args) {
    String[] tokens;
    String s,value;
    Strings.NextInfo info;

    tokens= Strings.splitStringWithQuotes(s=" a ",',');
    Out.println("("+s+")\n"+Out.join(tokens,"|"));
    tokens= Strings.splitStringWithQuotes(s="a, ",',');
    Out.println("("+s+")\n"+Out.join(tokens,"|"));
    tokens= Strings.splitStringWithQuotes(s="",',');
    Out.println("("+s+")\n"+Out.join(tokens,"|"));
    tokens= Strings.splitStringWithQuotes(s=",",',');
    Out.println("("+s+")\n"+Out.join(tokens,"|"));
    tokens= Strings.splitStringWithQuotes(s=",,'asd'   sd,",',');
    Out.println("("+s+")\n"+Out.join(tokens,"|"));
    tokens= Strings.splitStringWithQuotes(s="1;2",';');
    Out.println("("+s+")\n"+Out.join(tokens,"|"));
    tokens= Strings.splitStringWithQuotes(s="  '1'  ; ss\"2\" ; ",';');
    Out.println("("+s+")\n"+Out.join(tokens,"|"));
    tokens= Strings.splitStringWithQuotes(s="  \"111 \"\" ; 2  ;",';');
    Out.println("("+s+")\n"+Out.join(tokens,"|"));

    info= Strings.findSubstringIgnoringQuotedStart(s="la la \" la>>urur",">>");
    Out.println("\n("+s+"), find >> : "+info.index+",("+info.value+")");
    info= Strings.findSubstringIgnoringQuotedStart(s="la la \" la>>urur",">>",6);
    Out.println("("+s+"), find >> from 6: "+info);
    info= Strings.findSubstringIgnoringQuotedStart(s="la la \" la>>urur",">>",7);
    Out.println("("+s+"), find >> from 7: "+info.index+",("+info.value+")");
    info= Strings.findSubstringIgnoringQuotedStart(s="'la >> la \"' la>>urur",">>");
    Out.println("("+s+"), find >> : "+info.index+",("+info.value+")");
    value= Strings.endSubstringIgnoringQuotedStart(s="'la ' la \"'",0);
    Out.println("("+s+"), endSubstring(0): ("+value+")");
    value= Strings.endSubstringIgnoringQuotedStart(s="'la ' la \"'",1);
    Out.println("("+s+"), endSubstring(1): ("+value+")");
  }
}