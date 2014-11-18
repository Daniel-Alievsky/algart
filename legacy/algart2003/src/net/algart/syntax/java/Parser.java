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

import java.util.*;
import net.algart.lib.*;

public class Parser implements java.io.Serializable {
  public static final int DEBUG_LEVEL = GlobalProperties.getClassDebugLevel(Parser.class);

  public String javaFileName= null; //can be set for more precise inrormation in exceptions
  public abstract class LocalException extends AbstractException {
    public String infoJavaFileName= javaFileName;
    public LocalException(String s) {super(s);}
  }
  public abstract class SyntaxException extends LocalException {
    public int infoLine;
    public int infoColumn;
    public SyntaxException(String s) {
      super(s);
      infoLine= et.getCurrentLine();
      infoColumn= et.getCurrentColumn();
      setMessage(s+" at line "+infoLine+", column "+infoColumn);
    }
  }
  public class SyntaxNextFieldOfSameTypeException extends SyntaxException {
    public SyntaxNextFieldOfSameTypeException() {super("Next field of the same type expected after ','");}
  }
  public class SyntaxIdentifierExpectedException extends SyntaxException {
    public SyntaxIdentifierExpectedException() {super("Identifier expected");}
  }
  public class SyntaxSemicolonExpectedException extends SyntaxException {
    public SyntaxSemicolonExpectedException() {super("';' expected");}
  }
  public class SyntaxSemicolonExpectedAfterSeverelFieldsException extends SyntaxException {
    public SyntaxSemicolonExpectedAfterSeverelFieldsException() {super("';' expected after severel fields of the same type");}
  }
  public class SyntaxCommaOrSemicolonOrEqualsExpectedAfterFieldNameException extends SyntaxException {
    public SyntaxCommaOrSemicolonOrEqualsExpectedAfterFieldNameException() {super("',' or ';' or '=' expected after fields name");}
  }
  public class SyntaxArgTypeExpectedException extends SyntaxException {
    public SyntaxArgTypeExpectedException() {super("Method argument type expected");}
  }
  public class SyntaxArgNameExpectedException extends SyntaxException {
    public SyntaxArgNameExpectedException() {super("Method argument name expected");}
  }
  public class SyntaxNextArgOrBraceExpectedException extends SyntaxException {
    public SyntaxNextArgOrBraceExpectedException() {super("Next method argument or right brace expected");}
  }
  public class SyntaxIllegalThrowsException extends SyntaxException {
    public SyntaxIllegalThrowsException() {super("Illegal \"throws\" declaration");}
  }
  public class SyntaxWordAfterDotExpectedException extends SyntaxException {
    public SyntaxWordAfterDotExpectedException() {super("Illegal identifier: word expected after '.'");}
  }
  public class SyntaxRightSquareBracketExpectedException extends SyntaxException {
    public SyntaxRightSquareBracketExpectedException() {super("Illegal array type: ']' expected after '['");}
  }

  public final transient ExtendedTokenizer et;
  public Parser() {
    et= null;
  }
  public Parser(char[] src, boolean decodeUnicodeEscapes) {
    if (decodeUnicodeEscapes) src= Strings.decodeUnicodeEscapes(src);
    et= new ExtendedTokenizer(src);
  }
  public Parser(String src, boolean decodeUnicodeEscapes) {
    if (decodeUnicodeEscapes) src= Strings.decodeUnicodeEscapes(src);
    et= new ExtendedTokenizer(src);
  }
  public static final char NESTED_CLASS_SEPARATOR= '\\';
    // Not '$', because '$' can be used in Java identifiers of local classes
    // Should be replaced to '.' or '$' in all member names while printing
    // or using together with reflection
  public static final char MEMBER_SEPARATOR= '.';
  public char activeLocalClassSeparator= '$';
    // NESTED_CLASS_SEPARATOR in member names is automatically replaced
    // to activeLocalClassSeparator by name(), all toString() methods
    // and by getXxxMap() methods
    // activeLocalClassSeparator is '$' by default

