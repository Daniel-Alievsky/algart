package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, FloatStack.java, all)
  Float(?!ing) ==> Long ;;
  float ==> long
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Stack of <tt>long</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface LongStack extends Stack {
    /**
     * Removes the element at the top of this stack and returns its value,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     *
     * <p>It this object is an AlgART long array, implementing {@link MutableLongArray} interface,
     * and it is not empty, the same action may be performed by the following code:</p>
     * <pre>
     * long result = longArray.{@link LongArray#getLong(long) getLong}(longArray.{@link #length() length()}-1);
     * longArray.{@link MutableArray#length(long) length}(longArray.{@link #length() length()}-1);
     * </pre>
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    public long popLong();

    /**
     * Appends <tt>value</tt> element to the end of this stack.
     *
     * <p>It this object is an AlgART long array, implementing {@link MutableLongArray} interface,
     * the same action may be performed by the following code:</p>
     * <pre>
     * longArray.{@link MutableLongArray#length(long) length}(longArray.{@link #length() length()}+1);
     * longArray.{@link UpdatableLongArray#setLong(long, long)
     * setLong}(longArray.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this stack.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    public void pushLong(long value);
/*Repeat.IncludeEnd*/
}
