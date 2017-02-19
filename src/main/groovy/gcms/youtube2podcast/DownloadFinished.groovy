package gcms.youtube2podcast

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition

/**
 * Created by gustavo on 19/02/17.
 */
class DownloadFinished implements Condition {
    @Override
    void await() throws InterruptedException {

    }

    @Override
    void awaitUninterruptibly() {

    }

    @Override
    long awaitNanos(long nanosTimeout) throws InterruptedException {
        return 0
    }

    @Override
    boolean await(long time, TimeUnit unit) throws InterruptedException {
        return false
    }

    @Override
    boolean awaitUntil(Date deadline) throws InterruptedException {
        return false
    }

    @Override
    void signal() {

    }

    @Override
    void signalAll() {

    }
}
