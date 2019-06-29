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
            nPage = Integer.valueOf(sPage);
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
     *  下载文档的全部图片
     *
     * @param documentId 文档编号
     * @throws IOException       pdf创建错误
     * @throws DocumentException pdf创建错误
     */
    void downloadWholeDocument(String documentId) throws IOException, DocumentException {
        String srcPath = TEMP_PATH + "/" + documentId;
        FileUtil.mkdir(new File(srcPath));
        FileUtil.mkdir(new File(DES_PATH));

        int page = 1, nDownloadedPage;
        // 断点下载
        nDownloadedPage = readDownloadedPage(documentId);
        if (nDownloadedPage != 1) {
            System.out.println(String.format("下载继续，当前已完成 %d 页", nDownloadedPage));
            nDownloadedPage ++;
        }
        StringBuilder currentDownPage = new StringBuilder();
        PdfInfo pdfInfo = getPdfInfo(documentId);
        String imgUrl;
        StaticLog.info("\n开始下载...");
        while (pdfInfo != null) {
            String nextPage = moveToNextPage(pdfInfo);
            if (!Constants.TAG_OF_END.contains(nextPage)) {
                //跳过已下载的文件
                if (page < nDownloadedPage) {
                    System.out.print(String.format("\r当前页码: [%d]  已跳过", page));
                    page ++; continue;
                }
                imgUrl = (pdfInfo.getHost() + Constants.IMG_PREFIX_URL + nextPage);
                downloadFile(imgUrl, srcPath + "/" + autoGenericCode(page, Constants.MAX_BIT_OF_PAGE) + ".gif");
                currentDownPage.append("\r").append(String.format("已下载页数：[%d] 页", page));
                System.out.print(currentDownPage);
                // 保存当前下载完成页码
                writeDownloadedPage(documentId, page);
                page++;
            } else {
                break;
            }
        }
        StaticLog.info("\n开始生成...");
        PdfGenerator.creatPDF(srcPath, DES_PATH + "/" + documentId + ".pdf", "gif");
        FileUtil.del(new File(srcPath));
    }

    /**
     * 将数字字符串的左边补充0，使其长度达到指定长度
     *
     * @param number 需要处理的数字
     * @param width  补充后字符串长度
     * @return 通过填充0达到长度的数字字符串
     */
    private String autoGenericCode(int number, int width) {
        return String.format("%0" + width + "d", number);
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

        if (StrUtil.isNotBlank(pdfPageUrlStr)) {
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
        if(href != null){
            fullUrl = viewHost.substring(0, viewHost.length()-1) + HtmlUtil.unescape(href);
        }else {
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
}
