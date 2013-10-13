package net.algart.arrays;

/**
 * <p>The degenerate memory model that does not allow to create any AlgART arrays.
 * Any attempt to create an array by methods of this class leads to
 * {@link UnsupportedElementTypeException} (or, maybe, <tt>NullPointerException</tt>,
 * <tt>IllegalArgumentException</tt> or <tt>ClassCastException</tt>,
 * if the comments to the method require this).
 *
 * <p>This class may be useful to detect a bug, when some method have a {@link MemoryModel} argument,
 * but you are sure that this argument must not be used for creating any AlgART arrays.</p>
 *
 * <p>This class is <b>immutable</b> and <b>thread-safe</b>:
 * there are no ways to modify settings of its instance returned by {@link #getInstance()} method.
 * Moreover, it is a <b>singleton</b>: {@link #getInstance()} always returns the same object.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class SignalMemoryModel extends AbstractMemoryModel {
    private static final SignalMemoryModel INSTANCE = new SignalMemoryModel();

    private SignalMemoryModel() {
    }

    /**
     * Returns an instance of this memory model.
     *
     * @return an instance of this memory model.
     */
    public static SignalMemoryModel getInstance() {
        return INSTANCE;
    }

    public MutableArray newEmptyArray(Class<?> elementType) {
        return newEmptyArray(elementType, 157);
    }

    public MutableArray newEmptyArray(Class<?> elementType, long initialCapacity) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (initialCapacity < 0)
            throw new IllegalArgumentException("Negative initial capacity");
        throw new UnsupportedElementTypeException("SignalMemoryModel cannot create any AlgART arrays");
    }

    public MutableArray newArray(Class<?> elementType, long initialLength) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (initialLength < 0)
            throw new IllegalArgumentException("Negative initial length");
        throw new UnsupportedElementTypeException("SignalMemoryModel cannot create any AlgART arrays");
    }

    public UpdatableArray newUnresizableArray(Class<?> elementType, long length) {
        Arrays.checkElementTypeForNullAndVoid(elementType);
        if (length < 0)
            throw new IllegalArgumentException("Negative array length");
        throw new UnsupportedElementTypeException("SignalMemoryModel cannot create any AlgART arrays");
    }

    /**
     * This implementation always returns <tt>false</tt>.
     *
     * @param elementType the type of array elements.
     * @return            <tt>false</tt> always.
     * @throws NullPointerException if <tt>elementType</tt> is <tt>null</tt>.
     */
    public boolean isElementTypeSupported(Class<?> elementType) {
        if (elementType == null)
            throw new NullPointerException("Null elementType argument");
        return false;
    }

    /**
     * This implementation always returns <tt>false</tt>.
     *
     * @return <tt>false</tt> always.
     */
    public boolean areAllPrimitiveElementTypesSupported() {
        return false;
    }

    /**
     * This implementation always returns <tt>false</tt>.
     *
     * @return <tt>false</tt> always.
     */
    public boolean areAllElementTypesSupported() {
        return false;
    }

    /**
     * This implementation always returns <tt>-1</tt>.
     *
     * @param elementType the type of array elements.
     * @return            <tt>-1</tt> always.
     * @throws NullPointerException if <tt>elementType</tt> is <tt>null</tt>.
     */
    public long maxSupportedLength(Class<?> elementType) {
        if (elementType == null)
            throw new NullPointerException("Null elementType argument");
        return -1;
    }

    /**
     * This implementation always returns <tt>false</tt>.
     *
     * @return <tt>false</tt> always.
     */
    public boolean isCreatedBy(Array array) {
        return false;
    }
}
