package se.pitch;

import org.jitsi.sctp4j.NetworkLink;
import org.jitsi.sctp4j.SctpSocket;
import org.jitsi.util.Logger;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;

public class UdpLink
        implements NetworkLink
{
    /**
     * The logger
     */
    private final static Logger logger = Logger.getLogger(UdpLink.class);

    /**
     * <tt>SctpSocket</tt> instance that is used in this connection.
     */
    private final SctpSocket sctpSocket;

    /**
     * Udp socket used for transport.
     */
    private final DatagramSocket udpSocket;

    /**
     * Destination UDP port.
     */
    private final int remotePort;

    /**
     * Destination <tt>InetAddress</tt>.
     */
    private final InetAddress remoteIp;

    /**
     * Creates new instance of <tt>UdpConnection</tt>.
     *
     * @param sctpSocket SCTP socket instance used by this connection.
     * @param localIp local IP address.
     * @param localPort local UDP port.
     * @param remoteIp remote address.
     * @param remotePort destination UDP port.
     * @throws IOException when we fail to resolve any of addresses
     *                     or when opening UDP socket.
     */
    public UdpLink(SctpSocket sctpSocket,
                   String localIp, int localPort,
                   String remoteIp, int remotePort)
            throws IOException
    {
        this.sctpSocket = sctpSocket;

        this.udpSocket
                = new DatagramSocket(localPort, InetAddress.getByName(localIp));

        this.remotePort = remotePort;
        this.remoteIp = InetAddress.getByName(remoteIp);

        // Listening thread
        new Thread(
                new Runnable()
                {
                    public void run()
                    {
                        try
                        {
                            byte[] buff = new byte[2048];
                            DatagramPacket p = new DatagramPacket(buff, 2048);
                            while(true)
                            {
                                System.out.println("UdpSocket.receive()");
                                udpSocket.receive(p);

                                CharsetDecoder decoder = Charset.forName("ISO-8859-1").newDecoder();
                                ByteBuffer bytes = ByteBuffer.allocate(2048);
                                bytes.put(p.getData());
                                bytes.flip();
                                System.out.println(decoder.decode(bytes).toString());

                                UdpLink.this.sctpSocket.onConnIn(
                                        p.getData(), p.getOffset(), p.getLength());
                            }
                        }
                        catch(IOException e)
                        {
                            logger.error(e, e);
                        }
                    }
                }
        ).start();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onConnOut(final SctpSocket s, final byte[] packetData)
            throws IOException
    {
        System.out.println("UdpLink.onConnOut()");
        DatagramPacket packet
                = new DatagramPacket( packetData,
                packetData.length,
                remoteIp,
                remotePort);
        udpSocket.send(packet);
    }
}
