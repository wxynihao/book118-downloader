package me.rainking;

/**
 * @author : Rain
 * @date : 2018/9/6 21:43
 * @description :
 */

public class PdfInfo {

    private String host = Constants.BOOK118_VIEW_HOST;
    private String url;
    private String img;
    private String readlimit;
    private String furl;
    private boolean isMobile = false;
    private boolean isNet = true;

    public String getNextUrl() {
        return String.format("%s%s?f=%s&img=%s&readLimit=%s&furl=%s&isMobile=%b&isNet=%b",
                host, Constants.NEXT_PAGE_PATH, url, img, readlimit, furl, isMobile, isNet);
    }

    public PdfInfo() {

    }

    public PdfInfo(String host, String url, String img, String readlimit, String furl) {
        this.url = url;
        this.img = img;
        this.readlimit = readlimit;
        this.furl = furl;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getImg() {
        return img;
    }

    public void setImg(String img) {
        this.img = img;
    }

    public String getReadlimit() {
        return readlimit;
    }

    public void setReadlimit(String readlimit) {
        this.readlimit = readlimit;
    }

    public String getFurl() {
        return furl;
    }

    public void setFurl(String furl) {
        this.furl = furl;
    }

    public boolean isMobile() {
        return isMobile;
    }

    public void setMobile(boolean mobile) {
        isMobile = mobile;
    }

    public boolean isNet() {
        return isNet;
    }

    public void setNet(boolean net) {
        isNet = net;
    }
}
