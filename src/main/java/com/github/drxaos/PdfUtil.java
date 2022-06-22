package com.github.drxaos;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfWriter;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

public class PdfUtil {

    public static List<BufferedImage> rasterizePdf(final byte[] pdfBytes) {
        return rasterizePdf(pdfBytes, 300);
    }

    public static List<BufferedImage> rasterizePdf(final byte[] pdfBytes, final float dpi) {
        try (final var pdf = PDDocument.load(pdfBytes)) {

            final var result = new ArrayList<BufferedImage>();
            final var pdfRenderer = new PDFRenderer(pdf);
            final int pagesCount = pdf.getNumberOfPages();
            IntStream.range(0, pagesCount).forEach(pageNumber -> {
                try {
                    result.add(pdfRenderer.renderImageWithDPI(pageNumber, dpi, ImageType.RGB));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static byte[] imagesToPdf(final List<BufferedImage> images) {
        final var result = new ByteArrayOutputStream();

        final float border = 12;
        final var outDoc = new Document(PageSize.A4, border, border, border, border);
        try (Closeable ignored = outDoc::close) {
            final var writer = PdfWriter.getInstance(outDoc, result);
            outDoc.open();

            final var pdfCB = new PdfContentByte(writer);
            for (BufferedImage value : images) {
                final float imageQuality = 1;
                final var image = Image.getInstance(pdfCB, value, imageQuality);
                image.scaleToFit(PageSize.A4.getWidth() - 2 * border, PageSize.A4.getHeight() - 2 * border);
                outDoc.add(image);
            }
        } catch (IOException | DocumentException e) {
            throw new RuntimeException(e);
        }

        return result.toByteArray();
    }
}
