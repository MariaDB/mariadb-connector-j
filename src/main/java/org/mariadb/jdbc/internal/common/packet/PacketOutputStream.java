package org.mariadb.jdbc.internal.common.packet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class PacketOutputStream extends OutputStream {
    private final static Logger log = LoggerFactory.getLogger(PacketOutputStream.class);

    private static final int MAX_PACKET_LENGTH = 0x00ffffff;
    private static final int SEQNO_OFFSET = 3;
    private static final int HEADER_LENGTH = 4;


    OutputStream baseStream;
    byte[] byteBuffer;
    int position;
    int seqNo;
    boolean compress;
    int maxAllowedPacket;
    int bytesWritten;
    boolean checkPacketLength;

    public PacketOutputStream(OutputStream baseStream) {
       this.baseStream = baseStream;
       byteBuffer = new byte[1024];
       this.seqNo = -1;
    }

    public void setCompress(boolean value) {
        if (this.seqNo != -1)
            throw new AssertionError("setCompress on already started packet is illegal");
        compress = value;
    }

    public void startPacket(int seqNo, boolean checkPacketLength) throws IOException {
        if (this.seqNo != -1) {
           throw new IOException("Last packet not finished");
        }
        this.seqNo = seqNo;
        position = HEADER_LENGTH;
        bytesWritten = 0;
        this.checkPacketLength = checkPacketLength;
    }

    public void startPacket(int seqNo) throws IOException {
        startPacket(seqNo, true);
    }
    public int getSeqNo() {
        return seqNo;
    }

    private void writeEmptyPacket(int seqNo) throws IOException {
        byteBuffer[0] = 0;
        byteBuffer[1] = 0;
        byteBuffer[2] = 0;
        byteBuffer[SEQNO_OFFSET] = (byte)seqNo;
        baseStream.write(byteBuffer, 0, 4);
        position = HEADER_LENGTH;
    }

    /* Used by LOAD DATA INFILE. End of data is indicated by packet of length 0. */
    public void sendFile(InputStream is, int seq) throws IOException{
    	int bufferSize = this.maxAllowedPacket > 0 ? Math.min(this.maxAllowedPacket, MAX_PACKET_LENGTH) : 1024;
    	bufferSize -= HEADER_LENGTH;
        byte[] buffer = new byte[bufferSize];
        int len;
        while((len = is.read(buffer)) > 0) {
          startPacket(seq++, false);
          write(buffer, 0, len);
          finishPacket();
        }
        writeEmptyPacket(seq);
    }

    public void finishPacket() throws IOException{
        if (this.seqNo == -1) {
            throw new AssertionError("Packet not started");
        }
        internalFlush();
        baseStream.flush();
        this.seqNo = -1;
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException{
      if (this.seqNo == -1) {
           throw new AssertionError("Use PacketOutputStream.startPacket() before write()");
      }

      for (;;) {
        if (len == 0)
            break;

        int bytesToWrite= Math.min(len, MAX_PACKET_LENGTH + HEADER_LENGTH - position);

        // Grow buffer if required
        if (byteBuffer.length - position < bytesToWrite) {
            byte[] tmp = new byte[Math.min(MAX_PACKET_LENGTH + HEADER_LENGTH, 2*(byteBuffer.length + bytesToWrite))];
            System.arraycopy(byteBuffer, 0, tmp, 0, position);
            byteBuffer = tmp;
        }
        System.arraycopy(bytes, off, byteBuffer, position,  bytesToWrite);
        position += bytesToWrite;
        off += bytesToWrite;
        len -= bytesToWrite;
        if (position == MAX_PACKET_LENGTH + HEADER_LENGTH) {
           internalFlush();
        }
      }
    }


    @Override
    public  void flush() throws IOException {
        throw new AssertionError("Do not call flush() on PacketOutputStream. use finishPacket() instead.");
    }

    private void internalFlush() throws IOException {
        int dataLen = position - HEADER_LENGTH;
        byteBuffer[0] = (byte)(dataLen & 0xff);
        byteBuffer[1] = (byte)((dataLen >> 8) & 0xff);
        byteBuffer[2] = (byte)((dataLen >> 16) & 0xff);
        byteBuffer[SEQNO_OFFSET] = (byte)this.seqNo;
        bytesWritten += dataLen + HEADER_LENGTH;
        if (maxAllowedPacket > 0 && bytesWritten > maxAllowedPacket && checkPacketLength) {
            this.seqNo=-1;
            throw new MaxAllowedPacketException("max_allowed_packet exceeded. wrote " + bytesWritten + ", max_allowed_packet = " +maxAllowedPacket, this.seqNo != 0);
        }
        baseStream.write(byteBuffer, 0, position);
        if (log.isTraceEnabled()) {
            byte[] tmp = new byte[Math.min(1000, position)];
            System.arraycopy(byteBuffer, 0, tmp, 0, Math.min(1000, position));
            log.trace(new String(tmp));
        }

        position = HEADER_LENGTH;
        this.seqNo++;
    }

    @Override
    public void write(byte[] bytes) throws IOException{
        write(bytes, 0, bytes.length);
    }

    @Override
    public void write(int b) throws IOException {
        byte[] a={(byte)b};
        write(a);
    }

    @Override
    public void close() throws IOException {
        baseStream.close();
        byteBuffer = null;
    }
    
    public void setMaxAllowedPacket(int maxAllowedPacket) {
    	this.maxAllowedPacket = maxAllowedPacket;
    }
}
