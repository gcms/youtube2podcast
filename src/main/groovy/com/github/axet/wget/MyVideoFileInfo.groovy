package com.github.axet.wget

import com.github.axet.vget.info.VideoFileInfo

/**
 * Created by gustavo on 20/02/17.
 */
class MyVideoFileInfo extends VideoFileInfo {
    private Map<String, String> headers = [:]

    MyVideoFileInfo(URL source) {
        super(source)
    }

//    accept:*/*,
// accept-encoding:gzip,
// range:bytes=6029280-,
// host:gustavocms.ddns.net:8080,
// connection:Keep-Alive,
// user-agent:Mozilla/5.0 (Linux; U; en-us; BeyondPod)
    public HttpURLConnection openConnection() throws IOException {
        URL url = getSource();

        HttpURLConnection conn;

        if (getProxy() != null) {
            conn = (HttpURLConnection) url.openConnection(getProxy().proxy);
        } else {
            conn = (HttpURLConnection) url.openConnection();
        }


        conn.setRequestProperty('accept', '*/*')
        conn.setRequestProperty('accept-encoding', 'gzip')
        conn.setRequestProperty('connection', 'Keep-Alive')
        conn.setRequestProperty('user-agent', 'Mozilla/5.0 (Linux; U; en-us; BeyondPod)')

        conn
    }
}
