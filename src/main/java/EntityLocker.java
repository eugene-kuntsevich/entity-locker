import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

public class EntityLocker<T> {

  private final ConcurrentHashMap<T, ReentrantLock> locks = new ConcurrentHashMap<>();

  public void lock(T id) throws InterruptedException {
    ReentrantLock reentrantLock = locks.computeIfAbsent(id, k -> new ReentrantLock());
    reentrantLock.lock();
  }

  public void unlock(T id) {
    ReentrantLock reentrantLock = locks.get(id);
    reentrantLock.unlock();
    /*if (reentrantLock.getHoldCount() == 0) {
      locks.remove(id);
    }*/
  }
}
