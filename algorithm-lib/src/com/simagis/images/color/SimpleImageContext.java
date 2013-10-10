package com.simagis.images.color;

import java.io.IOException;
import java.util.List;
import java.util.Collections;
import net.algart.arrays.*;
import net.algart.contexts.*;
import net.algart.math.functions.LinearFunc;

public class SimpleImageContext extends AbstractContext implements ImageContext {
    public SimpleImageContext() { // public to be usable as a service
        super(false);
    }

    private static ImageContext INSTANCE = new SimpleImageContext();

    public static ImageContext getInstance() {
        return INSTANCE;
    }

    public Image2D newRGBImage2D(Context allocationContext, List<Matrix<? extends PArray>> rgbi) {
        if (rgbi.size() < 3)
            throw new IllegalArgumentException("Too short rgbi argument");
        return newRGBImage2D(allocationContext, rgbi.get(0), rgbi.get(1), rgbi.get(2));
    }

    public Image2D newRGBImage2D(Context allocationContext,
        final Matrix<? extends PArray> r,
        final Matrix<? extends PArray> g,
        final Matrix<? extends PArray> b)
    {
        if (allocationContext == null)
            throw new NullPointerException("Null allocationContext");
        if (r == null || g == null || b == null)
            throw new NullPointerException("Null rgbi.get(k) element for some k=0,1,2");
        if (!r.dimEquals(g) || !r.dimEquals(b))
            throw new SizeMismatchException("r, g and b matrix dimensions mismatch: r is "
                + r + ", g is " + g + ", b is " + b);
        final List<Matrix<? extends PArray>> rgbiClone = Matrices.several(PArray.class, r, g, b);
        return new Image2D() {
            public long dimX() {
                return r.dimX();
            }

            public long dimY() {
                return r.dimY();
            }

            public boolean isGrayscale() {
                return false;
            }

            public Matrix<? extends PArray> r() {
                return r;
            }

            public Matrix<? extends PArray> g() {
                return g;
            }

            public Matrix<? extends PArray> b() {
                return b;
            }

            public Matrix<? extends PArray> i() {
                return ImageConversions.asIntensity(r, g, b);
            }

            public List<Matrix<? extends PArray>> rgbi() {
                return Collections.unmodifiableList(rgbiClone);
            }

            public void freeResources(ArrayContext context) {
                r.freeResources(context);
                g.freeResources(context);
                b.freeResources(context);
            }
        };
    }

    public Image2D newGrayscaleImage2D(Context allocationContext, final Matrix<? extends PArray> i) {
        if (allocationContext == null)
            throw new NullPointerException("Null allocationContext");
        if (i == null)
            throw new NullPointerException("Null i argument");
        final List<Matrix<? extends PArray>> rgbi = Collections.<Matrix<? extends PArray>>singletonList(i);
        return new Image2D() {
            public long dimX() {
                return i.dimX();
            }

            public long dimY() {
                return i.dimY();
            }

            public boolean isGrayscale() {
                return true;
            }

            public Matrix<? extends PArray> r() {
                return i;
            }

            public Matrix<? extends PArray> g() {
                return i;
            }

            public Matrix<? extends PArray> b() {
                return i;
            }

            public Matrix<? extends PArray> i() {
                return i;
            }

            public List<Matrix<? extends PArray>> rgbi() {
                return rgbi;
            }

            public void freeResources(ArrayContext context) {
                i.freeResources(context);
            }
        };
    }

    public Image2D newImage2D(Context allocationContext, List<Matrix<? extends PArray>> rgbi) {
        return rgbi.size() == 1 ?
            newGrayscaleImage2D(allocationContext, rgbi.get(0)) :
            newRGBImage2D(allocationContext, rgbi);
    }

    public Image2D copyImage2D(Context context, Image2D image2D) throws IOException {
        return image2D.isGrayscale()
            ? newGrayscaleImage2D(context, image2D.i())
            : newRGBImage2D(context, image2D.rgbi());
    }

    public Image2D openImage2D(String path) throws IOException {
        throw new UnsupportedOperationException();
    }

    public void shareImage2D(Image2D image, String path) {
        throw new UnsupportedOperationException();
    }
}
