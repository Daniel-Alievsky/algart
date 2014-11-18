/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2001-2003 Daniel Alievsky, AlgART Laboratory (http://algart.net)
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

package net.algart.lib;

import java.awt.*;
import java.awt.color.ColorSpace;
import java.awt.image.*;

/**
 * <p>Easy-to-use library for creating BufferedImage on the base of pixel arrays.
 *
 * @author  Daniel Alievsky
 * @version 1.0
 */

public final class ColorBuffers {

  /**
   * Don't let anyone instantiate this class.
   */
  private ColorBuffers() {}
  /*

  IMPORTANT: here (as in all standard Java and Windows modules)
     "RGB" means that
     RED is byte 2 (0xFF0000),
     GREEN is byte 1 (0x00FF00),
     BLUE is byte 0 (0x0000FF),
  and "BGR" means vise versa.
  It differs from our "RGB8" in AImage!
  */

  public static BufferedImage rgbToBufferedImage(int[] rgb, int sx, int sy, boolean useAlpha) {
    WritableRaster wr= Raster.createPackedRaster(
      new DataBufferInt(rgb,rgb.length),
      sx,sy,sx,
      useAlpha?
        new int[] {0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000}:
        new int[] {0x00ff0000, 0x0000ff00, 0x000000ff},
      new Point(0,0));
    return new BufferedImage(
      useAlpha?
        new DirectColorModel(32, 0x00ff0000, 0x0000ff00, 0x000000ff, 0xff000000):
        new DirectColorModel(24, 0x00ff0000, 0x0000ff00, 0x000000ff, 0x0),
      wr,false,null);
  }

  public static BufferedImage bgrToBufferedImage(int[] bgr, int sx, int sy, boolean useAlpha) {
    WritableRaster wr= Raster.createPackedRaster(
      new DataBufferInt(bgr,bgr.length),
      sx,sy,sx,
      useAlpha?
        new int[] {0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000}:
        new int[] {0x000000ff, 0x0000ff00, 0x00ff0000},
      new Point(0,0));
    return new BufferedImage(
      useAlpha?
        new DirectColorModel(32, 0x000000ff, 0x0000ff00, 0x00ff0000, 0xff000000):
        new DirectColorModel(24, 0x000000ff, 0x0000ff00, 0x00ff0000, 0x0),
      wr,false,null);
  }

  public static BufferedImage rgbToBufferedImage(byte[][] rgba, int sx, int sy) {
  // 2 possible valid situations:
  //   rgba.length==3: alpha-channel is not used
  //   rgba.length==4: rgba[3] is alpha-channel
  // rgba[0] should always contain the red band,
  // rgba[1] - green, rgba[2] - blue
    int[] indices= new int[rgba.length];
    int[] offsets= new int[rgba.length];
    for (int m=0; m<indices.length; m++) indices[m]= m;
    WritableRaster wr= Raster.createBandedRaster(
      new DataBufferByte(rgba,rgba[0].length),
      sx,sy,sx,indices,offsets,
      new Point(0,0));
      final int[] bits = new int[rgba.length];
      for (int i = 0; i < bits.length; i++) {
          bits[i] = 8;
      }
    return new BufferedImage(
      new ComponentColorModel(ColorSpace.getInstance(ColorSpace.CS_sRGB),
        bits,
        rgba.length>=4,false,ColorModel.TRANSLUCENT,DataBuffer.TYPE_BYTE),
      wr,false,null);
  }
}