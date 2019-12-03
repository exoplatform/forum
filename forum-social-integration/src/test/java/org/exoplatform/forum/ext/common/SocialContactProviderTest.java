package org.exoplatform.forum.ext.common;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.commons.testing.BaseExoTestCase;
import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.forum.common.user.CommonContact;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserProfile;

@ConfiguredBy({ @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.core.test.configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.social.component.search.test.configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/test-portal-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.forum.component.core.test.configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.forum.test.jcr-configuration.xml"),
    @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.forum.test.portal-configuration.xml") })

public class SocialContactProviderTest extends BaseExoTestCase {
  private final Log             LOG = ExoLogger.getLogger(SocialContactProviderTest.class);

  private OrganizationService   organizationService;

  private SocialContactProvider socialContactProvider;

  private final static String USERNAME = "lionel";

  public void setUp() throws Exception {
    super.setUp();
    organizationService = getContainer().getComponentInstanceOfType(OrganizationService.class);
    socialContactProvider = getContainer().getComponentInstanceOfType(SocialContactProvider.class);
    begin();
    UserHandler userHandler = organizationService.getUserHandler();
    User userlionel = userHandler.createUserInstance(USERNAME);
    userHandler.createUser(userlionel, true);
    userlionel.setFirstName("lionel");
    userlionel.setLastName("messi");
    userHandler.saveUser(userlionel, true);
  }

  public void tearDown() throws Exception {
    organizationService.getUserProfileHandler().removeUserProfile(USERNAME, true);
    organizationService.getUserHandler().removeUser(USERNAME, true);
    end();
  }

  /**
   * This testcase what will use for unit testing with scenario to check the
   * existence of phone numbers when phones Added
   * 
   * @throws Exception
   */
  public void testGetContactWhenPhonesAdded() throws Exception {
    // Given
    setDataUserProfile("223232560", "223232569", "223232579", null);

    // When
    CommonContact contact = socialContactProvider.getCommonContact(USERNAME);

    // Then
    assertNotNull(contact);
    assertEquals("223232579", contact.getWorkPhone());
    assertEquals("223232560, 223232569", contact.getHomePhone());
  }
  
  /**
   * This testcase what will use for unit testing with scenario to check the
   * existence of phone numbers when no phones added
   * 
   * @throws Exception
   */
  public void testGetContactWhenNoPhone() throws Exception {
    // Given
    setDataUserProfile("", "", "", "");

    // When
    CommonContact contact = socialContactProvider.getCommonContact(USERNAME);

    // Then
    assertNotNull(contact);
    assertEquals("", contact.getWorkPhone());
    assertEquals("", contact.getHomePhone());
  }

  /**
   * @param
   * @return
   * @throws Exception
   */
  private UserProfile setDataUserProfile(String homePhone, String homeMobile, String workPhone, String workMobile) throws Exception {
    UserProfile userProfile = organizationService.getUserProfileHandler().findUserProfileByName(USERNAME);
    if (userProfile == null) {
      userProfile = organizationService.getUserProfileHandler().createUserProfileInstance(USERNAME);
    }
    // HOME_PHONES
    userProfile.setAttribute("user.home-info.telecom.telephone.number", homePhone);
    userProfile.setAttribute("user.home-info.telecom.mobile.number", homeMobile);
    // WORK_PHONES
    userProfile.setAttribute("user.business-info.telecom.telephone.number", workPhone);
    userProfile.setAttribute("user.business-info.telecom.mobile.number", workMobile);

    organizationService.getUserProfileHandler().saveUserProfile(userProfile, true);

    return userProfile;
  }

}
