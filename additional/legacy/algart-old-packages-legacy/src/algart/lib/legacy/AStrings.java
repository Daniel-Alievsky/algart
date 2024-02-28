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

import java.util.*;

/**
 * <p>Title:     <b>Algart Strings</b><br>
 * Description:  Library of tools for working with strings<br>
 * @author       Daniel Alievsky
 * @version      1.1
 * @deprecated
 */

public class AStrings extends ATools {

  public static final String ENCODING_UTF8= "UTF-8";
  public static final String ENCODING_UTF16BE= "UnicodeBig";

  public static boolean between(int a, int b, int c) {
  /* Duplicated in ATools */
    return b<=c? a>=b && a<=c: a>=c && a<=b;
  }
  public static boolean between(int a, char[] bounds) {
  /* Analog of ATools.between() */
    for (int k=0; k<=bounds.length-2; k+=2) {
      if (between(a,bounds[k],bounds[k+1])) return true;
    }
    return false;
  }

  public static String substring(String s, int beginIndex, int endIndex) {
    try {
      int len= s.length();
      if (beginIndex<0) beginIndex= 0;
      if (endIndex>len) endIndex= len;
      return s.substring(beginIndex,endIndex);
    } catch (NullPointerException e) {
      return "";
    } catch (IndexOutOfBoundsException e) {
      return "";
    }
  }
  public static String substr(String s, int beginIndex, int len) {
    return substring(s,beginIndex,beginIndex+len);
  }

    /** Returns s if any string is null, or s is "", or oldSubstring is ""
   */
  public static String replace(String s, String oldSubstring, String newSubstring) {
    try {
      int len= s.length();
      int oldLen= oldSubstring.length();
      if (oldLen==0 || newSubstring==null) return s;
      StringBuffer sb= new StringBuffer(len);
      int p=0;
      for (;p<len;) {
        int q= s.indexOf(oldSubstring,p);
        if (q==-1) break;
        sb.append(s.substring(p,q)).append(newSubstring);
        p= q+oldLen;
      }
      sb.append(s.substring(p));
      return sb.toString();
    } catch (NullPointerException e) {
      return s;
    }
  }
  public static final int CASE_INSENSITIVE= java.util.regex.Pattern.CASE_INSENSITIVE;
  public static final int DOTALL=           java.util.regex.Pattern.DOTALL;

    public static boolean find(String s, String regex, int flags) {
    return java.util.regex.Pattern.compile(regex,flags).matcher(s).find();
  }
  public static String replaceAll(String s, String regex, String replacement, int flags) {
    return java.util.regex.Pattern.compile(regex,flags).matcher(s).replaceAll(replacement);
  }

    public static String beforeLastSubstring(String s, String sub) {
    int p= s.lastIndexOf(sub);
    if (p==-1) return s;
    return s.substring(0,p);
  }
  public static String afterLastSubstring(String s, String sub) {
    return s.substring(s.lastIndexOf(sub)+1);
  }

  public static String dup(char c, int len) {
    char[] ca= new char[len];
    for (int k=0; k<len; k++) ca[k]= c;
    return new String(ca);
  }
  public static String leftPad(String s, int len, char pad) {
    return s.length()>len? s: dup(pad,len-s.length())+s;
  }
  public static String leftTrim(String s) {
    int len= s.length();
    for (int p=0; p<len; p++) {
      if (s.charAt(p)>' ') return s.substring(p);
    }
    return "";
  }


  public static String addLFIfNotEmpty(String s) {
    if (s==null || s.length()==0) return "";
    return s+"\n";
  }

  /** Removes ending '\n' character or "\r\n" character pair;
   * also converts null to "".
   */
  public static String chomp(String s) {
    int len;
    if (s==null || (len=s.length())==0) return "";
    if (s.charAt(len-1)!='\n') return s;
    len--;
    if (len>0 && s.charAt(len - 1)=='\r') len--;
    return s.substring(0,len);
  }

