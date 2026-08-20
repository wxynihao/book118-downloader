package me.rainking;

import cn.hutool.core.util.ReUtil;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
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
     * 使用图片创建PDF文件，每个图片一页，页面尺寸与图片一致。
     * 只收集文件名为"数字.后缀"且后缀匹配的图片，按页码数字排序。
     *
     * @param srcDirOfImg  图片文件夹路径
     * @param desPathOfPdf PDF存储路径
     * @param suffix       图片文件后缀（不区分大小写）
     * @throws DocumentException pdf相关错误
     * @throws IOException       图片相关错误
     */
    public static void createPdf(String srcDirOfImg, String desPathOfPdf, String suffix) throws IOException, DocumentException {
        File dir = new File(srcDirOfImg);
        File[] picFiles = dir.listFiles();
        if (picFiles == null) {
            return;
        }
        List<File> files = new ArrayList<>();
        for (File picFile : picFiles) {
            if (isPageImage(picFile, suffix)) {
                files.add(picFile);
            }
        }
        if (files.isEmpty()) {
            return;
        }
        files.sort(Comparator.comparingInt(PdfGenerator::pageNumber));

        Image firstImg = Image.getInstance(files.get(0).getCanonicalPath());
        Document document = new Document(new Rectangle(firstImg.getWidth(), firstImg.getHeight()), 0, 0, 0, 0);
        PdfWriter.getInstance(document, new FileOutputStream(desPathOfPdf));
        document.open();
        for (File picFile : files) {
            Image img = Image.getInstance(picFile.getCanonicalPath());
            document.add(img);
            document.setPageSize(new Rectangle(img.getWidth(), img.getHeight()));
        }
        document.close();
    }

    /** 页面图片的文件名必须是"数字.后缀"形式，后缀不区分大小写 */
    private static boolean isPageImage(File file, String suffix) {
        String name = file.getName();
        int dot = name.lastIndexOf('.');
        return dot > 0
            && suffix.equalsIgnoreCase(name.substring(dot + 1))
            && ReUtil.isMatch("^\\d+$", name.substring(0, dot));
    }

    private static int pageNumber(File file) {
        String name = file.getName();
        return Integer.parseInt(name.substring(0, name.lastIndexOf('.')));
    }

}
