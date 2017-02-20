package gcms.youtube2podcast

import com.github.axet.vget.VGet
import com.github.axet.vget.info.VideoFileInfo
import com.github.axet.vget.vhs.YouTubeParser
import com.sun.syndication.feed.rss.*
import com.sun.syndication.io.WireFeedOutput
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by gustavo on 19/02/17.
 */
@Service
class RssFeed {
    @Autowired
    VideoDownloadInfoRepo repo

    String getFeedForVideo(YouTubeVideoURL url) {
        Channel channel = new Channel("rss_2.0");
        channel.setTitle("File adas das sa")
        channel.setDescription("babla lsdla dla sd")
        channel.setLink(url.URL)

        def vget = new VGet(new URL(url.URL))
        def parser = new MyYouTubeParser()
        vget.extract(parser, new AtomicBoolean(false)) {}

        parser.files.collect { YouTubeParser.VideoDownload it ->
            [it, new VideoFileInfo(it.url)]
        }.findAll {
            VideoFileInfo info = it[1]
            try {
                info.setReferer(new URL(url.URL))
                info.extract()

                return true
            } catch (Exception ignored) {
                return false
            }
        }.collect {
            YouTubeParser.VideoDownload download = it[0]
            VideoFileInfo info = it[1]

            Item item = new Item();
            item.setTitle(download.stream.toString());

            Description desc = new Description();
            desc.setType("html");
            desc.setValue(info.properties.collect {
                "<p>${it.key}: ${it.value}</p>"
            }.join(""))

            item.setDescription(desc);
            item.setLink(download.url.toString());

            Enclosure enclosure = new Enclosure( );
            enclosure.setUrl(download.url.toString());
            enclosure.setLength(info.length)
            enclosure.setType(info.contentType)

            item.getEnclosures().add(enclosure);

            channel.getItems().add(item);
        }

        StringWriter sw = new StringWriter()
        WireFeedOutput outputter = new WireFeedOutput();
        outputter.output(channel, sw);
        sw.flush()

        sw.toString()
    }

    Channel getFeed(YouTubePlaylistURL url, String hostname) {
        Channel channel = new Channel("rss_2.0");

        def doc = Jsoup.connect(url.getURL()).get();

        String playlistTitle = doc.select(".pl-header-title").text();
        String playlistAuthor = doc.select(".pl-header-details a:first-child").text()
        String playlistThumb = doc.select(".channel-header-profile-image").attr('src')


        channel.setLink(url.getURL());
        channel.setDescription("$playlistTitle by $playlistAuthor");
        channel.setTitle("${playlistAuthor} - ${playlistTitle}");

        def image = new Image()
        image.setUrl(playlistThumb)
        image.setTitle("cover")
        channel.setImage(image)
        channel.setLastBuildDate(new Date())

        def elements = doc.select("tr.pl-video")
        elements.each {
            def thumb = it.select(".pl-video-thumbnail img").attr('data-thumb')
            def title = it.select(".pl-video-title-link").text()
            def author = it.select(".pl-video-owner a").text()
            def time = it.select(".pl-video-time").text()
            def link = "https://www.youtube.com" + it.select("a.pl-video-title-link").attr('href')

            Item item = new Item();
            item.setAuthor(author)
            item.setTitle(title);

            Description desc = new Description();
            desc.setType("html");
            desc.setValue("<img src=\"${thumb}\"/>");
            item.setDescription(desc);
            item.setLink(link);

            Enclosure enclosure = new Enclosure( );
            enclosure.setUrl("http://${hostname}/audio/${getId(link)}");

            VideoDownloadInfo info = repo.getInfo(getId(link));
            if (info?.file?.mediaInfo != null) {
                enclosure.length = info.file.mediaInfo.length
                enclosure.type = info.file.mediaInfo.contentType
            }
            item.getEnclosures().add(enclosure);

            channel.getItems().add(item);
        }

        return channel
    }

    String getFeedAsString(String url, String hostname) {
        Channel channel = getFeed(new YouTubePlaylistURL(url), hostname)
        StringWriter sw = new StringWriter()
        WireFeedOutput outputter = new WireFeedOutput();
        outputter.output(channel, sw);
        sw.flush()

        sw.toString()
    }

    String getId(String url) {
        new YouTubeVideoURL(url).id
    }
}
