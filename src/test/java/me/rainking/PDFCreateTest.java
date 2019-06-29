package me.rainking;

import com.itextpdf.text.DocumentException;
import org.junit.Test;

import java.io.IOException;

public class PDFCreateTest {
    @Test
    public void createPDF() throws IOException, DocumentException {
        DocumentBrowser browser = new DocumentBrowser();
//        browser.downloadWholeDocument("7101132125001164");

        PdfGenerator.creatPDF("./temp/7101132125001164", "./out/7101132125001164.pdf", "gif");
    }
}
