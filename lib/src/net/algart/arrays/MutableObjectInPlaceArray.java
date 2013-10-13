package net.algart.arrays;

/**
 * <p>Special version of {@link MutableObjectArray} allowing
 * to load an element without creating new Java object.</p>
 *
 * <p>The arrays of object created via some {@link MemoryModel memory models} may not implement this interface.
 * You can check, is this interface implemented, by <tt>intstanceof</tt> operator.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableObjectInPlaceArray<E> extends UpdatableObjectInPlaceArray<E>, MutableObjectArray<E> {
    /*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, resultTypes)
      Float(?!ing) ==> Object ;;
      float ==> Object ;;
      ObjectArray ==> ObjectInPlaceArray<E>
         !! Auto-generated: NOT EDIT !! */
    public MutableObjectInPlaceArray<E> setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableObjectInPlaceArray<E> setData(long arrayPos, Object srcArray);

    public MutableObjectInPlaceArray<E> copy(Array src);

    public MutableObjectInPlaceArray<E> swap(UpdatableArray another);

    public MutableObjectInPlaceArray<E> length(long newLength);

    public MutableObjectInPlaceArray<E> ensureCapacity(long minCapacity);

    public MutableObjectInPlaceArray<E> trim();

    public MutableObjectInPlaceArray<E> append(Array appendedArray);

    public MutableObjectInPlaceArray<E> asCopyOnNextWrite();

    public MutableObjectInPlaceArray<E> shallowClone();
    /*Repeat.IncludeEnd*/
}
