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

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.lang.reflect.Field;
import java.net.Socket;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;
import org.mariadb.jdbc.internal.protocol.AbstractConnectProtocol;
import org.mariadb.jdbc.internal.protocol.Protocol;
import org.mariadb.jdbc.internal.util.constant.RedirectionErrorMessage;
import org.mariadb.jdbc.internal.util.constant.RedirectionOption;
import org.mariadb.jdbc.util.Options;

public class RedirectionConnectionTest extends BaseTest {

	  public boolean redirectAvailbleOnServer() throws SQLException {

		 Connection connection = setBlankConnection("&enableRedirect=off");

		 Statement stmt = connection.createStatement();
		 ResultSet result = stmt.executeQuery("show variables like \"%redirect_enabled%\";");
		 if(result.next()) {
				return result.getString("Value").equalsIgnoreCase("ON");
		 }
		 return false;
	  }

	 public boolean IsUsingRedirection(Connection connection) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		Field protocolField =  MariaDbConnection.class.getDeclaredField("protocol");
		Field isUsingRedirect = AbstractConnectProtocol.class.getDeclaredField("isUsingRedirectInfo");
		protocolField.setAccessible(true);
		isUsingRedirect.setAccessible(true);
		
		Field redirectHostField =  AbstractConnectProtocol.class.getDeclaredField("redirectHost");
		protocolField.setAccessible(true);
		redirectHostField.setAccessible(true);
				
		Protocol protocolVal = (Protocol) protocolField.get(connection);
		HostAddress redirectHostVal = (HostAddress) redirectHostField.get(protocolVal);
		Boolean isUsingRedirectVal = isUsingRedirect.getBoolean(protocolVal);
		Socket socket = protocolVal.getSocket();
		
		//Assume.assumeTrue(redirectHostVal!=null); //null means server does not support redirection, skip the case then

		if(redirectHostVal != null && redirectHostVal.host != null) {
			System.out.printf("Redirect host is: %s \n", redirectHostVal.host);
		}

		System.out.printf("Socket using host is: %s \n", socket.getRemoteSocketAddress().toString());

