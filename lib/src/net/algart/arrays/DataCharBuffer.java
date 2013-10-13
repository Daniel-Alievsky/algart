package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, DataFloatBuffer.java, all)
  Float ==> Char ;;
  float ==> char
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Data buffer for <tt>char</tt> elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DataCharBuffer extends DataBuffer {
    public DataCharBuffer map(long position);

    public DataCharBuffer map(long position, boolean readData);

    public DataCharBuffer mapNext();

    public DataCharBuffer mapNext(boolean readData);

    public DataCharBuffer map(long position, long maxCount);

    public DataCharBuffer map(long position, long maxCount, boolean readData);

    public DataCharBuffer force();

    public char[] data();
}
/*Repeat.IncludeEnd*/
