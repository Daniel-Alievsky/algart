package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, DataFloatBuffer.java, all)
  Float ==> Bit ;;
  <tt>float</tt>\s+elements ==> bit elements ;;
  float ==> long
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Data buffer for bit elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DataBitBuffer extends DataBuffer {
    public DataBitBuffer map(long position);

    public DataBitBuffer map(long position, boolean readData);

    public DataBitBuffer mapNext();

    public DataBitBuffer mapNext(boolean readData);

    public DataBitBuffer map(long position, long maxCount);

    public DataBitBuffer map(long position, long maxCount, boolean readData);

    public DataBitBuffer force();

    public long[] data();
}
/*Repeat.IncludeEnd*/
