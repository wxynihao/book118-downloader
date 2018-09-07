package me.rainking;

import cn.hutool.core.collection.CollUtil;

import java.util.List;

/**
 * @author : Rain
 * @date : 2018/9/6 22:26
 * @description :
 */

class Constants {

    static final String BOOK118_VIEW_HOST = "https://view45.book118.com";

    static final String IMG_PREFIX_URL = BOOK118_VIEW_HOST + "/img/?img=";

    static final String NEXT_PAGE_PATH = "/pdf/GetNextPage/";

    static final String OPEN_FULL_URL = "https://max.book118.com/index.php?g=Home&m=View&a=viewUrl&flag=1&cid=";

    static final String FILE_NOT_EXIST = "文件不存在";

    static final List<String> TAG_OF_END = CollUtil.newArrayList("!", "Over", "Error", "Response", "ReadLimit");

    /**
     * 匹配页面中隐藏的input的值，用于获取文档信息
     */
    static final String INPUT_PATTERN = "<input type=\"hidden\" id=\"(.*?)\" value=\"(.*?)\".*?/>";
    /**
     * 用于获取input标签中的id
     */
    static final String ID_PATTERN = ".*id=\"(.*?)\"";
    /**
     * 用于获取input标签中的value
     */
    static final String VALUE_PATTERN = ".*value=\"(.*?)\"";

    /**
     * 获取302跳转页面中的跳转href
     */
    static final String HREF_PATTERN = ".*href=\"(.*?)\"";

}
