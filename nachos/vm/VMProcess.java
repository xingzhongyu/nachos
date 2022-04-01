package nachos.vm;

import nachos.machine.*;
import nachos.userprog.*;

import java.util.HashMap;
import java.util.LinkedList;

/**
 * A <tt>UserProcess</tt> that supports demand-paging.
 */
public class VMProcess extends UserProcess {
    /**
     * Allocate a new process.
     */
    public VMProcess() {
	super();
    allocatedPages=new LinkedList<>();
    lazyLoadPages=new HashMap<>();
    TLBStoreIn=new TranslationEntry[Machine.processor().getTLBSize()];
        for (int i = 0; i < TLBStoreIn.length; i++) {
            TLBStoreIn[i]=new TranslationEntry(0,0,false,false,false,false);
        }
    }
    public int getFreePage(){

    int location=-1;
//    boolean status=Machine.interrupt().disable();
//    VMKernel.mainMemoryLock.acquire();
    if (!VMKernel.freePages.isEmpty()){
        location=VMKernel.freePages.removeFirst();

    }

    if (location==-1){

        TranslationEntryDecorators translationEntryDecorators=InvertedPageTable.getInstance().getNextEntry();









        Lib.assertTrue(translationEntryDecorators.translationEntry.valid);
        location=translationEntryDecorators.translationEntry.ppn;


        OutMemory(translationEntryDecorators.pid,translationEntryDecorators.translationEntry.vpn);
        InvertedPageTable.getInstance().removeEntry(translationEntryDecorators.pid,translationEntryDecorators.translationEntry.vpn);


    }

//    VMKernel.mainMemoryLock.release();
//    Machine.interrupt().restore(status);


    return  location;


    }
    @Override
    public int readVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        VMKernel.mainMemoryLock.acquire();


        int vpn=Processor.pageFromAddress(vaddr);

        TranslationEntry translationEntry=InvertedPageTable.getInstance().getEntry(pid,vpn);
        if (!translationEntry.valid){

            int ppn=getFreePage();
            InMemory(ppn,vpn);
        }

            translationEntry.used=true;
            InvertedPageTable.getInstance().changeEntry(pid,translationEntry);

            InvertedPageTable.getInstance().addUseNums(translationEntry);
            InvertedPageTable.getInstance().insertTran(pid,vpn);





        VMKernel.mainMemoryLock.release();
//        System.out.println("所读取虚拟地址为"+vpn+"所读取现实地址为"+translationEntry.ppn);
//        System.out.println(Arrays.toString(data));



