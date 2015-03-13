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
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.service.cache.CachedDataStorage;
import org.exoplatform.forum.service.impl.model.UserProfileFilter;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;

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
    // create user profile
    String userName = "user_bar";
    UserHandler userHandler = UserHelper.getUserHandler();
    User user = userHandler.createUserInstance(userName);
    user.setEmail("user_bar@plf.com");
    user.setFirstName(userName);
    user.setLastName(userName);
    user.setPassword("exo");
    //
    userHandler.createUser(user, true);

    // getUserInfo
    UserProfile userProfile = forumService_.getUserInfo(userName);
    assertNotNull("Get info UserProfile is null", userProfile);

    // get Default and storage this profile in ExoCache
    userProfile = forumService_.getDefaultUserProfile(userName, "");
    assertNotNull("Get default UserProfile is null", userProfile);

    // test cache user profile, get this profile is not null
    assertNotNull("Get default UserProfile is null", cachedStorage.getDefaultUserProfile(userName, null));
    
    // remove this profile in ExoCache by update this profile
    forumService_.saveUserSettingProfile(userProfile);

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

  public void testSavePostCount() throws Exception {
    // set Data
    initDefaultData();
    //
    String userGhost = "ghost";
    forumService_.saveUserProfile(createdUserProfile(userGhost), false, false);
    //
    UserProfile profile = cachedStorage.getQuickProfile(userGhost);
    assertNotNull(profile);
    assertEquals(0, profile.getTotalPost());
    Post newPost = createdPost();
    newPost.setOwner("ghost");
    forumService_.savePost(categoryId, forumId, topicId, newPost, true, new MessageBuilder());
    // Save last read of user ghost
    cachedStorage.saveLastPostIdRead(userGhost, new String[]{forumId + "/" + newPost.getId()},
                                     new String[]{topicId + "/" + newPost.getId()});
    //
    profile = cachedStorage.getQuickProfile(userGhost);
    assertEquals(1, profile.getTotalPost());
  }

  public void testSaveModerator() throws Exception {
    //
    initDefaultData();
    //
    String userGhost = "ghost";
    UserProfile userProfile = createdUserProfile(userGhost);
    userProfile.setUserRole(2l);
    userProfile.setUserTitle(Utils.USER);
    forumService_.saveUserProfile(userProfile, false, false);
    UserProfile profile = cachedStorage.getQuickProfile(userGhost);
    assertNotNull(profile);
    assertEquals(2, profile.getUserRole());

    // Save moderator on forum for user ghost
    Forum forum = forumService_.getForum(categoryId, forumId);
    forum.setModerators(new String[]{userGhost});
    forumService_.saveForum(categoryId, forum, false);
    //Save last read of user ghost
    cachedStorage.saveLastPostIdRead(userGhost, new String[]{forumId + "/postId"},
                                     new String[]{topicId + "/postId"});
    // Check role of user ghost
    profile = cachedStorage.getQuickProfile(userGhost);
    assertEquals(1, profile.getUserRole());
    assertEquals(Utils.MODERATOR, profile.getUserTitle());

    // Remove moderator user ghost 
    forum.setModerators(new String[]{});
    forumService_.saveForum(categoryId, forum, false);
    // Check again role of user ghost
    profile = cachedStorage.getQuickProfile(userGhost);
    assertEquals(2, profile.getUserRole());

    // Save moderator on category for user ghost
    Category cate = forumService_.getCategory(categoryId);
    cate.setModerators(new String[]{userGhost});
    forumService_.saveCategory(cate, false);
    //Save last read of user ghost
    cachedStorage.saveLastPostIdRead(userGhost, new String[]{forumId + "/postId"},
                                     new String[]{topicId + "/postId"});
    //
    profile = cachedStorage.getQuickProfile(userGhost);
    assertEquals(1, profile.getUserRole());
    assertEquals(Utils.MODERATOR, profile.getUserTitle());
  }

  public void testSearchUserProfile() throws Exception {
    
    int before = forumService_.getPageListUserProfile().getAvailable();
    //
    int numberUser = 20;
    // create user
    UserHandler userHandler = UserHelper.getUserHandler();
    for (int i = 0; i < numberUser; i++) {
      User user = userHandler.createUserInstance("foo" + i);
      user.setEmail("foo" + i + "@plf.com");
      user.setFirstName("test");
      user.setLastName("abc user");
      user.setPassword("exo");
      user.isEnabled();
      //
      userHandler.createUser(user, true);
      userHandler.setEnabled(user.getUserName(), true, true);
    }

    int allSize = before + numberUser;
    // check get all user profile (8 users create by OrganizationDatabaseInitializer)
    assertEquals(allSize, forumService_.searchUserProfileByFilter(new UserProfileFilter(null)).getSize());
    assertEquals(20, forumService_.searchUserProfileByFilter(new UserProfileFilter("foo")).getSize());
    assertEquals(false, forumService_.getQuickProfile("foo0").isDisabled());
    // Disable 10 users
    System.out.println("Disable 10 users");
    for (int i = 0; i < 10; i++) {
      userHandler.setEnabled("foo" + i, false, true);
    }
    //
    assertEquals(allSize - 10, forumService_.searchUserProfileByFilter(new UserProfileFilter(null)).getSize());
    assertEquals(10, forumService_.searchUserProfileByFilter(new UserProfileFilter("foo")).getSize());
    assertEquals(true, forumService_.getQuickProfile("foo0").isDisabled());
    // Enable 10 users
    for (int i = 0; i < 10; i++) {
      userHandler.setEnabled("foo" + i, true, true);
    }
    //
    assertEquals(allSize, forumService_.searchUserProfileByFilter(new UserProfileFilter(null)).getSize());
    assertEquals(20, forumService_.searchUserProfileByFilter(new UserProfileFilter("foo")).getSize());
    assertEquals(false, forumService_.getQuickProfile("foo0").isDisabled());
  }

  public void testAccessTopic() throws Exception{
    //
    initDefaultData();
    //
    String userTest = "test_user";
    UserProfile userProfile = createdUserProfile(userTest);
    userProfile.setUserRole(2l);
    userProfile.setUserTitle(Utils.USER);
    forumService_.saveUserProfile(userProfile, false, false);
    //
    userProfile = cachedStorage.getDefaultUserProfile(userTest, "");
    assertEquals(0, userProfile.getLastTimeAccessTopic(topicId));
    //
    long timeBeforeRead = CommonUtils.getGreenwichMeanTime().getTimeInMillis();
    forumService_.updateTopicAccess(userTest, topicId);
    forumService_.writeReads();
    //
    userProfile = cachedStorage.getDefaultUserProfile(userTest, "");
    //
    assertTrue(userProfile.getLastTimeAccessTopic(topicId) >= timeBeforeRead);
  }

}
