package me.rainking;

import com.itextpdf.text.DocumentException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;

import java.io.*;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author Rain
 * @description 文档浏览，包含获取起始页和全部预览图的方法
 * @date 2018/3/7 19:26
 */
public class DocumentBrowser {

    /**
     * 匹配页面中隐藏的input的值，用于获取文档信息
     */
    private static Pattern inputPattern = Pattern.compile("<input type=\"hidden\" id=\"(.*?)\" value=\"(.*?)\".*?/>");
    /**
     * 用于获取input标签中的id
     */
    private static Pattern idPattern = Pattern.compile(".*id=\"(.*?)\"");
    /**
     * 用于获取input标签中的value
     */
    private static Pattern valuePattern = Pattern.compile(".*value=\"(.*?)\"");
    /**
     * 在请求下一页的信息时，从返回的json数据中获取下一页的编号
     */
    private static Pattern nextPagePattern = Pattern.compile("NextPage\":\"(.*?)\",");


    private static final int MAX_RETRY_TIMES = 5;


    /**
     * 下载文档的全部图片
     *
     * @param documentId 文档编号
     * @throws IOException       pdf创建错误
     * @throws DocumentException pdf创建错误
     */
    public void downloadWholeDocument(String documentId) throws IOException, DocumentException {
        List<String> imgUrlList = getImgUrlOfDocument(documentId);

        String srcPath = "./temp/" + documentId;
        String desPath = "./out";

        mkDirectory(srcPath);
        mkDirectory(desPath);

        int sizeOfBook = imgUrlList.size();
        int page = 1;
        System.out.println("开始下载...");
        for (String imgUrl : imgUrlList) {
            downloadFile(imgUrl, srcPath + "/" + autoGenericCode(page, 3) + ".gif");
            System.out.println(page + "/" + sizeOfBook);
            page++;
        }

        System.out.println("开始生成...");

        PdfGenerator.creatPDF(srcPath, desPath + "/" + documentId + ".pdf");

        deleteDir(new File(srcPath));

    }

