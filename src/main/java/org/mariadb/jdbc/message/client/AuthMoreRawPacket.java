// SPDX-License-Identifier: LGPL-2.1-or-later
// Copyright (c) 2012-2014 Monty Program Ab
// Copyright (c) 2015-2021 MariaDB Corporation Ab

package org.mariadb.jdbc.message.client;

import java.io.IOException;
import org.mariadb.jdbc.client.context.Context;
import org.mariadb.jdbc.client.socket.PacketWriter;

public final class AuthMoreRawPacket implements ClientMessage {

  private final byte[] raw;

  public AuthMoreRawPacket(byte[] raw) {
    this.raw = raw;
  }

  @Override
  public int encode(PacketWriter writer, Context context) throws IOException {
    if (raw.length == 0) {
      writer.writeEmptyPacket();
    } else {
      writer.writeBytes(raw);
      writer.flush();
    }
    return 0;
  }
}
