package gcms.youtube2podcast
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.core.task.TaskExecutor
import org.springframework.stereotype.Service
/**
 * Created by gustavo on 19/02/17.
 */
@Service
class DownloadManager {
    public static Logger log = LoggerFactory.getLogger(DownloadManager)

    @Autowired
    private Downloader downloader

    @Autowired
    VideoDownloadInfoRepo repo

    @Autowired
    TaskExecutor taskExecutor

    private Map<String, DownloadTask> tasks = [:]

    void enqueue(String id) {
        assert !tasks.containsKey(id)

        def task = new DownloadTask(downloader, this, repo, id)
        tasks[id] = task
        taskExecutor.execute() {
            try {
                task.run()
            } finally {
                tasks.remove(id)
                checkNotDownloading(id)
            }
        }
    }

    private void checkNotDownloading(String id) {
        def info = repo.getInfo(id)
        if (info.status == VideoDownloadInfo.Status.DOWNLOADING) {
            log.warn("${info.dump()} finished but still marked as DOWNLOADING")
            info.status == VideoDownloadInfo.Status.NONE
            enqueue(id)
        }
    }

    boolean isDownloading(String id) {
        tasks.containsKey(id)
    }

    DownloadTask getTask(String id) {
        tasks[id]
    }

}
