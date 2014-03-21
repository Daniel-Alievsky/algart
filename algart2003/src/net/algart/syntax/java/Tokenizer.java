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

public class Tokenizer {
  /*
  Why not StreamTokenizer?
  1) It doesn't return contents of comments
  2) Errors (not too important):
    it supposes that "a-b" is the single token, because "-" is a "digit"
    it supposes that "1.234e23" is a pair of tokens "1.234" and "e23"
    it doesn't understand "0x3A3", "123.34f"
  */
  public static final int TT_EOF = -1;
  public static final int TT_EOL = '\n';
  public static final int TT_NUMBER = -2;
  public static final int TT_WORD = -3;
  public static final int TT_NOTHING = -4;
  public static final int TT_COMMENT_C = -10;
  public static final int TT_COMMENT_CPP = -11;
  public static final int TT_QUOTED = -12;
    // not '"', because '"' can be an end character of the line or file

  private int slen;
  private int sp;
  private char[] s;
  public Tokenizer (char[] src) {
    this.slen= src.length;
    this.sp= 0;
    this.s= src;
  }
  public Tokenizer (String src) {
    this.slen= src.length();
    this.sp= 0;
    this.s= new char[this.slen];
    src.getChars(0,this.slen,this.s,0);
  }
  public int offset() {return sp;}
  public int length() {return slen;}
  public void seek(int offset) {sp= offset<0? 0: offset>slen? slen: offset;}
  public void reset() {sp=0;}
  public String getSrc(int offset, int count) {
    return new String(s,offset,count);
  }
  public int getCurrentLine() {
    int count= 0;
    for (int k=0; k<sp; k++) if (s[k]=='\n') count++;
    return count+1;
  }
  public int getCurrentColumn() {
    int j= sp;
    while (j>0 && s[--j]!='\n') ;
    return sp-j;
  }

  private static final byte CT_WHITESPACE = 1;
  private static final byte CT_ALPHA = 2;
  private static final byte CT_DIGIT = 4;
  private static final byte CT_DIGITEXT = 8;
  private static final byte CT_DIGITHEX = 16;
  private static final byte CT_ALPHANUMERIC = CT_ALPHA|CT_DIGIT;
  private static final byte CT_CRLF= 32;
  private static byte ctype[] = new byte[128];
  {
    for (int k=0; k<=' '; k++) if (k!='\n') ctype[k]=CT_WHITESPACE;
    for (int k='A'; k<='Z'; k++) ctype[k]=CT_ALPHA;
    for (int k='a'; k<='z'; k++) ctype[k]=CT_ALPHA;
    ctype['_']=ctype['$']=CT_ALPHA;
    for (int k='0'; k<='9'; k++) ctype[k]=CT_DIGIT|CT_DIGITEXT|CT_DIGITHEX;
    ctype['-']=ctype['.']=CT_DIGITEXT;
    ctype['e']=ctype['E']=CT_ALPHA|CT_DIGITEXT;
    ctype['\n']=ctype['\r']=CT_WHITESPACE|CT_CRLF;
    for (int k='A'; k<='F'; k++) ctype[k]|=CT_DIGITHEX;
  }

  public int tokenType= TT_NOTHING;
  public int tokenSkippedSpaceOffset;
  public int tokenOffset;
  // always tokenOffset>=tokenSkippedSpaceOffset
  // tokenOffset==tokenSkippedSpaceOffset if nextToken() didn't skip any starting spaces
  public int tokenLength;
  public String getToken() {
    return new String(s,tokenOffset,tokenLength);
  }
  public String getTokenComment() {
    switch (tokenType) {
      case TT_COMMENT_C:
        return new String(s,tokenOffset+2,tokenLength-4);
      case TT_COMMENT_CPP: {
        char c;
        int q= tokenOffset+tokenLength;
        while (q>0 && (c=s[--q])<128 && (ctype[c]&CT_CRLF)!=0) ;
        return new String(s,tokenOffset+2,q+1-(tokenOffset+2))+"\n";
      }
    }
    return getToken();
  }
  public void nextToken() {
    tokenSkippedSpaceOffset= sp;
    char c;
    for (; true; sp++) {
      if (sp>=slen) {tokenType= TT_EOF; tokenOffset= slen; tokenLength=0; return;}
      if ((c=s[sp])=='\n') {tokenType= TT_EOL; tokenOffset= sp++; tokenLength=1; return;}
      if (c>' ') break;
    }

    byte ct= c<128? ctype[c]: CT_ALPHA;
    tokenType= c;
    tokenOffset= sp;
    tokenLength= 1;
    sp++;

    if ((ct&CT_ALPHA)!=0) {
      while (sp<slen && ((c=s[sp])>=128 || (ctype[c]&CT_ALPHANUMERIC)!=0)) sp++;
      tokenType= TT_WORD;

    } else if ((ct&CT_DIGITEXT)!=0) {
      int p= sp;
      if (c=='-') {
        if (p>=slen) return;
        c= s[p++];
        ct= c<128? ctype[c]: CT_ALPHA;
        if ((ct&CT_DIGIT)==0 && c!='.') return;
      }
      boolean isHex= false;
      if (p+1<slen && c=='0' && ((c=s[p++])=='x' || c=='X')) {
        isHex= true; p++;
      }
      boolean containsDigits= false;
      int ctExt= isHex? CT_DIGITHEX: CT_DIGITEXT;
      while (p<slen && (c=s[p])<128 && ((ct=ctype[c])&ctExt)!=0) {
        if ((ct&CT_DIGIT)!=0) containsDigits= true;
        p++;
      }
      if (p<slen && ((c=s[p])=='f' || c=='F' || c=='d' || c=='D')) p++;
      if (!containsDigits) return;
      sp= p;
      tokenType= TT_NUMBER;

    } else if (c=='"' || c=='\'') {
      char quote= c;
      while (sp<slen && (c=s[sp])!='\n' && c!=quote) {
        sp++;
        if (c=='\\') {
          if (sp<slen && (c=s[sp])!='\n') sp++;
        }
      }
      if (sp<slen) sp++; //including ending quote
      tokenType= TT_QUOTED;

    } else if (c=='/' && sp<slen && s[sp]=='*') {
      sp++;
      char lastc= c;
      while (sp<slen && !((c=s[sp])=='/' && lastc=='*')) {
        sp++; lastc= c;
      }
      if (sp<slen) sp++; //including ending '/'
      tokenType= TT_COMMENT_C;

    } else if (c=='/' && sp<slen && s[sp]=='/') {
      sp++;
      while (sp<slen && (c=s[sp])!='\n') sp++;
      if (sp<slen) sp++; //including ending '\n'
      tokenType= TT_COMMENT_CPP;

    } // else we should retain tokenType=c
    tokenLength= sp-tokenOffset;
    return;
  }

  public String toString() {
    switch (tokenType) {
      case TT_EOF:         return "EOF";
      case TT_EOL:         return "EOL";
      case TT_NUMBER:      return "NUMBER:"+getToken();
      case TT_WORD:        return "WORD:"+getToken();
      case TT_COMMENT_C:   return "/**/-COMMENT:"+getTokenComment();
      case TT_COMMENT_CPP: return "//-COMMENT:"+getTokenComment();
      case TT_QUOTED:      return "STRING:"+getToken();
      case TT_NOTHING:     return "NOTHING";
    }
    return tokenType<' '? tokenType+"": "'"+(char)tokenType+"'";
  }
}