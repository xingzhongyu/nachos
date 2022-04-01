package nachos.threads;

import nachos.machine.*;

/**
 * An implementation of condition variables that disables interrupt()s for
 * synchronization.
 *
 * <p>
 * You must implement this.
 *
 * @see    nachos.threads.Condition
 */
public class Condition2 {
    /**
     * Allocate a new condition variable.
     *
     * @param    conditionLock    the lock associated with this condition
     * variable. The current thread must hold this
     * lock whenever it uses <tt>sleep()</tt>,
     * <tt>wake()</tt>, or <tt>wakeAll()</tt>.
     */
    public Condition2(Lock conditionLock) {
        this.conditionLock = conditionLock;
    }

    /**
     * Atomically release the associated lock and go to sleep on this condition
     * variable until another thread wakes it using <tt>wake()</tt>. The
     * current thread must hold the associated lock. The thread will
     * automatically reacquire the lock before <tt>sleep()</tt> returns.
     */
    public void sleep() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());


        boolean status = Machine.interrupt().disable();
        conditionLock.release();

        KThread kThread = KThread.currentThread();
        waitQueue.waitForAccess(kThread);
        KThread.sleep();
        conditionLock.acquire();
        Machine.interrupt().restore(status);

    }

    /**
     * Wake up at most one thread sleeping on this condition variable. The
     * current thread must hold the associated lock.
     */
    public void wake() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean status = Machine.interrupt().disable();//注意方法


        KThread kThread = waitQueue.nextThread();
        if (kThread != null) {
            kThread.ready();
        }
        Machine.interrupt().restore(status);
    }

    /**
     * Wake up all threads sleeping on this condition variable. The current
     * thread must hold the associated lock.
     */
    public void wakeAll() {
        Lib.assertTrue(conditionLock.isHeldByCurrentThread());
        boolean status = Machine.interrupt().disabled();
        KThread kThread = waitQueue.nextThread();
        while (kThread != null) {
            kThread.ready();
            kThread = waitQueue.nextThread();


        }
        Machine.interrupt().restore(status);
    }

    private Lock conditionLock;
    private ThreadQueue waitQueue = ThreadedKernel.scheduler.newThreadQueue(true);
}
