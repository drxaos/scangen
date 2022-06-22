package com.github.drxaos;

import java.awt.*;
import java.awt.image.BufferedImage;

public class Blender {
    private BufferedImage newImage;
    private Graphics2D g2d;

    public Blender(final BufferedImage template, final Color background, final int count) {
        newImage = new BufferedImage(template.getWidth(), template.getHeight(), template.getType());
        g2d = newImage.createGraphics();
        g2d.setBackground(background);
        g2d.clearRect(0, 0, newImage.getWidth(), newImage.getHeight());
        g2d.setComposite(AlphaComposite.SrcOver.derive(1.7f / count));
    }

    public synchronized void blendIn(final BufferedImage image) {
        g2d.drawImage(image, 0, 0, null);
    }

    public synchronized BufferedImage finalizeAndDispose() {
        g2d.dispose();
        g2d = null;
        final var result = newImage;
        newImage = null;
        return result;
    }
}
