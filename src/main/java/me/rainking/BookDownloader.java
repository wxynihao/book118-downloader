package me.rainking;

import com.itextpdf.text.DocumentException;

import java.io.IOException;
import java.util.Scanner;

/**
 * @description
 * @author Rain
 * @date 2018/3/7 23:22
 */
public class BookDownloader {

    public static void main(String[] args) throws IOException, DocumentException {

        DocumentBrowser browser = new DocumentBrowser();
//        browser.downloadWholeDocument("7000010006001146");
        Scanner sc = new Scanner(System.in);
        while (true) {
            System.out.println("请输入文档编号并回车（#结束）：");
            String cid = sc.nextLine();
            if ("#".equals(cid)) {
                return;
            } else {
                browser.downloadWholeDocument(cid);
                System.out.println(cid + "完成，请到out文件夹查看。");
            }
        }
    }
}
