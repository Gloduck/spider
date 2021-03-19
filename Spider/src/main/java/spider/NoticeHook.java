package spider;

import spider.config.SpiderConfig;

import java.util.List;
import java.util.Set;

public interface NoticeHook {

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
     */
    void parseListFailed(SpiderConfig config, String currentList);

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
     * 开始下载前调用
     *
     * @param config
     * @param failedSet
     * @param info
     */
    void beforeDownload(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info);

    /**
     * 下载过程中调用
     * @param config
     * @param failedSet
     * @param info
     * @param current
     * @param total
     */
    void downloading(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info, long current, long total);

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
        public void parseListFailed(SpiderConfig config, String currentList) {
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
        public void beforeDownload(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info) {
            String name = Thread.currentThread().getName();
            System.out.printf("线程：%s开始下载，文件名为：%s，目标路径为：%s\n", name, info.getFileName(), info.getTargetPath());
        }

        @Override
        public void downloading(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info, long current, long total) {
            String name = Thread.currentThread().getName();
            System.out.printf("线程：%s当前的下载进度为；%f\n",name, ((current * 100.0) / total));
        }

        @Override
        public void afterDownload(SpiderConfig config, Set<String> failedSet, AbstractSpider.DownloadInfo info, boolean success) {
            String name = Thread.currentThread().getName();
            if (success) {
                System.out.printf("线程：%s下载文件成功，文件为：%s\n", name,  info.getTargetPath() + info.getFileName());
            } else {
                System.out.printf("线程：%s下载文件失败\n", name, info.getTargetPath() + info.getFileName());
            }
        }
    }
}
