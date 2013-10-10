package net.algart.math.patterns.demo;

import net.algart.math.patterns.Pattern;
import net.algart.math.patterns.Patterns;
import net.algart.math.IPoint;

import java.util.Set;
import java.util.HashSet;

/**
 * <p>Simple test illustrating operations with very large patterns (geometrically).
 * Please modify and debug this code to view how large patters are processed.</p>
 *
 * <p>AlgART Laboratory 2007-2013</p>
 *
 * @author Daniel Alievsky
 * @version 1.2
 * @since JDK 1.5
 */
public class LargePatternTest {
    public static void main(String[] args) {
        Pattern p1 = Patterns.newRectangularIntegerPattern(IPoint.valueOf(0, 0), IPoint.valueOf(20, 20));
        Set<IPoint> points = new HashSet<IPoint>();
        for (int k = 0; k < 1000; k++) {
            points.add(IPoint.valueOf(k, k));
            points.add(IPoint.valueOf(2000000000 + k, k));
            points.add(IPoint.valueOf(k, 2000000000 + k));
        }
        Pattern p2 = Patterns.newIntegerPattern(points);
        Pattern pSum = Patterns.newMinkowskiSum(p1, p2);
        System.out.println("Large sum: " + pSum + ", contains " + pSum.pointCount() + " points");
    }
}
