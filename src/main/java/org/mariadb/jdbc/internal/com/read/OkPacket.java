/*
 *
 * MariaDB Client for Java
 *
 * Copyright (c) 2012-2014 Monty Program Ab.
 * Copyright (c) 2015-2019 MariaDB Ab.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License along
 * with this library; if not, write to Monty Program Ab info@montyprogram.com.
 *
 * This particular MariaDB Client for Java file is work
 * derived from a Drizzle-JDBC. Drizzle-JDBC file which is covered by subject to
 * the following copyright and notice provisions:
 *
 * Copyright (c) 2009-2011, Marcus Eriksson
 *
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright notice, this list
 * of conditions and the following disclaimer.
 *
 * Redistributions in binary form must reproduce the above copyright notice, this
 * list of conditions and the following disclaimer in the documentation and/or
 * other materials provided with the distribution.
 *
 * Neither the name of the driver nor the names of its contributors may not be
 * used to endorse or promote products derived from this software without specific
 * prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS  AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR
 * PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY
 * OF SUCH DAMAGE.
 *
 */

package org.mariadb.jdbc.internal.com.read;

import java.nio.charset.StandardCharsets;

import org.mariadb.jdbc.internal.MariaDbServerCapabilities;
import org.mariadb.jdbc.internal.util.constant.ServerStatus;

public class OkPacket {

  private final long affectedRows;
  private final long insertId;
  private final short statusFlags;
  private final short warnings;
  private final String info;
  private final String sessionStateInfo;

  /**
   * Read Ok stream result.
   *
   * @param buffer current stream's rawBytes
   */
  public OkPacket(Buffer buffer, long clientCapabilities) {

    buffer.skipByte(); // 0x00 OkPacket header
    affectedRows = buffer.getLengthEncodedNumeric();
    insertId = buffer.getLengthEncodedNumeric();
 
    statusFlags = buffer.readShort();
    warnings = buffer.readShort();
    
    String message = "";
    String sessionStateMessage = "";
    if (buffer.remaining() > 0) {
    	if ((clientCapabilities & MariaDbServerCapabilities.CLIENT_SESSION_TRACK) !=0) {
    		message = buffer.readStringLengthEncoded(StandardCharsets.UTF_8);
    		
        	if ((statusFlags & ServerStatus.SERVER_SESSION_STATE_CHANGED) !=0 && buffer.remaining() > 0)  {
        		sessionStateMessage = buffer.readStringLengthEncoded(StandardCharsets.UTF_8);
            }
        } 
    	else {
    		message = buffer.readStringNullEnd(StandardCharsets.UTF_8);
    	}
    }
    info = message;
    sessionStateInfo = sessionStateMessage;

  }

  @Override
  public String toString() {
    return "affectedRows = "
        + affectedRows
        + "&insertId = "
        + insertId
        + "&statusFlags="
        + statusFlags
        + "&warnings="
        + warnings
        + "&info="
        + info
        + "&sessionStateInfo="
        + sessionStateInfo;
  }

  public long getAffectedRows() {
    return affectedRows;
  }

  public long getInsertId() {
    return insertId;
  }

  public short getServerStatus() {
    return statusFlags;
  }

  public short getWarnings() {
    return warnings;
  }

  public String getInfo() {
      return info;
  }
  
  public String getSessionStateInfo() {
	  return sessionStateInfo;
  }
}
