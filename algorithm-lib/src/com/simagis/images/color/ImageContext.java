package com.simagis.images.color;

import java.io.IOException;
import java.util.List;
import net.algart.contexts.*;
import net.algart.arrays.*;

/**
 * <p>The context informing the module, working with images supported by this package,
 * how to convert several {@link Matrix AlgART matrices} into such images.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface ImageContext extends Context {
    /**
     * Creates new instance of {@link Image2D}, consisting of the given matrices.
     * The <tt>rgbi</tt> list must contain 3, 4 or more matrices.
     * This method <i>must</i> process at least 3 first elements so that
     * the returned image will return <tt>rgbi.get(0)</tt>,
     * <tt>rgbi.get(1)</tt> and <tt>rgbi.get(2)</tt> elements
     * as its {@link Image2D#r()}, {@link Image2D#g()} and {@link Image2D#b()} components.
     * This method <i>may</i> use the 4th element <tt>rgbi.get(3)</tt>
     * as the intensity component returned by {@link Image2D#i()}.
     *
     * <p>The content of the passed matrices may be shared with the new instance.
     * However, the reference to the passed list must not be stored in the new instance.
     *
     * <p>The <tt>allocationContext</tt> argument must contain some context, that can be used while
     * performing calculations. It must not be <tt>null</tt>, but it is not required to implement {@link ImageContext}
     * interface. (For accessing {@link ImageContext} methods, an implementation of this method
     * can use <tt>this</tt> reference.) Usually this context is used for showing progress
     * and allowing interruption.</p>
     *
     * @param allocationContext the context used for creating new instance; usually can be identical to this one.
     * @param rgbi              the color components.
     * @return                  new image consisting of these components.
     * @throws NullPointerException     if <tt>allocationContext</tt>, <tt>rgbi</tt>
     *                                  or one of <tt>rgbi</tt> elements is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>rgbi.size()&lt;3</tt>.
     * @throws SizeMismatchException    if the processed matrices have different dimensions.
     */
    public Image2D newRGBImage2D(Context allocationContext, List<Matrix<? extends PArray>> rgbi);

    /**
     * Creates new RGB instance of {@link Image2D}, described by the given matrices.
     * Equivalent to the corresponding call of {@link #newRGBImage2D(Context, java.util.List)} method.
     *
     * <p>The content of the passed matrix may be shared with the new instance.
     *
     * <p>The <tt>allocationContext</tt> argument must contain some context, that can be used while
     * performing calculations. It must not be <tt>null</tt>, but it is not required to implement
     * {@link ImageContext} interface. (For accessing {@link ImageContext} methods, an implementation of this method
     * can use <tt>this</tt> reference.) Usually this context is used for showing progress
     * and allowing interruption.</p>
     *
     * @param allocationContext the context used for creating new instance; usually can be identical to this one.
     * @param r                 the red component.
     * @param g                 the green component.
     * @param b                 the blue component.
     * @return                  new grayscale image with this intensity.
     * @throws NullPointerException  if <tt>allocationContext</tt>, <tt>r</tt>, <tt>g</tt> or <tt>b</tt>
     *                               argument is <tt>null</tt>.
     * @throws SizeMismatchException if the processed matrices have different dimensions.
     */
    public Image2D newRGBImage2D(Context allocationContext,
            final Matrix<? extends PArray> r,
            final Matrix<? extends PArray> g,
            final Matrix<? extends PArray> b);

    /**
     * Creates new grayscale instance of {@link Image2D}, described by the given matrix of the intensity.
     *
     * <p>The content of the passed matrix may be shared with the new instance.
     *
     * <p>The <tt>allocationContext</tt> argument must contain some context, that can be used while
     * performing calculations. It must not be <tt>null</tt>, but it is not required to implement
     * {@link ImageContext} interface. (For accessing {@link ImageContext} methods, an implementation of this method
     * can use <tt>this</tt> reference.) Usually this context is used for showing progress
     * and allowing interruption.</p>
     *
     * @param allocationContext the context used for creating new instance; usually can be identical to this one.
     * @param i                 the intensity component.
     * @return                  new grayscale image with this intensity.
     * @throws NullPointerException if <tt>allocationContext</tt> or <tt>i</tt> argument is <tt>null</tt>.
     */
    public Image2D newGrayscaleImage2D(Context allocationContext, Matrix<? extends PArray> i);

    /**
     * Creates new RGB or grayscale instance of {@link Image2D}, described by the given matrices:
     * RGB if the length of this list is greater than 1, grayscale if it is equal to 1.
     * Equivalent to the corresponding call of {@link #newRGBImage2D(Context, java.util.List)} method,
     * if <tt>rgbi.size()&gt;1</tt>, or to the corresponding call of {@link #newGrayscaleImage2D(Context, Matrix)}
     * method, if <tt>rgbi.size()==1</tt>.
     *
     * @param allocationContext the context used for creating new instance; usually can be identical to this one.
     * @param rgbi              the color components.
     * @return                  new image consisting of these components.
     * @throws NullPointerException     if <tt>allocationContext</tt>, <tt>rgbi</tt>
     *                                  or one of <tt>rgbi</tt> elements is <tt>null</tt>.
     * @throws IllegalArgumentException if <tt>rgbi.size()&lt;3</tt> and <tt>rgbi.size()!=1</tt>.
     * @throws SizeMismatchException    if the processed matrices have different dimensions.
     */
    public Image2D newImage2D(Context allocationContext, List<Matrix<? extends PArray>> rgbi);

    public Image2D copyImage2D(Context context, Image2D image2D) throws IOException;

    public Image2D openImage2D(String path) throws IOException;

    public void shareImage2D(Image2D image, String path) throws IOException;
}