  public static String normalizeLF(String s) {
    return normalizeLF(s, "\n");
  }
  public static String normalizeLF(String s, String newLineSeparator) {
    return s.replaceAll("\\r\\n?|\\n", newLineSeparator);
  }
  public static String deTab(String s, int tabLen) {
    return deTab(s, tabLen, '\n');
  }
  public static String deTab(String s, int tabLen, char lineSeparator) {
    if (tabLen<=0) throw new IllegalArgumentException("AStrings.deTab: illegal tabLen argument "+tabLen);
    if (s.indexOf('\t')==-1) return s;
    int len= s.length();
    StringBuffer sb= new StringBuffer(len);
    for (int k=0,pos=0; k<len; k++) {
      char c= s.charAt(k);
      if (c==lineSeparator) {
        sb.append(c); pos= 0;
      } else if (c=='\t') {
        do {
          sb.append(' '); pos++;
        } while (pos%tabLen!=0);
      } else {
        sb.append(c); pos++;
      }
    }
    return sb.toString();
  }

  public static String ctrlsToXml(String s) {
    return ctrlsToXml(s,false,false);
  }
  public static String ctrlsToXml(String s, boolean encodeForAttributes) {
    return ctrlsToXml(s, encodeForAttributes, false);
  }
  public static String ctrlsToXml(String s, boolean encodeForAttributes, boolean replaceLowCtrlsToSpace) {
    int len= s.length();
    final char[] chars = len < 2048 ? s.toCharArray() : null;
    StringBuffer sb= new StringBuffer(len);
    for (int k=0; k<len; k++) {
      final char c = chars != null ? chars[k] : s.charAt(k);
      switch (c) {
        case '&': sb.append("&amp;"); break;
        case '<': sb.append("&lt;"); break;
        case '>': sb.append("&gt;"); break;
        default:
          if (encodeForAttributes) {
            if (c=='"') {
              sb.append("&quot;");
            } else if (c<' ' || c>=0xFFFE) {
              if (replaceLowCtrlsToSpace) sb.append(' ');
              else sb.append("&#"+leftPad(Integer.toString(c),2,'0')+";");
            } else {
              sb.append(c);
            }
          } else {
            if ((c<' ' && !(c=='\n' || c=='\r' || c=='\t')) || c>=0xFFFE) {
              if (replaceLowCtrlsToSpace) sb.append(' ');
              else sb.append("&#"+leftPad(Integer.toString(c ),2,'0')+";");
            } else {
              sb.append(c);
            }
          }
          break;
      }
    }
    return sb.toString();
  }
  public static String xmlToCtrls(String s) {
    StringBuffer sb= new StringBuffer(s.length());
    return xmlToCtrls(s,0,s.length(),sb).toString();
  }
  public static String xmlToCtrls(String s, int start, int end) {
    StringBuffer sb= new StringBuffer();
    return xmlToCtrls(s,start,end,sb).toString();
  }
  public static StringBuffer xmlToCtrls(String s, int start, int end, StringBuffer sb) {
    if (sb==null) return null;
    for (int k=start; k<end; k++) {
      char c= s.charAt(k);
      if (c=='&' && k+3<end) {
        String sub= s.substring(k+1,Math.min(k+6,end));
        char d1,d2;
        if (sub.startsWith("amp;")) {sb.append('&'); k+=4;}
        else if (sub.startsWith("lt;")) {sb.append('<'); k+=3;}
        else if (sub.startsWith("gt;")) {sb.append('>'); k+=3;}
        else if (sub.startsWith("quot;")) {sb.append('"'); k+=5;}
        else if (sub.charAt(0)=='#' && k+4<end && sub.charAt(3)==';'
          && ((d1=sub.charAt(1))>='0') && d1<='9'
          && ((d2=sub.charAt(2))>='0') && d2<='9') {
          sb.append((char)((d1-'0')*10+d2-'0')); k+=4;
        } else sb.append(c);
      } else {
        sb.append(c);
      }
    }
    return sb;
  }

