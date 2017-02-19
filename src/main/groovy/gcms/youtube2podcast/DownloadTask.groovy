package gcms.youtube2podcast

import com.github.axet.wget.info.ex.DownloadError
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock

/**
 * Created by gustavo on 19/02/17.
 */
class DownloadTask implements Runnable {
    public static Logger log = LoggerFactory.getLogger(DownloadTask)

    Downloader downloader
    DownloadManager manager
    VideoDownloadInfoRepo repo
    String id

    Lock lock
    Condition finished

    DownloadTask(Downloader downloader, DownloadManager manager, VideoDownloadInfoRepo repo, String id) {
        this.downloader = downloader
        this.manager = manager
        this.repo = repo
        this.id = id

        lock = new ReentrantLock()
        finished = lock.newCondition()

        VideoDownloadInfo info = repo.getInfo(id)
        info.status = VideoDownloadInfo.Status.QUEUE
        repo.update(info)
    }

    @Override
    void run() {
        try {
            download()
        } finally {
            lock.lock()
            log.info("${id}.signalAll()")
            finished.signalAll()
            lock.unlock()
        }
    }

    void download() {
        YouTubeVideoURL url = new YouTubeVideoURL(id)
        File targetDir = new File(repo.getVideoDir(id), 'files')

        def info = repo.getInfo(id)
        if (info.status == VideoDownloadInfo.Status.DOWNLOADED)
            throw new IllegalArgumentException("${id} is already downloaded")

        if (info.status == VideoDownloadInfo.Status.DOWNLOADING && manager.getTask(id) != this)
            throw new IllegalArgumentException("${id} is already being downloaded")

        info.status = VideoDownloadInfo.Status.DOWNLOADING
        info.downloadStart = new Date()
        repo.update(info)

        try {
            targetDir.mkdirs();
            FileUtils.deleteDirectory(targetDir)
            targetDir.mkdirs();

            DownloadedFile result = downloader.download(url, targetDir)

            info.status = VideoDownloadInfo.Status.DOWNLOADED
            info.downloadFinish = new Date()
            info.file = result
            repo.update(info)
        } catch (DownloadError error) {
            info.status = VideoDownloadInfo.Status.UNAVAILABLE
            repo.update(info)
        } catch (IOException ex) {
            info.status = VideoDownloadInfo.Status.NONE
            repo.update(info)
            manager.enqueue(id)  // Potentially buggy, still in downloading list
        }
    }

    public void waitToFinish(long timeout) {
        lock.lock()
        log.info("${id}.await(${timeout})")
        finished.await(timeout, TimeUnit.MILLISECONDS)
        lock.unlock()
    }

    public void waitToFinish() {
        lock.lock()
        log.info("${id}.await()")
        finished.await()
        lock.unlock()
    }
}
