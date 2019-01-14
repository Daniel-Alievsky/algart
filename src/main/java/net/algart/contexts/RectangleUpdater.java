/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2007-2019 Daniel Alievsky, AlgART Laboratory (http://algart.net)
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

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