  public boolean fillMethodsBodies= true;
    // if false, parse() will not fill "body" field in "MethodSource" classes

  public abstract class MemberSource implements java.io.Serializable {
    public String internalName; // "BaseClass/LocalClass/LocalClass.memberName"
    public int indexInList;
    public String name() {
      return name(Parser.this.activeLocalClassSeparator);
    }
    public String name(char activeLocalClassSeparator) {
      return internalName.replace(NESTED_CLASS_SEPARATOR,activeLocalClassSeparator);
    }
    public String signature() {
    // Overridden by MethodSource()
      return name();
    }
    public String previousSignature= null;
    // Filled by expandMethodArgumentTypes()
    public String[] appendLocalImports(String[] imports) {
      List list= new ArrayList();
      for (int j=this.internalName.length()-1; j>=0; j--) {
        char c= this.internalName.charAt(j);
        if (c==NESTED_CLASS_SEPARATOR || c==MEMBER_SEPARATOR) {
          list.add(
            (currentPackage==null? "": currentPackage.internalName.replace('.','/')+"/")
            +this.internalName.substring(0,j)+"\\*");
        }
      }
      for (int j=0; j<imports.length; j++) list.add(imports[j]);
      return (String[])list.toArray(new String[0]);
    }
  }

  public class PackageSource extends MemberSource {
    public PackageSource(String packageString, int indexInList) {
      this.internalName= packageString;
      this.indexInList= indexInList;
    }
    public String toString() {
      return "Package declaration "+name()+"\n";
    }
  }

  public class ImportSource extends MemberSource {
    public ImportSource(String importString, int indexInList) {
      this.internalName= importString;
      this.indexInList= indexInList;
    }
    public String toString() {
      return "Import declaration "+name()+"\n";
    }
  }

  public class FieldSource extends MemberSource {
    public String initializingExpression; // null when absent
    public String comment; // null when absent
    public FieldSource(String name, String initializingExpression, String comment, int indexInList) {
      this.internalName= name;
      this.initializingExpression= initializingExpression;
      this.comment= comment;
      this.indexInList= indexInList;
    }
    public FieldSource(String name, String comment, int indexInList) {
      this(name,null,comment,indexInList);
    }
    public String toString() {
      return "Field "+name()
        +(initializingExpression==null? "": "="+initializingExpression)
        +(comment==null? ";": "; /*"+comment.replaceAll("\\/\\*","/[[*]]").replaceAll("\\*\\/","[[*]]/")+"*/")
        +"\n";
    }
  }

  public class MethodSource extends MemberSource {
    public MethodArgumentSource[] args;
    public String throwsDescription; // "" when absent
    public String startComment; // possible start part of body; null when absent
    public String body;
    public MethodSource(
      String name,
      MethodArgumentSource[] args,
      String throwsDescription,
      String startComment,
      String body,
      int indexInList) {
      this.internalName= name;
      this.args= args;
      this.throwsDescription= throwsDescription;
      this.startComment= startComment;
      this.body= body;
      this.indexInList= indexInList;
    }
    public String signature() {
      StringBuffer sb= new StringBuffer(name());
      sb.append("(");
      for (int k=0; k<args.length; k++) {
        if (k>0) sb.append(",");
        sb.append(args[k].type);
      }
      sb.append(")");
      return sb.toString();
    }
    public String toString() {
      return toString(new MethodArgumentToString(),true);
    }
    public String toString(MethodArgumentToString mats, boolean showBody) {
      StringBuffer sb= new StringBuffer();
      sb.append("Method "+name()
        +(throwsDescription==null?"":" "+throwsDescription)
        +"(");

      for (int k=0; k<args.length; k++) {
        if (k>0) sb.append(",");
        sb.append("\n  "+mats.toString(args[k]));
      }
      sb.append(args.length==0?")":"\n)");
      if (startComment!=null)
        sb.append(" Comment: /*"
          +startComment.replaceAll("\\/\\*","/[[*]]").replaceAll("\\*\\/","[[*]]/")
          +"*/");
      if (showBody) sb.append(" Body: "+body);
      sb.append("\n");
      return sb.toString();
    }
  }

