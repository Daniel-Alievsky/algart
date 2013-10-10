package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, FloatStack.java, all)
  Float(Array|Stack) ==> Bit$1 ;;
  setFloat ==> setBit ;;
  getFloat ==> getBit ;;
  pushFloat ==> pushBit ;;
  popFloat ==> popBit ;;
  Float(?!ing) ==> Boolean ;;
  float ==> boolean
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Stack of <tt>boolean</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface BitStack extends Stack {
    /**
     * Removes the element at the top of this stack and returns its value,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     *
     * <p>It this object is an AlgART boolean array, implementing {@link MutableBitArray} interface,
     * and it is not empty, the same action may be performed by the following code:</p>
     * <pre>
     * boolean result = booleanArray.{@link BitArray#getBit(long) getBit}(booleanArray.{@link #length() length()}-1);
     * booleanArray.{@link MutableArray#length(long) length}(booleanArray.{@link #length() length()}-1);
     * </pre>
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    public boolean popBit();

    /**
     * Appends <tt>value</tt> element to the end of this stack.
     *
     * <p>It this object is an AlgART boolean array, implementing {@link MutableBitArray} interface,
     * the same action may be performed by the following code:</p>
     * <pre>
     * booleanArray.{@link MutableBitArray#length(long) length}(booleanArray.{@link #length() length()}+1);
     * booleanArray.{@link UpdatableBitArray#setBit(long, boolean)
     * setBit}(booleanArray.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this stack.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    public void pushBit(boolean value);
/*Repeat.IncludeEnd*/
}
