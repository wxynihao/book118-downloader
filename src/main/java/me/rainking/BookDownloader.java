package me.rainking;

import java.io.IOException;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.itextpdf.text.DocumentException;

/**
 * @author Rain
 * @description
 * @date 2018/3/7 23:22
 */
public class BookDownloader {

    /**
     * 从键盘上获取指定的输入内容
     *
     * @param sTitle
     * @param sErrorMessage
     * @param op
     * @return
     */
    public static String getKey(String sTitle, String sErrorMessage, Function<String, Boolean> op, Scanner pSc) {

        String sLine = "";
        Boolean nFlag = true;

        // 输出提示信息
        System.out.print(sTitle);
        while (nFlag) {
            sLine = pSc.nextLine();
            if (!op.apply(sLine)) {
                // 输入错误, 重新输入
                System.out.print(sErrorMessage + "\n" + sTitle);
            } else {
                nFlag = false;
            }
        }

        return sLine;
    }

    public static void main(String[] args) {

        DocumentBrowser browser = new DocumentBrowser();
        Scanner pSc = new Scanner(System.in, "UTF8");

        System.out.println("Ver.20190629 latest: https://github.com/wxynihao/book118-downloader");
        List<String> pDocumentIDList = browser.readTaskList();

        // 判断是否需要执行预设任务/遗留任务
        if (pDocumentIDList.size() > 0) {
            String sKey = getKey("任务清单已读取, 是否执行(Y/N): ", "输入错误, 请重试", item -> {
                return Pattern.matches("^Y|N$", item.trim().toUpperCase());
            }, pSc);
            if (sKey.toUpperCase().equals("N")) { pDocumentIDList.clear(); }
        }

        Boolean nFlag = true;
        while (nFlag) {
            // 执行任务
            if (pDocumentIDList.size() > 0) {
                // 打印任务
                System.out.println("待下载的文档:");
                pDocumentIDList.forEach(item -> System.out.print(item + " "));
                System.out.println();

                // 下载文档
                for (String sDocumentID : pDocumentIDList) {
                    // 拷贝列表
                    List<String> pLists = new ArrayList<>(Arrays.asList(new String[pDocumentIDList.size()]));
                    Collections.copy(pLists, pDocumentIDList);

                    System.out.println(String.format("下载文档： %s", sDocumentID));
                    try {
                        browser.downloadWholeDocument(sDocumentID);
                        System.out.println("生成" + sDocumentID + "完成, 请到out文件夹查看。\n");
                    } catch (IOException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    } catch (DocumentException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    // 把剩余的任务写入列表
                    pLists.remove(sDocumentID);
                    browser.writeTaskList(pLists);
                }
                // 清空下载列表
                pDocumentIDList.clear();
            }

            // 添加下载任务
            String sCmd = "";
            Pattern pPattern = Pattern.compile("(?:^https://.+/(\\d+)\\.shtm$)|(^\\d+$)");
            while (nFlag && !sCmd.equals("start")) {
                sCmd = getKey("添加下载任务\n"
                        + "1. 输入文档编号, 如 3045568\n"
                        + "2. 输入完整的文档网址, 如 https://max.book118.com/html/2012/1017/3045568.shtm\n"
                        + "3. 输入 start, 开始下载任务\n"
                        + "4. 输入 #, 退出程序\n"
                        + "输入指令: ",
                        "输入错误请重试", item -> {
                            item = item.trim().toLowerCase();
                            if (item.equals("start") || item.equals("#")) {
                                return true;
                            } else {
                                return pPattern.matcher(item).matches();
                            }
                        }, pSc).trim();
                if (sCmd.equals("#")) { nFlag = false; }
                Matcher pMatcher = pPattern.matcher(sCmd);
                if (pMatcher.find()) {
                    sCmd = pMatcher.group(1);
                    if (sCmd == null) { sCmd = pMatcher.group(2); }
                    pDocumentIDList.add(sCmd);
                    System.out.println(String.format("下载任务 %s 已填加", sCmd));
                }
            }
            // 写入任务列表
            if (pDocumentIDList.size() > 0) {
                browser.writeTaskList(pDocumentIDList);
            }
        }

        pSc.close();
    }
}