  public class ClassSource extends MemberSource {
    public ClassSource(String name, int indexInList) {
      this.internalName= name;
      this.indexInList= indexInList;
    }
    public String toString() {
      return "Class "+name()+"\n";
    }
  }

  public class InterfaceSource extends MemberSource {
    public InterfaceSource(String name, int indexInList) {
      this.internalName= name;
      this.indexInList= indexInList;
    }
    public String toString() {
      return "Interface "+name()+"\n";
    }
  }

  public static class MethodArgumentSource implements java.io.Serializable {
    public String type;
    public String name;
    public String comment; // null when absent
    public MethodArgumentSource(String type, String name, String comment) {
      this.type= type;
      this.name= name;
      this.comment= comment;
    }
    public String toString() {
      return type+" "+name
        +(comment==null? "": " /*"+comment.replaceAll("\\/\\*","/[[*]]").replaceAll("\\*\\/","[[*]]/")+"*/");
    }
  }
  public static class MethodArgumentToString {
    public String toString(MethodArgumentSource arg) {
      return arg.toString();
    }
  }

  public PackageSource currentPackage= null;
  public List memberSources = new ArrayList();  //of MemberSource
  public void parse() throws SyntaxException {
    do {
      et.nextTokenSignificant();
      if (et.tokenType==Tokenizer.TT_WORD) {
        String token= et.getToken();
        boolean isImport;
        if (token.equals("class")) {
          parseClass(false);
        } else if ((isImport=token.equals("import")) || token.equals("package")) {
          et.nextTokenSignificant();
          if (et.tokenType!=Tokenizer.TT_WORD) throw new SyntaxIdentifierExpectedException();
          if (isImport) memberSources.add(new ImportSource(parseIdentifierWithDots(true),memberSources.size()));
          else          memberSources.add(currentPackage= new PackageSource(parseIdentifierWithDots(false),-1));
          if (et.tokenType!=';') throw new SyntaxSemicolonExpectedException();
        }
      }
    } while (et.tokenType!=Tokenizer.TT_EOF);
  }

  public static final int METHODS_MAPPING_NAME= 0;
  public static final int METHODS_MAPPING_ONLY_NOT_OVERLOADED= 1;
  public static final int METHODS_MAPPING_SIGNATURE= 2;
  public static final int METHODS_MAPPING_PREVIOUS_SIGNATURE= 3;
  public Map getFieldsMap() {
    return getMembersMap(FieldSource.class,0);
  }
  public Map getMethodsMap() {
    return getMethodsMap(METHODS_MAPPING_SIGNATURE);
  }
  public Map getMethodsMap(int methodsMappingMode) {
    return getMembersMap(MethodSource.class,methodsMappingMode);
  }
  public Set getNotOverloadedMethodsSet() {
    Set result= new HashSet();
    Set overloaded= new HashSet();
    for (Iterator it= memberSources.iterator(); it.hasNext(); ) {
      MemberSource ms= (MemberSource)it.next();
      if (!(ms instanceof MethodSource)) continue;
      if (overloaded.contains(ms.name())) continue;
      if (!result.add(ms.name())) {
        overloaded.add(ms.name());
        result.remove(ms.name());
      }
    }
    return result;
  }
  public Map getClassesMap() {
    return getMembersMap(ClassSource.class,0);
  }
  public Map getInterfacesMap() {
    return getMembersMap(InterfaceSource.class,0);
  }
  private Map getMembersMap(Class requiredClass, int methodsMappingMode) {
    Map result= new HashMap();
    Set overloaded= new HashSet();
    for (Iterator it= memberSources.iterator(); it.hasNext(); ) {
      MemberSource ms= (MemberSource)it.next();
      if (!requiredClass.isInstance(ms)) continue;
      switch (methodsMappingMode) {
        case METHODS_MAPPING_SIGNATURE:
          result.put(ms.signature(),ms);
          break;
        case METHODS_MAPPING_PREVIOUS_SIGNATURE:
          result.put(ms.previousSignature,ms);
          break;
        case METHODS_MAPPING_ONLY_NOT_OVERLOADED:
          if (overloaded.contains(ms.name())) continue;
          if (result.put(ms.name(),ms)!=null) {
            overloaded.add(ms.name());
            result.remove(ms.name());
          }
          break;
        default:
          result.put(ms.name(),ms);
          break;
      }
    }
    return result;
  }
  public String[] getAllImports() {
    List result= new ArrayList();
    if (currentPackage!=null) result.add(currentPackage.internalName+".*");
    result.add("java.lang.*");
    for (Iterator it= memberSources.iterator(); it.hasNext(); ) {
      MemberSource ms= (MemberSource)it.next();
      if (ms instanceof ImportSource) result.add(ms.internalName);
    }
    return (String[])result.toArray(new String[0]);
  }

