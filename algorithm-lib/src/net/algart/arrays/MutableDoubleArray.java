package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, MutableFloatArray.java, all)
  Float(?!ing) ==> Double ;;
  float ==> double
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Resizable AlgART array of <tt>double</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface MutableDoubleArray extends DoubleStack, UpdatableDoubleArray, MutablePFloatingArray {
    public MutableDoubleArray setData(long arrayPos, Object srcArray, int srcArrayOffset, int count);

    public MutableDoubleArray setData(long arrayPos, Object srcArray);

    public MutableDoubleArray copy(Array src);

    public MutableDoubleArray swap(UpdatableArray another);

    public MutableDoubleArray length(long newLength);

    public MutableDoubleArray ensureCapacity(long minCapacity);

    public MutableDoubleArray trim();

    public MutableDoubleArray append(Array appendedArray);

    public MutableDoubleArray asCopyOnNextWrite();

    public MutableDoubleArray shallowClone();
    /*Repeat.IncludeEnd*/
}
