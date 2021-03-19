package spider.config;

import lombok.Data;
import org.jsoup.internal.StringUtil;

import java.util.List;

@Data
public class SpiderConfig {
    private String ua;
    private String cookie;
    private boolean overlayExists;
    private int coreThreadCount;
    private int maxThreadCount;
    private int timeoutMilliseconds;
    private int downloadBufferSize;
    private String baseDownloadPath;
    private String proxyHostAndPort;
    private String type;
    private List<String> targetLists;
    public void adjustConfig(){
        int processors = Runtime.getRuntime().availableProcessors();
        if(ua == null){
            ua = "";
        }
        if(cookie == null){
            cookie = "";
        }
        if(coreThreadCount <= 0){
            coreThreadCount = processors + 1;
        }
        if(maxThreadCount <= 0){
            maxThreadCount = processors * 2;
        }
        if(timeoutMilliseconds <= 0){
            timeoutMilliseconds = 3000;
        }
        if(downloadBufferSize <= 0){
            // 5M
            downloadBufferSize = 5 * 1024 * 1024;
        }
        if(StringUtil.isBlank(baseDownloadPath)){
            baseDownloadPath = "./";
        }
        if(type == null || targetLists == null || targetLists.size() == 0){
            throw new IllegalArgumentException();
        }

    }

}