		return isUsingRedirectVal
				&& redirectHostVal.port == socket.getPort()
				&& socket.getRemoteSocketAddress().toString().contains(redirectHostVal.host);
	 }

	 public boolean IsUsingDirectConnnection(Connection connection) throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		 Field protocolField =  MariaDbConnection.class.getDeclaredField("protocol");
		 protocolField.setAccessible(true);

		 Protocol protocolVal = (Protocol) protocolField.get(connection);
		 Socket socket = protocolVal.getSocket();
		
		 return (protocolVal.getPort() == socket.getPort() && socket.getRemoteSocketAddress().toString().contains(protocolVal.getHost()));
	  }

	  @Before
	  public void check() throws SQLException, NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {

		  Field optionsField =  MariaDbConnection.class.getDeclaredField("options");
		  optionsField.setAccessible(true);
		  Options optionsVal = (Options) optionsField.get(sharedConnection);
		  Assume.assumeTrue(optionsVal.enableRedirect.equalsIgnoreCase(RedirectionOption.OFF.toString())
				  || optionsVal.enableRedirect.equalsIgnoreCase(RedirectionOption.ON.toString())
				  || optionsVal.enableRedirect.equalsIgnoreCase(RedirectionOption.PREFERRED.toString()));
		  
	  }

	  @Test
	  public void testRedirectOnEnforceRedirect() throws SQLException {
		  Assume.assumeTrue(redirectAvailbleOnServer());

		  try (Connection connection = setBlankConnection("&enableRedirect=on")) {
			
			  assertTrue(IsUsingRedirection(connection));

		  } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | SQLException e) {
			  e.printStackTrace();
			  fail();
	      }

	  }
	  
	  @Test
	  public void testRedirectOnFailIfNotAvailable() throws SQLException {

		  Assume.assumeFalse(redirectAvailbleOnServer());

		  try (Connection connection = setBlankConnection("&enableRedirect=on")) {
			
			  fail("Connection should not succeed");

		  } catch ( SecurityException | IllegalArgumentException | SQLException e) {
			  e.printStackTrace();
			  assertTrue(e.getMessage().contains(RedirectionErrorMessage.RedirectNotAvailable));
		  }
	  }	  

	  @Test
	  public void testRedirectPreferredWShouldAlwaysSucceed() {

		  try (Connection connection = setBlankConnection("&enableRedirect=preferred")) {

		  } catch (SQLException e) {
			  e.printStackTrace();
			  fail();
	      }
	  }
	  
	  @Test
	  public void testRedirectOff() {

		  try (Connection connection = setBlankConnection("&enableRedirect=off")) {

			  assertTrue(!IsUsingRedirection(connection) && IsUsingDirectConnnection(connection));

		  } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException| SQLException e) {
			  e.printStackTrace();
			  fail();
	      }
	  }
	  
	  @Test
	  public void checkRedirectionInfo() throws SQLException {
		  Assume.assumeTrue(redirectAvailbleOnServer());
		  
		  try (Connection connection = setBlankConnection("&enableRedirect=on")) {
			assertTrue(IsUsingRedirection(connection)); 

			Field protocolField =  MariaDbConnection.class.getDeclaredField("protocol");
			Field redirectHostField =  AbstractConnectProtocol.class.getDeclaredField("redirectHost");
			Field redirectUserField =  AbstractConnectProtocol.class.getDeclaredField("redirectUser");
			protocolField.setAccessible(true);
			redirectHostField.setAccessible(true);
			redirectUserField.setAccessible(true);

			Protocol protocolVal = (Protocol) protocolField.get(connection);
			HostAddress redirectHostVal = (HostAddress) redirectHostField.get(protocolVal);
			String redirectUserVal = (String) redirectUserField.get(protocolVal);
			
			Statement stmt = sharedConnection.createStatement();
			ResultSet result = stmt.executeQuery("show variables like \"%redirect%\";");
			while(result.next()) {
				String variableName = result.getString("Variable_name");
				String variableValue = result.getString("Value");
				System.out.println(variableName+"   "+ variableValue);
				switch (variableName) {
				case "redirect_enabled":
					assertTrue(variableValue.equals("ON"));
					break;
				case "redirect_flag":
					assertTrue(redirectUserVal.contains(variableValue));
					break;
				case "redirect_server_host":
					assertTrue(redirectHostVal.host.equals(variableValue));
					break;
				case "redirect_server_port":
					assertTrue(redirectHostVal.port == Integer.parseInt(variableValue));
					break;
				}
			}

		  } catch (NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException | SQLException e) {
			  e.printStackTrace();
			  fail();
	      }

	  }
	  
	  @Test 
	  public void basicSelectTest() throws SQLException {
		  Assume.assumeTrue(redirectAvailbleOnServer());

		  try (Connection connection = setBlankConnection("&enableRedirect=on")) {
			  assertTrue(IsUsingRedirection(connection));

			  Statement stmt = sharedConnection.createStatement();
			  ResultSet result = stmt.executeQuery("select 1");
			  assertTrue(-1 == stmt.getUpdateCount());
			  assert(result.next());
			  assertTrue(result.getLong("1") == 1);
		  } catch (SQLException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			  e.printStackTrace();
			  fail();
		  }

	  }
	  
	  @Test
	  public void BasicErrorTest() throws SQLException {
		  Assume.assumeTrue(redirectAvailbleOnServer());

		  try (Connection connection = setBlankConnection("&enableRedirect=on")) {
			  assertTrue(IsUsingRedirection(connection));

			  Statement stmt;
			  stmt = connection.createStatement();
			  stmt.executeQuery("whraoaooa");
			  fail("should not come here, query should fail");
		  } catch (SQLException | NoSuchFieldException | SecurityException | IllegalArgumentException | IllegalAccessException e) {
			  assertTrue(e.getMessage().contains("You have an error in your SQL syntax"));
		}

	  }
}
