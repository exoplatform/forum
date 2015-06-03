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
package org.exoplatform.forum.service;

import java.util.Arrays;
import java.util.List;

import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.services.organization.User;

public class ForumServiceUtilsTestCase extends BaseForumServiceTestCase {

  public void setUp() throws Exception {
    super.setUp();
  }

  public void tearDown() throws Exception {
    super.tearDown();
  }
  
  public void testHasPermission() throws Exception {
    String userGroupMembership[] = null;
    String userId = "";
    assertFalse(ForumServiceUtils.hasPermission(userGroupMembership, userId));

    userGroupMembership = new String[] { "mary", "member:/platform/users" };
    assertFalse(ForumServiceUtils.hasPermission(userGroupMembership, userId));

    userId = "user";
    assertFalse(ForumServiceUtils.hasPermission(userGroupMembership, userId));

    userId = "mary";
    assertTrue(ForumServiceUtils.hasPermission(userGroupMembership, userId));

    userId = "demo";
    assertTrue(ForumServiceUtils.hasPermission(userGroupMembership, userId));

    userGroupMembership = new String[] { "john", "/platform/users" };
    assertTrue(ForumServiceUtils.hasPermission(userGroupMembership, userId));

    userGroupMembership = new String[] { "root", "manager:/platform/users" };
    assertFalse(ForumServiceUtils.hasPermission(userGroupMembership, userId));

    userGroupMembership = new String[] { "root", "member:/platform/administrators" };
    assertFalse(ForumServiceUtils.hasPermission(userGroupMembership, userId));
  }
  
  public void testHasPermissionWildcard() throws Exception {
    //
    assertTrue(ForumServiceUtils.hasPermission(new String[] { "*:/platform/users" }, "demo"));
    assertTrue(ForumServiceUtils.hasPermission(new String[] { "*:/platform/users" }, "ghost"));
    assertTrue(ForumServiceUtils.hasPermission(new String[] { "manager:/platform/users" }, "ghost"));
  }
  
  public void testIsGroupExpression() {
    String expr = "demo";
    assertFalse(ForumServiceUtils.isGroupExpression(expr));
    expr = "manager:/platform/users";
    assertFalse(ForumServiceUtils.isGroupExpression(expr));
    expr = "/platform/users";
    assertTrue(ForumServiceUtils.isGroupExpression(expr));
  }

  public void testIsMembershipExpression() {
    String expr = "demo";
    assertFalse(ForumServiceUtils.isMembershipExpression(expr));
    expr = "manager:/platform/users";
    assertTrue(ForumServiceUtils.isMembershipExpression(expr));
    expr = "/platform/users";
    assertFalse(ForumServiceUtils.isMembershipExpression(expr));
  }
  
  public void testGetUserPermission() throws Exception {
    String userGroupMembership[] = null;
    
    assertEquals(0, ForumServiceUtils.getUserPermission(userGroupMembership).size());
    userGroupMembership = new String[] { "mary", "demo" };
    assertEquals("[demo, mary]", ForumServiceUtils.getUserPermission(userGroupMembership).toString());

    userGroupMembership = new String[] { "mary", "demo", "abc"};
    assertEquals("[demo, mary]", ForumServiceUtils.getUserPermission(userGroupMembership).toString());

    userGroupMembership = new String[] { "mary", "/platform/users"};
    assertEquals("[demo, paul, jame, root, raul, john, ghost, mary]",
                  ForumServiceUtils.getUserPermission(userGroupMembership).toString());

    userGroupMembership = new String[] { "john", "manager:/platform/administrators" };
    assertEquals("[root, john]", ForumServiceUtils.getUserPermission(userGroupMembership).toString());

    // disable user demo
    UserHelper.getUserHandler().setEnabled("demo", false, true);
    //
    userGroupMembership = new String[] { "mary", "demo" };
    assertEquals("[mary]", ForumServiceUtils.getUserPermission(userGroupMembership).toString());
    //
    userGroupMembership = new String[] { "/platform/users"};
    assertEquals("[paul, jame, root, raul, john, ghost, mary]",
                  ForumServiceUtils.getUserPermission(userGroupMembership).toString());
    // enable user demo
    UserHelper.getUserHandler().setEnabled("demo", true, true);
    //
    userGroupMembership = new String[] { "mary", "demo" };
    assertEquals("[demo, mary]", ForumServiceUtils.getUserPermission(userGroupMembership).toString());
    //
    userGroupMembership = new String[] { "/platform/users"};
    assertEquals("[demo, paul, jame, root, raul, john, ghost, mary]",
                  ForumServiceUtils.getUserPermission(userGroupMembership).toString());
  }
  
