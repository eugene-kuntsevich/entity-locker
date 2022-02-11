import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class EntityLockerTest {

  private static final int THREADS = 700;
  private static final int ITERATIONS = 1000;

  @Test
  void lockingEntityInMultiThreadsAndExecutingProtectedCode() {
    EntityLocker<Long> entityLocker = new EntityLocker<>();
    CounterEntity entity = new CounterEntity(333L);
    List<Thread> threads = new ArrayList<>(THREADS);

    for (int i = 0; i < THREADS; i++) {
      Thread thread = new Thread(new CounterHandler(entity, entityLocker));
      threads.add(thread);
    }

    threads.forEach(Thread::start);
    threads.forEach(this::threadJoin);

    assertEquals(THREADS * ITERATIONS, entity.counter);
  }

  @Test
  void lockingTwoDiffEntitiesInMultiThreadsAndExecutingProtectedCode() {
    EntityLocker<Long> entityLocker = new EntityLocker<>();

    //prepare threads for the first entity
    CounterEntity firstEntity = new CounterEntity(333L);
    List<Thread> firstThreadsPool = new ArrayList<>(THREADS);
    for (int i = 0; i < THREADS; i++) {
      Thread thread = new Thread(new CounterHandler(firstEntity, entityLocker));
      firstThreadsPool.add(thread);
    }
    firstThreadsPool.forEach(Thread::start);
    firstThreadsPool.forEach(this::threadJoin);

    //prepare threads for the second entity
    CounterEntity secondEntity = new CounterEntity(999L);
    List<Thread> secondThreadsPool = new ArrayList<>(THREADS);
    for (int i = 0; i < THREADS; i++) {
      Thread thread = new Thread(new CounterHandler(secondEntity, entityLocker));
      secondThreadsPool.add(thread);
    }
    secondThreadsPool.forEach(Thread::start);
    secondThreadsPool.forEach(this::threadJoin);

    //asserts
    assertEquals(THREADS * ITERATIONS, firstEntity.counter);
    assertEquals(THREADS * ITERATIONS, secondEntity.counter);
  }

  @Test
  void lockAcquiredAndProtectedCodeNotExecutedBecauseTimeoutExpired() throws InterruptedException {
    AtomicInteger counter = new AtomicInteger();
    prepareTestDataForTimeoutTesting(counter, -10);
    assertEquals(0, counter.get());
  }

  @Test
  void lockReleasedAfterDelayAndProtectedCodeExecuted() throws InterruptedException {
    AtomicInteger counter = new AtomicInteger();
    prepareTestDataForTimeoutTesting(counter, 10);
    assertEquals(1, counter.get());
  }

  private void prepareTestDataForTimeoutTesting(AtomicInteger counter, int delay)
      throws InterruptedException {
    EntityLocker<Integer> entityLocker = new EntityLocker<>();
    CountDownLatch countDownLatch = new CountDownLatch(1);
    int timeout = 100;
    int id = 333;

    //thread for blocking lock
    new Thread(() -> entityLocker.tryWithLock(id, () ->
    {
      countDownLatch.countDown();
      try {
        Thread.sleep(timeout - delay);
      } catch (InterruptedException e) {
        fail();
      }
    })
    ).start();

    countDownLatch.await();
    assertEquals(0, countDownLatch.getCount());

    //trying to acquire lock and execute protected code
    entityLocker.tryWithLock(id, timeout, counter::getAndIncrement);
  }

  private void threadJoin(Thread thread) {
    try {
      thread.join();
    } catch (InterruptedException e) {
      fail();
    }
  }

  private record CounterHandler(CounterEntity counterEntity, EntityLocker<Long> entityLocker)
      implements Runnable {

    @Override
    public void run() {
      entityLocker.tryWithLock(counterEntity.id, () -> {
        for (int i = 0; i < ITERATIONS; i++) {
          counterEntity.counter++;
        }
      });
    }
  }

  private static class CounterEntity {

    long id;
    int counter = 0;

    public CounterEntity(long id) {
      this.id = id;
    }
  }
}