  public static String xmlAttr(String attrName, String value) {
    if (attrName==null || value==null || attrName.length()==0 || value.length()==0) return "";
    return " "+attrName+"=\""+ctrlsToXml(value,true)+"\"";
  }
  public static String xmlTag(String tagName, String text) {
    return xmlTag(tagName,text,true);
  }
  public static String xmlTag(String tagName, String text, boolean doEncodeText) {
    if (tagName==null || text==null || tagName.length()==0 || text.length()==0) return "";
    return "<"+tagName+">"+(doEncodeText?ctrlsToXml(text):text)+"</"+tagName+">";
  }

  public static String htmlPreformat(String html, boolean enableWordWrap) {
    return htmlPreformat(html,8,enableWordWrap,true,false);
  }
  public static String htmlPreformat(String ascii,
    int tabLen,
    boolean enableWordWrap,
    boolean addMonospaceFontTag,
    boolean addHtmlTag)
  {
    ascii= deTab(normalizeLF(ascii),tabLen);
    String prefix=
      (addHtmlTag? "<html><body>": "")
      +(addMonospaceFontTag? "<font face=\"monospace\">": "");
    String postfix=
      (addMonospaceFontTag? "</font>": "")
      +(addHtmlTag? "</body></html>": "");
    if (!enableWordWrap) {
      return prefix+replaceAll(
        replaceAll(ascii,"\\n","<br>",DOTALL),
        " ","&nbsp;",DOTALL)+postfix;
    }
    int len= ascii.length();
    StringBuffer sb= new StringBuffer(
      len+prefix.length()+postfix.length());
    sb.append(prefix);
    int k= 0;
    for (; k<len && ascii.charAt(k)==' '; k++) sb.append("&nbsp;");
    for (; k<len; k++) {
      char c= ascii.charAt(k);
      if (c=='\n') {
        sb.append("<br>");
        for (; k+1<len && ascii.charAt(k+1)==' '; k++) sb.append("&nbsp;");
      } else if (c==' ') {
        for (; k+1<len && ascii.charAt(k+1)==' '; k++) sb.append("&nbsp;");
        sb.append(' ');
      } else {
        sb.append(c);
      }
    }
    sb.append(postfix);
    return sb.toString();
  }

  public static class Attr {
    public final String name;
    public final String value;
    public Attr(String name, Object value) {
      String s= value+"";
      if (s.length()>=2 && s.startsWith("\"") && s.endsWith("\""))
        s= s.substring(1,s.length()-1);
      this.name= name;
      this.value= s;
    }
    public String toString() {
      return " "+ctrlsToXml(name,true)+"=\""+ctrlsToXml(value,true)+"\"";
    }
  }
  public static class Tag {
    public static final String XML_VERSION_SIMPLE= "<?xml version=\"1.0\"?>\n";
    public abstract static class LocalException extends AException {
      public LocalException(String s) {super(s);}
    }
    public static class NoTagsFound extends LocalException {
      public NoTagsFound() {super("No XML tags found in the text string");}
    }
    public String name;   // never null
    public Attr[] attrs;  // never null, new Attr[0] instead
    public Tag[] tags;    // never null, new Tag[0] instead
    public String text;   // never null, "" instead
    // For XML comments <!-- someword attr1="value1" ... any other text -->
    // we have:
    //    name= "!--someword" (spaced after <!-- are deleted if exist)
    //    attrs contains parsed attributes (if exist)
    //    tags is empty array
    //    text contains precise substring between <!-- and -->
    //      (instead usual "" used for single tags)
    public Map attrMap;
    public Map tagMap;
    public Tag(String name, Attr[] attrs, Tag[] tags, String text) {
      this.name= name+"";
      this.attrs= attrs==null? new Attr[0]: attrs;
      this.tags= tags==null? new Tag[0]: tags;
      this.text= text==null? "": text;
      correctMaps();
    }
    public void correctMaps() {
      attrMap= new HashMap();
      tagMap= new HashMap();
      for (int k=0; k<this.attrs.length; k++) attrMap.put(this.attrs[k].name,this.attrs[k].value);
      for (int k=0; k<this.tags.length; k++) {
        List subtags= (List)tagMap.get(this.tags[k].name);
        if (subtags==null) tagMap.put(this.tags[k].name,subtags=new ArrayList());
        subtags.add(this.tags[k]);
      }
    }
    public Tag(String name, Attr[] attrs, Tag[] tags)  {this(name,attrs,tags,null);}
    public Tag(String name, Attr[] attrs, String text) {this(name,attrs,null,text);}
    public Tag(String name, Attr[] attrs)              {this(name,attrs,null,null);}
    public Tag(String name, Tag[] tags, String text)   {this(name,null,tags,text);}
    public Tag(String name, Tag[] tags)                {this(name,null,tags,null);}
    public Tag(String name, String text)               {this(name,null,null,text);}
    public Tag(String xml) throws NoTagsFound {
      this(xml,0,xml.length(),null);
    }

