package me.rainking;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONException;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.StaticLog;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 文档浏览，包含获取起始页和全部预览图的方法
 *
 * @author Rain
 * @since 2018/3/7 19:26
 */
class DocumentBrowser {

    /** 预览接口每次请求返回的页数 */
    private static final int PARSE_STEP = 6;

    /** 同一批次预览数据获取失败的最大重试次数，超过后放弃后续解析 */
    private static final int MAX_PARSE_RETRY = 5;

    /** 相邻两次预览接口请求的间隔毫秒数，过快服务端不返回数据 */
    private static final long PARSE_INTERVAL_MS = 1000;

    /** HTTP 连接与读取超时毫秒数 */
    private static final int HTTP_TIMEOUT_MS = 30_000;

    /** 下载的预览图统一使用的后缀，PDF 生成时按文件内容识别实际图片格式 */
    private static final String PIC_SUFFIX = "png";

    /**
     * 下载文档的全部预览图并合成PDF（仅限网站开放预览的页数）
     *
     * @param documentIdOrUrl 文档编号或文档页链接
     */
    void downloadWholeDocument(String documentIdOrUrl) {
        String documentId = parseDocumentId(documentIdOrUrl);
        if (documentId == null) {
            StaticLog.error("\n无法从输入[{}]中解析出文档编号，请输入文档编号或文档页链接", documentIdOrUrl);
            return;
        }
        String srcPath = TEMP_PATH + "/" + documentId;
        FileUtil.mkdir(new File(srcPath));
        FileUtil.mkdir(new File(DES_PATH));
        String pdfPath = DES_PATH + "/" + documentId + ".pdf";
        try {
            StaticLog.info("\n开始解析...");
            String url = getPreviewData(documentId);
            if (url == null) {
                StaticLog.error("\n解析文档[{}]失败，页面中未找到有效的预览数据，可能是文档不支持预览或访问受限", documentId);
                return;
            }
            Map<String, String> pageAndUrl = getPicUrl(url);

            StaticLog.info("\n开始下载...");
            List<Map.Entry<String, String>> pages = new ArrayList<>(pageAndUrl.entrySet());
            pages.sort(Comparator.comparingInt(e -> Integer.parseInt(e.getKey())));
            int failed = 0;
            for (int i = 0; i < pages.size(); i++) {
                Map.Entry<String, String> entry = pages.get(i);
                if (!downloadFile(entry.getValue(), srcPath + "/" + entry.getKey() + "." + PIC_SUFFIX)) {
                    failed++;
                }
                System.out.print(String.format("\r已下载：[%d/%d] 失败：%d", i + 1, pages.size(), failed));
                System.out.flush();
            }
            System.out.println();
            if (failed > 0) {
                StaticLog.warn("\n共{}页下载失败，生成的PDF将缺少这些页", failed);
            }

            StaticLog.info("\n开始生成PDF...");
            PdfGenerator.createPdf(srcPath, pdfPath, PIC_SUFFIX);
            File pdfFile = new File(pdfPath);
            if (pdfFile.exists()) {
                StaticLog.info("\n生成完成：{}", pdfFile.getAbsolutePath());
            } else {
                StaticLog.error("\n生成失败：未产出PDF文件（可能没有成功下载任何一页）");
            }
        } catch (Exception e) {
            StaticLog.error("\n下载文档[{}]失败：{}", documentId, e.getMessage());
            StaticLog.error(e);
        } finally {
            FileUtil.del(new File(srcPath));
        }
    }

