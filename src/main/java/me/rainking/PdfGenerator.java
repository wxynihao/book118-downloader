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
import java.util.List;

/**
 * @description 处理本地文件
 * @author Rain
 * @date 2018/3/7 19:38
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
    public static void creatPDF(String srcPahOfImg, String desPathOfPdf) throws DocumentException, IOException {

        File file = new File(srcPahOfImg);

        File[] picFiles = file.listFiles();

        if (picFiles == null || picFiles.length == 0) {
            return;
        }

        List<File> files = Arrays.asList(picFiles);


        //需要根据第一页创建document的大小
        //如果不根据第一页创建，即使修改document的大小也不会生效，困惑
        Image firstImg = Image.getInstance(files.get(0).getCanonicalPath());
        Document document = new Document(new Rectangle(firstImg.getWidth(), firstImg.getHeight()), 0, 0, 0, 0);
        PdfWriter.getInstance(document, new FileOutputStream(desPathOfPdf));
        document.open();
        document.add(firstImg);

        for (int i = 1; i < files.size(); i++) {
            Image img = Image.getInstance(files.get(i).getCanonicalPath());
            document.setPageSize(new Rectangle(img.getWidth(), img.getHeight()));
            document.add(img);
        }
        document.close();
    }

}
