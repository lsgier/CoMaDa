package de.tum.in.net.WSNDataFramework.CUSTOM;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketException;

import de.tum.in.net.WSNDataFramework.Modules.HTTPServer.WSNHTTPServerModule;
import de.tum.in.net.WSNDataFramework.Modules.IPFIX.UnsignedDataInputStream;
import de.tum.in.net.WSNDataFramework.Modules.TinyOS.DataPacket;
import de.tum.in.net.WSNDataFramework.Modules.TinyOS.TinyOSDriver;



/**
 * TinyOS Driver
 * 
 * @author André Freitag
 */
public class ThomasTinyOSDTLSDriver extends TinyOSDriver {
	/* constructors */
	public ThomasTinyOSDTLSDriver() {
		this(6106, 4739, 5568);
	}
	public ThomasTinyOSDTLSDriver(int blipPort, int udpPort, int sslUdpPort) {
		_blipPort = blipPort;
		_udpPort = udpPort;
		_dtlsUdpPort = sslUdpPort;
	}


	/* protected methods */
	/**
	 * shutdown handler
	 */
	@Override
	protected void _shutdown() {
		super._shutdown();
		_dtlsThread.interrupt();
		_dtlsServerSocket.close();
		_tThread.interrupt();
	}
	protected void _runSuper() {
		super._run();
	}
	@Override
	protected void _run() {
		_tThread = new Thread(new Runnable() {

			@Override
			public void run() {
				_runSuper();
			}

		});
		_tThread.setName("Module Worker " + this.getClass().getName() + " - DTLS parent");
		_tThread.start();

		// packet fetcher
		_dtlsThread = new Thread(new Runnable() {

			@Override
			public void run() {
				try {
					_fetchDtlsPacket();
				} catch (Exception e) {
					e.printStackTrace();
				}
			}

		});
		_dtlsThread.setName("Module Worker " + this.getClass().getName() + " - DTLS Packet Fetcher");
		_dtlsThread.start();

		// wait for both
		try {
			_tThread.join();
			_dtlsThread.join();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	protected void _fetchDtlsPacket() {
		//		while (!Thread.interrupted()) {
		//			_processPacket(new byte[]{});
		//
		//			try {
		//				Thread.sleep(1000);
		//			} catch (InterruptedException e) {
		//				// TODO Auto-generated catch block
		//				e.printStackTrace();
		//			}
		//		}
		try {
			_dtlsServerSocket = new DatagramSocket(_dtlsUdpPort);

			while (!Thread.interrupted()) {
				// receive datagram
				DataPacket p;
				try {
					p = _receiveDtlsDataPacket();
				} catch (Exception e) {
					e.printStackTrace();
					break;
				}


				if (p!=null && p.data!=null && p.data.length != 0) {
					// PROCESS packet
					_processPacket(p.data, p.address);
				} else { // no packet received, socket broken
					System.err.println("socket closed");
					break;
				}
			}

		} catch (SocketException e) {
			e.printStackTrace();
		} finally {
			try {
				_dtlsServerSocket.close();
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}
	/**
	 * receive another data packet. blocks thread until a new packet is available.
	 * 
	 * @return data packet
	 * @throws IOException
	 */
	protected DataPacket _receiveDtlsDataPacket() throws IOException {
		// receive packet
		byte[] buf = new byte[2048];
		DatagramPacket p = new DatagramPacket(buf, buf.length);
		_dtlsServerSocket.receive(p);

		// extract DTLS header
		byte[] data = new byte[p.getLength()];
		System.arraycopy(p.getData(), 0, data, 0, data.length);
		int dataLen = getDataLen(data);
		if(p.getLength() < HEADER_LEN + dataLen){
			System.err.println("Insufficient Length for a SSL Proxy packet");
			return null;
		}
		String ipAddress = getAddress(data);

		byte[] realdata = new byte[dataLen];
		System.arraycopy(data, HEADER_LEN, realdata, 0, dataLen);

		if (this.app().module(WSNHTTPServerModule.class) != null) {
			this.app().module(WSNHTTPServerModule.class).protocolLog("SSL received, data length: "+dataLen);
		}

		// return packet
		return new DataPacket(realdata, (InetSocketAddress)p.getSocketAddress());
	}


	/* protected helper */
	private static final int HEADER_LEN = 54;

	protected int getAddrLen(byte[] buf) throws IOException{
		UnsignedDataInputStream stream = new UnsignedDataInputStream(new ByteArrayInputStream(buf, 0, 2));
		int ret = stream.readUnsignedShort();
		stream.close();
		return ret;
	}
	protected int getPort(byte[] buf) throws IOException{
		UnsignedDataInputStream stream = new UnsignedDataInputStream(new ByteArrayInputStream(buf, 50, 2));
		int ret = stream.readUnsignedShort();
		stream.close();
		return ret;
	}
	protected int getDataLen(byte[] buf) throws IOException{
		UnsignedDataInputStream stream = new UnsignedDataInputStream(new ByteArrayInputStream(buf, 52, 2));
		int ret = stream.readUnsignedShort();
		stream.close();
		return ret;
	}

	String getAddress(byte[] buf) throws IOException{

		if(buf.length < HEADER_LEN){
			return null;
		}
		int len = getAddrLen(buf);
		if(len < 0){
			return null;
		}
		return new String(buf, 2, len);
	}

	/* protected member */
	protected int _dtlsUdpPort;
	protected Thread _dtlsThread=null;
	protected Thread _tThread=null;

	protected DatagramSocket _dtlsServerSocket;
}