    public int spaceLenForChildren= 2;
    public Tag(String name, Attr[] attrs, Tag[] tags, String text, int spaceLenForChildren)  {
      this(name,attrs,tags,text);
      this.spaceLenForChildren= spaceLenForChildren;
    }
    public Tag(String name, Attr[] attrs, Tag[] tags, int spaceLenForChildren)  {this(name,attrs,tags,null,spaceLenForChildren);}
    public Tag(String name, Attr[] attrs, String text, int spaceLenForChildren) {this(name,attrs,null,text,spaceLenForChildren);}
    public Tag(String name, Attr[] attrs, int spaceLenForChildren)              {this(name,attrs,null,null,spaceLenForChildren);}
    public Tag(String name, Tag[] tags, String text, int spaceLenForChildren)   {this(name,null,tags,text,spaceLenForChildren);}
    public Tag(String name, Tag[] tags, int spaceLenForChildren)                {this(name,null,tags,null,spaceLenForChildren);}
    public Tag(String name, String text, int spaceLenForChildren)               {this(name,null,null,text,spaceLenForChildren);}
    public Tag(String xml, int spaceLenForChildren) throws NoTagsFound {
      this(xml);
      this.spaceLenForChildren= spaceLenForChildren;
    }

    public boolean isSingle() {
      if (name.startsWith("!--")) return true;
      return text.length()==0 && tags.length==0;
    }
    public int wrapMarginForAttributes= 64;
    public String toString() {
      return toString(0,false);
    }
    public String toString(boolean useParsedComments) {
      return toString(0,useParsedComments);
    }
    public String toString(int spaceLen) {
      return toString(spaceLen,false);
    }
    public String toString(int spaceLen, boolean useParsedComments) {
      String space= dup(' ',spaceLen);
      StringBuffer sb= new StringBuffer(space+'<');
      if (name.startsWith("!--")) {
        if (useParsedComments) {
          sb.append(ctrlsToXml(name));
          for (int k=0; k<attrs.length; k++) sb.append(attrs[k]);
        } else {
          sb.append("!--"+text);
        }
        sb.append("-->");
        return sb.toString();
      } else {
        sb.append(ctrlsToXml(name,true));
      }
      StringBuffer allAttr= new StringBuffer();
      for (int k=0; k<attrs.length; k++) allAttr.append(attrs[k]);
      if (allAttr.length()<=this.wrapMarginForAttributes) {
        sb.append(allAttr);
      } else {
        for (int k=0; k<attrs.length; k++) sb.append('\n').append(space).append(attrs[k]);
      }
      String textTrim= text.trim();
      boolean isSingle= textTrim.length()==0 && tags.length==0;
      if (isSingle) sb.append('/');
      sb.append('>');
      if (!isSingle) {
        for (int k=0; k<tags.length; k++) {
          sb.append('\n').append(tags[k].toString(
            spaceLen+this.spaceLenForChildren,useParsedComments));
        }
        sb.append(ctrlsToXml(textTrim,false));
        if (tags.length>0) sb.append('\n'+space);
        sb.append("</"+ctrlsToXml(name,true)+">");
      }
      return sb.toString();
    }
    public String attr(String name) {return (String)attrMap.get(name);}
    public int intAttr(String name) {return Integer.parseInt(attr(name));}
    public double doubleAttr(String name) {return Double.parseDouble(attr(name));}
    public boolean booleanAttr(String name) {return Boolean.valueOf(attr(name)).booleanValue();}
    public Tag[] tag(String name) {
      List subtags= (List)tagMap.get(name);
      if (subtags==null) return new Tag[0];
      return (Tag[])subtags.toArray(new Tag[0]);
    }

