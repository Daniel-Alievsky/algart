package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, FloatStack.java, all)
  Float(?!ing) ==> Short ;;
  float ==> short
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Stack of <tt>short</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ShortStack extends Stack {
    /**
     * Removes the element at the top of this stack and returns its value,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     *
     * <p>It this object is an AlgART short array, implementing {@link MutableShortArray} interface,
     * and it is not empty, the same action may be performed by the following code:</p>
     * <pre>
     * short result = shortArray.{@link ShortArray#getShort(long) getShort}(shortArray.{@link #length() length()}-1);
     * shortArray.{@link MutableArray#length(long) length}(shortArray.{@link #length() length()}-1);
     * </pre>
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    public short popShort();

    /**
     * Appends <tt>value</tt> element to the end of this stack.
     *
     * <p>It this object is an AlgART short array, implementing {@link MutableShortArray} interface,
     * the same action may be performed by the following code:</p>
     * <pre>
     * shortArray.{@link MutableShortArray#length(long) length}(shortArray.{@link #length() length()}+1);
     * shortArray.{@link UpdatableShortArray#setShort(long, short)
     * setShort}(shortArray.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this stack.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    public void pushShort(short value);
/*Repeat.IncludeEnd*/
}
