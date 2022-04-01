package nachos.threads;

import nachos.machine.*;

import java.util.*;

/**
 * A scheduler that chooses threads using a lottery.
 *
 * <p>
 * A lottery scheduler associates a number of tickets with each thread. When a
 * thread needs to be dequeued, a random lottery is held, among all the tickets
 * of all the threads waiting to be dequeued. The thread that holds the winning
 * ticket is chosen.
 *
 * <p>
 * Note that a lottery scheduler must be able to handle a lot of tickets
 * (sometimes billions), so it is not acceptable to maintain state for every
 * ticket.
 *
 * <p>
 * A lottery scheduler must partially solve the priority inversion problem; in
 * particular, tickets must be transferred through locks, and through joins.
 * Unlike a priority scheduler, these tickets add (as opposed to just taking
 * the maximum).
 */
public class LotteryScheduler extends PriorityScheduler {
    /**
     * Allocate a new lottery scheduler.
     */
    public LotteryScheduler() {
    }

    /**
     * Allocate a new lottery thread queue.
     *
     * @param    transferPriority    <tt>true</tt> if this queue should
     * transfer tickets from waiting threads
     * to the owning thread.
     * @return a new lottery thread queue.
     */
    public ThreadQueue newThreadQueue(boolean transferPriority) {
        // implement me
       return new LotteryQueue(transferPriority);



    }

    @Override
    protected ThreadState getThreadState(KThread thread) {
        if (thread.schedulingState == null)
            thread.schedulingState = new ThreadState(thread);



        return (ThreadState) thread.schedulingState;//或许这里的多态有帮助

    }

    protected class LotteryQueue extends PriorityScheduler.PriorityQueue{


        LotteryQueue(boolean transferPriority) {
            super(transferPriority);
        }

        @Override
        protected PriorityScheduler.ThreadState pickNextThread() {
          ThreadState threadState=null;
          if (!InnerWaitQueue.isEmpty()){
                int lottery=0;
                KThread[] kThreads= InnerWaitQueue.toArray(new KThread[0]);
                for (KThread kThread:kThreads){
                    lottery+=getThreadState(kThread).getEffectivePriority();
                }
                int random=Lib.random(lottery+1);
                int ans=0;
                for (KThread kThread:kThreads){
                    ans+=getThreadState(kThread).getEffectivePriority();
                    if (ans>=random){
                        threadState=getThreadState(kThread);
                        break;
                    }

                }


                //得到相应的threadState
          }

//          List<String> strings=new ArrayList<>();
//          strings.add("KThread1");
//          strings.add("KThread2");
//          strings.add("KThread3");
//             if(threadState != null&&strings.contains(threadState.thread.getName()) ) {
//                 System.out.println(threadState.getEffectivePriority());
//
//             }



          return threadState;
        }
    }
    protected class ThreadState extends PriorityScheduler.ThreadState{

        /**
         * Allocate a new <tt>ThreadState</tt> object and associate it with the
         * specified thread.
         *
         * @param thread the thread this state belongs to.
         */
        public ThreadState(KThread thread) {
            super(thread);
        }


        @Override
        public int getEffectivePriority(){
            if (max==-2){
                max=priority;
                for (PriorityQueue list:lists){
                    for (KThread kThread:list.InnerWaitQueue){
                        max=max+getThreadState(kThread).getEffectivePriority();
                        //这里使用了多态的方法，可能有效，需要对代码再次测评

                    }
                }
            }
            return max;
        }

    }
}
