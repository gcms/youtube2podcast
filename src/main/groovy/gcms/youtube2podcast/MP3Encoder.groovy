package gcms.youtube2podcast

import net.bramp.ffmpeg.FFmpeg
import net.bramp.ffmpeg.FFmpegExecutor
import net.bramp.ffmpeg.FFprobe
import net.bramp.ffmpeg.builder.FFmpegBuilder
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
/**
 * Created by gustavo on 19/02/17.
 */
@Service
class MP3Encoder implements InitializingBean {
    public static Logger log = LoggerFactory.getLogger(MP3Encoder)

    @Autowired
    VideoDownloadInfoRepo repo

    long lastEncoding = System.currentTimeMillis()
    long avgTimeBtweenEncodings = 0
    long numUpdates = 0
    public void tryEncode() {
        log.info("Scanning dir for non-encoded files...")

        for (VideoDownloadInfo info : repo) {
            if (info.status != VideoDownloadInfo.Status.DOWNLOADED)
                continue

            if (info.file.mediaInfo.contentType == 'audio/mp3')
                continue

            try {
                encode(info)
                long timeSinceLastEncode = System.currentTimeMillis() - lastEncoding
                avgTimeBtweenEncodings = (avgTimeBtweenEncodings * numUpdates + timeSinceLastEncode) / (numUpdates + 1)

                numUpdates++
                lastEncoding = System.currentTimeMillis()
            } catch (Exception ignored) {
            }
        }

        long timeSinceLastEncode = System.currentTimeMillis() - lastEncoding
        avgTimeBtweenEncodings = (avgTimeBtweenEncodings * numUpdates + timeSinceLastEncode) / (numUpdates + 1)

        try {
            log.info("Sleeping ${avgTimeBtweenEncodings}")
            Thread.sleep(Math.min(Math.max(avgTimeBtweenEncodings, 2000), 60 * 60 * 1000))
        } catch (InterruptedException ignored) {}

    }

    public void encode(VideoDownloadInfo info) {
        log.info("Encoding ${info.dump()}")

        File source = info.file.path
        String outputName = source.name.replaceAll(~/\.[\w\d]+$/, '.mp3')
        File output = new File(source.parentFile, outputName)

        FFmpegBuilder builder = new FFmpegBuilder()
                .setInput(source.getCanonicalPath())
                .overrideOutputFiles(true)
                .addOutput(output.getCanonicalPath())
                .setAudioCodec("libmp3lame")
                .setTargetSize(Math.min(20*1024*1024, (long) (FileUtils.sizeOf(source) / 2)))
                .done();

        FFmpegExecutor executor = new FFmpegExecutor(loadFFmpeg(), loadFFprobe());
        executor.createJob(builder).run();


        info.file.path = output
        info.file.mediaInfo.contentType = "audio/mp3"
        info.file.mediaInfo.length = FileUtils.sizeOf(output)

        repo.update(info)
        FileUtils.deleteQuietly(source)
    }

    public static FFmpeg loadFFmpeg() throws IOException {
        return new FFmpeg(findInPath("ffmpeg").getCanonicalPath());
    }

    public static FFprobe loadFFprobe() throws IOException {
        return new FFprobe(findInPath("ffprobe").getCanonicalPath());
    }

    private static File findInPath(String bin) throws IOException {
        String path = System.getenv("PATH");
        for (String p : path.split("[:;]")) {
            File dir = new File(p);
            File file = new File(dir, bin);
            if (file.exists())
                return file;
        }

        return null;
    }

    @Override
    void afterPropertiesSet() throws Exception {
        if (findInPath("ffmpeg") != null)
            Thread.start { while(true) { tryEncode() } }
    }
}
