package gcms.youtube2podcast

import org.jboss.logging.Param
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.MediaType
import org.springframework.util.StreamUtils
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

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

    @RequestMapping(value = "/feed")
    public void getFeed(@RequestParam("url") String url) {
        response.sendRedirect("/feed/${new YouTubePlaylistURL(url).id}")
    }

    @RequestMapping(value = "/feed/{id}", produces = MediaType.TEXT_XML_VALUE)
    public String feed(@PathVariable String id) {
        rss.getFeedAsString(id, baseURL)
    }

    @RequestMapping(value = "/audio/{id}")
    public void audio(@PathVariable String id) {
        if (canResolve(audio.queryFile(id)))
            return

        if (timeout > 0)
            audio.waitFor(id, timeout)
        else
            audio.waitFor(id, 120)

        if (!canResolve(audio.queryFile(id))) {
            response.setHeader('Retry-After', '10')
            response.setStatus(204)
        }
    }

    private boolean canResolve(AudioResult action) {
        if (action.action == AudioResult.Status.START) {
            download(action)
            return true
        } else if (action.action == AudioResult.Status.UNAVAILABLE) {
            response.setStatus(404)
            return true
        }

        return false
    }

    private void download(AudioResult action) {
        log.info("Sending content: ${action.mediaInfo.dump()}")
        response.setContentType(action.mediaInfo.contentType)
        response.setContentLength((int) action.mediaInfo.length)
        response.setStatus(200)
        StreamUtils.copy(action.stream, response.outputStream)
    }
}
