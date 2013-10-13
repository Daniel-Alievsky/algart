package net.algart.arrays;

/**
 * <p>Stack of some objects (non-primitive values).</p>
 *
 * <p>Any class implementing this interface <b>must</b> contain non-primitive elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ObjectStack<E> extends Stack {
    /**
     * Equivalent to {@link #popElement()}.
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    public E pop();

    /**
     * Equivalent to {@link #pushElement(Object) pushElement(value)}.
     *
     * @param value to be added to the top of this stack.
     * @throws ArrayStoreException    if <tt>value</tt> is notan instance of the class of stack elements.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    public void push(E value);

}
