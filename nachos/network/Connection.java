package nachos.network;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import nachos.machine.Lib;
import nachos.machine.Machine;
import nachos.machine.MalformedPacketException;
import nachos.machine.Packet;
import nachos.threads.Condition;
import nachos.threads.Lock;

class Connection {
	
	private Lock stateLock = new Lock();
	private Condition connectionEstablished;
	private NTPState currentState = NTPState.CLOSED;
	private boolean calledClose = false;

	private SendWindow sendWindow = new SendWindow();
	private ReceiveWindow receiveWindow = new ReceiveWindow();

	private ByteStream residualData = new ByteStream();
	private ByteStream sendBuffer = new ByteStream();
	
	int destAddress, destPort, srcPort;
	
	Connection(int destAddress, int destPort, int srcPort) {

		this.destAddress = destAddress;
		this.destPort = destPort;
		this.srcPort = srcPort;
		connectionEstablished = new Condition(stateLock);
	}


	/**
	 * 连接到另一个 nachos 实例
	 *
	 *
	 *
	 */

	boolean connect() {
		stateLock.acquire();
		currentState.connect(this);
		stateLock.release();
		
		//如果我们遇到死锁情况，则返回 false

		if (currentState == NTPState.DEADLOCK) {
			currentState = NTPState.CLOSED;
			return false;
		}
		return true;
	}
	

	boolean accept() {
		stateLock.acquire();
		currentState.accept(this);
		stateLock.release();
		return currentState == NTPState.ESTABLISHED;
	}


	void close() {
		stateLock.acquire();
		calledClose = true;
		currentState.close(this);
		stateLock.release();
	}
	

	protected void finished() {


		if (calledClose || exhausted()) {
			sendWindow.clear();
			receiveWindow.clear();
			NetKernel.postOffice.finished(this);
		}
	}






	void packet(MailMessage msg) {
		stateLock.acquire();
		switch (msg.flags) {
			case MailMessage.SYN -> currentState.syn(this, msg);

			case MailMessage.SYN | MailMessage.ACK -> currentState.synack(this, msg);
			case MailMessage.DATA -> currentState.data(this, msg);
			case MailMessage.ACK -> currentState.ack(this, msg);
			case MailMessage.STP -> {
				receiveWindow.stopAt(msg.sequence);
				currentState.stp(this, msg);
			}
			case MailMessage.FIN -> currentState.fin(this, msg);
			case MailMessage.FIN | MailMessage.ACK -> currentState.finack(this, msg);
			default -> {
			}
			// 丢弃无效数据包



		}
		stateLock.release();
	}
	







	void retransmit() {
		stateLock.acquire();
		currentState.timer(this);
		stateLock.release();
	}







	/**
	 *将缓冲区中的数据排队等待发送
	 */
	int send(byte[] buffer, int offset, int length) {
		byte[] toSend = new byte[length];
		System.arraycopy(buffer, offset, toSend, 0, Math.min(length, buffer.length - offset));
		
		stateLock.acquire();
		int sent = currentState.send(this, toSend);
		stateLock.release();
		return sent;
	}
	
	/**

	 */
	byte[] receive(int bytes) {
		stateLock.acquire();
		byte[] data = currentState.recv(this, bytes);
		stateLock.release();
		return data;
	}


	private void transmit(int flags) {














		NetKernel.postOffice.enqueue(Objects.requireNonNull(getMessage(flags, 0, MailMessage.EMPTY_CONTENT)).packet);
	}
	/** 确认一个数据包 */
	private void transmitAck(int sequence) {

		NetKernel.postOffice.enqueue(getMessage(MailMessage.ACK, sequence, MailMessage.EMPTY_CONTENT).packet);
	}
	private void transmitStp() {
		NetKernel.postOffice.enqueue(sendWindow.stopPacket(sendBuffer));
	}



	private void transmitData() {
		while (sendBuffer.size() > 0 && !sendWindow.full()) {
			byte[] toSend = sendBuffer.dequeue(Math.min(MailMessage.maxContentsLength, sendBuffer.size()));
			MailMessage msg = sendWindow.add(toSend);
			if (msg != null) {

				NetKernel.postOffice.enqueue(msg.packet);
			}
			else {



				Lib.assertNotReached();
				break;
			}
		}
	}
	
	/**
	 * 构造一个新的数据包寻址到这个连接的另一个端点
	 */
	private MailMessage getMessage(int flags, int sequence, byte[] contents) {
		try {
			return new MailMessage(destAddress, destPort, Machine.networkLink().getLinkAddress(), srcPort, flags, sequence, contents);
		} catch (MalformedPacketException e) {
			return null;
		}
	}
	
	private boolean exhausted() {
		return residualData.size() == 0 && receiveWindow.empty();
	}

