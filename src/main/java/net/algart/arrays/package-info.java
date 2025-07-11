/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2025 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

/**
 <p><i>AlgART arrays and matrices</i>: generalized arrays and matrices of any Java types
 and basic algorithms of their processing.</p>

 <p>AlgART arrays are classes allowing to store one- or multi-dimensional random access arrays,
 containing elements of any Java type, including primitive types.</p>

 <p>AlgART arrays are <i>homogeneous</i>: the type of elements
 of an array are the same (for primitive elements) or
 are inheritors of the same class (for non-primitive elements).
 AlgART arrays, unlike standard Java arrays, can be <i>resizable</i>:
 you can add elements to the array end or remove some elements at any time.</p>

 <p>AlgART arrays include all basic functionality of the
 {@link java.util.ArrayList standard Java ArrayList class}
 and of {@link java.nio Java NIO buffers},
 but also provide a lot of new features.</p>

 <p>The basic AlgART array interfaces are <b>{@link net.algart.arrays.Array}</b>,
 <b>{@link net.algart.arrays.UpdatableArray}</b>, <b>{@link net.algart.arrays.MutableArray}</b>:
 one-dimensional arrays with any element type.
 There are many subinterfaces with additional functionality and restrictions.</p>

 <p>The basic interface fo representing AlgART multi-dimensional matrix is <b>{@link net.algart.arrays.Matrix}</b>.</p>

 <p>The classes <b>{@link net.algart.arrays.Arrays}</b> and <b>{@link net.algart.arrays.Matrices}</b>
 offer a wide collection of useful functions for processing AlgART arrays and matrices.
 The classes
 <b>{@link net.algart.arrays.JArrays}</b>,
 <b>{@link net.algart.arrays.JBuffers}</b>,
 <b>{@link net.algart.arrays.PackedBitArrays}</b>,
 <b>{@link net.algart.arrays.PackedBitArraysPer8}</b>,
 <b>{@link net.algart.arrays.PackedBitBuffers}</b>
 provide some basic functionality for processing regular Java arrays, buffers, and packed bit arrays.</p>

 <h2>Contents</h2>
 <ul>
 <li><a href="#features">Main features of AlgART arrays</a></li>
 <li><a href="#diagramOfInterfaces">The diagram of interfaces and implementations</a></li>
 <li><a href="#maximalLength">The maximal supported array length</a></li>
 <li><a href="#protectionAgainstUnallowedChanges">5 levels of protection against unallowed changes</a></li>
 <li><a href="#multithreading">Multithreading and synchronization</a></li>
 <li><a href="#exceptions">Runtime exceptions and errors</a></li>
 <li><a href="#systemProperties">System properties used for customizing AlgART arrays</a></li>
 <li><a href="#logging">Built-in logging</a></li>
 <li><a href="#betterByStandard">Tasks that are better solved by standard Java collections</a></li>
 </ul>

 <h2 id="features">Main features of AlgART arrays</h2>

 <ol>
 <li>The addressing of array elements is <b>63-bit</b>. So, it's theoretically possible to
 create and process arrays containing up to <code>2<sup>63</sup>-1</code> (<code>~10<sup>19</sup></code>)
 elements of any primitive or non-primitive types, if OS and hardware can provide necessary amount
 of memory or disk space. Please see also the section
 "<a href=#maximalLength>The maximal supported array length</a>" below.<br>&nbsp;
 </li>

 <li>Multi-dimensional arrays, named <i>matrices</i>, are supported via the
 {@link net.algart.arrays.Matrix} interface. Any one-dimensional
 array can be viewed as a matrix and vice versa.
 <br>&nbsp;
 </li>

 <li>AlgART arrays are implemented with help of special factories, named
 <b>Virtual Memory Models</b> ({@link net.algart.arrays.MemoryModel} interface),
 that provide a standard way of implementing any schemes for storing data,
 from simple Java arrays to mapped disk files.
 The current implementation offers 4 basic memory models:
 <ul>
 <li>the simplest and fastest {@link net.algart.arrays.SimpleMemoryModel Simple memory model},
 that stores data in usual Java arrays;</li>
 <li>{@link net.algart.arrays.BufferMemoryModel Buffer memory model},
 that use Java NIO buffers for storing data:
 it does not provide the maximal access speed, but can provide better overall performance
 in applications that require large amount of RAM, and also provides good compatibility
 with Java code working with channels and NIO buffers;</li>
 <li>advanced {@link net.algart.arrays.LargeMemoryModel Large memory model},
 that can store large amount of primitive elements in mapped-files &mdash;
 it is the only way (in current Java versions) to create very large arrays,
 containing, theoretically, up to <code>2<sup>63</sup>-1</code> elements;</li>
 <li>special {@link net.algart.arrays.CombinedMemoryModel Combined memory model},
 allowing efficient storing non-primitive elements in a set of another arrays
 &mdash; together with <code>LargeMemoryModel</code>, it allows to store
 more than <code>2<sup>31</sup>-1</code> non-primitive elements in one array.</li>
 </ul>
 Moreover, the {@link net.algart.arrays.LargeMemoryModel Large memory model}
 is based on special low-level factories, named <b>Data File Models</b>
 ({@link net.algart.arrays.DataFileModel} interface).
 Creating non-standard data file models allows to easily implement storing
 array elements in almost any possible devices or storages.
 For example, it's possible to create data file model that will represent a content of
 {@link java.awt.image.BufferedImage} as AlgART array.
 <br>&nbsp;
 </li>

 <li>Arrays implement <b>maximal efficient memory usage</b>. In particular, AlgART arrays allow to
 use only 1 bit per element while storing <code>boolean</code> values, 4 bytes per elements
 while storing <code>int</code> or <code>float</code> values, or 3*4=12 bytes per element while
 storing Java objects consisting 3 <code>int</code> field, as for the following <code>Circle</code> object:
 <pre>
 class Circle {
     int x;
     int y;
     int r;
 }
 </pre>
 Unlike this, Java NIO buffers allow efficient storing only non-boolean primitive elements,
 and standard <code>ArrayList</code> always stores every element as a separate instance,
 that require a lot of extra memory for simple structures.<br>&nbsp;
 </li>

 <li>There are <b><a href="#diagramOfInterfaces">separate interfaces for almost all kinds of data access</a></b>
 that makes usage of arrays more simple and stable.
 Namely, there are separate interfaces
 for read-only access ({@link net.algart.arrays.Array}),
 read-write access without changing the array length
 ({@link net.algart.arrays.UpdatableArray}),
 stack access &mdash; adding and removing the last element
 ({@link net.algart.arrays.Stack}),
 and full access including resizing the array
 ({@link net.algart.arrays.MutableArray}).
 In addition, there are {@link net.algart.arrays.DataBuffer} interface,
 providing convenient and maximally efficient block access to AlgART arrays,
 and {@link net.algart.arrays.DirectAccessible} interface
 for quick access to internal Java array if the elements are
 really stored in an underlying Java array.
 There is also full set of interfaces for quick and convenient access to
 elements of all primitive types.<br>&nbsp;<br>

 This architectural solution allows safe programming style,
 when illegal array operations are <i>syntactically</i> impossible.
 For example, the methods, which process an AlgART array argument and calculate some results,
 but don't need any modifications of the passed array,
 declare their array argument as <code>Array</code> or <code><i>Xxx</i>Array</code>,
 where <code><i>Xxx</i></code> is <code>Byte</code>, <code>Int</code>, <code>Float</code>, ...
 &mdash; interfaces containing only reading, but not writing methods.
 The methods, which correct the array content, but don't need to add or remove array elements,
 declare their array argument as <code>UpdatableArray</code> (or <code>Updatable<i>Xxx</i>Array</code>)
 &mdash; interfaces containing only reading and writing, but not resizing methods.
 This solution allows to avoid "optional" operations,
 used for implementation of read-only arrays in standard Java libraries.<br>&nbsp;
 </li>

 <li>The AlgART arrays architecture offers <b><a href="#protectionAgainstUnallowedChanges">advanced
 means for protection against unallowed changes</a></b> of the array content.
 Namely, any array can be {@link net.algart.arrays.Array#mutableClone(MemoryModel) cloned}
 (the simplest protection),
 converted to {@link net.algart.arrays.Array#asImmutable() immutable} form,
 to more efficient (in some cases) {@link net.algart.arrays.Array#asTrustedImmutable()
 trusted immutable} form or
 to special quite safe {@link net.algart.arrays.Array#asCopyOnNextWrite() copy-on-next-write} form.
 Also, any resizable array can be converted to
{@link net.algart.arrays.UpdatableArray#asUnresizable() unresizable} form,
 which length is fixed.
 Using read-only {@link net.algart.arrays.Array} or
 unresizable {@link net.algart.arrays.UpdatableArray} interfaces
 (and their inheritors for primitive types),
 instead of the full {@link net.algart.arrays.MutableArray},
 also helps to avoid unwanted operations.
 </li>
 </ol>



 <h2 id="diagramOfInterfaces">The diagram of interfaces and implementations</h2>

 <p>The basic set of AlgART array interfaces and classes can be represented as 3-dimensional structure.
 The 1st dimension corresponds to the type of elements: there are separate interfaces and classes
 for all 8 primitive Java types and for <code>Object</code> type (and its inheritors).
 The 2nd dimension describes the array functionality:
 read-only access ({@link net.algart.arrays.Array}),
 read/write access ({@link net.algart.arrays.UpdatableArray}),
 full access including resizing ({@link net.algart.arrays.MutableArray}),
 quick access to internal Java array ({@link net.algart.arrays.DirectAccessible}).
 The 3rd dimension is the {@link net.algart.arrays.MemoryModel Virtual Memory Model}:
 the scheme of storing array elements.</p>

 <p>Below is a diagram of basic array interfaces and classes.</p>

 <table border="1">
     <caption><b>{@link net.algart.arrays.SimpleMemoryModel Simple memory model}</b>
 (the only model that supports <i>all</i> {@link net.algart.arrays.Array#elementType() element types})
 </caption>
     <tr>
         <td style="width:20%;padding:4px">Read-only access</td>
         <td style="width:20%;padding:4px">Read/write access</td>
         <td style="width:20%;padding:4px">Stack access (adding/removing the last element)</td>
         <td style="width:20%;padding:4px">Full access</td>
         <td style="width:20%;padding:4px">Access to internal Java array</td>
     </tr>
     <tr>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.Array}</i>,<br>
         <i>{@link net.algart.arrays.BitArray}</i>,<br>
         <i>{@link net.algart.arrays.CharArray}</i>,<br>
         <i>{@link net.algart.arrays.ByteArray}</i>,<br>
         <i>{@link net.algart.arrays.ShortArray}</i>,<br>
         <i>{@link net.algart.arrays.IntArray}</i>,<br>
         <i>{@link net.algart.arrays.LongArray}</i>,<br>
         <i>{@link net.algart.arrays.FloatArray}</i>,<br>
         <i>{@link net.algart.arrays.DoubleArray}</i>,<br>
         <i>{@link net.algart.arrays.ObjectArray}</i>
         </td>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.UpdatableArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableBitArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableCharArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableByteArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableShortArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableIntArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableLongArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableFloatArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableDoubleArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableObjectArray}</i>
         </td>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.Stack}</i>,<br>
         <i>{@link net.algart.arrays.BitStack}</i>,<br>
         <i>{@link net.algart.arrays.CharStack}</i>,<br>
         <i>{@link net.algart.arrays.ByteStack}</i>,<br>
         <i>{@link net.algart.arrays.ShortStack}</i>,<br>
         <i>{@link net.algart.arrays.IntStack}</i>,<br>
         <i>{@link net.algart.arrays.LongStack}</i>,<br>
         <i>{@link net.algart.arrays.FloatStack}</i>,<br>
         <i>{@link net.algart.arrays.DoubleStack}</i>,<br>
         <i>{@link net.algart.arrays.ObjectStack}</i>
         </td>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.MutableArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableBitArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableCharArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableByteArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableShortArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableIntArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableLongArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableFloatArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableDoubleArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableObjectArray}</i>
         </td>
         <td style="vertical-align:top;padding:4px">Interface
         <i>{@link net.algart.arrays.DirectAccessible}</i>:
         implemented by all arrays excepting bit (<code>boolean</code>) ones
         and {@link net.algart.arrays.Array#asImmutable() immutable} instances
         </td>
     </tr>
 </table>
 <p>&nbsp;</p>
 <table border="1">
     <caption><b>{@link net.algart.arrays.BufferMemoryModel Buffer memory model}</b>
 and <b>{@link net.algart.arrays.LargeMemoryModel Large memory model}</b>
 (support all <i>primitive</i> {@link net.algart.arrays.Array#elementType() element types})</caption>
     <tr>
         <td style="width:20%;padding:4px">Read-only access</td>
         <td style="width:20%;padding:4px">Read/write access</td>
         <td style="width:20%;padding:4px">Stack access (adding/removing the last element)</td>
         <td style="width:20%;padding:4px">Full access</td>
         <td style="width:20%;padding:4px">Access to internal Java array</td>
     </tr>
     <tr>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.Array}</i>,<br>
         <i>{@link net.algart.arrays.BitArray}</i>,<br>
         <i>{@link net.algart.arrays.CharArray}</i>,<br>
         <i>{@link net.algart.arrays.ByteArray}</i>,<br>
         <i>{@link net.algart.arrays.ShortArray}</i>,<br>
         <i>{@link net.algart.arrays.IntArray}</i>,<br>
         <i>{@link net.algart.arrays.LongArray}</i>,<br>
         <i>{@link net.algart.arrays.FloatArray}</i>,<br>
         <i>{@link net.algart.arrays.DoubleArray}</i>,<br>
         (but not <i>{@link net.algart.arrays.ObjectArray}</i>)
         </td>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.UpdatableArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableBitArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableCharArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableByteArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableShortArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableIntArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableLongArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableFloatArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableDoubleArray}</i><br>
         (but not <i>{@link net.algart.arrays.UpdatableObjectArray}</i>)
         </td>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.Stack}</i>,<br>
         <i>{@link net.algart.arrays.BitStack}</i>,<br>
         <i>{@link net.algart.arrays.CharStack}</i>,<br>
         <i>{@link net.algart.arrays.ByteStack}</i>,<br>
         <i>{@link net.algart.arrays.ShortStack}</i>,<br>
         <i>{@link net.algart.arrays.IntStack}</i>,<br>
         <i>{@link net.algart.arrays.LongStack}</i>,<br>
         <i>{@link net.algart.arrays.FloatStack}</i>,<br>
         <i>{@link net.algart.arrays.DoubleStack}</i>,<br>
         (but not <i>{@link net.algart.arrays.ObjectStack}</i>)
         </td>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.MutableArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableBitArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableCharArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableByteArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableShortArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableIntArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableLongArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableFloatArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableDoubleArray}</i>,<br>
         (but not <i>{@link net.algart.arrays.MutableObjectArray}</i>)
         </td>
         <td style="vertical-align:top;padding:4px">Interface
         <i>{@link net.algart.arrays.DirectAccessible}</i>:
         is never implemented
         </td>
     </tr>
 </table>
 <table border="1">
     <caption><b>{@link net.algart.arrays.CombinedMemoryModel} memory model</b>
 (supports only <i>non-primitive</i> {@link net.algart.arrays.Array#elementType() element types})</caption>
     <tr>
         <td style="width:20%;padding:4px">Read-only access</td>
         <td style="width:20%;padding:4px">Read/write access</td>
         <td style="width:20%;padding:4px">Stack access (adding/removing the last element)</td>
         <td style="width:20%;padding:4px">Full access</td>
         <td style="width:20%;padding:4px">Access to internal Java array</td>
     </tr>
     <tr>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.Array}</i>,<br>
         <i>{@link net.algart.arrays.ObjectArray}</i>,<br>
         <i>{@link net.algart.arrays.ObjectInPlaceArray}</i> (optional)
         </td>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.UpdatableArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableObjectArray}</i>,<br>
         <i>{@link net.algart.arrays.UpdatableObjectInPlaceArray}</i> (optional)
         </td>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.ObjectStack}</i>
         </td>
         <td style="vertical-align:top;padding:4px">Interfaces:<br>
         <i>{@link net.algart.arrays.MutableArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableObjectArray}</i>,<br>
         <i>{@link net.algart.arrays.MutableObjectInPlaceArray}</i> (optional)
         </td>
         <td style="vertical-align:top;padding:4px">Interface
         <i>{@link net.algart.arrays.DirectAccessible}</i>:
         is never implemented
         </td>
     </tr>
 </table>

 <p>There are special superinterfaces for some groups of primitive types,
 allowing to specify any array with elements from such groups.
 The hierarchy is the following:</p>

 <ul>
 <li><i>{@link net.algart.arrays.Array}</i> subinterfaces:
   <ul>
   <li><i>{@link net.algart.arrays.PArray}</i> subinterfaces:
     <ul>
     <li><i>{@link net.algart.arrays.PFixedArray}</i> subinterfaces:
       <ul>
       <li><i>{@link net.algart.arrays.BitArray}</i></li>
       <li><i>{@link net.algart.arrays.CharArray}</i></li>
       <li><i>{@link net.algart.arrays.PIntegerArray}</i> subinterfaces:
         <ul>
         <li><i>{@link net.algart.arrays.ByteArray}</i></li>
         <li><i>{@link net.algart.arrays.ShortArray}</i></li>
         <li><i>{@link net.algart.arrays.IntArray}</i></li>
         <li><i>{@link net.algart.arrays.LongArray}</i></li>
         </ul>
       </li>
       </ul>
     </li>
     <li><i>{@link net.algart.arrays.PFloatingArray}</i> subinterfaces:
       <ul>
       <li><i>{@link net.algart.arrays.FloatArray}</i></li>
       <li><i>{@link net.algart.arrays.DoubleArray}</i></li>
       </ul>
     </li>
     </ul>
   </li>
   <li><i>{@link net.algart.arrays.ObjectArray}</i></li>
   </ul>
 </li>
 </ul>

 <p>Also, all subinterfaces of <i>{@link net.algart.arrays.PFixedArray}</i> and
 <i>{@link net.algart.arrays.PFloatingArray}</i> are grouped into the common interface
 <i>{@link net.algart.arrays.PNumberArray}</i> (any primitive types excepting <code>boolean</code>
 and <code>char</code>, alike <code>java.lang.Number</code> class).</p>

 <p>There are the same hierarchies for updatable and mutable arrays, but not for stacks.</p>

 <h2 id="maximalLength">The maximal supported array length</h2>

 <p>The maximal possible length of AlgART array depends on the
 {@link net.algart.arrays.MemoryModel memory model}, which has created this array.
 An attempt to create an array with length exceeding the limit, specified by the memory model,
 or an attempt to {@link net.algart.arrays.MutableArray#length(long) increase the length}
 or {@link net.algart.arrays.MutableArray#ensureCapacity(long) capacity}
 of an existing array over this limit, leads to
 {@link net.algart.arrays.TooLargeArrayException} (instead of the usual <code>OutOfMemoryError</code>).</p>

 <p>The maximal array lengths for different memory models are listed below.</p>

 <table border="1" style="width:60%">
     <caption><b>{@link net.algart.arrays.SimpleMemoryModel Simple memory model}</b></caption>
     <tr>
         <td style="padding:4px" colspan="3">
         The maximal array length is defined by the language limitations for arrays.
         So, it cannot exceed <code>2<sup>31</sup>-1</code> &mdash; the maximal possible length of Java arrays,
         excepting bit arrays, that can contain up to <code>2<sup>37</sup>-1</code>
         because they are packed into <code>long[]</code>.
         However, real Java Machines usually limit the maximal length of arrays by <code>2<sup>31</sup>-1</code>
         <i>bytes</i> (though the language theoretically allows to defined an array with <code>2<sup>31</sup>-1</code>
         <i>elements</i>). It reduces the maximal possible length of AlgART arrays.</td>
     </tr>
     <tr>
         <td style="width:34%;padding:4px">The type of elements</td>
         <td style="width:33%;padding:4px">Theoretical limit for array length</td>
         <td style="width:34%;padding:4px">Usual real limit for array length</td>
     </tr>
     <tr>
         <td style="vertical-align:top;padding:4px">
         <code>boolean</code> (<i>{@link net.algart.arrays.BitArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>char</code> (<i>{@link net.algart.arrays.CharArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>byte</code> (<i>{@link net.algart.arrays.ByteArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>short</code> (<i>{@link net.algart.arrays.ShortArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>int</code> (<i>{@link net.algart.arrays.IntArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>long</code> (<i>{@link net.algart.arrays.LongArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>float</code> (<i>{@link net.algart.arrays.FloatArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>double</code> (<i>{@link net.algart.arrays.DoubleArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>Object</code> (<i>{@link net.algart.arrays.ObjectArray}</i>)
         </td>
         <td style="vertical-align:top;padding:4px"><code>2<sup>37</sup>-1</code><br>
         <code>2<sup>31</sup>-1</code><br>
         <code>2<sup>31</sup>-1</code><br>
         <code>2<sup>31</sup>-1</code><br>
         <code>2<sup>31</sup>-1</code><br>
         <code>2<sup>31</sup>-1</code><br>
         <code>2<sup>31</sup>-1</code><br>
         <code>2<sup>31</sup>-1</code><br>
         <code>2<sup>31</sup>-1</code>
         </td>
         <td style="vertical-align:top;padding:4px"><code>2<sup>34</sup>-1</code><br>
         <code>2<sup>30</sup>-1</code><br>
         <code>2<sup>31</sup>-1</code><br>
         <code>2<sup>30</sup>-1</code><br>
         <code>2<sup>29</sup>-1</code><br>
         <code>2<sup>28</sup>-1</code><br>
         <code>2<sup>29</sup>-1</code><br>
         <code>2<sup>28</sup>-1</code><br>
         depends on JVM implementation and the size of objects
         </td>
     </tr>
 </table>
 <div style="margin:0 0 20px">
 The real limits are less in 32-bit JVM, that usually cannot utilize 2 GB of memory.
 </div>

 <table border="1" style="width:60%">
     <caption><b>{@link net.algart.arrays.BufferMemoryModel Buffer memory model}</b></caption>
     <tr>
         <td style="padding:4px" colspan="2">
         The maximal array length is defined by the Java API limitations for <code>ByteBuffer</code> class.
         This API use <code>int</code> type for the buffer length
         and allows creating direct NIO buffers only as views of <code>ByteBuffer</code>.
         So, the limit is <code>2<sup>31</sup>-1</code> <i>bytes</i>.</td>
     </tr>
     <tr>
         <td style="width:50%;padding:4px">The type of elements</td>
         <td style="width:50%;padding:4px">The limit for array length</td>
     </tr>
     <tr>
         <td style="vertical-align:top;padding:4px">
         <code>boolean</code> (<i>{@link net.algart.arrays.BitArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>char</code> (<i>{@link net.algart.arrays.CharArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>byte</code> (<i>{@link net.algart.arrays.ByteArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>short</code> (<i>{@link net.algart.arrays.ShortArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>int</code> (<i>{@link net.algart.arrays.IntArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>long</code> (<i>{@link net.algart.arrays.LongArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>float</code> (<i>{@link net.algart.arrays.FloatArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>double</code> (<i>{@link net.algart.arrays.DoubleArray}</i>)<code><sup>&nbsp;</sup></code>
         </td>
         <td style="vertical-align:top;padding:4px"><code>2<sup>34</sup>-1</code><br>
         <code>2<sup>30</sup>-1</code><br>
         <code>2<sup>31</sup>-1</code><br>
         <code>2<sup>30</sup>-1</code><br>
         <code>2<sup>29</sup>-1</code><br>
         <code>2<sup>28</sup>-1</code><br>
         <code>2<sup>29</sup>-1</code><br>
         <code>2<sup>28</sup>-1</code>
         </td>
     </tr>
 </table>
 <div style="margin:0 0 20px">
The real limits are less in 32-bit JVM, that usually cannot utilize 2 GB of memory.
 </div>

 <table border="1" style="width:60%">
     <caption><b>{@link net.algart.arrays.LargeMemoryModel Large memory model}</b></caption>
     <tr>
         <td style="padding:4px" colspan="2">
         The maximal array length is limited by <code>2<sup>63</sup>-1</code> <i>bytes</i> (the maximal
         supported file length in Java API and most OS), but also, of course,
         cannot exceed the common limit <code>2<sup>63</sup>-1</code> <i>elements</i>
         (that is more strict limitation for bit arrays).</td>
     </tr>
     <tr>
         <td style="width:50%;padding:4px">The type of elements</td>
         <td style="width:50%;padding:4px">The limit for array length</td>
     </tr>
     <tr>
         <td style="vertical-align:top;padding:4px">
         <code>boolean</code> (<i>{@link net.algart.arrays.BitArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>char</code> (<i>{@link net.algart.arrays.CharArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>byte</code> (<i>{@link net.algart.arrays.ByteArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>short</code> (<i>{@link net.algart.arrays.ShortArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>int</code> (<i>{@link net.algart.arrays.IntArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>long</code> (<i>{@link net.algart.arrays.LongArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>float</code> (<i>{@link net.algart.arrays.FloatArray}</i>)<code><sup>&nbsp;</sup></code><br>
         <code>double</code> (<i>{@link net.algart.arrays.DoubleArray}</i>)<code><sup>&nbsp;</sup></code>
         </td>
         <td style="vertical-align:top;padding:4px"><code>2<sup>63</sup>-1</code><br>
         <code>2<sup>62</sup>-1</code><br>
         <code>2<sup>63</sup>-1</code><br>
         <code>2<sup>62</sup>-1</code><br>
         <code>2<sup>61</sup>-1</code><br>
         <code>2<sup>60</sup>-1</code><br>
         <code>2<sup>61</sup>-1</code><br>
         <code>2<sup>60</sup>-1</code>
         </td>
     </tr>
 </table>
<div style="margin:0 0 20px">
 In other words, the limits are so large that the real maximal array length
 depends only on the available disk space.
 </div>

 <table border="1" style="width:60%">
     <caption><b>{@link net.algart.arrays.CombinedMemoryModel Combined memory model}</b></caption>
     <tr>
         <td style="padding:4px" colspan="1">
         The maximal array length depends on the corresponding limit for a memory model,
         that is used by the {@link net.algart.arrays.CombinedMemoryModel.Combiner combiner}
         which defines an algorithm of storing objects.
         For example, if the storage is based on {@link net.algart.arrays.LargeMemoryModel Large memory model},
         the maximal array length usually depends only on the available disk space.</td>
     </tr>
 </table>



 <h2 id="protectionAgainstUnallowedChanges">5 levels of protection against unallowed changes</h2>

 <p>There is an often and important problem to protect some application data
 against unallowed changes, to avoid hard bugs connected with unexpected damage of application
 objects. Below is a typical code illustrating this problem:</p>

 <pre>
 DataClass a = ...; // some important data
 someResults = someObject.process(a); // some method that need read-only access to the argument
 </pre>

 <p>Here <code>a</code> is some "important" data, that must stay immutable while the following call
 of <code>process</code> method. Maybe, for example, some parallel threads are reading this object now.
 And <code>someObject.process</code> is a method, which, theoretically, should not correct the passed data:
 it only analyzes it and creates some result.
 But it's very possible, that we are not sure, that this method really fulfils it.
 Maybe, its implementation is downloaded from Internet (and can be written by a hacker
 to damage our application). Or this method performs very complex tasks, and we cannot be sure
 that it doesn't contain bugs.</p>

 <p>Java arrays offer only one way to protect data against changes and to solve this issue:
 cloning an array. An example:</p>

 <pre>
 int[] a = ...; // some important array
 int[] protected = (int[])a.clone();
 someResults = someObject.process(protected);
 </pre>

 <p>Here the <code>process</code> method can corrupt the passed argument, but the original
 array will stay unchanged.</p>

 <p>Standard Java collections, as well as buffers from <code>java.nio</code> package, offer
 an additional method: making <i>an immutable view</i>. For example:</p>

 <pre>
 List a = ...; // some important list
 List protected = Collections.unmodifiableList(a);
 someResults = someObject.process(protected);
 </pre>

 <p>Now, if the <code>process</code> method will try to correct the passed argument, an exception
 will be thrown. This solution has an advantage: no additional memory is required for storing
 a clone.</p>

 <p>AlgART array architecture supports <b>5 ways</b> for solving this task, including 2 ways
 described above. We shall compare all them below.</p>

 <h3>1. Syntactical protection</h3>

 <p>The 1st solution is the simplest, fastest, but not safe enough. It is a <i>syntactical</i> solution.
 If the <code>process</code> method does not need to modify its argument, it should be declared with
 <code>Array</code> argument type, which doesn't contain any mutation methods at all:</p>

 <pre>
 public ResultType process(Array a);
 </pre>

 <p>A usage example:</p>

 <pre>
 Array a = ...; // maybe, there is MutableArray in this expression
 someResults = someObject.process(a);
 </pre>

 <p>Of course, it is not a problem to write a "malicious" <code>process</code> method which will
 correct its argument by operators alike the following: <code>((MutableArray)a).set(0, ...)</code>.
 However, if <code>process</code> method is written by you or by your colleagues,
 and you only need to protect against possible bugs, the syntactical protection will help you.</p>

 <h3>2. Cloning</h3>

 <p>It is a very simple and absolutely safe solution:</p>

 <pre>
 Array a = ...;
 Array protected = a.{@link net.algart.arrays.Array#updatableClone(MemoryModel)
 updatableClone}({@link net.algart.arrays.Arrays#SMM}); // or a.{@link
 net.algart.arrays.Array#mutableClone(MemoryModel)
 mutableClone}(...)
 someResults = someObject.process(protected);
 </pre>

 <p>Unfortunately, such a solution requires additional memory and time, even if the <code>process</code>
 method, really, does not try do modify its argument.</p>

 <h3>3. Immutable view</h3>

 <p>It is a traditional, also simple and absolutely safe method,
 used by standard Java collections and NIO buffers:</p>

 <pre>
 Array a = ...;
 Array protected = a.{@link net.algart.arrays.Array#asImmutable() asImmutable()};
 someResults = someObject.process(protected);
 </pre>

 <p>The difference from the analogous technique, implemented in standard Java libraries,
 is absence of "optional" operations.  In a case of Java collections, <code>process</code>
 method will have an ability to call a mutation method
 for the passed array, but this method will throw an exception.
 Unlike this, {@link net.algart.arrays.Array#asImmutable()} method returns
 an object that does not implement  any interfaces and does not contain
 any methods, which allow to change data anyhow.</p>

 <p>The main defect of this solution is disabling any possible optimization, based on direct access
 to stored data. For example, {@link net.algart.arrays.DirectAccessible} interface can allow access to
 the Java array, internally used for storing elements. If <code>process</code> method needs a lot
 of accesses to elements in random order, then using {@link net.algart.arrays.DirectAccessible} interface
 in a separate algorithm branch can optimize the method in times, in a case when the AlgART array is really
 based on Java arrays. Unfortunately, an immutable view has no right to provide such direct access
 to internal storage, because it is a possible way to corrupt the array.</p>

 <h3>4. Trusted immutable view</h3>

 <p>It is a compromise between absolute safety, provided by cloning and immutable views,
 and maximal efficiency, achieved while using syntactical protection only. An example of usage:</p>

 <pre>
 Array a = ...;
 Array protected = a.{@link net.algart.arrays.Array#asTrustedImmutable() asTrustedImmutable()};
 try {
     someResults = someObject.process(protected);
 } finally {
     protected.{@link net.algart.arrays.Array#checkUnallowedMutation() checkUnallowedMutation()};
 }
 </pre>

 <p>Unlike usual immutable view, a trusted immutable view <i>may</i> implement some interfaces,
 that allow to change the array content &mdash; only if it is really necessary for
 optimization. (The only example of such interface in this package is {@link net.algart.arrays.DirectAccessible}.)
 So, the <code>process</code> method <i>can</i> corrupt the original array <code>a</code>.
 However, any changes in the original array will be detected by the following call
 "<code>protected.checkUnallowedMutation()</code>" with almost 100% probability,
 and if the array was changed, {@link net.algart.arrays.UnallowedMutationError} will be thrown.
 To detect changes, <code>checkUnallowedMutation</code> usually calculates a hash code
 and compares it with the hash code calculated in <code>asTrustedImmutable</code> method.</p>

 <p>This technique is a suitable solution if you trust the authors of <code>process</code> method
 (for example, it is written by you or your colleagues), but this method is not trivial
 and you are not sure that all possible bugs in this method are already fixed.
 Unlike all other 4 protection methods, it is the only way to automatically detect such bugs:
 so, trusted immutable views can be very useful in a stage of testing application.</p>

 <p>This solution have the following defects.</p>

 <ul>
 <li>It requires little additional time (for calculation of hash code) while calling
 {@link net.algart.arrays.Array#asTrustedImmutable() asTrustedImmutable()} and
 {@link net.algart.arrays.Array#checkUnallowedMutation() checkUnallowedMutation()} methods.
 (However, as well as immutable views, this method does not require any additional memory.)</li>

 <li>If there are another threads working with the original array,
 then the unallowed changes of elements can lead to errors in that threads before
 detecting these changes and throwing {@link net.algart.arrays.UnallowedMutationError}.</li>

 <li>This method protects against algorithmic bugs, but not against a malicious method,
 specially written by a hacker. If the author of the method have read the source code of this package,
 it <i>can</i> perform changes of the array, which will not be detected by this technique.</li>
 </ul>

 <h3>5. Copy-on-next-write view</h3>

 <p>It is a more efficient alternative to cloning an array. This solution is also absolutely
 safe, but, sometimes, it requires additional memory and time. An example:</p>

 <pre>
 Array a = ...;
 Array protected = a.{@link net.algart.arrays.Array#asCopyOnNextWrite() asCopyOnNextWrite()};
 someResults = someObject.process(protected);
 </pre>

 <p>Now <code>process</code> method may freely change the passed argument (if it implements necessary
 interfaces). However, the first (not further!) attempt to modify the passed <code>protected</code> array,
 or any other access that can lead to its modification (like {@link net.algart.arrays.DirectAccessible#javaArray()}
 method), will lead to reallocation of the underlying storage, used for array elements,
 before performing the operation. It means that modification will not affect
 the original <code>a</code> array, but only <code>protected</code> array.</p>

 <p>This solution is the best choice, if you need a strict guarantee that the original array
 will not be modified (that is not provided by trusted immutable views), and you don't need
 a guarantee that no additional memory will be occupied. If the <code>process</code> method
 will not try to modify the array or use optimization interfaces alike {@link net.algart.arrays.DirectAccessible},
 then this solution will provide the maximal efficiency.
 If the method will try to get direct access for optimization via {@link net.algart.arrays.DirectAccessible}
 interface, then the internal data will be cloned at this moment, that can require additional memory and time,
 but all further accesses to elements will work with maximal efficiency.</p>

 <p>This solution have the following defects.</p>

 <ul>
 <li>It is not better than simple cloning, if the processing method always use the direct
 access to the storage for optimization goals (for example, via {@link net.algart.arrays.DirectAccessible}
 interface), and the passed array really allows this optimization (for example, is based on Java arrays).
 Good processing methods can use {@link net.algart.arrays.Array#isCopyOnNextWrite()} method
 to choose the best behavior.</li>

 <li>It does not help to detect bugs that lead to unallowed changes of array elements. But you
 can use this technique together with trusted immutable view:
 "<code>Array protected = a.{@link net.algart.arrays.Array#asCopyOnNextWrite()
 asCopyOnNextWrite()}.{@link net.algart.arrays.Array#asTrustedImmutable()
 asTrustedImmutable()}</code>" Such protection is also absolutely safe,
 but also allows to catch unallowed attempts of correction by
 {@link net.algart.arrays.Array#checkUnallowedMutation() checkUnallowedMutation()} method.</li>
 </ul>


 <h2 id="multithreading">Multithreading and synchronization</h2>

 <p>{@link net.algart.arrays.Array#asImmutable() Immutable} AlgART arrays are absolutely <b>thread-safe</b>
 and can be used simultaneously in several threads.
 Moreover, even if an AlgART array is mutable (for example, implements {@link net.algart.arrays.MutableArray}),
 but all threads, accessing it, only read data from it and do not attempt to modify the array by any way,
 then this array is also <b>thread-safe</b> and no synchronization is required.
 (The same rules are correct for usual Java arrays.)</p>

 <p>If there are 2 or more threads accessing an AlgART array, and at least one from them modifies it
 (for example, changes elements or the length), then you should synchronize access to the array.
 Without external synchronization, the resulting data in the array will be unspecified.
 However, if you do not use any methods from {@link net.algart.arrays.MutableArray} /
 {@link net.algart.arrays.Stack} interfaces and their inheritors,
 but only read and write elements via methods provided by {@link net.algart.arrays.UpdatableArray} interface
 (and its versions for concrete element types),
 then the behavior while simultaneous multithreading access <b>will be the same as for usual Java arrays</b>.
 In particular, access to one element will never affect another elements.
 So, you can correctly simultaneously work with several non-overlapping sets of elements of the same array
 from several threads without synchronization, if different threads work with different sets.
 (Please compare: the standard <code>java.util.BitSet</code> class does not provide such guarantees.)</p>


 <h2 id="exceptions">Runtime exceptions and errors</h2>

 <p>The methods of classes implementing AlgART arrays, usually, can throw exceptions declared in
 the Javadoc comments to methods. In addition, there are following exception, that can be thrown by methods
 and are not always specified in comments.</p>

 <ul>
 <li><b><code>java.io.IOError</code></b> can be thrown at any moment by any method processing an AlgART array,
 as well as <code>OutOfMemoryError</code> can be thrown my almost any Java method.
 <code>java.io.IOError</code> is usually thrown when an array is based on some external file,
 as in the {@link net.algart.arrays.LargeMemoryModel Large memory model},
 and there is some problem with access to this file, for example, not enough disk space.
 <br>&nbsp;
 <br>
 One of the typical situations leading to this error is unexpected program termination,
 when some threads work with arrays created by {@link net.algart.arrays.LargeMemoryModel Large memory model}.
 In this case, the built-in shutdown hook is started to remove all temporarily allocated disk files,
 and since it is started, almost <i>any</i> access to an AlgART array, based on a temporary file,
 lead to <code>IOError</code> with <code>IllegalStateException</code> as a cause.
 <br>&nbsp;
 <br>
 Warning: <code>java.io.IOError</code> can be also thrown while processing an AlgART array,
 based on some external file, as in the {@link net.algart.arrays.LargeMemoryModel Large memory model},
 if the current thread is <i>interrupted</i> by <code>Thread.interrupt()</code> method.
 Also this error is thrown if the <code>Thread.interrupt()</code> method is called for a thread,
 that is currently performing multithreading copying by {@link net.algart.arrays.Arrays#copy copy} method.
 Usually, such behavior is not suitable. So, <b>you should not try to interrupt the
 threads, processing AlgART arrays, via <code>Thread.interrupt()</code> technique!</b>
 Please use an alternative technique: some volatile flag, required interruption,
 or <code>net.algart.contexts.InterruptionContext</code> interface.
 For interruption of {@link net.algart.arrays.Arrays#copy Arrays.copy} method,
 you can also use the custom implementation of {@link net.algart.arrays.ArrayContext}.
 <br>&nbsp;</li>

 <li><b>{@link net.algart.arrays.TooLargeArrayException TooLargeArrayException}</b> can be thrown by methods,
 which allocate memory in form of AlgART arrays, if the size of an allocated array is too large:
 see {@link net.algart.arrays.TooLargeArrayException comments to this exception}. In other words,
 this exception is possible in the same situations as the standard <code>OutOfMemoryError</code>,
 but its probability is very low: usually an attempt to create too large AlgART array leads to
 another exceptions, like <code>OutOfMemoryError</code> or <code>java.io.IOError</code> with "disk full" message.
 <br>&nbsp;</li>

 <li>Any <b><code>RuntimeException</code></b> can be thrown by methods, using
 {@link net.algart.arrays.ArrayContext ArrayContext} technique to allow interruption by user
 (for example, by {@link net.algart.arrays.Arrays#copy Arrays.copy} method),
 if the context, passed to this method, throws this exception in its
 {@link net.algart.arrays.ArrayContext#checkInterruption() checkInterruption()} method.
 It is the recommended way of interrupting long-working methods, processing AlgART arrays.
 <br>&nbsp;</li>

 <li><code>java.io.IOError</code> was added since JDK 1.6 only. Under Java 1.5, the similar
 <b><code>net.algart.arrays.IOErrorJ5</code></b> exception is thrown instead.
 This error is package-private and may be excluded in future versions of AlgART libraries.
 <br>&nbsp;</li>

 <li><b><code>AssertionError</code></b> can be thrown at any time if some bug will be auto-detected in AlgART libraries.
 Some internal checks (that can lead to this error in a case of the bug) are skipped
 when Java is started without <code>-ea</code> flag.
 The most serious bugs, if they will be auto-detected, lead to <b><code>InternalError</code></b>
 instead of <code>AssertionError</code>.
 We hope that these errors will not be thrown inside this package in your applications.
 <br>&nbsp;</li>
 </ul>


 <h2 id="systemProperties">System properties used for customizing AlgART arrays</h2>

 <p>Behavior of AlgART arrays depends on some system properties, that allow to customize many important aspects.
 Below is the list of these properties.</p>

 <dl>
 <dt>"<b><code>net.algart.arrays.maxAvailableProcessors</code></b>"</dt>
 <dd>Defines the maximal number of processor units, that are permitted to be used simultaneously
 by AlgART libraries for any goals. Namely, AlgART libraries never directly use
 the system value, returned by <code>Runtime.getRuntime().availableProcessors()</code> call,
 but use a minimum from that value and the value, specified in this property.
 The default value depends on JVM: on 64-bit JVM it is 256, on 32-bit it is only 8.
 If it is not suitable, please specify another value (from 1 to 1024).
 See {@link net.algart.arrays.Arrays.SystemSettings#availableProcessors()}
 for more details.</dd>

 <dt>"<b><code>net.algart.arrays.CPUCount</code></b>"</dt>
 <dd>Defines the number of processor units that should be used for multithreading optimization.
 If not exists, or zero, or negative, then
 {@link net.algart.arrays.Arrays.SystemSettings#availableProcessors()} will be used instead.
 See {@link net.algart.arrays.Arrays.SystemSettings#cpuCountProperty()}
 for more details.</dd>

 <dt>"<b><code>net.algart.arrays.globalMemoryModel</code></b>"</dt>
 <dd>Defines the default
 {@link net.algart.arrays.MemoryModel memory model},
 that can be used in your classes which need AlgART arrays.
 The {@link net.algart.arrays.SimpleMemoryModel simple memory model} is used by default.
 See {@link net.algart.arrays.Arrays.SystemSettings#globalMemoryModel()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.maxTempJavaMemory</code></b>"</dt>
 <dd>Defines the maximal amount of usual Java memory, in bytes, which can be freely used
 by methods, processing AlgART arrays, for internal needs and for creating results.
 May contain any non-negative <code>long</code> value.
 Default value is <code>33554432</code> (32 MB).
 See {@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.maxTempJavaMemoryForTiling</code></b>"</dt>
 <dd>Defines the maximal amount of usual Java memory, in bytes, which can be used
 by methods, performing conversion AlgART matrices into the
 {@link net.algart.arrays.Matrix#tile(long...) tiled form}
 and inversely from the tiled form.
 May contain any non-negative <code>long</code> value.
 Default value is
 <code>max(134217728,{@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()})</code>
 (134217728=128&nbsp;MB).
 See {@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemoryForTiling()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.maxMultithreadingMemory</code></b>"</dt>
 <dd>Defines the maximal size of memory block, in bytes, that should be processed in several threads
 for optimization on multiprocessor or multicore computers.
 May contain any positive <code>long</code> value.
 Default value is <code>1048576)</code> (1 MB).
 See {@link net.algart.arrays.Arrays.SystemSettings#maxMultithreadingMemory()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.maxMappedMemory</code></b>"</dt>
 <dd>Defines the maximal amount of system memory, in bytes, allowed for simultaneous mapping
 by {@link net.algart.arrays.DefaultDataFileModel} class.
 May contain any non-negative <code>long</code> value.
 Default value is <code>536870912</code> (512 MB).
 See {@link net.algart.arrays.DefaultDataFileModel#maxMappedMemory()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.globalThreadPoolSize</code></b>"</dt>
 <dd>Defines the number of threads in the global system thread pool that will be used for multithreading optimization.
 If zero or negative, then the thread pools will be created on demand.
 If not exists, the global thread pool with
 <code>{@link net.algart.arrays.Arrays.SystemSettings#availableProcessors()}*MULT+1</code>
 threads (default value) will be used, where MULT is an integer value of
 "<code>net.algart.arrays.globalThreadPoolsPerCPU</code>"
 system property. See {@link net.algart.arrays.DefaultThreadPoolFactory#globalThreadPool()}
 for more details.</dd>

 <dt>"<b><code>net.algart.arrays.globalThreadPoolsPerCPU</code></b>"</dt>
 <dd>Helps to define the number of threads in the global system thread pool
 if "<code>net.algart.arrays.globalThreadPoolSize</code>" system property does not exist: see above.</dd>

 <dt>"<b><code>net.algart.arrays.globalDiskSynchronizer</code></b>"</dt>
 <dd>Defines the default
 {@link net.algart.arrays.Arrays.SystemSettings.DiskSynchronizer disk synchronizer},
 that will be used for synchronization of all disk operations, performed by this package.
 See {@link net.algart.arrays.Arrays.SystemSettings#globalDiskSynchronizer()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.profiling</code></b>"</dt>
 <dd>Defines whether the algorithms, processing AlgART arrays, should write to logs some timing information.
 May be "<code>false</code>" or "<code>true</code>". Default value is identical to "-ea" JVM flag:
 if java was called with "-ea" flag (assertions are enabled), the default profiling mode is <code>true</code>,
 in another case it is <code>false</code>.
 See {@link net.algart.arrays.Arrays.SystemSettings#profilingMode()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.LargeMemoryModel.dataFileModel</code></b>"</dt>
 <dd>Defines the default {@link net.algart.arrays.DataFileModel data file model},
 used by the {@link net.algart.arrays.LargeMemoryModel Large memory model}.
 {@link net.algart.arrays.DefaultDataFileModel DefaultDataFileModel} is used by default.
 See {@link net.algart.arrays.LargeMemoryModel#getInstance()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.DefaultDataFileModel.numberOfBanksPerCPU</code></b>"</dt>
 <dd>Defines the number of banks per each existing CPU or CPU kernel,
 used by the {@link net.algart.arrays.DefaultDataFileModel default data file model}.
 Default value is <code>3</code>.
 See {@link net.algart.arrays.DefaultDataFileModel#recommendedNumberOfBanks()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.DefaultDataFileModel.bankSize</code></b>"</dt>
 <dd>Defines the size of banks, used by the {@link net.algart.arrays.DefaultDataFileModel default data file model}
 for unresizable arrays (on 64-bit Java machines).
 Default value is <code>16777216</code> (16 MB).
 See {@link net.algart.arrays.DefaultDataFileModel#recommendedBankSize(boolean)} with
 <code>true</code> argument for more details.</dd>

 <dt>"<b><code>net.algart.arrays.DefaultDataFileModel.resizableBankSize</code></b>"</dt>
 <dd>Defines the size of banks, used by the {@link net.algart.arrays.DefaultDataFileModel default data file model}
 for resizable arrays (on 64-bit Java machines).
 Default value is <code>4194304</code> (4 MB).
 See {@link net.algart.arrays.DefaultDataFileModel#recommendedBankSize(boolean)} with
 <code>false</code> argument for more details.</dd>

 <dt>"<b><code>net.algart.arrays.DefaultDataFileModel.singleMappingLimit</code></b>"</dt>
 <dd>Defines the limit for file size, so that less unresizable files are mapped only once
 in the {@link net.algart.arrays.DefaultDataFileModel default data file model}
 (on 64-bit Java machines).
 Default value is <code>268435456</code> (256 MB).
 See {@link net.algart.arrays.DefaultDataFileModel#recommendedSingleMappingLimit()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.DefaultDataFileModel.bankSize32</code></b>"</dt>
 <dd>Defines the size of banks, used by the {@link net.algart.arrays.DefaultDataFileModel default data file model}
 for unresizable arrays (on 32-bit Java machines).
 Default value is <code>4194304</code> (4 MB).
 See {@link net.algart.arrays.DefaultDataFileModel#recommendedBankSize(boolean)} with
 <code>true</code> argument for more details.</dd>

 <dt>"<b><code>net.algart.arrays.DefaultDataFileModel.resizableBankSize32</code></b>"</dt>
 <dd>Defines the size of banks, used by the {@link net.algart.arrays.DefaultDataFileModel default data file model}
 for resizable arrays (on 32-bit Java machines).
 Default value is <code>2097152</code> (2 MB).
 See {@link net.algart.arrays.DefaultDataFileModel#recommendedBankSize(boolean)} with
 <code>false</code> argument for more details.</dd>

 <dt>"<b><code>net.algart.arrays.DefaultDataFileModel.singleMappingLimit32</code></b>"</dt>
 <dd>Defines the limit for file size, so that less unresizable files are mapped only once
 in the {@link net.algart.arrays.DefaultDataFileModel default data file model}
 (on 32-bit Java machines).
 Default value is <code>4194304</code> (4 MB).
 See {@link net.algart.arrays.DefaultDataFileModel#recommendedSingleMappingLimit()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.DefaultDataFileModel.autoResizingOnMapping</code></b>"</dt>
 <dd>Defines whether AlgART mapping manager in the
 {@link net.algart.arrays.DefaultDataFileModel default data file model}
 should increase the file size via standard I/O API, or it is increased automatically
 as a result of new mappings.
 Default value is <code>false</code>.
 See {@link net.algart.arrays.DefaultDataFileModel#autoResizingOnMapping()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.DefaultDataFileModel.lazyWriting</code></b>"</dt>
 <dd>Defines whether the {@link net.algart.arrays.DefaultDataFileModel default data file model}
 will use lazy-writing mode by default.
 Default value is <code>true</code> in Java 1.7 or higher Java version and <code>false</code> in Java 1.5 and Java 1.6.
 See {@link net.algart.arrays.DefaultDataFileModel#defaultLazyWriting()} method and
 {@link net.algart.arrays.DefaultDataFileModel#DefaultDataFileModel(java.io.File, long, boolean)}
 constructor for more details.</dd>

 <dt>"<b><code>net.algart.arrays.StandardIODataFileModel.numberOfBanks</code></b>"</dt>
 <dd>Defines the number of banks, used by
 the {@link net.algart.arrays.StandardIODataFileModel alternative data file model}.
 Default value is <code>32</code>.
 See {@link net.algart.arrays.StandardIODataFileModel#recommendedNumberOfBanks()} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.StandardIODataFileModel.bankSize</code></b>"</dt>
 <dd>Defines the size of banks, used by {@link net.algart.arrays.StandardIODataFileModel alternative data file model}
 for both resizable and unresizable arrays.
 Default value is <code>65536</code> (64 KB).
 See {@link net.algart.arrays.StandardIODataFileModel#recommendedBankSize(boolean)} for more details.</dd>

 <dt>"<b><code>net.algart.arrays.StandardIODataFileModel.directBuffers</code></b>"</dt>
 <dd>Defines whether the {@link net.algart.arrays.StandardIODataFileModel alternative data file model}
 will use direct byte buffers by default.
 Default value is <code>true</code>.
 See {@link net.algart.arrays.StandardIODataFileModel#defaultDirectBuffers()} method and
 {@link net.algart.arrays.StandardIODataFileModel#StandardIODataFileModel(java.io.File, long, boolean, boolean)}
 constructor for more details.</dd>
 </dl>

 <p>All these properties, excepting "<code>net.algart.arrays.CPUCount</code>",
 are loaded while initialization of the corresponding classes.
 So, any changes of them will be applied only at the next start of the Java application.</p>

 <p>Note: all properties containing integer values, excepting
 "{@link net.algart.arrays.Arrays.SystemSettings#cpuCountProperty()
 net.algart.arrays.CPUCount}",
 can contain a suffix <code>K</code>, <code>M</code>, <code>G</code>, <code>T</code>
 (or <code>k</code>, <code>m</code>, <code>g</code>, <code>t</code>),
 that means that the integer value, specified before this suffix, is multiplied by
 1024 (2<sup>10</sup>, "Kilo"), 1048576 (2<sup>20</sup>, "Mega"), 1073741824 (2<sup>30</sup>, "Giga")
 or 1099511627776 (2<sup>40</sup>, "Tera") correspondingly.
 For example, you can specify <code>-Dnet.algart.arrays.DefaultDataFileModel.bankSize=64m</code>
 to set the bank size to 64 MB.</p>

 <p>Note: the properties
 "{@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()
 net.algart.arrays.maxTempJavaMemory}",
 "{@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemoryForTiling()
 net.algart.arrays.maxTempJavaMemoryForTiling}",
 "{@link net.algart.arrays.Arrays.SystemSettings#maxMultithreadingMemory()
 net.algart.arrays.maxMultithreadingMemory}",
 "{@link net.algart.arrays.DefaultDataFileModel#maxMappedMemory()
 net.algart.arrays.maxMappedMemory}" are limited by the value 2<sup>56</sup>~7.2*10<sup>16</sup>:
 if one of these properties exceeds this value, this limit is used instead.
 It guarantees that using the values of these properties will never lead to integer overflows.</p>

 <p>The most important properties, that usually should be customized, are
 "{@link net.algart.arrays.Arrays.SystemSettings#MAX_AVAILABLE_PROCESSORS
 net.algart.arrays.maxAvailableProcessors}" (in applications that prefer not to use all available processors),
 "{@link net.algart.arrays.Arrays.SystemSettings#maxTempJavaMemory()
 net.algart.arrays.maxTempJavaMemory}" and
 "{@link net.algart.arrays.DefaultDataFileModel#maxMappedMemory()
 net.algart.arrays.maxMappedMemory}".
 Other properties can be not customized in most cases.</p>

 <p>There are some other system properties, starting with "<code>net.algart.arrays.</code>" substring,
 used for internal goals. They are undocumented and should not be used in your applications.</p>


 <h2 id="logging">Built-in logging</h2>

 <p>Some classes implementing AlgART arrays logs some situations via standard <code>java.util.logging</code> tools.
 Now 3 loggers are used:</p>

 <ul>
 <li><code>Logger.getLogger("{@link net.algart.arrays.Arrays net.algart.arrays.Arrays}")</code></li>
 <li><code>Logger.getLogger("{@link net.algart.arrays.LargeMemoryModel
net.algart.arrays.LargeMemoryModel}")</code></li>
 </ul>

 <p>We don't specify what situations are logged with levels <code>FINE</code> or lower.
 Below is the information about logging with higher levels.</p>

 <ul>
 <li><code>Logger.getLogger("{@link net.algart.arrays.Arrays net.algart.arrays.Arrays}")</code>
 is used in following situations.
    <ul>
    <li>The level <code>SEVERE</code>:
        <ul>
        <li>When {@link net.algart.arrays.Arrays} class is initialized and the system properties
        <code>net.algart.arrays.globalMemoryModel</code> or <code>net.algart.arrays.globalDiskSynchronizer</code>
        contain names of illegal classes.
        See the methods {@link net.algart.arrays.Arrays.SystemSettings#globalMemoryModel()}
        and {@link net.algart.arrays.Arrays.SystemSettings#globalDiskSynchronizer()}.
        </li>
        </ul>
    </li>
    <li>The level <code>CONFIG</code>:
        <ul>
        <li>While executing {@link net.algart.arrays.Arrays#copy(ArrayContext, UpdatableArray, Array)}
        and {@link net.algart.arrays.Arrays#copy(ArrayContext, UpdatableArray, Array, int)} methods,
        if {@link net.algart.arrays.Arrays.SystemSettings#profilingMode()} returns <code>true</code>
        and the execution time is long enough.
        In this case, these methods log the time of copying and a short description of the source array
        (generated by its <code>toString()</code> method).
        Please note that these methods underlie in a lot of array processing algorithms,
        that create some "lazy" array view and then actualize it via copying into a new array.
        So, these 2 methods are often the main methods that should be profiled.
        </li>
        <li>While executing {@link net.algart.arrays.AbstractIterativeArrayProcessor#process()}
        method,
        if {@link net.algart.arrays.Arrays.SystemSettings#profilingMode()} returns <code>true</code>
        and the execution time is long enough.
        In this case, this method logs the time of iterative processing and a short description of the resulting matrix
        (generated by its <code>toString()</code> method).
        </li>
        </ul>
    </li>
    </ul>
 </li>

 <li><code>Logger.getLogger("{@link net.algart.arrays.LargeMemoryModel net.algart.arrays.LargeMemoryModel}")</code>
 is used in following situations.
    <ul>
    <li>The level <code>SEVERE</code>:
        <ul>
        <li>When {@link net.algart.arrays.LargeMemoryModel} is initialized and the system property
        <code>net.algart.arrays.LargeMemoryModel.dataFileModel</code> contains illegal name of data memory model.
        See the method {@link net.algart.arrays.LargeMemoryModel#getInstance()}.
        </li>
        <li>In <b>finalization code</b> and <b>built-in shutdown hook</b>, if some error occurs while releasing
        {@link net.algart.arrays.DataFile#map mappings},
        that usually means flushing all non-saved data to disk.
        </li>
        <li>In <b>finalization code</b> and <b>built-in shutdown hook</b>, if an attempt to delete a data file
        via {@link net.algart.arrays.DataFileModel#delete DataFileModel.delete} method leads to exception
        other than <code>java.io.IOError</code> (or <code>net.algart.arrays.IOErrorJ5</code> under JDK 1.5).
        Usually it means incorrect implementation of the custom overridden implementation of this method.
        </li>
        </ul>
    </li>
    <li>The level <code>WARNING</code>:
        <ul>
        <li>In <b>finalization code</b>, if we tries to delete a temporary {@link net.algart.arrays.DataFile data file}
        by {@link net.algart.arrays.DataFileModel#delete} method many times, but it's impossible.
        This situation occurs rarely and is not normal: the finalization code is performed only
        when no instances of AlgART arrays use this data file and all mappings are already finalized,
        so, the file should be normally deleted. Usually, if this situation though occurs,
        the deletion of this file is just scheduled to the next garbage collection.
        However, if there were a lot of attempts to delete this file already,
        this fact is logged with <code>WARNING</code> level and deletion of this file by finalization code is canceled.
        (The file will probably be though deleted by the built-in shutdown hook.)
        </li>
        </ul>
    </li>
    <li>The level <code>CONFIG</code>:
        <ul>
        <li>In <b>built-in shutdown hook</b>, when we delete a temporary {@link net.algart.arrays.DataFile data file}.
        Both normal deletion and failure while deletion are logged with the same <code>CONFIG</code> level, but with
        different messages. (More precisely, the situations are logged when the
        {@link net.algart.arrays.DataFileModel#delete DataFileMode.delete(DataFile)} method returns
        <code>true</code> or throws an exception. If this method returns <code>false</code>, that means that
        the file was already deleted, this situation is not logged or is logged with lower levels.)
        The failure while deletion is not too serious problem, because the list of all non-deleted
        temporary files can be retrieved by {@link net.algart.arrays.DataFileModel#allTemporaryFiles()} method
        and saved for further deletion in your own shutdown task installed by
        {@link net.algart.arrays.Arrays#addShutdownTask(Runnable, net.algart.arrays.Arrays.TaskExecutionOrder)
        Arrays.addShutdownTask(Runnable, TaskExecutionOrder)}.
        </li>
        <li>Maybe, in some other situations.
        </li>
        </ul>
    </li>
    </ul>
 </li>
 </ul>

 <h2 id="betterByStandard">Tasks that are better solved by standard Java collections</h2>

 <p>The following tasks are not well solved in this architecture: please use
 standard Java libraries in these cases.

 <ul>
 <li>AlgART arrays are always oriented to <b>random access</b>: here is no analog of the standard
 <code>{@link java.util.LinkedList}</code>
 class.</li>

 <li>AlgART arrays <b>do not support synchronization</b> and, therefore, <b>may be not thread-safe</b>.
 There is an exception: all {@link net.algart.arrays.Array#asImmutable() immutable arrays},
 as well as most of all immutable objects, <b>are thread-safe</b>.
 All AlgART arrays <b>are thread-compatible</b>:
 there are no problems to use an external synchronization to provide simultaneous access
 to any kind of arrays from several threads. See <a href="#multithreading">above</a>
 the precise specification of array behavior in a case of multithreading access.</li>

 <li>AlgART arrays <b>do not support serialization</b> (do not implement <code>java.io.Serializable</code>
 interface). However, the {@link net.algart.arrays.LargeMemoryModel Large memory model}
 provides easy storing arrays in external mapped files, that allows to implement an efficient alternative
 to standard serialization mechanism.</li>

 <li>Finding, inserting and removing elements to/from the middle of AlgART arrays are not
 included into the basic set of interfaces, unlike standard Java collections.
 However, there are special insertion and removal methods in {@link net.algart.arrays.Arrays} class.
 Adding and removing elements to/from the array end are simple: see
 {@link net.algart.arrays.MutableArray#pushElement(Object)},
 {@link net.algart.arrays.MutableArray#popElement()},
 {@link net.algart.arrays.MutableArray#append(Array)},
 {@link net.algart.arrays.MutableCharArray#append(String)}.</li>
 </ul>

 @author Daniel Alievsky
 */
package net.algart.arrays;