    /**
     * 递归删除目录下的所有文件及子目录下所有文件
     *
     * @param dir 将要删除的文件目录
     * @return boolean Returns "true" if all deletions were successful.
     * If a deletion fails, the method stops attempting to
     * delete and returns "false".
     */
    private static boolean deleteDir(File dir) {
        if (dir.isDirectory()) {
            String[] children = dir.list();
            //递归删除目录中的子目录下
            for (String aChildren : children) {
                boolean success = deleteDir(new File(dir, aChildren));
                if (!success) {
                    return false;
                }
            }
        }
        // 目录此时为空，可以删除
        return dir.delete();
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
     * 文件夹不存在时创建文件夹
     *
     * @param path 文件夹路径
     * @return 创建是否成功
     */
    private static boolean mkDirectory(String path) {
        try {
            File file = new File(path);
            return !file.exists() && file.mkdirs();
        } catch (Exception e) {
            System.out.println("文件夹创建错误，请尝试使用管理员权限运行！");
        }
        return false;
    }

    /**
     * 获取文档的全部预览图片地址
     *
     * @param documentId 文档的编号
     * @return 全部图片地址
     */
    public List<String> getImgUrlOfDocument(String documentId) {

        List<String> imgUrlList = new ArrayList<>(128);

        Map<String, String> pdfInfo = getPdfInfo(documentId);

        String urlPrefix = pdfInfo.get("host") + "img/?img=";

        String nextPage = "";

        System.out.println("开始获取链接，请耐心等待...");
        while (!"ReadLimit".equals(nextPage)) {
            nextPage = getNextPage(pdfInfo);
            if (!"ReadLimit".equals(nextPage)) {
                imgUrlList.add(urlPrefix + nextPage);
                System.out.println("获取到第" + imgUrlList.size() + "页。");
            }

            try {
                pdfInfo.put("img", URLEncoder.encode(nextPage, "utf8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }


        return imgUrlList;
    }

    /**
     * 获取文档的预览地址
     *
     * @param documentId 文档的编号
     * @return 预览地址
     */
    private Map<String, String> getPdfInfo(String documentId) {

        String url = "https://max.book118.com/index.php?g=Home&m=View&a=viewUrl&flag=1&cid=" + documentId;
        String pdfPageUrlStr = getUrlContent(url);

        if ("".equals(pdfPageUrlStr)) {
            return null;
        } else {
            pdfPageUrlStr = "https:" + pdfPageUrlStr;
        }

        String pdfPageHtml = getUrlContent(pdfPageUrlStr);
        if (pdfPageHtml.contains("文件不存在")) {
            System.out.println("获取预览地址失败，请稍后再试！");
            return null;
        }

        Matcher matcher = inputPattern.matcher(pdfPageHtml);
        List<String> result = new ArrayList<>();
        while (matcher.find()) {
            result.add(matcher.group());
        }

        Map<String, String> pdfInfo = new HashMap<>();

        for (String inputStr : result) {
            Matcher idMatcher = idPattern.matcher(inputStr);
            String id = "";
            while (idMatcher.find()) {
                id = idMatcher.group(1).toLowerCase();
            }

            Matcher valueMatcher = valuePattern.matcher(inputStr);
            String value = "";
            while (valueMatcher.find()) {
                try {
                    value = URLEncoder.encode(valueMatcher.group(1), "utf8");
                } catch (UnsupportedEncodingException e) {
                    e.printStackTrace();
                }
            }

            pdfInfo.put(id, value);
        }

        int posOfQue = pdfPageUrlStr.indexOf("?");
        pdfInfo.put("host", pdfPageUrlStr.substring(0, posOfQue));


        return pdfInfo;
    }

    /**
     * 获取下一页的图片地址编号
     *
     * @param pdfInfo pdf的一些信息
     * @return 下一页的编号
     */
    private String getNextPage(Map<String, String> pdfInfo) {

        String nextPageUrl = pdfInfo.get("host") + "/pdf/GetNextPage/?";

        String pdfInfoStr = "f=" + pdfInfo.get("url")
                + "&img=" + pdfInfo.get("img")
                + "&isMobile=false"
                + "&isNet=True"
                + "&readLimit=" + pdfInfo.get("readlimit")
                + "&furl=" + pdfInfo.get("furl");

        nextPageUrl = nextPageUrl + pdfInfoStr;
        String nextPageJson = getUrlContent(nextPageUrl);

        Matcher nextPageMatcher = nextPagePattern.matcher(nextPageJson);
        String nextPage = "";
        while (nextPageMatcher.find()) {
            nextPage = nextPageMatcher.group(1);
        }

        return nextPage;
    }

    /**
     * 下载文件到本地
     *
     * @param url       文件的 url
     * @param localPath 本地存储路径
     * @return 下载是否成功
     */
    public boolean downloadFile(String url, String localPath) {

        if (sendGet(url) == null) {
            return false;
        }

        boolean isDownloaded = false;

        byte[] content = sendGet(url).asBytes();

        File file = new File(localPath);
        try {
            BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file));
            out.write(content);
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (file.exists() && file.length() != 0) {
            isDownloaded = true;
        }

        return isDownloaded;
    }

    /**
     * 获取请求的内容
     *
     * @param url 访问的链接
     * @return 服务端返回的结果的字符串
     */
    private String getUrlContent(String url) {
        String urlContent = "";
        Content content = sendGet(url);
        if (content != null) {
            urlContent = content.toString();
        }
        return urlContent;
    }

    /**
     * 使用httpclient流式API中的方法执行Get请求
     *
     * @param url 请求的url
     * @return 服务端返回的内容
     */
    public Content sendGet(String url) {
        Content content = null;

        int retryTimes = 0;

        while (content == null && retryTimes != MAX_RETRY_TIMES) {
            try {
                retryTimes++;
                content = Request.Get(url).connectTimeout(50000).socketTimeout(50000).execute().returnContent();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        if (content == null) {
            System.out.println(url + "下载失败！");
        }

        return content;
    }
}
