package nachos.userprog;

import nachos.machine.*;
import nachos.threads.*;
import nachos.userprog.*;

import java.io.EOFException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * Encapsulates the state of a user process that is not contained in its
 * user thread (or threads). This includes its address translation state, a
 * file table, and information about the program being executed.
 *
 * <p>
 * This class is extended by other classes to support additional functionality
 * (such as additional syscalls).
 *
 * @see	nachos.vm.VMProcess
 * @see	nachos.network.NetProcess
 */
public class UserProcess {
    /**
     * Allocate a new process.
     */
    public UserProcess() {
	int numPhysPages = Machine.processor().getNumPhysPages();
//	pageTable = new TranslationEntry[numPhysPages];
	openFiles[0]=UserKernel.console.openForReading();
	openFiles[1]=UserKernel.console.openForWriting();
	pageTable=new TranslationEntry[numPhysPages];//这里对页表进行初始化
	for (int i=0;i<numPhysPages;i++){
		pageTable[i]=new TranslationEntry(i,0,false,false,false,false);
	}

//我认为，只要虚拟内存不分配物理地址，可以任意大


	this.pid=nextPid;
	nextPid++;
	runningProcess++;
//	for (int i=0; i<numPhysPages; i++)
//	    pageTable[i] = new TranslationEntry(i,i, true,false,false,false);
    }

    /**
     * Allocate and return a new process of the correct class. The class name
     * is specified by the <tt>nachos.conf</tt> key
     * <tt>Kernel.processClassName</tt>.
     *
     * @return	a new process of the correct class.
     */
    public static UserProcess newUserProcess() {
	return (UserProcess)Lib.constructObject(Machine.getProcessClassName());
    }

    /**
     * Execute the specified program with the specified arguments. Attempts to
     * load the program, and then forks a thread to run it.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the program was successfully executed.
     */
    public boolean execute(String name, String[] args) {
	if (!load(name, args))
	    return false;
	
	new UThread(this).setName(name).fork();

	return true;
    }

    /**
     * Save the state of this process in preparation for a context switch.
     * Called by <tt>UThread.saveState()</tt>.
     */
    public void saveState() {
    }

    /**
     * Restore the state of this process after a context switch. Called by
     * <tt>UThread.restoreState()</tt>.
     */
    public void restoreState() {
	Machine.processor().setPageTable(pageTable);
    }

    /**
     * Read a null-terminated string from this process's virtual memory. Read
     * at most <tt>maxLength + 1</tt> bytes from the specified address, search
     * for the null terminator, and convert it to a <tt>java.lang.String</tt>,
     * without including the null terminator. If no null terminator is found,
     * returns <tt>null</tt>.
     *
     * @param	vaddr	the starting virtual address of the null-terminated
     *			string.
     * @param	maxLength	the maximum number of characters in the string,
     *				not including the null terminator.
     * @return	the string read, or <tt>null</tt> if no null terminator was
     *		found.
     */
    public String readVirtualMemoryString(int vaddr, int maxLength) {
	Lib.assertTrue(maxLength >= 0);

	byte[] bytes = new byte[maxLength+1];

	int bytesRead = readVirtualMemory(vaddr, bytes);

	for (int length=0; length<bytesRead; length++) {
	    if (bytes[length] == 0)
		return new String(bytes, 0, length);
	}

	return null;
    }

    /**
     * Transfer data from this process's virtual memory to all of the specified
     * array. Same as <tt>readVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data) {
	return readVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from this process's virtual memory to the specified array.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to read.
     * @param	data	the array where the data will be stored.
     * @param	offset	the first byte to write in the array.
     * @param	length	the number of bytes to transfer from virtual memory to
     *			the array.
     * @return	the number of bytes successfully transferred.
     */
    public int readVirtualMemory(int vaddr, byte[] data, int offset,
				 int length) {
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	
	// for now, just assume that virtual addresses equal physical addresses
//	if (vaddr < 0 || vaddr >= memory.length)
//	    return 0;
//	int pageNum=vaddr/pageSize;处理器中已经有方法进行计算
//	int frameNum=vaddr%pageSize;
//		for (int i = 0; i <pageTable.length ; i++) {
//			if (pageTable[i].vpn==pageNum){
//
//			}
//		}
		if (vaddr<0||vaddr+length-1>Processor.makeAddress(numPages-1,pageSize-1)){
			return 0;
		}


	length=Math.min(length,data.length-offset);
	length=Math.min(length,numPages*pageSize-vaddr);
	int ans=0;
	while (ans<length){

		int pageNum=Processor.pageFromAddress(vaddr+ans);
		int pageOffset=Processor.offsetFromAddress(vaddr+ans);//这里的偏移量含义为在页表中的偏移量

		if (!findPage(pageNum).valid){
//			return -1;
			break;



		}
		int amount=Math.min(length-ans,pageSize-pageOffset);
		int trddr=findPage(pageNum).ppn*pageSize+pageOffset;

		System.arraycopy(memory,trddr,data,offset+ans,amount);
		ans+=amount;
	}
//	System.out.println(Arrays.toString(data));





//	int amount = Math.min(length, memory.length-vaddr);
//	System.arraycopy(memory, vaddr, data, offset, amount);

	return  ans;

    }

