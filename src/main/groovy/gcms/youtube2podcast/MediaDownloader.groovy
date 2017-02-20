package gcms.youtube2podcast

import com.github.axet.vget.VGet
import com.github.axet.vget.info.VideoFileInfo
import com.github.axet.vget.info.VideoInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by gustavo on 19/02/17.
 */
class MediaDownloader {
    public static Logger log = LoggerFactory.getLogger(MediaDownloader)

    VGet vget
    File finalFile
    VideoFileInfo fileInfo
    YouTubeVideoURL url

    MediaDownloader(YouTubeVideoURL url, File targetDir) {
        this.url = url
        this.vget = new VGet(new URL(url.URL), targetDir)
    }

    DownloadedFile extract() {
        log.info("STARTING DOWNLOAD of ${url}")

        log.info("EXTRACTING information from ${url}")
        vget.extract(new MyYouTubeParser(), new AtomicBoolean(false)) {}

        VideoInfo info = vget.getVideo()
        fileInfo = getAudioFile(info)

        vget.targetFile(fileInfo, vget.getExt(fileInfo), new AtomicBoolean(false))
        finalFile = fileInfo.getTarget()

        def mediaInfo = new MediaInfo(contentType: fileInfo.contentType, length: fileInfo.length)
        new DownloadedFile(mediaInfo: mediaInfo, path: finalFile)
    }

    DownloadedFile download() {
        assert vget != null && fileInfo != null && finalFile != null

        log.info("DOWNLOADING ${url} to ${finalFile}")
        vget.download(fileInfo, finalFile)

        log.info("FINISHED DOWNLOADING ${url}")

        def mediaInfo = new MediaInfo(contentType: fileInfo.contentType, length: fileInfo.length)
        new DownloadedFile(mediaInfo: mediaInfo, path: finalFile)
    }

    static VideoFileInfo getAudioFile(VideoInfo info) {
        List<VideoFileInfo> files = info.getInfo()
        assert files.size() <= 2

        files.size() == 2 ? files[1] : files[0]
    }
}