        return super.readVirtualMemory(vaddr, data, offset, length);
    }

    @Override
    public TranslationEntry findPage(int i) {
        return InvertedPageTable.getInstance().getEntry(pid,i);
    }


    @Override
    public int writeVirtualMemory(int vaddr, byte[] data, int offset, int length) {
        VMKernel.mainMemoryLock.acquire();
        int vpn=Processor.pageFromAddress(vaddr);
        TranslationEntry translationEntry=InvertedPageTable.getInstance().getEntry(pid,vpn);
//        if (!translationEntry.valid){
//            InMemory(getFreePage(), vpn);
//
//        }
        if (!translationEntry.valid){
            int ppn=getFreePage();
            InvertedPageTable.getInstance().Assert(ppn);
            InMemory(ppn,vpn);





            translationEntry=InvertedPageTable.getInstance().getEntry(pid,vpn);
        }



        translationEntry.used=true;
        translationEntry.dirty=true;
        InvertedPageTable.getInstance().addUseNums(translationEntry);
        InvertedPageTable.getInstance().insertTran(pid,vpn);

        InvertedPageTable.getInstance().changeEntry(pid,translationEntry);
        VMKernel.mainMemoryLock.release();
//        System.out.println("所写入虚拟地址为"+vpn+"所写入现实地址为"+translationEntry.ppn);
//        System.out.println(Arrays.toString(data));


        return super.writeVirtualMemory(vaddr, data, offset, length);
    }
    public void OutMemory(int pid,int vpn){//换出的不一定是自己的PID，但换入的一定是

        TranslationEntry translationEntry=InvertedPageTable.getInstance().getEntry(pid,vpn);
        if (translationEntry==null||!translationEntry.valid){
            return;
        }


       if (this.pid==pid){
           for (int i=0;i<Machine.processor().getTLBSize()  ;i++){
               TranslationEntry translationEntry1=Machine.processor().readTLBEntry(i);
               if (translationEntry1.vpn==translationEntry.vpn&&translationEntry1.ppn==translationEntry.ppn&&translationEntry1.valid){
                   InvertedPageTable.getInstance().updateEntry(pid,translationEntry1);//同步可能存在的TLB与页表

                   translationEntry=InvertedPageTable.getInstance().getEntry(pid,translationEntry.vpn);
                   translationEntry1.valid=false;
                   Machine.processor().writeTLBEntry(i,translationEntry1);
                   break;

               }
           }
       }
//       VMKernel.mainMemoryLock.acquire();
        if (translationEntry.dirty){
            byte[] memory=Machine.processor().getMemory();
            Swapper.getInstance().writeToFile(vpn,pid,memory, translationEntry.ppn*Processor.pageSize);
        }
//        VMKernel.mainMemoryLock.release();

    }
    public void InMemory(int ppn, int vpn){
        TranslationEntry translationEntry=InvertedPageTable.getInstance().getEntry(pid,vpn);

        if (translationEntry==null||translationEntry.valid){//reason
            return;
        }

//        InvertedPageTable.getInstance().addUseNums(translationEntry);
        boolean dirty,used;
        if (lazyLoadPages.containsKey(vpn)){
            CoffSectionCal coffSectionCal=lazyLoadPages.remove(vpn);
            if (coffSectionCal==null){
                return;
            }
            CoffSection coffSection= coff.getSection(coffSectionCal.getSectionNumber());
            coffSection.loadPage(coffSectionCal.getPageOffset(),ppn);
//            System.out.println("section编号为   "+coffSectionCal.getSectionNumber());
//            System.out.println("section偏移量"+coffSectionCal.getPageOffset());

            dirty=true;
            used=true;
        }else {
            byte[] memory=Machine.processor().getMemory();
            byte[] ans=Swapper.getInstance().readFromFile(vpn,pid);//外部页表的作用

            System.arraycopy(ans,0,memory,ppn*pageSize, pageSize);

            dirty=false;
            used=false;
        }
        TranslationEntry temp=new TranslationEntry(vpn,ppn,true,false,used,dirty);
//        InvertedPageTable.updateEntry(pid,temp);


//        InvertedPageTable.AddEntry(pid,temp);
        InvertedPageTable.getInstance().changeEntry(pid,temp);



    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
//	super.saveState();

        for (int i = 0; i <Machine.processor().getTLBSize() ; i++) {
            TLBStoreIn[i]=Machine.processor().readTLBEntry(i);
            if (TLBStoreIn[i].valid){
                InvertedPageTable.getInstance().updateEntry(pid,TLBStoreIn[i]);

            }
        }

    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
//	super.restoreState();

    for (int i=0;i<TLBStoreIn.length;i++){
        if (TLBStoreIn[i].valid){
//            Machine.processor().writeTLBEntry(i,TLBStoreIn[i]);
            //可能会被其他进程换出，所以需要进行判断
            TranslationEntry translationEntry=InvertedPageTable.getInstance().getEntry(pid,TLBStoreIn[i].vpn);
            if (translationEntry!=null&& translationEntry.valid){
                Machine.processor().writeTLBEntry(i,translationEntry);
            }else {
                Machine.processor().writeTLBEntry(i,new TranslationEntry(0,0,false,false,false,false));
            }
        }else {
            Machine.processor().writeTLBEntry(i,new TranslationEntry(0,0,false,false,false,false));
        }
    }
    }

    protected boolean distribution(int vpn, int needPages,boolean readOnly){
        for (int i = 0; i <needPages ; i++) {
            InvertedPageTable.getInstance().insertEntry(pid,new TranslationEntry(vpn+i,0,false,false,false,false));
            Swapper.getInstance().insertUnallocated(vpn+i,pid);
            allocatedPages.add(vpn+i);

        }
        numPages+=needPages;


        return true;
    }
    protected void releasePages(){
        VMKernel.mainMemoryLock.acquire();
        for (int vpn:allocatedPages){
            TranslationEntry translationEntry=InvertedPageTable.getInstance().getEntry(pid,vpn);
            if (translationEntry.valid){
                VMKernel.freePages.add(translationEntry.ppn);

            }


            InvertedPageTable.getInstance().removeEntry(pid,vpn);



        Swapper.getInstance().deletePosition(vpn,pid);
        }

        VMKernel.mainMemoryLock.release();
    }
    /**
     * Initializes page tables for this process so that the executable can be
     * demand-paged.
     *
     * @return	<tt>true</tt> if successful.
     */
    protected boolean loadSections() {
        for (int i = 0; i <coff.getNumSections() ; i++) {
            CoffSection coffSection= coff.getSection(i);
            for (int j = 0; j <coffSection.getLength() ; j++) {
                int vpn=coffSection.getFirstVPN()+j;

                CoffSectionCal coffSectionCal=new CoffSectionCal(i,j);
                lazyLoadPages.put(vpn,coffSectionCal);
            }
        }
	return true;
    }