  public void expandMethodArgumentTypes(ClassLoader classLoader, String[] imports) {
  // imports[] should be corrected by Types.correctImports()
  // Note: this method replaces ALL '$' characters in classes names with '.',
  // even if these characters don't separate local classes names, but are
  // the usual chararacters in Java identifier. For example,
  // the name of following local class:
  //    public class MyParentClass {
  //       public static class MyClass$$ {...}
  //    }
  // will be expanded as "(packageName).MyParentClass.MyClass.."
    for (Iterator it= memberSources.iterator(); it.hasNext(); ) {
      Object o= it.next();
      if (o instanceof MethodSource) {
        MethodSource ms= (MethodSource)o;
        ms.previousSignature= ms.signature();
        String[] localImports= ms.appendLocalImports(imports);
        for (int k=0; k<ms.args.length; k++) {
          Class argClass= Types.forJavaName(
            ms.args[k].type,classLoader,localImports);
          if (argClass!=null)
            ms.args[k].type= JVM.toJavaName(argClass,true);
        }
      }
    }
  }

  public String currentClassName= "";
  public void parseClass(boolean isInterface) throws SyntaxException {
  // All parseXxx methods parse the current token and read the next one
    String saveClassName= currentClassName;
    try {
      et.nextTokenSignificant();
      String className= et.tokenType==Tokenizer.TT_WORD? et.getToken(): "?";
      if (currentClassName.length()>0) currentClassName+= this.NESTED_CLASS_SEPARATOR;
      currentClassName+= className;

      if (isInterface) memberSources.add(new InterfaceSource(currentClassName,memberSources.size()));
      else             memberSources.add(new ClassSource(currentClassName,memberSources.size()));

      while (et.tokenType!='{' && et.tokenType!=Tokenizer.TT_EOF) et.nextToken();
      if (DEBUG_LEVEL>=1) System.out.println("Class "+currentClassName+" started: "+et.getCurrentLine()+","+et.getCurrentColumn());

      int lastTokenType= Tokenizer.TT_NOTHING;
      String lastToken= null;
      while (et.tokenType!='}' && et.tokenType!=Tokenizer.TT_EOF) {
        et.nextTokenSignificantOrBlock();
        parseSquareBrackets();
        String token;
        if (et.tokenType==',' || et.tokenType=='=' || et.tokenType==';') {
          parseField(lastToken);
        } else if (et.tokenType=='(' && lastTokenType==Tokenizer.TT_WORD) {
          parseMethod(lastToken);
        } else if (et.tokenType==Tokenizer.TT_WORD && ((token= et.getToken()).equals("class")
          || token.equals("interface"))) {
          parseClass(token.equals("interface"));
        }
        lastTokenType= et.tokenType;
        lastToken= lastTokenType==Tokenizer.TT_WORD? et.getToken(): null;
      }

      if (DEBUG_LEVEL>=1) System.out.println("Class "+currentClassName+" finished: "+et.getCurrentLine()+","+et.getCurrentColumn());
      et.nextTokenSignificantOrBlock();
    } finally {
      currentClassName= saveClassName;
    }
  }

