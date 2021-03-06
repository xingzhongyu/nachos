package nachos.network;

import nachos.machine.*;

/**
 * A mail message. Includes a packet header, a mail header, and the actual
 * payload.
 * 
 * @see nachos.machine.Packet
 */
 class MailMessage {
	/**
	 * Allocate a new mail message to be sent, using the specified parameters.
	 * 
	 * @param dstLink
	 *            the destination link address.
	 * @param dstPort
	 *            the destination port.
	 * @param srcLink
	 *            the source link address.
	 * @param srcPort
	 *            the source port.
	 * @param flags
	 *            packet flags
	 * @param sequence
	 *            sequence number for this packet
	 * @param contents
	 *            the contents of the packet.
	 */
	 MailMessage(int dstLink, int dstPort, int srcLink, int srcPort,
			int flags, int sequence, byte[] contents) throws MalformedPacketException {


		if (dstPort < 0 || dstPort >= portLimit || srcPort < 0 || srcPort >= portLimit || contents.length > maxContentsLength)
			throw new MalformedPacketException();

		this.dstPort = (byte) dstPort;
		this.srcPort = (byte) srcPort;
		this.flags = flags;
		this.sequence = sequence;
		this.contents = contents;

		byte[] packetContents = new byte[headerLength + contents.length];

		packetContents[0] = (byte) dstPort;
		packetContents[1] = (byte) srcPort;
		Lib.bytesFromInt(packetContents, 2, 2, flags);
		Lib.bytesFromInt(packetContents, 4, 4, sequence);

		System.arraycopy(contents, 0, packetContents, headerLength,
				contents.length);

		packet = new Packet(dstLink, srcLink, packetContents);
	}

	/**
	 * Allocate a new mail message using the specified packet from the network.
	 * 
	 * @param packet
	 *            the packet containg the mail message.
	 */
	 MailMessage(Packet packet) throws MalformedPacketException {
		this.packet = packet;




		
		if (packet.contents.length < headerLength || packet.contents[0] < 0 || packet.contents[1] < 0)
			throw new MalformedPacketException();

		dstPort = packet.contents[0];
		srcPort = packet.contents[1];
		flags = Lib.bytesToInt(packet.contents, 2, 2);
		sequence = Lib.bytesToInt(packet.contents, 4, 4);

		contents = new byte[packet.contents.length - headerLength];
		System.arraycopy(packet.contents, headerLength, contents, 0,
				contents.length);
	}

	/**
	 * Return a string representation of the message headers.
	 */
	 public String toString() {
		return "from (" + packet.srcLink + ":" + srcPort + ") to ("
				+ packet.dstLink + ":" + dstPort + "), " + contents.length
				+ " bytes";
	}

	/** This message, as a packet that can be sent through a network link. */
	 Packet packet;
	/** The port used by this message on the destination machine. */
	 int dstPort;
	/** The port used by this message on the source machine. */
	 int srcPort;
	/** Flags on this message */
	 int flags;
	/** Sequence number of this message */
	 int sequence;
	/** The contents of this message, excluding the mail message header. */
	 byte[] contents;

	/**
	 * The number of bytes in a mail header. The header is formatted as follows:
	 * 
	 * <table>
	 * <tr>
	 * <td>offset</td>
	 * <td>size</td>
	 * <td>value</td>
	 * </tr>
	 * <tr>
	 * <td>0</td>
	 * <td>1</td>
	 * <td>destination port</td>
	 * </tr>
	 * <tr>
	 * <td>1</td>
	 * <td>1</td>
	 * <td>source port</td>
	 * </tr>
	 * <tr>
	 * <td>2</td>
	 * <td>2</td>
	 * <td>flags</td>
	 * </tr>
	 * <tr>
	 * <td>4</td>
	 * <td>4</td>
	 * <td>sequence number</td>
	 * </tr>
	 * </table>
	 */
	 static final int headerLength = 8;

	/** Maximum payload (real data) that can be included in a single mesage. */
	 static final int maxContentsLength = Packet.maxContentsLength - headerLength;

	/**
	 * The upper limit on mail ports. All ports fall between <tt>0</tt> and
	 * <tt>portLimit - 1</tt>.
	 */
	 static final int portLimit = 128;
	
	/**
	 * ?????????????????????
	 *
	 */
	 static final int DATA = 0;
	 static final int SYN = 1;
	 static final int ACK = 2;
	 static final int STP = 4;
	 static final int FIN = 8;
	 
	 /** ????????????????????? */

	 static final byte[] EMPTY_CONTENT = new byte[0];
 }
