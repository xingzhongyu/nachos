package nachos.network;

import nachos.machine.*;
import nachos.vm.*;

/**
 * A <tt>VMProcess</tt> that supports networking syscalls.
 */
public class NetProcess extends VMProcess {
	/**
	 * Allocate a new process.
	 */
	public NetProcess() {
		super();
	}

	private static final int syscallConnect = 11, syscallAccept = 12;

	/**
	 * Handle a syscall exception. Called by <tt>handleException()</tt>. The
	 * <i>syscall</i> argument identifies which syscall the user executed:
	 * 
	 * <table>
	 * <tr>
	 * <td>syscall#</td>
	 * <td>syscall prototype</td>
	 * </tr>
	 * <tr>
	 * <td>11</td>
	 * <td><tt>int  connect(int host, int port);</tt></td>
	 * </tr>
	 * <tr>
	 * <td>12</td>
	 * <td><tt>int  accept(int port);</tt></td>
	 * </tr>
	 * </table>
	 * 
	 * @param syscall
	 *            the syscall number.
	 * @param a0
	 *            the first syscall argument.
	 * @param a1
	 *            the second syscall argument.
	 * @param a2
	 *            the third syscall argument.
	 * @param a3
	 *            the fourth syscall argument.
	 * @return the value to be returned to the user.
	 */
	public int handleSyscall(int syscall, int a0, int a1, int a2, int a3) {
		switch (syscall) {
		case syscallAccept:
			return handleAccept(a0);
		case syscallConnect:
			return handleConnect(a0,a1);
		default:
			return super.handleSyscall(syscall, a0, a1, a2, a3);
		}
	}

	public int findEmpty(){
		int location=-1;
		for (int i = 0; i < openFilesLength; i++) {
			if (openFiles[i]==null){
				location=i;
				break;
			}
		}
		return location;
	}


	private int handleConnect(int host, int port) {
		Lib.assertTrue(port >= 0 && port < Packet.linkAddressLimit);
		int fileDesc = findEmpty();
		if (fileDesc != -1) {
//			try {
				openFiles[fileDesc] = new OpenSocket(NetKernel.postOffice.connect(host,port),this);
//
//			} catch (ClassCastException cce) {
//				Lib.assertNotReached("Error - kernel not of type NetKernel");
//			}
		}

		return fileDesc;
	}


	private int handleAccept(int port) {
		Lib.assertTrue(port >= 0 && port < Packet.linkAddressLimit);
		int fileDesc = findEmpty();
		if (fileDesc != -1) {
			Connection c = null;
			try {



				c = NetKernel.postOffice.accept(port);
			} catch (ClassCastException cce) {
				Lib.assertNotReached("Error - kernel not of type NetKernel");
			}

			if (c != null) {
				openFiles[fileDesc] = new OpenSocket(c,this);
//				FileRef.referenceFile(fileTable[fileDesc].getName());
				return fileDesc;
			}
		}

		return -1;
	}


	private static class OpenSocket extends OpenFile {

		OpenSocket(Connection c,NetProcess netProcess) {
			super(null, c.srcPort + "," + c.destAddress + "," + c.destPort);
			connection = c;
			this.netProcess=netProcess;

		}


		@Override
		public void close() {
			connection.close();
			connection = null;
		}

		@Override
		public int read(byte[] buf, int offset, int length) {

			Lib.assertTrue(offset < buf.length && length <= buf.length - offset);
			if (connection == null)
				return -1;
			else {
				byte[] receivedData = connection.receive(length);
				if (receivedData == null)
					return -1;
				else {
					System.arraycopy(receivedData, 0, buf, offset, receivedData.length);

//					netProcess.writeVirtualMemory(buf)




					return receivedData.length;
				}
			}
		}



		@Override
		public int write(byte[] buf, int offset, int length) {
			if (connection == null)
				return -1;
			else
				return connection.send(buf, offset, length);
		}


















		private Connection connection;
		private NetProcess netProcess;
	}
}
