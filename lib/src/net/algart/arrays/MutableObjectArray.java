package net.algart.arrays;

/**
 * <p>Resizable AlgART array of some objects (non-primitive values) with the specified generic type <tt>E</tt>,
 * read-write and resize access.</p
 *
 * <p>Any class implementing this interface <b>must</b> contain non-primitive elements
 * ({@link #elementType()} must not return a primitive type).</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableObjectArray<E> extends ObjectStack<E>, MutableArray, UpdatableObjectArray<E> {
    public <D> MutableObjectArray<D> cast(Class<D> elementType);

    /*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, resultTypes)
      Float(?!ing) ==> Object ;;
      float ==> Object ;;
      ObjectArray ==> ObjectArray<E>
         !! Auto-generated: NOT EDIT !! */
    public MutableObjectArray<E> setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableObjectArray<E> setData(long arrayPos, Object srcArray);

    public MutableObjectArray<E> copy(Array src);

    public MutableObjectArray<E> swap(UpdatableArray another);

    public MutableObjectArray<E> length(long newLength);

    public MutableObjectArray<E> ensureCapacity(long minCapacity);

    public MutableObjectArray<E> trim();

    public MutableObjectArray<E> append(Array appendedArray);

    public MutableObjectArray<E> asCopyOnNextWrite();

    public MutableObjectArray<E> shallowClone();
    /*Repeat.IncludeEnd*/
}
