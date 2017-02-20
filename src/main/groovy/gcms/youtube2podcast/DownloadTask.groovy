package gcms.youtube2podcast

import com.github.axet.wget.info.ex.DownloadError
import com.github.axet.wget.info.ex.DownloadInterruptedError
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.nio.file.Files
import java.nio.file.Paths
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

        targetDir.mkdirs();
        FileUtils.deleteDirectory(targetDir)
        targetDir.mkdirs();

        info.downloadStart = new Date()
        repo.update(info)

        try {
            def md = new MediaDownloader(url, targetDir)

            info.status = VideoDownloadInfo.Status.DOWNLOADING
            info.file = md.extract()
            repo.update(info)

            DownloadedFile result = md.download()
//            DownloadedFile result = downloader.download(url, targetDir)

            info.status = VideoDownloadInfo.Status.DOWNLOADED
            info.downloadFinish = new Date()
            info.file = result

            def fileSize = FileUtils.sizeOf(info.file.path)
            if (fileSize != info.file.mediaInfo.length) {
                log.warn("File size (${fileSize}) differs from contentLength ($info.file.mediaInfo.length)")
                info.file.mediaInfo.length = fileSize
            }

            repo.update(info)
//
//            def link = Paths.get(new File("/tmp/files/${id}").toURI())
//            Files.createSymbolicLink(link, Paths.get(result.path.toURI()))

        } catch (DownloadInterruptedError interrupt) {
            log.warn("Download interrupted: ${interrupt}")
            info.status = VideoDownloadInfo.Status.NONE
            repo.update(info)
        } catch(DownloadError error) {
            log.warn("DownloadERROR: ${error}! Will make file UNAVAILABLE ${id}")
            info.status = VideoDownloadInfo.Status.UNAVAILABLE
            info.error = error.toString()
            repo.update(info)
        } catch (IOException ex) {
            info.status = VideoDownloadInfo.Status.NONE
            repo.update(info)
            log.warn(ex.toString())
//            manager.enqueue(id)  // Potentially buggy, still in downloading list
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