    public Tag[] allTags() {
      return allTags((String)null);
    }
    public Tag[] allTags(String name) {
      List allTagsList= new ArrayList();
      allTags(name,allTagsList);
      return (Tag[])allTagsList.toArray(new Tag[0]);
    }
    public List allTags(List allTagsList) {
      return allTags(null,allTagsList);
    }
    public List allTags(String name, List allTagsList) {
      for (int k=0; k<tags.length; k++) {
        if (name==null || tags[k].name.equals(name)) allTagsList.add(tags[k]);
        tags[k].allTags(name,allTagsList);
      }
      return allTagsList;
    }
    public int startIndex;        // position of start < in parsed string
    public int endIndex;          // 1+ position of end > in parsed string
    public int startIndexInner;   // 1+ position of end > of open tag
    public int endIndexInner;     // position of start < of close tag; ==startIndexInner for single tag
    private Tag(String xml, int start, int end, StringBuffer outerText) throws NoTagsFound {
      Tag tag= parse(xml,start,end,outerText);
      if (tag==null) throw new NoTagsFound();
      this.name= tag.name;
      this.attrs= tag.attrs;
      this.tags= tag.tags;
      this.text= tag.text;
      this.attrMap= tag.attrMap;
      this.tagMap= tag.tagMap;
    }

