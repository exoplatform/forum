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
import java.util.Date;
import java.util.Objects;

public class UserProfileData implements CachedData<UserProfile> {
  private static final long serialVersionUID = 1L;

  private String              userId;

  private String              screenName;
  
  private String              userTitle;                                             

  private long                userRole;                                              

  private String              signature              = "";

  private Date                joinedDate             = null;

  private Date                lastLoginDate          = null;

  private Date                lastPostDate           = null;

  private boolean             isDisplaySignature     = true;

  private boolean             isDisplayAvatar        = true;

  private boolean             isBanned               = false;

  private boolean             isDisabled             = false;
  
  private long                banUntil               = 0;
  
  private String              banReason;

  private int                 banCounter             = 0;

  private String[]            banReasonSummary; 
  
  private Date                createdDateBan;
  
  private Double              timeZone;

  private String              shortDateformat;

  private String              longDateformat;

  private long                maxTopic               = 10;

  private long                maxPost                = 10;
  
  private long                totalPost              = 0;
  
  private String[]            moderateForums;                                        

  private String[]            moderateCategory;

  public UserProfileData(UserProfile profile) {
    this.userId = profile.getUserId();
    this.userTitle = profile.getUserTitle();
    this.screenName = profile.getScreenName();
    this.userRole = profile.getUserRole();
    this.signature = profile.getSignature();
    this.lastLoginDate = profile.getLastLoginDate();
    this.joinedDate = profile.getJoinedDate();
    this.lastPostDate = profile.getLastPostDate();
    this.totalPost = profile.getTotalPost();
    this.isDisplaySignature = profile.getIsDisplaySignature();
    this.isDisplayAvatar = profile.getIsDisplayAvatar();
    this.timeZone = profile.getTimeZone();
    this.shortDateformat = profile.getShortDateFormat();
    this.longDateformat = profile.getLongDateFormat();
    this.moderateForums = profile.getModerateForums();
    this.moderateCategory = profile.getModerateCategory();
    this.maxTopic = profile.getMaxTopicInPage();
    this.maxPost = profile.getMaxPostInPage();
    this.isBanned = profile.getIsBanned();
    this.banUntil = profile.getBanUntil();
    this.banReason = profile.getBanReason();
    this.banCounter = profile.getBanCounter();
    this.banReasonSummary = profile.getBanReasonSummary();
    this.createdDateBan = profile.getCreatedDateBan();
    this.isDisabled = profile.isDisabled();
  }
  
  @Override
  public UserProfile build() {
    UserProfile userProfile = new UserProfile();
    userProfile.setUserId(this.userId);
    userProfile.setUserTitle(this.userTitle);
    userProfile.setScreenName(this.screenName);
    userProfile.setUserRole(this.userRole);
    userProfile.setSignature(this.signature);
    userProfile.setLastLoginDate(this.lastLoginDate);
    userProfile.setJoinedDate(this.joinedDate);
    userProfile.setLastPostDate(this.lastPostDate);
    userProfile.setTotalPost(this.totalPost);
    userProfile.setIsDisplaySignature(this.isDisplaySignature);
    userProfile.setIsDisplayAvatar(this.isDisplayAvatar);
    userProfile.setTimeZone(this.timeZone);
    userProfile.setShortDateFormat(this.shortDateformat);
    userProfile.setLongDateFormat(this.longDateformat);
    userProfile.setModerateForums(this.moderateForums);
    userProfile.setModerateCategory(this.moderateCategory);
    userProfile.setMaxPostInPage(this.maxPost);
    userProfile.setMaxTopicInPage(this.maxTopic);
    userProfile.setIsBanned(this.isBanned);
    userProfile.setBanUntil(this.banUntil);
    userProfile.setBanReason(this.banReason);
    userProfile.setBanCounter(this.banCounter);
    userProfile.setBanReasonSummary(this.banReasonSummary);
    userProfile.setCreatedDateBan(this.createdDateBan);
    userProfile.setDisabled(this.isDisabled);
    return userProfile;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    UserProfileData that = (UserProfileData) o;
    return userRole == that.userRole &&
            isDisplaySignature == that.isDisplaySignature &&
            isDisplayAvatar == that.isDisplayAvatar &&
            isBanned == that.isBanned &&
            isDisabled == that.isDisabled &&
            banUntil == that.banUntil &&
            banCounter == that.banCounter &&
            maxTopic == that.maxTopic &&
            maxPost == that.maxPost &&
            totalPost == that.totalPost &&
            Objects.equals(userId, that.userId) &&
            Objects.equals(screenName, that.screenName) &&
            Objects.equals(userTitle, that.userTitle) &&
            Objects.equals(signature, that.signature) &&
            Objects.equals(joinedDate, that.joinedDate) &&
            Objects.equals(lastLoginDate, that.lastLoginDate) &&
            Objects.equals(lastPostDate, that.lastPostDate) &&
            Objects.equals(banReason, that.banReason) &&
            Arrays.equals(banReasonSummary, that.banReasonSummary) &&
            Objects.equals(createdDateBan, that.createdDateBan) &&
            Objects.equals(timeZone, that.timeZone) &&
            Objects.equals(shortDateformat, that.shortDateformat) &&
            Objects.equals(longDateformat, that.longDateformat) &&
            Arrays.equals(moderateForums, that.moderateForums) &&
            Arrays.equals(moderateCategory, that.moderateCategory);
  }

  @Override
  public int hashCode() {
    return Objects.hash(userId, screenName, userTitle, userRole, signature, joinedDate, lastLoginDate,
            lastPostDate, isDisplaySignature, isDisplayAvatar, isBanned, isDisabled, banUntil, banReason,
            banCounter, banReasonSummary, createdDateBan, timeZone, shortDateformat, longDateformat, maxTopic, maxPost,
            totalPost, moderateForums, moderateCategory);
  }
}
