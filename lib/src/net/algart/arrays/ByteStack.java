package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, FloatStack.java, all)
  Float(?!ing) ==> Byte ;;
  float ==> byte
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Stack of <tt>byte</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ByteStack extends Stack {
    /**
     * Removes the element at the top of this stack and returns its value,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     *
     * <p>It this object is an AlgART byte array, implementing {@link MutableByteArray} interface,
     * and it is not empty, the same action may be performed by the following code:</p>
     * <pre>
     * byte result = byteArray.{@link ByteArray#getByte(long) getByte}(byteArray.{@link #length() length()}-1);
     * byteArray.{@link MutableArray#length(long) length}(byteArray.{@link #length() length()}-1);
     * </pre>
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    public byte popByte();

    /**
     * Appends <tt>value</tt> element to the end of this stack.
     *
     * <p>It this object is an AlgART byte array, implementing {@link MutableByteArray} interface,
     * the same action may be performed by the following code:</p>
     * <pre>
     * byteArray.{@link MutableByteArray#length(long) length}(byteArray.{@link #length() length()}+1);
     * byteArray.{@link UpdatableByteArray#setByte(long, byte)
     * setByte}(byteArray.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this stack.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    public void pushByte(byte value);
/*Repeat.IncludeEnd*/
}
