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

import org.mariadb.jdbc.internal.SQLExceptionMapper;
import org.mariadb.jdbc.internal.common.QueryException;
import org.mariadb.jdbc.internal.mysql.listener.Listener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantReadWriteLock;


public class FailoverProxy implements InvocationHandler {
    private final static Logger log = LoggerFactory.getLogger(FailoverProxy.class);

    public final static String METHOD_IS_EXPLICIT_CLOSED =  "isExplicitClosed";
    public final static String METHOD_GET_OPTIONS =  "getOptions";
    public final static String METHOD_GET_PROXY =  "getProxy";
    public final static String METHOD_EXECUTE_QUERY =  "executeQuery";
    public final static String METHOD_SET_READ_ONLY =  "setReadonly";
    public final static String METHOD_IS_READ_ONLY = "isReadOnly";
    public final static String METHOD_CLOSED_EXPLICIT =  "closeExplicit";
    public final static String METHOD_IS_CLOSED = "isClosed";


    public final ReentrantReadWriteLock lock;

    private Listener listener;

    public FailoverProxy(Listener listener, ReentrantReadWriteLock lock) throws QueryException, SQLException{
        this.lock = lock;
        this.listener = listener;
        this.listener.setProxy(this);
        this.listener.initializeConnection();
    }

    /**
     * proxy that catch Protocol call, to permit to catch errors and handle failover when multiple hosts
     * @param proxy the current protocol
     * @param method the called method on the protocol
     * @param args methods parameters
     * @return protocol method result
     * @throws Throwable the method throwed error if not catch by failover
     */
    @Override
    public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
        String methodName = method.getName();
        switch (methodName) {
            case METHOD_IS_EXPLICIT_CLOSED:
                return listener.isExplicitClosed();
            case METHOD_GET_OPTIONS:
                return listener.getJdbcUrl().getOptions();
            case METHOD_GET_PROXY:
                return this;
            case METHOD_IS_CLOSED:
                return listener.isClosed();
            case METHOD_EXECUTE_QUERY:
                try {
                    this.listener.preExecute();
                } catch (QueryException e) {
                    return handleFailOver(e, method, args);
                }
                break;
            case METHOD_SET_READ_ONLY:
                this.listener.switchReadOnlyConnection((Boolean) args[0]);
                return null;
            case METHOD_IS_READ_ONLY:
                return this.listener.isReadOnly();
            case METHOD_CLOSED_EXPLICIT:
                this.listener.preClose();
                return null;
        }
        try {
            return listener.invoke(method, args);
        } catch (InvocationTargetException e) {
            if (e.getTargetException() != null) {
                if (e.getTargetException() instanceof QueryException) {
                    if (hasToHandleFailover((QueryException) e.getTargetException())) {
                        return handleFailOver((QueryException) e.getTargetException(), method, args);
                    }
                }
                throw e.getTargetException();
            }
            throw e;
        }
    }


    /**
     * After a connection exception, launch failover
     * @param qe the exception thrown
     * @param method the method to call if failover works well
     * @param args the arguments of the method
     * @return the object return from the method
     * @throws Throwable
     */
    private Object handleFailOver(QueryException qe, Method method, Object[] args) throws Throwable{
        HandleErrorResult handleErrorResult = listener.handleFailover(method, args);
        if (handleErrorResult.mustThrowError) listener.throwFailoverMessage(qe, handleErrorResult.isReconnected);
        return handleErrorResult.resultObject;
    }

    /**
     * Check if this Sqlerror is a connection exception. if that's the case, must be handle by failover
     *
     * error codes :
     * 08000 : connection exception
     * 08001 : SQL client unable to establish SQL connection
     * 08002 : connection name in use
     * 08003 : connection does not exist
     * 08004 : SQL server rejected SQL connection
     * 08006 : connection failure
     * 08007 : transaction resolution unknown
     * 70100 : connection was killed
     * @param e the Exception
     * @return true if there has been a connection error that must be handled by failover
     */
    public boolean hasToHandleFailover(QueryException e){
        if (e.getSqlState() != null && e.getSqlState().startsWith("08")
                //|| "70100".equals(e.getSqlState())
                ) {
            return true;
        }
        return false;
    }

    public void reconnect() throws SQLException {
        try {
            listener.reconnect();
        } catch (QueryException e) {
            SQLExceptionMapper.throwException(e, null, null);
        }
    }

    public Listener getListener() {
        return listener;
    }
}
