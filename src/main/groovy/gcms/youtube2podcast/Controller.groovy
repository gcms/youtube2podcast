package gcms.youtube2podcast

import com.github.axet.vget.VGet
import org.apache.commons.io.FileUtils
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse
import java.util.concurrent.atomic.AtomicBoolean
import java.util.regex.Matcher
import java.util.regex.Pattern
import java.util.zip.GZIPOutputStream

/**
 * Created by gustavo on 19/02/17.
 */

@RestController
class Controller {
    public static Logger log = LoggerFactory.getLogger(Controller)

    @Autowired
    RssFeed rss

    @Autowired
    AudioManager audio

    @Autowired
    HttpServletRequest request

    @Autowired
    HttpServletResponse response

    public String getBaseURL() {
        new URL(request.requestURL.toString()).getAuthority()
    }

    public long getTimeout() {
        def timeout = request.getHeader('Keep-Alive')
        if (timeout == null)
            return -1

        timeout.find(~/timeout=(\\d+)/) { it[1] }?.toLong() ?: -1
    }


    static Pattern RANGE_PATTERN = ~/bytes=(\d+)\-(\d*)[^\d,]?/
    public Range getRange() {
//        Range:bytes=0-274
        String range = request.getHeader('Range')
        if (range == null)
            return null
        Matcher m = RANGE_PATTERN.matcher(range)
        if (!m.find())
            return null

        String start = m.group(1)
        String end = m.group(2)


        start.toInteger()..(end.isInteger() ? end.toInteger() : Integer.MAX_VALUE)
    }

    @RequestMapping(value = "/url/{id}")
    public String findUrl(@PathVariable String id) {
        def url = new YouTubeVideoURL(id)
        def vget = new VGet(new URL(url.URL))
        vget.extract(new MyYouTubeParser(), new AtomicBoolean(false)) {}

        def fileInfo = vget.video.info.last()

        return fileInfo.source.toString()
    }

    @RequestMapping(value = "/feed")
    public void getFeed(@RequestParam("url") String url) {
        response.sendRedirect("/feed/${new YouTubePlaylistURL(url).id}")
    }

    @RequestMapping(value = "/feed/{id}", produces = MediaType.TEXT_XML_VALUE)
    public String feed(@PathVariable String id) {
        rss.getFeedAsString(id, baseURL)
    }

    @RequestMapping(value = "/audio/{id}", method = RequestMethod.HEAD)
    public void audioHeader(@PathVariable String id) {
        response.setHeader('X-Application-Context', baseURL)
        def info = audio.queryFile(id)

        if (info.mediaInfo != null) {
            response.setContentType(info.mediaInfo.contentType)
            response.setContentLength((int) info.mediaInfo.length)
            response.setStatus(HttpServletResponse.SC_OK)
        }
    }

    @RequestMapping(value = "/audio/{id}", method = RequestMethod.GET)
    public void audio(@PathVariable String id) {
        String url = findUrl(id)

        response.sendRedirect(url)
        return

        log.info(request.headerNames.collect() { new MapEntry(it, request.getHeader(it)) }.toString())

        if (range != null) {
            sendPartial(audio.queryFile(id), range)
            return
        }

        if (canResolve(id, audio.queryFile(id)))
            return

        if (timeout > 0)
            audio.waitFor(id, timeout)
        else
            audio.waitFor(id, 10)

        if (!canResolve(id, audio.queryFile(id))) {
            response.setHeader('Retry-After', '10')
            response.setStatus(HttpServletResponse.SC_NO_CONTENT)
        }
    }

