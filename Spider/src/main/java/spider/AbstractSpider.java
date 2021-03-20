package spider;

import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.http.HttpRequest;
import lombok.Data;
import org.jsoup.internal.StringUtil;
import spider.config.SpiderConfig;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

/**
 * @author Gloduck
 */
public abstract class AbstractSpider {
    protected SpiderConfig config;
    protected Set<String> failedSet = new ConcurrentSkipListSet<>();
    private List<String> allTargetUrls = null;
    protected NoticeHook noticeHook;


    public AbstractSpider(SpiderConfig config) {
        this.config = config;
    }

    public AbstractSpider(SpiderConfig config, NoticeHook noticeHook) {
        this.config = config;
        this.noticeHook = noticeHook;
    }

    /**
     * 获取下载信息
     * @param url 下载url
     * @return 下载信息
     * @throws Exception 出现的异常
     */
    protected abstract DownloadInfo getDownloadInfo(String url) throws Exception;

    /**
     * 通过列表解析出需要下载页面的链接
     * @param singleList 列表url
     * @return 解析后的url
     * @throws Exception 出现的异常
     */
    protected abstract List<String> parsePageList(String singleList) throws Exception;

    /**
     * 模板方法，解析所有的下载链接
     *
     * @param list 列表链接
     * @return 列表里所有的url
     */
    private List<String> doParsePageList(List<String> list) {
        if (noticeHook != null) {
            noticeHook.beforeParseList(config);
        }
        List<String> res = new LinkedList<>();
        List<String> urls;
        for (String current : list) {
            try {
                if(noticeHook != null){
                    noticeHook.parseListing(config, current);
                }
                urls = parsePageList(current);
                res.addAll(urls);
            } catch (Exception e) {
                if (noticeHook != null) {
                    noticeHook.parseListFailed(config, current, e);
                }
            }
        }
        if (noticeHook != null) {
            noticeHook.afterParseList(config, res);
        }
        return res;
    }

    /**
     * 通过URL建立一个URL请求
     * @param url 请求URL
     * @return 请求
     */
    protected HttpRequest getRequest(String url) {
        HttpRequest request = HttpRequest.get(url)
                .timeout(config.getTimeoutMilliseconds())
                .cookie(config.getCookie())
                .header("User-Agent", config.getUa());
        String proxyConfig = config.getProxyHostAndPort();
        if (!StringUtil.isBlank(proxyConfig)) {
            String[] split = proxyConfig.split(":");
            if (split.length == 2 && NumberUtil.isInteger(split[1])) {
                Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(split[0], Integer.parseInt(split[1])));
                request.setProxy(proxy);
            }
        }
        return request;
    }

    /**
     * 开始文件
     */
    public final void startDownload() {
        BlockingQueue<Runnable> queue = new LinkedBlockingDeque<>();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(config.getCoreThreadCount(), config.getMaxThreadCount(), 0L, TimeUnit.MILLISECONDS, queue, Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        allTargetUrls = doParsePageList(config.getTargetLists());
        for (String downloadUrl : allTargetUrls) {
            executor.submit(new DownloadTask(downloadUrl));
        }
        if(noticeHook != null){
            noticeHook.allTaskDone(config, failedSet, allTargetUrls);
        }
        executor.shutdown();
    }

    /**
     * 获取失败列表
     * @return 失败的列表
     */
    public final Set<String> getFailedSet(){
        return this.failedSet;
    }

    /**
     * 返回所有的目标链接
     * @return
     */
    public final List<String> getAllTargetUrls(){
        return this.allTargetUrls;
    }

    /**
     * 调整文件名
     * @param name 原文件名
     * @return 替换后的文件名
     */
    protected final String adjustFileName(String name) {
        return name.replaceAll("[\\\\/*?<>:\"|]", "");
    }

    /**
     * 使用NIO下载
     * @param info 下载信息
     * @return 是否下载成功
     */
    @Deprecated
    private boolean nioDownload(DownloadInfo info) {
        boolean success = false;

        try (InputStream ins = new URL(info.getLink()).openStream()) {
            Path target = Paths.get(info.getTargetPath(), info.getFileName());
            Files.createDirectories(target.getParent());
            Files.copy(ins, target, StandardCopyOption.REPLACE_EXISTING);
            success = true;
        } catch (IOException e) {
            if(noticeHook != null){
                noticeHook.downloadFailed(config, failedSet, info, e);
            }
        }

        return success;
    }

    /**
     * 下载文件，并且记录进度
     * @param info 文件信息
     * @return 是否下载成功
     */
    private boolean download(DownloadInfo info) {
        boolean success = false;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            Path target = Paths.get(info.getTargetPath(), info.getFileName());
            if (Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !config.isOverlayExists()) {
                return true;
            }
            Files.createDirectories(target.getParent());
            URL url = new URL(info.getLink());
            URLConnection urlConnection = url.openConnection();
            long fileLength = urlConnection.getContentLengthLong();
            ProgressMonitor progressMonitor = new ProgressMonitor(fileLength, info);

            File file = target.toFile();
            inputStream = urlConnection.getInputStream();
            outputStream = new FileOutputStream(file);
            IoUtil.copyByNIO(inputStream, outputStream, config.getDownloadBufferSize(), progressMonitor);
            success = true;
        } catch (Exception e) {
            if (noticeHook != null) {
                noticeHook.downloadFailed(config, failedSet, info, e);
            }
        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {
                if(noticeHook != null){
                    noticeHook.downloadFailed(config, failedSet, info, e);
                }
            }
        }
        return success;
    }

    /**
     * 下载任务
     */
    private class DownloadTask implements Runnable {
        private final String url;

        public DownloadTask(String url) {
            this.url = url;
        }

        @Override
        public void run() {
            if (noticeHook != null) {
                noticeHook.beforeGetDownloadInfo(config, failedSet, url);
            }
            DownloadInfo info = null;
            try {
                info = getDownloadInfo(url);
            } catch (Exception e) {
                noticeHook.getDownloadInfoFailed(config, failedSet, url, e);
            }
            if (noticeHook != null) {
                noticeHook.afterGetDownloadInfo(config, failedSet, info);
            }
            if (info == null) {
                failedSet.add(url);
                return;
            }
            if (noticeHook != null) {
                noticeHook.beforeDownload(config, failedSet, info);
            }
            boolean success = download(info);
            if (noticeHook != null) {
                noticeHook.afterDownload(config, failedSet, info, success);
            }
            if (!success) {
                failedSet.add(url);
            }
        }
    }

    @Data
    protected static class DownloadInfo {
        private String fileName;
        private String targetPath;
        private String link;
    }

    private class ProgressMonitor implements StreamProgress {
        private final long total;
        private final DownloadInfo downloadInfo;

        public ProgressMonitor(long total, DownloadInfo downloadInfo) {
            this.total = total;
            this.downloadInfo = downloadInfo;
        }

        @Override
        public void start() {

        }

        @Override
        public void progress(long progressSize) {
            if (noticeHook != null) {
                noticeHook.downloading(config, failedSet, downloadInfo, progressSize, total);
            }
        }

        @Override
        public void finish() {

        }

    }
}
