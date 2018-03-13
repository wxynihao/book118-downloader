package me.rainking;

import org.junit.Test;

/**
 * @description:
 * @author: Rain
 * @date: 2018/3/7 11:34
 */
public class PdfGeneratorTest {

    /**
     * 根据图片生成PDF测试
     * @throws Exception
     */
    @Test
    public void creatPDF() throws Exception {
        PdfGenerator.creatPDF("D:\\Users\\Rain\\Downloads\\新建文件夹", "./ss.pdf");
    }
}