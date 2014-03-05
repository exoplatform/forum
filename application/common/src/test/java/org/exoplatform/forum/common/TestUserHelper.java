/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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

import java.util.Date;

import junit.framework.TestCase;

import org.exoplatform.forum.common.UserHelper.FILTER_TYPE;
import org.exoplatform.forum.common.UserHelper.UserFilter;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;

public class TestUserHelper extends TestCase {


  public void testQueryFilter() {
    assertEquals(true, true);
    UserFilter filter = new UserFilter("", FILTER_TYPE.EMAIL);
    Query q = UserHelper.queryFilter(filter);
    assertEquals(true, q.isEmpty());
    //
    filter = new UserFilter("email", FILTER_TYPE.FIRST_NAME);
    q = UserHelper.queryFilter(filter);
    assertEquals("*email*", q.getEmail());
    //
    filter = new UserFilter("*email", FILTER_TYPE.EMAIL);
    q = UserHelper.queryFilter(filter);
    assertEquals("*email", q.getEmail());
    //
    filter = new UserFilter("email*", FILTER_TYPE.EMAIL);
    q = UserHelper.queryFilter(filter);
    assertEquals("email*", q.getEmail());
    //
    filter = new UserFilter("*email*", FILTER_TYPE.EMAIL);
    q = UserHelper.queryFilter(filter);
    assertEquals("*email*", q.getEmail());
  }
  
  public void testMatchUser() {
    User user = null;
    UserFilter userFilter = new UserFilter("abc", FILTER_TYPE.USER_NAME);
    //
    assertFalse(UserHelper.matchUser(userFilter, user));
    //
    user = new MockUser("abc", "ABC", "xABcD", "fooAbCbar");
    assertTrue(UserHelper.matchUser(userFilter, user));
    //
    userFilter = new UserFilter("abc", FILTER_TYPE.FIRST_NAME);
    assertTrue(UserHelper.matchUser(userFilter, user));
    //
    userFilter = new UserFilter("abc", FILTER_TYPE.LAST_NAME);
    assertTrue(UserHelper.matchUser(userFilter, user));
    //
    userFilter = new UserFilter("abc", FILTER_TYPE.EMAIL);
    assertTrue(UserHelper.matchUser(userFilter, user));
    
    //
    user = new MockUser("foo", "bar", "xyz", "bool");
    userFilter = new UserFilter("abc", FILTER_TYPE.USER_NAME);
    assertFalse(UserHelper.matchUser(userFilter, user));
    //
    userFilter = new UserFilter("abc", FILTER_TYPE.FIRST_NAME);
    assertFalse(UserHelper.matchUser(userFilter, user));
    //
    userFilter = new UserFilter("abc", FILTER_TYPE.LAST_NAME);
    assertFalse(UserHelper.matchUser(userFilter, user));
    //
    userFilter = new UserFilter("abc", FILTER_TYPE.EMAIL);
    assertFalse(UserHelper.matchUser(userFilter, user));
  }

  private class MockUser implements User {
    private String userName, firstName, lastName, email;

    public MockUser(String userName, String firstName, String lastName, String email) {
      this.userName = userName;
      this.firstName = firstName;
      this.lastName = lastName;
      this.email = email;
    }
    
    @Override
    public String getUserName() {
      return userName;
    }

    @Override
    public void setUserName(String s) {
    }

    @Override
    public String getPassword() {
      return null;
    }

    @Override
    public void setPassword(String s) {
    }

    @Override
    public String getFirstName() {
      return firstName;
    }

    @Override
    public void setFirstName(String s) {
    }

    @Override
    public String getLastName() {
      return lastName;
    }

    @Override
    public void setLastName(String s) {
    }

    @Override
    public String getFullName() {
      return null;
    }

    @Override
    public void setFullName(String s) {
    }

    @Override
    public String getEmail() {
      return email;
    }

    @Override
    public void setEmail(String s) {
    }

    @Override
    public Date getCreatedDate() {
      return null;
    }

    @Override
    public void setCreatedDate(Date t) {
    }

    @Override
    public Date getLastLoginTime() {
      return null;
    }

    @Override
    public void setLastLoginTime(Date t) {
    }

    @Override
    public String getDisplayName() {
      return null;
    }

    @Override
    public void setDisplayName(String displayName) {
    }

    @Override
    public String getOrganizationId() {
      return null;
    }

    @Override
    public void setOrganizationId(String organizationId) {
    }

  }

}
