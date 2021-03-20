package me.rainking;

import java.util.Scanner;

/**
 * @author Rain
 * @since 2018/3/7 23:22
 */
public class BookDownloader {

    public static void main(String[] args) {
        System.out.println("Ver.20210316 latest: https://github.com/wxynihao/book118-downloader");
        System.out.println("Please input document id：");
        DocumentBrowser browser = new DocumentBrowser();
        Scanner scan = new Scanner(System.in, "UTF8");
        if (scan.hasNext()) {
            String documentId = scan.next();
            browser.downloadWholeDocument(documentId);
        }
    }
}
