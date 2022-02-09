import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class EntityLocker<T> {

  private final ConcurrentMap<T, ExtendedReentrantLock> locks = new ConcurrentHashMap<>();

  public void runWithLock(T id, Runnable protectedCode) {
    ExtendedReentrantLock reentrantLock =
        locks.computeIfAbsent(id, k -> new ExtendedReentrantLock());

    reentrantLock.lock();

    try {
      protectedCode.run();
    } finally {
      reentrantLock.unlock();
    }
  }
}
