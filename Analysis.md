```javascript
/*
 * 全屏方法
 */
function openFull(url) {
    var openUrl,
        judgeText = judge();
    // ogm 2017-05-27添加判断
    var tmp_doc_aid;
    if ($("input[name=tmp_doc_aid]").val()) {
        tmp_doc_aid = $("input[name=tmp_doc_aid]").val();
    } else {
        tmp_doc_aid = $("#feedback input[name=aid]").val();
    }
    var codeHtml = '<div style="margin: 20px;text-align: center">' +
        '<label style="font-size:16px;">请输入验证码：</label>' +
        '<input type="text" name="code" id="v_code" style="width:80px;">&nbsp;&nbsp;&nbsp;' +
        '<img src="/index.php?m=Public&a=verify" class="yzmImg" onclick="javascript:yzmTime();">&nbsp;&nbsp;' +
        '<div style="margin-top: 20px;"><input type="button" value="提 交" id="subCode" style="background: #2c9cf0;color:#fff;padding: 3px 10px;border-radius: 3px;border:1px solid #2c9cf0;font-size:14px;">' +
        '</div></div>';
    $.ajax({
        url: '/index.php?g=Home&m=View&a=viewUrl',
        type: 'GET',
        data: {
            cid: tmp_doc_aid,
            flag: 1
        },
        async: false,
        success: function (data) {
            openUrl = data;
        }
    });

    if (openUrl == 'false') {
        layer.open({
            type: 1,
            title: '预览过于频繁，请输入验证码',
            fixed: true,
            area: ['380px', '200px'],
            offset: '10%',
            content: codeHtml,
            success: function (layero, index) {
                $("#subCode").click(function () {
                    if ($("#v_code").val() == '') {
                        layer.msg('请输入验证码');
                        return false;
                    }
                    $.ajax({
                        url: '/index.php?g=Home&m=View&a=viewUrl',
                        type: 'GET',
                        data: {
                            cid: tmp_doc_aid,
                            code: $("#v_code").val()
                        },
                        async: false,
                        success: function (data) {
                            if (data == 'error') {
                                layer.msg('验证码输入错误');
                            } else {
                                openUrl = data;
                                layer.closeAll();
                                getViewHtml(judgeText, openUrl);
                            }
                        }
                    });
                })
            }
        });
    } else if (openUrl == 'error') {
        layer.msg('验证码输入错误');
    } else {
        getViewHtml(judgeText, openUrl);
    }
}
```

```javascript
function getNextPage() {
    if (nextpage == -1) {
        return false;
    }
    locked = true;
    $.ajax({
        type: "get",
        url: "GetNextPage/",
        data: {
            f: $("#Url").val(),
            img: $("#Img").val(),
            isMobile: $("#IsMobi").val(),
            isNet: $("#IsNet").val(),
            readLimit: $("#ReadLimit").val(),
            furl: $("#Furl").val()
        },
        dataType: "json",
        success: function (data) {
            $("#loading").hide();
            switch (data.NextPage) {
                case "!":
                    return false;
                    break;
                case "Over":
                    nextpage = -1;
                    return false;
                    break;
                case "Error":
                    alert(data.ErrorMsg);
                    break;
                case "Response":
                    location.href = data.ErrorMsg;
                    nextpage = -1;
                    return false;
                    break;
                case "ReadLimit":
                    var insertPos = $("#Url");
                    insertPos.after("<div style='text-align:center;margin:30px auto 100px auto'>-- 试读结束只能查看 " + data.ErrorMsg + " 页。继续阅读，请关闭本窗口，再点击付费阅读按钮。 --</div>");
                    nextpage = -1;
                    return false;
                    break;
            }
            nextpage = data.PageIndex + 1;
            var imgWidth = data.Width;
            var imgHeight = data.Height;
            if (data.Width > maxWidth) {
                imgWidth = maxWidth;
                imgHeight = (maxWidth / data.Width) * data.Height;
            }
            var maxWidthCss = "max-width:" + maxWidth + "px;";
            if ($("#ZoomDiv>span").length > 0) {
                var imgScale = parseFloat($("#ZoomDiv>span").eq(1).html()) / 100;
                if (imgScale != 1) {
                    maxWidthCss = "";
                }
                imgWidth = parseInt(imgScale * imgWidth);
                imgHeight = parseInt(imgScale * imgHeight);
            }
            var div = $("#p" + data.PageIndex);
            if (div.length > 0) {
                if (data.PageIndex == 1) {
                    div.css("width", imgWidth);
                    div.css("height", imgHeight);
                }
                div.html("<img style='border:1px solid #CACACA;" + maxWidthCss + "width:" + imgWidth + "px;height:" + imgHeight + "px' src='../img/?img=" + data.NextPage + "'>");
            } else {
                var preDiv = $("#p" + (data.PageIndex - 1));
                preDiv.after("<div data-name='page' id='p" + data.PageIndex + "' style='text-align:center;margin:10px auto;" + maxWidthCss + "width:" + imgWidth + "px;height:" + imgHeight + "px'><img style='border:1px solid #CACACA;" + maxWidthCss + "width:" + imgWidth + "px;height:" + imgHeight + "px' src='../img/?img=" + data.NextPage + "'></div>");
            }
            if (data.PageIndex == 1) {
                $("#pagecount").html(data.PageCount);
                pCount = data.PageCount;
                pload(data.PageWidth);
                oriHeight = window.innerHeight;
            }
            $("#Img").val(data.NextPage);
            if (pCount > 0 & data.PageIndex >= pCount) {
                nextpage = -1;
                return false;
            }
            if (data.PageIndex < 2) {
                getNextPage();
                return false;
            } else {
                if (data.PageIndex < 3) {
                    getNextPage();
                    return false;
                }
            }
            locked = false;
        },
        error: function (XMLHttpRequest, textStatus, errorThrown) {
            locked = false;
        }
    });
}

```
第一步
show.js文件
openFull(url)
请求
https://max.book118.com/index.php?g=Home&m=View&a=viewUrl&cid=150266376&flag=1
返回
//view43.book118.com/?
readpage=_MZZdENrtejG5NDVYRFn1g==
&furl=YOQStEpojXCy0xJO83bqoAVYhrL_yFZqP4Ozsj8XfKNfzsc8uPp1xb6rEMxHNfHaLhdOot8ZnYZW9dtUywtqpMfl@dBGXLrSSZrkJBCQKDaSlrKKrCXYvw==
&n=1

