package com.github.drxaos;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.image.AffineTransformOp;
import java.awt.image.BufferedImage;

public class ImageTransformUtil {

    public static BufferedImage crop(
            final BufferedImage imageToCrop,
            final Color background,
            final int top,
            final int right,
            final int bottom,
            final int left
    ) {
        final BufferedImage croppedImage;
        if (imageToCrop != null) {
            croppedImage = new BufferedImage(imageToCrop.getWidth() - right - left, imageToCrop.getHeight() - bottom - top, imageToCrop.getType());
            final var graphics2D = croppedImage.createGraphics();
            graphics2D.setBackground(background);
            graphics2D.clearRect(0, 0, croppedImage.getWidth(), croppedImage.getHeight());
            graphics2D.drawImage(imageToCrop, -left, -top, null);
            graphics2D.dispose();
        } else {
            croppedImage = null;
        }

        return croppedImage;
    }

    public static BufferedImage rotate(
            final BufferedImage imag,
            final Color background,
            final double radians,
            final double locationX,
            final double locationY
    ) {
        final var tx = AffineTransform.getRotateInstance(radians, locationX, locationY);
        final var op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
        final var newImage = new BufferedImage(imag.getWidth(), imag.getHeight(), imag.getType());
        final var graphics2D = newImage.createGraphics();
        graphics2D.setBackground(background);
        graphics2D.clearRect(0, 0, newImage.getWidth(), newImage.getHeight());
        graphics2D.dispose();
        op.filter(imag, newImage);
        return newImage;
    }

    public static BufferedImage scale(
            final BufferedImage imag,
            final double scale
    ) {
        final var tx = AffineTransform.getScaleInstance(scale, scale);
        final var op = new AffineTransformOp(tx, AffineTransformOp.TYPE_BICUBIC);
        final var newImage = new BufferedImage((int) (imag.getWidth() * scale), (int) (imag.getHeight() * scale), imag.getType());
        final var graphics2D = newImage.createGraphics();
        graphics2D.setBackground(Color.WHITE);
        graphics2D.clearRect(0, 0, newImage.getWidth(), newImage.getHeight());
        graphics2D.dispose();
        op.filter(imag, newImage);
        return newImage;
    }
}
