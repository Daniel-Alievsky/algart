package net.algart.contexts;

/**
 * <p>The context allowing to inform the application that some rectangular region
 * of some 2D matrix or image has been updated.</p>
 *
 * <p>For example, if some algorithm prepares an image for drawing,
 * it is a good idea to request this context and, if it is provided,
 * to notify the application about every new ready region of the image.
 * Then application will be able to pass corresponding context to the algorithm
 * and perform drawing ready portions of the image, not waiting for finishing
 * the algorithm.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public interface RectangleUpdater extends Context {

    /**
     * Informs that the given rectangular region of some 2D object is updated.
     *
     * @param left   the x-coordinate of the top left vertex of the rectangle.
     * @param top    the y-coordinate of the top left vertex of the rectangle.
     * @param width  the width of the rectangle.
     * @param height the width of the rectangle.
     */
    public void updateRectangle(double left, double top, double width, double height);
}
