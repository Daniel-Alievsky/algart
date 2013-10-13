package net.algart.contexts;

import net.algart.arrays.*;

/**
 * <p>The context informing the module, working with {@link net.algart.arrays AlgART arrays},
 * about the preferred {@link MemoryModel memory model}.</p>
 *
 * <p>Any module, that need to allocate AlgART arrays and that have no another information about
 * desired memory model for new arrays (for example, passed directly as an algorithm parameter),
 * should request this context to get the preferred memory model and use it. For example:</p>
 *
 * <pre>
 * public {@link FloatArray} calculateSomeData({@link Context} context, some-other-arguments...) {
 * &#32;   {@link ArrayMemoryContext} amc = context.{@link #as(Class) as}({@link ArrayMemoryContext}.class);
 * &#32;   // - EMPTY context returns the default memory model ({@link
 * net.algart.arrays.Arrays.SystemSettings#globalMemoryModel()})
 * &#32;   {@link MemoryModel} mm = amc.{@link #getMemoryModel()};
 * &#32;   {@link MutableFloatArray} result = mm.{@link MemoryModel#newEmptyFloatArray() newEmptyFloatArray()};
 *
 * &#32;   . . . // filling result array by some algorithm
 *
 * &#32;   return result;
 * }
 * </pre>
 *
 * <p>It allows an application to control how all AlgART arrays will be created by any modules,
 * "understanding" this context. For example, the application can require different algorithms
 * to create arrays by different instances of {@link LargeMemoryModel}, that allocates
 * temporary files in different disk directories.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ArrayMemoryContext extends Context {

    /**
     * Returns the {@link MemoryModel memory model} that should be used
     * for creating any instances of AlgART arrays.
     *
     * @return the desired memory model.
     */
    public MemoryModel getMemoryModel();

    /**
     * Returns the {@link MemoryModel memory model} that should be used
     * for creating any instances of AlgART arrays
     * with specified type of elements.
     * The typical implementation returns <tt>mm.{@link MemoryModel#isElementTypeSupported
     * isElementTypeSupported}(elementType) ? mm : {@link SimpleMemoryModel#getInstance()}</tt>,
     * where <tt>mm</tt> is the result of {@link #getMemoryModel()} method.
     *
     * @param elementType the required element type.
     * @return            the desired memory model.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public MemoryModel getMemoryModel(Class<?> elementType);

    /**
     * Returns the {@link MemoryModel memory model} that should be used
     * for creating any instances of AlgART arrays with some additional settings
     * (recommendations). Additional settings should be passed via
     * <tt>settings</tt> argument (usually in JSON or analogous format);
     * you can specify here some details about desired memory model.
     * The context may consider your recommendations, specified in this argument,
     * but also may ingnore them and return the same result as the
     * simple {@link #getMemoryModel()} method.
     *
     * <p>If <tt>settings</tt> is <tt>""</tt> (empty string), this methods
     * must be equivalent to {@link #getMemoryModel()}.
     *
     * @param settings additional desires about the required memory model.
     * @return         the desired memory model.
     * @throws NullPointerException if the argument is <tt>null</tt>.
     */
    public MemoryModel getMemoryModel(String settings);
}
