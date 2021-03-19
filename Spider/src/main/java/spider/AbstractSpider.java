package spider;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.IoUtil;
import cn.hutool.core.io.StreamProgress;
import cn.hutool.core.util.NumberUtil;
import cn.hutool.core.util.URLUtil;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import lombok.Data;
import org.jsoup.internal.StringUtil;
import spider.config.SpiderConfig;

import java.io.*;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.*;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

public abstract class AbstractSpider {
    protected SpiderConfig config;
    protected Set<String> failedSet = new ConcurrentSkipListSet<>();
    protected NoticeHook noticeHook;


    public AbstractSpider( SpiderConfig config) {
        this.config = config;
    }

    public AbstractSpider(SpiderConfig config, NoticeHook noticeHook) {
        this.config = config;
        this.noticeHook = noticeHook;
    }

    /**
     * 获取下载信息
     *
     * @param url
     * @return
     */
    protected abstract DownloadInfo getDownloadInfo(String url);

    /**
     * 通过列表解析出需要下载页面的链接
     *
     * @return
     */
    protected abstract List<String> parsePageList(List<String> list);

    /**
     * 获取get请求
     *
     * @param url
     * @return
     */
    protected final HttpRequest getRequest(String url) {
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

    public final void startDownload() {
        BlockingQueue<Runnable> queue = new LinkedBlockingDeque<>();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(config.getCoreThreadCount(), config.getMaxThreadCount(), 0L, TimeUnit.MILLISECONDS, queue, Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());
        List<String> downloadUrls = parsePageList(config.getTargetLists());
        int size = downloadUrls.size();
        for (int i = 0; i < size; i++) {
            executor.submit(new DownloadTask(downloadUrls.get(i)));
        }
        executor.shutdown();
    }

    protected final String adjustFileName(String name) {
        return name.replaceAll("[\\\\/*?<>:\"|]", "");
    }

    @Deprecated
    private final boolean nioDownload(DownloadInfo info) {
        boolean success = false;

        try (InputStream ins = new URL(info.getLink()).openStream()) {
            Path target = Paths.get(info.getTargetPath(), info.getFileName());
            Files.createDirectories(target.getParent());
            Files.copy(ins, target, StandardCopyOption.REPLACE_EXISTING);
            success = true;
        } catch (IOException e) {
        }

        return success;
    }

    private final boolean download(DownloadInfo info) {
        boolean success = false;
        InputStream inputStream = null;
        OutputStream outputStream = null;
        try {
            Path target = Paths.get(info.getTargetPath(), info.getFileName());
            if(Files.exists(target, LinkOption.NOFOLLOW_LINKS) && !config.isOverlayExists()){
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
        } catch (Exception e) {

        } finally {
            try {
                if (outputStream != null) {
                    outputStream.close();
                }
                if (inputStream != null) {
                    inputStream.close();
                }
            } catch (IOException e) {

            }
        }
        return success;
    }

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
            DownloadInfo info = getDownloadInfo(url);
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
        private long current = 0;
        private long total;
        private DownloadInfo downloadInfo;

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
