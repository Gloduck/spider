package spider;

import spider.config.SpiderConfig;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;
import java.util.Set;

public interface NoticeHook {

    /**
     * 所有任务结束后调用
     *
     * @param config
     * @param failedSet
     * @param allTargetUrls
     */
    void allTaskDone(SpiderConfig config, Set<String> failedSet, List<String> allTargetUrls);

    /**
     * 开始解析列表时候调用
     *
     * @param config
     */
    void beforeParseList(SpiderConfig config);

    /**
     * 解析列表完成后调用
     *
     * @param config
     * @param resultList
     */
    void afterParseList(SpiderConfig config, List<String> resultList);

    /**
     * 解析列表过程中调用
     *
     * @param config
     * @param currentList
     */
    void parseListing(SpiderConfig config, String currentList);

    /**
     * 解析列表失败调用
     *
     * @param config
     * @param currentList
     * @param e
     */
    void parseListFailed(SpiderConfig config, String currentList, Exception e);

    /**
     * 开始解析下载文件前调用
     *
     * @param config
     * @param failedSet
     * @param currentUrl
     */
    void beforeGetDownloadInfo(SpiderConfig config, Set<String> failedSet, String currentUrl);

    /**
     * 完成解析下载文件前调用
     *
     * @param config
     * @param failedSet
     * @param info
     */
    void afterGetDownloadInfo(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info);

    /**
     * 解析下载文件失败调用
     *
     * @param config
     * @param failedSet
     * @param currentUrl
     * @param e
     */
    void getDownloadInfoFailed(SpiderConfig config, Set<String> failedSet, String currentUrl, Exception e);

    /**
     * 开始下载前调用
     *
     * @param config
     * @param failedSet
     * @param info
     */
    void beforeDownload(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info);

    /**
     * 下载过程中调用
     *
     * @param config
     * @param failedSet
     * @param info
     * @param current
     * @param total
     */
    void downloading(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info, long current, long total);

    /**
     * 下载失败调用
     *
     * @param config
     * @param failedSet
     * @param info
     * @param e
     */
    void downloadFailed(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info, Exception e);

    /**
     * 下载完成后调用
     *
     * @param config
     * @param failedSet
     * @param info
     * @param success
     */
    void afterDownload(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info, boolean success);

    class DefaultNoticeHook implements NoticeHook {

        @Override
        public void allTaskDone(SpiderConfig config, Set<String> failedSet, List<String> allTargetUrls) {
            System.out.printf("所有的任务已经完成，目标链接数为：%d，失败数为：%d\n失败的任务如下：\n");
            for (String s : failedSet) {
                System.out.println(s);
            }
        }

        @Override
        public void beforeParseList(SpiderConfig config) {
            System.out.println("开始解析列表");

        }

        @Override
        public void afterParseList(SpiderConfig config, List<String> resultList) {
            System.out.printf("解析列表完成，待下载视频一共有%d个\n", resultList.size());
        }

        @Override
        public void parseListing(SpiderConfig config, String currentList) {
            System.out.printf("当前正在解析列表：%s\n", currentList);
        }

        @Override
        public void parseListFailed(SpiderConfig config, String currentList, Exception e) {
            System.out.printf("解析列表：%s失败\n", currentList);
        }


        @Override
        public void beforeGetDownloadInfo(SpiderConfig config, Set<String> failedSet, String currentUrl) {
            String name = Thread.currentThread().getName();
            System.out.printf("线程：%s开始解析链接：%s\n", name, currentUrl);
        }

        @Override
        public void afterGetDownloadInfo(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info) {
            String name = Thread.currentThread().getName();
            if (info == null) {
                System.out.printf("线程：%s解析失败\n", name);

            } else {
                System.out.printf("线程：%s解析成功\n", name);
            }
        }

        @Override
        public void getDownloadInfoFailed(SpiderConfig config, Set<String> failedSet, String currentUrl, Exception e) {

        }

        @Override
        public void beforeDownload(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info) {
            String name = Thread.currentThread().getName();
            String size = "未知";
            try {
                URL url = new URL(info.getLink());
                URLConnection urlConnection = url.openConnection();
                long contentLengthLong = urlConnection.getContentLengthLong();
                size = String.format("%dMB", (contentLengthLong / (1024 * 1024)));
            } catch (IOException e) {
            }
            System.out.printf("线程：%s开始下载，文件名为：%s，目标路径为：%s，文件大小为：%s\n", name, info.getFileName(), info.getTargetPath(), size);
        }

        @Override
        public void downloading(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info, long current, long total) {
            String name = Thread.currentThread().getName();
            System.out.printf("线程：%s当前的下载进度为；%f\n", name, ((current * 100.0) / total));
        }

        @Override
        public void downloadFailed(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info, Exception e) {
            System.out.printf("下载：%s失败\n", info.getLink());
        }

        @Override
        public void afterDownload(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info, boolean success) {
            String name = Thread.currentThread().getName();
            if (success) {
                System.out.printf("线程：%s下载文件成功，文件为：%s\n", name, info.getTargetPath() + info.getFileName());
            } else {
                System.out.printf("线程：%s下载文件失败\n", name, info.getTargetPath() + info.getFileName());
            }
        }
    }
}
