package net.algart.arrays;

/*Repeat.SectionStart all*/
/**
 * <p>Stack of <tt>float</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface FloatStack extends Stack {
    /**
     * Removes the element at the top of this stack and returns its value,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     *
     * <p>It this object is an AlgART float array, implementing {@link MutableFloatArray} interface,
     * and it is not empty, the same action may be performed by the following code:</p>
     * <pre>
     * float result = floatArray.{@link FloatArray#getFloat(long) getFloat}(floatArray.{@link #length() length()}-1);
     * floatArray.{@link MutableArray#length(long) length}(floatArray.{@link #length() length()}-1);
     * </pre>
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    public float popFloat();

    /**
     * Appends <tt>value</tt> element to the end of this stack.
     *
     * <p>It this object is an AlgART float array, implementing {@link MutableFloatArray} interface,
     * the same action may be performed by the following code:</p>
     * <pre>
     * floatArray.{@link MutableFloatArray#length(long) length}(floatArray.{@link #length() length()}+1);
     * floatArray.{@link UpdatableFloatArray#setFloat(long, float)
     * setFloat}(floatArray.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this stack.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    public void pushFloat(float value);
/*Repeat.SectionEnd all*/
}