	public TranslationEntry findPage(int i){
		return pageTable[i];
	}

    /**
     * Transfer all data from the specified array to this process's virtual
     * memory.
     * Same as <tt>writeVirtualMemory(vaddr, data, 0, data.length)</tt>.
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data) {
	return writeVirtualMemory(vaddr, data, 0, data.length);
    }

    /**
     * Transfer data from the specified array to this process's virtual memory.
     * This method handles address translation details. This method must
     * <i>not</i> destroy the current process if an error occurs, but instead
     * should return the number of bytes successfully copied (or zero if no
     * data could be copied).
     *
     * @param	vaddr	the first byte of virtual memory to write.
     * @param	data	the array containing the data to transfer.
     * @param	offset	the first byte to transfer from the array.
     * @param	length	the number of bytes to transfer from the array to
     *			virtual memory.
     * @return	the number of bytes successfully transferred.
     */
    public int writeVirtualMemory(int vaddr, byte[] data, int offset,
				  int length) {//页表
	Lib.assertTrue(offset >= 0 && length >= 0 && offset+length <= data.length);

	byte[] memory = Machine.processor().getMemory();
	int ans=0;
//	// for now, just assume that virtual addresses equal physical addresses
//	if (vaddr < 0 || vaddr >= memory.length)
//	    return 0;
//
//	int amount = Math.min(length, memory.length-vaddr);
//	System.arraycopy(data, offset, memory, vaddr, amount);
	length=Math.min(length, data.length-offset);
	length=Math.min(length,numPages*pageSize-vaddr);
	while (ans<length){
		int pageNum=Processor.pageFromAddress(vaddr+ans);//虚拟地址应当连续

		if (pageNum<0){


//			return -1;
			break;


		}else {
			TranslationEntry translationEntry=findPage(pageNum);
			if (!translationEntry.valid||translationEntry.readOnly){
				break;
			}
		}
		int pageOffset=Processor.offsetFromAddress(vaddr+ans);
		int trddr=findPage(pageNum).ppn*pageSize+pageOffset;

//		pageTable[pageNum].dirty=true;
		int amount=Math.min(length-ans,pageSize-pageOffset);
		System.arraycopy(data,offset+ans,memory,trddr,amount);
		ans+=amount;

//		pageTable[pageNum].valid=true;页表条目的valid表示是否分配物理页
	}
	return ans;

    }

    /**
     * Load the executable with the specified name into this process, and
     * prepare to pass it the specified arguments. Opens the executable, reads
     * its header information, and copies sections and arguments into this
     * process's virtual memory.
     *
     * @param	name	the name of the file containing the executable.
     * @param	args	the arguments to pass to the executable.
     * @return	<tt>true</tt> if the executable was successfully loaded.
     */
    private boolean load(String name, String[] args) {
	Lib.debug(dbgProcess, "UserProcess.load(\"" + name + "\")");
	
	OpenFile executable = ThreadedKernel.fileSystem.open(name, false);
	if (executable == null) {
	    Lib.debug(dbgProcess, "\topen failed");
	    return false;
	}

	try {
	    coff = new Coff(executable);
	}

	catch (EOFException e) {
	    executable.close();
	    Lib.debug(dbgProcess, "\tcoff load failed");
	    return false;
	}

	// make sure the sections are contiguous and start at page 0
	numPages = 0;
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    if (section.getFirstVPN() != numPages) {
		coff.close();
		Lib.debug(dbgProcess, "\tfragmented executable");
		return false;
	    }
		if (!distribution(section.getFirstVPN(),section.getLength(),section.isReadOnly())){
			releasePages();
			return false;
		}
//	    numPages += section.getLength();
	}

