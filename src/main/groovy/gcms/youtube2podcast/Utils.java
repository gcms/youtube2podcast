package gcms.youtube2podcast;

import java.io.DataInput;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;

/**
 * Created by gustavo on 19/02/17.
 */
public class Utils {
    public static final int BUFFER_SIZE = 4096;

    public static void copy(RandomAccessFile randomAccess, OutputStream os, long start, int size) throws IOException {
        byte[] buffer = new byte[BUFFER_SIZE];

        randomAccess.seek(start);

        while (size > 0) {
            int toRead = Math.min(size, BUFFER_SIZE);
            int readBytes = randomAccess.read(buffer, 0, toRead);
            os.write(buffer, 0, readBytes);
            size -= readBytes;
        }

        os.flush();
        assert size == 0;
    }
}
