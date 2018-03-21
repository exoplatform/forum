/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package org.exoplatform.forum.service.cache.model.data;

import org.exoplatform.forum.common.cache.model.CachedData;
import org.exoplatform.forum.service.UserProfile;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class LoginUserProfileData implements CachedData<UserProfile> {
  private static final long serialVersionUID = 1L;

  private String              userId;

  private String              screenName;
  
  private long                userRole;                                              

  // UserBan
  private boolean             isBanned               = false;
  
  private long                banUntil               = 0;

  private String              email                  = "";

  // UserOption
  private String              timeFormat;

  private Double              timeZone;

  private String              shortDateformat;

  private String              longDateformat;

  private long                maxTopic               = 10;

  private long                maxPost                = 10;
  
  private boolean             isAutoWatchMyTopics    = false;

  private boolean             isAutoWatchTopicIPost  = false;
  
  private String[]            lastReadPostOfTopic;

  private String[]            lastReadPostOfForum;

  private String[]            collapCategories;

  private String[]            moderateForums;                                        

  private String[]            moderateCategory;
  
  private long                newMessage             = 0;
  
  private Map<String, Long>   lastAccessTopics       = new HashMap<String, Long>();

  private Map<String, Long>   lastAccessForums       = new HashMap<String, Long>();

  public LoginUserProfileData(UserProfile profile) {
    this.userId = profile.getUserId();
    this.screenName = profile.getScreenName();
    this.userRole = profile.getUserRole();
    this.isBanned = profile.getIsBanned();
    this.banUntil = profile.getBanUntil();
    this.timeFormat = profile.getTimeFormat();
    this.timeZone = profile.getTimeZone();
    this.shortDateformat = profile.getShortDateFormat();
    this.longDateformat = profile.getLongDateFormat();
    this.moderateForums = profile.getModerateForums();
    this.moderateCategory = profile.getModerateCategory();
    this.maxTopic = profile.getMaxTopicInPage();
    this.maxPost = profile.getMaxPostInPage();
    this.newMessage = profile.getNewMessage();
    this.isAutoWatchMyTopics = profile.getIsAutoWatchMyTopics();
    this.isAutoWatchTopicIPost = profile.getIsAutoWatchTopicIPost();
    this.lastReadPostOfForum = profile.getLastReadPostOfForum();
    this.lastReadPostOfTopic = profile.getLastReadPostOfTopic();
    this.collapCategories = profile.getCollapCategories();
    this.email = profile.getEmail();
    this.lastAccessTopics = profile.getLastAccessTopics();
    this.lastAccessForums = profile.getLastAccessForums();
  }
  
  @Override
  public UserProfile build() {
    UserProfile userProfile = new UserProfile();
    userProfile.setUserId(this.userId);
    userProfile.setScreenName(this.screenName);
    userProfile.setUserRole(this.userRole);
    userProfile.setIsBanned(this.isBanned);
    userProfile.setBanUntil(this.banUntil);
    userProfile.setTimeFormat(this.timeFormat);
    userProfile.setTimeZone(this.timeZone);
    userProfile.setShortDateFormat(this.shortDateformat);
    userProfile.setLongDateFormat(this.longDateformat);
    userProfile.setModerateForums(this.moderateForums);
    userProfile.setModerateCategory(this.moderateCategory);
    userProfile.setMaxPostInPage(this.maxPost);
    userProfile.setMaxTopicInPage(this.maxTopic);
    userProfile.setNewMessage(this.newMessage);
    userProfile.setIsAutoWatchMyTopics(this.isAutoWatchMyTopics);
    userProfile.setIsAutoWatchTopicIPost(this.isAutoWatchTopicIPost);
    userProfile.setLastReadPostOfForum(this.lastReadPostOfForum);
    userProfile.setLastReadPostOfTopic(this.lastReadPostOfTopic);
    userProfile.setCollapCategories(this.collapCategories);
    userProfile.setEmail(this.email);
    userProfile.setLastAccessTopics(this.lastAccessTopics);
    userProfile.setLastAccessForums(this.lastAccessForums);
    return userProfile;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    LoginUserProfileData that = (LoginUserProfileData) o;
    return userRole == that.userRole &&
            isBanned == that.isBanned &&
            banUntil == that.banUntil &&
            maxTopic == that.maxTopic &&
            maxPost == that.maxPost &&
            isAutoWatchMyTopics == that.isAutoWatchMyTopics &&
            isAutoWatchTopicIPost == that.isAutoWatchTopicIPost &&
            newMessage == that.newMessage &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(screenName, that.screenName) &&
            Objects.equals(email, that.email) &&
            Objects.equals(timeFormat, that.timeFormat) &&
            Objects.equals(timeZone, that.timeZone) &&
            Objects.equals(shortDateformat, that.shortDateformat) &&
            Objects.equals(longDateformat, that.longDateformat) &&
            Arrays.equals(lastReadPostOfTopic, that.lastReadPostOfTopic) &&
            Arrays.equals(lastReadPostOfForum, that.lastReadPostOfForum) &&
            Arrays.equals(collapCategories, that.collapCategories) &&
            Arrays.equals(moderateForums, that.moderateForums) &&
            Arrays.equals(moderateCategory, that.moderateCategory) &&
            Objects.equals(lastAccessTopics, that.lastAccessTopics) &&
            Objects.equals(lastAccessForums, that.lastAccessForums);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, screenName, userRole, isBanned, banUntil, email,
            timeFormat, timeZone, shortDateformat, longDateformat, maxTopic, maxPost, isAutoWatchMyTopics,
            isAutoWatchTopicIPost, lastReadPostOfTopic, lastReadPostOfForum, collapCategories, moderateForums,
            moderateCategory, newMessage, lastAccessTopics, lastAccessForums);
  }
}