	// make sure the argv array will fit in one page
	byte[][] argv = new byte[args.length][];
	int argsSize = 0;
	for (int i=0; i<args.length; i++) {
	    argv[i] = args[i].getBytes();
	    // 4 bytes for argv[] pointer; then string plus one for null byte
	    argsSize += 4 + argv[i].length + 1;
	}
	if (argsSize > pageSize) {
	    coff.close();
	    Lib.debug(dbgProcess, "\targuments too long");
	    return false;
	}

	// program counter initially points at the program entry point
	initialPC = coff.getEntryPoint();	

	// next comes the stack; stack pointer initially points to top of it
//	numPages += stackPages;
		if (!distribution(numPages,stackPages,false)){
			releasePages();
			return false;
		}
	initialSP = numPages*pageSize;

	// and finally reserve 1 page for arguments
//	numPages++;
		if (!distribution(numPages,1,false)){
			releasePages();
			return false;

		}

	if (!loadSections())
	    return false;

	// store arguments in last page
		//加载参数并对参数地址进行储存
	int entryOffset = (numPages-1)*pageSize;
	int stringOffset = entryOffset + args.length*4;

	this.argc = args.length;
	this.argv = entryOffset;
	
	for (int i=0; i<argv.length; i++) {
	    byte[] stringOffsetBytes = Lib.bytesFromInt(stringOffset);
	    Lib.assertTrue(writeVirtualMemory(entryOffset,stringOffsetBytes) == 4);
	    entryOffset += 4;
	    Lib.assertTrue(writeVirtualMemory(stringOffset, argv[i]) ==
		       argv[i].length);
	    stringOffset += argv[i].length;
	    Lib.assertTrue(writeVirtualMemory(stringOffset,new byte[] { 0 }) == 1);
	    stringOffset += 1;
	}