    public void sendPartial(AudioResult action, Range<Long> range) {
            long length = action.file.file.path.length()
            long start = range.from
            long end = Math.min(range.to, length -1)

            log.info("SENDING partial ${start}-${end}/${action.mediaInfo.length} (${length})")

            if (end < start) {
                response.setStatus(HttpServletResponse.SC_REQUESTED_RANGE_NOT_SATISFIABLE)
                return
            }

            boolean isDownloading = action.action == AudioResult.Status.WAIT

            response.setStatus(HttpServletResponse.SC_PARTIAL_CONTENT)
            response.setContentType(action.mediaInfo.contentType)
            response.setContentLength((int) end - start + 1)
//            bytes 21010-47021/47022
            response.setHeader('Accept-Ranges', 'bytes')
            response.setHeader('Content-Range', "bytes ${start}-${end}/${isDownloading ? '*' : action.mediaInfo.length}")

//            Utils.copy(raf, response.outputStream, start, (int) end - start + 1)

            response.setBufferSize(StreamUtils.BUFFER_SIZE);

            StreamUtils.copyRange(new BufferedInputStream(action.stream), response.outputStream, start, end)

        log.info(response.headerNames.collect() { new MapEntry(it, response.getHeader(it)) }.toString())
    }

    private boolean canResolve(String id, AudioResult action) {
        if (action.action == AudioResult.Status.START) {
            download(action)
            return true
        } else if (action.action == AudioResult.Status.UNAVAILABLE) {
            response.setStatus(HttpServletResponse.SC_NOT_FOUND)
            return true
        } else if (action.hasFile()) {
            sendSlow(id, action)
            return true
//            def raf
//
//            try {
//                raf = action.getRaf()
//
//                long start = 0
//                long end = (raf.length() / 512 / 2) * 512 -1;
//
//                sendPartial(action, start..end)
//
//                return true
//            } finally {
//                if (raf != null) try {
//                    raf.close()
//                } catch (Exception ignored) {}
//            }

        }

        return false
    }
    private void download(AudioResult action) {
//        if (action.mediaInfo.length > 1024 * 1024 * 10) {
//            sendPartial(action, 0..(1024 -1))
//            return
//        }

        response.setContentType(action.mediaInfo.contentType)
        response.setContentLength((int) action.mediaInfo.length)
        response.setStatus(HttpServletResponse.SC_OK)
        response.setBufferSize(StreamUtils.BUFFER_SIZE)

        def os = response.outputStream
        log.info("Sending content: ${action.mediaInfo.dump()}")
        StreamUtils.copy(new BufferedInputStream(action.stream), os)
    }

    public void sendSlow(String id, AudioResult action) {
        response.setContentType(action.mediaInfo.contentType)
        response.setContentLength((int) action.mediaInfo.length)
        response.setStatus(HttpServletResponse.SC_OK)
        response.setBufferSize(StreamUtils.BUFFER_SIZE)

        File file = action.file.file.path
        long contentLength = action.mediaInfo.length

        InputStream is = new BufferedInputStream(new FileInputStream(file))
        OutputStream os = response.outputStream

        long sent = 0

        long fileSize = file.length()


        byte[] buffer = new byte[StreamUtils.BUFFER_SIZE]
        while (sent < fileSize || audio.isDownloading(id)) {
            long available = fileSize - sent;
            while (available  < StreamUtils.BUFFER_SIZE * 10 && audio.isDownloading(id)) {
                log.info("NOT available... Waiting. fileSize: ${fileSize}, sent: $sent ${file}")
                try {
                    Thread.sleep(500)
                } catch (InterruptedException ignored) {
                }

                fileSize = file.length()
                available = fileSize - sent;
            }


            int toCopy = Math.min(available, StreamUtils.BUFFER_SIZE)
            int read = is.read(buffer, 0, toCopy)

            if (read == -1) {
                log.error("END of STREAM!")
                return
            }

            if (read == 0) {
                log.warn("READ 0!")
                continue
            }

            Thread.sleep(10)

            os.write(buffer, 0, read)

//            int copied = StreamUtils.copyRange(is, os, 0, toCopy)

            int copied = read;

            sent += copied

            log.info("Copied: ${copied}, Sent: ${(long)sent/1024}/${(long)available/1024}/${(long)contentLength/1024}")
            fileSize = file.length()
        }

        os.flush()
    }

    static long copyUntil(InputStream is, OutputStream os, long bytes) {
        StreamUtils.copyRange(is, os, 0, bytes)
    }

}
