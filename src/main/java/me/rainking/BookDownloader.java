package me.rainking;

import com.itextpdf.text.DocumentException;

import java.io.IOException;
import java.util.Scanner;

/**
 * @description:
 * @author: Rain
 * @date: 2018/3/7 23:22
 */
public class BookDownloader {

    public static void main(String[] args) throws IOException, DocumentException {

        DocumentBrowser browser = new DocumentBrowser();

        Scanner sc = new Scanner(System.in);

        while (true) {
            System.out.println("请输入文档编号并回车（#结束）：");
            String cid = sc.nextLine();
            if ("#".equals(cid)) {
                return;
            } else {
                browser.downloadWholeDocument(cid);
                System.out.println(cid + "下载完成。");
            }
        }


    }

}
