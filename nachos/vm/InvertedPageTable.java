package nachos.vm;


import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.TranslationEntry;

import java.util.*;

public class InvertedPageTable {
    private static final InvertedPageTable instance=new InvertedPageTable();

    public static Hashtable<VpnAndPid,TranslationEntry> InnerPageTable= new Hashtable<>();

    public static TranslationEntryDecorators[] PhysicalTable=new TranslationEntryDecorators[Machine.processor().getNumPhysPages()];

    public static boolean RM=true;





    static Comparator<VpnAndPid> comparator= new Comparator<VpnAndPid>() {
        @Override
        public int compare(VpnAndPid o1, VpnAndPid o2) {
            TranslationEntry translationEntry1 = InvertedPageTable.getInstance().getEntry(o1.getPid(), o1.getVpn());
            TranslationEntry translationEntry2 = InvertedPageTable.getInstance().getEntry(o2.getPid(), o2.getVpn());

            return RM?translationEntry1.useNums - translationEntry2.useNums:translationEntry2.useNums-translationEntry1.useNums;//反转可得 2-1为降序， 1-2为升序
        }
    };









    public static PriorityQueue<VpnAndPid> vpnAndPidPriorityQueue=new PriorityQueue<>(comparator);

//    public static LinkedList<TranslationEntryDecorators> entryDecoratorsLinkedList=new LinkedList<>();

    public static Stack<VpnAndPid> vpnAndPids=new Stack<>();


    private  InvertedPageTable(){

    }
    public static InvertedPageTable getInstance(){
        return instance;
    }

    public TranslationEntry getEntry(int pid,int vpn){

        VpnAndPid vpnAndPid=new VpnAndPid(vpn, pid);
//        TranslationEntry translationEntry=null;
//        if (InnerPageTable.containsKey(vpnAndPid)){
//
//            translationEntry=InnerPageTable.get(vpnAndPid);
//        }
//        return translationEntry;
        TranslationEntry  translationEntry=InnerPageTable.get(vpnAndPid);


        return translationEntry;


    }
    public boolean insertEntry(int pid,TranslationEntry translationEntry){
        VpnAndPid vpnAndPid=new VpnAndPid(translationEntry.vpn,pid);
        if (InnerPageTable.containsKey(vpnAndPid)){
            return false;
        }
        InnerPageTable.put(vpnAndPid,translationEntry);
        TranslationEntryDecorators translationEntryDecorators=new TranslationEntryDecorators(pid,translationEntry);

        if (translationEntry.valid){
            vpnAndPidPriorityQueue.add(vpnAndPid);
            PhysicalTable[translationEntry.ppn]=translationEntryDecorators;

            vpnAndPids.push(vpnAndPid);
        }
        return true;
    }
    public boolean Assert(int ppn){
        return PhysicalTable[ppn]==null;
    }
    public  void changeEntry(int pid, TranslationEntry translationEntry){

        VpnAndPid vpnAndPid=new VpnAndPid(translationEntry.vpn,pid);
        if (!InnerPageTable.containsKey(vpnAndPid)){
            return;
        }
        TranslationEntry old=InnerPageTable.get(vpnAndPid);
//        translationEntry.dirty= old.dirty||translationEntry.dirty;
//        translationEntry.used= old.used||translationEntry.used;

//        VMKernel.mainMemoryLock.acquire();
//
//        VMKernel.mainMemoryLock.release();
//        old.valid=false;
//     if(old.valid){
//         if (PhysicalTable[old.ppn]==null){
//             return;
//         }

//         PhysicalTable[old.ppn]=null;
//         old.valid=false;
//     }


     if (translationEntry.valid){
//         Lib.assertTrue(PhysicalTable[translationEntry.ppn]==null);



//         if (PhysicalTable[translationEntry.ppn]!=null){

//             return;
//         }
         TranslationEntryDecorators translationEntryDecorators=new TranslationEntryDecorators(pid, translationEntry);
        PhysicalTable[translationEntry.ppn]=translationEntryDecorators;
         vpnAndPidPriorityQueue.add(vpnAndPid);
        vpnAndPids.push(vpnAndPid);


     }
     InnerPageTable.put(vpnAndPid,translationEntry);

//     vpnAndPids.remove(vpnAndPid);
//     vpnAndPids.add(vpnAndPid);
//    translationEntry.useNums++;


    }
    public TranslationEntry removeEntry(int pid,int vpn){
        VpnAndPid vpnAndPid=new VpnAndPid(vpn, pid);
        TranslationEntry translationEntry1=InnerPageTable.get(new VpnAndPid(vpn, pid));
        if (translationEntry1!=null&&translationEntry1.valid){
//            PhysicalTable[translationEntry1.ppn]=null;
            translationEntry1.valid=false;
            vpnAndPids.remove(vpnAndPid);
            vpnAndPidPriorityQueue.remove(vpnAndPid);


        }
        return translationEntry1;

    }


