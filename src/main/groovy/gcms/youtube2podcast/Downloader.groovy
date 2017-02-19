package gcms.youtube2podcast

import com.github.axet.vget.VGet
import com.github.axet.vget.info.VideoFileInfo
import com.github.axet.vget.info.VideoInfo
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by gustavo on 19/02/17.
 */
@Service
class Downloader {
    public static Logger log = LoggerFactory.getLogger(Downloader)

    public DownloadedFile download(YouTubeVideoURL url, File targetDir) {
        log.info("STARTING DOWNLOAD of ${url}")

        log.info("EXTRACTING information from ${url}")
        VGet vget = new VGet(new URL(url.getURL()), targetDir)
        vget.extract()

        VideoInfo info = vget.getVideo()
        VideoFileInfo fileInfo = getAudioFile(info)

        vget.targetFile(fileInfo, vget.getExt(fileInfo), new AtomicBoolean(false))
        File finalFile = fileInfo.getTarget()

        log.info("DOWNLOADING ${url} to ${finalFile}")
        vget.download(fileInfo, finalFile)

        log.info("FINISHED DOWNLOADING ${url}")
        def mediaInfo = new MediaInfo(contentType: fileInfo.contentType, length: fileInfo.length)
        new DownloadedFile(mediaInfo: mediaInfo, path: finalFile)
    }

    VideoFileInfo getAudioFile(VideoInfo info) {
        List<VideoFileInfo> files = info.getInfo()
        assert files.size() <= 2

        files.size() == 2 ? files[1] : files[0]
    }

}