//    protected boolean load(String name,String[] args){
//        return true;
//    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
	super.unloadSections();
    }
    public boolean handleTLBMiss(int address){
//boolean status=Machine.interrupt().disable();
        int vpn=Processor.pageFromAddress(address);
      TranslationEntry translationEntry=InvertedPageTable.getInstance().getEntry(pid,vpn);
      if (translationEntry==null){
          Lib.assertNotReached("eee");

          return false;
      }
//      InvertedPageTable.getInstance().addUseNums(translationEntry);
      if (!translationEntry.valid){
//          num++;

          Machine.stats.numPageFaults++;

          int ppn=getFreePage();


//         Lib.assertTrue( InvertedPageTable.getInstance().Assert(ppn));



          InMemory(ppn,vpn);
          translationEntry=InvertedPageTable.getInstance().getEntry(pid,vpn);
            Lib.assertTrue(translationEntry.valid);
      }
        int location=-1;
      for (int i=0;i<Machine.processor().getTLBSize();i++){
         TranslationEntry TLBTranslation=Machine.processor().readTLBEntry(i);
          if (!TLBTranslation.valid){

              location=i;
              break;


          }
      }
      if (location==-1){
         location=Lib.random(Machine.processor().getTLBSize());


      }
      TranslationEntry translationEntry1=Machine.processor().readTLBEntry(location);
        if (translationEntry1.valid){
            InvertedPageTable.getInstance().updateEntry(pid,translationEntry1);

        }

        Machine.processor().writeTLBEntry(location,translationEntry);
//        Machine.interrupt().restore(status);


        return true;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    // 2021年11月25日
    //TODO
    
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
        case Processor.exceptionTLBMiss:
            int address=processor.readRegister(Processor.regBadVAddr);
//            int vpn=Processor.pageFromAddress(address);
            boolean temp=handleTLBMiss(address);
//            if (!temp){
//                System.out.println("o");
//            }

            break;
	default:
	    super.handleException(cause);
	    break;
	}
    }
    protected TranslationEntry[] TLBStoreIn;
	protected LinkedList<Integer> allocatedPages;
    protected HashMap<Integer,CoffSectionCal> lazyLoadPages;//coff
//    public static int num=0;



    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
    private static final char dbgVM = 'v';


}
