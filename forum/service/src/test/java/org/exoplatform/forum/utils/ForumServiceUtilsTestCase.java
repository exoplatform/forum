package org.exoplatform.forum.utils;

import java.util.List;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.forum.base.AssertUtils;
import org.exoplatform.forum.base.BaseTestCase;
import org.exoplatform.forum.membership.SimpleMockOrganizationService;
import org.exoplatform.forum.service.ForumServiceUtils;
import org.exoplatform.services.organization.auth.OrganizationAuthenticatorImpl;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.IdentityRegistry;

@ConfiguredBy( { 
  @ConfigurationUnit(
      scope = ContainerScope.PORTAL,
      path = "forumconf/ForumServiceUtils-configuration.xml"
  ) 
})
public class ForumServiceUtilsTestCase extends BaseTestCase {

  protected SimpleMockOrganizationService organizationService = null;

  protected IdentityRegistry              identityRegistry    = null;

  public ForumServiceUtilsTestCase() throws Exception {
  }
  
  @Override
  public void beforeRunBare() throws Exception {
    setGetAllConfig(false);
    super.beforeRunBare();
  }
  
  protected void setUp() throws Exception {
    organizationService = (SimpleMockOrganizationService) getContainer().getComponentInstanceOfType(SimpleMockOrganizationService.class);
    identityRegistry = (IdentityRegistry)getContainer().getComponentInstanceOfType(IdentityRegistry.class);
  }

  public void testHasPermission() throws Exception {

    String user = "user1";
    organizationService.addMemberships(user, "*:/platform/users");

    simulateAuthenticate(user);

    assertFalse(ForumServiceUtils.hasPermission(null, user));
    assertFalse(ForumServiceUtils.hasPermission(new String[] { " " }, user));
    assertTrue(ForumServiceUtils.hasPermission(new String[] { user }, user));
    assertFalse(ForumServiceUtils.hasPermission(new String[] { "foo" }, user));

    assertTrue(ForumServiceUtils.hasPermission(new String[] { "/platform/users" }, user));
    assertTrue(ForumServiceUtils.hasPermission(new String[] { "/platform/users", user }, user));

    // must match one (OR)
    assertTrue(ForumServiceUtils.hasPermission(new String[] { "/platform/users", "user2" }, user));

    assertFalse(ForumServiceUtils.hasPermission(new String[] { "/foo" }, user));
    // suspicious * should theorically match 'admin'
    assertTrue(ForumServiceUtils.hasPermission(new String[] { "admin:/platform/users" }, user));

    assertTrue(ForumServiceUtils.hasPermission(new String[] { "*:/platform/users" }, user));

  }

  private void simulateAuthenticate(String user) throws Exception {
    Identity identity = new OrganizationAuthenticatorImpl(organizationService).createIdentity(user);
    identityRegistry.register(identity);
  }

  public void testGetPermissionNull() throws Exception {
    List<String> emptyList = ForumServiceUtils.getUserPermission(null);
    assertNotNull(emptyList);
    assertEquals(0, emptyList.size());
  }

  public void testGetPermissionByGroup() throws Exception {
     organizationService.addMemberships("user1", "*:/platform/users");
     organizationService.addMemberships("user2", "*:/platform/users");
     organizationService.addMemberships("user3", "*:/platform");
        
     assertEquals(ForumServiceUtils.getUserPermission(new String [] {"/platform/users"}).size(), 2);
     AssertUtils.assertContains(ForumServiceUtils.getUserPermission(new String [] {"/platform/users"}), "user1", "user2");
     AssertUtils.assertNotContains(ForumServiceUtils.getUserPermission(new String [] {"/platform/users"}), "user3");
     AssertUtils.assertContains(ForumServiceUtils.getUserPermission(new String [] {"/platform/users", "/platform"}), "user1", "user2", "user3");
  }

  public void testGetPermissionByUser() throws Exception {
    organizationService.addMemberships("user1", "*:/platform/users");
    organizationService.addMemberships("user3", "*:/platform/users");

    assertEquals(1, ForumServiceUtils.getUserPermission(new String[] { "user1" }).size());
    AssertUtils.assertContains(ForumServiceUtils.getUserPermission(new String[] { "user1" }), "user1");

    // matching permission against a user return this user at minimum
    assertEquals(1, ForumServiceUtils.getUserPermission(new String[] { "user2" }).size());
    AssertUtils.assertContains(ForumServiceUtils.getUserPermission(new String[] { "user2" }), "user2");

    AssertUtils.assertContains(ForumServiceUtils.getUserPermission(new String[] { "user1", "user2" }), "user1", "user2");

    AssertUtils.assertNotContains(ForumServiceUtils.getUserPermission(new String[] { "user1", "user2" }), "user3");
    AssertUtils.assertContains(ForumServiceUtils.getUserPermission(new String[] { "user1", "user2" }), "user1", "user2");
  }

  public void testGetPermissionByMembership() throws Exception {
    organizationService.addMemberships("user1", "*:/platform/users");
  }

}
