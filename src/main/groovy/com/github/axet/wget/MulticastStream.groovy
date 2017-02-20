package com.github.axet.wget


/**
 * Created by gustavo on 20/02/17.
 */
class MulticastStream extends OutputStream {

    private Set<OutputStream> streams = new HashSet<>();

    MulticastStream(Set<OutputStream> streams) {
        this.streams.addAll(streams)
    }

    MulticastStream(OutputStream... streams) {
        this.streams.addAll(streams)
    }

    @Override
    void write(int b) throws IOException {
        streams.each { it.write() }
    }

    @Override
    void write(byte[] buffer, int start, int len) {
        streams.each {
            it.write(buffer, start, len)
        }
    }

    @Override
    void write(byte[] buffer) {
        streams.each {
            it.write(buffer)
        }
    }
}
