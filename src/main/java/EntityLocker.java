import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.locks.ReentrantLock;

public class EntityLocker<T> {

  private final ConcurrentMap<T, ReentrantLock> locks = new ConcurrentHashMap<>();

  public void runWithLock(T id, Runnable protectedCode) {
    ReentrantLock reentrantLock = locks.computeIfAbsent(id, k -> new ReentrantLock());

    reentrantLock.lock();

    try {
      protectedCode.run();
    } finally {
      reentrantLock.unlock();
    }
  }
}
