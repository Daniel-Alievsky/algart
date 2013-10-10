package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, FloatArray.java, all)
  PFloating ==> PFixed ;;
  Float(?!ing) ==> Char ;;
  float ==> char
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>AlgART array of <tt>char</tt> values, read-only access.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface CharArray extends PFixedArray {
    public Class<? extends CharArray> type();

    public Class<? extends UpdatableCharArray> updatableType();

    public Class<? extends MutableCharArray> mutableType();

    /**
     * Returns the element #<tt>index</tt>.
     *
     * @param index index of element to get.
     * @return      the element at the specified position in this array.
     * @throws IndexOutOfBoundsException if <tt>index</tt> is out of range <tt>0..length()-1</tt>.
     */
    public char getChar(long index);

    /**
     * Returns the minimal index <tt>k</tt>, so that
     * <tt>lowIndex&lt;=k&lt;min(highIndex,thisArray.{@link #length() length()})</tt>
     * and <tt>{@link #getChar(long) getChar}(k)==value</tt>,
     * or <tt>-1</tt> if there is no such array element.
     *
     * <p>In particular, if <tt>lowIndex&gt;=thisArray.{@link #length() length()}}</tt>
     * or <tt>lowIndex&gt;=highIndex</tt>, this method returns <tt>-1</tt>,
     * and if <tt>lowIndex&lt;0</tt>, the result is the same as if <tt>lowIndex==0</tt>.
     *
     * <p>You may specify <tt>lowIndex=0</tt> and <tt>highIndex=Long.MAX_VALUE</tt> to search
     * through all array elements.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the first occurrence of this value in this array
     *                  in range <tt>lowIndex&lt;=index&lt;highIndex</tt>,
     *                  or <tt>-1</tt> if this value does not occur in this range.
     */
    public long indexOf(long lowIndex, long highIndex, char value);

    /**
     * Returns the maximal index <tt>k</tt>, so that <tt>highIndex&gt;k&gt;=max(lowIndex,0)</tt>
     * and <tt>{@link #getChar(long) getChar}(k)==value</tt>,
     * or <tt>-1</tt> if there is no such array element.
     *
     * <p>In particular, if <tt>highIndex&lt;=0</tt> or <tt>highIndex&lt;=lowIndex</tt>,
     * this method returns <tt>-1</tt>,
     * and if <tt>highIndex&gt;=thisArray.{@link #length() length()}</tt>,
     * the result is the same as if <tt>highIndex==thisArray.{@link #length() length()}</tt>.
     *
     * <p>You may specify <tt>lowIndex=0</tt> and <tt>highIndex=Long.MAX_VALUE</tt> to search
     * through all array elements.
     *
     * @param lowIndex  the low index in the array for search (inclusive).
     * @param highIndex the high index in the array for search (exclusive).
     * @param value     the value to be found.
     * @return          the index of the last occurrence of this value in this array
     *                  in range <tt>lowIndex&lt;=index&lt;highIndex</tt>,
     *                  or <tt>-1</tt> if this value does not occur in this range.
     */
    public long lastIndexOf(long lowIndex, long highIndex, char value);

    public DataCharBuffer buffer(DataBuffer.AccessMode mode, long capacity);

    public DataCharBuffer buffer(DataBuffer.AccessMode mode);

    public DataCharBuffer buffer(long capacity);

    public DataCharBuffer buffer();

    public CharArray asImmutable();

    public CharArray asTrustedImmutable();

    public MutableCharArray mutableClone(MemoryModel memoryModel);

    public UpdatableCharArray updatableClone(MemoryModel memoryModel);
    /*Repeat.IncludeEnd*/
}