//view43.book118.com/?
readpage=AQAXTY7bf_FeK@kPYt5tIQ==
&furl=YOQStEpojXCy0xJO83bqoAVYhrL_yFZqP4Ozsj8XfKNfzsc8uPp1xb6rEMxHNfHaLhdOot8ZnYZW9dtUywtqpMfl@dBGXLrSSZrkJBCQKDaSlrKKrCXYvw==
&n=1


第二步
getViewHtml(judgeText,openUrl);

judgeText
全屏预览 ['+tit+'] --欢迎下载，永久保存到你电脑。如预览无反应,请关闭预览后重试打开！

openUrl
//view43.book118.com/?
readpage=_MZZdENrtejG5NDVYRFn1g==
&furl=YOQStEpojXCy0xJO83bqoAVYhrL_yFZqP4Ozsj8XfKNfzsc8uPp1xb6rEMxHNfHaLhdOot8ZnYZW9dtUywtqpMfl@dBGXLrSSZrkJBCQKDaSlrKKrCXYvw==
&n=1

https://view43.book118.com/pdf/GetNextPage/
?f=dXAxNC0yLmJvb2sxMTguY29tLjgwXDM5NTg1NjgtNWE2ODAzMDE3MWE4ZS5wZGY%3D
&img=Hs92T42xAvsvX7Ls%40qzOaU7OQNoC9hgWLXXXUwhA05Rrwn%40gruSWbjeohkU9kkkK
&isMobile=false
&isNet=True
&readLimit=bBrnrC9WKGud2vVPzWSICw%3D%3D
&furl=YOQStEpojXCy0xJO83bqoAVYhrL_yFZqP4Ozsj8XfKNfzsc8uPp1xb6rEMxHNfHaLhdOot8ZnYZW9dtUywtqpMfl%40dBGXLrSSZrkJBCQKDaSlrKKrCXYvw%3D%3D


https://view43.book118.com//pdf/GetNextPage/
?readlimit=y%40M8SbOoQcb0tzZixbVOLw%3D%3D
&img=Hs92T42xAvsvX7Ls%40qzOaU7OQNoC9hgWLXXXUwhA05Rrwn%40gruSWbjeohkU9kkkK
&ismobi=false
&furl=YOQStEpojXCy0xJO83bqoAVYhrL_yFZqP4Ozsj8XfKNfzsc8uPp1xb6rEMxHNfHaLhdOot8ZnYZW9dtUywtqpMfl%40dBGXLrSSZrkJBCQKDaSlrKKrCXYvw%3D%3D
&url=dXAxNC0yLmJvb2sxMTguY29tLjgwXDM5NTg1NjgtNWE2ODAzMDE3MWE4ZS5wZGY%3D
&isnet=True&

https://view43.book118.com/img/?img=Hs92T42xAvsvX7Ls@qzOaU7OQNoC9hgWLXXXUwhA05Rrwn@gruSWbj2auJIJuuHbJ13ti4M8if8=
https://view43.book118.com/img/?img=hs92t42xavsvx7ls%40qzoau7oqnoc9hgwlxxxuwha05rrwn%40gruswbj2aujijuuhbj13ti4m8if8%3D



http://view43.book118.com/pdf/dXAxNC0yLmJvb2sxMTguY29tLjgwXDM5NTg1NjgtNWE2ODAzMDE3MWE4ZS5wZGY%3d?readpage=_MZZdENrtejG5NDVYRFn1g%3D%3D&furl=YOQStEpojXCy0xJO83bqoAVYhrL_yFZqP4Ozsj8XfKNfzsc8uPp1xb6rEMxHNfHaLhdOot8ZnYZW9dtUywtqpMfl%40dBGXLrSSZrkJBCQKDaSlrKKrCXYvw%3D%3D&token=47neOQrdm_A9oZY3zM4sTxxDFZflnk94


加载的页面
http://view43.book118.com/pdf/dXAxNC0yLmJvb2sxMTguY29tLjgwXDM5NTg1NjgtNWE2ODAzMDE3MWE4ZS5wZGY%3d?readpage=656G1m%40eSfNdaHgzkRPg6g%3D%3D
&furl=YOQStEpojXCy0xJO83bqoAVYhrL_yFZqP4Ozsj8XfKNfzsc8uPp1xb6rEMxHNfHaLhdOot8ZnYZW9dtUywtqpMfl%40dBGXLrSSZrkJBCQKDaSlrKKrCXYvw%3D%3D
&token=47neOQrdm_BlCCszH_1U4QvKUMmIEUVR