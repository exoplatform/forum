/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
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

import org.exoplatform.forum.base.BaseForumServiceTestCase;
import org.exoplatform.forum.service.cache.CachedDataStorage;
import org.exoplatform.forum.service.impl.model.UserProfileFilter;

public class UserProfileTestCase extends BaseForumServiceTestCase {
  
  CachedDataStorage cachedStorage;
  
  @Override
  public void setUp() throws Exception {
    super.setUp();
    
    cachedStorage = (CachedDataStorage) getService(DataStorage.class);
    setMembershipEntry("/platform/administrators", "*", true);
  }
  
  @Override
  public void tearDown() throws Exception {
    //
    super.tearDown();
    membershipEntries.clear();
  }

  public void testUserProfile() throws Exception {
    String userName = "tu.duy";
    UserProfile userProfile = createdUserProfile(userName);

    // save UserProfile
    forumService_.saveUserProfile(userProfile, true, true);

    // getUserInfo
    userProfile = forumService_.getUserInfo(userName);
    assertNotNull("Get info UserProfile is null", userProfile);

    // get Default and storage this profile in ExoCache
    userProfile = forumService_.getDefaultUserProfile(userName, "");
    assertNotNull("Get default UserProfile is null", userProfile);
    
    // test cache user profile, get this profile is not null
    assertNotNull("Get default UserProfile is null", cachedStorage.getDefaultUserProfile(userName, null));
    
    // getUserInformations
    userProfile = forumService_.getUserInformations(userProfile);
    assertNotNull("Get informations UserProfile is null", userProfile);

    // getUserSettingProfile
    userProfile = forumService_.getUserSettingProfile(userName);
    assertNotNull("Get Setting UserProfile is not null", userProfile);

    // saveUserSettingProfile
    assertEquals("Default AutoWatchMyTopics is false", userProfile.getIsAutoWatchMyTopics(), false);
    userProfile.setIsAutoWatchMyTopics(true);
    forumService_.saveUserSettingProfile(userProfile);
    userProfile = forumService_.getUserSettingProfile(userName);
    assertEquals("Edit AutoWatchMyTopics and can't save this property. AutoWatchMyTopics is false", userProfile.getIsAutoWatchMyTopics(), true);
  }
  
  public void testUserProfileListAccess() throws Exception {
    //
    UserProfile profile1 = createdUserProfile("username1");
    profile1.setScreenName("User " + profile1.getUserId());
    UserProfile profile2 = createdUserProfile("username2");
    profile2.setScreenName("User " + profile2.getUserId());
    forumService_.saveUserProfile(profile1, true, true);
    forumService_.saveUserProfile(profile2, true, true);
    
    //
    UserProfile[] userProfiles = forumService_.searchUserProfileByFilter(new UserProfileFilter(profile1.getUserId())).load(0, 5);
    assertEquals(1, userProfiles.length);
    //
    userProfiles = forumService_.searchUserProfileByFilter(new UserProfileFilter(profile2.getUserId())).load(0, 5);
    assertEquals(1, userProfiles.length);
    //
    userProfiles = forumService_.searchUserProfileByFilter(new UserProfileFilter("User")).load(0, 5);
    assertEquals(2, userProfiles.length);
    //not found
    userProfiles = forumService_.searchUserProfileByFilter(new UserProfileFilter("guys")).load(0, 5);
    assertEquals(0, userProfiles.length);
    
    //contains %
    userProfiles = forumService_.searchUserProfileByFilter(new UserProfileFilter("user%")).load(0, 5);
    assertEquals(2, userProfiles.length);
    
    //contains *
    userProfiles = forumService_.searchUserProfileByFilter(new UserProfileFilter("user*")).load(0, 5);
    assertEquals(2, userProfiles.length);
    
    //Get all profiles
    userProfiles = forumService_.searchUserProfileByFilter(new UserProfileFilter("")).load(0, 5);
    assertEquals(2, userProfiles.length);
    assertEquals(2, forumService_.searchUserProfileByFilter(new UserProfileFilter("")).getSize());
  }

  public void testUserLogin() throws Exception {
    String[] userIds = new String[] { USER_ROOT, USER_JOHN, USER_DEMO };
    for (int i = 0; i < userIds.length; i++) {
      forumService_.saveUserProfile(createdUserProfile(userIds[i]), true, true);
    }
    // Add user login
    loginUser(USER_ROOT);
    forumService_.userLogin(USER_ROOT);
    loginUser(USER_JOHN);
    forumService_.userLogin(USER_JOHN);
    loginUser(USER_DEMO);
    forumService_.userLogin(USER_DEMO);

    // Get all user online:
    assertEquals("Get all user online", 3, forumService_.getOnlineUsers().size());

    // isOnline
    assertEquals("John is not Online", forumService_.isOnline(USER_JOHN), true);
    // get Last Login
    assertEquals("Demo can't last Login", forumService_.getLastLogin(), USER_DEMO);
  }
  
  public void testCacheLoginUser() throws Exception {
    String[] userIds = new String[] { "user1", "user2" };
    for (int i = 0; i < userIds.length; i++) {
      forumService_.saveUserProfile(createdUserProfile(userIds[i]), true, true);
    }
    // Add user login
    loginUser("user1");
    forumService_.userLogin("user1");
    loginUser("user2");
    forumService_.userLogin("user2");
    
    UserProfile profile = cachedStorage.getDefaultUserProfile("user2", null);
    assertEquals("user2", profile.getScreenName());
  }


}
