package gcms.youtube2podcast

import com.github.axet.vget.info.VideoFileInfo
import com.github.axet.vget.info.VideoInfo
import com.github.axet.vget.vhs.YouTubeInfo
import com.github.axet.vget.vhs.YouTubeInfo.StreamVideo
import com.github.axet.vget.vhs.YouTubeParser
import com.github.axet.wget.info.ex.DownloadError
import com.github.axet.wget.info.ex.DownloadRetry
import com.github.axet.vget.vhs.YouTubeParser.VideoDownload

import java.util.concurrent.atomic.AtomicBoolean

/**
 * Created by gustavo on 19/02/17.
 */
class MyYouTubeParser extends YouTubeParser {

    List<VideoDownload> files = []

    static class AverageQualityFirst implements Comparator<VideoDownload> {
        int isVideo(VideoDownload video) {
            video.stream instanceof StreamVideo ? 0 : 1
        }

        int ordinal(VideoDownload o1) {
            if (o1.stream instanceof YouTubeInfo.StreamCombined) {
                YouTubeInfo.StreamCombined c1 = (YouTubeInfo.StreamCombined) o1.stream;
                return Math.abs(c1.vq.ordinal() - YouTubeInfo.YoutubeQuality.p480.ordinal());
            }
            if (o1.stream instanceof StreamVideo) {
                StreamVideo c1 = (StreamVideo) o1.stream;
                return Math.abs(c1.vq.ordinal() - YouTubeInfo.YoutubeQuality.p480.ordinal());
            }
            if (o1.stream instanceof YouTubeInfo.StreamAudio) {
                YouTubeInfo.StreamAudio c1 = (YouTubeInfo.StreamAudio) o1.stream;
//                return Math.abs(c1.aq.ordinal() - YouTubeInfo.AudioQuality.k96.ordinal());
                return YouTubeInfo.AudioQuality.k24.ordinal() - c1.aq.ordinal()
            }
            throw new RuntimeException("bad video array type");
        }

        @Override
        int compare(VideoDownload o1, VideoDownload o2) {
            int cmp = isVideo(o1) - isVideo(o2)
            if (cmp != 0)
                return cmp

            return ordinal(o1) - ordinal(o2)
        }
    }

    public List<VideoFileInfo> extract(final VideoInfo vinfo, final AtomicBoolean stop, final Runnable notify) {
        List<VideoDownload> videos = extractLinks((YouTubeInfo) vinfo, stop, notify);

        if (videos.size() == 0) {
            // rare error:
            //
            // The live recording you're trying to play is still being processed
            // and will be available soon. Sorry, please try again later.
            //
            // retry. since youtube may already rendrered propertly quality.
            throw new DownloadRetry("empty video download list," + " wait until youtube will process the video");
        }

        List<VideoDownload> audios = new ArrayList<VideoDownload>();

        for (int i = videos.size() - 1; i >= 0; i--) {
            if (videos.get(i).stream == null) {
                videos.remove(i);
            } else if ((videos.get(i).stream instanceof YouTubeInfo.StreamAudio)) {
                audios.add(videos.remove(i));
            }
        }

        Collections.sort(videos, new AverageQualityFirst());
        Collections.sort(audios, new AverageQualityFirst());

        files.addAll(videos)
        files.addAll(audios)

        for (int i = 0; i < videos.size();) {
            VideoDownload v = videos.get(i);

            YouTubeInfo yinfo = (YouTubeInfo) vinfo;
            yinfo.setStreamInfo(v.stream);

            VideoFileInfo info = new VideoFileInfo(v.url);

            if (v.stream instanceof YouTubeInfo.StreamCombined) {
                vinfo.setInfo(Arrays.asList(info));
            }

            if (v.stream instanceof YouTubeInfo.StreamVideo) {
                if (audios.size() > 0) {
                    VideoFileInfo info2 = new VideoFileInfo(audios.get(0).url); // take first (highest quality)
                    vinfo.setInfo(Arrays.asList(info, info2));
                } else {
                    // no audio stream?
                    vinfo.setInfo(Arrays.asList(info));
                }
            }

            vinfo.setSource(v.url);
            return vinfo.getInfo();
        }

        for (int i = 0; i < audios.size();) { // only audio mode?
            VideoFileInfo info = new VideoFileInfo(audios.get(i).url);
            vinfo.setInfo(Arrays.asList(info));

            vinfo.setSource(info.getSource());
            return vinfo.getInfo();
        }

        // throw download stop if user choice not maximum quality and we have no
        // video rendered by youtube

        throw new DownloadError("no video with required quality found,"
                + " increace VideoInfo.setVq to the maximum and retry download");
    }

}
