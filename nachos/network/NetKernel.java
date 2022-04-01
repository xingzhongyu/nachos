package nachos.network;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import nachos.machine.*;
import nachos.threads.*;
import nachos.vm.*;

/**
 * A kernel with network support.
 */
public class NetKernel extends VMKernel {
	/**
	 * Allocate a new networking kernel.
	 */
	public NetKernel() {
		super();
	}







	/**
	 * 初始化内核
	 */
	public void initialize(String[] args) {
		super.initialize(args);
		postOffice = new SocketPostOffice();
	}

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
		postOffice.shutdown();
		super.terminate();
	}

	public static SocketPostOffice postOffice;

	/**
	 * 一个用线程来封装 NachOS 传输协议的管理的类来进行交付等。
	 */
	static class SocketPostOffice {
		SocketPostOffice() {
			nothingToSend = new Condition(sendLock);
			terminationCondition = new Condition(terminationLock);

			//设置传递线程中断处理程序

			Machine.networkLink().setInterruptHandlers(new Runnable() {
				public void run() {
					receiveInterrupt();
				}
			},
			new Runnable() {
				public void run() {
					sendInterrupt();
				}
			});


			KThread postalDeliveryThread = new KThread(
					new Runnable() {
						public void run() {
							postalDelivery();
						}
					}
			), postalSendThread = new KThread(
					new Runnable() {
						public void run() {
							send();
						}
					}
			), timerInterruptThread = new KThread(
					new Runnable() {
						public void run() {
							timerRoutine();
						}
					}
			);

			postalDeliveryThread.fork();
			postalSendThread.fork();
			timerInterruptThread.fork();
		}

		Connection accept(int port) {
			Connection c = awaitingConnectionMap.retrieve(port);

			if (c != null)
				c.accept();

			return c;
		}

		Connection connect(int host, int port) {
			Connection connection = null;
			boolean found = false;
			int srcPort, tries = 0;
			while (connection == null) {

				//查找连接的源端口
				srcPort = portGenerator.nextInt(MailMessage.portLimit);
				tries = 0;
				
				while (!(found = (connectionMap.get(srcPort, host, port) == null)) && tries++ < MailMessage.portLimit)
					srcPort = (srcPort+1) % MailMessage.portLimit;
				
				if (found) {
					connection = new Connection(host, port, srcPort);
					connectionMap.put(connection);
					if (!connection.connect()) {
						connectionMap.remove(connection);
						connection = null;
					}
				}//除非端口饱和，否则随机化并重试
			}

			return connection;
		}

		private Random portGenerator = new Random();

		/**
		 *关闭连接从 connectionMap 中删除
		 *
		 * 这应该只在关闭“实时”连接（即从连接成功返回的连接）时从内核调用。
		 */
		void close(Connection connection) {
			if (connectionMap.remove(connection.srcPort, connection.destAddress, connection.destPort) != null)
				connection.close();
		}



		void shutdown() {
			connectionMap.shutdown();
			awaitingConnectionMap.shutdown();

			terminationLock.acquire();

			while (!connectionMap.isEmpty())
				terminationCondition.sleep();

			terminationLock.release();

		}

		private Lock terminationLock = new Lock();
		private Condition terminationCondition;



		void finished(Connection c) {
			if (connectionMap.remove(c.srcPort, c.destAddress, c.destPort) != null) {
				terminationLock.acquire();
				terminationCondition.wake();
				terminationLock.release();
			}
		}

		void enqueue(Packet p) {
			sendLock.acquire();
			sendQueue.add(p);
			nothingToSend.wake();
			sendLock.release();
		}


		void enqueue(List<Packet> ps) {
			sendLock.acquire();
			sendQueue.addAll(ps);
			nothingToSend.wake();
			sendLock.release();
		}

		/**
		 * 将数据包传送到适当套接字的方法。
		 */
		private void postalDelivery() {
			MailMessage pktMsg = null;
			Connection connection = null;
			while (true) {
				messageReceived.P();

				try {
					pktMsg = new MailMessage(Machine.networkLink().receive());
				} catch (MalformedPacketException e) {
					continue;

				}


				if ((connection = connectionMap.get(pktMsg.dstPort, pktMsg.packet.srcLink, pktMsg.srcPort)) != null)
					connection.packet(pktMsg);
				else if (pktMsg.flags == MailMessage.SYN) {
					connection = new Connection(pktMsg.packet.srcLink, pktMsg.srcPort, pktMsg.dstPort);
					connection.packet(pktMsg);


					connectionMap.put(connection);


					awaitingConnectionMap.addWaiting(connection);
				} else if (pktMsg.flags == MailMessage.FIN) {
					try {
						enqueue(new MailMessage(pktMsg.packet.srcLink, pktMsg.srcPort, pktMsg.packet.dstLink, pktMsg.dstPort, MailMessage.FIN | MailMessage.ACK, 0, MailMessage.EMPTY_CONTENT).packet);
					} catch (MalformedPacketException e) {
					}
				}
			}
		}

		/**
		 * 当数据包到达并且可以从网络链接中出列时调用。
		 */
		private void receiveInterrupt() {
			messageReceived.V();
		}




		private void send() {
			Packet p = null;
			while (true) {
				sendLock.acquire();


				while (sendQueue.isEmpty())
					nothingToSend.sleep();


				p = sendQueue.poll();
				sendLock.release();


				Machine.networkLink().send(p);
				messageSent.P();
			}
		}

		/**
		 *当一个数据包已经发送并且另一个可以排队到网络链接时调用。即使前一个数据包被丢弃，也会调用此方法。
		 *
		 *
		 *
		 *
		 *
		 */
		private void sendInterrupt() {
			messageSent.V();
		}






		private void timerRoutine() {
			while (true) {
				alarm.waitUntil(20000);





				connectionMap.retransmitAll();
				awaitingConnectionMap.retransmitAll();
			}
		}

		private final ConnectionMap connectionMap = new ConnectionMap();
		private final AwaitingConnectionMap awaitingConnectionMap = new AwaitingConnectionMap();

		private final Semaphore messageReceived = new Semaphore(0);
		private final Semaphore messageSent = new Semaphore(0);
		private final Lock sendLock = new Lock();



		private final Condition nothingToSend;

		private final LinkedList<Packet> sendQueue = new LinkedList<Packet>();
	}













	private static class ConnectionMap {
		void retransmitAll() {
			lock.acquire();
			for (Connection c : map.values())
				c.retransmit();
			lock.release();
		}

		void remove(Connection conn) {
			remove(conn.srcPort, conn.destAddress, conn.destPort);
		}

		boolean isEmpty() {
			lock.acquire();
			boolean b = map.isEmpty();
			lock.release();
			return b;
		}

		/**
		 * Closes all connections and removes them from this map.
		 */
		void shutdown() {
			lock.acquire();
			for (Connection c : map.values())
				c.close();
			lock.release();
		}

		Connection get(int sourcePort, int destinationAddress, int destinationPort) {
			lock.acquire();
			Connection c = map.get(new SocketKey(sourcePort,destinationAddress,destinationPort));
			lock.release();
			return c;
		}

		void put(Connection c) {
			lock.acquire();
			map.put(new SocketKey(c.srcPort,c.destAddress,c.destPort),c);
			lock.release();
		}

		Connection remove(int sourcePort, int destinationAddress, int destinationPort) {
			lock.acquire();
			Connection c = map.remove(new SocketKey(sourcePort,destinationAddress,destinationPort));
			lock.release();
			return c;
		}

		private final HashMap<SocketKey, Connection> map = new HashMap<SocketKey, Connection>();
		
		private final Lock lock = new Lock();
	}






	private static class AwaitingConnectionMap {

		boolean addWaiting(Connection c) {
			boolean ret = false;
			lock.acquire();
			if (!map.containsKey(c.srcPort))
				map.put(c.srcPort, new HashMap<>());

			if (map.get(c.srcPort).containsKey(null))
				ret = false;//Connection already exists
			else {
				map.get(c.srcPort).put(new SocketKey(c.srcPort,c.destAddress,c.destPort), c);
				ret = true;
			}
			lock.release();
			return ret;
		}

		/**
		 * 关闭所有连接并将它们从该映射中删除。
		 */
		void shutdown() {
			lock.acquire();
			map.clear();
			lock.release();
		}

		void retransmitAll() {
			lock.acquire();
			for (HashMap<SocketKey,Connection> hm : map.values())
				for (Connection c : hm.values())
					c.retransmit();
			lock.release();
		}





		Connection retrieve(int port) {
			Connection c = null;
			lock.acquire();
			if (map.containsKey(port)) {
				HashMap<SocketKey,Connection> mp = map.get(port);

				c = mp.remove(mp.keySet().iterator().next());



				if (mp.isEmpty())
					map.remove(port);
			}
			lock.release();
			
			return c;
		}

		private HashMap<Integer,HashMap<SocketKey,Connection>> map = new HashMap<Integer,HashMap<SocketKey,Connection>>();
		
		private Lock lock = new Lock();
	}

	private static class SocketKey {
		SocketKey(int srcPrt, int destAddr, int destPrt) {
			sourcePort = srcPrt;
			destAddress = destAddr;
			destPort = destPrt;
			hashcode = Long.valueOf(((long) sourcePort) + ((long) destAddress) + ((long) destPort)).hashCode();
		}

		@Override
		public int hashCode() {
			return hashcode;
		}

		@Override
		public boolean equals(Object o) {
			if (this == o)
				return true;
			else if (o instanceof SocketKey) {
				SocketKey oC = (SocketKey) o;
				return sourcePort == oC.sourcePort &&
				destAddress == oC.destAddress &&
				destPort == oC.destPort;
			} else
				return false;
		}

		private int sourcePort, destAddress, destPort, hashcode;
	}
}











