package me.rainking;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

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

    String sTempPath = "./temp";
    String sDesPath = "./out";
    String sFileNameTaskList = "tasklist.txt";
    String sFileNameDownloadedPage = "page.txt";

    List<String> readTaskList() {
        List<String> aTaskDocumentId = new ArrayList<>();
        String sTaskListPath = sDesPath + "/" + sFileNameTaskList;
        File pFile = new File(sTaskListPath);
        String sLine = null;

        if (pFile.exists()) {
            BufferedReader pReader;
            try {
                pReader = new BufferedReader(new FileReader(pFile));
                while ((sLine = pReader.readLine()) != null) {
                    sLine = sLine.trim();
                    if (sLine.length() > 0) { aTaskDocumentId.add(sLine); }
                }
                pReader.close();
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        return aTaskDocumentId;
    }

    void writeTaskList(List<String> pLists) {
        String sTaskListPath = sDesPath + "/" + sFileNameTaskList;
        File pFile = new File(sTaskListPath);
        if (! pFile.exists()) {
            FileUtil.mkdir(sDesPath);
        }
        try {
            BufferedOutputStream pWriter = new BufferedOutputStream(new FileOutputStream(sTaskListPath));
            for (String sDocumentId : pLists) {
                pWriter.write(sDocumentId.getBytes());
                pWriter.write('\n');
            }
            pWriter.close();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }

    int readDownloadedPage(String sDocumentId) {
        int nPage = 1;
        File pFile = new File(sTempPath + "/" + sDocumentId + "/" + sFileNameDownloadedPage);
        if (pFile.exists()) {
            try {
                Scanner pSc = new Scanner(pFile);
                if (pSc.hasNextInt()) { nPage = pSc.nextInt(); }
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }
        return nPage;
    }

    void writeDownloadedPagge(String sDocumentId, int nPage) {
        File pFile = new File(sTempPath + "/" + sDocumentId + "/" + sFileNameDownloadedPage);
        try {
            BufferedOutputStream pOut = new BufferedOutputStream(new FileOutputStream(pFile));
            pOut.write(String.valueOf(nPage).getBytes());
            pOut.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    String moveToNextPage(PdfInfo pInfo) {
        String sNextPage = getNextPage(pInfo);
        try {
            pInfo.setImg(URLEncoder.encode(sNextPage, "utf8"));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
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
        String srcPath = sTempPath + "/" + documentId;
        FileUtil.mkdir(new File(srcPath));
        FileUtil.mkdir(new File(sDesPath));

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
        System.out.println("\n开始下载...");
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
                writeDownloadedPagge(documentId, page);                             // 保存当前下载完成页码
                page++;
            } else {
                break;
            }
        }
        System.out.println("\n开始生成...");
        PdfGenerator.creatPDF(srcPath, sDesPath + "/" + documentId + ".pdf");
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
                } catch (UnsupportedEncodingException e) {
                    StaticLog.error(e.getMessage());
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
