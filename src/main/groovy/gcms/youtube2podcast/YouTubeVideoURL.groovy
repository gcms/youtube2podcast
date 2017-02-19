package gcms.youtube2podcast

/**
 * Created by gustavo on 19/02/17.
 */
import java.util.regex.Pattern

/**
 * Created by gustavo on 15/02/17.
 */
class YouTubeVideoURL {
    private String id

    public YouTubeVideoURL(String id) {
        this.id = id.contains('youtube.com') ? extractId(id) : id
    }


    public YouTubeVideoURL(URL url) {
        this.id = extractId(url.toString())
    }

    private static Pattern ID_PATTERN = ~/v=([^&]+)&?/
    private static String extractId(String url) {
        def m = ID_PATTERN.matcher(url)
        if (!m.find())
            throw new IllegalArgumentException("Invalid URL. Doesn't include video parameter")

        return m.group(1)
    }

    String getId() {
        id
    }

    String getURL() {
        "http://www.youtube.com/watch?v=$id"
    }

    String toString() {
        getURL()
    }
}
