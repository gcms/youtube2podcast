package com.github.axet.wget;

import com.github.axet.wget.info.DownloadInfo;
import com.github.axet.wget.info.URLInfo;
import com.github.axet.wget.info.ex.DownloadInterruptedError;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by gustavo on 20/02/17.
 */
public class MyDirect extends DirectSingle {
    /**
     * @param info   download file information
     * @param target
     */
    public MyDirect(DownloadInfo info, File target) {
        super(info, target);
        this.info = info;
    }

    public DownloadInfo getInfo() {
        return info;
    }

    void downloadToStream(RetryWrap.Wrap w, OutputStream target, DownloadInfo info, AtomicBoolean stop, Runnable notify) throws IOException {
        OutputStream fos = target;
        try {
            HttpURLConnection conn = info.openConnection();

            info.setCount(0);

            fos = target;

            byte[] bytes = new byte[BUF_SIZE];
            int read = 0;

            RetryWrap.check(conn);

            BufferedInputStream binaryreader = new BufferedInputStream(conn.getInputStream());

            while ((read = binaryreader.read(bytes)) > 0) {
                w.resume();
                fos.write(bytes, 0, read);
                info.setCount(info.getCount() + read);
                notify.run();

                if (stop.get())
                    throw new DownloadInterruptedError("stop");
                if (Thread.interrupted())
                    throw new DownloadInterruptedError("interrupted");
            }

            binaryreader.close();
        } finally {
            if (fos != null)
                fos.close();
        }
    }


    public void download(final OutputStream target, final AtomicBoolean stop, final Runnable notify) {
        info.setState(URLInfo.States.DOWNLOADING);
        notify.run();
        try {
            RetryWrap.wrap(stop, new RetryWrap.Wrap() {
                @Override
                public void proxy() {
                    info.getProxy().set();
                }

                @Override
                public void resume() {
                    info.setRetry(0);
                }

                @Override
                public void error(Throwable e) {
                    info.setRetry(info.getRetry() + 1);
                }

                @Override
                public void download() throws IOException {
                    info.setState(URLInfo.States.DOWNLOADING);
                    notify.run();
                    downloadToStream(this, target, info, stop, notify);
                }

                @Override
                public boolean retry(int delay, Throwable e) {
                    info.setDelay(delay, e);
                    notify.run();
                    return RetryWrap.retry(info.getRetry());
                }

                @Override
                public void moved(URL url) {
                    info.setState(URLInfo.States.RETRYING);
                    notify.run();
                }
            });
            info.setState(URLInfo.States.DONE);
            notify.run();
        } catch (DownloadInterruptedError e) {
            info.setState(URLInfo.States.STOP);
            notify.run();
            throw e;
        } catch (RuntimeException e) {
            info.setState(URLInfo.States.ERROR);
            notify.run();
            throw e;
        }
    }
}