    public void updateEntry(int pid, TranslationEntry translationEntry){
        VpnAndPid vpnAndPid=new VpnAndPid(translationEntry.vpn, pid);
        TranslationEntry oldEntry=InnerPageTable.get(vpnAndPid);



        if (oldEntry==null){
            return;
        }
        if(translationEntry.valid&& oldEntry.valid){
            translationEntry.used= translationEntry.used|| oldEntry.used;
            translationEntry.dirty=translationEntry.dirty|| oldEntry.dirty;

//            vpnAndPids.remove(vpnAndPid);
//            vpnAndPids.add(0,vpnAndPid);
//            vpnAndPidPriorityQueue.add(vpnAndPid);

        }

        translationEntry.valid=translationEntry.valid && oldEntry.valid;

        InnerPageTable.put(vpnAndPid,translationEntry);
//        translationEntry.useNums++;
//        entryDecoratorsLinkedList.remove(new TranslationEntryDecorators(vpnAndPid.getPid(),translationEntry));
//        entryDecoratorsLinkedList.add(new TranslationEntryDecorators(vpnAndPid.getPid(),translationEntry));


    }
//    private  void updateEntry2(int pid, TranslationEntry newEntry, VpnAndPid vpnAndPid, TranslationEntry oldEntry) {
//        if (oldEntry.valid) {
//            if (PhysicalTable[oldEntry.ppn] == null) {
//
//                return;
//            }
//            PhysicalTable[oldEntry.ppn] = null;
//
//        }
//        if (newEntry.valid) {
//            if (PhysicalTable[newEntry.ppn] != null) {
//
//                return;
//            }
//            PhysicalTable[newEntry.ppn] = new TranslationEntryDecorators(pid,newEntry);
//        }
//        InnerPageTable.put(vpnAndPid, newEntry);
//    }
//    public static void AddEntry(int pid,TranslationEntry translationEntry){
//        VpnAndPid vpnAndPid=new VpnAndPid(translationEntry.vpn,pid);
//        if (InnerPageTable.containsKey(vpnAndPid)){
//            return;
//        }
//        updateEntry2(pid,translationEntry,vpnAndPid,translationEntry);
//
//    }
    public TranslationEntryDecorators getNextEntry(){//可以换出的页表的项，有效才能换出
        TranslationEntryDecorators translationEntryDecorators=null;
        while (translationEntryDecorators==null||!translationEntryDecorators.translationEntry.valid){
            int index= Lib.random(PhysicalTable.length);
            translationEntryDecorators=PhysicalTable[index];
        }
        return translationEntryDecorators;
    }

    public TranslationEntryDecorators getNextEntry2(){
//        TranslationEntryDecorators translationEntryDecorators=null;
        VpnAndPid vpnAndPid=vpnAndPids.remove(0);



        return PhysicalTable[getEntry(vpnAndPid.getPid(),vpnAndPid.getVpn()).ppn];


    }
    public TranslationEntryDecorators getNextEntry3(){
        VpnAndPid vpnAndPid=vpnAndPidPriorityQueue.poll();




        TranslationEntry translationEntry=getEntry(vpnAndPid.getPid(), vpnAndPid.getVpn());

//        System.out.println(translationEntry.useNums);


        return PhysicalTable[translationEntry.ppn];

    }
    public void addUseNums(TranslationEntry translationEntry){

        translationEntry.useNums++;
    }

   public void insertTran(int pid,int vpn){
        VpnAndPid vpnAndPid=new VpnAndPid(vpn,pid);

        vpnAndPids.remove(vpnAndPid);
        vpnAndPids.push(vpnAndPid);
   }
}



