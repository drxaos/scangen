package com.github.drxaos;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Random;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.IntStream;

public class ReportService {

    public byte[] generateOdtPdfScan(String templateName) throws Exception {

        var inp = new FileInputStream(templateName);
        var pdf = OfficeUtil.convert("pdf", templateName, inp);

        var images = PdfUtil.rasterizePdf(pdf);

        var seed = new AtomicLong(templateName.hashCode());
        var processedImages = images.stream().map(it -> {
            var dotsRandom = new Random(seed.get() + 1);
            var shiftRandom = new Random(seed.get() + 12);
            var nextRandom = new Random(seed.get() + 99);
            double scale = 2.5;
            int xMargin = 100;
            int yMargin = 100;
            int xShift = (int) (shiftRandom.nextGaussian() / 50 * scale * it.getWidth());
            int yShift = (int) (shiftRandom.nextGaussian() / 50 * scale * it.getHeight());

            System.out.println("noise");
            it = ImageUtil.noise(it, Color.WHITE, 4000, 1, 3, seed.get() + 2);
            it = ImageUtil.noise(it, Color.BLACK, 2 + dotsRandom.nextInt(3), 1, 2, seed.get() + 3);

            it = ImageUtil.scale(it, Color.WHITE, scale);
            it = ImageUtil.noise(it, Color.BLACK, 7 + dotsRandom.nextInt(10), 1, 2, seed.get() + 4);

            System.out.println("margin");
            it = ImageUtil.crop(it, Color.BLACK, -yMargin + yShift, -xMargin + xShift, -yMargin, -xMargin);

            it = superSampleRotate(seed, it, xMargin, yMargin);

            System.out.println("postprocess");
            it = ImageUtil.crop(it, Color.WHITE, yMargin, xMargin, yMargin - yShift, xMargin - xShift);
            it = ImageUtil.scale(it, Color.WHITE, 1 / scale);
            it = ImageUtil.blur(it);
            it = ImageUtil.sharpen(it);

            seed.set(nextRandom.nextLong());
            return it;
        }).toList();

        return PdfUtil.imagesToPdf(processedImages);
    }

    private BufferedImage superSampleRotate(
            AtomicLong seed,
            BufferedImage it,
            int xMargin,
            int yMargin
    ) {
        var random = new Random(seed.get() + 11);

        double rotateCenterX = it.getWidth() * random.nextDouble() + xMargin;
        double rotateCenterY = it.getHeight() * random.nextDouble() + yMargin;
        double angle = random.nextGaussian() * Math.PI / 500d;

        System.out.println("prepare");
        it = ImageUtil.blur(it);

        List<BufferedImage> ssList;
        try {
            ssList = makeSamples(rotateCenterX, rotateCenterY, angle, it);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        System.out.println("blend");
        it = ImageUtil.blend(Color.WHITE, ssList);
        return it;
    }

    private List<BufferedImage> makeSamples(double rotateCenterX, double rotateCenterY, double angle, BufferedImage ssTemplate) throws IOException {
        ExecutorService executorService = Executors.newFixedThreadPool(4);
        try (Closeable close = executorService::shutdown) {
            final List<Future<BufferedImage>> futures = IntStream.range(-7, 7).mapToObj(n -> executorService.submit(() -> {
                System.out.println("sample " + n);
                var sse = ImageUtil.rotate(ssTemplate, Color.BLACK, angle * (1d + 0.00117 * n), rotateCenterX, rotateCenterY);
                return ImageUtil.blur(sse);
            })).toList();
            return futures.stream().map(bufferedImageFuture -> {
                try {
                    return bufferedImageFuture.get();
                } catch (InterruptedException | ExecutionException e) {
                    throw new RuntimeException(e);
                }
            }).toList();
        }
    }
}
