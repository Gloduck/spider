
import cn.hutool.core.io.IoUtil;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;

import spider.AbstractSpider;
import spider.NoticeHook;
import spider.config.SpiderConfig;
import spider.impl.PornHubSpider;

import java.io.*;


public class SpiderApp {

    public static void main(String[] args) {
        start();
    }

    public static void start() {
        try {
            String jsonConfig = IoUtil.read(new FileReader("config.json"));
            JSONObject jsonObject = JSONUtil.parseObj(jsonConfig);
            SpiderConfig config = jsonObject.toBean(SpiderConfig.class);
            config.adjustConfig();
            AbstractSpider abstractSpider;
            switch (config.getType()) {
                case "pornhub":
                    abstractSpider = new PornHubSpider(config, new NoticeHook.DefaultNoticeHook());
                    break;
                default:
                    throw new IllegalArgumentException();
            }
            abstractSpider.startDownload();
            
        } catch (FileNotFoundException e) {
            System.out.println("启动失败，无法找到配置文件");
        } catch (IllegalArgumentException e) {
            System.out.println("启动失败，请检查配置文件是否错误");
        }
    }


}
