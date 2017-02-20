package gcms.youtube2podcast

/**
 * Created by gustavo on 19/02/17.
 */
class VideoDownloadInfo {
    public enum Status {
        NONE, QUEUE, DOWNLOADING, DOWNLOADED, UNAVAILABLE
    }

    String id
    String error

    Date lastUpdated

    Status status = Status.NONE
    DownloadedFile file

    Date downloadStart
    Date downloadFinish
}
