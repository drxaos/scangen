package com.github.drxaos;

import com.jhlabs.image.CrystallizeFilter;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendComposite;
import org.apache.pdfbox.pdmodel.graphics.blend.BlendMode;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.ColorModel;
import java.util.Random;
import java.util.stream.IntStream;

public class ImageNoiseUtil {

    public static BufferedImage noise(
            final BufferedImage image,
            final Color color,
            final int count,
            final double baseStroke,
            final double growStroke,
            final long seed
    ) {
        final var random = new Random(seed);

        final var g = (Graphics2D) image.getGraphics();

        IntStream.range(0, count).forEach(i -> {
            final int x = random.nextInt(image.getWidth());
            final int y = random.nextInt(image.getHeight());
            g.setStroke(new BasicStroke((float) (baseStroke + Math.abs(random.nextGaussian()) * ((random.nextInt(5) < 1)
                    ? growStroke
                    : 0))));
            g.setColor(color);
            g.drawLine(x, y, x + random.nextInt(5) - 2, y + random.nextInt(5) - 2);
        });

        g.dispose();

        final var filter = new NoiseFilter(seed);
        final var dst = filter.createCompatibleDestImage(image, ColorModel.getRGBdefault());
        filter.filter(image, dst);

        final var g2 = (Graphics2D) image.getGraphics();
        g2.setComposite(BlendComposite.getInstance(BlendMode.DARKEN, 0.8f));
        g2.drawImage(dst, 0, 0, image.getWidth(), image.getHeight(), null);
        g2.dispose();

        return image;
    }

    public static class NoiseFilter extends CrystallizeFilter {
        public NoiseFilter(final long seed) {
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
