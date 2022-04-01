package nachos.vm;


import nachos.machine.Machine;
import nachos.machine.OpenFile;
import nachos.machine.Processor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

//作为频繁访问文件的对象，可以使用单例模式
public class Swapper {
    private static final String fileName="MySwapFile";
    public  static  Swapper instance=new Swapper(fileName);

    private HashMap<VpnAndPid,Integer> swapMap;
    private OpenFile swapFile;
    private HashSet<VpnAndPid> unAllocated;
    private LinkedList<Integer> avaLocations;


private Swapper(String fileName){

    swapMap=new HashMap<>();
    swapFile=VMKernel.fileSystem.open(fileName,true);
    unAllocated=new HashSet<>();
    avaLocations=new LinkedList<>();
    byte[] temp=new byte[Processor.pageSize* Machine.processor().getNumPhysPages()];
    swapFile.write(temp,0, temp.length);


}






public static Swapper getInstance() {
    return instance;
}


public void deletePosition(int vpn, int pid){
    VpnAndPid vpnAndPid=new VpnAndPid(vpn,pid);
    if (!swapMap.containsKey(vpnAndPid)){
        return;
    }
    int location=swapMap.remove(vpnAndPid);
    avaLocations.add(location);


}

public byte[] readFromFile(int vpn,int pid){
 Integer location;
    location=swapMap.get(new VpnAndPid(vpn, pid));
    if (location==null){
        return new byte[Processor.pageSize];
    }
    byte[] ans=new byte[Processor.pageSize];
    if (swapFile.read(location*Processor.pageSize,ans,0,ans.length)==-1){
        return new byte[Processor.pageSize];
    }else {
        return ans;
    }

}
public void insertUnallocated(int vpn,int pid){
    VpnAndPid vpnAndPid=new VpnAndPid(vpn,pid);
    unAllocated.add(vpnAndPid);


}
public void writeToFile(int vpn, int pid, byte[] data, int offset){
    int location=-1;
    VpnAndPid vpnAndPid=new VpnAndPid(vpn, pid);
    if (unAllocated.contains(vpnAndPid)){
        unAllocated.remove(vpnAndPid);
        if (avaLocations.isEmpty()){
            avaLocations.add(swapMap.size());

        }
        location=avaLocations.removeFirst();

        swapMap.put(vpnAndPid,location);
    }else {
        location=swapMap.get(vpnAndPid);
    }
if (location==-1){
    return;
}
swapFile.write(location*Processor.pageSize,data,offset, Processor.pageSize);





}


}
