package gcms.youtube2podcast

import java.util.regex.Pattern

/**
 * Created by gustavo on 19/02/17.
 */
class YouTubePlaylistURL {
    String id

    public YouTubePlaylistURL(String id) {
        this.id = id.contains('youtube.com') ? extractId(id) : id
    }

    private static Pattern ID_PATTERN = ~/list=([^&]+)&?/
    private static String extractId(String url) {
        def m = ID_PATTERN.matcher(url)
        if (!m.find())
            throw new IllegalArgumentException("Invalid Playlist URL. Doesn't include list parameter")

        return m.group(1)
    }

    String getURL() {
        "https://www.youtube.com/playlist?list=${id}"
    }
}
