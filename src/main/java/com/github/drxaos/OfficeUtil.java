package com.github.drxaos;

import org.apache.commons.io.FilenameUtils;
import org.jodconverter.core.DocumentConverter;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.document.DocumentFormat;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.core.office.OfficeManager;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class OfficeUtil {

    public static byte[] convert(final String targetFormatExt, String inputFileName, InputStream inputFile) throws IOException, OfficeException {
        final DocumentFormat conversionTargetFormat = DefaultDocumentFormatRegistry.getFormatByExtension(targetFormatExt);

        if (conversionTargetFormat == null) {
            System.out.println("unknown format");
            return null;
        }

        ByteArrayOutputStream convertedFile = doConvert(
                conversionTargetFormat,
                inputFile,
                inputFileName
        );

        return convertedFile.toByteArray();
    }

    private static ByteArrayOutputStream doConvert(
            final DocumentFormat targetFormat,
            final InputStream inputFile,
            String inputFileName
    ) throws OfficeException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        OfficeManager officeManager = LocalOfficeManager.make();
        officeManager.start();

        try {

            final DocumentConverter converter = LocalConverter.builder()
                    .officeManager(officeManager)
                    .build();

            final DocumentFormat sourceFormat = DefaultDocumentFormatRegistry.getFormatByExtension(
                    FilenameUtils.getExtension(inputFileName)
            );

            if (sourceFormat == null) {
                System.out.printf(
                        "Cannot convert file with extension %s since we cannot find the format in our registry%n",
                        FilenameUtils.getExtension(inputFileName)
                );
            }

            // Convert...
            converter.convert(inputFile)
                    .as(sourceFormat)
                    .to(outputStream)
                    .as(targetFormat)
                    .execute();

            return outputStream;
        } finally {
            if (officeManager.isRunning()) {officeManager.stop();}
        }
    }
}
