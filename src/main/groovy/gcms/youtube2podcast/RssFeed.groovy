package gcms.youtube2podcast
import com.sun.syndication.feed.rss.*
import com.sun.syndication.io.WireFeedOutput
import org.jsoup.Jsoup
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
/**
 * Created by gustavo on 19/02/17.
 */
@Service
class RssFeed {
    @Autowired
    FileManager fileManager

    Channel getFeed(YouTubePlaylistURL url, String hostname) {
        Channel channel = new Channel("rss_2.0");

        def doc = Jsoup.connect(url.getURL()).get();

        String playlistTitle = doc.select(".pl-header-title").text();
        String playlistAuthor = doc.select(".pl-header-details a:first-child").text()
        String playlistThumb = doc.select(".pl-header-thumb img").attr('src')


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

            Enclosure enclosure = new Enclosure( );
            link = "http://${hostname}/audio/${getId(link)}"
            enclosure.setUrl(link);

            MediaInfo info = fileManager.getFileInfo(getId(link));
            if (info != null) {
                enclosure.length = info.size
                enclosure.type = info.contentType
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
