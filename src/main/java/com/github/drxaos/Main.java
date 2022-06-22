package com.github.drxaos;

import java.io.FileInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        final var fin = "3.odt";
        final var fout = "3.pdf";

        final var in = new FileInputStream(fin);

        System.out.println("converting to pdf");
        final var pdfBytes = OfficeUtil.convert("pdf", fout, in);

        final var scanGenerator = new ScanGenerator();
        final byte[] bytes = scanGenerator.convertToScan(pdfBytes, fin.hashCode());
        Files.write(Paths.get(fout), bytes);
    }
}
