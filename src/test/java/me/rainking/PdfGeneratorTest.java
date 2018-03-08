package me.rainking;

import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.*;

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