package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, DataFloatBuffer.java, all)
  Float ==> Short ;;
  float ==> short
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Data buffer for <tt>short</tt> elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DataShortBuffer extends DataBuffer {
    public DataShortBuffer map(long position);

    public DataShortBuffer map(long position, boolean readData);

    public DataShortBuffer mapNext();

    public DataShortBuffer mapNext(boolean readData);

    public DataShortBuffer map(long position, long maxCount);

    public DataShortBuffer map(long position, long maxCount, boolean readData);

    public DataShortBuffer force();

    public short[] data();
}
/*Repeat.IncludeEnd*/