	return true;
    }

    /**
     * Allocates memory for this process, and loads the COFF sections into
     * memory. If this returns successfully, the process will definitely be
     * run (this is the last step in process initialization that can fail).
     *
     * @return	<tt>true</tt> if the sections were successfully loaded.
     */
    protected boolean loadSections() {


	if (numPages >UserKernel.freePages.size()) {

	    coff.close();
	    Lib.debug(dbgProcess, "\tinsufficient physical memory");
	    return false;
	}
//		UserKernel.mainMemoryLock.acquire();
//	pageTable=new TranslationEntry[numPages];
//		for (int i = 0; i <numPages ; i++) {
//			Integer nextFreePage=UserKernel.freePages.remove();
//
//			pageTable[i]=new TranslationEntry(i,nextFreePage,true,false,false,false);
//		}
//		UserKernel.mainMemoryLock.release();
	// load sections
	for (int s=0; s<coff.getNumSections(); s++) {
	    CoffSection section = coff.getSection(s);
	    
	    Lib.debug(dbgProcess, "\tinitializing " + section.getName()
		      + " section (" + section.getLength() + " pages)");

	    for (int i=0; i<section.getLength(); i++) {
		int vpn = section.getFirstVPN()+i;
		if (pageTable[vpn]==null){
			return false;
		}

		pageTable[vpn].readOnly=section.isReadOnly();
		// for now, just assume virtual addresses=physical addresses
		section.loadPage(i, pageTable[vpn].ppn);
	    }
	}
	
	return true;
    }

    /**
     * Release any resources allocated by <tt>loadSections()</tt>.
     */
    protected void unloadSections() {
		coff.close();
		for (int i = 0; i <openFilesLength ; i++) {
			if (openFiles[i]!=null){
				handleClose(i);

			}
		}
		releasePages();
    }
	protected boolean distribution(int vpn,int needPages,boolean readOnly){
		LinkedList<TranslationEntry> distributed=new LinkedList<>();

		if (vpn>pageTable.length){
			return false;
		}
		for (int i=0;i<needPages;i++){

			int location=-1;
			UserKernel.mainMemoryLock.acquire();
			if (!UserKernel.freePages.isEmpty()){
				location=UserKernel.freePages.remove();
			}
			UserKernel.mainMemoryLock.release();
			if (location==-1){
				for (TranslationEntry translationEntry:distributed){
					pageTable[translationEntry.vpn]=new TranslationEntry(translationEntry.vpn,0,false,false,false,false);
					UserKernel.mainMemoryLock.acquire();
					UserKernel.freePages.add(translationEntry.ppn);
					numPages--;
					UserKernel.mainMemoryLock.release();
				}
			}else {
			TranslationEntry translationEntry=new TranslationEntry(vpn+i,location,true,readOnly,false,false);
			distributed.add(translationEntry);
			pageTable[vpn+i]=translationEntry;
			numPages++;
			}
		}
		return true;
	}
	protected void releasePages(){
		UserKernel.mainMemoryLock.acquire();

		for (int i = 0; i < numPages; i++) {
			UserKernel.freePages.add(pageTable[i].ppn);
			pageTable[i]=new TranslationEntry(pageTable[i].vpn,0,false,false,false,false);
		}
		UserKernel.mainMemoryLock.release();
	}

    /**
     * Initialize the processor's registers in preparation for running the
     * program loaded into this process. Set the PC register to point at the
     * start function, set the stack pointer register to point at the top of
     * the stack, set the A0 and A1 registers to argc and argv, respectively,
     * and initialize all other registers to 0.
     */
    public void initRegisters() {
	Processor processor = Machine.processor();

	// by default, everything's 0
	for (int i=0; i<processor.numUserRegisters; i++)
	    processor.writeRegister(i, 0);

	// initialize PC and SP according
	processor.writeRegister(Processor.regPC, initialPC);
	processor.writeRegister(Processor.regSP, initialSP);

	// initialize the first two argument registers to argc and argv
	processor.writeRegister(Processor.regA0, argc);
	processor.writeRegister(Processor.regA1, argv);
    }

    /**
     * Handle the halt() system call. 
     */
    private int handleHalt() {

	if(pid==0){
		Kernel.kernel.terminate();
		Machine.halt();
	}
	
	Lib.assertNotReached("Machine.halt() did not halt machine!");
	return 0;
    }
	private int handleExit(int status){
//		System.out.println("handleExit");

		this.status=status;
		exited=true;
//		for (UserProcess userProcess:children.values()){
//			userProcess.parent=null;
//		}


		if (parent!=null){
			joinLock.acquire();
			joinCondition.wake();
			joinLock.release();
			parent.children.remove(this.pid);

		}
		unloadSections();
		if (runningProcess==1){

			Kernel.kernel.terminate();

		}
		runningProcess--;
		KThread.finish();
		return 0;
	}
	private int handleExec(int fileNum,int argc,int argvPtr){
//		System.out.println("handleExec");
		String fileName=readVirtualMemoryString(fileNum,256);
		if (fileName==null||argc<0||argvPtr>numPages*pageSize||argvPtr<0){
			return -1;
		}

		String[] args=new String[argc];
		for (int i = 0; i < argc; i++) {
			byte[] EntryOffset=new byte[4];
			int readNum=readVirtualMemory(argvPtr+i*4,EntryOffset);
			if (readNum!=4){
				return -1;
			}
			args[i]=readVirtualMemoryString(Lib.bytesToInt(EntryOffset,0),256);
		}
		UserProcess userProcess=UserProcess.newUserProcess();
		if (!userProcess.execute(fileName,args)){
			return -1;
		}

		userProcess.parent=this;
		this.children.put(userProcess.pid,userProcess);
		return userProcess.pid;




	}




	private int handleJoin(int pid,int statusAddress){
		if (statusAddress<0||statusAddress>=numPages*pageSize){
			return -1;
		}

		UserProcess userProcess=children.get(pid);
		if (userProcess==null){
			return -1;
		}
		userProcess.joinLock.acquire();
		userProcess.joinCondition.sleep();
		userProcess.joinLock.release();


		byte[] BStatus=Lib.bytesFromInt(userProcess.status);
		writeVirtualMemory(statusAddress,BStatus);
		if (!userProcess.exited){
			return 0;
		}

		return 1;
	}
	private int handleCreate(int fileNum){
//		System.out.println("handleCreate");
		String fileName=readVirtualMemoryString(fileNum,256);
		if (fileName==null){
			return -1;
		}
		int descriptor=-1;

		for (int i=0;i<openFilesLength;i++){
			if (openFiles[i]==null){
				descriptor=i;
				break;
			}
		}
		if (descriptor==-1){
			return -1;
		}else {
			openFiles[descriptor]=ThreadedKernel.fileSystem.open(fileName,true);
			return  descriptor;
		}
	}
	private int handleSleep(int time){
		new Alarm().waitUntil(time);
		return 0;
	}
	private int handleFork(){
		UserProcess userProcess=UserProcess.newUserProcess();
//		userProcess.pageTable=this.pageTable;
//		new UThread(userProcess)
//		if (numPages>UserKernel.freePages.size()){
//			return -1;
//		}
		int forkPages=0;
		userProcess.pageTable=new TranslationEntry[numPages];
		for (int i = 0; i < coff.getNumSections(); i++) {
			CoffSection coffSection=coff.getSection(i);
			forkPages+=coffSection.getLength();
			for (int j = 0; j <coffSection.getLength() ; j++) {
				int vpn=coffSection.getFirstVPN()+j;

				userProcess.pageTable[vpn]=pageTable[vpn];
			}
		}
		UserKernel.mainMemoryLock.acquire();
//		for (int i = 0; i <stackPages ; i++) {
//
//		}
		for (int i = forkPages; i < forkPages+stackPages; i++) {
			int freePage=UserKernel.freePages.remove();
			userProcess.pageTable[i]=new TranslationEntry(i,freePage,true,false,false,false);
			byte[] temp=new byte[pageSize];
			readVirtualMemory(i,temp);
			userProcess.writeVirtualMemory(i,temp);
		}
		forkPages=forkPages+stackPages;
//		forkPages++;
		userProcess.pageTable[forkPages]=pageTable[forkPages];
		userProcess.numPages=forkPages;
//		System.out.println(userProcess.coff);
		UserKernel.mainMemoryLock.release();
//		if (numPages-coff)
		new UThread(userProcess).setName("fork-"+pid).fork();
		return userProcess.pid;
	}
	private int handleOpen(int fileNum){
		String fileName=readVirtualMemoryString(fileNum,256);

		if (fileName==null){
			return -1;
		}
		int descriptor=-1;

		for (int i = 0; i <openFilesLength ; i++) {
			if (openFiles[i]==null){
				descriptor=i;
				break;


			}
		}
		if (descriptor==-1){
			return -1;
		}else {
			openFiles[descriptor]=ThreadedKernel.fileSystem.open(fileName,false);
			return descriptor;

		}

	}
	private int handleRead(int descriptor,int buffer,int length){
		if (descriptor>=openFilesLength||descriptor<0||openFiles[descriptor]==null){
			return -1;

		}
		byte[] bytes=new byte[length];
		int readNum=openFiles[descriptor].read(bytes,0,length);

		if (readNum==-1){
			return -1;
		}

		int writeLocation=writeVirtualMemory(buffer,bytes,0,readNum);
		if (writeLocation!=readNum){
			return -1;
		}

		return writeLocation;
	}
	private int handleWrite(int descriptor,int buffer,int length){
		if (descriptor>=openFilesLength||descriptor<0||openFiles[descriptor]==null){
			return -1;
		}

		byte[] bytes=new byte[length];
		int readNum=readVirtualMemory(buffer,bytes);
//		if (readNum<=0){
//
//			return 0;//通过翻译英文注释可以得到这里应当返回0
//		}
		int writeNum=openFiles[descriptor].write(bytes,0,readNum);






		return writeNum;




	}


	private int handleClose(int descriptor){
//		System.out.println("handleClose");
		if (descriptor>=openFilesLength||descriptor<0||openFiles[descriptor]==null){
			return -1;
		}

		openFiles[descriptor].close();
		openFiles[descriptor]=null;

		return 0;
	}












	private int handleUnlink(int fileNum){
		String fileName=readVirtualMemoryString(fileNum,256);
		if (fileName==null){
			return -1;
		}
		for (int i = 0; i <openFilesLength ; i++) {
			if (openFiles[i]!=null){
				if (openFiles[i].getName().equals(fileName)){
					return -1;
				}
			}
		}
		if (ThreadedKernel.fileSystem.remove(fileName)){
			return 0;
		}else {
			return -1;
		}

	}
    private static final int
        syscallHalt = 0,
	syscallExit = 1,
	syscallExec = 2,
	syscallJoin = 3,
	syscallCreate = 4,
	syscallOpen = 5,
	syscallRead = 6,
	syscallWrite = 7,
	syscallClose = 8,
	syscallUnlink = 9,
	syscallSleep = 13,
	syscallFork = 14;

    /**
     * Handle a syscall exception. Called by <tt>handleException()</tt>. The
     * <i>syscall</i> argument identifies which syscall the user executed:
     *
     * <table>
     * <tr><td>syscall#</td><td>syscall prototype</td></tr>
     * <tr><td>0</td><td><tt>void halt();</tt></td></tr>
     * <tr><td>1</td><td><tt>void exit(int status);</tt></td></tr>
     * <tr><td>2</td><td><tt>int  exec(char *name, int argc, char **argv);
     * 								</tt></td></tr>
     * <tr><td>3</td><td><tt>int  join(int pid, int *status);</tt></td></tr>
     * <tr><td>4</td><td><tt>int  creat(char *name);</tt></td></tr>
     * <tr><td>5</td><td><tt>int  open(char *name);</tt></td></tr>
     * <tr><td>6</td><td><tt>int  read(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>7</td><td><tt>int  write(int fd, char *buffer, int size);
     *								</tt></td></tr>
     * <tr><td>8</td><td><tt>int  close(int fd);</tt></td></tr>
     * <tr><td>9</td><td><tt>int  unlink(char *name);</tt></td></tr>
     * </table>
     * 
     * @param	syscall	the syscall number.
     * @param	a0	the first syscall argument.
     * @param	a1	the second syscall argument.
     * @param	a2	the third syscall argument.
     * @param	a3	the fourth syscall argument.
     * @return	the value to be returned to the user.
     */
    public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
	switch (syscall) {
	case syscallHalt:
	    return handleHalt();
		case syscallExit:
			return handleExit(a0);
		case syscallExec:
			return handleExec(a0,a1,a2);
		case syscallJoin:
			return handleJoin(a0,a1);
		case syscallCreate:
			return handleCreate(a0);
		case syscallOpen:
			return handleOpen(a0);
		case syscallRead:
			return handleRead(a0,a1,a2);
		case syscallWrite:
			return handleWrite(a0,a1,a2);
		case syscallClose:
			return handleClose(a0);
		case syscallUnlink:
			return handleUnlink(a0);
		case syscallSleep:
			return handleSleep(a0);
		case syscallFork:
			return handleFork();
	default:
	    Lib.debug(dbgProcess, "Unknown syscall " + syscall);
	    Lib.assertNotReached("Unknown system call!");
	}
	return 0;
    }

    /**
     * Handle a user exception. Called by
     * <tt>UserKernel.exceptionHandler()</tt>. The
     * <i>cause</i> argument identifies which exception occurred; see the
     * <tt>Processor.exceptionZZZ</tt> constants.
     *
     * @param	cause	the user exception that occurred.
     */
    public void handleException(int cause) {
	Processor processor = Machine.processor();

	switch (cause) {
	case Processor.exceptionSyscall:
	    int result = handleSyscall(processor.readRegister(Processor.regV0),
				       processor.readRegister(Processor.regA0),
				       processor.readRegister(Processor.regA1),
				       processor.readRegister(Processor.regA2),
				       processor.readRegister(Processor.regA3)
				       );
	    processor.writeRegister(Processor.regV0, result);
	    processor.advancePC();
	    break;

	default:
	    Lib.debug(dbgProcess, "Unexpected exception: " +
		      Processor.exceptionNames[cause]);
	    Lib.assertNotReached("Unexpected exception");

	}
    }

    /** The program being run by this process. */
    protected Coff coff;

    /** This process's page table. */
    protected TranslationEntry[] pageTable;
    /** The number of contiguous pages occupied by the program. */
    protected int numPages;
	protected int openFilesLength=16;
	protected OpenFile[] openFiles=new OpenFile[openFilesLength];
    /** The number of pages in the program's stack. */
    protected final int stackPages = 8;
	public static int nextPid=0;
	public int pid;

	public UserProcess parent;
	public Map<Integer,UserProcess> children=new HashMap<>();
    private int initialPC, initialSP;
    private int argc, argv;

	private final Lock joinLock=new Lock();
	private final Condition joinCondition=new Condition(joinLock);


	private static int runningProcess=0;
	private int status=0;
	private boolean exited;
    private static final int pageSize = Processor.pageSize;
    private static final char dbgProcess = 'a';
}
