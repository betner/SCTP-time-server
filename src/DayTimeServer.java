package se.pitch;

import org.jitsi.sctp4j.*;
import org.jitsi.util.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class DayTimeServer {
    private final static Logger logger = Logger.getLogger(DayTimeServer.class);

    static int US_STREAM = 0;
    static int FR_STREAM = 1;

    static SimpleDateFormat USformatter = new SimpleDateFormat("h:mm:ss a EEE d MMM yy, zzzz", Locale.US);
    static SimpleDateFormat FRformatter = new SimpleDateFormat("h:mm:ss a EEE d MMM yy, zzzz", Locale.FRENCH);
    static final ByteBuffer buf = ByteBuffer.allocateDirect(60);
    static final CharBuffer cbuf = CharBuffer.allocate(60);
    static final Charset charset = Charset.forName("ISO-8859-1");
    static final CharsetEncoder encoder = charset.newEncoder();

    public static void main(String[] args) throws IOException, InterruptedException {
        int sctpPort = 55555;
        int udpPort = 44444;
        int remoteUdpPort = 33333;

        String localAddress = "localhost";
        String remoteAddress = localAddress;

        Sctp.init();

        SctpSocket socket = Sctp.createSocket(sctpPort);
        UdpLink link = new UdpLink(socket, localAddress, udpPort, remoteAddress, remoteUdpPort);
        socket.setLink(link);

        socket.setDataCallback((data, sid, ssn, tsn, ppid, context, flags) ->
                logger.info("Server got some data: " + data.length
                + " stream: " + sid
                + " payload protocol id: " + ppid));

        socket.listen();

        // wait for connection
        while(!socket.accept()) {
            Thread.sleep(100);
        }

        Date today = new Date();
        cbuf.put(USformatter.format(today)).flip();
        encoder.encode(cbuf, buf, true);
        buf.flip();
        byte[] bytes = new byte[buf.remaining()];
        buf.get(bytes);

        socket.send(bytes, true, US_STREAM, 0);

        cbuf.clear();
        cbuf.put(FRformatter.format(today)).flip();
        buf.clear();
        encoder.encode(cbuf, buf, true);
        buf.flip();
        bytes = new byte[buf.remaining()];
        buf.get(bytes);

        socket.send(bytes, true, FR_STREAM, 0);

        Thread.sleep(40000);
        socket.close();
        Sctp.finish();
    }

}
