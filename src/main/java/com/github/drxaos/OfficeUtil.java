package com.github.drxaos;

import org.apache.commons.io.FilenameUtils;
import org.jodconverter.core.document.DefaultDocumentFormatRegistry;
import org.jodconverter.core.document.DocumentFormat;
import org.jodconverter.core.office.OfficeException;
import org.jodconverter.local.LocalConverter;
import org.jodconverter.local.office.LocalOfficeManager;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class OfficeUtil {

    public static byte[] convert(final String targetFormatExt, final String inputFileName, final InputStream inputFile) throws OfficeException {
        final var conversionTargetFormat = DefaultDocumentFormatRegistry.getFormatByExtension(targetFormatExt);

        if (conversionTargetFormat == null) {
            throw new RuntimeException("unknown target format");
        }

        final var convertedFile = doConvert(conversionTargetFormat, inputFile, inputFileName);

        return convertedFile.toByteArray();
    }

    private static ByteArrayOutputStream doConvert(
            final DocumentFormat targetFormat,
            final InputStream inputFile,
            final String inputFileName
    ) throws OfficeException {
        final var outputStream = new ByteArrayOutputStream();
        final var officeManager = LocalOfficeManager.make();
        officeManager.start();

        try {

            final var converter = LocalConverter.builder()
                    .officeManager(officeManager)
                    .build();

            final var sourceFormat = DefaultDocumentFormatRegistry.getFormatByExtension(
                    FilenameUtils.getExtension(inputFileName)
            );

            if (sourceFormat == null) {
                throw new RuntimeException("unknown source format");
            }

            // Convert...
            converter.convert(inputFile)
                    .as(sourceFormat)
                    .to(outputStream)
                    .as(targetFormat)
                    .execute();

            return outputStream;
        } finally {
            if (officeManager.isRunning()) {
                officeManager.stop();
            }
        }
    }
}