	private enum NTPState {		
		CLOSED {
			@Override
			void connect(Connection c) {

				c.transmit(MailMessage.SYN);


				c.currentState = SYN_SENT;

				c.connectionEstablished.sleep();
			}
			
			@Override
			byte[] recv(Connection c, int maxBytes) {
				byte[] data = super.recv(c, maxBytes);
				

				if (c.exhausted())
					c.finished();
				
				return (data.length == 0) ? (null) : (data);
			}
			
			@Override
			int send(Connection c, byte[] buffer) {
				return -1;
			}
			
			@Override
			void syn(Connection c, MailMessage msg) {


				c.currentState = SYN_RCVD;
			} 
			
			@Override
			void fin(Connection c, MailMessage msg) {

				c.transmit(MailMessage.FIN | MailMessage.ACK);
			}

		},
		
		SYN_SENT {
			@Override
			void timer(Connection c) {

				c.transmit(MailMessage.SYN);
			}

			@Override
			void syn(Connection c, MailMessage msg) {


				c.currentState = DEADLOCK;
				c.connectionEstablished.wake();
			} 


			@Override
			void synack(Connection c, MailMessage msg) {



				c.currentState = ESTABLISHED;
				c.connectionEstablished.wake();
			}
			
			@Override
			void data(Connection c, MailMessage msg) {

				c.transmit(MailMessage.SYN);
			}
			@Override
			void stp(Connection c, MailMessage msg) {

				c.transmit(MailMessage.SYN);
			}
			@Override
			void fin(Connection c, MailMessage msg) {

				c.transmit(MailMessage.SYN);
			}
		},
		
		SYN_RCVD {
			@Override
			void accept(Connection c) {

				c.transmit(MailMessage.SYN | MailMessage.ACK);

				c.currentState = ESTABLISHED;
			}
		},
		
		ESTABLISHED {			
			@Override
			void close(Connection c) {
				if (c.sendWindow.empty() && c.sendBuffer.size() == 0) {//没有更多的数据要发送，无论是在队列中还是在窗口中
					c.transmit(MailMessage.FIN);

					c.currentState = CLOSING;
				}
				else {
					c.transmitStp();

					c.currentState = STP_SENT;
				}
			}
			
			@Override
			void syn(Connection c, MailMessage msg) {

				c.transmit(MailMessage.SYN | MailMessage.ACK);
			}
			
			@Override
			void data(Connection c, MailMessage msg) {
				c.receiveWindow.add(msg);



					c.transmitAck(msg.sequence);

			}
			
			@Override
			void ack(Connection c, MailMessage msg) {
				c.sendWindow.acked(msg.sequence);
				c.transmitData();
			}
			
			@Override
			void stp(Connection c, MailMessage msg) {
				c.sendWindow.clear();

				c.currentState = STP_RCVD;
			}
			
			@Override
			void fin(Connection c, MailMessage msg) {
				c.sendWindow.clear();
				c.transmit(MailMessage.FIN | MailMessage.ACK);

				c.currentState = CLOSED;
				c.finished();
			}
		},
		
		STP_SENT {
			@Override int send(Connection c, byte[] buffer) {

				return -1;
			}
			
			@Override
			void timer(Connection c) {
				if (c.sendWindow.empty())
					c.transmit(MailMessage.FIN);
				else
					c.transmitStp();
				

				super.timer(c);
			}
			
			@Override
			void ack(Connection c, MailMessage msg) {
				c.sendWindow.acked(msg.sequence);
				c.transmitData();
				
				if (c.sendWindow.empty() && c.sendBuffer.size() == 0) {
					c.transmit(MailMessage.FIN);
					c.currentState = CLOSING;
				}
			}
			
			@Override
			void syn(Connection c, MailMessage msg) {
				c.transmit(MailMessage.SYN | MailMessage.ACK);
			}
			
			@Override
			void data(Connection c, MailMessage msg) {
				c.transmitStp();
			}
			
			@Override
			void stp(Connection c, MailMessage msg) {
				c.sendWindow.clear();
				c.transmit(MailMessage.FIN);
				c.currentState = CLOSING;
			}
			
			@Override
			void fin(Connection c, MailMessage msg) {
				c.transmit(MailMessage.FIN | MailMessage.ACK);
				c.currentState = CLOSED;
				c.finished();
			}
			
		},
		
		STP_RCVD {
			@Override
			int send(Connection c, byte[] buffer) {
				// 无法在关闭连接上发送更多数据

				return -1;
			}
			
			@Override
			void close(Connection c) {
				c.transmit(MailMessage.FIN);

				c.currentState = CLOSING;
			}
			
			@Override
			void data(Connection c, MailMessage msg) {
				c.receiveWindow.add(msg);
					c.transmitAck(msg.sequence);





			}
			
			@Override
			void fin(Connection c, MailMessage msg) {
				c.transmit(MailMessage.FIN | MailMessage.ACK);

				c.currentState = CLOSED;
				c.finished();
			}
		},
		CLOSING {
			@Override
			int send(Connection c, byte[] buffer) {

				return -1;
			}
			
			@Override
			void timer(Connection c) {
				c.transmit(MailMessage.FIN);
			}
			
			@Override
			void syn(Connection c, MailMessage msg) {
				c.transmit(MailMessage.SYN | MailMessage.ACK);
			}
			
			@Override
			void data(Connection c, MailMessage msg) {
				c.transmit(MailMessage.FIN);
			}



			@Override
			void stp(Connection c, MailMessage msg) {
				c.transmit(MailMessage.FIN);
			}
			
			@Override
			void fin(Connection c, MailMessage msg) {
				c.transmit(MailMessage.FIN | MailMessage.ACK);

				c.currentState = CLOSED;
				c.finished();
			}

			@Override
			void finack(Connection c, MailMessage msg) {

				c.currentState = CLOSED;
				c.finished();
			}
		},
		
