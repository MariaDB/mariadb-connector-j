/*
MariaDB Client for Java

Copyright (c) 2012-2014 Monty Program Ab.
Copyright (c) 2015-2016 MariaDB Ab.

This library is free software; you can redistribute it and/or modify it under
the terms of the GNU Lesser General Public License as published by the Free
Software Foundation; either version 2.1 of the License, or (at your option)
any later version.

This library is distributed in the hope that it will be useful, but
WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
for more details.

You should have received a copy of the GNU Lesser General Public License along
with this library; if not, write to Monty Program Ab info@montyprogram.com.

This particular MariaDB Client for Java file is work
derived from a Drizzle-JDBC. Drizzle-JDBC file which is covered by subject to
the following copyright and notice provisions:

Copyright (c) 2009-2011, Marcus Eriksson

Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:
Redistributions of source code must retain the above copyright notice, this list
of conditions and the following disclaimer.

Redistributions in binary form must reproduce the above copyright notice, this
list of conditions and the following disclaimer in the documentation and/or
other materials provided with the distribution.

Neither the name of the driver nor the names of its contributors may not be
used to endorse or promote products derived from this software without specific
prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS  AND CONTRIBUTORS "AS IS"
AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
OF SUCH DAMAGE.
*/

package org.mariadb.jdbc.internal.stream;

import org.mariadb.jdbc.internal.util.ExceptionMapper;
import org.mariadb.jdbc.internal.util.dao.QueryException;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.sql.Timestamp;
import java.util.Calendar;
import java.util.zip.DeflaterOutputStream;

public class PacketOutputStream extends OutputStream {
    private static final int MIN_COMPRESSION_SIZE = 16 * 1024;
    private static final float MIN_COMPRESSION_RATIO = 0.9f;
    private static final int MAX_PACKET_LENGTH = 0x00ffffff;
    private static final int HEADER_LENGTH = 4;
    private static final int BUFFER_DEFAULT_SIZE = 4096;
    private static final float NORMAL_INCREASE = 4f;
    private static final float BIG_SIZE_INCREASE = 1.5f;

    public ByteBuffer buffer;
    public ByteBuffer firstBuffer;

    int seqNo;
    int compressSeqNo;
    int lastSeq;
    int maxAllowedPacket = MAX_PACKET_LENGTH;
    int maxPacketSize = MAX_PACKET_LENGTH;
    boolean checkPacketLength;
    int maxRewritableLengthAllowed;
    boolean useCompression;
    public OutputStream outputStream;
    private volatile boolean closed = false;

    /**
     * Initialization with server outputStream.
     * @param outputStream server outPutStream
     */
    public PacketOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
        buffer = firstBuffer = ByteBuffer.allocate(BUFFER_DEFAULT_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        useCompression = false;
        buffer.position(4);
    }

