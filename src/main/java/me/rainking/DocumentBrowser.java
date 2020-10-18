package me.rainking;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.io.file.FileWriter;
import cn.hutool.json.JSONObject;
import com.itextpdf.text.DocumentException;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.StaticLog;

/**
 * @author Rain
 * @description 文档浏览，包含获取起始页和全部预览图的方法
 * @date 2018/3/7 19:26
 */
class DocumentBrowser {

    private static final String TEMP_PATH = "./temp";
    private static final String DES_PATH = "./out";

    private static final String TASK_LIST_FILE = DES_PATH + "/tasklist.txt";
    private static final String FILE_DOWNLOAD_PAGE = TEMP_PATH + "/%s/page.txt";

    List<String> readTaskList() {
        List<String> aTaskDocumentId = new ArrayList<>();
        if (FileUtil.exist(TASK_LIST_FILE)) {
            FileReader fileReader = new FileReader(TASK_LIST_FILE);
            aTaskDocumentId = fileReader.readLines().stream()
                    .map(StrUtil::trim).filter(StrUtil::isNotBlank).collect(Collectors.toList());
        }
        return aTaskDocumentId;
    }

    void writeTaskList(List<String> pLists) {
        if (!FileUtil.exist(DES_PATH)) {
            FileUtil.mkdir(DES_PATH);
        }
        FileWriter fileWriter = new FileWriter(TASK_LIST_FILE);
        fileWriter.appendLines(pLists);
    }

    private int readDownloadedPage(String sDocumentId) {
        int nPage = 1;
        String filePath = String.format(FILE_DOWNLOAD_PAGE, sDocumentId);
        if (FileUtil.exist(filePath)) {
            FileReader fileReader = new FileReader(filePath);
            String sPage = fileReader.readString();
            nPage = Integer.parseInt(sPage);
        }
        return nPage;
    }

    private void writeDownloadedPage(String sDocumentId, int nPage) {
        String filePath = String.format(FILE_DOWNLOAD_PAGE, sDocumentId);
        FileWriter fileWriter = new FileWriter(filePath);
        fileWriter.write(String.valueOf(nPage));
    }

    private String moveToNextPage(PdfInfo pInfo) {
        String sNextPage = getNextPage(pInfo);
        try {
            pInfo.setImg(URLEncoder.encode(sNextPage, "utf8"));
        } catch (Exception e) {
            StaticLog.error("moveToNextPage error", e);
        }
        return sNextPage;
    }

    /**
     * 下载文档的全部图片
     *
     * @param documentId 文档编号
     * @throws IOException       pdf创建错误
     * @throws DocumentException pdf创建错误
     */
    void downloadWholeDocument(String documentId) throws IOException, DocumentException {
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
            downloadFile("http:" + entry.getValue(), srcPath + "/" + autoGenericCode(Integer.parseInt(entry.getKey())) + ".gif");
            currentDownPage.append("\r").append(String.format("已下载页数：[%s] 页", ++i));
            System.out.print(currentDownPage);
        }

        StaticLog.info("\n开始生成...");
        PdfGenerator.creatPDF(srcPath, DES_PATH + "/" + documentId + ".pdf", "gif");
        FileUtil.del(new File(srcPath));
        StaticLog.info("\n生成完成");
    }

    /**
     * 将数字字符串的左边补充0，使其长度达到指定长度
     *
     * @param number 需要处理的数字
     * @return 通过填充0达到长度的数字字符串
     */
    private String autoGenericCode(int number) {
        return String.format("%0" + Constants.MAX_BIT_OF_PAGE + "d", number);
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
        String url = Constants.PREVIEW_URL + documentId;
        String previewDataFull = HttpUtil.get(url);
        int pos = previewDataFull.indexOf("PREVIEW_DATA");
        Integer pId = parseProjectId(previewDataFull, pos);
        String aid = parseStrInPreviewData(previewDataFull, "aid", pos);
        String viewToken = parseStrInPreviewData(previewDataFull, "view_token", pos);
        String aidEncode = parseStrInPreviewData(previewDataFull, "aid_encode", pos);
        return StrUtil.format(Constants.PIC_LINK_URL, pId, aid, viewToken, aidEncode);
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
     * 获取文档的预览地址
     *
     * @param documentId 文档的编号
     * @return 预览地址
     */
    private PdfInfo getPdfInfo(String documentId) {
        String url = Constants.OPEN_FULL_URL + documentId;
        String pdfPageUrlStr = HttpUtil.get(url);

        if (StrUtil.isNotBlank(pdfPageUrlStr) && pdfPageUrlStr.startsWith("//")) {
            pdfPageUrlStr = "https:" + pdfPageUrlStr;
        } else {
            StaticLog.error("获取失败！");
            return null;
        }

        int endOfHost = pdfPageUrlStr.indexOf("?");
        String viewHost = pdfPageUrlStr.substring(0, endOfHost);
        String redirectPage = HttpUtil.get(pdfPageUrlStr);
        String href = ReUtil.get(Constants.HREF_PATTERN, redirectPage, 1);
        String fullUrl;
        if (href != null) {
            fullUrl = viewHost.substring(0, viewHost.length() - 1) + HtmlUtil.unescape(href);
        } else {
            fullUrl = pdfPageUrlStr;
        }


        String pdfPageHtml = HttpUtil.get(fullUrl);
        if (pdfPageHtml.contains(Constants.FILE_NOT_EXIST)) {
            StaticLog.error("获取预览地址失败，请稍后再试！");
            return null;
        }

        List<String> result = ReUtil.findAllGroup0(Constants.INPUT_PATTERN, pdfPageHtml);
        Map<String, String> pdfInfoMap = new HashMap<>(6);
        pdfInfoMap.put("host", viewHost);
        for (String inputStr : result) {
            String id = ReUtil.get(Constants.ID_PATTERN, inputStr, 1);
            String value = ReUtil.get(Constants.VALUE_PATTERN, inputStr, 1);
            if (StrUtil.isNotBlank(id) && StrUtil.isNotBlank(value)) {
                id = id.toLowerCase();
                try {
                    value = URLEncoder.encode(value, "utf8");
                } catch (Exception e) {
                    StaticLog.error("URLEncoder Error", e);
                }
                pdfInfoMap.put(id, value);
            }
        }
        return BeanUtil.mapToBean(pdfInfoMap, PdfInfo.class, true);
    }

    private String getNextPage(PdfInfo pdfInfo) {
        String nextUrl = pdfInfo.getNextUrl();
        String nextPageJson = HttpUtil.get(nextUrl);
        PageInfo pageInfo = JSONUtil.toBean(nextPageJson, PageInfo.class);
        return pageInfo.getNextPage();
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

    public static void main(String[] args) {
        DocumentBrowser documentBrowser = new DocumentBrowser();
        String url = documentBrowser.getPreviewData("5032121100002141");
        System.out.println(documentBrowser.getPicUrl(url));
    }
}
