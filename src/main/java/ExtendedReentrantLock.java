import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

public class ExtendedReentrantLock extends ReentrantLock {

  private final AtomicInteger lockCounter = new AtomicInteger(0);

  public void lock() {
    lockCounter.getAndIncrement();
    super.lock();
  }

  public void unlock() {
    lockCounter.getAndDecrement();
    super.unlock();
  }

  public int getCountOfLocks() {
    return lockCounter.get();
  }
}
