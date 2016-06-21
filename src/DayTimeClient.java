package se.pitch;

import org.jitsi.sctp4j.Sctp;
import org.jitsi.sctp4j.SctpSocket;
import org.jitsi.util.Logger;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.Charset;
import java.nio.charset.CharsetDecoder;


public class DayTimeClient {
    static int US_STREAM = 0;
    static int FR_STREAM = 1;
    static ByteBuffer buf = ByteBuffer.allocateDirect(60);
    static Charset charset = Charset.forName("ISO-8859-1");
    static CharsetDecoder decoder = charset.newDecoder();

    private final static Logger logger = Logger.getLogger(DayTimeClient.class);

    public static void main(String[] args) throws IOException, InterruptedException {
        String localAddr = "localhost";
        int localUdpPort = 33333;
        int localSctpPort = 66666;

        String remoteAddr = "localhost";
        int remoteUdpPort = 44444;
        int remoteSctpPort = 55555;

        Sctp.init();

        final SctpSocket client = Sctp.createSocket(localSctpPort);

        UdpLink link = new UdpLink(client, localAddr, localUdpPort, remoteAddr, remoteUdpPort);
        client.setLink(link);

        client.setDataCallback((data, sid, ssn, tsn, ppid, context, flags) -> {
            logger.info("Client got some data: " + data.length
                    + " stream: " + sid
                    + " payload protocol id: " + ppid);


            buf.put(data);

            buf.flip();

            if (buf.remaining() > 0 &&
                    sid == US_STREAM) {

                try {
                    System.out.println("(US) " + decoder.decode(buf).toString());
                } catch (CharacterCodingException e) {
                    e.printStackTrace();
                }
            } else if (buf.remaining() > 0 &&
                   sid == FR_STREAM) {

                try {
                    System.out.println("(FR) " +  decoder.decode(buf).toString());
                } catch (CharacterCodingException e) {
                    e.printStackTrace();
                }
            }
            buf.clear();

        });


        client.connect(remoteSctpPort);
        Thread.sleep(4000);
        client.close();
        Sctp.finish();
    }

}
