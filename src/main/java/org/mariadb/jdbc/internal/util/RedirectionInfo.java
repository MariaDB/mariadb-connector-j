/*
 *
 * MariaDB Client for Java
 *
 * Copyright (c) 2012-2014 Monty Program Ab.
 * Copyright (c) 2015-2017 MariaDB Ab.
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

package org.mariadb.jdbc.internal.util;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.mariadb.jdbc.HostAddress;

public class RedirectionInfo {
	//Redirection Info format: Location: mysql://[%s]:%u/?user=%s&ttl=%u\n

    private final HostAddress host;
    private final String user;
    private final int ttl;

    public RedirectionInfo(HostAddress host, String user, int ttl) {
        this.host 	= host;
        this.user 	= user;
        this.ttl 	= ttl;
    }

    public HostAddress getHost() {
        return host;
    }

    public String getUser() {
        return user;
    }
    
    public int getTTL() {
    	return ttl;
    }

    /**
    * Parse redirection info from a message returned by server.
    *
    * @param msg  the string which may contain redirection information.
    * @return RedirectionInfo host and user for redirection.
    */
    public static RedirectionInfo parseRedirectionInfo(String msg) {
        /**
         * Get redirected server information contained in OK packet.
         * Redirection string format:
         * Location: mysql://[%s]:%u/?user=%s&ttl=%u\n
         */
    	String host = "";
    	String user = "";
    	int port = -1;
    	int ttl = -1;
        try {

            Pattern INFO_PATTERN =
            		Pattern.compile("^Location: mysql://\\[([^\\]:]+)\\]:([0-9]+)/\\?user=([^&]+)&ttl=([0-9]+)\\n");

            Matcher m = INFO_PATTERN.matcher(msg);
            if(m.find()) {
            	host = m.group(1);
            	port = Integer.parseInt(m.group(2));
            	user = m.group(3);
            	ttl = Integer.parseInt(m.group(4));
            }

        } catch (Exception e) {
        	//eat exception
        	e.printStackTrace();
        }
        
        if(host=="") {
        	return null;
        }
        else {
        	return new RedirectionInfo(new HostAddress(host, port), user, ttl);
        }
    }
}
