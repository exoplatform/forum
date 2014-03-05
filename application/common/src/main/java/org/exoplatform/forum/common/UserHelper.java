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
import org.exoplatform.services.organization.impl.GroupImpl;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

/**
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
 * @version $Revision$
 */
public class UserHelper {

  public static int LIMIT_THRESHOLD = 200;
  
  public enum FILTER_TYPE {
    USER_NAME("userName"), LAST_NAME("lastName"),
    FIRST_NAME("firstName"), EMAIL("email");
    
    String name;
    FILTER_TYPE(String name) {
      this.name = name;
    }

    @Override
    public String toString() {
      return name;
    }

    public static FILTER_TYPE getType(String name) {
      for (FILTER_TYPE type : values()) {
        if (type.toString().equals(name)) {
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
      for (String user : userIds) {
        user = user.trim();
        if (user.indexOf("$") >= 0) user = user.replace("$", "&#36");

        if (user.indexOf("/") >= 0) {
          if (!UserHelper.hasGroupIdAndMembershipId(user)) {
            if (errorUser.length() == 0) errorUser.append(user);
            else errorUser.append(", ").append(user);
          }
        } else {// user
          if ((getUserHandler().findUserByName(user) == null)) {
            if (errorUser.length() == 0) errorUser.append(user);
            else errorUser.append(", ").append(user);
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

  public static boolean hasUserInGroup(String groupId, String userId) throws Exception {
    ListAccess<User> listUsers = getUserPageListByGroupId(groupId);
    for (User user : listUsers.load(0, listUsers.getSize())) {
      if (user.getUserName().equals(userId))
        return true;
    }
    return false;
  }

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
  public static boolean isDisableUser(String userName) {
    try {
      User user = getUserByUserId(userName);
      return (user == null);
    } catch (Exception e) {
      return true;
    }
  }
  
  public static String[] getUserGroups() throws Exception {
    Object[] objGroupIds = getGroupHandler().findGroupsOfUser(UserHelper.getCurrentUser()).toArray();
    String[] groupIds = new String[objGroupIds.length];
    for (int i = 0; i < groupIds.length; i++) {
      groupIds[i] = ((GroupImpl) objGroupIds[i]).getId();
    }
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
  
  public static Collection<Membership> findMembershipsByUser(String userId) {
    try {
      return getMembershipHandler().findMembershipsByUser(userId);
    } catch (Exception e) {
      return Collections.emptyList();
    }
  }

  /**
   * 
   * @param userId username
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
   * Match user by group id
   *
   * @param memberShipHandler
   * @param groupId
   * @param userName
   * @return
   * @throws Exception
   */
  public static boolean matchGroup(MembershipHandler memberShipHandler, String groupId, String userName) throws Exception {
    return memberShipHandler.findMembershipsByUserAndGroup(userName, groupId).size() > 0;
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
   * @param userFilter
   * @return
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
    if (FILTER_TYPE.USER_NAME.equals(userFilter.getFilterType()) &&
        StringUtils.containsIgnoreCase(user.getUserName(), userFilter.getKeyword())) {
      return true;
    }
    if (FILTER_TYPE.LAST_NAME.equals(userFilter.getFilterType()) &&
        StringUtils.containsIgnoreCase(user.getLastName(), userFilter.getKeyword())) {
      return true;
    }
    if (FILTER_TYPE.FIRST_NAME.equals(userFilter.getFilterType()) &&
        StringUtils.containsIgnoreCase(user.getFirstName(), userFilter.getKeyword())) {
      return true;
    }
    if (FILTER_TYPE.EMAIL.equals(userFilter.getFilterType()) &&
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
      if (FILTER_TYPE.USER_NAME.equals(userFilter.getFilterType())) {
        q.setUserName(keyword);
      }
      if (FILTER_TYPE.LAST_NAME.equals(userFilter.getFilterType())) {
        q.setLastName(keyword);
      }
      if (FILTER_TYPE.FIRST_NAME.equals(userFilter.getFilterType())) {
        q.setFirstName(keyword);
      }
      if (FILTER_TYPE.EMAIL.equals(userFilter.getFilterType())) {
        q.setEmail(keyword);
      }
    }
    return q;
  }
  
  public static class UserFilter {
    private String      keyword;
    private FILTER_TYPE filterType;
    private String      groupId;

    public UserFilter(String keyword, FILTER_TYPE filterType) {
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

    public FILTER_TYPE getFilterType() {
      return filterType;
    }

    public String getGroupId() {
      return groupId;
    }
  }

}
