/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2024 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.arrays;

import java.util.EmptyStackException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * <p>The memory model allowing to create <i>combined arrays</i>:
 * special kind of AlgART arrays, that store an array of Java objects with minimal amount of memory,
 * namely in one or several another "parallel" arrays.
 * A set of these arrays is named "the internal <i>storage</i>" of the combined array.</p>
 *
 * <p>There is an essential problem with storing large arrays of small objects in Java language.
 * For example, let a <code>point</code> is described by 2 integer values:
 * <code>x</code> and <code>y</code>,
 * and we need to store 10 million points. In C++ or Pascal language, we can create an array
 * of 10&nbsp;000&nbsp;000 structures (<code>struct</code> or <code>record</code> keyword), and this array
 * occupies ~80 MB memory (4 bytes per every integer).</p>
 *
 * <p>Unfortunately, the only simple way to store 10 million points in Java
 * is usage of Java array (or collection) containing instance of Java-class, such as</p><pre>
 * class Point {
 * &#32;   int x;
 * &#32;   int y;
 * }</pre>
 *
 * <p>For example,</p><pre>
 * &#32;   Point[] points = new Point[10000000];
 * &#32;   for (int k = 0; k &lt; points.length; k++)
 * &#32;       points[k] = new Point();</pre>
 *
 * <p>Such an array occupies more than 200 MB and is created very slowly (several seconds on
 * CPU P-IV 2 GHz, under Java 1.5). The reason is creating separate object for every point
 * and saving a pointer to it in Java array. Only array of primitive types work
 * efficiently in Java.</p>
 *
 * <p>The arrays created by this memory model (<i>"combined" arrays</i>) allow a solution of this problem.
 * This memory model stores an array of any Java objects in one or several "parallel" another AlgART arrays -
 * typically, wrappers for Java-arrays of primitive types
 * (created by {@link SimpleMemoryModel}) or for <code>ByteBuffer</code> objects
 * (created by {@link LargeMemoryModel}). This array is created quickly,
 * does not make a difficult problem for future garbage collection
 * and occupies only necessary amount of memory.</p>
 *
 * <p>However, <i>in many situations, combined arrays work slower, than
 * simple array of references as listed above</i>. In particular,
 * an access to stored elements of combined arrays are usually slower <i>in several times</i>,
 * than usage direct references array or standard <code>ArrayList</code>.
 * Usually, you need combined arrays if you should save occupied memory
 * or quickly create large array.</p>
 *
 * <p>The only thing that you should provide to use this class is implementing
 * {@link CombinedMemoryModel.Combiner} interface or its inheritors
 * {@link CombinedMemoryModel.CombinerInPlace},
 * {@link CombinedMemoryModel.BufferedCombiner},
 * which "tell" how to store (or load) one object in (from) a set of AlgART arrays.
 * The {@link CombinedMemoryModel.CombinerInPlace} interface allows to
 * quite eliminate usage of Java heap while working with combined array;
 * the {@link CombinedMemoryModel.BufferedCombiner} interface allows to optimize
 * block access to the combined array.</p>
 *
 * <p>Below are the main features of arrays created via this memory model.</p><ul>
 *
 * <li>Only element types inherited from <code>Object</code> are supported (not primitive):
 * so, the created arrays always implement the {@link ObjectArray} /
 * {@link UpdatableObjectArray} / {@link MutableObjectArray} interface.</li>
 *
 * <li>Arrays, created by<ul>
 * <li>{@link #newEmptyArray(Class) newEmptyArray(Class elementType)},</li>
 * <li>{@link #newEmptyArray(Class, long) newEmptyArray(Class elementType, long initialCapacity)},</li>
 * <li>{@link #newArray(Class, long) newArray(Class elementType, long initialLength)}</li>
 * </ul>are always mutable and resizable: they implement {@link MutableObjectArray} interface.
 * Arrays, created by<ul>
 * <li>{@link #asUpdatableCombinedArray(Class, UpdatableArray[])
 * asUpdatableCombinedArray(Class elementType, UpdatableArray[] storage)},</li>
 * </ul>are unresizable.
 * Arrays, created by<ul>
 * <li>{@link #asCombinedArray(Class, Array[])
 * asCombinedArray(Class elementType, Array[] storage)},</li>
 * </ul>are immutable.</li>
 *
 * <li>Arrays, created by this memory model, never implement {@link DirectAccessible} interface.</li>
 *
 * <li>Arrays, created by this memory model, never have <i>new</i> or <i>new-read-only-view</i>
 * status: {@link Array#isNew()} and {@link Array#isNewReadOnlyView()} method always return <code>false</code>,
 * because they are views of some other ({@link #getStorage(Array) underlying}) arrays.</li>
 *
 * <li>The {@link Array#loadResources(ArrayContext)},
 * {@link Array#flushResources(ArrayContext)}, {@link Array#flushResources(ArrayContext, boolean)} and
 * {@link Array#freeResources(ArrayContext, boolean)} methods just call the same methods of the storage arrays.</li>
 *
 * <li>If the combiner, passed while creating the memory model by {@link #getInstance(Combiner)}, implements
 * extended {@link CombinedMemoryModel.CombinerInPlace} interface, then the arrays, created by this memory model,
 * implement {@link ObjectInPlaceArray} / {@link UpdatableObjectInPlaceArray}
 * / {@link MutableObjectInPlaceArray}
 * interface, that allow to avoid allocating new Java objects while accessing elements.</li>
 *
 * <li>{@link UpdatableArray#copy(Array)}, {@link UpdatableArray#swap(UpdatableArray)},
 * {@link Array#updatableClone(MemoryModel)}, {@link Array#mutableClone(MemoryModel)} methods
 * copies the content of elements (<i>not references to elements!</i>) by calling
 * the corresponding methods of the storage arrays (and their
 * {@link Array#subArray(long, long) subarrays}).</li>
 *
 * <li id="hashCodeEquals">The {@link Array#equals(Object)} method,
 * in a case when another array is combined and uses the same combiner,
 * is based on calls of the corresponding methods of the storage array (and their
 * {@link Array#subArray(long, long) subarrays});
 * so, result of {@link Array#equals(Object)} (for the same combiner in both arrays)
 * <i>does not depend on implementation of <code>equals</code> method</i>
 * in the class of elements ({@link Array#elementType()})</li>
 *
 * <li>However, {@link Array#hashCode()} method, according to the comments for it,
 * <i>is still based on implementation of <code>hashCode</code> method</i>
 * in the class of elements.
 * So, you <i>should store in combined arrays only such objects,
 * that have <code>hashCode</code> method based on the their content
 * (stored inside a combined array via {@link CombinedMemoryModel.Combiner})</i>.
 * In another case, standard contract for <code>hashCode</code> and <code>equals</code>
 * methods can be violated, that can lead to problems, for example, while using arrays
 * as keys in hash maps.</li>
 * </ul>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of the created instance.</p>
 *
 * @param <E> the generic type of array elements in object arrays created by this memory model.
 * @author Daniel Alievsky
 */
public final class CombinedMemoryModel<E> extends AbstractMemoryModel {

    /**
     * <p>This interface should be implemented to allow saving objects
     * in arrays created via {@link CombinedMemoryModel#getInstance(Combiner)
     * combined memory model}.</p>
     *
     * <p>It describes how to store (or load) one object in (from) a set of AlgART arrays.
     * More precisely, this interface should provide methods for storing and loading
     * all data of one object (an element of a combined array) in/from
     * several "parallel" AlgART arrays, named "storage".
     * An element with given index should be stored in the following elements in storage:</p>
     * <pre>
     * &nbsp;&nbsp;&nbsp;&nbsp;storage[k][d[k]*index...d[k]*(index+1)-1], k=0,1,...</pre>
     * <p>where <code>d</code> is an array returned by {@link #numbersOfElementsPerOneCombinedElement(int)}
     * method.</p>
     *
     * <p>Important! The classes of objects stored or loaded by this combiner <b>should
     * have correct <code>hashCode</code> method, based on the content of an object</b>:
     * the data stored or loaded by this combiner. See also <a href="CombinedMemoryModel.html#hashCodeEquals">comments
     * to <code>CombinedMemoryModel</code></a>.
     *
     * <p>Typical implementation supposes that a storage is one or several arrays of primitive types
     * (created by {@link SimpleMemoryModel}).</p>
     *
     * <p>The methods of this interface may not check indexes of elements:
     * all necessary checks are performed by AlgART array implementation created by {@link CombinedMemoryModel}.</p>
     *
     * @param <E> the generic type of array elements in object arrays.
     */
    public interface Combiner<E> {
        /**
         * Returns an element #<code>index</code> of the combined array from the given set of AlgART arrays.
         * Unlike {@link CombinerInPlace#getInPlace(long, Object, Array[])},
         * this method always creates a new object and should work always.
         * This method is called by {@link ObjectArray#getElement(long)}.
         *
         * @param index   an index in the combined array.
         * @param storage a set of arrays where the retrieved content is stored now.
         * @return new created object containing an element #<code>index</code> of combined array.
         */
        E get(long index, Array[] storage);

        /**
         * Stores the element <code>value</code> at position #<code>index</code> of the combined array
         * inside the given set of AlgART arrays.
         * This method is called by {@link UpdatableObjectArray#setElement(long, Object)}.
         *
         * <p>Important: this method must not throw <code>NullPointerException</code>
         * if the <code>value</code> argument is {@code null}. Instead, it should
         * store some "signal" value in the storage, that cannot be stored for any
         * possible non-null elements, or just some default ("empty") value.
         * In the first case, further {@link #get get(index, storage)} should
         * return {@code null}; in the second case, it should return an instance
         * in the default state.
         *
         * @param index   an index in the combined array.
         * @param value   the stored element
         * @param storage a set of arrays where the content will be stored.
         */
        void set(long index, E value, UpdatableArray[] storage);

        /**
         * Should create a storage (several AlgART arrays),
         * allowing to store elements of the combined arrays.
         * Called while creating new combined arrays.
         *
         * <p>The initial lengths of created arrays should be calculated on the base of
         * passed <code>length</code> argument, that means the length of necessary
         * <i>combined</i> array. Namely, the length of returned array #<code>k</code>
         * should be equal to <code>length*{@link #numbersOfElementsPerOneCombinedElement(int)
         * numbersOfElementsPerOneCombinedElement}(k)</code>. This condition is automatically
         * verified while creating combined arrays.
         *
         * <p>If <code>unresizable</code> argument is <code>true</code>, it means that this method
         * is called for creating {@link Array#isUnresizable() unresizable} combined array.
         * In this case we recommend to use {@link MemoryModel#newUnresizableArray(Class, long)
         * MemoryModel.newUnresizableArray} method for creating storage arrays.
         * If <code>unresizable</code> argument is <code>false</code>, every element of returned
         * Java array <i>must</i> implement {@link MutableArray} interface.
         *
         * @param length      initial length of corresponding <i>combined</i> arrays.
         * @param unresizable if <code>true</code>, the created arrays <i>should</i> be unresizable,
         *                    in other case they <i>must</i> be mutable and implement {@link MutableArray} interface.
         * @return created storage.
         */
        UpdatableArray[] allocateStorage(long length, boolean unresizable);

        /**
         * Should return the number of sequential elements of the array #<code>indexOfArrayInStorage</code>
         * in the storage, used for storing one element of the combined array.
         * Called while creating new combined arrays.
         *
         * @param indexOfArrayInStorage index of the storage array.
         * @return the number of sequential elements used for storing one combined element.
         */
        int numbersOfElementsPerOneCombinedElement(int indexOfArrayInStorage);
    }

    /**
     * <p>Special version of {@link Combiner} interface allowing
     * to load an element without creating new Java object.
     * If the argument of {@link CombinedMemoryModel#getInstance(Combiner)} method
     * implements this interface, then the arrays created by returned memory model
     * will implement {@link MutableObjectInPlaceArray}
     * (or, maybe, only {@link UpdatableObjectInPlaceArray} or {@link ObjectInPlaceArray},
     * for arrays created by
     * {@link CombinedMemoryModel#asUpdatableCombinedArray(Class, UpdatableArray[])} or
     * {@link CombinedMemoryModel#asCombinedArray(Class, Array[])} methods).</p>
     *
     * @param <E> the generic type of array elements in object arrays.
     */
    public interface CombinerInPlace<E> extends Combiner<E> {

        /**
         * Creates a new element that can be stored in or loaded from the combined array.
         * Never returns {@code null}.
         *
         * @return some instance of an element of the combined array.
         */
        E allocateElement();

        /**
         * Loads an element #<code>index</code> of the combined array from the given set of arrays
         * into <code>resultValue</code> object.
         * This method is called by {@link ObjectInPlaceArray#getInPlace(long, Object)} method.
         *
         * @param index       an index in the combined array.
         * @param resultValue the object where the retrieved content will be stored.
         * @param storage     a set of arrays where the retrieved content is stored now.
         * @throws NullPointerException if <code>resultValue</code> is {@code null}.
         */
        void getInPlace(long index, E resultValue, Array[] storage);
    }

    /**
     * <p>Special version of {@link Combiner} interface allowing
     * to optimize block access to the combined array.</p>
     *
     * <p>If the argument of {@link CombinedMemoryModel#getInstance(Combiner)} method
     * implements this interface, then {@link Array#getData(long, Object, int, int) getData(...)},
     * {@link UpdatableArray#setData(long, Object, int, int) setData(...)} and
     * {@link Array#buffer buffer-access} methods will use block <code>get/set</code>
     * methods declared in this interface. In another case, those methods
     * will call separate <code>get/set</code> method, declared in {@link Combiner},
     * for every loaded/stored element of the combined array.</p>
     *
     * @param <E> the generic type of array elements in object arrays.
     */
    public interface BufferedCombiner<E> extends Combiner<E> {
        /**
         * Reads <code>count</code> elements of the combined array,
         * starting from te specified index, from the given set of AlgART arrays (<code>storage</code>).
         * Loaded elements are placed into <code>resultValues</code>
         * Java array at the positions <code>#offset..#offset+count-1</code>.
         * This method is called by {@link ObjectArray#getData(long, Object, int, int)}
         * and {@link DataObjectBuffer#map(long)} methods.
         *
         * <p>Note: if <code>resultValues[offset+k]!=null</code> for some index <code>k (0&lt;=k&lt;count)</code>,
         * and the element type allows changing full element state,
         * this method <i>may</i> not to allocate new object for this index,
         * but load the corresponding combined element <code>#index+k</code>
         * into <code>resultValues[offset+k]</code>.
         * It can essentially optimize loading a large number of elements.</p>
         *
         * @param index        starting index in the combined array.
         * @param resultValues the target Java array.
         * @param offset       starting position in the target Java array.
         * @param count        the number of elements to be retrieved.
         * @param storage      a set of arrays where the retrieved content is stored now.
         */
        void get(long index, E[] resultValues, int offset, int count, Array[] storage);

        /**
         * Stores <code>count</code> elements of the combined array,
         * starting from te specified index,
         * inside the given set of AlgART arrays (<code>storage</code>).
         * The elements are loaded from <code>values</code> Java array
         * from at the positions <code>#offset..#offset+count-1</code>.
         * This method is called by {@link UpdatableObjectArray#setData(long, Object, int, int)}
         * and {@link DataObjectBuffer#force(long, long)} methods.
         *
         * <p>Important: this method must not throw <code>NullPointerException</code>
         * if some element of <code>values</code> Java array is {@code null}. Instead, it should
         * store some "signal" value in the storage, that cannot be stored for any
         * possible non-null elements, or just some default ("empty") value.
         * In the first case, further {@link #get get(index, storage)} should
         * return {@code null} for this element; in the second case, it should return an instance
         * in the default state.
         *
         * @param index   starting index in the combined array.
         * @param values  the source Java array.
         * @param offset  starting position in the source Java array.
         * @param count   the number of elements to be stored.
         * @param storage a set of arrays where the content will be stored.
         */
        void set(long index, E[] values, int offset, int count, UpdatableArray[] storage);
    }

    /**
     * <p>A skeleton class allowing to simplify implementation of
     * {@link Combiner} interface.</p>
     *
     * <p>To create a combiner, based in this class, it's enough
     * to override only 2 very simple methods
     * {@link #loadElement()} and {@link #storeElement(Object)}.
     * These methods operate with a little <code>ByteBuffer</code>
     * {@link #workStorage}, and this class automatically
     * copy the content of this buffer from / to the storage array.</p>
     *
     * <p>This combiner always use a single {@link ByteArray} as a storage.
     * If you need to store different types of data (for example, <code>int</code>
     * and <code>double</code> fields of the stored objects),
     * you may use corresponding views of {@link #workStorage} buffer
     * (<code>asIntBuffer()</code>, <code>asDoubleBuffer</code>, etc.).</p>
     *
     * <p>Unfortunately, for simple structure of element types,
     * this combiner usually work essentially slower
     * than the direct implementation of {@link Combiner} interface.</p>
     *
     * @param <E> the generic type of array elements in object arrays.
     */
    public abstract static class AbstractByteBufferCombiner<E> implements Combiner<E> {
        /**
         * A little <code>ByteBuffer</code> for storing one element of the combined array.
         * This reference is copied from the corresponding constructor argument.
         */
        protected final ByteBuffer workStorage;

        final Class<?> elementType;
        final int elementSize;
        final boolean isDirect;
        final byte[] array;
        private final MemoryModel mm;

        /**
         * Creates a new instance of this combiner.
         *
         * @param elementType              the type of elements of the combined array.
         * @param workStorageForOneElement a little <code>ByteBuffer</code> enough to store one element
         *                                 of the combined array. May be direct ByteBuffer, but the heap
         *                                 one usually provides better performance.
         * @param memoryModel              the {@link MemoryModel memory model} which will be used for creating
         *                                 combined arrays.
         * @throws NullPointerException     if one of the arguments is {@code null}.
         * @throws IllegalArgumentException if the passed ByteBuffer is read-only.
         */
        protected AbstractByteBufferCombiner(
                Class<?> elementType,
                ByteBuffer workStorageForOneElement,
                MemoryModel memoryModel) {
            Objects.requireNonNull(elementType, "Null elementType");
            if (elementType == void.class) {
                throw new IllegalArgumentException("Illegal elementType: it cannot be void.class");
            }
            Objects.requireNonNull(workStorageForOneElement, "Null workStorageForOneElement argument");
            if (workStorageForOneElement.isReadOnly()) {
                throw new IllegalArgumentException("Illegal workStorageForOneElement argument: "
                        + "it must not be read-only");
            }
            Objects.requireNonNull(memoryModel, "Null memoryModel argument");
            if (!memoryModel.isElementTypeSupported(byte.class)) {
                throw new IllegalArgumentException("Illegal memoryModel argument: it must support byte elements");
            }
            this.elementType = elementType;
            this.workStorage = workStorageForOneElement;
            this.elementSize = workStorageForOneElement.limit();
            this.isDirect = !workStorageForOneElement.hasArray();
            this.array = isDirect ? new byte[elementSize] : workStorageForOneElement.array();
            this.mm = memoryModel;
        }

        /**
         * Should create one element of the combined array and fill it from {@link #workStorage}.
         *
         * @return newly created object.
         */
        protected abstract E loadElement();

        /**
         * Should store all information about the passed element of the combiner array in {@link #workStorage}.
         *
         * @param element the stored element.
         */
        protected abstract void storeElement(E element);

        public final E get(long index, Array[] storage) {
            storage[0].getData(index * elementSize, array, 0, elementSize);
            if (isDirect) {
                workStorage.rewind();
                workStorage.put(array);
            }
            return loadElement();
        }

        public final void set(long index, E value, UpdatableArray[] storage) {
            storeElement(value);
            if (isDirect) {
                workStorage.rewind();
                workStorage.get(array);
            }
            storage[0].setData(index * elementSize, array, 0, elementSize);
        }

        public final UpdatableArray[] allocateStorage(long length, boolean unresizable) {
            if (unresizable) {
                return new UpdatableByteArray[]{mm.newUnresizableByteArray(length * elementSize)};
            } else {
                return new MutableByteArray[]{mm.newByteArray(length * elementSize)};
            }
        }

        public final int numbersOfElementsPerOneCombinedElement(int indexOfArrayInStorage) {
            return elementSize;
        }
    }

    /**
     * <p>A version of {@link AbstractByteBufferCombiner} skeleton class
     * implementing {@link CombinerInPlace} interface.</p>
     *
     * @param <E> the generic type of array elements in object arrays.
     */
    public abstract static class AbstractByteBufferCombinerInPlace<E>
            extends AbstractByteBufferCombiner<E>
            implements CombinerInPlace<E> {
        /**
         * Creates a new instance of this combiner.
         *
         * @param elementType              the type of elements of the combined array.
         * @param workStorageForOneElement a little <code>ByteBuffer</code> enough to store one element
         *                                 of the combined array. May be direct ByteBuffer, but the heap
         *                                 one usually provides better performance.
         * @param memoryModel              the {@link MemoryModel memory model} which will be used for creating
         *                                 combined arrays.
         * @throws NullPointerException     if one of the arguments is {@code null}.
         * @throws IllegalArgumentException if the passed ByteBuffer is read-only.
         */
        protected AbstractByteBufferCombinerInPlace(
                Class<?> elementType,
                ByteBuffer workStorageForOneElement,
                MemoryModel memoryModel) {
            super(elementType, workStorageForOneElement, memoryModel);
        }

        public abstract E allocateElement();

        /**
         * Should fill the passed element of the combined array from {@link #workStorage}.
         *
         * @param resultElement the object where the retrieved content will be stored.
         * @throws NullPointerException if the argument is {@code null}.
         */
        protected abstract void loadElementInPlace(E resultElement);

        public final void getInPlace(long index, E resultValue, Array[] storage) {
            storage[0].getData(index * elementSize, array, 0, elementSize);
            if (isDirect) {
                workStorage.rewind();
                workStorage.put(array);
            }
            loadElementInPlace(resultValue);
        }
    }

    private final Combiner<E> combiner;

    private CombinedMemoryModel(Combiner<E> combiner) {
        Objects.requireNonNull(combiner, "Null combiner argument");
        this.combiner = combiner;
    }

    /**
     * Creates new memory model with corresponding combiner.
     * If <code>combiner</code> argument implements {@link CombinerInPlace} (not only {@link Combiner}),
     * then the arrays created via this memory model will implement {@link ObjectInPlaceArray} interface
     * (not only {@link ObjectArray}).
     *
     * <p><i>Note</i>: if you need to create several instances of {@link CombinedMemoryModel}
     * with identical combiner, please use the same reference to {@link Combiner}
     * object for all memory model instances.
     * If two combined arrays were created with the same combiner, then
     * some operations with these arrays (namely,
     * {@link UpdatableArray#copy(Array)} and {@link UpdatableArray#swap(UpdatableArray)} will work faster.
     *
     * @param <E>      the generic type of array elements.
     * @param combiner will be used for creation of combined arrays by this memory model.
     * @return created memory model.
     * @throws NullPointerException if <code>combiner</code> is {@code null}.
     */
    public static <E> CombinedMemoryModel<E> getInstance(Combiner<E> combiner) {
        return new CombinedMemoryModel<E>(combiner);
    }

    /**
     * This implementation returns <code>{@link #newEmptyArray(Class, long) newArray}(elementType, 10)</code>.
     *
     * @param elementType the type of array elements.
     * @return created array.
     * @throws NullPointerException            if <code>elementType</code> is {@code null}.
     * @throws IllegalArgumentException        if <code>elementType</code> is not supported of <code>void.class</code>.
     * @throws UnsupportedElementTypeException if <code>elementType</code> is not supported by this memory model.
     */
    public MutableArray newEmptyArray(Class<?> elementType) {
        return newEmptyArray(elementType, 10);
    }

    /**
     * Constructs an empty array with the specified element type and initial capacity.
     * The element type can be any non-primitive class (inheritor of <code>Object</code> class).
     * The created array will always implement the {@link MutableObjectArray} interface
     * (or {@link ObjectInPlaceArray}, if the combiner, passed while creating the memory model, implements
     * {@link CombinedMemoryModel.CombinerInPlace}).
     *
     * @param elementType     the type of array elements (non-primitive).
     * @param initialCapacity the initial capacity of the array.
     * @return created array.
     * @throws NullPointerException            if <code>elementType</code> is {@code null}.
     * @throws IllegalArgumentException        if <code>elementType</code> is <code>void.class</code>
     *                                         or if the specified initial length is negative.
     * @throws UnsupportedElementTypeException if <code>elementType</code> is a primitive type.
     * @throws TooLargeArrayException          if the specified initial length is too large.
     * @see #isElementTypeSupported(Class)
     */
    public MutableArray newEmptyArray(Class<?> elementType, long initialCapacity) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (initialCapacity < 0) {
            throw new IllegalArgumentException("Negative initial capacity");
        }
        if (!Object.class.isAssignableFrom(elementType)) {
            throw new UnsupportedElementTypeException(
                    "Primitive element types are not allowed (passed type: " + elementType + ")");
        }
        Class<E> eType = InternalUtils.cast(elementType);
        if (combiner instanceof CombinerInPlace<?>) {
            return new MutableCombinedInPlaceArray<E>(eType, initialCapacity, 0, (CombinerInPlace<E>) combiner);
        } else {
            return new MutableCombinedArray<E>(eType, initialCapacity, 0, combiner);
        }
    }

    public MutableArray newArray(Class<?> elementType, long initialLength) {
        return newEmptyArray(elementType, initialLength).length(initialLength).trim();
    }

    public UpdatableArray newUnresizableArray(Class<?> elementType, long length) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (length < 0) {
            throw new IllegalArgumentException("Negative array length");
        }
        if (!Object.class.isAssignableFrom(elementType)) {
            throw new UnsupportedElementTypeException(
                    "Primitive element types are not allowed (passed type: " + elementType + ")");
        }
        Class<E> eType = InternalUtils.cast(elementType);
        if (combiner instanceof CombinerInPlace<?>) {
            return new UpdatableCombinedInPlaceArray<E>(eType, length, length, (CombinerInPlace<E>) combiner);
        } else {
            return new UpdatableCombinedArray<E>(eType, length, length, combiner);
        }
    }

    /**
     * Returns <code>true</code> if this element type is an inheritor of <code>Object</code> class.
     *
     * @param elementType the type of array elements.
     * @return <code>true</code> if this memory model supports this element type.
     */
    public boolean isElementTypeSupported(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        return Object.class.isAssignableFrom(elementType);
    }

    public boolean areAllPrimitiveElementTypesSupported() {
        return false;
    }

    public boolean areAllElementTypesSupported() {
        return false;
    }

    /**
     * This implementation always returns <code>Long.MAX_VALUE</code> for supported
     * (non-primitive) element types.
     * Actual maximal possible array length depends on memory model used by the combiner
     * and on the number of sequential elements of storage arrays
     * used for storing one element of the combined array.
     *
     * @param elementType the type of array elements.
     * @return maximal possible length of arrays supported by this memory model.
     * @throws NullPointerException if <code>elementType</code> is {@code null}.
     */
    public long maxSupportedLength(Class<?> elementType) {
        Objects.requireNonNull(elementType, "Null elementType argument");
        return Long.MAX_VALUE;
    }

    public boolean isCreatedBy(Array array) {
        return array instanceof CombinedArray<?> && ((CombinedArray<?>) array).combiner == this.combiner;
    }

    /**
     * Returns <code>true</code> if the passed instance is a combined array created by some instance of
     * combined memory model.
     * Returns <code>false</code> if the passed array is {@code null}
     * or an AlgART array created by another memory model.
     *
     * @param array the checked array.
     * @return <code>true</code> if this array is a combined array created by a combined memory model.
     */
    public static boolean isCombinedArray(Array array) {
        return array instanceof CombinedArray<?>;
    }

    /**
     * Returns an immutable combined array backed by the storage,
     * which consists of immutable views of the passed argument
     * (<code>storage[0].{@link Array#asImmutable() asImmutable()}</code>,
     * (<code>storage[1].{@link Array#asImmutable() asImmutable()}</code>, ...,
     * (<code>storage[storage.length-1].{@link Array#asImmutable() asImmutable()}</code>,
     * with the current combiner (specified while creating this memory model).
     *
     * <p>If modifications of the passed arrays lead to reallocation
     * of the internal storage, then the returned array <i>ceases to be a view of passed arrays</i>.
     * The only possible reasons for reallocation are the following:
     * calling {@link MutableArray#length(long)},
     * {@link MutableArray#ensureCapacity(long)} or {@link MutableArray#trim()} methods
     * for this array, or any modification of this or returned array in a case when
     * this array is {@link Array#asCopyOnNextWrite() copy-on-next-write}.
     *
     * @param elementType the type of created array elements
     * @param storage     the internal storage where the combined array elements will be stored
     * @return combined array backed by the specified storage
     */
    public ObjectArray<E> asCombinedArray(Class<E> elementType, Array[] storage) {
        Array[] storageClone = storage.clone(); // preserves the type of storage elements
        for (int k = 0; k < storageClone.length; k++) {
            storageClone[k] = storageClone[k].asImmutable();
        }
        if (combiner instanceof CombinerInPlace<?>) {
            return new CombinedInPlaceArray<E>(elementType, storageClone, (CombinerInPlace<E>) combiner);
        } else {
            return new CombinedArray<E>(elementType, storageClone, combiner);
        }
    }

    /**
     * Returns an unresizable combined array backed by the storage,
     * which consists of shallow unresizable copies of the passed argument
     * (<code>storage[0].{@link UpdatableArray#asUnresizable() asUnresizable()}.{@link
     * Array#shallowClone() shallowClone()}</code>,
     * (<code>storage[1].{@link UpdatableArray#asUnresizable() asUnresizable()}.{@link
     * Array#shallowClone() shallowClone()}</code>, ...,
     * (<code>storage[storage.length-1].{@link UpdatableArray#asUnresizable() asUnresizable()}.{@link
     * Array#shallowClone() shallowClone()}</code>,
     * with the current combiner (specified while creating this memory model).
     * Changes of elements of the passed arrays
     * will be reflected in the returned combined array, and vice-versa.
     *
     * <p>Using shallow copies means that any further changes of lengths or capacities
     * of the passed <code>storage</code> arrays will not affect to the length or capacity of
     * the returned combined array. However, changes of elements of the passed arrays
     * will be reflected in the returned combined array, and vice-versa. Also it means that
     * if modifications of the passed arrays characteristics lead to reallocation
     * of their internal storage, then the returned array <i>ceases to be a view of passed arrays</i>.
     * The only possible reasons for reallocation are the following:
     * calling {@link MutableArray#length(long)},
     * {@link MutableArray#ensureCapacity(long)} or {@link MutableArray#trim()} methods
     * for <code>storage[]</code> elements, or any modification of <code>storage[]</code> elements
     * in a case when they are {@link Array#asCopyOnNextWrite() copy-on-next-write}.
     *
     * @param elementType the type of created array elements
     * @param storage     the internal storage where the combined array elements will be stored
     * @return combined array backed by the specified storage
     */
    public UpdatableObjectArray<E> asUpdatableCombinedArray(Class<E> elementType, UpdatableArray[] storage) {
        UpdatableArray[] storageClone = storage.clone(); // preserves the type of storage elements
        for (int k = 0; k < storageClone.length; k++) {
            storageClone[k] = storageClone[k].asUnresizable().shallowClone();
        }
        if (combiner instanceof CombinerInPlace<?>) {
            return new UpdatableCombinedInPlaceArray<E>(elementType, storageClone, (CombinerInPlace<E>) combiner);
        } else {
            return new UpdatableCombinedArray<E>(elementType, storageClone, combiner);
        }
    }

    /**
     * Returns a brief string description of this memory model.
     *
     * <p>The result of this method may depend on implementation.
     *
     * @return a brief string description of this object.
     */
    public String toString() {
        return "Combined memory model [combiner: " + this.combiner + "]";
    }

    /**
     * Returns Java array of shallow copies (produced by {@link Array#shallowClone()}
     * of arrays used as internal storage,
     * where the elements of the combined array are stored.
     * If this combined array implements {@link UpdatableArray} interface,
     * than the returned array will be an instance of {@link UpdatableArray}[].
     * If this combined array implements {@link MutableArray} interface,
     * than the returned array will be an instance of {@link MutableArray}[].
     *
     * <p>Using shallow copies means that you are free to change the lengths or capacities
     * of the returned arrays: it will not affect to the length or capacity of
     * the passed combined array. However, changes of elements
     * of the returned arrays will be reflected in the combined array, and vice-versa.
     * If modifications of the passed combined array characteristics lead to reallocation
     * of the internal storage, then the returned arrays ceases to be a view of the passed array.
     * The only possible reasons for reallocation are the following:
     * calling {@link MutableArray#length(long)},
     * {@link MutableArray#ensureCapacity(long)} or {@link MutableArray#trim()} methods
     * for this array, or any modification of this or returned array in a case when
     * this array is {@link Array#asCopyOnNextWrite() copy-on-next-write}.
     *
     * @param combinedArray the combined array.
     * @return array of shallow copies of arrays used as internal storage.
     * @throws NullPointerException     if <code>combinedArray</code> is {@code null}.
     * @throws IllegalArgumentException if <code>combinedArray</code> is not a combined array
     *                                  (created by this memory model).
     * @see Array#shallowClone()
     */
    public static Array[] getStorage(Array combinedArray) {
        Objects.requireNonNull(combinedArray, "Null combinedArray argument");
        if (!(combinedArray instanceof CombinedArray<?> cv)) {
            throw new IllegalArgumentException("The passed argument is not a combined array");
        }
        Array[] result = cv.storage.clone();
        for (int k = 0; k < result.length; k++) {
            result[k] = cv.storage[k].shallowClone();
        }
        return result;
    }

    /**
     * Fully equivalent to {@link #getStorage(Array)} method.
     *
     * @param combinedArray the combined array.
     * @return array of shallow copies of arrays used as internal storage.
     * @throws NullPointerException     if <code>combinedArray</code> is {@code null}.
     * @throws IllegalArgumentException if <code>combinedArray</code> is not a combined array
     *                                  (created by this memory model).
     * @see Array#shallowClone()
     */
    public static UpdatableArray[] getStorage(UpdatableArray combinedArray) {
        return (UpdatableArray[]) getStorage((Array) combinedArray);
    }

    /**
     * Fully equivalent to {@link #getStorage(Array)} method.
     *
     * @param combinedArray the combined array.
     * @return array of shallow copies of arrays used as internal storage.
     * @throws NullPointerException     if <code>combinedArray</code> is {@code null}.
     * @throws IllegalArgumentException if <code>combinedArray</code> is not a combined array
     *                                  (created by this memory model).
     * @see Array#shallowClone()
     */
    public static MutableArray[] getStorage(MutableArray combinedArray) {
        return (MutableArray[]) getStorage((Array) combinedArray);
    }

    /**
     * Returns array of string representations (results of <code>toString()</code> method}
     * of arrays used as internal storage,
     * where the elements of the combined array are stored.
     *
     * @param combinedArray the combined array.
     * @return array of string representations of arrays used as internal storage.
     * @throws NullPointerException     if <code>combinedArray</code> is {@code null}.
     * @throws IllegalArgumentException if <code>combinedArray</code> is not a combined array
     *                                  (created by this memory model).
     */
    public static String[] getStorageToStrings(Array combinedArray) {
        Objects.requireNonNull(combinedArray, "Null combinedArray argument");
        if (!(combinedArray instanceof CombinedArray<?> cv)) {
            throw new IllegalArgumentException("The passed argument is not a combined array");
        }
        String[] result = new String[cv.storage.length];
        for (int k = 0; k < result.length; k++) {
            result[k] = cv.storage[k].toString();
        }
        return result;
    }

    static class CombinedArray<E> extends AbstractArray implements ObjectArray<E> {

        final Class<E> elementType;
        final Combiner<E> combiner;
        Array[] storage;
        final int[] numbersOfElements;
        final boolean allNumbersAre1;

        CombinedArray(Class<E> elementType, Array[] storage, Combiner<E> combiner) {
            super(Integer.MAX_VALUE, 0);
            Objects.requireNonNull(storage, "Null storage argument");
            Objects.requireNonNull(combiner, "Null combiner argument");
            this.combiner = combiner;
            this.elementType = elementType;
            this.allNumbersAre1 = checkStorageAndNumbersOfElements(storage, combiner);
            this.storage = storage;
            this.numbersOfElements = new int[storage.length];
            for (int k = 0; k < storage.length; k++) {
                this.numbersOfElements[k] = combiner.numbersOfElementsPerOneCombinedElement(k);
            }
            this.length = storage[0].length() / numbersOfElements[0];
            recalculateCapacity();
        }

        CombinedArray(Class<E> elementType, long initialCapacity, long initialLength, Combiner<E> combiner) {
            super(initialCapacity, initialLength);
            Objects.requireNonNull(combiner, "Null combiner argument");
            this.combiner = combiner;
            this.elementType = elementType;
            boolean unresizableRequired = this.isUnresizable();
            UpdatableArray[] stor = combiner.allocateStorage(initialLength, unresizableRequired);
            this.allNumbersAre1 = checkStorageAndNumbersOfElements(stor, combiner);
            this.numbersOfElements = new int[stor.length];
            for (int k = 0; k < stor.length; k++) {
                this.numbersOfElements[k] = combiner.numbersOfElementsPerOneCombinedElement(k);
            }
            for (int k = 0; k < stor.length; k++) {
                if (!unresizableRequired && !(stor[k] instanceof MutableArray)) {
                    throw new IllegalArgumentException("Storage array #" + k + " does not implement MutableArray, "
                            + "though the resizable one is required while calling Combiner.allocateStorage method");
                }
                long correctLength = InternalUtils.longMulAndException(initialLength, numbersOfElements[k]);
                if (stor[k].length() != correctLength) {
                    throw new IllegalArgumentException("Incorrect length of storage array #" + k
                            + ": length = " + storage[k].length() + ", but the correct one is " + correctLength);
                }
            }
            this.capacity = initialCapacity;
            if (!unresizableRequired) {
                for (int k = 0; k < stor.length; k++) {
                    ((MutableArray) stor[k]).ensureCapacity(InternalUtils.longMulAndException(
                            initialCapacity, numbersOfElements[k]));
                }
            } else {
                if (initialCapacity != initialLength) {
                    throw new AssertionError("Unequal length and capacity while creating unresizable array!");
                }
            }
            if (unresizableRequired) {
                this.storage = stor;
            } else { // In this case allocateStorage MAY return UpdatableArray[] instead of necessary MutableArray[]
                this.storage = new MutableArray[stor.length];
                System.arraycopy(stor, 0, this.storage, 0, stor.length);
            }
        }

        final void recalculateCapacity() {
            long cap = Long.MAX_VALUE;
            for (int k = 0; k < storage.length; k++) {
                cap = Math.min(cap, storage[k].capacity() / numbersOfElements[k]);
            }
            this.capacity = cap;
        }

        private static boolean checkStorageAndNumbersOfElements(Array[] storage, Combiner<?> combiner) {
            Objects.requireNonNull(storage, "Null Array[] storage");
            if (storage.length == 0) {
                throw new IllegalArgumentException("Storage must contain at least 1 array");
            }
            boolean result = true;
            long length0 = 157;
            int ne0 = 28;
            for (int k = 0; k < storage.length; k++) {
                Objects.requireNonNull(storage[k], "Null storage[" + k + "] array");
                int ne = combiner.numbersOfElementsPerOneCombinedElement(k);
                if (ne <= 0) {
                    throw new IllegalArgumentException("combiner.numbersOfElementsPerOneCombinedElement("
                            + k + ") = " + ne + " <= 0");
                }
                result &= ne == 1;
                if (storage[k].length() % ne != 0) {
                    throw new IllegalArgumentException("Incorrect length of storage array #" + k
                            + ": length = " + storage[k].length() + " and is not divided by "
                            + "combiner.numbersOfElementsPerOneCombinedElement(" + k + ") = " + ne);
                }
                long length = storage[k].length() / ne;
                if (k == 0) {
                    length0 = length;
                    ne0 = ne;
                } else if (length != length0) {
                    throw new IllegalArgumentException("Storage arrays lengths don't correspond to each other: "
                            + " the length of array # " + k + " = " + storage[k].length()
                            + " = " + length + " * " + ne + " (" + ne + " per one combined object), "
                            + "but the length of array #0 = " + storage[0].length()
                            + " = " + length0 + " * " + ne0 + " (" + ne0 + " per one combined object)");
                }
            }
            return result;
        }

        public final Class<E> elementType() {
            return elementType;
        }

        public Class<? extends ObjectArray<E>> type() {
            return InternalUtils.cast(ObjectArray.class);
        }

        public Class<? extends UpdatableObjectArray<E>> updatableType() {
            return InternalUtils.cast(UpdatableObjectArray.class);
        }

        public Class<? extends MutableObjectArray<E>> mutableType() {
            return InternalUtils.cast(MutableObjectArray.class);
        }

        public final Object getElement(long index) {
            return combiner.get(index, storage);
        }

        public final E get(long index) {
            return combiner.get(index, storage);
        }

        public long indexOf(long lowIndex, long highIndex, E value) {
            long k = Math.max(lowIndex, 0);
            long n = Math.min(length(), highIndex);
            if (value == null) {
                for (; k < n; k++) {
                    if (get(k) == null) {
                        return k;
                    }
                }
            } else {
                for (; k < n; k++) {
                    if (value.equals(get(k))) {
                        return k;
                    }
                }
            }
            return -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, E value) {
            long k = Math.min(length(), highIndex);
            // warning: highIndex-1 can be invalid value Long.MAX_VALUE
            long low = Math.max(lowIndex, 0);
            if (value == null) {
                for (; k > low; ) {
                    if (get(--k) == null) {
                        return k;
                    }
                }
            } else {
                for (; k > low; ) {
                    if (value.equals(get(--k))) {
                        return k;
                    }
                }
            }
            return -1;
        }

        public void getData(long arrayPos, Object destArray, int destArrayOffset, int count) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of loaded elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            E[] dest = InternalUtils.cast(destArray);
            if (combiner instanceof BufferedCombiner<?>) {
                ((BufferedCombiner<E>) combiner).get(arrayPos, dest, destArrayOffset, count, storage);
            } else {
                getDataInternal(arrayPos, dest, destArrayOffset, count);
            }
        }

        public void getData(long arrayPos, Object destArray) {
            Objects.requireNonNull(destArray, "Null destArray argument");
            if (arrayPos < 0 || arrayPos > length) {
                throw rangeException(arrayPos);
            }
            int count = java.lang.reflect.Array.getLength(destArray);
            if (count > length - arrayPos) {
                count = (int) (length - arrayPos);
            }
            E[] dest = InternalUtils.cast(destArray);
            if (combiner instanceof BufferedCombiner<?>) {
                ((BufferedCombiner<E>) combiner).get(arrayPos, dest, 0, count, storage);
            } else {
                getDataInternal(arrayPos, dest, 0, count);
            }
        }

        private void getDataInternal(long arrayPos, Object[] dest, int destArrayOffset, int count) {
            for (int k = destArrayOffset, kMax = destArrayOffset + count; k < kMax; k++, arrayPos++) {
                dest[k] = combiner.get(arrayPos, storage);
            }
        }

        public Array subArray(long fromIndex, long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            Array[] stor = new Array[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = storage[k].subArray(
                        fromIndex * numbersOfElements[k], toIndex * numbersOfElements[k]);
            }
            return new CombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public Array subArr(long position, long count) {
            checkSubArrArguments(position, count);
            Array[] stor = new Array[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = storage[k].subArr(
                        position * numbersOfElements[k], count * numbersOfElements[k]);
            }
            return new CombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public DataObjectBuffer<E> buffer(DataBuffer.AccessMode mode, long capacity) {
            return InternalUtils.cast(super.buffer(mode, capacity));
        }

        public DataObjectBuffer<E> buffer(DataBuffer.AccessMode mode) {
            return InternalUtils.cast(super.buffer(mode));
        }

        public DataObjectBuffer<E> buffer(long capacity) {
            return InternalUtils.cast(super.buffer(capacity));
        }

        public DataObjectBuffer<E> buffer() {
            return InternalUtils.cast(super.buffer());
        }

        public final ObjectArray<E> asImmutable() {
            if (isImmutable()) {
                return this;
            }
            Array[] stor = new Array[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = storage[k].asImmutable();
            }
            return new CombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public boolean isImmutable() {
            return storage[0].isImmutable();
        }

        public final ObjectArray<E> asTrustedImmutable() {
            if (isImmutable()) {
                return this;
            }
            Array[] stor = new Array[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = storage[k].asTrustedImmutable();
            }
            return new CombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public final void checkUnallowedMutation() throws UnallowedMutationError {
            try {
                for (Array s : storage) {
                    s.checkUnallowedMutation();
                }
            } catch (UnallowedMutationError ex) {
                UnallowedMutationError e = new UnallowedMutationError(
                        "Unallowed mutations of trusted immutable array "
                                + " are detected by checkUnallowedMutation() method [" + this + "]");
                e.initCause(ex);
                throw e;
            }
        }

        public Array asCopyOnNextWrite() {
            if (isImmutable()) {
                return this;
            }
            Array[] stor = new Array[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = storage[k].asCopyOnNextWrite();
            }
            return new CombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public boolean isCopyOnNextWrite() {
            return storage[0].isCopyOnNextWrite();
        }

        public boolean isUnresizable() {
            return true;
        }

        public Array shallowClone() {
            Array[] stor = new Array[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = storage[k].shallowClone();
            }
            return new CombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public MutableObjectArray<E> mutableClone(MemoryModel memoryModel) {
            return InternalUtils.cast(super.mutableClone(memoryModel));
        }

        public UpdatableObjectArray<E> updatableClone(MemoryModel memoryModel) {
            return InternalUtils.cast(super.updatableClone(memoryModel));
        }

        public E[] ja() {
            return Arrays.toJavaArray(this);
        }

        public <D> ObjectArray<D> cast(Class<D> elementType) {
            if (!elementType.isAssignableFrom(this.elementType)) {
                throw new ClassCastException("Illegal desired element type " + elementType + " for " + this);
            }
            return InternalUtils.cast(this);
        }

        @Override
        public final void loadResources(ArrayContext context) {
            for (int k = 0; k < storage.length; k++) {
                storage[k].loadResources(context == null ? null : context.part(k, k + 1, storage.length));
            }
        }

        @Override
        public final void flushResources(ArrayContext context, boolean forcePhysicalWriting) {
            for (int k = 0; k < storage.length; k++) {
                storage[k].flushResources(
                        context == null ? null : context.part(k, k + 1, storage.length),
                        forcePhysicalWriting);
            }
        }

        @Override
        public final void freeResources(ArrayContext context, boolean forcePhysicalWriting) {
            for (int k = 0; k < storage.length; k++) {
                storage[k].freeResources(
                        context == null ? null : context.part(k, k + 1, storage.length),
                        forcePhysicalWriting);
            }
        }

        public String toString() {
            return (isImmutable() ? "immutable " : "")
                    + "combined array " + elementType.getName()
                    + "[" + length() + "], capacity " + capacity();
        }

        public final boolean equals(Object obj) {
            if (obj instanceof CombinedArray<?> && ((CombinedArray<?>) obj).combiner == this.combiner) {
                for (int k = 0; k < storage.length; k++) {
                    CombinedArray<?> a = (CombinedArray<?>) obj;
                    if (!storage[k].equals(a.storage[k])) {
                        return false;
                    }
                }
                return true;
            }
            return super.equals(obj);
        }

        final Object javaArrayInternal() {
            return null;
        }

        final int javaArrayOffsetInternal() {
            return 0;
        }
    }

    static class UpdatableCombinedArray<E> extends CombinedArray<E> implements UpdatableObjectArray<E> {
        UpdatableCombinedArray(Class<E> elementType, UpdatableArray[] storage, Combiner<E> combiner) {
            super(elementType, storage, combiner);
        }

        UpdatableCombinedArray(Class<E> elementType, long initialCapacity, long initialLength, Combiner<E> combiner) {
            super(elementType, initialCapacity, initialLength, combiner);
        }

        public final void setElement(long index, Object value) {
            if (value != null && !elementType.isAssignableFrom(value.getClass())) {
                throw new ClassCastException("Invalid type of setElement argument");
            }
            combiner.set(index, InternalUtils.<E>cast(value), (UpdatableArray[]) storage);
        }

        public final void set(long index, E value) {
            setElement(index, value);
        }

        public UpdatableArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (count < 0) {
                throw new IllegalArgumentException("Negative number of stored elements (" + count + ")");
            }
            if (arrayPos < 0) {
                throw rangeException(arrayPos);
            }
            if (arrayPos > length - count) {
                throw rangeException(arrayPos + count - 1);
            }
            E[] src = InternalUtils.cast(srcArray);
            if (combiner instanceof BufferedCombiner<?>) {
                ((BufferedCombiner<E>) combiner).set(arrayPos, src, srcArrayOffset, count, (UpdatableArray[]) storage);
            } else {
                setDataInternal(arrayPos, src, srcArrayOffset, count);
            }
            return this;
        }

        public UpdatableArray setData(long arrayPos, Object srcArray) {
            Objects.requireNonNull(srcArray, "Null srcArray argument");
            if (arrayPos < 0 || arrayPos > length) {
                throw rangeException(arrayPos);
            }
            int count = java.lang.reflect.Array.getLength(srcArray);
            if (count > length - arrayPos) {
                count = (int) (length - arrayPos);
            }
            E[] src = InternalUtils.cast(srcArray);
            if (combiner instanceof BufferedCombiner<?>) {
                ((BufferedCombiner<E>) combiner).set(arrayPos, src, 0, count, (UpdatableArray[]) storage);
            } else {
                setDataInternal(arrayPos, src, 0, count);
            }
            return this;
        }

        private void setDataInternal(long arrayPos, E[] src, int srcArrayOffset, int count) {
            UpdatableArray[] stor = (UpdatableArray[]) storage;
            for (int k = srcArrayOffset, kMax = srcArrayOffset + count; k < kMax; k++, arrayPos++) {
                if (src[k] != null && !elementType.isAssignableFrom(src[k].getClass())) {
                    throw new ClassCastException("Invalid type of setElement argument");
                }
                combiner.set(arrayPos, src[k], stor);
            }
        }

        public final void copy(long destIndex, long srcIndex) {
            if (srcIndex < 0 || srcIndex >= length) {
                throw rangeException(srcIndex);
            }
            if (destIndex < 0 || destIndex >= length) {
                throw rangeException(destIndex);
            }
            UpdatableArray[] stor = (UpdatableArray[]) storage;
            if (allNumbersAre1) {
                for (UpdatableArray s : stor) {
                    s.copy(destIndex, srcIndex);
                }
            } else {
                for (int k = 0; k < storage.length; k++) {
                    int ne = numbersOfElements[k];
                    stor[k].copy(destIndex * ne, srcIndex * ne, ne);
                }
            }
        }

        public final void copy(long destIndex, long srcIndex, long count) {
            if (count < 0) {
                throw new IndexOutOfBoundsException("Negative number of copied elements (count = " + count
                        + ") in " + getClass());
            }
            if (srcIndex < 0) {
                throw rangeException(srcIndex);
            }
            if (srcIndex > length - count) {
                throw rangeException(srcIndex + count - 1);
            }
            if (destIndex < 0) {
                throw rangeException(destIndex);
            }
            if (destIndex > length - count) {
                throw rangeException(destIndex + count - 1);
            }
            UpdatableArray[] stor = (UpdatableArray[]) storage;
            if (allNumbersAre1) {
                for (UpdatableArray s : stor) {
                    s.copy(destIndex, srcIndex, count);
                }
            } else {
                for (int k = 0; k < storage.length; k++) {
                    int ne = numbersOfElements[k];
                    stor[k].copy(destIndex * ne, srcIndex * ne, count * ne);
                }
            }
        }

        public final void swap(long firstIndex, long secondIndex) {
            if (firstIndex < 0 || firstIndex >= length) {
                throw rangeException(firstIndex);
            }
            if (secondIndex < 0 || secondIndex >= length) {
                throw rangeException(secondIndex);
            }
            UpdatableArray[] stor = (UpdatableArray[]) storage;
            if (allNumbersAre1) {
                for (int k = 0; k < storage.length; k++) {
                    stor[k].swap(firstIndex, secondIndex);
                }
            } else {
                for (int k = 0; k < storage.length; k++) {
                    int ne = numbersOfElements[k];
                    stor[k].swap(firstIndex * ne, secondIndex * ne, ne);
                }
            }
        }

        public final void swap(long firstIndex, long secondIndex, long count) {
            if (count < 0) {
                throw new IndexOutOfBoundsException("Negative number of swapped elements (count = " + count
                        + ") in " + getClass());
            }
            if (firstIndex < 0) {
                throw rangeException(firstIndex);
            }
            if (firstIndex > length - count) {
                throw rangeException(firstIndex + count - 1);
            }
            if (secondIndex < 0) {
                throw rangeException(secondIndex);
            }
            if (secondIndex > length - count) {
                throw rangeException(secondIndex + count - 1);
            }
            UpdatableArray[] stor = (UpdatableArray[]) storage;
            if (allNumbersAre1) {
                for (int k = 0; k < storage.length; k++) {
                    stor[k].swap(firstIndex, secondIndex, count);
                }
            } else {
                for (int k = 0; k < storage.length; k++) {
                    int ne = numbersOfElements[k];
                    stor[k].swap(firstIndex * ne, secondIndex * ne, count * ne);
                }
            }
        }

        public UpdatableArray copy(Array src) {
            if (src instanceof CombinedArray<?> a && a.combiner == this.combiner) {
                AbstractArray.checkCopyArguments(this, src);
                for (int k = 0; k < storage.length; k++) {
                    ((UpdatableArray[]) storage)[k].copy(a.storage[k]);
                }
            } else {
                defaultCopy(this, src);
            }
            return this;
        }

        public UpdatableArray swap(UpdatableArray another) {
            if (another instanceof UpdatableCombinedArray<?> a && a.combiner == this.combiner) {
                AbstractArray.checkSwapArguments(this, another);
                for (int k = 0; k < storage.length; k++) {
                    ((UpdatableArray) storage[k]).swap((UpdatableArray) a.storage[k]);
                }
            } else {
                defaultSwap(this, another);
            }
            return this;
        }

        public UpdatableObjectArray<E> fill(Object value) {
            copy(Arrays.nObjectCopies(length, value, elementType));
            return this;
        }

        public UpdatableObjectArray<E> fill(long position, long count, Object value) {
            subArr(position, count).copy(Arrays.nObjectCopies(count, value, elementType));
            return this;
        }

        public UpdatableObjectArray<E> subArray(long fromIndex, long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            UpdatableArray[] stor = new UpdatableArray[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = ((UpdatableArray) storage[k]).subArray(
                        fromIndex * numbersOfElements[k], toIndex * numbersOfElements[k]);
            }
            return new UpdatableCombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public UpdatableObjectArray<E> subArr(long position, long count) {
            checkSubArrArguments(position, count);
            UpdatableArray[] stor = new UpdatableArray[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = ((UpdatableArray) storage[k]).subArr(
                        position * numbersOfElements[k], count * numbersOfElements[k]);
            }
            return new UpdatableCombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public UpdatableArray asCopyOnNextWrite() {
            UpdatableArray[] stor = new UpdatableArray[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = ((UpdatableArray[]) storage)[k].asCopyOnNextWrite();
            }
            return new UpdatableCombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public UpdatableObjectArray<E> asUnresizable() {
            return this;
        }

        public void setNonNew() {
            setNewStatus(false); // not necessary in current implementation
        }

        public UpdatableObjectArray<E> shallowClone() {
            UpdatableCombinedArray<E> result = InternalUtils.cast(super.standardObjectClone());
            // not call super.clone() here! super.clone may be is already overridden!
            result.storage = shallowCloneArrays(storage);
            return result;
        }

        public <D> UpdatableObjectArray<D> cast(Class<D> elementType) {
            return InternalUtils.cast(super.cast(elementType));
        }

        public String toString() {
            return "unresizable combined array " + elementType.getName()
                    + "[" + length() + "], capacity " + capacity();
        }
    }

    static class MutableCombinedArray<E> extends UpdatableCombinedArray<E> implements MutableObjectArray<E> {
        MutableCombinedArray(Class<E> elementType, MutableArray[] storage, Combiner<E> combiner) {
            super(elementType, storage, combiner);
        }

        MutableCombinedArray(Class<E> elementType, long initialCapacity, long initialLength, Combiner<E> combiner) {
            super(elementType, initialCapacity, initialLength, combiner);
        }

        public MutableObjectArray<E> length(long newLength) {
            if (newLength != this.length) {
                for (int k = 0; k < storage.length; k++) {
                    InternalUtils.longMulAndException(newLength, numbersOfElements[k]); // check possible overflows
                }
                for (int k = 0; k < storage.length; k++) {
                    ((MutableArray[]) storage)[k].length(newLength * numbersOfElements[k]);
                }
                this.length = newLength;
                recalculateCapacity();
            }
            return this;
        }

        public MutableObjectArray<E> ensureCapacity(long minCapacity) {
            if (minCapacity > capacity()) {
                for (int k = 0; k < storage.length; k++) {
                    InternalUtils.longMulAndException(minCapacity, numbersOfElements[k]); // check possible overflows
                }
                for (int k = 0; k < storage.length; k++) {
                    ((MutableArray[]) storage)[k].ensureCapacity(minCapacity * numbersOfElements[k]);
                }
                recalculateCapacity();
            }
            return this;
        }

        public MutableObjectArray<E> trim() {
            if (this.length < capacity()) {
                for (int k = 0; k < storage.length; k++) {
                    ((MutableArray[]) storage)[k].trim();
                }
                recalculateCapacity();
            }
            return this;
        }

        public MutableObjectArray<E> append(Array appendedArray) {
            defaultAppend(this, appendedArray);
            return this;
        }

        public final Object popElement() {
            return pop();
        }

        public final E pop() {
            long index = length() - 1;
            if (index < 0) {
                throw new EmptyStackException();
            }
            E result = get(index);
            length(index);
            return result;
        }

        public final void pushElement(Object value) {
            long index = length();
            if (index == Long.MAX_VALUE) {
                throw new TooLargeArrayException("Too large desired array length (>Long.MAX_VALUE)");
            }
            length(index + 1);
            setElement(index, value);
        }

        public final void push(E value) {
            pushElement(value);
        }

        public void removeTop() {
            long index = length() - 1;
            if (index < 0) {
                throw new EmptyStackException();
            }
            length(index);
        }

        public MutableObjectArray<E> setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            super.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        public MutableObjectArray<E> setData(long arrayPos, Object srcArray) {
            super.setData(arrayPos, srcArray);
            return this;
        }

        public MutableObjectArray<E> copy(Array src) {
            super.copy(src);
            return this;
        }

        public MutableObjectArray<E> swap(UpdatableArray another) {
            super.swap(another);
            return this;
        }

        public boolean isUnresizable() {
            return false;
        }

        public MutableObjectArray<E> asCopyOnNextWrite() {
            MutableArray[] stor = (MutableArray[]) storage.clone();
            for (int k = 0; k < storage.length; k++) {
                stor[k] = stor[k].asCopyOnNextWrite();
                // it is necessary to avoid future changes in the result storage via CombinedMemoryModel.storage method
            }
            return new MutableCombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public UpdatableObjectArray<E> asUnresizable() {
            UpdatableArray[] stor = new UpdatableArray[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = ((MutableArray[]) storage)[k].asUnresizable();
                // it is necessary to avoid future changes in the result storage via CombinedMemoryModel.storage method
            }
            return new UpdatableCombinedArray<E>(this.elementType, stor, this.combiner);
        }

        public MutableObjectArray<E> shallowClone() {
            return InternalUtils.cast(super.shallowClone());
        }

        public <D> MutableObjectArray<D> cast(Class<D> elementType) {
            return InternalUtils.cast(super.cast(elementType));
        }

        public String toString() {
            return "mutable combined array " + elementType.getName()
                    + "[" + length() + "], capacity " + capacity();
        }
    }

    private static final class CombinedInPlaceArray<E>
            extends CombinedArray<E> implements ObjectInPlaceArray<E> {
        CombinedInPlaceArray(
                Class<E> elementType, Array[] storage,
                CombinerInPlace<E> combiner) {
            super(elementType, storage, combiner);
        }

        CombinedInPlaceArray(
                Class<E> elementType, long initialCapacity, long initialLength,
                CombinerInPlace<E> combiner) {
            super(elementType, initialCapacity, initialLength, combiner);
        }

        public long indexOf(long lowIndex, long highIndex, E value) {
            if (value == null) {
                return super.indexOf(lowIndex, highIndex, value);
            } else {
                long k = Math.max(lowIndex, 0);
                long n = Math.min(length(), highIndex);
                if (k >= n) {
                    return -1;
                }
                E e = allocateElement();
                for (; k < n; k++) {
                    if (value.equals(getInPlace(k, e))) {
                        return k;
                    }
                }
            }
            return -1;
        }

        public long lastIndexOf(long lowIndex, long highIndex, E value) {
            if (value == null) {
                return super.lastIndexOf(lowIndex, highIndex, value);
            } else {
                long k = Math.min(length(), highIndex);
                // warning: highIndex-1 can be invalid value Long.MAX_VALUE
                long low = Math.max(lowIndex, 0);
                if (k <= low) {
                    return -1;
                }
                E e = allocateElement();
                for (; k > low; k--) {
                    if (value.equals(getInPlace(k, e))) {
                        return k;
                    }
                }
            }
            return -1;
        }

        public E allocateElement() {
            return ((CombinerInPlace<E>) combiner).allocateElement();
        }

        public E getInPlace(long index, Object resultValue) {
            E resValue = InternalUtils.<E>cast(resultValue);
            ((CombinerInPlace<E>) combiner).getInPlace(index, resValue, storage);
            return resValue;
        }
    }

    private static final class UpdatableCombinedInPlaceArray<E>
            extends UpdatableCombinedArray<E> implements UpdatableObjectInPlaceArray<E> {
        UpdatableCombinedInPlaceArray(
                Class<E> elementType, UpdatableArray[] storage,
                CombinerInPlace<E> combiner) {
            super(elementType, storage, combiner);
        }

        UpdatableCombinedInPlaceArray(
                Class<E> elementType, long initialCapacity, long initialLength,
                CombinerInPlace<E> combiner) {
            super(elementType, initialCapacity, initialLength, combiner);
        }

        public E allocateElement() {
            return ((CombinerInPlace<E>) combiner).allocateElement();
        }

        public E getInPlace(long index, Object resultValue) {
            E resValue = InternalUtils.<E>cast(resultValue);
            ((CombinerInPlace<E>) combiner).getInPlace(index, resValue, storage);
            return resValue;
        }

        public UpdatableObjectInPlaceArray<E> subArray(long fromIndex, long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            UpdatableArray[] stor = new UpdatableArray[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = ((UpdatableArray) storage[k]).subArray(
                        fromIndex * numbersOfElements[k], toIndex * numbersOfElements[k]);
            }
            return new UpdatableCombinedInPlaceArray<E>(this.elementType, stor, (CombinerInPlace<E>) this.combiner);
        }

        public UpdatableObjectInPlaceArray<E> subArr(long position, long count) {
            checkSubArrArguments(position, count);
            UpdatableArray[] stor = new UpdatableArray[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = ((UpdatableArray) storage[k]).subArr(
                        position * numbersOfElements[k], count * numbersOfElements[k]);
            }
            return new UpdatableCombinedInPlaceArray<E>(this.elementType, stor, (CombinerInPlace<E>) this.combiner);
        }

        public UpdatableObjectInPlaceArray<E> asUnresizable() {
            return this;
        }
    }

    private static final class MutableCombinedInPlaceArray<E>
            extends MutableCombinedArray<E> implements MutableObjectInPlaceArray<E> {
        MutableCombinedInPlaceArray(
                Class<E> elementType, MutableArray[] storage,
                CombinerInPlace<E> combiner) {
            super(elementType, storage, combiner);
        }

        MutableCombinedInPlaceArray(
                Class<E> elementType, long initialCapacity, long initialLength,
                CombinerInPlace<E> combiner) {
            super(elementType, initialCapacity, initialLength, combiner);
        }

        public E allocateElement() {
            return ((CombinerInPlace<E>) combiner).allocateElement();
        }

        public E getInPlace(long index, Object resultValue) {
            E resValue = InternalUtils.<E>cast(resultValue);
            ((CombinerInPlace<E>) combiner).getInPlace(index, resValue, storage);
            return resValue;
        }

        public MutableObjectInPlaceArray<E> setData(long arrayPos, Object srcArray, int srcArrayOffset, int count) {
            super.setData(arrayPos, srcArray, srcArrayOffset, count);
            return this;
        }

        public MutableObjectInPlaceArray<E> setData(long arrayPos, Object srcArray) {
            super.setData(arrayPos, srcArray);
            return this;
        }

        public MutableObjectInPlaceArray<E> copy(Array src) {
            super.copy(src);
            return this;
        }

        public MutableObjectInPlaceArray<E> swap(UpdatableArray another) {
            super.swap(another);
            return this;
        }

        public MutableObjectInPlaceArray<E> length(long newLength) {
            super.length(newLength);
            return this;
        }

        public MutableObjectInPlaceArray<E> ensureCapacity(long minCapacity) {
            super.ensureCapacity(minCapacity);
            return this;
        }

        public MutableObjectInPlaceArray<E> trim() {
            super.trim();
            return this;
        }

        public MutableObjectInPlaceArray<E> append(Array appendedArray) {
            super.append(appendedArray);
            return this;
        }

        public UpdatableObjectInPlaceArray<E> subArray(long fromIndex, long toIndex) {
            checkSubArrayArguments(fromIndex, toIndex);
            UpdatableArray[] stor = new UpdatableArray[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = ((UpdatableArray) storage[k]).subArray(
                        fromIndex * numbersOfElements[k], toIndex * numbersOfElements[k]);
            }
            return new UpdatableCombinedInPlaceArray<E>(this.elementType, stor, (CombinerInPlace<E>) this.combiner);
        }

        public UpdatableObjectInPlaceArray<E> subArr(long position, long count) {
            checkSubArrArguments(position, count);
            UpdatableArray[] stor = new UpdatableArray[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = ((UpdatableArray) storage[k]).subArr(
                        position * numbersOfElements[k], count * numbersOfElements[k]);
            }
            return new UpdatableCombinedInPlaceArray<E>(this.elementType, stor, (CombinerInPlace<E>) this.combiner);
        }

        public MutableObjectInPlaceArray<E> asCopyOnNextWrite() {
            MutableArray[] stor = (MutableArray[]) storage.clone();
            for (int k = 0; k < storage.length; k++) {
                stor[k] = stor[k].asCopyOnNextWrite();
                // it is necessary to avoid future changes in the result storage via CombinedMemoryModel.storage method
            }
            return new MutableCombinedInPlaceArray<E>(this.elementType, stor, (CombinerInPlace<E>) this.combiner);
        }

        public UpdatableObjectInPlaceArray<E> asUnresizable() {
            UpdatableArray[] stor = new UpdatableArray[storage.length];
            for (int k = 0; k < storage.length; k++) {
                stor[k] = ((MutableArray[]) storage)[k].asUnresizable();
                // it is necessary to avoid future changes in the result storage via CombinedMemoryModel.storage method
            }
            return new UpdatableCombinedInPlaceArray<E>(this.elementType, stor, (CombinerInPlace<E>) this.combiner);
        }

        public MutableObjectInPlaceArray<E> shallowClone() {
            return InternalUtils.cast(super.shallowClone());
        }
    }

    private static Array[] shallowCloneArrays(Array[] src) {
        Objects.requireNonNull(src, "Cannot make shallow clones of arrays: null Array[] src");
        Array[] result = src.clone(); // preserves the type of src elements
        for (int k = 0; k < result.length; k++) {
            Objects.requireNonNull(src[k], "Cannot make shallow clones of arrays: array #" + k + " is null");
            result[k] = src[k].shallowClone();
        }
        return result;
    }
}
