package com.github.axet.wget
import com.github.axet.vget.VGet
import com.github.axet.vget.info.VideoFileInfo
import com.github.axet.vget.vhs.YouTubeInfo
import com.github.axet.wget.info.DownloadInfo
import gcms.youtube2podcast.MyYouTubeParser
import gcms.youtube2podcast.YouTubeVideoURL

import java.text.DecimalFormat
import java.text.NumberFormat
import java.util.concurrent.atomic.AtomicBoolean
/**
 * Created by gustavo on 20/02/17.
 */
class Example {
    static void main(String[] args) {
        def urlString = 'https://www.youtube.com/watch?v=mA7B1Q6voXI'
        def url = new YouTubeVideoURL(urlString)

        def parser = new MyYouTubeParser()
        def vget = new VGet(new URL(url.URL))
        vget.extract(parser, new AtomicBoolean(false)) {}

        parser.files.each {
            println it.stream
            println it.url

            def fi = new VideoFileInfo(it.url)
            fi.setReferer(new URL(url.URL))
            fi.extract()

            println "Size: ${getSize(fi.length)}, Type: ${fi.getContentType()}, File: ${fi.contentFilename}"
            println '-----'

            if (it.stream instanceof YouTubeInfo.StreamAudio) {
                println "Downloading ${it.stream}"
                def target = new File("/tmp/temp/${it.stream}")
                if (target.exists()) target.delete()

                def fos = new FileOutputStream(target)
                def pis = new PipedInputStream()
                def pos = new PipedOutputStream(pis)



                long lastUpdate = System.currentTimeMillis()
                def wget = new MyDirect(new DownloadInfo(it.url), target)

                wget.download(new MulticastStream(fos), new AtomicBoolean(false)) {
                    if (System.currentTimeMillis() - lastUpdate > 10 * 1000) {
                        lastUpdate = System.currentTimeMillis()
                        println "${wget.info.state} ${nf.format(wget.info.count/1024/1024)}M"// / ${nf.format(wget.info.length/1024/1024)}M"
                    }
                }

                println "Total: ${getSize(target.length())}"
            } else {
                println "Skip..."
            }
        }
    }

    static String getSize(long length) {
        if (length < 1024)
            return length;

        if (length < 1024 * 1024)
            return nf.format(length / 1024) + 'K'

        if (length < 1024 * 1024 * 1024)
            return nf.format(length / 1024 / 1024) + 'M'

        return nf.format(length / 1024 / 1024 / 1024) + 'G'
    }

    static NumberFormat nf = new DecimalFormat("0.00")
}
