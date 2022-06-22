package com.github.drxaos;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.Closeable;
import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

public class ScanGenerator {

    public byte[] convertToScan(final byte[] pdfBytes, final long seed) throws Exception {
        final var images = PdfUtil.rasterizePdf(pdfBytes);
        final var processedImages = processPages(images, seed);
        return PdfUtil.imagesToPdf(processedImages);
    }

    private List<BufferedImage> processPages(final List<BufferedImage> images, final long seed) {
        return IntStream.range(0, images.size()).mapToObj(idx -> {
            System.out.println("\nPage " + idx);
            return processPage(images.get(idx), seed + idx);
        }).toList();
    }

    private BufferedImage processPage(final BufferedImage image, final long pageSeed) {
        final var page = new AtomicReference<>(image);

        final var dotsRandom = new Random(pageSeed + 111);
        final int whiteNoiseCount = page.get().getHeight() * page.get().getWidth() / 2000;
        final int bigDotsCount = 2 + dotsRandom.nextInt(3);
        noise1(page, whiteNoiseCount, bigDotsCount, pageSeed + 222);

        final double superSampleScale = 2.2;
        scale(page, superSampleScale);

        final int smallDotsCount = 7 + dotsRandom.nextInt(10);
        noise2(page, smallDotsCount, pageSeed + 333);

        final var shiftRandom = new Random(pageSeed + 444);
        final double maxShift = 1d / 50;
        final int xShift = (int) (shiftRandom.nextGaussian() * maxShift * page.get().getWidth());
        final int yShift = (int) (shiftRandom.nextGaussian() * maxShift * page.get().getHeight());
        final int xExtendMargins = 30;
        final int yExtendMargins = 30;
        margin(page, xShift, yShift, xExtendMargins, yExtendMargins);

        superSampleRotate(page, xExtendMargins, yExtendMargins, pageSeed + 555);

        crop(page, xShift, yShift, xExtendMargins, yExtendMargins);
        reduce(page, superSampleScale);
        return page.get();
    }

    private void reduce(final AtomicReference<BufferedImage> page, final double superSampleScale) {
        System.out.println("reduce");
        scale(page, 1 / superSampleScale);
        page.updateAndGet(ImageConvolveUtil::blur);
        page.updateAndGet(ImageConvolveUtil::sharpen);
    }

    private void crop(
            final AtomicReference<BufferedImage> page,
            final int xShift,
            final int yShift,
            final int xExtendMargins,
            final int yExtendMargins
    ) {
        System.out.println("crop");
        page.updateAndGet(it -> ImageTransformUtil.crop(
                it,
                Color.WHITE,
                yExtendMargins,
                xExtendMargins,
                yExtendMargins - yShift,
                xExtendMargins - xShift
        ));
    }

    private void margin(
            final AtomicReference<BufferedImage> page,
            final int xShift,
            final int yShift,
            final int xExtendMargins,
            final int yExtendMargins
    ) {
        System.out.println("margin");
        page.updateAndGet(it -> ImageTransformUtil.crop(
                it,
                Color.BLACK,
                -yExtendMargins + yShift,
                -xExtendMargins + xShift,
                -yExtendMargins,
                -xExtendMargins
        ));
    }

    private void noise2(final AtomicReference<BufferedImage> page, final int smallDotsCount, final long pageSeed) {
        System.out.println("noise2");
        page.updateAndGet(it -> ImageNoiseUtil.noise(it, Color.BLACK, smallDotsCount, 1, 2, pageSeed));
    }

    private void scale(final AtomicReference<BufferedImage> page, final double superSampleScale) {
        System.out.println("scale");
        page.updateAndGet(it -> ImageTransformUtil.scale(it, superSampleScale));
    }

    private void noise1(final AtomicReference<BufferedImage> page, final int whiteNoiseCount, final int bigDotsCount, final long pageSeed) {
        System.out.println("noise1");
        page.updateAndGet(it -> ImageNoiseUtil.noise(it, Color.WHITE, whiteNoiseCount, 1, 3, pageSeed + 1));
        page.updateAndGet(it -> ImageNoiseUtil.noise(it, Color.BLACK, bigDotsCount, 1, 2, pageSeed + 2));
    }

    private void superSampleRotate(final AtomicReference<BufferedImage> page, final int xMargin, final int yMargin, final long seed) {
        final var random = new Random(seed);

        final double rotateCenterX = page.get().getWidth() * random.nextDouble() + xMargin;
        final double rotateCenterY = page.get().getHeight() * random.nextDouble() + yMargin;
        final double angle = random.nextGaussian() * Math.PI / 500d;

        System.out.println("rotate prepare");
        page.updateAndGet(ImageConvolveUtil::blur);

        final var ssTemplate = page.get();

        final var blender = new Blender(ssTemplate, Color.WHITE, 15);

        final var executor = new ForkJoinPool(calculateParallelism(ssTemplate));
        try (Closeable ignored = executor::shutdown) {
            IntStream.range(-7, 7).boxed()
                    .sorted(Comparator.comparingInt(Math::abs))
                    .map(n -> executor.submit(() ->
                            blender.blendIn(makeSample(ssTemplate, rotateCenterX, rotateCenterY, angle, n))
                    ))
                    .toList()
                    .forEach(ForkJoinTask::join);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("rotate finalize");
        page.updateAndGet(it -> blender.finalizeAndDispose());
    }

    private int calculateParallelism(final BufferedImage ssTemplate) {
        Runtime.getRuntime().gc();
        final var buff = ssTemplate.getRaster().getDataBuffer();
        final int imageMemSize = buff.getSize() * DataBuffer.getDataTypeSize(buff.getDataType()) / 8;
        final long freeMemory = Runtime.getRuntime().freeMemory();
        return (int) Math.max(1, Math.min(freeMemory / imageMemSize, Runtime.getRuntime().availableProcessors() / 2));
    }

    private BufferedImage makeSample(
            final BufferedImage ssTemplate,
            final double rotateCenterX,
            final double rotateCenterY,
            final double angle,
            final int n
    ) {
        System.out.println("rotate sample " + n);
        final var sse = ImageTransformUtil.rotate(ssTemplate, Color.BLACK, angle * (1d + 0.00117 * n), rotateCenterX, rotateCenterY);
        return ImageConvolveUtil.blur(sse);
    }
}
