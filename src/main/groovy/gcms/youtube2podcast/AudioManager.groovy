package gcms.youtube2podcast
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
/**
 * Created by gustavo on 19/02/17.
 */
@Service
class AudioManager {
    public static Logger log = LoggerFactory.getLogger(AudioManager)

    @Autowired
    VideoDownloadInfoRepo repo

    @Autowired
    DownloadManager downloadManager

    AudioResult queryFile(String id) {
        VideoDownloadInfo info = repo.getInfo(id)

        log.info("${id} requested. STATUS: ${info.status}")

        if (info.status == VideoDownloadInfo.Status.UNAVAILABLE) {
            return new AudioResult(info, AudioResult.Status.UNAVAILABLE)
        }

        if (info.status == VideoDownloadInfo.Status.DOWNLOADED) {
            return new AudioResult(info, AudioResult.Status.START)
        }

        // Status == DOWNLOADING or NONE
        // Even if Status = NONE it can be already in the task queue
        if (!downloadManager.isDownloading(id)) {
            downloadManager.enqueue(id)
        }

        log.info("STATUS for ${id} is ${info.status}. WAITing for download")

        new AudioResult(info, AudioResult.Status.WAIT)
    }

    void waitFor(String id, long timeout) {
        log.info("Waiting for ${id} during ${timeout}")
        def task = downloadManager.getTask(id)
        if (task != null) {
            if (timeout > 0) {
                task.waitToFinish(timeout * 1000)
            } else {
                task.waitToFinish()
            }
        }

        log.info("Finished waiting for ${id}")
    }
}
