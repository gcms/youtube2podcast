package gcms.youtube2podcast

import com.fasterxml.jackson.databind.ObjectMapper
import com.google.common.collect.Iterators
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
/**
 * Created by gustavo on 19/02/17.
 */
@Service
class VideoDownloadInfoRepo implements Iterable<VideoDownloadInfo> {
    public static Logger log = LoggerFactory.getLogger(VideoDownloadInfoRepo)

    @Value('${youtube2podcast.repo.datadir}')
    File root

    public File getVideoDir(String id) {
        File videoDir = new File(root, id)
        videoDir.mkdirs()
        videoDir
    }

    File getInfoFile(String id) {
        new File(getVideoDir(id), ".info")
    }


    def mapper = new ObjectMapper()
    VideoDownloadInfo getInfo(String id) {
        File infoFile = getInfoFile(id)

        synchronized (infoFile.path) {
            log.info ("Reading ${id} from ${infoFile.path}")

            if (infoFile.exists())
                return mapper.readValue(infoFile.text, VideoDownloadInfo)


            return new VideoDownloadInfo(id: id)
        }
    }

    void update(VideoDownloadInfo info) {
        File infoFile = getInfoFile(info.id)
        synchronized (infoFile.path) {
            log.info ("Saving ${info.dump()} to ${infoFile.path}")
            info.lastUpdated = new Date()
            infoFile.text = mapper.writeValueAsString(info)
        }
    }

    @Override
    Iterator<VideoDownloadInfo> iterator() {
        List<File> ids = root.listFiles().findAll { it.isDirectory() && new File(it, '.info').exists() }
        Iterators.transform(ids.iterator()) { getInfo(it.name) }
    }
}