  public void parseField(String fieldName) throws SyntaxException {
    if (DEBUG_LEVEL>=1) System.out.println(" Field "+currentClassName+this.MEMBER_SEPARATOR+fieldName+" started: "+et.getCurrentLine()+","+et.getCurrentColumn());
    int oldLen= memberSources.size();
    String initializingExpression= parseFieldInitializingExpression();
    boolean commentsFound= false;
    if (et.tokenType==',') {
      do {
        et.nextTokenSignificant(true);
        memberSources.add(new FieldSource(
          currentClassName+this.MEMBER_SEPARATOR+fieldName,
          initializingExpression,
          et.tokenSkippedComments==null?null:et.tokenSkippedComments.toString(),
          memberSources.size()));
        if (et.tokenSkippedComments!=null) commentsFound= true;
        if (et.tokenType!=Tokenizer.TT_WORD) throw new SyntaxNextFieldOfSameTypeException();
        fieldName= et.getToken();
        et.nextTokenSignificant();
        parseSquareBrackets();
        initializingExpression= parseFieldInitializingExpression();
      } while (et.tokenType==',');
      if (et.tokenType!=';') throw new SyntaxSemicolonExpectedAfterSeverelFieldsException();
    }
    int newLen= memberSources.size();

    if (et.tokenType!=';') throw new SyntaxCommaOrSemicolonOrEqualsExpectedAfterFieldNameException();
    et.nextTokenSignificant(true);
    memberSources.add(new FieldSource(
      currentClassName+this.MEMBER_SEPARATOR+fieldName,
      initializingExpression,
      et.tokenSkippedComments==null?null:et.tokenSkippedComments.toString(),
      memberSources.size()));
    if (!commentsFound && et.tokenSkippedComments!=null) {
      for (int k=oldLen; k<newLen; k++)
        ((FieldSource)memberSources.get(k)).comment= et.tokenSkippedComments.toString();
    }
  }
  public String parseFieldInitializingExpression() throws SyntaxException {
    if (et.tokenType!='=') return null;
    int p= et.offset();
    do et.nextTokenSignificantOrBraceBlock('{','}','(',')');
    while (et.tokenType!=';' && et.tokenType!=',' && et.tokenType!=Tokenizer.TT_EOF);
    return et.getSrc(p,et.tokenOffset-p);
  }

  public void parseMethod(String methodName) throws SyntaxException {
    if (DEBUG_LEVEL>=1) System.out.println(" Method "+currentClassName+this.MEMBER_SEPARATOR+methodName+" started: "+et.getCurrentLine()+","+et.getCurrentColumn());
    List arglist= new ArrayList();
    et.nextTokenSignificant();
    while (et.tokenType!=')') {
      String argType= parseMethodArgType();
      String argName= parseMethodArgName();
      argType+= parseMethodArgNameSquareBrackets;
      String argComment= parseMethodArgDelim();
      arglist.add(new MethodArgumentSource(argType,argName,argComment));
    }
    et.nextTokenSignificant();

    String throwsDescription= parseMethodThrowsDescription(false);
    String startComment= null;
    String body= null;
    if (et.tokenType!=';') {  // some methods can be abstract
//    while (et.tokenType!='{' && et.tokenType!=Tokenizer.TT_EOF) et.nextTokenSignificant();
        // scanning for the first {; it is not too correct, by allows to
        // understand, in future, some possible enhancements such as
        // adding "virtual" keywords after arguments list.
//    REMOVED: now I think it isn't the best idea
      if (et.tokenType=='{') {
        int savep= et.offset()-1;
        et.nextTokenSignificant(true);
        startComment= et.tokenSkippedComments==null?null:et.tokenSkippedComments.toString();
        et.seek(savep);
        et.nextTokenSignificantOrBlock();
        body= et.getToken();
      }
    }
    if (DEBUG_LEVEL>=1) System.out.println(" Method "+currentClassName+this.MEMBER_SEPARATOR+methodName+" finished: "+et.getCurrentLine()+","+et.getCurrentColumn());
    et.nextTokenSignificantOrBlock();

    MethodArgumentSource[] args= new MethodArgumentSource[arglist.size()];
    for (int k=0; k<args.length; k++) args[k]= (MethodArgumentSource)arglist.get(k);
    memberSources.add(new MethodSource(
      currentClassName+this.MEMBER_SEPARATOR+methodName,args,throwsDescription,startComment,
      fillMethodsBodies? body: null,
      memberSources.size()));
  }