  public void testGetUserPermissionWildcard() throws Exception {
    assertListEquals("demo, paul, jame, root, raul, john, ghost, mary", 
                 ForumServiceUtils.getUserPermission(new String[] { "*:/platform/users" }));
    //
    UserHelper.getUserHandler().setEnabled("demo", false, true);
    //
    assertListEquals("paul, jame, root, raul, john, ghost, mary", 
                 ForumServiceUtils.getUserPermission(new String[] { "*:/platform/users" }));
    assertListEquals("paul, jame, root, raul, john, ghost, mary", 
                 ForumServiceUtils.getUserPermission(new String[] { "member:/platform/users" }));
    //
    assertListEquals("paul, root, ghost", 
                 ForumServiceUtils.getUserPermission(new String[] { "manager:/platform/users" }));
    //
    createUser("user01", "/platform/users", "member");
    assertListEquals("user01, paul, jame, root, john, raul, ghost, mary",
                 ForumServiceUtils.getUserPermission(new String[] { "member:/platform/users" }));
    //
    createUser("user02", "/platform/users", "member");
    assertListEquals("john", ForumServiceUtils.getUserPermission(new String[] { "member:/platform/administrators" }));
    //
    createUser("user02", "/platform/administrators", "member");
    assertListEquals("user02, john", ForumServiceUtils.getUserPermission(new String[] { "member:/platform/administrators" }));

    createUser("user03", "/platform/guests", "member");
    assertListEquals("user03", ForumServiceUtils.getUserPermission(new String[] { "*:/platform/guests" }));

    assertListEquals("paul, root, ghost", ForumServiceUtils.getUserPermission(new String[] { "manager:/platform/users" }));
    createUser("user03", "/platform/users", "manager");
    assertListEquals("user03, paul, root, ghost", ForumServiceUtils.getUserPermission(new String[] { "manager:/platform/users" }));
    // remove user
    for (int i = 0; i < 4; i++) {
      //
      UserHelper.getUserHandler().removeUser("user0" + i, false);
    }
    //
    UserHelper.getUserHandler().setEnabled("demo", true, true);
    assertListEquals("demo, jame, paul, root, john, raul, ghost, mary", 
                     ForumServiceUtils.getUserPermission(new String[] { "member:/platform/users" }));
  }

  private void createUser(String userName, String groupId, String membershipType) throws Exception {
    //
    User user = UserHelper.getUserHandler().findUserByName(userName);
    if (user == null) {
      user = UserHelper.getUserHandler().createUserInstance(userName);
      user.setEmail(userName + "@plf.com");
      user.setFirstName(userName);
      user.setLastName(userName);
      user.setPassword(userName);
      user.setDisplayName(userName + " " + userName);
      UserHelper.getUserHandler().createUser(user, true);
    }
    if(groupId != null) {
      UserHelper.getMembershipHandler()
      .linkMembership(user, 
                      UserHelper.getGroupHandler().findGroupById(groupId), 
                      UserHelper.getOrganizationService().getMembershipTypeHandler().findMembershipType(membershipType),
                      true);
    }
  }
  
  private void assertListEquals(String valueString, List<String> list) {
    if (valueString == null && list != null || valueString != null && list == null) {
      fail();
    }
    //
    List<String> values = Arrays.asList(valueString.replace(" ", "").split(","));
    if (values.size() != list.size()) {
      fail();
    }
    for (String val : values) {
      if (!list.contains(val)) {
        fail();
      }
    }
  }
  
}
