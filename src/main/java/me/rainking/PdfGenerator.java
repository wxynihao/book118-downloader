package me.rainking;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * 处理本地文件
 *
 * @author Rain
 * @since 2018/3/7 19:38
 */
public class PdfGenerator {

    /**
     * 使用图片创建PDF文件
     *
     * @param srcPahOfImg  图片文件夹路径
     * @param desPathOfPdf PDF存储路径
     * @throws DocumentException pdf相关错误
     * @throws IOException       图片相关错误
     */
    public static void creatPDF(String srcPahOfImg, String desPathOfPdf, String sSufix) throws IOException, DocumentException {

        File file = new File(srcPahOfImg);
        File[] picFiles = file.listFiles();
        if (picFiles == null || picFiles.length == 0) {
            return;
        }
        List<File> files = Arrays.asList(picFiles);
        files.sort(Comparator.comparing(e -> Integer.valueOf(e.getName().split("\\.")[0])));

        Image firstImg = Image.getInstance(files.get(0).getCanonicalPath());
        Document document = new Document(new Rectangle(firstImg.getWidth(), firstImg.getHeight()), 0, 0, 0, 0);
        PdfWriter.getInstance(document, new FileOutputStream(desPathOfPdf));
        document.open();

        for (File picFile : picFiles) {
            String sFileName = picFile.getCanonicalPath();
            // 过滤出指定后缀名的文件
            if (!sFileName.substring(sFileName.lastIndexOf('.') + 1).equals(sSufix)) {
                continue;
            }
            Image img = Image.getInstance(sFileName);
            document.add(img);
            document.setPageSize(new Rectangle(img.getWidth(), img.getHeight()));
        }
        document.close();
    }

}
