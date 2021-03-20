package me.rainking;

import org.junit.Test;

/**
 * @author Rain
 * @since 2019/3/13
 */
public class DocumentBrowserTest {

    @Test
    public void downloadWholeDocument() {
        DocumentBrowser browser = new DocumentBrowser();
        browser.downloadWholeDocument("5134312044001323");
    }
}