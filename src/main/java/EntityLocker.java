import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

public class EntityLocker<T> {

  private final ConcurrentMap<T, ReentrantLock> locks = new ConcurrentHashMap<>();

  public void tryWithLock(T id, Runnable protectedCode) {
    ReentrantLock reentrantLock = locks.computeIfAbsent(id, k -> new ReentrantLock());
    reentrantLock.lock();
    executeProtectedCodeAndUnlock(id, protectedCode, reentrantLock);
  }

  public void tryWithLock(T id, long timeout, Runnable protectedCode) throws InterruptedException {
    ReentrantLock reentrantLock = locks.computeIfAbsent(id, k -> new ReentrantLock());
    boolean locked = reentrantLock.tryLock(timeout, TimeUnit.MILLISECONDS);
    if (locked) {
      executeProtectedCodeAndUnlock(id, protectedCode, reentrantLock);
    }
  }

  private void executeProtectedCodeAndUnlock(T id, Runnable protectedCode,
      ReentrantLock reentrantLock) {
    try {
      protectedCode.run();
    } finally {
      if (!reentrantLock.isHeldByCurrentThread()) {
        locks.remove(id);
      }
      reentrantLock.unlock();
    }
  }
}
