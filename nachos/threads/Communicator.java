package nachos.threads;

import nachos.machine.*;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>,
 * and multiple threads can be waiting to <i>listen</i>. But there should never
 * be a time when both a speaker and a listener are waiting, because the two
 * threads can be paired off at this point.
 */
public class Communicator {
    /**
     * Allocate a new communicator.
     */
    Lock lock;
    boolean hasTran;
//    int speakerNum = 0;
//    int listenerNum = 0;
    Condition2 speakerCondition;
    Condition2 listenerCondition;
    Integer word;

    public Communicator() {
        lock = new Lock();

        hasTran=true;



        speakerCondition = new Condition2(lock);
        listenerCondition = new Condition2(lock);
    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param    word    the integer to transfer.
     */
    public void speak(int word) {
//        boolean status=Machine.interrupt().disable();
        lock.acquire();
        if (!hasTran){
            speakerCondition.sleep();
        }
        hasTran=false;
        this.word=word;
        listenerCondition.wake();
        speakerCondition.sleep();

//        System.out.println("speaker"+words);


//        if (listenerNum == 0) {
//            speakerNum++;
//            speakerCondition.sleep();//在睡眠函数内部会有锁的释放
//
//        }else {
////            speakerNum++;
//            listenerNum--;
//
//            listenerCondition.wake();
//        }


        lock.release();
//    Machine.interrupt().restore(status);

    }

    /**
     * Wait for a thread to speak through this communicator, and then return
     * the <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return the integer transferred.
     */
    public int listen() {
//        boolean status=Machine.interrupt().disable();
        lock.acquire();
        if (hasTran){
            listenerCondition.sleep();
        }

        hasTran=true;


            //        System.out.println("listener"+words);
//        int word = words.poll();


//        listenerCondition.sleep();
        speakerCondition.wake();
//        if (speakerNum == 0) {
//            listenerNum++;
//            listenerCondition.sleep();
////            speakerNum--;
////            System.out.println(listenerCondition);
//        }else {
//
//            speakerNum--;
//
//            speakerCondition.wake();
//
//
//        }
//
//        System.out.println("开始倾听");



        lock.release();
//        Machine.interrupt().restore(status);
        return word;

    }
}
