package com.github.drxaos;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        var fin = "3.odt";
        var fout = "3.pdf";

        final ScanGenerator scanGenerator = new ScanGenerator();
        final byte[] bytes = scanGenerator.generateOdtPdfScan(fin);
        Files.write(Paths.get(fout), bytes);
    }
}
