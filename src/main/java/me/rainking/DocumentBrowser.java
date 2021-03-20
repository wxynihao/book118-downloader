package me.rainking;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.StaticLog;
import com.itextpdf.text.DocumentException;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 文档浏览，包含获取起始页和全部预览图的方法
 *
 * @author Rain
 * @since 2018/3/7 19:26
 */
class DocumentBrowser {

    /**
     * 下载文档的全部图片
     *
     * @param documentId 文档编号
     * @throws IOException       pdf创建错误
     * @throws DocumentException pdf创建错误
     */
    void downloadWholeDocument(String documentId) {
        String srcPath = TEMP_PATH + "/" + documentId;
        FileUtil.mkdir(new File(srcPath));
        FileUtil.mkdir(new File(DES_PATH));

        StaticLog.info("\n开始解析...");
        String url = getPreviewData(documentId);
        Map<String, String> pageAndUrl = getPicUrl(url);
        StaticLog.info("\n解析完成，共{}页", pageAndUrl.size());

        StringBuilder currentDownPage = new StringBuilder();
        StaticLog.info("\n开始下载...");
        int i = 0;
        for (Map.Entry<String, String> entry : pageAndUrl.entrySet()) {
            downloadFile("http:" + entry.getValue(), srcPath + "/" + Integer.parseInt(entry.getKey()) + ".gif");
            currentDownPage.append("\r").append(String.format("已下载页数：[%s] 页", ++i));
            System.out.print(currentDownPage);
        }

        StaticLog.info("\n开始生成...");
        try {
            PdfGenerator.creatPDF(srcPath, DES_PATH + "/" + documentId + ".pdf", "gif");
        } catch (IOException e) {
            StaticLog.error("\nIOException...");
            e.printStackTrace();
        } catch (DocumentException e) {
            StaticLog.error("\nDocumentException...");
            e.printStackTrace();
        }
        FileUtil.del(new File(srcPath));
        StaticLog.info("\n生成完成");
    }

    private Map<String, String> getPicUrl(String baseUrl) {
        Map<String, String> pageNumAndUrl = new HashMap<>();
        // 第一次获取，解析总页数
        int page = 1;
        int step = 6;
        String firstGet = HttpUtil.get(baseUrl + page);
        JSONObject data = getJson(firstGet, "data");
        data.forEach((k, v) -> pageNumAndUrl.put(k, v.toString()));
        JSONObject pages = getJson(firstGet, "pages");
        int limit = getPreviewLimit(pages);
        StaticLog.info("\n共需解析{}页", limit);
        if (limit > step) {
            for (int i = page + step; i < limit; ) {
                StaticLog.info("\n解析至第{}页", i);
                // 必须休眠，否则获取不到结果
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                String getContent = HttpUtil.get(baseUrl + i);
                JSONObject dataContent = getJson(getContent, "data");
                // 未获取到则进行重试，直至获取到
                if (!"".equals(dataContent.getStr(String.valueOf(i)))) {
                    i += step;
                    dataContent.forEach((k, v) -> pageNumAndUrl.put(k, v.toString()));
                }
            }
        }
        return pageNumAndUrl;
    }

    //  "pages": {"preview": "50", "actual": "796", "filetype": "pdf"}
    private int getPreviewLimit(JSONObject pages) {
        String limitStr = pages.getStr("preview");
        return Integer.parseInt(limitStr);
    }

    private JSONObject getJson(String all, String key) {
        int pos = all.indexOf(key);
        int start = all.indexOf("{", pos + 1);
        int end = all.indexOf("}", start + 1);
        return JSONUtil.parseObj(all.substring(start, end + 1));
    }

    private String getPreviewData(String documentId) {
        String url = PREVIEW_URL + documentId;
        String previewDataFull = HttpUtil.get(url);
        int pos = previewDataFull.indexOf("PREVIEW_DATA");
        Integer pId = parseProjectId(previewDataFull, pos);
        String aid = parseStrInPreviewData(previewDataFull, "aid", pos);
        String viewToken = parseStrInPreviewData(previewDataFull, "view_token", pos);
        String aidEncode = parseStrInPreviewData(previewDataFull, "aid_encode", pos);
        return StrUtil.format(PIC_LINK_URL, pId, aid, viewToken, aidEncode);
    }

    private int parseProjectId(final String previewDataFull, int pos) {
        int keyPos = previewDataFull.indexOf("project_id", pos);
        int start = previewDataFull.indexOf(":", keyPos + 1);
        int end = previewDataFull.indexOf(",", start + 1);
        return Integer.parseInt(previewDataFull.substring(start + 1, end).trim());
    }

    private String parseStrInPreviewData(final String previewDataFull, String key, int pos) {
        int keyPos = previewDataFull.indexOf(key, pos);
        if (keyPos == -1) {
            return null;
        }
        int start = previewDataFull.indexOf("'", keyPos + 1);
        int end = previewDataFull.indexOf("'", start + 1);
        return previewDataFull.substring(start + 1, end);
    }

    /**
     * 下载文件到本地
     *
     * @param url       文件的 url
     * @param localPath 本地存储路径
     */
    private void downloadFile(String url, String localPath) {
        File file = new File(localPath);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file); BufferedOutputStream out = new BufferedOutputStream(fileOutputStream)) {
            HttpUtil.download(url, out, true);
        } catch (IOException e) {
            StaticLog.error(e.getMessage());
        }
    }

    private static final String PREVIEW_URL = "https://max.book118.com/index.php?g=Home&m=NewView&a=index&aid=";

    private static final String PIC_LINK_URL = "https://openapi.book118.com/getPreview.html?project_id={}&aid={}&view_token={}&aid_encode={}&page=";

    private static final String TEMP_PATH = "./temp";

    private static final String DES_PATH = "./out";
}