    public static Tag parse(String xml) {
      return parse(xml,0,xml.length(),null);
    }
    public static Tag parse(String xml, StringBuffer outerText) {
      return parse(xml,0,xml.length(),outerText);
    }
    public static Tag parse(String xml, int start) {
      return parse(xml,start,xml.length(),null);
    }
    public static Tag parse(String xml, int start, int end) {
      return parse(xml,start,end,null);
    }
    public static Tag parse(String xml, int start, int end, StringBuffer outerText) {
    // Returns null if no tag found
      if (start>=end) return null;
      int plt;
      for (plt=start;;) {
        plt= xml.indexOf('<',plt);
        if (plt==-1 || plt>=end) return null;
        if (plt+2>end || xml.charAt(plt+1)!='?') break;
        int pclose= xml.indexOf("?>",plt+2);
        if (pclose==-1 || pclose>=end) return null;
        plt= pclose+2;
      }
      boolean isSingle;
      String nameClear,name;
      StringBuffer innerText;
      int psp,pgt,ptagend,pclose=0,pcloseend;
      if (plt+7<=end && xml.startsWith("!--",plt+1)) {
        ptagend= xml.indexOf("-->",plt+4);
        if (ptagend==-1 || ptagend>=end) return null;
        pgt= ptagend+2;
        pcloseend= ptagend+3;
        isSingle= true;

        int pname= plt+4;
        while (pname<ptagend && xml.charAt(pname)<=' ') pname++;
        psp= pname;
        while (psp<ptagend && xml.charAt(psp)>' ') psp++;
        nameClear= "!--"+xml.substring(pname,psp);
        name= xmlToCtrls(nameClear);
        innerText= new StringBuffer(xml.substring(plt+4,ptagend));
      } else {
        pgt= xml.indexOf('>',plt);
        if (pgt==-1 || pgt>=end) return null;
        isSingle= pgt>=plt+2 && xml.charAt(pgt-1)=='/';
        ptagend= isSingle? pgt-1: pgt; // excluding / before >
        if (pgt>=plt+2 && xml.charAt(plt+1)=='/') isSingle= true;
          // for closing tags - used in recursion

        psp= plt+1;
        while (psp<ptagend && xml.charAt(psp)>' ') psp++;
        nameClear= xml.substring(plt+1,psp);
        name= xmlToCtrls(nameClear);
        pcloseend= pgt+1;
        innerText= new StringBuffer();
      }
      xmlToCtrls(xml,start,plt,outerText);

      Tag[] tags= null;
      if (!isSingle) {
        String nameClose= "/"+name;
        boolean foundCorrespondingClose= false;
        List tagList= new ArrayList();
        int p= pgt+1;
        for (int count=0; count<10000000; count++) {
          Tag tag= parse(xml,p,end,innerText);
          if (tag==null) break;
          if (tag.name.startsWith("/")) {
            foundCorrespondingClose= tag.name.equals(nameClose);
            if (foundCorrespondingClose) {
              pclose= tag.startIndex;
              pcloseend= tag.endIndex;
            }
            break;
          }
          p= tag.endIndex;
          tagList.add(tag);
        }
        isSingle= !foundCorrespondingClose;
        if (isSingle) innerText.setLength(0);
        else          tags= (Tag[])tagList.toArray(new Tag[0]);
      }

      List attrList= new ArrayList();
      for (; psp<ptagend; ) {
        char c= 0;
        int pname= psp+1;
        while (pname<ptagend && xml.charAt(pname)<=' ') pname++;
        if (pname>=ptagend) break;
        int pnamesp= pname;
        while (pnamesp<ptagend && (c=xml.charAt(pnamesp))>' ' && c!='=') pnamesp++;
        if (pnamesp>=ptagend) break;
        int peq= pnamesp;
        while (peq<ptagend && (c=xml.charAt(peq))<=' ' && c!='=') peq++;
        if (peq>=ptagend) break;
        if (c!='=') break;
        int pvalue= peq+1;
        while (pvalue<ptagend && (c=xml.charAt(pvalue))<=' ') pvalue++;
        if (pvalue>=ptagend) break;
        psp= pvalue+1;
        if (c=='"') {
          while (psp<ptagend && xml.charAt(psp)!='"') psp++;
          if (psp>=ptagend) break;
        }
        while (psp<ptagend && xml.charAt(psp)>' ') psp++;
        attrList.add(new Attr(xmlToCtrls(xml,pname,pnamesp),xmlToCtrls(xml,pvalue,psp)));
      }
      Tag result= new Tag(name,
        (Attr[])attrList.toArray(new Attr[0]),
        tags,
        innerText.toString());
      result.startIndex= plt;
      result.startIndexInner= pgt+1;
      result.endIndex= pcloseend;
      result.endIndexInner= isSingle?result.startIndexInner:pclose;
      return result;
    }
    public static Tag[] parseTags(String xml) {
      return parseTags(xml,null,0,xml.length(),null);
    }
    public static Tag[] parseTags(String xml, StringBuffer outerText) {
      return parseTags(xml,null,0,xml.length(),outerText);
    }
    public static Tag[] parseTags(String xml, int start, int end) {
      return parseTags(xml,null,start,end,null);
    }
    public static Tag[] parseTags(String xml, int start, int end, StringBuffer outerText) {
      return parseTags(xml,null,start,end,outerText);
    }
    public static Tag[] parseTags(String xml, String name) {
      return parseTags(xml,name,0,xml.length(),null);
    }
    public static Tag[] parseTags(String xml, String name, StringBuffer outerText) {
      return parseTags(xml,name,0,xml.length(),outerText);
    }
    public static Tag[] parseTags(String xml, String name, int start, int end) {
      return parseTags(xml,name,start,end,null);
    }
    public static Tag[] parseTags(String xml, String name, int start, int end, StringBuffer outerText) {
      List tagList= new ArrayList();
      int p= start;
      for (;;) {
        Tag tag= parse(xml,p,end,outerText);
        if (tag==null) break;
        p= tag.endIndex;
        if (name==null || tag.name.equals(name))
          tagList.add(tag);
      }
      if (p<end) xmlToCtrls(xml,p,end,outerText);
      return (Tag[])tagList.toArray(new Tag[0]);
    }
    public static Tag[] parseAllTags(String xml) {
      return parseAllTags(xml,null,0,xml.length(),null);
    }
    public static Tag[] parseAllTags(String xml, StringBuffer outerText) {
      return parseAllTags(xml,null,0,xml.length(),outerText);
    }
    public static Tag[] parseAllTags(String xml, int start, int end) {
      return parseAllTags(xml,null,start,end,null);
    }
    public static Tag[] parseAllTags(String xml, int start, int end, StringBuffer outerText) {
      return parseAllTags(xml,null,start,end,outerText);
    }
    public static Tag[] parseAllTags(String xml, String name) {
      return parseAllTags(xml,name,null);
    }
    public static Tag[] parseAllTags(String xml, String name, StringBuffer outerText) {
      return parseAllTags(xml,name,0,xml.length(),outerText);
    }
    public static Tag[] parseAllTags(String xml, String name, int start, int end) {
      return parseAllTags(xml,name,start,end,null);
    }
    public static Tag[] parseAllTags(String xml, String name, int start, int end, StringBuffer outerText) {
      List allTagsList= new ArrayList();
      Tag[] tags= parseTags(xml,start,end,outerText);
      for (int k=0; k<tags.length; k++) {
        if (name==null || tags[k].name.equals(name)) allTagsList.add(tags[k]);
        tags[k].allTags(name,allTagsList);
      }
      return (Tag[])allTagsList.toArray(new Tag[0]);
    }
  }


