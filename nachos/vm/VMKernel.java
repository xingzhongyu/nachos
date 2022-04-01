package nachos.vm;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;
import nachos.vm.*;

import java.util.Hashtable;

/**
 * A kernel that can support multiple demand-paging user processes.
 */
public class VMKernel extends UserKernel {
    /**
     * Allocate a new VM kernel.
     */
    public VMKernel() {
	super();
    }

    /**
     * Initialize this kernel.
     */
    public void initialize(String[] args) {
	super.initialize(args);
    }

    /**
     * Test this kernel.
     */	
    public void selfTest() {
	super.selfTest();
    }
//    public TranslationEntry getRetrievePage(){
//
//        return null;
//    }
    /**
     * Start running user programs.
     */
    public void run() {
	super.run();
    }
    
    /**
     * Terminate this kernel. Never returns.
     */
    public void terminate() {
	super.terminate();
    }
//    public class tableKey{
//        int vpn;
//        int pid;
//
//        public tableKey(int vpn, int pid) {
//            this.vpn = vpn;
//            this.pid = pid;
//        }
//    }

    // dummy variables to make javac smarter
    private static VMProcess dummy1 = null;

    private static final char dbgVM = 'v';
//    private static Hashtable<tableKey,Integer> hashtable=new Hashtable<>();


}
