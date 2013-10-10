package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, FloatStack.java, all)
  Float(?!ing) ==> Char ;;
  float ==> char
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Stack of <tt>char</tt> values.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface CharStack extends Stack {
    /**
     * Removes the element at the top of this stack and returns its value,
     * or throws <tt>EmptyStackException</tt> if the stack is empty.
     *
     * <p>It this object is an AlgART char array, implementing {@link MutableCharArray} interface,
     * and it is not empty, the same action may be performed by the following code:</p>
     * <pre>
     * char result = charArray.{@link CharArray#getChar(long) getChar}(charArray.{@link #length() length()}-1);
     * charArray.{@link MutableArray#length(long) length}(charArray.{@link #length() length()}-1);
     * </pre>
     *
     * @return the element at the top of this stack (it is removed from the stack by this method).
     * @throws java.util.EmptyStackException if this stack is empty.
     */
    public char popChar();

    /**
     * Appends <tt>value</tt> element to the end of this stack.
     *
     * <p>It this object is an AlgART char array, implementing {@link MutableCharArray} interface,
     * the same action may be performed by the following code:</p>
     * <pre>
     * charArray.{@link MutableCharArray#length(long) length}(charArray.{@link #length() length()}+1);
     * charArray.{@link UpdatableCharArray#setChar(long, char)
     * setChar}(charArray.{@link #length() length()}-1, value);
     * </pre>
     *
     * @param value to be added to the top of this stack.
     * @throws TooLargeArrayException if the resulting stack length is too large for this type of stacks.
     */
    public void pushChar(char value);
/*Repeat.IncludeEnd*/
}
