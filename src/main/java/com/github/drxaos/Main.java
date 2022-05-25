package com.github.drxaos;

import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static void main(String[] args) throws Exception {
        var fin = "3.odt";
        var fout = "3.pdf";

        final ReportService reportService = new ReportService();
        final byte[] bytes = reportService.generateOdtPdfScan(fin);
        Files.write(Paths.get(fout), bytes);
    }
}
