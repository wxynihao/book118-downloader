package me.rainking;

import com.itextpdf.text.DocumentException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;

/**
 * @description:
 * @author: Rain
 * @date: 2018/3/7 9:44
 */
public class DocumentBrowserTest {

    DocumentBrowser browser = new DocumentBrowser();

    /**
     * PDF信息获取测试
     */
    @Test
    public void getImgUrlOfDocument() {

        System.out.println(browser.getImgUrlOfDocument("150266376"));
    }

    @Test
    public void getUrlContent(){
        String documentId = "150266376";
        String url = "https://max.book118.com/index.php?g=Home&m=View&a=viewUrl&flag=1&cid=" + documentId;

        System.out.println(browser.sendGet(url));
    }


    /**
     * 文件下载测试
     */
    @Test
    public void downloadFile() {
        String imgUrl = "http://view43.book118.com/img/?img=Hs92T42xAvsvX7Ls@qzOaU7OQNoC9hgWLXXXUwhA05Rrwn@gruSWbmGX8hJLL8TBpVSehhPgrrw=";
        DocumentBrowser browser = new DocumentBrowser();
        Assert.assertTrue(browser.downloadFile(imgUrl, "img.gif"));
    }

    @Test
    public void downloadWholeDocument() throws IOException, DocumentException {
        DocumentBrowser browser = new DocumentBrowser();
        browser.downloadWholeDocument("150266376");
    }

}