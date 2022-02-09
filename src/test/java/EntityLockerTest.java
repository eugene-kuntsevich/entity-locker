import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.ArrayList;
import java.util.List;
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
      entityLocker.runWithLock(counterEntity.id, () -> {
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
