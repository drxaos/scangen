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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class PdfUtil {
    public static List<BufferedImage> rasterizePdf(byte[] pdfBytes, float dpi) throws IOException {
        try (PDDocument pdf = PDDocument.load(pdfBytes)) {

            ArrayList<BufferedImage> result = new ArrayList<>();
            PDFRenderer pdfRenderer = new PDFRenderer(pdf);
            int pagesCount = pdf.getNumberOfPages();
            for (int pageNumber = 0; pageNumber < pagesCount; pageNumber++) {
                result.add(pdfRenderer.renderImageWithDPI(pageNumber, dpi, ImageType.RGB));
            }

            return result;
        }
    }

    public static List<BufferedImage> rasterizePdf(byte[] pdfBytes) throws IOException {
        return rasterizePdf(pdfBytes, 300);
    }

    public static byte[] imagesToPdf(List<BufferedImage> images) throws IOException, DocumentException {
        ByteArrayOutputStream result = new ByteArrayOutputStream();

        float border = 12;
        Document outDoc = new Document(PageSize.A4, border, border, border, border);
        PdfWriter writer = PdfWriter.getInstance(outDoc, result);
        outDoc.open();
        try {
            PdfContentByte pdfCB = new PdfContentByte(writer);
            for (BufferedImage value : images) {
                float imageQuality = 1;
                Image image = Image.getInstance(pdfCB, value, imageQuality);
                image.scaleToFit(PageSize.A4.getWidth() - 2 * border, PageSize.A4.getHeight() - 2 * border);
                outDoc.add(image);
            }
        } finally {
            outDoc.close();
        }

        return result.toByteArray();
    }
}