    private Map<String, String> getPicUrl(String baseUrl) {
        Map<String, String> pageNumAndUrl = new HashMap<>();
        // 第一次获取，解析总页数
        String firstGet = httpGet(baseUrl + 1);
        if (firstGet == null) {
            throw new IllegalStateException("请求第1页预览数据失败");
        }
        JSONObject data = getJson(firstGet, "data");
        if (data == null) {
            throw new IllegalStateException("预览返回中未找到data对象，返回格式可能已变化");
        }
        collectPages(data, pageNumAndUrl);

        int limit = getPreviewLimit(getJson(firstGet, "pages"));
        if (limit < 0) {
            throw new IllegalStateException("预览返回中未找到有效的pages.preview总页数，返回格式可能已变化");
        }
        StaticLog.info("\n共需解析{}页", limit);

        int retry = 0;
        for (int i = 1 + PARSE_STEP; i <= limit; ) {
            StaticLog.info("\n解析至第{}页", i);
            sleepBeforeNextRequest();
            if (Thread.currentThread().isInterrupted()) {
                StaticLog.warn("\n解析被中断，提前结束（已解析{}页/共{}页）", pageNumAndUrl.size(), limit);
                break;
            }
            JSONObject dataContent = null;
            String getContent = httpGet(baseUrl + i);
            if (getContent != null) {
                dataContent = getJson(getContent, "data");
            }
            // 本批起始页拿到数据才算成功；键不存在（null）与空串一样视为未获取到
            if (dataContent != null && StrUtil.isNotBlank(dataContent.getStr(String.valueOf(i)))) {
                collectPages(dataContent, pageNumAndUrl);
                i += PARSE_STEP;
                retry = 0;
            } else if (++retry >= MAX_PARSE_RETRY) {
                StaticLog.error("\n第{}页起的预览数据连续{}次未获取到，停止解析", i, MAX_PARSE_RETRY);
                break;
            }
        }
        if (pageNumAndUrl.size() < limit) {
            StaticLog.warn("\n解析到{}页，少于预览总页数{}，PDF将缺少部分页面", pageNumAndUrl.size(), limit);
        }
        return pageNumAndUrl;
    }

