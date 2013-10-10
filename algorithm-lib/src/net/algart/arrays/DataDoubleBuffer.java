package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, DataFloatBuffer.java, all)
  Float ==> Double ;;
  float ==> double
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Data buffer for <tt>double</tt> elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DataDoubleBuffer extends DataBuffer {
    public DataDoubleBuffer map(long position);

    public DataDoubleBuffer map(long position, boolean readData);

    public DataDoubleBuffer mapNext();

    public DataDoubleBuffer mapNext(boolean readData);

    public DataDoubleBuffer map(long position, long maxCount);

    public DataDoubleBuffer map(long position, long maxCount, boolean readData);

    public DataDoubleBuffer force();

    public double[] data();
}
/*Repeat.IncludeEnd*/
