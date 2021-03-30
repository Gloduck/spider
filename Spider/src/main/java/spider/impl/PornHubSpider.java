package spider.impl;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import lombok.Data;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import spider.AbstractSpider;
import spider.NoticeHook;
import spider.config.SpiderConfig;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.LinkedList;
import java.util.List;

/**
 * @author Gloduck
 */
public class PornHubSpider extends AbstractSpider {
    private final static ScriptEngineManager ENGINE_MANAGER = new ScriptEngineManager();


    public PornHubSpider(SpiderConfig config) {
        super(config);
    }

    public PornHubSpider(SpiderConfig config, NoticeHook noticeHook) {
        super(config, noticeHook);
    }

    @Override
    public DownloadInfo getDownloadInfo(String url) throws Exception {
        Document document;
        DownloadInfo downloadInfo;
        HttpRequest request = getRequest(url);
        HttpResponse response = request.execute();
        document = Jsoup.parse(response.body());
        // 解析作者名称
        String authorName = document.select(".video-info-row.userRow").select(".bolded").text();
        String title = document.select(".inlineFree").text();
        Elements videoInfo = document.select("#player");
        // 获取下载链接
        ScriptEngine javascript = ENGINE_MANAGER.getEngineByName("javascript");
        String playerObjList = "var playerObjList = {};";
        String script = playerObjList + videoInfo.select("script").html();
        javascript.eval(script);
        String mediaLink = (String) javascript.get("media_5");
        request.setUrl(mediaLink);
        response = request.execute();
        String json = response.body();
        JSONArray jsonArray = JSONUtil.parseArray(json);
        DownloadJsonObject downloadJsonObject = jsonArray.getJSONObject(jsonArray.size() - 1).toBean(DownloadJsonObject.class);
        String link = downloadJsonObject.getVideoUrl();
        downloadInfo = new DownloadInfo();
        String fileName = String.format("%s.mp4", adjustFileName(title));
        downloadInfo.setFileName(fileName);
        downloadInfo.setLink(link);
        downloadInfo.setTargetPath(String.format("%s/%s/", config.getBaseDownloadPath(), adjustFileName(authorName)));
        return downloadInfo;
    }

    @Override
    public List<String> parsePageList(String singleList) throws Exception {
        HttpRequest request = getRequest(singleList);
        List<String> res = new LinkedList<>();
        HttpResponse response;
        response = request.execute();
        Document document = Jsoup.parse(response.body());
        Elements videoList = document.select(".videos.row-5-thumbs li");
        for (Element element : videoList) {
            String key = element.attr("data-video-vkey");
            String link = String.format("https://cn.pornhub.com/view_video.php?viewkey=%s", key);
            res.add(link);
        }
        return res;
    }

    @Data
    private static class DownloadJsonObject {
        private boolean defaultQuality;
        private String format;
        private String videoUrl;
        private String quality;
    }
}
