package me.rainking;

import com.itextpdf.text.pdf.PdfReader;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import static org.junit.Assert.assertEquals;

/**
 * @author Rain
 * @since 2026/8/20
 */
public class PdfGeneratorTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void onePagePerImageWithItsOwnSize() throws Exception {
        File dir = folder.newFolder("imgs");
        writeImage(dir, "1.png", 200, 100);
        writeImage(dir, "2.png", 100, 300);
        File pdf = new File(folder.getRoot(), "out.pdf");

        PdfGenerator.createPdf(dir.getPath(), pdf.getPath(), "png");

        PdfReader reader = new PdfReader(pdf.getPath());
        try {
            assertEquals(2, reader.getNumberOfPages());
            assertEquals(200f, reader.getPageSize(1).getWidth(), 0.5f);
            assertEquals(100f, reader.getPageSize(1).getHeight(), 0.5f);
            assertEquals(100f, reader.getPageSize(2).getWidth(), 0.5f);
            assertEquals(300f, reader.getPageSize(2).getHeight(), 0.5f);
        } finally {
            reader.close();
        }
    }

    @Test
    public void skipsNonPageFilesAndSortsNumerically() throws Exception {
        File dir = folder.newFolder("imgs");
        writeImage(dir, "1.png", 50, 50);
        writeImage(dir, "2.png", 60, 60);
        writeImage(dir, "10.png", 70, 70);
        writeImage(dir, ".DS_Store", 10, 10);
        writeImage(dir, "x.png", 80, 80);
        writeImage(dir, "3.gif", 90, 90);
        File pdf = new File(folder.getRoot(), "out.pdf");

        PdfGenerator.createPdf(dir.getPath(), pdf.getPath(), "png");

        PdfReader reader = new PdfReader(pdf.getPath());
        try {
            assertEquals(3, reader.getNumberOfPages());
            // 页码按数字排序：1、2、10，而非字典序的 1、10、2
            assertEquals(50f, reader.getPageSize(1).getWidth(), 0.5f);
            assertEquals(60f, reader.getPageSize(2).getWidth(), 0.5f);
            assertEquals(70f, reader.getPageSize(3).getWidth(), 0.5f);
        } finally {
            reader.close();
        }
    }

    private void writeImage(File dir, String name, int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        ImageIO.write(img, "png", new File(dir, name));
    }
}