    /** 相邻请求之间必须休眠，否则服务端不返回数据 */
    private void sleepBeforeNextRequest() {
        try {
            Thread.sleep(PARSE_INTERVAL_MS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** 只收集页码为纯数字的条目，保证后续按页码排序和命名可行 */
    private static void collectPages(JSONObject data, Map<String, String> target) {
        data.forEach((k, v) -> {
            if (ReUtil.isMatch("^\\d+$", k)) {
                target.put(k, v.toString());
            }
        });
    }

    //  "pages": {"preview": "50", "actual": "796", "filetype": "pdf"}
    static int getPreviewLimit(JSONObject pages) {
        if (pages == null) {
            return -1;
        }
        String limitStr = pages.getStr("preview");
        if (limitStr == null) {
            return -1;
        }
        String trimmed = limitStr.trim();
        return ReUtil.isMatch("^\\d+$", trimmed) ? Integer.parseInt(trimmed) : -1;
    }

    /**
     * 在返回内容中定位 "key":{...} 形式（键名带引号，兼容JSONP包装）的对象并解析。
     * 按大括号配平截取，支持对象内嵌套。
     *
     * @return 解析结果；键不存在或内容不完整时返回 null
     */
    static JSONObject getJson(String content, String key) {
        if (StrUtil.isBlank(content)) {
            return null;
        }
        Matcher matcher = Pattern.compile("\"" + key + "\"\\s*:\\s*\\{").matcher(content);
        if (!matcher.find()) {
            return null;
        }
        int start = matcher.end() - 1;
        int depth = 0;
        for (int i = start; i < content.length(); i++) {
            char c = content.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                try {
                    return JSONUtil.parseObj(content.substring(start, i + 1));
                } catch (JSONException e) {
                    return null;
                }
            }
        }
        return null;
    }

    /**
     * 从输入中解析文档编号，支持直接粘贴文档页链接
     * 如 https://max.book118.com/html/2019/0113/7124155114002001.shtm 将解析为 7124155114002001
     *
     * @param input 文档编号或文档页链接
     * @return 文档编号，无法解析时返回 null
     */
    static String parseDocumentId(String input) {
        if (StrUtil.isBlank(input)) {
            return null;
        }
        // 去掉链接中的锚点和查询参数，再取最后一段（链接时为文件名，纯编号时为其本身）
        String s = input.trim();
        int anchor = s.indexOf('#');
        if (anchor >= 0) {
            s = s.substring(0, anchor);
        }
        int query = s.indexOf('?');
        if (query >= 0) {
            s = s.substring(0, query);
        }
        int slash = s.lastIndexOf('/');
        String lastPart = slash >= 0 ? s.substring(slash + 1) : s;
        String documentId = String.join("", ReUtil.findAll("\\d+", lastPart, 0));
        return documentId.isEmpty() ? null : documentId;
    }

    private String getPreviewData(String documentId) {
        String url = PREVIEW_URL + documentId;
        String previewDataFull = httpGet(url);
        if (previewDataFull == null) {
            return null;
        }
        int pos = previewDataFull.indexOf("PREVIEW_DATA");
        if (pos == -1) {
            return null;
        }
        Integer pId = parseProjectId(previewDataFull, pos);
        // aid 形如 parseInt('188244482')，按值两侧的单引号提取
        String aid = parseStrInPreviewData(previewDataFull, "aid", pos);
        String viewToken = parseStrInPreviewData(previewDataFull, "view_token", pos);
        String aidEncode = parseStrInPreviewData(previewDataFull, "aid_encode", pos);
        if (pId == null || aid == null || viewToken == null || aidEncode == null) {
            return null;
        }
        return StrUtil.format(PIC_LINK_URL, pId, aid, viewToken, aidEncode);
    }

    Integer parseProjectId(final String previewDataFull, int pos) {
        int keyPos = findKeyPos(previewDataFull, "project_id", pos);
        if (keyPos == -1) {
            return null;
        }
        int start = previewDataFull.indexOf(":", keyPos + 1);
        // 字段值以逗号或右花括号结束（该字段可能是对象最后一个字段）
        int end = previewDataFull.indexOf(",", start + 1);
        int braceEnd = previewDataFull.indexOf("}", start + 1);
        if (end == -1 || (braceEnd != -1 && braceEnd < end)) {
            end = braceEnd;
        }
        if (start == -1 || end == -1) {
            return null;
        }
        try {
            return Integer.parseInt(previewDataFull.substring(start + 1, end).trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    String parseStrInPreviewData(final String previewDataFull, String key, int pos) {
        int keyPos = findKeyPos(previewDataFull, key, pos);
        if (keyPos == -1) {
            return null;
        }
        int start = previewDataFull.indexOf("'", keyPos + 1);
        int end = previewDataFull.indexOf("'", start + 1);
        if (start == -1 || end == -1) {
            return null;
        }
        return previewDataFull.substring(start + 1, end);
    }

    /**
     * 定位 key 作为字段名出现的位置（key 后跳过空白即跟随冒号），
     * 避免把 aid 错误匹配到 aid_encode 这类包含相同前缀的字段上
     */
    private static int findKeyPos(String s, String key, int from) {
        int idx = s.indexOf(key, from);
        while (idx != -1) {
            int j = idx + key.length();
            while (j < s.length() && Character.isWhitespace(s.charAt(j))) {
                j++;
            }
            if (j < s.length() && s.charAt(j) == ':') {
                return idx;
            }
            idx = s.indexOf(key, idx + 1);
        }
        return -1;
    }

    private String httpGet(String url) {
        try {
            return HttpRequest.get(url).timeout(HTTP_TIMEOUT_MS).execute().body();
        } catch (RuntimeException e) {
            StaticLog.error("\n请求失败：{}", url);
            StaticLog.error(e);
            return null;
        }
    }

    /**
     * 下载文件到本地。协议相对地址（//开头）优先尝试 https，失败时回退 http
     *
     * @param url       文件的 url
     * @param localPath 本地存储路径
     * @return 是否下载成功
     */
    private boolean downloadFile(String url, String localPath) {
        if (url.startsWith("//")) {
            return tryDownload("https:" + url, localPath) || tryDownload("http:" + url, localPath);
        }
        return tryDownload(url, localPath);
    }

    private boolean tryDownload(String url, String localPath) {
        File file = new File(localPath);
        try (FileOutputStream fileOutputStream = new FileOutputStream(file); BufferedOutputStream out = new BufferedOutputStream(fileOutputStream)) {
            HttpUtil.download(url, out, true);
            return true;
        } catch (IOException | RuntimeException e) {
            StaticLog.error("\n下载失败：{}（{}）", localPath, url);
            FileUtil.del(file);
            return false;
        }
    }

    private static final String PREVIEW_URL = "https://max.book118.com/index.php?g=Home&m=NewView&a=index&aid=";

    private static final String PIC_LINK_URL = "https://openapi.book118.com/getPreview.html?project_id={}&aid={}&view_token={}&aid_encode={}&page=";

    private static final String TEMP_PATH = "./temp";

    private static final String DES_PATH = "./out";
}
