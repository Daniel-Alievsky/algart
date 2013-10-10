package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, FloatStack.java, all)
  Float(?!ing) ==> Int ;;
  float ==> int
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Stack of <tt>int</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface IntStack extends Stack {
    /**
     * Removes the element at the top of this stack and returns its value,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     *
     * <p>It this object is an AlgART int array, implementing {@link MutableIntArray} interface,
     * and it is not empty, the same action may be performed by the following code:</p>
     * <pre>
     * int result = intArray.{@link IntArray#getInt(long) getInt}(intArray.{@link #length() length()}-1);
     * intArray.{@link MutableArray#length(long) length}(intArray.{@link #length() length()}-1);
     * </pre>
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    public int popInt();

    /**
     * Appends <tt>value</tt> element to the end of this stack.
     *
     * <p>It this object is an AlgART int array, implementing {@link MutableIntArray} interface,
     * the same action may be performed by the following code:</p>
     * <pre>
     * intArray.{@link MutableIntArray#length(long) length}(intArray.{@link #length() length()}+1);
     * intArray.{@link UpdatableIntArray#setInt(long, int)
     * setInt}(intArray.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this stack.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    public void pushInt(int value);
/*Repeat.IncludeEnd*/
}
