package me.rainking;

import com.itextpdf.text.DocumentException;
import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.fluent.Content;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.ContentType;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import java.io.*;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @description: 文档浏览，包含获取起始页和全部预览图的方法
 * @author: Rain
 * @date: 2018/3/7 19:26
 */
public class DocumentBrowser {

    private static Pattern inputPattern = Pattern.compile("<input type=\"hidden\" id=\"(.*?)\" value=\"(.*?)\".*?/>");
    private static Pattern idPattern = Pattern.compile(".*id=\"(.*?)\"");
    private static Pattern valuePattern = Pattern.compile(".*value=\"(.*?)\"");
    private static Pattern nextPagePattern = Pattern.compile("NextPage\":\"(.*?)\",");


    public void downloadWholeDocument(String documentId) throws IOException, DocumentException {
        List<String> imgUrlList = getImgUrlOfDocument(documentId);

        String srcPath = "./temp/" + documentId;
        String desPath = "./out";

        mkDirectory(srcPath);
        mkDirectory(desPath);

        int sizeOfBook = imgUrlList.size();
        int page = 1;
        for (String imgUrl : imgUrlList) {
            downloadFile(imgUrl, srcPath + "/" + autoGenericCode(page, 3) + ".gif");
            System.out.println(page + "/" + sizeOfBook);
            page++;
        }

        PdfGenerator.creatPDF(srcPath, desPath + "/" + documentId + ".pdf");
    }

    private String autoGenericCode(int number, int width) {
        String result = "";
        result = String.format("%0" + width + "d", number );

        return result;
    }

    public static boolean mkDirectory(String path) {
        File file = null;
        try {
            file = new File(path);
            if (!file.exists()) {
                return file.mkdirs();
            } else {
                return false;
            }
        } catch (Exception e) {
        } finally {
            file = null;
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

        while (!nextPage.equals("ReadLimit")) {
            nextPage = getNextPage(pdfInfo);
            if (!nextPage.equals("ReadLimit")) {
                imgUrlList.add(urlPrefix + nextPage);
            }

            try {
                pdfInfo.put("img", URLEncoder.encode(nextPage, "utf8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

        }


        return imgUrlList;
    }

//    final String requrl = "//view43.book118.com/?readpage=A0uyuIYdYhay1fq@KX_aEg==&furl=YOQStEpojXCy0xJO83bqoAVYhrL_yFZqP4Ozsj8XfKNfzsc8uPp1xb6rEMxHNfHaLhdOot8ZnYZW9dtUywtqpMfl@dBGXLrSSZrkJBCQKDaSlrKKrCXYvw==&n=1";

    /**
     * 获取文档的预览地址
     *
     * @param documentId 文档的编号
     * @return 预览地址
     */
    public Map<String, String> getPdfInfo(String documentId) {

        String url = "https://max.book118.com/index.php?g=Home&m=View&a=viewUrl&flag=1&cid=" + documentId;
        String pdfPageUrlStr = getUrlContent(url);

//        pdfPageUrlStr = requrl;

        if (pdfPageUrlStr.equals("")) {
            return null;
        } else {
            pdfPageUrlStr = "https:" + pdfPageUrlStr;
        }

        String pdfPageHtml = getUrlContent(pdfPageUrlStr);
        if (pdfPageHtml.contains("文件不存在")) {
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
     * @param pdfInfo
     * @return
     */
    public String getNextPage(Map<String, String> pdfInfo) {

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
    public String getUrlContent(String url) {
        String urlContent = "";
        if (sendGet(url) != null) {
            urlContent = sendGet(url).toString();
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
        try {
            content = Request.Get(url).connectTimeout(50000).socketTimeout(50000).execute().returnContent();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

    public static String httpGet(String url) {

        String result = "";

        // 创建一个客户端
        CloseableHttpClient httpclient = HttpClients.createDefault();
        // 创建get方法
        HttpGet httpget = new HttpGet(url);

        CloseableHttpResponse response = null;
        try {
            response = httpclient.execute(httpget);
            int statuscode = response.getStatusLine().getStatusCode();
            if (statuscode == HttpStatus.SC_OK) {
                HttpEntity entity = response.getEntity();
                if (entity != null) {

                    ContentType contentType = ContentType.getOrDefault(entity);
                    Charset charset = contentType.getCharset();
                    result = EntityUtils.toString(entity, charset);
                }
            }
            response.close();

        } catch (ClientProtocolException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            result = e.getMessage().toString();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
            result = e.getMessage().toString();
        }

        return result;

    }

    private static final CloseableHttpClient httpclient = HttpClients.createDefault();
    private static final String userAgent = "Mozilla/5.0 (Windows NT 6.2; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/54.0.2840.87 Safari/537.36";

    /**
     * 发送HttpGet请求
     *
     * @param url 请求地址
     * @return 返回字符串
     */
    public static String sendGetReq(String url) {
        String result = null;
        CloseableHttpResponse response = null;
        try {
            HttpGet httpGet = new HttpGet(url);
            httpGet.setHeader("User-Agent", userAgent);
            response = httpclient.execute(httpGet);
            HttpEntity entity = response.getEntity();
            if (entity != null) {
                result = EntityUtils.toString(entity);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (response != null) {
                try {
                    response.close();
                } catch (IOException e) {
                }
            }
        }
        return result;
    }


}
