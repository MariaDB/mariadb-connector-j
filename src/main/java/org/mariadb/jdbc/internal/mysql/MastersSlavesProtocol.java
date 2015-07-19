/*
MariaDB Client for Java

Copyright (c) 2012 Monty Program Ab.

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

package org.mariadb.jdbc.internal.mysql;

import org.mariadb.jdbc.HostAddress;
import org.mariadb.jdbc.JDBCUrl;
import org.mariadb.jdbc.internal.common.QueryException;
import org.mariadb.jdbc.internal.mysql.listener.impl.MastersSlavesListener;
import org.mariadb.jdbc.internal.mysql.listener.tools.SearchFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class MastersSlavesProtocol extends MySQLProtocol {

    private final static Logger log = LoggerFactory.getLogger(MastersSlavesProtocol.class);
    boolean masterConnection = false;
    boolean mustBeMasterConnection = false;

    public MastersSlavesProtocol(final JDBCUrl url, final ReentrantReadWriteLock lock) {
        super(url, lock);
    }


    /**
     * loop until found the failed connection.
     *
     * @param listener current listener
     * @param addresses list of HostAddress to loop
     * @param blacklist current blacklist
     * @param searchFilter search parameter
     * @throws QueryException if not found
     */
    public static void loop(MastersSlavesListener listener, final List<HostAddress> addresses, Map<HostAddress, Long> blacklist, SearchFilter searchFilter) throws QueryException {
        if (log.isDebugEnabled()) {
            log.debug("searching for master:" + searchFilter.isSearchForMaster() + " replica:" + searchFilter.isSearchForSlave() + " addresses:" + addresses);
        }

        MastersSlavesProtocol protocol;
        List<HostAddress> loopAddresses = new LinkedList<>(addresses);
        int maxConnectionTry = listener.getRetriesAllDown();
        QueryException lastQueryException = null;

        while (!loopAddresses.isEmpty() || (!searchFilter.isUniqueLoop() && maxConnectionTry > 0)) {
            protocol = getNewProtocol(listener.getProxy(), listener.getJdbcUrl());

            if (listener.isExplicitClosed() || (!listener.isSecondaryHostFail() && !listener.isMasterHostFail())) return;
            maxConnectionTry--;

            try {
                protocol.setHostAddress(loopAddresses.get(0));
                loopAddresses.remove(0);

                if (log.isDebugEnabled()) log.debug("trying to connect to " + protocol.getHostAddress());

                protocol.connect();
                blacklist.remove(protocol.getHostAddress());
                if (log.isDebugEnabled()) log.debug("connected to " + (protocol.isMasterConnection()?"primary ":"replica ") + protocol.getHostAddress());

                if (searchFilter.isSearchForMaster() && protocol.isMasterConnection()) {
                    if (foundMaster(listener, protocol, searchFilter)) return;
                } else if (searchFilter.isSearchForSlave() && !protocol.isMasterConnection()) {
                    if (foundSecondary(listener, protocol, searchFilter)) return;
                } else {
                    protocol.close();
                }

            } catch (QueryException e) {
                lastQueryException = e;
                blacklist.put(protocol.getHostAddress(), System.currentTimeMillis());
                if (log.isDebugEnabled()) log.debug("Could not connect to " + protocol.getHostAddress() + " searching: " + searchFilter + " error: " + e.getMessage());
            }

            if (!searchFilter.isSearchForMaster() && !searchFilter.isSearchForSlave()) return;

            //loop is set so
            if (loopAddresses.isEmpty() && !searchFilter.isUniqueLoop() && maxConnectionTry > 0) {
                loopAddresses = new LinkedList<>(addresses);
                listener.checkIfTypeHaveChanged(searchFilter);
            }

        }

        if (searchFilter.isSearchForMaster() || searchFilter.isSearchForSlave()) {
            String error = "No active connection found for replica";
            if (searchFilter.isSearchForMaster()) error = "No active connection found for master";
            if (lastQueryException != null) {
                throw new QueryException(error, lastQueryException.getErrorCode(), lastQueryException.getSqlState(), lastQueryException);
            }
            throw new QueryException(error);
        }

    }

    private static boolean foundMaster(MastersSlavesListener listener, MastersSlavesProtocol protocol,SearchFilter searchFilter) {
        protocol.setMustBeMasterConnection(true);
        searchFilter.setSearchForMaster(false);
        listener.foundActiveMaster(protocol);
        if (!searchFilter.isSearchForSlave()) return true;
        else {
            if (listener.isExplicitClosed()
                    || searchFilter.isFineIfFoundOnlyMaster()
                    || !listener.isSecondaryHostFail()) return true;
        }
        return false;
    }

    private static boolean foundSecondary(MastersSlavesListener listener, MastersSlavesProtocol protocol,SearchFilter searchFilter) {
        searchFilter.setSearchForSlave(false);
        protocol.setMustBeMasterConnection(false);
        listener.foundActiveSecondary(protocol);
        if (!searchFilter.isSearchForMaster()) return true;
        else {
            if (listener.isExplicitClosed()
                    || searchFilter.isFineIfFoundOnlySlave()
                    || !listener.isMasterHostFail()) return true;
        }
        return false;
    }

    public static MastersSlavesProtocol getNewProtocol(FailoverProxy proxy, JDBCUrl jdbcUrl) {
        MastersSlavesProtocol newProtocol = new MastersSlavesProtocol(jdbcUrl, proxy.lock);
        newProtocol.setProxy(proxy);
        return newProtocol;
    }

    public boolean mustBeMasterConnection() {
        return mustBeMasterConnection;
    }
    public void setMustBeMasterConnection(boolean mustBeMasterConnection) {
        this.mustBeMasterConnection = mustBeMasterConnection;
    }
}
