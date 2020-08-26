package be.quodlibet.boxable;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.io.IOException;
import java.util.Random;

/*
original
seconds: 68.3, PDFs: 100, 1.46 pdf/sec
size: 534836 bytes

1st commit, remove repeated row.getHeight() calls:
  seconds: 35.5, PDFs: 300, 8.44 pdf/sec (530% faster)

2nd commit, memoize Paragraph.tokens:
  seconds: 32.2, PDFs: 300, 9.33 pdf/sec (11% faster)

3rd commit, memoize Paragraph.getLines:
  trial 1: seconds: 27.3, PDFs: 300, 10.99 pdf/sec (18% faster)

4th commit, find possible wrap points without regex:
1: seconds: 25.6, PDFs: 300, 11.73 pdf/sec (6.7% faster)
2: seconds: 23.7, PDFs: 300, 12.68 pdf/sec (15% faster)
avg: 12.2 pdf/sec (11% faster)

5th commit, use rtrim instead of regex:
1: seconds: 22.5, PDFs: 300, 13.36 pdf/sec
2: seconds: 21.3, PDFs: 300, 14.10 pdf/sec
avg: 13.73 (12.5% faster)

6th commit, reduce unnecessary getStringWidth calls
1: seconds: 20.1, PDFs: 300, 14.96 pdf/sec
2: seconds: 20.3, PDFs: 300, 14.81 pdf/sec
avg: 14.89 (8.4% faster)

7th commit, cache width in Token
1: seconds: 18.9, PDFs: 300, 15.85 pdf/sec
2: seconds: 19.5, PDFs: 300, 15.42 pdf/sec
avg: 15.64 (5% faster)

8th commit, PageContentStreamOptimized
1: seconds: 18.0, PDFs: 300, 16.65 pdf/sec
2: seconds: 18.5, PDFs: 300, 16.25 pdf/sec
avg: 16.45 (5.2% faster, 8.6% smaller PDF)

69d2d0c2:
seconds: 15.8, PDFs: 300, 19.01 pdf/sec
seconds: 14.7, PDFs: 300, 20.34 pdf/sec
avg: 19.68

97bf4a99:
seconds: 13.6, PDFs: 300, 22.04 pdf/sec
seconds: 14.1, PDFs: 300, 21.26 pdf/sec
seconds: 22.7, PDFs: 500, 22.00 pdf/sec
size: 383593 bytes
 - 15 times faster than original
 - 28.3% smaller PDF
*/

public class Perf {
    public static void main(String[] args) throws Exception {
        Perf perf = new Perf();
//        perf.generatePdf("perf-97bf4a99.pdf");
//        System.exit(0);

        for (int i = 0; i < 10; i++) {
            System.out.print("\rWarmup " + i);
            perf.generatePdf(null);
        }

        int LOOPS = 500;
        long start = System.nanoTime();
        for (int i = 0; i < LOOPS; i++) {
            perf.generatePdf(null);
            if (i % 10 == 0) System.out.print("\r" + (100 * i / LOOPS) + "%");
        }
        long stop = System.nanoTime();
        System.out.println("\r100%");

        double seconds = (stop - start) / 1_000_000_000.;
        System.out.printf("seconds: %.1f, PDFs: %d, %.2f pdf/sec%n",
                seconds, LOOPS, LOOPS / seconds);
    }

    void generatePdf(String fileName) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            addTable(document, page);

            if (fileName != null) document.save(fileName);
        }
    }

    private void addTable(PDDocument document, PDPage page) throws IOException {
        float margin = 50;
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);

        BaseTable table = new BaseTable(625, yStartNewPage, 70, tableWidth,
                margin, document, page, true, true);
        generateDataRows(table);
        table.draw();
    }

    private void generateDataRows(BaseTable table) {
        for (String[] colData : randomData) {
            Row<PDPage> row = table.createRow(12f);
            for (String colDatum : colData) {
                row.createCell(10, colDatum);
            }
        }
    }

    String[][] randomData;

    Perf() {
        randomData = new String[500][];
        for (int row = 0; row < randomData.length; row++) {
            String[] cols = new String[10];
            randomData[row] = cols;
            for (int col = 0; col < cols.length; col++) {
                cols[col] = randomString();
            }
        }
    }

    static final String RANDOM_CHAR_SET = "abcefg1234567890 -.,;@:";
    Random random = new Random(123);

    String randomString() {
        int length = 3 + random.nextInt(18);
        char[] str = new char[length];
        for (int i = 0; i < str.length; i++) {
            str[i] = RANDOM_CHAR_SET.charAt(random.nextInt(RANDOM_CHAR_SET.length()));
        }
        return new String(str);
    }
}
