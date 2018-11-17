package me.rainking;

import com.itextpdf.text.DocumentException;

import java.io.IOException;
import java.util.Scanner;

/**
 * @author Rain
 * @description
 * @date 2018/3/7 23:22
 */
public class BookDownloader {

	public static void main(String[] args) throws IOException, DocumentException {

		DocumentBrowser browser = new DocumentBrowser();
		try (Scanner sc = new Scanner(System.in, "UTF8")) {
			while (true) {
				System.out.print("请输入文档编号并回车（#结束）：");
				String cid = sc.nextLine();
				if ("#".equals(cid)) {
					return;
				} else {
					browser.downloadWholeDocument(cid);
					System.out.println("生成" + cid + "完成，请到out文件夹查看。\n");
				}
			}
		}
	}
}
