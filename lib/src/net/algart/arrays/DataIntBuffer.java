package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, DataFloatBuffer.java, all)
  Float ==> Int ;;
  float ==> int
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Data buffer for <tt>int</tt> elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DataIntBuffer extends DataBuffer {
    public DataIntBuffer map(long position);

    public DataIntBuffer map(long position, boolean readData);

    public DataIntBuffer mapNext();

    public DataIntBuffer mapNext(boolean readData);

    public DataIntBuffer map(long position, long maxCount);

    public DataIntBuffer map(long position, long maxCount, boolean readData);

    public DataIntBuffer force();

    public int[] data();
}
/*Repeat.IncludeEnd*/
