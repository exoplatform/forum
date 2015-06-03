/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.commons.utils.ListAccessImpl;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.organization.Group;
import org.exoplatform.services.organization.GroupHandler;
import org.exoplatform.services.organization.Membership;
import org.exoplatform.services.organization.MembershipHandler;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.organization.UserHandler;
import org.exoplatform.services.organization.UserStatus;
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

/**
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
 * @version $Revision$
 */
public class UserHelper {

  /**
   *  The value to compile between tow logic to filter users by key and groupId.
   *  + If all users of group less than @LIMIT_THRESHOLD we will get all users of group
   *    and match this list users with filter key.
   *  + Else we will get all users by filter key and match this list users with group filter.
   */
  private static final int LIMIT_THRESHOLD = 200;
  
  public enum FilterType {
    USER_NAME("userName"), LAST_NAME("lastName"),
    FIRST_NAME("firstName"), EMAIL("email");
    
    String name;
    FilterType(String name) {
      this.name = name;
    }

    public String getName() {
      return name;
    }

    public static FilterType getType(String name) {
      for (FilterType type : values()) {
        if (type.getName().equalsIgnoreCase(name)) {
          return type;
        }
      }
      return null;
    }
  }

  public static OrganizationService getOrganizationService() {
    return CommonUtils.getComponent(OrganizationService.class);
  }
  
  public static UserHandler getUserHandler() {
    return getOrganizationService().getUserHandler();
  }
  
  public static GroupHandler getGroupHandler() {
    return getOrganizationService().getGroupHandler();
  }

  public static MembershipHandler getMembershipHandler() {
    return getOrganizationService().getMembershipHandler();
  }

  public static List<Group> getAllGroup() throws Exception {
    return Collections.unmodifiableList((List<Group>) getGroupHandler().getAllGroups());
  }

  public static String checkValueUser(String values) throws Exception {
    StringBuilder errorUser = new StringBuilder();
    if (values != null && values.trim().length() > 0) {
      String[] userIds = values.split(",");
      for (String str : userIds) {
        str = str.trim();
        if (str.indexOf("$") >= 0) str = str.replace("$", "&#36");

        if (str.indexOf("/") >= 0) {
          if (!UserHelper.hasGroupIdAndMembershipId(str)) {
            if (errorUser.length() == 0) errorUser.append(str);
            else errorUser.append(", ").append(str);
          }
        } else {// user
          if ((getUserHandler().findUserByName(str, UserStatus.ENABLED) == null)) {
            if (errorUser.length() == 0) errorUser.append(str);
            else errorUser.append(", ").append(str);
          }
        }
      }
    }
    return errorUser.toString();
  }

  public static boolean hasGroupIdAndMembershipId(String str) throws Exception {
    if(str.indexOf(":") >= 0) { //membership
      String[] array = str.split(":") ;
      try {
        getGroupHandler().findGroupById(array[1]).getId() ;
      }catch (Exception e) {
        return false ;
      }
      if(array[0].length() == 1 && array[0].charAt(0) == '*') {
        return true ;
      }else if(array[0].length() > 0){
        if(getOrganizationService().getMembershipTypeHandler().findMembershipType(array[0])== null) return false ;
      }else return false ;
    }else { //group
      try {
        getGroupHandler().findGroupById(str).getId() ;
      }catch (Exception e) {
        return false ;
      }
    }
    return true ;
  }

