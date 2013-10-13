package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, DataFloatBuffer.java, all)
  Float ==> Long ;;
  float ==> long
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Data buffer for <tt>long</tt> elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DataLongBuffer extends DataBuffer {
    public DataLongBuffer map(long position);

    public DataLongBuffer map(long position, boolean readData);

    public DataLongBuffer mapNext();

    public DataLongBuffer mapNext(boolean readData);

    public DataLongBuffer map(long position, long maxCount);

    public DataLongBuffer map(long position, long maxCount, boolean readData);

    public DataLongBuffer force();

    public long[] data();
}
/*Repeat.IncludeEnd*/
