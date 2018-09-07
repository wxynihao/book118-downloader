package me.rainking;

/**
 * @author : Rain
 * @date : 2018/9/6 21:58
 * @description : 下一页的信息
 */

public class PageInfo {

	/**
	{
		"NextPage": "7o@o7xcocmljx@u6I0iYC7erqNjqt2ikNplHSbiTewxBBX8M5@R1laGW91pGvXmmgXflQ9AKCpE=",
		"PageCount": 13,
		"ErrorMsg": "",
		"PageIndex": 1,
		"PageWidth": 792,
		"Width": 792,
		"Height": 1120
	}
    **/

    private String NextPage;
    private Integer PageCount;
    private String ErrorMsg;
    private Integer PageIndex;
    private Integer PageWidth;
    private Integer Width;
    private Integer Height;

    public PageInfo() {
    }

    public PageInfo(String nextPage, Integer pageCount, String errorMsg, Integer pageIndex, Integer pageWidth, Integer width, Integer height) {
        NextPage = nextPage;
        PageCount = pageCount;
        ErrorMsg = errorMsg;
        PageIndex = pageIndex;
        PageWidth = pageWidth;
        Width = width;
        Height = height;
    }

    public String getNextPage() {
        return NextPage;
    }

    public void setNextPage(String nextPage) {
        NextPage = nextPage;
    }

    public Integer getPageCount() {
        return PageCount;
    }

    public void setPageCount(Integer pageCount) {
        PageCount = pageCount;
    }

    public String getErrorMsg() {
        return ErrorMsg;
    }

    public void setErrorMsg(String errorMsg) {
        ErrorMsg = errorMsg;
    }

    public Integer getPageIndex() {
        return PageIndex;
    }

    public void setPageIndex(Integer pageIndex) {
        PageIndex = pageIndex;
    }

    public Integer getPageWidth() {
        return PageWidth;
    }

    public void setPageWidth(Integer pageWidth) {
        PageWidth = pageWidth;
    }

    public Integer getWidth() {
        return Width;
    }

    public void setWidth(Integer width) {
        Width = width;
    }

    public Integer getHeight() {
        return Height;
    }

    public void setHeight(Integer height) {
        Height = height;
    }
}