  public static class NextInfo {
    public String value;
    public int index;
    public NextInfo(int index, String value) {
      this.value= value;
      this.index= index;
    }
  }
  public static NextInfo findNextQuote(String s, int start) {
    if (start>=s.length()) return null;
    char c= s.charAt(start);
    if (c!='"' && c!='\'') return null;
    return findNextQuote(s,start,c);
  }
  public static NextInfo findNextQuote(String s, int start, char quote) {
    StringBuffer sb= new StringBuffer();
    int p= start;
    int len= s.length();
    if (p>=len || s.charAt(p++)!=quote) return null;
    char c;
    while (p<len && (c=s.charAt(p))!=quote) {
      p++;
      if (c=='\\' && p<len) {
        switch (c=s.charAt(p)) {
          case '\\': c= '\\'; break;
          case 'n':  c= '\n'; break;
          case 'r':  c= '\r'; break;
          case 'b':  c= '\b'; break;
          case 't':  c= '\t'; break;
          case 'f':  c= '\f'; break;
          case '\'': c= '\''; break;
          case '"':  c= '"'; break;
        }
        p++;
      }
      sb.append(c);
    }
    return new NextInfo(p,sb.toString());
  }

  public static NextInfo findSubstringIgnoringQuotedStart(String s, String match, int start) {
    NextInfo result= findNextQuote(s,start);
    if (result==null) {
      if (match==null) return new NextInfo(s.length(),s.substring(start));
      int index= s.indexOf(match,start);
      if (index==-1) return null;
      return new NextInfo(index,s.substring(start,index));
    }
    if (match==null) return result;
    result.index= s.indexOf(match,result.index+1);
    if (result.index==-1) return null;
    return result;
  }

  public static String[] split(String s) {
    return split(s," \t\n\r\f");
  }
  public static String[] split(String s, String separators) {
    if (s==null) return new String[0];
    StringTokenizer stok= new StringTokenizer(s,separators);
    String[] result= new String[stok.countTokens()];
    for (int k=0; k<result.length; k++) {
      result[k]= stok.nextToken();
    }
    return result;
  }

  public static String javaNameToTagName(String name) {
    return javaNameToWords(name,"-");
  }
  public static String javaNameToWords(String name, String separator) {
    StringBuffer sb = new StringBuffer();
    char chLast= 0;
    for (int k=0; k<name.length(); k++) {
      char ch= name.charAt(k);
      if (Character.isLetter(ch) && Character.isUpperCase(ch)
        && (k==0 || Character.isLetter(chLast) || Character.isDigit(chLast))) {
        if (k>0) sb.append(separator);
        sb.append(Character.toLowerCase(ch));
      } else {
        sb.append(ch);
      }
      chLast= ch;
    }
    return sb.toString();
  }
}
