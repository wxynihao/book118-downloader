package me.rainking;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Rain
 * @since 2019/3/13
 */
public class DocumentBrowserTest {

    private final DocumentBrowser browser = new DocumentBrowser();

    /** 与线上 PREVIEW_DATA 实际格式一致的样本 */
    private static final String PREVIEW_PAGE =
        "var PREVIEW_DATA = { project_id: 1, aid: parseInt('188244482'), view_token: 'Bw_2kzdPgXts68EgVwn_mM4e8ALH_opi', aid_encode: '5134312044001323'}; //1:max站";

    /** 与线上 getPreview 接口实际格式一致的样本（JSONP包装、键名带引号、\/转义） */
    private static final String PREVIEW_API_BODY =
        "jsonpReturn({\"status\":200,\"message\":\"ok\",\"data\":{\"1\":\"\\/\\/h\\/a.png?v=1\",\"2\":\"\\/\\/h\\/b.png\"},"
            + "\"pages\":{\"preview\":\"101\",\"actual\":\"796\",\"filetype\":\"pdf\"}});";

    @Test
    public void parseDocumentId() {
        assertEquals("7124155114002001",
            DocumentBrowser.parseDocumentId("https://max.book118.com/html/2019/0113/7124155114002001.shtm"));
        assertEquals("113657916",
            DocumentBrowser.parseDocumentId("http://max.book118.com/html/2017/0611/113657916.shtm"));
        assertEquals("7124155114002001",
            DocumentBrowser.parseDocumentId("https://max.book118.com/html/2019/0113/7124155114002001.shtm?source=search#preview"));
        assertEquals("7124155114002001", DocumentBrowser.parseDocumentId("7124155114002001"));
        assertNull(DocumentBrowser.parseDocumentId("https://max.book118.com/html/2019/0113/no-id.shtm"));
        assertNull(DocumentBrowser.parseDocumentId(""));
    }

    @Test
    public void getJsonParsesObjectFromJsonpBody() {
        JSONObject data = DocumentBrowser.getJson(PREVIEW_API_BODY, "data");
        assertEquals("//h/a.png?v=1", data.getStr("1"));
        assertEquals("//h/b.png", data.getStr("2"));

        JSONObject pages = DocumentBrowser.getJson(PREVIEW_API_BODY, "pages");
        assertEquals("101", pages.getStr("preview"));
    }

    @Test
    public void getJsonSupportsNestedBraces() {
        JSONObject data = DocumentBrowser.getJson("{\"data\":{\"1\":\"x\",\"extra\":{\"a\":1}}}", "data");
        assertEquals(2, data.size());
    }

    @Test
    public void getJsonReturnsNullOnMissingOrBrokenContent() {
        assertNull(DocumentBrowser.getJson("{\"pages\":{}}", "data"));
        assertNull(DocumentBrowser.getJson(null, "data"));
        assertNull(DocumentBrowser.getJson("\"data\":{broken", "data"));
        // "data" 不会误匹配 "mydata" 这类同后缀键名
        assertNull(DocumentBrowser.getJson("{\"mydata\":{}}", "data"));
    }

    @Test
    public void getPreviewLimitReadsPreviewCount() {
        assertEquals(101, DocumentBrowser.getPreviewLimit(JSONUtil.parseObj("{\"preview\":\"101\",\"actual\":\"796\"}")));
        assertEquals(-1, DocumentBrowser.getPreviewLimit(JSONUtil.parseObj("{\"actual\":\"796\"}")));
        assertEquals(-1, DocumentBrowser.getPreviewLimit(JSONUtil.parseObj("{\"preview\":\"abc\"}")));
        assertEquals(-1, DocumentBrowser.getPreviewLimit(null));
    }

    @Test
    public void parsePreviewDataFields() {
        int pos = PREVIEW_PAGE.indexOf("PREVIEW_DATA");
        assertEquals(Integer.valueOf(1), browser.parseProjectId(PREVIEW_PAGE, pos));
        assertEquals("188244482", browser.parseStrInPreviewData(PREVIEW_PAGE, "aid", pos));
        assertEquals("Bw_2kzdPgXts68EgVwn_mM4e8ALH_opi", browser.parseStrInPreviewData(PREVIEW_PAGE, "view_token", pos));
        assertEquals("5134312044001323", browser.parseStrInPreviewData(PREVIEW_PAGE, "aid_encode", pos));
    }

    @Test
    public void parsePreviewDataReturnsNullWhenFieldAbsent() {
        String page = "var PREVIEW_DATA = { project_id: 2 };";
        int pos = page.indexOf("PREVIEW_DATA");
        assertEquals(Integer.valueOf(2), browser.parseProjectId(page, pos));
        assertNull(browser.parseStrInPreviewData(page, "aid", pos));
        assertNull(browser.parseStrInPreviewData(page, "aid_encode", pos));
        assertNull(browser.parseProjectId("var PREVIEW_DATA = { aid: 1 };", "PREVIEW_DATA".length() - 2));
    }

    @Test
    @Ignore("集成测试：访问真实网站并下载整个预览，需要时手动移除本注解执行")
    public void downloadWholeDocument() {
        browser.downloadWholeDocument("5134312044001323");
        assertTrue(new File("out/5134312044001323.pdf").exists());
    }
}
