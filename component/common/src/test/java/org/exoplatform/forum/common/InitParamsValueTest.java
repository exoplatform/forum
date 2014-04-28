/*
 * Copyright (C) 2003-2014 eXo Platform SAS.
 *
 * This program is free software; you can redistribute it and/or
* modify it under the terms of the GNU Affero General Public License
* as published by the Free Software Foundation; either version 3
* of the License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with this program; if not, see<http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.common;

import junit.framework.TestCase;

import org.exoplatform.commons.testing.KernelUtils;
import org.exoplatform.container.xml.InitParams;

public class InitParamsValueTest extends TestCase {

  public void testGetStringValue() {
    //
    assertNull(InitParamsValue.getString(null, null, null));
    assertEquals("", InitParamsValue.getString(null, null, ""));
    //
    InitParams initParams = new InitParams();
    KernelUtils.addValueParam(initParams, "role", "ADMIN");
    assertEquals("USER", InitParamsValue.getString(initParams, null, "USER"));
    assertEquals("ADMIN", InitParamsValue.getString(initParams, "role", "USER"));
  }

  public void testGetNumberValue() {
    assertEquals(0, InitParamsValue.getLong(null, null, 0l));
    //
    InitParams initParams = new InitParams();
    KernelUtils.addValueParam(initParams, "role", "ADMIN");
    KernelUtils.addValueParam(initParams, "roleNumber", "1");
    KernelUtils.addValueParam(initParams, "double-value", "1.5");
    //
    assertEquals(100, InitParamsValue.getLong(initParams, "not-param", 100l));
    //
    assertEquals(10, InitParamsValue.getLong(initParams, "role", 10l));
    assertEquals(10, InitParamsValue.getInteger(initParams, "role", 10));
    assertEquals(10d, InitParamsValue.getDouble(initParams, "role", 10d));
    //
    assertEquals(1, InitParamsValue.getLong(initParams, "roleNumber", 10l));
    assertEquals(1, InitParamsValue.getInteger(initParams, "roleNumber", 10));
    assertEquals(1d, InitParamsValue.getDouble(initParams, "roleNumber", 10d));
    //
    assertEquals(10, InitParamsValue.getLong(initParams, "double-value", 10l));
    assertEquals(10, InitParamsValue.getInteger(initParams, "double-value", 10));
    assertEquals(1.5d, InitParamsValue.getDouble(initParams, "double-value", 10d));
  }
  
  public void testGetBooleanValue() {
    assertFalse(InitParamsValue.getBoolean(null, null));
    //
    assertFalse(InitParamsValue.getBoolean(null, "true"));
    //
    InitParams initParams = new InitParams();
    KernelUtils.addValueParam(initParams, "isAdmin", "true");
    KernelUtils.addValueParam(initParams, "roleNumber", "1");
    KernelUtils.addValueParam(initParams, "double-value", "1.5");
    //
    assertFalse(InitParamsValue.getBoolean(initParams, "roleNumber"));
    assertFalse(InitParamsValue.getBoolean(initParams, "double-value"));
    //
    assertTrue(InitParamsValue.getBoolean(initParams, "isAdmin"));
  }
}