		DEADLOCK {};


		void connect(Connection c) {}

		void accept(Connection c) {}

		byte[] recv(Connection c, int maxBytes) {
			while (c.residualData.size() < maxBytes) {
				MailMessage msg = c.receiveWindow.remove();
				if (msg == null)
					break;
				try {
					c.residualData.write(msg.contents);
				} catch (IOException ignored) {}
			}
			
			return c.residualData.dequeue(Math.min(c.residualData.size(), maxBytes));
		}
		/** an app called write(). */
		int send(Connection c, byte[] buffer) {
			try {
				c.sendBuffer.write(buffer);
			} catch (IOException e) {
				e.printStackTrace();
			}
			c.transmitData();
			return buffer.length;
		} 

		void close(Connection c) {}

		void timer(Connection c) {

			NetKernel.postOffice.enqueue(c.sendWindow.packets());
		}

		void syn(Connection c, MailMessage msg) {} 

		void synack(Connection c, MailMessage msg) {}

		void data(Connection c, MailMessage msg) {}

		void ack(Connection c, MailMessage msg) {}

		void stp(Connection c, MailMessage msg) {}

		void fin(Connection c, MailMessage msg) {}

		void finack(Connection c, MailMessage msg) {}
	}
	
	private static class Window {
		protected static final int WINDOW_SIZE = 16;
		protected ArrayList<MailMessage> window = new ArrayList<MailMessage>(WINDOW_SIZE);
		protected int startSequence, lastSequenceNumber = -1;
		Window() {
			clear();
		}


		
		boolean add(MailMessage msg) {

			if (msg.sequence < startSequence)
				return true;
			

			if (msg.sequence >= startSequence + WINDOW_SIZE)
				return false;

			if (lastSequenceNumber > -1 && msg.sequence >= lastSequenceNumber)
				return false;
			
			int windowIndex = msg.sequence - startSequence;
			while (window.size() < windowIndex+1)
				window.add(null);
			if (window.get(windowIndex) == null)
				window.set(windowIndex, msg);
			
			return true;
		} 
		
		boolean empty() {
			return window.size() == 0;
		}
		boolean full() {
			return window.size() == WINDOW_SIZE;
		}
		
		List<Packet> packets() {
			List<Packet> lst = new ArrayList<Packet>();
			for (MailMessage m : window) {
				if (m != null)
					lst.add(m.packet);
			}

			return lst;
		}
		
		void clear() {
			window.clear();
			startSequence = 0;
			lastSequenceNumber = -1;
		}
	}
	
	private class SendWindow extends Window {
		protected int sequenceNumber;

		void acked(int sequence) {
			if (sequence < startSequence || sequence >= startSequence + window.size() || sequence >= lastSequenceNumber)
				return;
			
			int windowIndex = sequence - startSequence;
			window.set(windowIndex, null);
			

			while (window.size() > 0 && window.get(0) == null) {
				window.remove(0);
				startSequence++;
			}
		}

		MailMessage add(byte[] bytes) {
			MailMessage msg = getMessage(MailMessage.DATA, sequenceNumber, bytes);
			if (super.add(msg)) {

				sequenceNumber++;
			} else {

				msg = null;
			}
			
			return msg;
		}

		Packet stopPacket(ByteStream sendBuffer) {
			if (stopPacket == null) {
				lastSequenceNumber = (startSequence + window.size()) + (sendBuffer.size() / MailMessage.maxContentsLength) + (sendBuffer.size() % MailMessage.maxContentsLength != 0 ? 1 : 0);
				stopPacket = Objects.requireNonNull(getMessage(MailMessage.STP, lastSequenceNumber, MailMessage.EMPTY_CONTENT)).packet;
			}
			
			return stopPacket;
		}
		
		private Packet stopPacket = null;
	}
	
	private static class ReceiveWindow extends Window {
		MailMessage remove() {
			if (window.size() > 0 && window.get(0) != null) {
				startSequence++;
				return window.remove(0);
			}
			else
				return null;
		}

		void stopAt(int sequence) {
			if (lastSequenceNumber == -1)
				lastSequenceNumber = sequence;
		}
	}
	
	private static final char networkDebugFlag = 'n';

	private static class ByteStream extends ByteArrayOutputStream {
		byte[] dequeue(int bytes) {
			byte[] temp = super.toByteArray(), returnArray;
			
			if (bytes > temp.length)
				returnArray = new byte[temp.length];
			else
				returnArray = new byte[bytes];
			
			System.arraycopy(temp, 0, returnArray, 0, returnArray.length);
			
			super.reset();
			
			super.write(temp, returnArray.length, temp.length - returnArray.length);
			
			return returnArray;
		}
	}
}
