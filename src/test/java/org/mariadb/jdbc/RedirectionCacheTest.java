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

package org.mariadb.jdbc;

import static org.junit.Assert.fail;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.Assert;
import org.junit.Assume;
import org.junit.Test;
import org.mariadb.jdbc.BaseTest;

public class RedirectionCacheTest extends BaseTest {

	public boolean redirectAvailbleOnServer() throws SQLException {

	Connection connection = setBlankConnection("&enableRedirect=off");

	Statement stmt = connection.createStatement();
	ResultSet result = stmt.executeQuery("show variables like \"%redirect_enabled%\";");
	if(result.next()) {
	  return result.getString("Value").equalsIgnoreCase("ON");
	}
	return false;
		
  }

  @Test
  public void testParseRedirectionInfo() throws SQLException {
	Assume.assumeTrue(redirectAvailbleOnServer());

    try {
      int NUM_OF_THREADS = 100;

      String url =
          connU
              + "?user="
              + username
              + (password != null && !"".equals(password) ? "&password=" + password : "")
              + "&enableRedirect=on&pool=0";

      // Create the threads
      Thread[] threadList = new Thread[NUM_OF_THREADS];

      // spawn threads
      for (int i = 0; i < NUM_OF_THREADS; i++) {
        threadList[i] = new TestCache(url);
        threadList[i].start();
      }

      // wait for all threads to end
      for (int i = 0; i < NUM_OF_THREADS; i++) {
        threadList[i].join();
      }

    } catch (Exception e) {
      e.printStackTrace();
      fail();
    }
  }
}

final class TestCache extends Thread {
  int m_myId;
  String url;
  static int c_nextId = 1;

  static synchronized int getNextId() {
    return c_nextId++;
  }

  public TestCache(String url) {
    super();
    this.url = url;
    // Assign an Id to the thread
    m_myId = getNextId();
  }

  public void run() {
    int i = 0;
    int loop = 10;
    int failureCount = 0;
    for (i = 0; i < loop; i++) {
      try {
        Connection conn = null;
        ResultSet rs = null;
        Statement stmt = null;

        conn = DriverManager.getConnection(url);

        stmt = conn.createStatement(); // Create a Statement

        // Execute the Query
        rs = stmt.executeQuery("select user() as user");

        // Loop through the results
        Assert.assertTrue(rs.next());
        yield(); // Yield To other threads

        // Close all the resources
        rs.close();
        rs = null;

        // Close the statement
        stmt.close();
        stmt = null;

        conn.close();
        conn = null;
      } catch (Exception e) {
        System.out.println("Thread " + m_myId + " got Exception: " + e);
        e.printStackTrace();
        failureCount++;
      }
    }
    System.out.println("Thread " + m_myId + " is finished. ");
    System.out.println("Failure Count: " + failureCount);
  }
}
