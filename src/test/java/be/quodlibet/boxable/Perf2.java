package be.quodlibet.boxable;

import be.quodlibet.boxable.line.LineStyle;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;

import java.awt.Color;
import java.io.IOException;
import java.util.Random;

/*
original
seconds: 40.1, PDFs: 50, 1.25 pdf/sec
seconds: 80.7, PDFs: 100, 1.24 pdf/sec
size: 549163 bytes

213ef83d
seconds: 20.4, PDFs: 300, 14.73 pdf/sec

e5c661f7, cache width in Token:
seconds: 3.7, PDFs: 50, 13.51 pdf/sec

PageContentStreamOptimized:
seconds: 18.4, PDFs: 300, 16.26 pdf/sec (20% faster, 5.3% smaller PDF)

drawing all cells with background color: 15.5 pdf/sec
use rowHeight in Table.fillCellColor:
seconds: 17.9, PDFs: 300, 16.79 pdf/sec (8.4% faster)

reuse common Token instances:
seconds: 16.6, PDFs: 300, 18.03 pdf/sec
seconds: 16.2, PDFs: 300, 18.56 pdf/sec
avg: 18.3 (9% faster)


--- rotated ---
382a076b, rotated cells:
seconds: 46.3, PDFs: 400, 8.65 pdf/sec
size: 1091024

rotated cells, without beginText/endText and internal text matrix control:
seconds: 35.9, PDFs: 400, 11.14 pdf/sec (29% faster)
size: 644302 (41% smaller)
--- end rotated ---

97bf4a99
seconds: 23.6, PDFs: 500, 21.20 pdf/sec
size: 439929 bytes
 - 17.0 times faster than original
 - 19.9% smaller PDF
*/

public class Perf2 {
    public static void main(String[] args) throws Exception {
        Perf2 perf = new Perf2();
//        perf.generatePdf("perf2-97bf4a99.pdf");
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

    void generatePdf(String saveToFile) throws Exception {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage();
            document.addPage(page);

            addTable(document, page);

            if (saveToFile != null) document.save(saveToFile);
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
        for (int rowIndex = 0; rowIndex < randomData.length; rowIndex++) {
            String[] colData = randomData[rowIndex];
            Row<PDPage> row = table.createRow(12f);
            for (int col = 0; col < colData.length; col++) {
                String colDatum = colData[col];
                Cell cell = row.createCell(12.5f, colDatum);
                if (col == 2) cell.setFillColor(Color.LIGHT_GRAY);
                else if (col == 4) cell.setFillColor(Color.CYAN);
                else if (col < 3 && rowIndex % 2 == 0) {
                    cell.setLeftBorderStyle(new LineStyle(Color.BLUE, 3f));
                    cell.setBottomBorderStyle(dashedRedThick);
                } else if (col == 6) {
                    cell.setTopBorderStyle(dotGreenMedium);
                    cell.setRightBorderStyle(dotGreenMedium);
                    cell.setBottomBorderStyle(dotGreenMedium);
                    cell.setLeftBorderStyle(dotGreenMedium);
                }
            }
        }
    }

    LineStyle dashedRedThick;
    LineStyle dotGreenMedium;

    String[][] randomData;

    Perf2() {
        randomData = new String[500][];
        for (int row = 0; row < randomData.length; row++) {
            String[] cols = new String[8];
            randomData[row] = cols;
            for (int col = 0; col < cols.length; col++) {
                cols[col] = randomCell();
            }
         }

        dashedRedThick = LineStyle.produceDashed(Color.RED, 5, new float[]{5f, 1f}, 5f);
        dotGreenMedium = LineStyle.produceDashed(Color.GREEN, 2, new float[]{1f}, 0);
    }

    static final String RANDOM_CHAR_SET = "abcdefghijklmnopqrstuvwxyz -.,;@:";
    Random random = new Random(177920);

    String randomCell() {
        StringBuilder sb = new StringBuilder();
        int listType = random.nextInt(20);
        if (listType == 0) randomList(sb, "ol");
        else if (listType == 1) randomList(sb, "ul");
        else randomString(sb);
        return sb.toString();
    }

    void randomList(StringBuilder sb, String listTag) {
        openTag(sb, listTag);
        int itemCount = random.nextInt(5);
        for (int i = 0; i < itemCount; i++) {
            openTag(sb, "li");
            randomString(sb);
            closeTag(sb, "li");
        }
        closeTag(sb, listTag);
    }

    void randomString(StringBuilder sb) {
        int length = 3 + random.nextInt(18);
        boolean bold = false;
        boolean italic = false;
        for (int i = 0; i < length; i++) {
            if (random.nextInt(20) == 0) {
                bold = !bold;
                if (bold) openTag(sb, "b");
                else closeTag(sb, "b");
            }
            if (random.nextInt(20) == 0) {
                italic = !italic;
                if (italic) openTag(sb, "i");
                else closeTag(sb, "i");
            }
            sb.append(RANDOM_CHAR_SET.charAt(random.nextInt(RANDOM_CHAR_SET.length())));
        }
        if (bold) closeTag(sb, "b");
        if (italic) closeTag(sb, "i");
    }

    static void openTag(StringBuilder sb, String tag) {
        sb.append("<").append(tag).append(">");
    }

    static void closeTag(StringBuilder sb, String tag) {
        sb.append("</").append(tag).append(">");
    }
}