  public String parseMethodArgType() throws SyntaxException {
    if (et.tokenType!=Tokenizer.TT_WORD) throw new SyntaxArgTypeExpectedException();
    if (et.getToken().equals("final")) {
      et.nextTokenSignificant();
      if (et.tokenType!=Tokenizer.TT_WORD) throw new SyntaxArgTypeExpectedException();
    }
    return parseIdentifierWithDots(false) + parseSquareBrackets();
  }
  public String parseMethodArgNameSquareBrackets= "";
  public String parseMethodArgName() throws SyntaxException {
    if (et.tokenType!=Tokenizer.TT_WORD) throw new SyntaxArgNameExpectedException();
    String argName= et.getToken();
    et.nextTokenSignificant(true);
    parseMethodArgNameSquareBrackets= parseSquareBrackets();
    return argName;
  }
  public String parseMethodArgDelim() throws SyntaxException {
    if (et.tokenType==')') return et.tokenSkippedComments==null?null:et.tokenSkippedComments.toString();
    if (et.tokenType!=',') throw new SyntaxNextArgOrBraceExpectedException();
    et.nextTokenSignificant(true);
    if (et.tokenType!=Tokenizer.TT_WORD) throw new SyntaxNextArgOrBraceExpectedException();
    return et.tokenSkippedComments==null?null:et.tokenSkippedComments.toString();
  }
  public String parseMethodThrowsDescription(boolean parseNextBraceBlock) throws SyntaxException {
    if (et.tokenType==Tokenizer.TT_WORD && et.getToken().equals("throws")) {
      StringBuffer result= new StringBuffer("throws ");
      et.nextTokenSignificant();
      for (;;) {
        if (et.tokenType!=Tokenizer.TT_WORD) throw new SyntaxIllegalThrowsException();
        result.append(et.getToken());
        et.nextTokenSignificantOrBlock(parseNextBraceBlock);
        if (et.tokenType==(parseNextBraceBlock?ExtendedTokenizer.TT_BLOCK:'{') || et.tokenType==';')
            return result.toString();
        if (et.tokenType!=',' && et.tokenType!='.') throw new SyntaxIllegalThrowsException();
        result.append((char)et.tokenType);
        et.nextTokenSignificant();
      }
    }
    return "";
  }
  public String parseMethodStartComments() throws SyntaxException {
    String startComment= null;
    et.nextTokenNotEol();
    while (et.tokenType==Tokenizer.TT_COMMENT_C || et.tokenType==Tokenizer.TT_COMMENT_CPP) {
      if (startComment==null) startComment= "";
      startComment+= et.getTokenComment();
      et.nextTokenNotEol();
    }
    return startComment;
  }

  public String parseIdentifierWithDots(boolean lastWordCanBeAsterisk) throws SyntaxException {
    if (et.tokenType!=Tokenizer.TT_WORD) return "";
    String identifier= et.getToken();
    et.nextTokenSignificant();
    while (et.tokenType=='.') {
      et.nextTokenSignificant();
      boolean isAsterisk= lastWordCanBeAsterisk && et.tokenType=='*';
      if (!isAsterisk && et.tokenType!=Tokenizer.TT_WORD) throw new SyntaxWordAfterDotExpectedException();
      identifier+= "."+et.getToken();
      et.nextTokenSignificant();
      if (isAsterisk) break;
    }
    return identifier;
  }
  public String parseSquareBrackets() throws SyntaxException {
    String squareBrackets= "";
    while (et.tokenType=='[') {
      et.nextTokenSignificant();
      if (et.tokenType!=']') throw new SyntaxRightSquareBracketExpectedException();
      squareBrackets+= "[]";
      et.nextTokenSignificant();
    }
    return squareBrackets;
  }

}