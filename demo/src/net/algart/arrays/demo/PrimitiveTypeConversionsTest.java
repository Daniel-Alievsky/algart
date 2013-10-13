package net.algart.arrays.demo;

import net.algart.arrays.*;

/**
 * <p>Test for primitive type conversions in primitive arrays.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class PrimitiveTypeConversionsTest {
    private static MemoryModel mm = Arrays.SystemSettings.globalMemoryModel();
    public static void main(String[] args) {
        int[] v = {200, -100, 200, -100, 200, -100};
        UpdatableByteArray uba = mm.newByteArray(v.length);
        uba.setInt(0, v[0]);
        uba.setInt(1, v[1]);
        uba.setLong(2, v[2]);
        uba.setLong(3, v[3]);
        uba.setDouble(4, v[4]);
        uba.setDouble(5, v[5]);
        for (int k = 0; k < uba.length(); k++) {
            System.out.println(v[k] + ": byte " + uba.getByte(k) + ", int " + uba.getInt(k)
                + ", long " + uba.getLong(k) + ", double " + uba.getDouble(k));
        }
        System.out.println();

        v = new int[] {40000, -10000, 40000, -10000, 40000, -10000};
        UpdatableShortArray usa = mm.newShortArray(v.length);
        usa.setInt(0, v[0]);
        usa.setInt(1, v[1]);
        usa.setLong(2, v[2]);
        usa.setLong(3, v[3]);
        usa.setDouble(4, v[4]);
        usa.setDouble(5, v[5]);
        for (int k = 0; k < usa.length(); k++) {
            System.out.println(v[k] + ": short " + usa.getShort(k) + ", int " + usa.getInt(k)
                + ", long " + usa.getLong(k) + ", double " + usa.getDouble(k));
        }
        System.out.println();

        long[] vL = {2*1000*1000*1000, -1000*1000*1000,
            4L*1000*1000*1000, -1000*1000*1000};
        UpdatableIntArray uia = mm.newIntArray(vL.length);
        uia.setLong(0, vL[0]);
        uia.setLong(1, vL[1]);
        uia.setDouble(2, vL[2]);
        uia.setDouble(3, vL[3]);
        for (int k = 0; k < uia.length(); k++) {
            System.out.println(vL[k] + ": int " + uia.getInt(k)
                + ", long " + uia.getLong(k) + ", double " + uia.getDouble(k));
        }
        System.out.println();
    }
}
