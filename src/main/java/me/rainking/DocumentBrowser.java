package me.rainking;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.ReUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.http.HtmlUtil;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import cn.hutool.log.StaticLog;
import com.itextpdf.text.DocumentException;

import java.io.*;
import java.net.URLEncoder;
import java.util.*;

/**
 * @author Rain
 * @description 文档浏览，包含获取起始页和全部预览图的方法
 * @date 2018/3/7 19:26
 */
public class DocumentBrowser {

	/**
	 * 下载文档的全部图片
	 *
	 * @param documentId 文档编号
	 * @throws IOException pdf创建错误
	 * @throws DocumentException pdf创建错误
	 */
	public void downloadWholeDocument(String documentId) throws IOException, DocumentException {
		List<String> imgUrlList = getImgUrlOfDocument(documentId);

		String srcPath = "./temp/" + documentId;
		String desPath = "./out";

		FileUtil.mkdir(new File(srcPath));
		FileUtil.mkdir(new File(desPath));

		int sizeOfBook = imgUrlList.size();
		int page = 1;
		StaticLog.info("开始下载...");
		for (String imgUrl : imgUrlList) {
			downloadFile(imgUrl, srcPath + "/" + autoGenericCode(page, String.valueOf(sizeOfBook).length()) + ".gif");
			StaticLog.info(page + "/" + sizeOfBook);
			page++;
		}
		StaticLog.info("开始生成...");
		PdfGenerator.creatPDF(srcPath, desPath + "/" + documentId + ".pdf");
		FileUtil.del(new File(srcPath));
	}

	/**
	 * 将数字字符串的左边补充0，使其长度达到指定长度
	 *
	 * @param number 需要处理的数字
	 * @param width 补充后字符串长度
	 * @return 通过填充0达到长度的数字字符串
	 */
	private String autoGenericCode(int number, int width) {
		return String.format("%0" + width + "d", number);
	}

	/**
	 * 获取文档的全部预览图片地址
	 *
	 * @param documentId 文档的编号
	 * @return 全部图片地址
	 */
	public List<String> getImgUrlOfDocument(String documentId) {

		List<String> imgUrlList = new LinkedList<>();
		PdfInfo pdfInfo = getPdfInfo(documentId);
		String nextPage;

		StaticLog.info("开始获取链接，请耐心等待...");
		while (pdfInfo != null) {
			nextPage = getNextPage(pdfInfo);
			if (!Constants.TAG_OF_END.contains(nextPage)) {
				imgUrlList.add(pdfInfo.getHost() + Constants.IMG_PREFIX_URL + nextPage);
				StaticLog.info("获取到第" + imgUrlList.size() + "页。");
			} else {
				break;
			}
			try {
				pdfInfo.setImg(URLEncoder.encode(nextPage, "utf8"));
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
		String pdfPageHtml = HttpUtil.get(viewHost + HtmlUtil.unescape(href));

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
		String nextPageJson = HttpUtil.get(pdfInfo.getNextUrl());
		PageInfo pageInfo = JSONUtil.toBean(nextPageJson, PageInfo.class);
		return pageInfo.getNextPage();
	}

	/**
	 * 下载文件到本地
	 *
	 * @param url 文件的 url
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
