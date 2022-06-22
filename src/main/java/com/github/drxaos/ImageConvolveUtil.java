package com.github.drxaos;

import java.awt.image.BufferedImage;
import java.awt.image.ConvolveOp;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;

public class ImageConvolveUtil {

    private static float[] toFloatArray(final double[] doubleArray) {
        final float[] floatArray = new float[doubleArray.length];
        IntStream.range(0, doubleArray.length).forEach(i -> floatArray[i] = (float) doubleArray[i]);
        return floatArray;
    }

    private static BufferedImage convolve(final BufferedImage image, final List<Float> k, final int w, final int h) {
        final float sum = k.stream().reduce(0f, Float::sum);
        final var k2 = k.stream().map(a -> a / sum).toList();
        final var kernel = new Kernel(w, h, toFloatArray(k2.stream().mapToDouble(f -> f).toArray()));
        final var op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        return op.filter(image, null);
    }

    public static BufferedImage blur(final BufferedImage image) {
        final var k = new ArrayList<>(Arrays.asList(
                1f, 1f, 1f, 1f, 1f,
                1f, 5f, 10f, 5f, 1f,
                1f, 10f, 15f, 10f, 1f,
                1f, 5f, 10f, 5f, 1f,
                1f, 1f, 1f, 1f, 1f
        ));
        return convolve(image, k, 5, 5);
    }

    public static BufferedImage sharpen(BufferedImage image) {
        final var k = new ArrayList<>(Arrays.asList(
                -1f, -1f, -1f,
                -1f, 9f, -1f,
                -1f, -1f, -1f
        ));
        return convolve(image, k, 3, 3);
    }
}
