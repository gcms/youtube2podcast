package gcms.youtube2podcast


/**
 * Created by gustavo on 19/02/17.
 */

class AudioResult {
    VideoDownloadInfo file

    MediaInfo getMediaInfo() {
        file?.file?.mediaInfo
    }

    boolean hasFile() {
        file.status == VideoDownloadInfo.Status.DOWNLOADING && file.file && file.file.path.exists()
    }

    RandomAccessFile raf
    RandomAccessFile getRaf() {
        raf ?: (raf = new RandomAccessFile(file.file.path, "r"))
    }

    public enum Status {
        START, WAIT, UNAVAILABLE
    }

    Status action
    InputStream getStream() {
        new FileInputStream(file.file.path)
    }

    AudioResult(VideoDownloadInfo file, Status action) {
        this.file = file
        this.action = action
    }
}
