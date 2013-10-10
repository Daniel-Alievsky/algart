package net.algart.arrays;

/*Repeat(INCLUDE_FROM_FILE, DataFloatBuffer.java, all)
  Float ==> Byte ;;
  float ==> byte
     !! Auto-generated: NOT EDIT !! */
/**
 * <p>Data buffer for <tt>byte</tt> elements.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface DataByteBuffer extends DataBuffer {
    public DataByteBuffer map(long position);

    public DataByteBuffer map(long position, boolean readData);

    public DataByteBuffer mapNext();

    public DataByteBuffer mapNext(boolean readData);

    public DataByteBuffer map(long position, long maxCount);

    public DataByteBuffer map(long position, long maxCount, boolean readData);

    public DataByteBuffer force();

    public byte[] data();
}
/*Repeat.IncludeEnd*/
