package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, FloatStack.java, all)
  Float(?!ing) ==> Double ;;
  float ==> double
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Stack of <tt>double</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DoubleStack extends Stack {
    /**
     * Removes the element at the top of this stack and returns its value,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     *
     * <p>It this object is an AlgART double array, implementing {@link MutableDoubleArray} interface,
     * and it is not empty, the same action may be performed by the following code:</p>
     * <pre>
     * double result = doubleArray.{@link DoubleArray#getDouble(long) getDouble}(doubleArray.{@link #length() length()}-1);
     * doubleArray.{@link MutableArray#length(long) length}(doubleArray.{@link #length() length()}-1);
     * </pre>
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    public double popDouble();

    /**
     * Appends <tt>value</tt> element to the end of this stack.
     *
     * <p>It this object is an AlgART double array, implementing {@link MutableDoubleArray} interface,
     * the same action may be performed by the following code:</p>
     * <pre>
     * doubleArray.{@link MutableDoubleArray#length(long) length}(doubleArray.{@link #length() length()}+1);
     * doubleArray.{@link UpdatableDoubleArray#setDouble(long, double)
     * setDouble}(doubleArray.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this stack.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    public void pushDouble(double value);
/*Repeat.IncludeEnd*/
}