    protected void increase(int newCapacity) {
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity).order(ByteOrder.LITTLE_ENDIAN);
        System.arraycopy(buffer.array(), 0, newBuffer.array(), 0, buffer.position());
        newBuffer.position(buffer.position());
        buffer = newBuffer;
    }

    public void setUseCompression(boolean useCompression) {
        this.useCompression = useCompression;
    }

    /**
     * Initialize stream sequence. Max stream allowed size will be checked.
     * @param seqNo stream sequence number
     * @param checkPacketLength indication that max stream allowed size will be checked.
     * @throws IOException if any error occur during data send to server
     */
    public void startPacket(int seqNo, boolean checkPacketLength) throws IOException {
        if (closed) {
            throw new IOException("Stream has already closed");
        }
        this.seqNo = seqNo;
        this.compressSeqNo = seqNo;
        this.checkPacketLength = checkPacketLength;
        buffer.clear();
        buffer.position(4);

    }

    /**
     * Initialize stream sequence.
     * @param seqNo stream sequence number
     * @throws IOException if any error occur during data send to server
     */
    public void startPacket(int seqNo) throws IOException {
        startPacket(seqNo, true);
    }

    /**
     * Send an empty stream to server.
     * @param seqNo stream sequence number
     * @throws IOException if any error occur during data send to server
     */
    public void writeEmptyPacket(int seqNo) throws IOException {
        byte[] header;
        if (!useCompression) {
            header = new byte[4];
            header[0] = ((byte) 0);
            header[1] = ((byte) 0);
            header[2] = ((byte) 0);
            header[3] = ((byte) seqNo);
            outputStream.write(header, 0, 4);
        } else {
            header = new byte[7];
            header[0] = (byte) 4;
            header[1] = (byte) 0;
            header[2] = (byte) 0;
            header[3] = (byte) compressSeqNo;
            header[4] = (byte) 0;
            header[5] = (byte) 0;
            header[6] = (byte) 0;
            outputStream.write(header, 0, 7);
            header = new byte[4];
            header[0] = ((byte) 0);
            header[1] = ((byte) 0);
            header[2] = ((byte) 0);
            header[3] = ((byte) seqNo);
            outputStream.write(header, 0, 4);
        }
        outputStream.flush();
    }

    /**
     * Used to send LOAD DATA INFILE. End of data is indicated by stream of length 0.
     * @param is inputStream to send
     * @param seq stream sequence number
     * @throws IOException if any error occur during data send to server
     */
    public void sendFile(InputStream is, int seq) throws IOException {
        this.seqNo = seq;
        this.compressSeqNo = 2;
        if (!useCompression) {
            buffer.clear();
            //reserve the 4th first bytes for header
            buffer.position(4);
            this.checkPacketLength = false;
            byte[] buffer = new byte[BUFFER_DEFAULT_SIZE];
            int len;
            while ((len = is.read(buffer)) > 0) {
                write(buffer, 0, len);
            }
            finishPacket();
            writeEmptyPacket(this.seqNo++);
        } else {
            buffer.clear();
            buffer.position(4);
            this.checkPacketLength = false;

            //write file into buffer
            byte[] readFileBuffer = new byte[BUFFER_DEFAULT_SIZE];
            int len;
            while ((len = is.read(readFileBuffer)) > 0) {
                write(readFileBuffer, 0, len);
            }

            if (buffer.position() > 4) {
                checkPacketMaxSize(buffer.position());

                buffer.flip();
                int limit = buffer.limit();
                buffer.position(4);
                int position = 0;
                int expectedPacketSize = limit + HEADER_LENGTH * ((limit / maxPacketSize) + 1);
                byte[] bufferBytes = new byte[expectedPacketSize];

                //write first packet
                while (position < expectedPacketSize - 4) {
                    int length = buffer.remaining();
                    if (length > maxPacketSize) {
                        length = maxPacketSize;
                    }
                    bufferBytes[position++] = (byte) (length & 0xff);
                    bufferBytes[position++] = (byte) (length >>> 8);
                    bufferBytes[position++] = (byte) (length >>> 16);
                    bufferBytes[position++] = (byte) seqNo++;

                    if (length > 0) {
                        buffer.get(bufferBytes, position, length);
                        position += length;
                    }
                }
                //write second packet (empty packet)
                bufferBytes[position++] = (byte) 0;
                bufferBytes[position++] = (byte) 0;
                bufferBytes[position++] = (byte) 0;
                bufferBytes[position++] = (byte) seqNo++;

                //send data
                compressedAndSend(position, bufferBytes);
            } else {
                writeEmptyPacket(seqNo);
            }

            //save big buffer next query to avoid new allocation if next query size is similar
            if (buffer.capacity() > BUFFER_DEFAULT_SIZE) {
                buffer = firstBuffer;
            }

            buffer.clear();
            buffer.position(4);
        }
    }

    /**
     * Send stream to server.
     * @param is inputStream to send
     * @throws IOException if any error occur during data send to server
     */
    public void sendStream(InputStream is) throws IOException {
        byte[] buffer = new byte[BUFFER_DEFAULT_SIZE];
        int len;
        while ((len = is.read(buffer)) > 0) {
            write(buffer, 0, len);
        }
    }

    /**
     * Send stream to server.
     * @param is inputStream to send
     * @param readLength max size to send
     * @throws IOException if any error occur during data send to server
     */
    public void sendStream(InputStream is, long readLength) throws IOException {
        byte[] buffer = new byte[BUFFER_DEFAULT_SIZE];
        long remainingReadLength = readLength;
        int read;
        while (remainingReadLength > 0) {
            read = is.read(buffer, 0, Math.min((int)remainingReadLength, BUFFER_DEFAULT_SIZE));
            if (read == -1) {
                return;
            }
            write(buffer, 0, read);
            remainingReadLength -= read;
        }
    }

    /**
     * Send reader stream to server.
     * @param reader reader to send
     * @throws IOException if any error occur during data send to server
     */
    public void sendStream(Reader reader) throws IOException {
        char[] buffer = new char[BUFFER_DEFAULT_SIZE];
        int len;
        while ((len = reader.read(buffer)) > 0) {
            byte[] bytes = new String(buffer, 0, len).getBytes("UTF-8");
            write(bytes, 0, bytes.length);
        }
    }

    /**
     * Send reader stream to server.
     * @param reader reader to send
     * @param readLength max size to send
     * @throws IOException if any error occur during data send to server
     */
    public void sendStream(Reader reader, long readLength) throws IOException {
        char[] buffer = new char[BUFFER_DEFAULT_SIZE];
        long remainingReadLength = readLength;
        int read;
        while (remainingReadLength > 0) {
            read = reader.read(buffer, 0, Math.min((int)remainingReadLength, BUFFER_DEFAULT_SIZE));
            if (read == -1) {
                return;
            }
            byte[] bytes = new String(buffer, 0, read).getBytes("UTF-8");
            write(bytes, 0, bytes.length);
            remainingReadLength -= read;
        }

    }

    /**
     * Ending command that tell to send buffer to server.
     * @throws IOException if any connection error occur
     */
    public void finishPacket() throws IOException {
        if (buffer.position() > 4) {
            checkPacketMaxSize(buffer.position());

            if (useCompression) {
                flushWithCompression();
            } else {
                flushDirect();
            }
        }

        //save big buffer next query to avoid new allocation if next query size is similar
        if (buffer.limit() * 2 < buffer.capacity()) {
            buffer = firstBuffer;
        }

        this.lastSeq =  (useCompression) ? this.compressSeqNo : this.seqNo;
    }

    @Override
    public void write(byte[] bytes) throws IOException {
        write(bytes, 0, bytes.length);
    }

    @Override
    public void write(int byteInt) throws IOException {
        byte[] byteArray = {(byte) byteInt};
        write(byteArray);
    }

    @Override
    public void write(byte[] bytes, int off, int len) throws IOException {
        assureBufferCapacity(len);
        buffer.put(bytes, off, len);
    }


    @Override
    public void flush() throws IOException {
        throw new AssertionError("Do not call flush() on PacketOutputStream. use finishPacket() instead.");
    }

    /**
     * Check that current buffer + length will not be superior to max_allowed_packet + header size.
     * That permit to separate rewritable queries to be separate in multiple stream.
     * @param length additionnal length
     * @return true if with this additional length stream can be send in the same stream
     */
    public boolean checkRewritableLength(int length) {
        return !(checkPacketLength && buffer.position() + length > maxRewritableLengthAllowed);
    }

    private void checkPacketMaxSize(int limit) throws MaxAllowedPacketException {
        if (checkPacketLength
                && maxAllowedPacket > 0
                && limit > (maxAllowedPacket - 1)) {
            this.seqNo = -1;
            throw new MaxAllowedPacketException("max_allowed_packet=" + maxAllowedPacket + ". stream size " + limit
                    + " is > to max_allowed_packet", this.seqNo != 0);
        }
    }

    private void flushDirect() throws IOException {
        buffer.flip();
        // the 4th first byte are reserved for first header.
        int dataLength = buffer.remaining() - 4;

        if (dataLength < maxPacketSize) {
            //if only one packet, put array to socket
            buffer.put((byte) (dataLength & 0xff))
                    .put((byte) (dataLength >>> 8))
                    .put((byte) (dataLength >>> 16))
                    .put((byte) seqNo++);

            outputStream.write(buffer.array(), 0, buffer.limit());
            outputStream.flush();
        } else {

            //multiple packet. Send first one
            buffer.put((byte) (maxPacketSize & 0xff))
                    .put((byte) (maxPacketSize >>> 8))
                    .put((byte) (maxPacketSize >>> 16))
                    .put((byte) seqNo++);

            outputStream.write(buffer.array(), 0, maxPacketSize + 4);
            outputStream.flush();
            buffer.position(maxPacketSize + 4);

            while (buffer.remaining() > 0 ) {
                int length = buffer.remaining();
                buffer.position(buffer.position() - 4);
                if (length > maxPacketSize) {
                    buffer.put((byte) (maxPacketSize & 0xff))
                            .put((byte) (maxPacketSize >>> 8))
                            .put((byte) (maxPacketSize >>> 16))
                            .put((byte) seqNo++);

                    outputStream.write(buffer.array(), buffer.position() - 4, maxPacketSize + 4);
                    outputStream.flush();
                    buffer.position(buffer.position() + maxPacketSize);
                } else {
                    buffer.put((byte) (length & 0xff))
                            .put((byte) (length >>> 8))
                            .put((byte) (length >>> 16))
                            .put((byte) seqNo++);
                    outputStream.write(buffer.array(), buffer.position() - 4, length + 4);
                    outputStream.flush();
                    break;
                }
            }
        }
    }


    private void flushWithCompression() throws IOException {
        buffer.flip();
        int limit = buffer.limit();
        buffer.position(4);
        int position = 0;
        int expectedPacketSize = limit - 4 + HEADER_LENGTH * ((limit / maxPacketSize) + 1);
        byte[] bufferBytes = new byte[expectedPacketSize];

        while (position < expectedPacketSize) {
            int length = buffer.remaining();
            if (length > maxPacketSize) {
                length = maxPacketSize;
            }
            bufferBytes[position++] = (byte) (length & 0xff);
            bufferBytes[position++] = (byte) (length >>> 8);
            bufferBytes[position++] = (byte) (length >>> 16);
            bufferBytes[position++] = (byte) seqNo++;

            if (length > 0) {
                buffer.get(bufferBytes, position, length);
                position += length;
            }
        }
        //now bufferBytes in filled with uncompressed data
        compressedAndSend(position, bufferBytes);
    }

    /**
     * Compress datas and send them to database.
     * @param notCompressPosition notCompressPosition
     * @param bufferBytes not compressed data buffer
     * @throws IOException if any compression or connection error occur
     */
    private void compressedAndSend(int notCompressPosition, byte[] bufferBytes) throws IOException {
        int position = 0;
        int packetLength;

        while (position - notCompressPosition < 0) {
            packetLength = Math.min(notCompressPosition - position, maxPacketSize);
            boolean compressedPacketSend = false;

            if (packetLength > MIN_COMPRESSION_SIZE) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                DeflaterOutputStream deflater = new DeflaterOutputStream(baos);

                deflater.write(bufferBytes, position, packetLength);
                deflater.finish();
                deflater.close();

                byte[] compressedBytes = baos.toByteArray();
                baos.close();

                if (compressedBytes.length < (int) (MIN_COMPRESSION_RATIO * packetLength)) {

                    int compressedLength = compressedBytes.length;
                    writeCompressedHeader(compressedLength, packetLength);
                    outputStream.write(compressedBytes, 0, compressedLength);
                    compressedPacketSend = true;
                }
            }

            if (!compressedPacketSend) {
                writeCompressedHeader(packetLength, 0);
                outputStream.write(bufferBytes, position, packetLength);
            }

            position += packetLength;
            outputStream.flush();
        }
    }

    private void writeCompressedHeader(int packetLength, int initialLength) throws IOException {
        byte[] header = new byte[7];
        header[0] = (byte) (packetLength & 0xff);
        header[1] = (byte) ((packetLength >> 8) & 0xff);
        header[2] = (byte) ((packetLength >> 16) & 0xff);
        header[3] = (byte) this.compressSeqNo++;
        header[4] = (byte) (initialLength & 0xff);
        header[5] = (byte) ((initialLength >> 8) & 0xff);
        header[6] = (byte) ((initialLength >> 16) & 0xff);
        outputStream.write(header);
    }



    @Override
    public void close() throws IOException {
        outputStream.close();
        buffer = null;
        firstBuffer = null;
        closed = true;
    }

    /**
     * Initialize maximal send size (can be send in multiple stream).
     * @param maxAllowedPacket value of server maxAllowedPacket
     */
    public void setMaxAllowedPacket(int maxAllowedPacket) {
        this.maxAllowedPacket = maxAllowedPacket;
        if (maxAllowedPacket > 0) {
            maxPacketSize = Math.min(maxAllowedPacket - 1, MAX_PACKET_LENGTH);
            maxRewritableLengthAllowed = (int) (maxAllowedPacket - 4 * Math.ceil(((double)maxAllowedPacket) / maxPacketSize));
        } else {
            maxPacketSize = MAX_PACKET_LENGTH;
        }
    }

    /**
     * Ensure that the buffer remaining size permit to write a data with a size len.
     * @param len size of the data
     */
    public void assureBufferCapacity(final int len) {
        while (len > buffer.remaining()) {
            int newCapacity = Math.max(
                    (int)(len + buffer.position() * NORMAL_INCREASE),
                    (int) ((buffer.capacity() > 4194304) ? buffer.capacity() * BIG_SIZE_INCREASE : buffer.capacity() * NORMAL_INCREASE));
            increase(newCapacity);
        }
    }

    /**
     * Write a byte data to buffer.
     * @param theByte byte to write
     * @return this
     */
    public PacketOutputStream writeByte(final byte theByte) {
        assureBufferCapacity(1);
        buffer.put(theByte);
        return this;
    }

    /**
     * Write count time the byte value.
     * @param theByte byte to write to buffer
     * @param count number of time the value will be put to buffer
     * @return this
     */
    public PacketOutputStream writeBytes(final byte theByte, final int count) {
        for (int i = 0; i < count; i++) {
            this.writeByte(theByte);
        }
        return this;
    }


    /**
     * Write byte array to buffer.
     * @param bytes  byte array
     * @return this.
     */
    public PacketOutputStream writeByteArray(final byte[] bytes) {
        assureBufferCapacity(bytes.length);
        buffer.put(bytes);
        return this;
    }

    /**
     * Write byte array data to binary data.
     * @param bytes  byte array to encode
     * @return this.
     */
    public PacketOutputStream writeByteArrayLength(final byte[] bytes) {
        assureBufferCapacity(bytes.length + 9);
        writeFieldLength(bytes.length);
        buffer.put(bytes);
        return this;
    }

    /**
     * Write string data in binary format.
     * @param str string value to encode
     * @return this.
     */
    public PacketOutputStream writeString(final String str) {
        final byte[] strBytes;
        try {
            strBytes = str.getBytes("UTF-8");
            return writeByteArray(strBytes);
        } catch (UnsupportedEncodingException u) {
            return this;
        }
    }

    /**
     * Write short data in binary format.
     * @param theShort short data to encode
     * @return this
     */
    public PacketOutputStream writeShort(final short theShort) {
        assureBufferCapacity(2);
        buffer.putShort(theShort);
        return this;
    }

    /**
     * Write int data in binary format.
     * @param theInt int data
     * @return this.
     */
    public PacketOutputStream writeInt(final int theInt) {
        assureBufferCapacity(4);
        buffer.putInt(theInt);
        return this;
    }

    /**
     * Write long data in binary format.
     * @param theLong long data
     * @return this
     */
    public PacketOutputStream writeLong(final long theLong) {
        assureBufferCapacity(8);
        buffer.putLong(theLong);
        return this;
    }

    /**
     * Write field length to encode in binary format.
     * @param length data length to encode
     * @return this.
     */
    public PacketOutputStream writeFieldLength(long length) {
        if (length < 251) {
            buffer.put((byte) length);
        } else if (length < 65536) {
            assureBufferCapacity(3);
            buffer.put((byte) 0xfc);
            buffer.putShort((short) length);
        } else if (length < 16777216) {
            assureBufferCapacity(4);
            buffer.put((byte) 0xfd);
            buffer.put((byte) (length & 0xff));
            buffer.put((byte) (length >>> 8));
            buffer.put((byte) (length >>> 16));
        } else {
            assureBufferCapacity(9);
            buffer.put((byte) 0xfe);
            buffer.putLong(length);
        }
        return this;
    }

    /**
     * Write string in binary format.
     * @param str string to encode
     * @return this.
     */
    public PacketOutputStream writeStringLength(final String str) {
        try {
            final byte[] strBytes = str.getBytes("UTF-8");
            assureBufferCapacity(strBytes.length + 9);
            writeFieldLength(strBytes.length);
            buffer.put(strBytes);
        } catch (UnsupportedEncodingException u) {
        }
        return this;
    }

    /**
     * Write timestamp in binary format.
     * @param calendar session calendar
     * @param ts timestamp to send
     * @param fractionalSeconds must fractionnal second be send to server
     * @return this
     */
    public PacketOutputStream writeTimestampLength(final Calendar calendar, Timestamp ts, boolean fractionalSeconds) {
        assureBufferCapacity(fractionalSeconds ? 12 : 8);
        buffer.put((byte) (fractionalSeconds ? 11 : 7));//length

        buffer.putShort((short) calendar.get(Calendar.YEAR));
        buffer.put((byte) ((calendar.get(Calendar.MONTH) + 1) & 0xff));
        buffer.put((byte) (calendar.get(Calendar.DAY_OF_MONTH) & 0xff));
        buffer.put((byte) calendar.get(Calendar.HOUR_OF_DAY));
        buffer.put((byte) calendar.get(Calendar.MINUTE));
        buffer.put((byte) calendar.get(Calendar.SECOND));
        if (fractionalSeconds) {
            buffer.putInt(ts.getNanos() / 1000);
        }
        return this;
    }

    /**
     * Write date in binary format.
     * @param calendar date
     * @return this
     */
    public PacketOutputStream writeDateLength(final Calendar calendar) {
        assureBufferCapacity(8);
        buffer.put((byte) 7);//length
        buffer.putShort((short) calendar.get(Calendar.YEAR));
        buffer.put((byte) ((calendar.get(Calendar.MONTH) + 1) & 0xff));
        buffer.put((byte) (calendar.get(Calendar.DAY_OF_MONTH) & 0xff));
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        buffer.put((byte) 0);
        return this;
    }

    /**
     * Write time in binary format.
     * @param calendar session calendar.
     * @param fractionalSeconds fractional seconds must be send
     * @return this
     */
    public PacketOutputStream writeTimeLength(final Calendar calendar, final boolean fractionalSeconds) {
        if (fractionalSeconds) {
            assureBufferCapacity(13);
            buffer.put((byte) 12);
            buffer.put((byte) 0);
            buffer.putInt(0);
            buffer.put((byte) calendar.get(Calendar.HOUR_OF_DAY));
            buffer.put((byte) calendar.get(Calendar.MINUTE));
            buffer.put((byte) calendar.get(Calendar.SECOND));
            buffer.putInt(calendar.get(Calendar.MILLISECOND) * 1000);
        } else {
            assureBufferCapacity(9);
            buffer.put((byte) 8);//length
            buffer.put((byte) 0);
            buffer.putInt(0);
            buffer.put((byte) calendar.get(Calendar.HOUR_OF_DAY));
            buffer.put((byte) calendar.get(Calendar.MINUTE));
            buffer.put((byte) calendar.get(Calendar.SECOND));
        }
        return this;
    }


    /**
     * Send directly to socket the sql data.
     * @param sql the query
     * @throws IOException if connection error occur
     * @throws QueryException if packet max size is to big.
     */
    public void sendPreparePacket(String sql) throws IOException, QueryException {
        if (closed) {
            throw new IOException("Stream has already closed");
        }
        compressSeqNo = 0;
        byte[] sqlBytes = sql.getBytes("UTF-8");
        int sqlLength = sqlBytes.length + 1;
        if (sqlLength > maxAllowedPacket) {
            throw new QueryException("Could not send query: max_allowed_packet=" + maxAllowedPacket + " but packet size is : "
                    + sqlLength, -1, ExceptionMapper.SqlStates.INTERRUPTED_EXCEPTION.getSqlState());
        }
        byte[] packetBuffer = new byte[sqlLength + 4];
        packetBuffer[0] = (byte) (sqlLength & 0xff);
        packetBuffer[1] = (byte) (sqlLength >>> 8);
        packetBuffer[2] = (byte) (sqlLength >>> 16);
        packetBuffer[3] = (byte) 0;
        packetBuffer[4] = (byte) 0x16;

        System.arraycopy(sqlBytes, 0, packetBuffer, 5, sqlLength - 1);

        if (!useCompression) {
            outputStream.write(packetBuffer);
            outputStream.flush();
        } else {
            compressedAndSend(sqlLength + 4, packetBuffer);
        }
    }


    /**
     * Send directly to socket the sql data.
     * @param sql the query
     * @throws IOException if connection error occur
     * @throws QueryException if packet max size is to big.
     */
    public void sendTextPacket(String sql) throws IOException, QueryException {
        if (closed) {
            throw new IOException("Stream has already closed");
        }
        seqNo = 0;
        compressSeqNo = 0;
        byte[] sqlBytes = sql.getBytes("UTF-8");
        int sqlLength = sqlBytes.length;

        if (sqlLength + 1 > maxAllowedPacket) {
            throw new QueryException("Could not send query: max_allowed_packet=" + maxAllowedPacket + " but packet size is : "
                    + (sqlLength + 1), -1, ExceptionMapper.SqlStates.INTERRUPTED_EXCEPTION.getSqlState());
        }
        if (!useCompression) {

            if (sqlLength + 1 <= maxPacketSize) {
                byte[] packetBuffer = new byte[sqlLength + 5];
                packetBuffer[0] = (byte) ((sqlLength + 1) & 0xff);
                packetBuffer[1] = (byte) ((sqlLength + 1) >>> 8);
                packetBuffer[2] = (byte) ((sqlLength + 1) >>> 16);
                packetBuffer[3] = (byte) seqNo++;
                packetBuffer[4] = (byte) 0x03; //TEXT protocol

                System.arraycopy(sqlBytes, 0, packetBuffer, 5, sqlLength);

                outputStream.write(packetBuffer);
                outputStream.flush();
            } else {
                //send first packet
                byte[] packetBuffer = new byte[maxPacketSize + 4];
                packetBuffer[0] = (byte) (maxPacketSize & 0xff);
                packetBuffer[1] = (byte) (maxPacketSize >>> 8);
                packetBuffer[2] = (byte) (maxPacketSize >>> 16);
                packetBuffer[3] = (byte) seqNo++;
                packetBuffer[4] = (byte) 0x03; //TEXT protocol
                System.arraycopy(sqlBytes, 0, packetBuffer, 5, maxPacketSize - 1);
                int sqlBytesPosition = maxPacketSize - 1;
                outputStream.write(packetBuffer);
                outputStream.flush();
                int length;
                while ((length = sqlLength - sqlBytesPosition) > 0) {
                    if (length > maxPacketSize) {
                        packetBuffer[0] = (byte) (maxPacketSize & 0xff);
                        packetBuffer[1] = (byte) (maxPacketSize >>> 8);
                        packetBuffer[2] = (byte) (maxPacketSize >>> 16);
                        packetBuffer[3] = (byte) seqNo++;
                        System.arraycopy(sqlBytes, sqlBytesPosition, packetBuffer, 4, maxPacketSize);
                        outputStream.write(packetBuffer);
                        outputStream.flush();
                        sqlBytesPosition += maxPacketSize;
                    } else {
                        packetBuffer[0] = (byte) (length & 0xff);
                        packetBuffer[1] = (byte) (length >>> 8);
                        packetBuffer[2] = (byte) (length >>> 16);
                        packetBuffer[3] = (byte) seqNo++;
                        System.arraycopy(sqlBytes, sqlBytesPosition, packetBuffer, 4, length);
                        outputStream.write(packetBuffer, 0, length + 4);
                        outputStream.flush();
                        break;
                    }
                }
            }
        } else {

            if (sqlLength < maxPacketSize) {
                byte[] packetBuffer = new byte[sqlLength + 5];
                packetBuffer[0] = (byte) ((sqlLength + 1) & 0xff);
                packetBuffer[1] = (byte) ((sqlLength + 1) >>> 8);
                packetBuffer[2] = (byte) ((sqlLength + 1) >>> 16);
                packetBuffer[3] = (byte) seqNo++;
                packetBuffer[4] = (byte) 0x03;

                System.arraycopy(sqlBytes, 0, packetBuffer, 5, sqlLength);
                compressedAndSend(sqlLength + 5, packetBuffer);

            } else {
                final int expectedPacketSize = sqlLength + 1 + 4 * (((sqlLength + 1) / maxPacketSize) + 1);

                //create packet
                byte[] packetBuffer = new byte[expectedPacketSize];
                packetBuffer[0] = (byte) (maxPacketSize & 0xff);
                packetBuffer[1] = (byte) (maxPacketSize >>> 8);
                packetBuffer[2] = (byte) (maxPacketSize >>> 16);
                packetBuffer[3] = (byte) seqNo++;
                packetBuffer[4] = (byte) 0x03;
                System.arraycopy(sqlBytes, 0, packetBuffer, 5, maxPacketSize - 1);

                int sqlBytesPosition = maxPacketSize - 1;
                int positionDest = maxPacketSize + 4;

                int length;
                while ((length = sqlLength - sqlBytesPosition) > 0) {
                    if (length > maxPacketSize) {
                        packetBuffer[positionDest++] = (byte) (maxPacketSize & 0xff);
                        packetBuffer[positionDest++] = (byte) (maxPacketSize >>> 8);
                        packetBuffer[positionDest++] = (byte) (maxPacketSize >>> 16);
                        packetBuffer[positionDest++] = (byte) seqNo++;
                        System.arraycopy(sqlBytes, sqlBytesPosition, packetBuffer, positionDest, maxPacketSize);
                        sqlBytesPosition += maxPacketSize;
                        positionDest += maxPacketSize;
                    } else {
                        packetBuffer[positionDest++] = (byte) (length & 0xff);
                        packetBuffer[positionDest++] = (byte) (length >>> 8);
                        packetBuffer[positionDest++] = (byte) (length >>> 16);
                        packetBuffer[positionDest++] = (byte) seqNo++;
                        System.arraycopy(sqlBytes, sqlBytesPosition, packetBuffer, positionDest, length);
                        break;
                    }
                }
                compressedAndSend(expectedPacketSize, packetBuffer);
            }
        }
    }
}
