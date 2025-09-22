package actor;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public abstract class ActorThread<M> extends Thread {

    private final LinkedBlockingQueue<M> messageQueue = new LinkedBlockingQueue<>();

    /** Called by another thread, to send a message to this thread. */
    public void send(M message) {
        messageQueue.offer(message);
    }
    
    /** Returns the first message in the queue, or blocks if none available. */
    protected M receive() throws InterruptedException {
        return messageQueue.take();
    }
    
    /** Returns the first message in the queue, or blocks up to 'timeout'
        milliseconds if none available. Returns null if no message is obtained
        within 'timeout' milliseconds. */
    protected M receiveWithTimeout(long timeout) throws InterruptedException {
        return messageQueue.poll(timeout, TimeUnit.MILLISECONDS);
    }
}