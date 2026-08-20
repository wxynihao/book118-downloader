package me.rainking;

import java.util.Scanner;

/**
 * @author Rain
 * @since 2018/3/7 23:22
 */
public class BookDownloader {

    public static void main(String[] args) {
        System.out.println("V2026.0820 latest: https://github.com/wxynihao/book118-downloader");
        DocumentBrowser browser = new DocumentBrowser();
        if (args.length > 0) {
            for (String arg : args) {
                browser.downloadWholeDocument(arg);
            }
            return;
        }
        System.out.println("Please input document id or url (empty line to exit)：");
        try (Scanner scan = new Scanner(System.in, "UTF8")) {
            while (scan.hasNextLine()) {
                String line = scan.nextLine().trim();
                if (line.isEmpty()) {
                    break;
                }
                browser.downloadWholeDocument(line);
                System.out.println("\nPlease input next document id or url (empty line to exit)：");
            }
        }
    }
}
