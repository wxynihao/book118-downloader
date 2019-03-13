package me.rainking;

import com.itextpdf.text.DocumentException;
import org.junit.Test;

import java.io.IOException;

/**
 * @author Rain
 * @date 2019/3/13
 */
public class DocumentBrowserTest {

    @Test
    public void downloadWholeDocument() throws IOException, DocumentException {
        DocumentBrowser browser = new DocumentBrowser();
        browser.downloadWholeDocument("5134312044001323");
    }
}