  /**
   * Check user in group
   * 
   * @param groupId The group to check
   * @param userId The user's id that to check
   * @return
   * @throws Exception
   */
  public static boolean hasUserInGroup(String groupId, String userId) {
    try {
      return matchGroup(getMembershipHandler(), groupId, userId);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Get all users on group by groupId
   * 
   * @param groupId The group's id
   * @return ListAccess of Users.
   * @throws Exception
   */
  public static ListAccess<User> getUserPageListByGroupId(String groupId) throws Exception {
    return getUserHandler().findUsersByGroupId(groupId) ;
  }
  
  public static User getUserByUserId(String userId) throws Exception {
    return getUserHandler().findUserByName(userId) ;
  }
  
  /**
   * Check user disable on system or not.
   *  
   * @param userName The user name 
   * @return
   */
  public static boolean isDisabledUser(String userName) {
    try {
      User user = getUserByUserId(userName);
      return (user == null || !user.isEnabled());
    } catch (Exception e) {
      return true;
    }
  }
  
  public static String[] getUserGroups() throws Exception {
    ConversationState state = ConversationState.getCurrent();
    Set<String> groups = state.getIdentity().getGroups();
    String[] groupIds = groups.toArray(new String[groups.size()]);
    return groupIds;
  }

  public static List<String> getAllGroupId() throws Exception {
    List<String> grIds = new ArrayList<String>();
    for (Group gr : getAllGroup()) {
      grIds.add(gr.getId());
    }
    return grIds;
  }

  
  public static List<Group> findGroups(Group group) throws Exception {
    return (List<Group>) getGroupHandler().findGroups(group);
  }
  
  public static boolean isAnonim() {
    String userId = UserHelper.getCurrentUser();
    if (userId == null)
      return true;
    return false;
  }
  
  /**
   * Get all memberships of user by userId
   * 
   * @param userId The user's id
   * @return list of memberships
   */
  public static Collection<Membership> findMembershipsByUser(String userId) {
    try {
      return getMembershipHandler().findMembershipsByUser(userId);
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  /**
   * 
   * @param userId userame
   * @return list of groups an user belong, and memberships of the user in each group. If userId is null, groups and memberships of the current
   * user will be returned.
   * @throws Exception
   */
  public static List<String> getAllGroupAndMembershipOfUser(String userId) {
    List<String> listOfUser = new ArrayList<String>();
    if (userId == null || userId.equals(getCurrentUser())) {
      ConversationState conversionState = ConversationState.getCurrent();
      Identity identity = conversionState.getIdentity();
      userId = identity.getUserId();
      if (userId != null) {
        listOfUser.add(userId);
        for (MembershipEntry membership : identity.getMemberships()) {
          listOfUser.add(membership.getGroup()); // its groups
          listOfUser.add(membership.getMembershipType() + ":" + membership.getGroup()); // its memberships
        }
      }
    } else {
      listOfUser.add(userId); // himself
      Collection<Membership> memberships = findMembershipsByUser(userId);
      for (Membership membership : memberships) {
        listOfUser.add(membership.getGroupId()); // its groups
        listOfUser.add(membership.getMembershipType() + ":" + membership.getGroupId()); // its memberships
      }
    }
    return listOfUser;
  }

  static public String getEmailUser(String userName) throws Exception {
    User user = getUserHandler().findUserByName(userName) ;
    String email = user.getEmail() ;
    return email;
  }

  static public String getCurrentUser() {
    try {
      return Util.getPortalRequestContext().getRemoteUser();
    } catch (Exception e) {
      return null;
    }
  }
  
  /**
   * Get the display name of user or group or membership
   * 
   * @param owner The id of user or group or membership
   * @return The String value
   * @throws Exception
   */
  public static String getDisplayNameOfOwner(String owner) throws Exception {
    if (CommonUtils.isEmpty(owner) == true) {
      return CommonUtils.EMPTY_STR;
    }

    if (hasGroupIdAndMembershipId(owner)) {
      if (owner.contains(CommonUtils.COLON)) {
        String membership = owner.substring(0, owner.indexOf(CommonUtils.COLON));
        String groupId = owner.substring(membership.length() + 1);
        Group group = getGroupHandler().findGroupById(groupId);
        if (group != null) {
          return membership + " in " + group.getGroupName();
        }
      }
      Group group = getGroupHandler().findGroupById(owner);
      return (group != null) ? group.getGroupName() : CommonUtils.EMPTY_STR;
    } else {
      User user = getUserHandler().findUserByName(owner, UserStatus.ANY);
      if (user != null) {
        String displayName = user.getDisplayName();
        if (CommonUtils.isEmpty(displayName) || owner.equals(displayName)) {
          displayName = user.getFirstName() + CommonUtils.SPACE + user.getLastName();
        }
        return displayName;
      }
    }
    return CommonUtils.EMPTY_STR;
  }

  /**
   * Match user by group id
   *
   * @param memberShipHandler The MemberShipHandler
   * @param groupId The group's id
   * @param userId The user's id
   * @return
   * @throws Exception
   */
  private static boolean matchGroup(MembershipHandler memberShipHandler, String groupId, String userId) throws Exception {
    return memberShipHandler.findMembershipsByUserAndGroup(userId, groupId).size() > 0;
  }

  /**
   * Search users by query ignore case in group
   * 
   * @param userFilter The user filter
   * @return
   * @throws Exception
   */
  private static ListAccess<User> searchUserByQuery(UserFilter userFilter) throws Exception {
    Query q = queryFilter(userFilter);
    if (q.isEmpty()) {
      return getUserHandler().findAllUsers();
    }
    //
    return getUserHandler().findUsersByQuery(q);
  }

  /**
   * Search user by query
   * 
   * @param userFilter The UserFilter
   * @return The result is ListAccess of Users
   * @throws Exception
   */
  public static ListAccess<User> searchUser(UserFilter userFilter) throws Exception {
    ListAccess<User> listUsers = null;
    //
    String groupId = userFilter.getGroupId();

    if (!CommonUtils.isEmpty(groupId)) {
      listUsers = getUserHandler().findUsersByGroupId(groupId);
      //
      if (CommonUtils.isEmpty(userFilter.getKeyword())) {
        return listUsers;
      }
      //
      List<User> results = new ArrayList<User>();
      if (listUsers.getSize() > LIMIT_THRESHOLD) {
        // search users by query
        listUsers = searchUserByQuery(userFilter);
        // filter user if user doesn't exist in group
        results = filter(userFilter, listUsers, true);
      } else {
        // filter user match by userFilter
        results = filter(userFilter, listUsers, false);
      }
      listUsers = new ListAccessImpl<User>(User.class, results);
    } else {
      //
      listUsers = searchUserByQuery(userFilter);
    }
    return listUsers;
  }
  
  /**
   * Filter users on ListAccess result 
   * 
   * @param userFilter The user filter
   * @param listUsers The result before filter
   * @param matchGroup Is match group or match user
   * @return
   * @throws Exception
   */
  public static List<User> filter(UserFilter userFilter, ListAccess<User> listUsers, boolean matchGroup) throws Exception {
    List<User> results = new ArrayList<User>();
    MembershipHandler memberShipHandler = getMembershipHandler();
    for (User user : listUsers.load(0, listUsers.getSize())) {
      if (matchGroup && matchGroup(memberShipHandler, userFilter.getGroupId(), user.getUserName()) ||
          !matchGroup && matchUser(userFilter, user)) {
        results.add(user);
      }
    }
    return results;
  }

  /**
   * Match user with user filter
   * 
   * @param userFilter The user filter
   * @param user the user
   * @return
   */
  public static boolean matchUser(UserFilter userFilter, User user) {
    if (user == null) {
      return false;
    }
    if (FilterType.USER_NAME == userFilter.getFilterType() &&
        StringUtils.containsIgnoreCase(user.getUserName(), userFilter.getKeyword())) {
      return true;
    }
    if (FilterType.LAST_NAME == userFilter.getFilterType() &&
        StringUtils.containsIgnoreCase(user.getLastName(), userFilter.getKeyword())) {
      return true;
    }
    if (FilterType.FIRST_NAME == userFilter.getFilterType() &&
        StringUtils.containsIgnoreCase(user.getFirstName(), userFilter.getKeyword())) {
      return true;
    }
    if (FilterType.EMAIL == userFilter.getFilterType() &&
        StringUtils.containsIgnoreCase(user.getEmail(), userFilter.getKeyword())) {
      return true;
    }
    return false;
  }

  /**
   * Build query filter for search user
   * 
   * @param userFilter
   * @return
   */
  public static Query queryFilter(UserFilter userFilter) {
    Query q = new Query();
    String keyword = userFilter.getKeyword();
    if (!CommonUtils.isEmpty(keyword) && userFilter.getFilterType() != null) {
      if (keyword.indexOf("*") < 0) {
        if (keyword.charAt(0) != '*')
          keyword = "*" + keyword;
        if (keyword.charAt(keyword.length() - 1) != '*')
          keyword += "*";
      }
      keyword = keyword.replace('?', '_');
      if (FilterType.USER_NAME == userFilter.getFilterType()) {
        q.setUserName(keyword);
      }
      if (FilterType.LAST_NAME == userFilter.getFilterType()) {
        q.setLastName(keyword);
      }
      if (FilterType.FIRST_NAME == userFilter.getFilterType()) {
        q.setFirstName(keyword);
      }
      if (FilterType.EMAIL == userFilter.getFilterType()) {
        q.setEmail(keyword);
      }
    }
    return q;
  }
  
  public static class UserFilter {
    private String      keyword;
    private FilterType filterType;
    private String      groupId;

    public UserFilter(String keyword, FilterType filterType) {
      this.keyword = keyword;
      this.filterType = filterType;
    }

    public UserFilter setGroupId(String groupId) {
      this.groupId = groupId;
      return this;
    }

    public String getKeyword() {
      return keyword;
    }

    public FilterType getFilterType() {
      return filterType;
    }

    public String getGroupId() {
      return groupId;
    }
  }

}
