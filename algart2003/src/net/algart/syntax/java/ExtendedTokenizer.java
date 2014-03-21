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

import net.algart.lib.*;

public class ExtendedTokenizer extends Tokenizer {
  public static final int DEBUG_LEVEL = GlobalProperties.getClassDebugLevel(ExtendedTokenizer.class);
  public static final int TT_BLOCK= -20;

  public ExtendedTokenizer (char[] src) {
    super(src);
  }
  public ExtendedTokenizer (String src) {
    super(src);
  }
  public void discardLine() {
    do nextToken();
    while (tokenType!=TT_EOF && tokenType!=TT_EOL);
  }
  public void nextTokenSignificantOrBlock() {
    nextTokenSignificantOrBlock(true);
  }
  public void nextTokenSignificantOrBlock(boolean parseBraceBlock) {
    if (parseBraceBlock) nextTokenSignificantOrBraceBlock('{','}');
    else nextTokenSignificant();
  }

  public void nextTokenSignificantOrBraceBlock(char openBrace, char closeBrace) {
    nextTokenSignificantOrBraceBlock(openBrace,closeBrace,openBrace,closeBrace);
  }
  public void nextTokenSignificantOrBraceBlock(
    char openBrace1, char closeBrace1, char openBrace2, char closeBrace2
  ) {
    int pStart= offset();
    nextTokenSignificant();
    int p= offset();
    if (tokenType==openBrace1) {
      continueBraceBlock(openBrace1,closeBrace1);
      tokenOffset= p-1;
      tokenLength= offset()-tokenOffset;
      tokenType= TT_BLOCK;
    } else if (tokenType==openBrace2) {
      continueBraceBlock(openBrace2,closeBrace2);
      tokenOffset= p-1;
      tokenLength= offset()-tokenOffset;
      tokenType= TT_BLOCK;
    }
    tokenSkippedSpaceOffset= pStart;
 }
  protected int braceLevel= 0;
  private void continueBraceBlock(char openBrace, char closeBrace) {
    if (DEBUG_LEVEL>=1) Out.println(Out.dup(' ',braceLevel)+openBrace+": "+getCurrentLine()+','+getCurrentColumn());
    braceLevel++;
    while (tokenType!=TT_EOF) {
      nextToken();
      if (tokenType==closeBrace) break;
      if (tokenType==openBrace) continueBraceBlock(openBrace,closeBrace);
    }
    braceLevel--;
    if (DEBUG_LEVEL>=1) Out.println(Out.dup(' ',braceLevel)+closeBrace+": "+getCurrentLine()+','+getCurrentColumn());
    // here we can get TT_EOF twice
  }

  public void nextTokenNotEol() {
    int pStart= offset();
    do nextToken();
    while (tokenType==TT_EOL);
    tokenSkippedSpaceOffset= pStart;
  }

  public StringBuffer tokenSkippedComments= null;
  public void nextTokenSignificant() {
    nextTokenSignificant(false);
  }
  public void nextTokenSignificant(boolean returnComments) {
    if (!returnComments) tokenSkippedComments= null;
    int pStart= offset();
    do {
      nextToken();
      if (returnComments && (tokenType==TT_COMMENT_C || tokenType==TT_COMMENT_CPP)) {
        if (tokenSkippedComments==null) tokenSkippedComments= new StringBuffer();
        tokenSkippedComments.append(getTokenComment());
      }
    } while (tokenType==TT_COMMENT_C || tokenType==TT_COMMENT_CPP || tokenType==TT_EOL);
    tokenSkippedSpaceOffset= pStart;
  }

  public String toString() {
    switch (tokenType) {
      case TT_BLOCK: return "{}-BLOCK:"+getToken();
    }
    return super.toString();
  }
}
