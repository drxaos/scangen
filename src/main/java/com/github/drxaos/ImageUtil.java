package com.github.drxaos;

import com.jhlabs.image.CrystallizeFilter;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendComposite;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.awt.image.ConvolveOp;
import java.awt.image.ImageObserver;
import java.awt.image.Kernel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class ImageUtil {

    private static float[] toFloatArray(double[] doubleArray) {
        float[] floatArray = new float[doubleArray.length];
        for (int i = 0; i < doubleArray.length; i++) {
            floatArray[i] = (float) doubleArray[i];
        }
        return floatArray;
    }

    private static BufferedImage convolve(BufferedImage image, List<Float> k, int w, int h) {
        float sum = k.stream().reduce(0f, Float::sum);
        k = k.stream().map(a -> a / sum).toList();
        Kernel kernel = new Kernel(w, h, toFloatArray(k.stream().mapToDouble(f -> f).toArray()));
        ConvolveOp op = new ConvolveOp(kernel, ConvolveOp.EDGE_NO_OP, null);
        image = op.filter(image, null);

        return image;
    }

    public static BufferedImage blur(BufferedImage image) {
        List<Float> k = new ArrayList<Float>(Arrays.asList(
                1f, 1f, 1f, 1f, 1f,
                1f, 5f, 10f, 5f, 1f,
                1f, 10f, 15f, 10f, 1f,
                1f, 5f, 10f, 5f, 1f,
                1f, 1f, 1f, 1f, 1f
        ));
        return convolve(image, k, 5, 5);
    }

    public static BufferedImage sharpen(BufferedImage image) {
        ArrayList<Float> k = new ArrayList<>(Arrays.asList(
                -1f, -1f, -1f,
                -1f, 9f, -1f,
                -1f, -1f, -1f
        ));
        return convolve(image, k, 3, 3);
    }

    public static BufferedImage noise(BufferedImage image, Color color, int count, double baseStroke, double growStroke, long seed) {
        Random random = new Random(seed);

        Graphics2D g = (Graphics2D) image.getGraphics();

        for (int i = 0; i < count; i++) {
            int x = random.nextInt(image.getWidth());
            int y = random.nextInt(image.getHeight());
            g.setStroke(new BasicStroke((float) (baseStroke + Math.abs(random.nextGaussian()) * ((random.nextInt(5) < 1)
                    ? growStroke
                    : 0))));
            g.setColor(color);
            g.drawLine(x, y, x + random.nextInt(5) - 2, y + random.nextInt(5) - 2);
        }

        g.dispose();

        NoiseFilter filter = new NoiseFilter(seed);
        BufferedImage dst = filter.createCompatibleDestImage(image, ColorModel.getRGBdefault());
        filter.filter(image, dst);

        g = (Graphics2D) image.getGraphics();
        g.setComposite(BlendComposite.getInstance(BlendMode.DARKEN, 0.8f));
        g.drawImage(dst, 0, 0, image.getWidth(), image.getHeight(), (ImageObserver) null);
        g.dispose();

        return image;
    }

    public static BufferedImage scale(BufferedImage imageToScale, int dWidth, int dHeight) {
        BufferedImage scaledImage = null;
        if (imageToScale != null) {
            scaledImage = new BufferedImage(dWidth, dHeight, imageToScale.getType());
            Graphics2D graphics2D = scaledImage.createGraphics();
            graphics2D.drawImage(imageToScale, 0, 0, dWidth, dHeight, null);
            graphics2D.dispose();
        }

        return scaledImage;
    }

    public static BufferedImage crop(BufferedImage imageToCrop, Color background, int top, int right, int bottom, int left) {
        BufferedImage croppedImage = null;
        if (imageToCrop != null) {
            croppedImage = new BufferedImage(imageToCrop.getWidth() - right - left, imageToCrop.getHeight() - bottom - top, imageToCrop.getType());
            Graphics2D graphics2D = croppedImage.createGraphics();
            graphics2D.setBackground(background);
            graphics2D.clearRect(0, 0, croppedImage.getWidth(), croppedImage.getHeight());
            graphics2D.drawImage(imageToCrop, -left, -top, null);
            graphics2D.dispose();
        }

        return croppedImage;
    }

    public static BufferedImage rotate(BufferedImage imag, Color background, double radians, double locationX, double locationY) {
        AffineTransform tx = AffineTransform.getRotateInstance(radians, locationX, locationY);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
        BufferedImage newImage = new BufferedImage(imag.getWidth(), imag.getHeight(), imag.getType());
        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.setBackground(background);
        graphics2D.clearRect(0, 0, newImage.getWidth(), newImage.getHeight());
        graphics2D.dispose();
        op.filter(imag, newImage);
        return newImage;
    }

    public static BufferedImage scale(BufferedImage imag, Color background, double scale) {
        AffineTransform tx = AffineTransform.getScaleInstance(scale, scale);
        AffineTransformOp op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
        BufferedImage newImage = new BufferedImage((int) (imag.getWidth() * scale), (int) (imag.getHeight() * scale), imag.getType());
        Graphics2D graphics2D = newImage.createGraphics();
        graphics2D.setBackground(background);
        graphics2D.clearRect(0, 0, newImage.getWidth(), newImage.getHeight());
        graphics2D.dispose();
        op.filter(imag, newImage);
        return newImage;
    }

    public static BufferedImage blend(Color background, List<BufferedImage> images) {
        BufferedImage newImage = new BufferedImage(images.get(0).getWidth(), images.get(0).getHeight(), images.get(0).getType());

        final Graphics2D g2d = newImage.createGraphics();
        g2d.setBackground(background);
        g2d.clearRect(0, 0, newImage.getWidth(), newImage.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver.derive(1.7f / images.size()));
        images.forEach(it -> g2d.drawImage(it, 0, 0, null));
        g2d.dispose();

        return newImage;
    }

    public static class NoiseFilter extends CrystallizeFilter {
        public NoiseFilter(long seed) {
            setScale(0.2f);
            setRandomness(0.2f);
            setAmount(0.1f);
            setTurbulence(0.1f);
            gain = 0.1f;
            bias = 0.1f;
            setAngleCoefficient(0.2f);
            setEdgeThickness(0);
            random = new Random(seed);
        }
    }
}
