/***************************************************************************
 * Copyright (C) 2003-2007 eXo Platform SAS.
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
 ***************************************************************************/
package org.exoplatform.forum.webui.popup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.exoplatform.commons.utils.ListAccess;
import org.exoplatform.forum.ForumUtils;
import org.exoplatform.forum.TimeConvertUtils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.TransformHTML;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIPopupContainer;
import org.exoplatform.forum.service.Category;
import org.exoplatform.forum.service.Forum;
import org.exoplatform.forum.service.ForumLinkData;
import org.exoplatform.forum.service.ForumPageList;
import org.exoplatform.forum.service.JCRPageList;
import org.exoplatform.forum.service.UserProfile;
import org.exoplatform.forum.service.Utils;
import org.exoplatform.forum.webui.BaseForumForm;
import org.exoplatform.forum.webui.UIForumLinks;
import org.exoplatform.forum.webui.UIForumPageIterator;
import org.exoplatform.forum.webui.UIForumPortlet;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.Query;
import org.exoplatform.services.organization.User;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.UIPopupWindow;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.Event.Phase;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.UIFormInputWithActions.ActionData;
import org.exoplatform.webui.form.UIFormSelectBox;
import org.exoplatform.webui.form.UIFormStringInput;
import org.exoplatform.webui.form.UIFormTextAreaInput;
import org.exoplatform.webui.form.input.UICheckBoxInput;
import org.exoplatform.webui.form.validator.UserConfigurableValidator;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class,
    template = "app:/templates/forum/webui/popup/UIModeratorManagementForm.gtmpl",
    events = {
      @EventConfig(listeners = UIModeratorManagementForm.SearchUserActionListener.class), 
      @EventConfig(listeners = UIModeratorManagementForm.GetAllUserActionListener.class, phase=Phase.DECODE), 
      @EventConfig(listeners = UIModeratorManagementForm.ViewProfileActionListener.class, phase=Phase.DECODE), 
      @EventConfig(listeners = UIModeratorManagementForm.EditProfileActionListener.class, phase=Phase.DECODE), 
      @EventConfig(listeners = UIModeratorManagementForm.SaveActionListener.class, phase=Phase.DECODE), 
      @EventConfig(listeners = UIModeratorManagementForm.AddValuesModForumActionListener.class, phase=Phase.DECODE), 
      @EventConfig(listeners = UIModeratorManagementForm.AddValuesModCategoryActionListener.class, phase=Phase.DECODE), 
      @EventConfig(listeners = UIModeratorManagementForm.CloseActionListener.class, phase=Phase.DECODE),
      @EventConfig(listeners = UIModeratorManagementForm.CancelActionListener.class, phase=Phase.DECODE)
    }
)
public class UIModeratorManagementForm extends BaseForumForm implements UIPopupComponent {
  private List<UserProfile>   userProfiles                       = new ArrayList<UserProfile>();
  
  public UserProfile          editUserProfile                    = null;

  private String[]            permissionUser                     = null;

  private String[]            titleUser                          = null;

  protected JCRPageList       userPageList;

  private boolean             isEdit                             = false;

  private List<ForumLinkData> forumLinks                         = null;

  private List<String>        listModerate                       = new ArrayList<String>();

  private List<String>        listModCate                        = new ArrayList<String>();

  public static final String  FIELD_USERPROFILE_FORM             = "ForumUserProfile";

  public static final String  FIELD_USEROPTION_FORM              = "ForumUserOption";

  public static final String  FIELD_USERBAN_FORM                 = "ForumUserBan";

  public static final String  FIELD_USERID_INPUT                 = "ForumUserName";

  public static final String  FIELD_SCREENNAME_INPUT             = "ScreenName";

  public static final String  FIELD_USERTITLE_INPUT              = "ForumUserTitle";

  public static final String  FIELD_USERROLE_CHECKBOX            = "isAdmin";

  public static final String  FIELD_SIGNATURE_TEXTAREA           = "Signature";

  public static final String  FIELD_ISDISPLAYSIGNATURE_CHECKBOX  = "IsDisplaySignature";

  public static final String  FIELD_MODERATECATEGORYS_MULTIVALUE = "ModCategorys";

  public static final String  FIELD_MODERATEFORUMS_MULTIVALUE    = "ModForums";

  public static final String  FIELD_MODERATETOPICS_MULTIVALUE    = "MosTopics";

  public static final String  FIELD_ISDISPLAYAVATAR_CHECKBOX     = "IsDisplayAvatar";

  public static final String  FIELD_TIMEZONE_SELECTBOX           = "TimeZone";

  public static final String  FIELD_SHORTDATEFORMAT_SELECTBOX    = "ShortDateformat";

  public static final String  FIELD_LONGDATEFORMAT_SELECTBOX     = "LongDateformat";

  public static final String  FIELD_TIMEFORMAT_SELECTBOX         = "Timeformat";

  public static final String  FIELD_MAXTOPICS_SELECTBOX          = "MaximumThreads";

  public static final String  FIELD_MAXPOSTS_SELECTBOX           = "MaximumPosts";

  public static final String  FIELD_TIMEZONE                     = "timeZone";

  public static final String  FIELD_ISBANNED_CHECKBOX            = "IsBanned";

  public static final String  FIELD_BANUNTIL_SELECTBOX           = "BanUntil";

  public static final String  FIELD_BANREASON_TEXTAREA           = "BanReason";

  public static final String  FIELD_BANCOUNTER_INPUT             = "BanCounter";

  public static final String  FIELD_BANREASONSUMMARY_MULTIVALUE  = "BanReasonSummary";

  public static final String  FIELD_CREATEDDATEBAN_INPUT         = "CreatedDateBan";

  public static final String  FIELD_SEARCH_USER                  = "SearchUser";

  private String              valueSearch                        = null;

  private String              keyWord                            = ForumUtils.EMPTY_STR;

  private boolean             isViewSearchUser                   = false;

  private UIForumPageIterator pageIterator                       = null;
  
  public UIModeratorManagementForm() throws Exception {
    pageIterator = addChild(UIForumPageIterator.class, null, "ForumUserPageIterator");
    
    addUIFormInput(new UIFormStringInput(FIELD_SEARCH_USER, FIELD_SEARCH_USER, null)
        .addValidator(UserConfigurableValidator.class, UserConfigurableValidator.USERNAME));

    titleUser = new String[] { i18n("UIForumPortlet.label.PermissionAdmin"), i18n("UIForumPortlet.label.PermissionModerator"), 
                               i18n("UIForumPortlet.label.PermissionUser"), i18n("UIForumPortlet.label.PermissionGuest") };
    permissionUser = new String[titleUser.length];
    for (int i = 0; i < titleUser.length; i++) {
      permissionUser[i] = titleUser[i].toLowerCase();
    }
    setAddColonInLabel(true);
  }

  protected void initPlaceholder() throws Exception {
    ((UIFormStringInput)getChildById(FIELD_SEARCH_USER)).setHTMLAttribute("placeholder", i18n("UIModeratorManagementForm.label.Search"));
  }
  
  public void setValueSearch(String value) {
    this.valueSearch = value;
  }

  public void setPageListUserProfile() throws Exception {
    userPageList = this.getForumService().getPageListUserProfile();
    userPageList.setPageSize(5);
    this.pageIterator.updatePageList(this.userPageList);
  }

  private boolean isAdmin(String userId) throws Exception {
    return getForumService().isAdminRoleConfig(userId);
  }

  protected boolean getIsBanned(UserProfile userProfile) throws Exception {
    if (userProfile.getBanUntil() > 0) {
      Calendar calendar = CommonUtils.getGreenwichMeanTime();
      if (calendar.getTimeInMillis() >= userProfile.getBanUntil()) {
        userProfile.setIsBanned(false);
        return false;
      }
    }
    return true;
  }

  @SuppressWarnings("unchecked")
  private void setListUserProfile() throws Exception {
    this.userProfiles =  new CopyOnWriteArrayList<UserProfile>();
    if (valueSearch == null || valueSearch.trim().length() < 1) {
      int page = pageIterator.getPageSelected();
      this.userProfiles.addAll(this.userPageList.getPage(page));
      pageIterator.setSelectPage(userPageList.getCurrentPage());
    } else {
      this.userProfiles.addAll(this.userPageList.getpage(this.valueSearch));
      pageIterator.setSelectPage(this.userPageList.getCurrentPage());
      valueSearch = null;
    }
  }

  protected List<UserProfile> getListProFileUser() throws Exception {
    if (!isViewSearchUser) {
      this.setListUserProfile();
    } else {
      try {
        int page = pageIterator.getPageSelected();
        this.userProfiles = new ArrayList<UserProfile>();
        List list = this.userPageList.getPageUser(page);
        for (Object obj : list) {
          if (obj instanceof User)
            this.userProfiles.add(getForumService().getUserProfileManagement(((User) obj).getUserName()));
          else if (obj instanceof UserProfile)
            this.userProfiles.add((UserProfile) obj);
        }
      } catch (Exception e) {
        log.debug("Failed to get user info to list of userProfiles.", e);
        this.setListUserProfile();
      }
    }
    if (userProfiles == null){
      return new ArrayList<UserProfile>();
    }
    return userProfiles;
  }

  private UserProfile getUserProfile(String userId) throws Exception {
    for (UserProfile userProfile : this.userProfiles) {
      if (userProfile.getUserId().equals(userId)) {
        if (userProfile.getUserRole() != 0 && isAdmin(userProfile.getUserId())) {
          userProfile.setUserRole((long) 0);
          userProfile.setUserTitle(Utils.ADMIN);
        }
        return userProfile;
      }
    }
    UserProfile userProfile = new UserProfile();
    userProfile.setUserId(userId);
    return userProfile;
  }

  public UserProfile getEditProfile() {
    return editUserProfile;
  }

  public void activate() {
  }

  public void deActivate() {
  }

  private String stringForumProcess(List<String> values) {
    StringBuilder outPut = new StringBuilder();
    if (!values.isEmpty()) {
      for (String value : values) {
        if (!ForumUtils.isEmpty(value)) {
          if (value.indexOf('(') > 0) {
            String forumSubPath = value.substring(value.lastIndexOf('(')).replace("(", "");
            String categoryId = forumSubPath.split("/")[0];
            String forumId = forumSubPath.split("/")[1];
            Forum forum = getForumService().getForum(categoryId, forumId);
            if (forum != null) {
              outPut.append(forum.getForumName() + "\n");
            } else {
              removeItemContainsInList(listModerate, forumId);
            }
          }
        }
      }
    }
    return outPut.toString();
  }
  
  private String stringCategoryProcess(List<String> values) {
    StringBuilder outPut = new StringBuilder();
    if (!values.isEmpty()) {
      for (String value : values) {
        if (!ForumUtils.isEmpty(value)) {
          if (value.indexOf('(') > 0) {
            String forumSubPath = value.substring(value.lastIndexOf('(')).replace("(", "");
            String categoryId = forumSubPath.split("/")[0];
            Category category = getForumService().getCategory(categoryId);
            if (category != null) {
              outPut.append(category.getCategoryName() + "\n");
            } else {
              removeItemContainsInList(listModCate, categoryId);
              removeItemContainsInList(listModerate, categoryId);
            }
          }
        }
      }
    }
    return outPut.toString();
  }

  private void removeItemContainsInList(List<String> list, String item) {
    int index = containsIndexList(list, item);
    if (index >= 0) {
      list.remove(index);
    }
  }

  private int containsIndexList(List<String> list, String item) {
    for (int i = 0; i < list.size(); ++i) {
      String str = list.get(i);
      if (str.indexOf(item) >= 0) {
        return i;
      }
    }
    return -1;
  }

  private List<String> setListCategoryIds() {
    List<String> listId = new ArrayList<String>();
    if (!this.listModCate.isEmpty()) {
      for (String value : listModCate) {
        if (value != null && value.trim().length() > 0) {
          listId.add(value.substring(value.lastIndexOf("(") + 1));
        }
      }
    }
    return listId;
  }

  private List<String> setListForumIds() {
    List<String> listId = new ArrayList<String>();
    if (!this.listModerate.isEmpty()) {
      for (String value : listModerate) {
        if (value != null && value.trim().length() > 0) {
          listId.add(value.substring(value.lastIndexOf(ForumUtils.SLASH) + 1));
        }
      }
    }
    return listId;
  }

  protected String getCategoryId(String str) {
    return str.substring((str.lastIndexOf('(') + 1), str.lastIndexOf('/'));
  }

  private List<String> getModerateList(List<String> forumsModerate) {
    List<String> list = new ArrayList<String>();
    for (String string : forumsModerate) {
      if (string.indexOf('(') > 0) {
        string = string.substring((string.lastIndexOf('(') + 1));
        list.add(string);
      }
    }
    return list;
  }

  protected boolean getIsEdit() {
    return this.isEdit;
  }

  public void setModForunValues(List<String> values) {
    this.listModerate = new ArrayList<String>(values);
    UIFormInputWithActions inputSetProfile = this.getChildById(FIELD_USERPROFILE_FORM);
    String value = stringForumProcess(values);
    inputSetProfile.getUIFormTextAreaInput(FIELD_MODERATEFORUMS_MULTIVALUE).setValue(value);
  }

  public void setModCateValues(List<String> values) {
    this.listModCate = values;
    UIFormInputWithActions inputSetProfile = this.getChildById(FIELD_USERPROFILE_FORM);
    String value = stringCategoryProcess(values);
    inputSetProfile.getUIFormTextAreaInput(FIELD_MODERATECATEGORYS_MULTIVALUE).setValue(value);
  }

  private void initUserProfileForm() throws Exception {
    this.setForumLinks();
    List<SelectItemOption<String>> list;
    UIFormStringInput userId = new UIFormStringInput(FIELD_USERID_INPUT, FIELD_USERID_INPUT, null);
    userId.setValue(this.editUserProfile.getUserId());
    userId.setReadOnly(true);
    userId.setDisabled(true);
    UIFormStringInput screenName = new UIFormStringInput(FIELD_SCREENNAME_INPUT, FIELD_SCREENNAME_INPUT, null);
    String screenN = editUserProfile.getScreenName();
    if (ForumUtils.isEmpty(screenN))
      screenN = editUserProfile.getUserId();
    screenName.setValue(screenN);
    UIFormStringInput userTitle = new UIFormStringInput(FIELD_USERTITLE_INPUT, FIELD_USERTITLE_INPUT, null);
    String title = this.editUserProfile.getUserTitle();
    boolean isAdmin = false;
    UICheckBoxInput userRole = new UICheckBoxInput(FIELD_USERROLE_CHECKBOX, FIELD_USERROLE_CHECKBOX, false);
    if (this.editUserProfile.getUserRole() == 0)
      isAdmin = true;
    if (isAdmin(this.editUserProfile.getUserId())) {
      userRole.setDisabled(true);
      isAdmin = true;
      if (this.editUserProfile.getUserRole() != 0)
        title = Utils.ADMIN;
    }
    userRole.setValue(isAdmin);
    userTitle.setValue(title);

    UIFormTextAreaInput signature = new UIFormTextAreaInput(FIELD_SIGNATURE_TEXTAREA, FIELD_SIGNATURE_TEXTAREA, null);
    signature.setValue(this.editUserProfile.getSignature());
    UICheckBoxInput isDisplaySignature = new UICheckBoxInput(FIELD_ISDISPLAYSIGNATURE_CHECKBOX, FIELD_ISDISPLAYSIGNATURE_CHECKBOX, false);
    isDisplaySignature.setChecked(this.editUserProfile.getIsDisplaySignature());

    UIFormTextAreaInput moderateForums = new UIFormTextAreaInput(FIELD_MODERATEFORUMS_MULTIVALUE, FIELD_MODERATEFORUMS_MULTIVALUE, null);
    List<String> values = Arrays.asList(editUserProfile.getModerateForums());
    this.listModerate = new ArrayList<String>(values);
    moderateForums.setValue(stringForumProcess(values));
    moderateForums.setReadOnly(true);

    UIFormTextAreaInput moderateCategorys = new UIFormTextAreaInput(FIELD_MODERATECATEGORYS_MULTIVALUE, FIELD_MODERATECATEGORYS_MULTIVALUE, null);
    List<String> valuesCate = Arrays.asList(editUserProfile.getModerateCategory());
    this.listModCate = new ArrayList<String>(valuesCate);
    moderateCategorys.setValue(stringCategoryProcess(valuesCate));
    moderateCategorys.setReadOnly(true);

    UIAvatarContainer avatarContainer = createUIComponent(UIAvatarContainer.class, null, "Avatar");
    avatarContainer.setUserProfile(this.editUserProfile);
    avatarContainer.setForumService(getForumService());
    UICheckBoxInput isDisplayAvatar = new UICheckBoxInput(FIELD_ISDISPLAYAVATAR_CHECKBOX, FIELD_ISDISPLAYAVATAR_CHECKBOX, false);
    isDisplayAvatar.setChecked(this.editUserProfile.getIsDisplayAvatar());
    // Option
    String[] timeZone1 = getLabel(FIELD_TIMEZONE).split(ForumUtils.SLASH);
    list = new ArrayList<SelectItemOption<String>>();
    for (String string : timeZone1) {
      list.add(new SelectItemOption<String>(string, ForumUtils.getTimeZoneNumberInString(string)));
    }
    UIFormSelectBox timeZone = new UIFormSelectBox(FIELD_TIMEZONE_SELECTBOX, FIELD_TIMEZONE_SELECTBOX, list);
    double timeZoneOld = -editUserProfile.getTimeZone();
    Date date = getNewDate(timeZoneOld);
    String mark = "-";
    if (timeZoneOld < 0) {
      timeZoneOld = -timeZoneOld;
    } else if (timeZoneOld > 0) {
      mark = "+";
    } else {
      timeZoneOld = 0.0;
      mark = ForumUtils.EMPTY_STR;
    }
    timeZone.setValue(mark + timeZoneOld + "0");

    list = new ArrayList<SelectItemOption<String>>();
    String[] format = new String[] { "M-d-yyyy", "M-d-yy", "MM-dd-yy", "MM-dd-yyyy", "yyyy-MM-dd", "yy-MM-dd", "dd-MM-yyyy", "dd-MM-yy", "M/d/yyyy", "M/d/yy", "MM/dd/yy", "MM/dd/yyyy", "yyyy/MM/dd", "yy/MM/dd", "dd/MM/yyyy", "dd/MM/yy" };
    for (String frm : format) {
      list.add(new SelectItemOption<String>((frm.toLowerCase() + " (" + TimeConvertUtils.getFormatDate(frm, date) + ")"), frm));
    }
    UIFormSelectBox shortdateFormat = new UIFormSelectBox(FIELD_SHORTDATEFORMAT_SELECTBOX, FIELD_SHORTDATEFORMAT_SELECTBOX, list);
    shortdateFormat.setValue(editUserProfile.getShortDateFormat());
    list = new ArrayList<SelectItemOption<String>>();
    format = new String[] { "EEE, MMMM dd, yyyy", "EEEE, MMMM dd, yyyy", "EEEE, dd MMMM, yyyy", "EEE, MMM dd, yyyy", "EEEE, MMM dd, yyyy", "EEEE, dd MMM, yyyy", "MMMM dd, yyyy", "dd MMMM, yyyy", "MMM dd, yyyy", "dd MMM, yyyy" };
    for (String idFrm : format) {
      list.add(new SelectItemOption<String>((idFrm.toLowerCase() + " (" + TimeConvertUtils.getFormatDate(idFrm, date) + ")"), idFrm.replaceFirst(" ", "=")));
    }
    UIFormSelectBox longDateFormat = new UIFormSelectBox(FIELD_LONGDATEFORMAT_SELECTBOX, FIELD_LONGDATEFORMAT_SELECTBOX, list);
    longDateFormat.setValue(editUserProfile.getLongDateFormat().replaceFirst(" ", "="));
    list = new ArrayList<SelectItemOption<String>>();
    list.add(new SelectItemOption<String>("12-hour", "hh:mm=a"));
    list.add(new SelectItemOption<String>("24-hour", "HH:mm"));
    UIFormSelectBox timeFormat = new UIFormSelectBox(FIELD_TIMEFORMAT_SELECTBOX, FIELD_TIMEFORMAT_SELECTBOX, list);
    timeFormat.setValue(editUserProfile.getTimeFormat().replace(' ', '='));
    list = new ArrayList<SelectItemOption<String>>();
    for (int i = 5; i <= 45; i = i + 5) {
      list.add(new SelectItemOption<String>(String.valueOf(i), ("id" + i)));
    }
    UIFormSelectBox maximumThreads = new UIFormSelectBox(FIELD_MAXTOPICS_SELECTBOX, FIELD_MAXTOPICS_SELECTBOX, list);
    maximumThreads.setValue("id" + editUserProfile.getMaxTopicInPage());
    list = new ArrayList<SelectItemOption<String>>();
    for (int i = 5; i <= 35; i = i + 5) {
      list.add(new SelectItemOption<String>(String.valueOf(i), ("id" + i)));
    }
    UIFormSelectBox maximumPosts = new UIFormSelectBox(FIELD_MAXPOSTS_SELECTBOX, FIELD_MAXPOSTS_SELECTBOX, list);
    maximumPosts.setValue("id" + editUserProfile.getMaxPostInPage());
    // Ban
    UICheckBoxInput isBanned = new UICheckBoxInput(FIELD_ISBANNED_CHECKBOX, FIELD_ISBANNED_CHECKBOX, false);
    boolean isBan = editUserProfile.getIsBanned();
    isBanned.setChecked(isBan);
    list = new ArrayList<SelectItemOption<String>>();
    String dv = "Day";
    int i = 1;
    long oneDate = 86400000, until = 0;
    Calendar cal = CommonUtils.getGreenwichMeanTime();
    if (isBan) {
      until = editUserProfile.getBanUntil();
      cal.setTimeInMillis(until);
      list.add(new SelectItemOption<String>(getLabel("Banned until: ") + TimeConvertUtils.getFormatDate(editUserProfile.getShortDateFormat() + " hh:mm a", cal.getTime()) + " GMT+0", ("Until_" + until)));
    }
    while (true) {
      if (i == 2 && dv.equals("Day")) {
        dv = "Days";
      }
      if (i == 8 && dv.equals("Days"))
        i = 10;
      if (i == 11) {
        i = 2;
        dv = "Weeks";
      }
      if (i == 4 && dv.equals("Weeks")) {
        i = 1;
        dv = "Month";
      }
      if (i == 2 && dv.equals("Month")) {
        dv = "Months";
      }
      if (i == 7 && dv.equals("Months")) {
        i = 1;
        dv = "Year";
      }
      if (i == 2 && dv.equals("Year")) {
        dv = "Years";
      }
      if (i == 4 && dv.equals("Years")) {
        break;
      }
      if (dv.equals("Days") || dv.equals("Day")) {
        cal = CommonUtils.getGreenwichMeanTime();
        until = cal.getTimeInMillis() + i * oneDate;
        cal.setTimeInMillis(until);
      }
      if (dv.equals("Weeks")) {
        cal = CommonUtils.getGreenwichMeanTime();
        until = cal.getTimeInMillis() + i * oneDate * 7;
        cal.setTimeInMillis(until);
      }
      if (dv.equals("Month") || dv.equals("Months")) {
        cal = CommonUtils.getGreenwichMeanTime();
        cal.setLenient(true);
        int t = cal.get(Calendar.MONTH) + i;
        if (t >= 12) {
          cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + 1);
          t -= 12;
        }
        cal.set(Calendar.MONTH, t);
        until = cal.getTimeInMillis();
      }
      if (dv.equals("Years") || dv.equals("Year")) {
        cal = CommonUtils.getGreenwichMeanTime();
        cal.setLenient(true);
        cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) + i);
        until = cal.getTimeInMillis();
      }
      list.add(new SelectItemOption<String>(i + " " + getLabel(dv) + " (" + TimeConvertUtils.getFormatDate(editUserProfile.getShortDateFormat() + " hh:mm a", cal.getTime()) + " GMT+0)", ("Until_" + until)));
      ++i;
    }
    UIFormSelectBox banUntil = new UIFormSelectBox(FIELD_BANUNTIL_SELECTBOX, FIELD_BANUNTIL_SELECTBOX, list);
    if (isBan) {
      banUntil.setValue("Until_" + editUserProfile.getBanUntil());
    }
    UIFormTextAreaInput banReason = new UIFormTextAreaInput(FIELD_BANREASON_TEXTAREA, FIELD_BANREASON_TEXTAREA, null);
    UIFormStringInput banCounter = new UIFormStringInput(FIELD_BANCOUNTER_INPUT, FIELD_BANCOUNTER_INPUT, null);
    banCounter.setValue(editUserProfile.getBanCounter() + ForumUtils.EMPTY_STR);
    UIFormTextAreaInput banReasonSummary = new UIFormTextAreaInput(FIELD_BANREASONSUMMARY_MULTIVALUE, FIELD_BANREASONSUMMARY_MULTIVALUE, null);
    banReasonSummary.setValue(ForumUtils.unSplitForForum(editUserProfile.getBanReasonSummary()));
    banReasonSummary.setReadOnly(true);
    UIFormStringInput createdDateBan = new UIFormStringInput(FIELD_CREATEDDATEBAN_INPUT, FIELD_CREATEDDATEBAN_INPUT, null);
    if (isBan) {
      banReason.setValue(editUserProfile.getBanReason());
      createdDateBan.setValue(TimeConvertUtils.getFormatDate("MM/dd/yyyy, hh:mm a", editUserProfile.getCreatedDateBan()));
    } else {
      banReason.setDisabled(false);
    }
    UIFormInputWithActions inputSetProfile = new UIFormInputWithActions(FIELD_USERPROFILE_FORM);
    inputSetProfile.addUIFormInput(userId);
    inputSetProfile.addUIFormInput(screenName);
    inputSetProfile.addUIFormInput(userTitle);
    inputSetProfile.addUIFormInput(userRole);
    inputSetProfile.addUIFormInput(moderateCategorys);
    inputSetProfile.addUIFormInput(moderateForums);
    inputSetProfile.addUIFormInput(signature);
    inputSetProfile.addUIFormInput(isDisplaySignature);
    
    inputSetProfile.addUIFormInput(avatarContainer);
    inputSetProfile.addUIFormInput(isDisplayAvatar);

    String[] fields = new String[] {FIELD_MODERATEFORUMS_MULTIVALUE, FIELD_MODERATECATEGORYS_MULTIVALUE};
    String[] actionNames = new String[] {"AddValuesModForum", "AddValuesModCategory"};
    List<ActionData> actions;
    ActionData actionData;
    for (int j = 0; j < fields.length; j++) {
      String string = fields[j];
      actionData = new ActionData();
      actionData.setActionListener(actionNames[j]);
      actionData.setActionParameter(string);
      actionData.setCssIconClass("uiIconAddIcon uiIconLightGray");
      actionData.setActionName(string);
      actions = new ArrayList<ActionData>();
      actions.add(actionData);
      inputSetProfile.setActionField(string, actions);
    }
    //
    addUIFormInput(inputSetProfile);

    UIFormInputWithActions inputSetOption = new UIFormInputWithActions(FIELD_USEROPTION_FORM);
    inputSetOption.addUIFormInput(timeZone);
    inputSetOption.addUIFormInput(shortdateFormat);
    inputSetOption.addUIFormInput(longDateFormat);
    inputSetOption.addUIFormInput(timeFormat);
    inputSetOption.addUIFormInput(maximumThreads);
    inputSetOption.addUIFormInput(maximumPosts);
    addUIFormInput(inputSetOption);

    UIFormInputWithActions inputSetBan = new UIFormInputWithActions(FIELD_USERBAN_FORM);
    inputSetBan.addUIFormInput(isBanned);
    inputSetBan.addUIFormInput(banUntil);
    inputSetBan.addUIFormInput(banReason);
    inputSetBan.addUIFormInput(banCounter);
    inputSetBan.addUIFormInput(banReasonSummary);
    inputSetBan.addUIFormInput(createdDateBan);
    //
    addUIFormInput(inputSetBan);
    
    UIPageListTopicByUser pageListTopicByUser = addChild(UIPageListTopicByUser.class, null, null);
    pageListTopicByUser.setUserName(this.editUserProfile.getUserId());
    UIPageListPostByUser listPostByUser = addChild(UIPageListPostByUser.class, null, null);
    listPostByUser.setUserName(this.editUserProfile.getUserId());
  }

  private Date getNewDate(double timeZoneOld) {
    Calendar calendar = CommonUtils.getGreenwichMeanTime();
    calendar.setTimeInMillis(calendar.getTimeInMillis() + (long) (timeZoneOld * 3600000));
    return calendar.getTime();
  }

  private void setForumLinks() throws Exception {
    UIForumLinks uiForumLinks = this.getAncestorOfType(UIForumPortlet.class).getChild(UIForumLinks.class);
    boolean hasGetService = false;
    if (uiForumLinks == null)
      hasGetService = true;
    else
      this.forumLinks = uiForumLinks.getForumLinks();
    if (this.forumLinks == null || forumLinks.size() <= 0)
      hasGetService = true;
    if (hasGetService) {
      this.getForumService().getAllLink(ForumUtils.EMPTY_STR, ForumUtils.EMPTY_STR);
    }
  }

  protected List<ForumLinkData> getForumLinks() throws Exception {
    return this.forumLinks;
  }

  private void searchUserProfileByKey(String keyword) throws Exception {
    try {
      Map<String, Object> mapObject = new HashMap<String, Object>();
      OrganizationService service = this.getApplicationComponent(OrganizationService.class);
      keyword = "*" + keyword + "*";
      List<Object> results = new CopyOnWriteArrayList<Object>();
      Query q;
      q = new Query();
      q.setUserName(keyword);
      ListAccess<User> listAcess = service.getUserHandler().findUsersByQuery(q);
      for (User user : listAcess.load(0, listAcess.getSize())) {
        mapObject.put(user.getUserName(), user);
      }

      q = new Query();
      q.setLastName(keyword);
      listAcess = service.getUserHandler().findUsersByQuery(q);
      for (User user : listAcess.load(0, listAcess.getSize())) {
        mapObject.put(user.getUserName(), user);
      }

      q = new Query();
      q.setFirstName(keyword);
      listAcess = service.getUserHandler().findUsersByQuery(q);
      for (User user : listAcess.load(0, listAcess.getSize())) {
        mapObject.put(user.getUserName(), user);
      }

      q = new Query();
      q.setEmail(keyword);
      listAcess = service.getUserHandler().findUsersByQuery(q);
      for (User user : listAcess.load(0, listAcess.getSize())) {
        mapObject.put(user.getUserName(), user);
      }

      for (Object object : getForumService().searchUserProfile(keyword).getAll()) {
        mapObject.put(((UserProfile) object).getUserId(), object);
      }

      results.addAll(Arrays.asList(mapObject.values().toArray()));

      this.userPageList = new ForumPageList(results);
      this.userPageList.setPageSize(5);
      pageIterator.updatePageList(this.userPageList);
      pageIterator.setSelectPage(1);
      this.isViewSearchUser = true;
    } catch (Exception e) {
      this.isViewSearchUser = false;
    }
  }

  static public class ViewProfileActionListener extends BaseEventListener<UIModeratorManagementForm> {
    public void onEvent(Event<UIModeratorManagementForm> event, UIModeratorManagementForm uiForm, String userId) throws Exception {
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      UIViewUserProfile viewUserProfile = openPopup(popupContainer, UIViewUserProfile.class, 670, 0);
      viewUserProfile.setUserProfileViewer(uiForm.getUserProfile(userId));
    }
  }

  static public class EditProfileActionListener extends BaseEventListener<UIModeratorManagementForm> {
    public void onEvent(Event<UIModeratorManagementForm> event, UIModeratorManagementForm uiForm, String userId) throws Exception {
      uiForm.editUserProfile = uiForm.getForumService().updateUserProfileSetting(uiForm.getUserProfile(userId));
      uiForm.removeChildById("ForumUserProfile");
      uiForm.removeChildById("ForumUserOption");
      uiForm.removeChildById("ForumUserBan");
      uiForm.removeChild(UIPageListTopicByUser.class);
      uiForm.removeChild(UIPageListPostByUser.class);
      uiForm.initUserProfileForm();
      uiForm.isEdit = true;
      UIPopupWindow popupWindow = uiForm.getAncestorOfType(UIPopupWindow.class);
      popupWindow.setWindowSize(950, 540);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupWindow.getParent());
    }
  }

  static public class CancelActionListener extends EventListener<UIModeratorManagementForm> {
    public void execute(Event<UIModeratorManagementForm> event) throws Exception {
      UIModeratorManagementForm uiForm = event.getSource();
      uiForm.isEdit = false;
      UIPopupWindow popupWindow = uiForm.getAncestorOfType(UIPopupWindow.class);
      popupWindow.setWindowSize(760, 350);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupWindow.getParent());
    }
  }

  static public class SaveActionListener extends BaseEventListener<UIModeratorManagementForm> {
    public void onEvent(Event<UIModeratorManagementForm> event, UIModeratorManagementForm uiForm, String userId) throws Exception {
      UserProfile userProfile = uiForm.editUserProfile;
      UIFormInputWithActions inputSetProfile = uiForm.getChildById(FIELD_USERPROFILE_FORM);
      String userTitle = inputSetProfile.getUIStringInput(FIELD_USERTITLE_INPUT).getValue();
      String screenName = inputSetProfile.getUIStringInput(FIELD_SCREENNAME_INPUT).getValue();
      long userRole = 2;
      boolean isAdmin = inputSetProfile.getUICheckBoxInput(FIELD_USERROLE_CHECKBOX).isChecked();
      if (isAdmin)
        userRole = 0;
      else if (uiForm.isAdmin(userProfile.getUserId())) {
        isAdmin = true;
        userRole = 0;
        if (userTitle == null || userTitle.trim().length() == 0)
          userTitle = Utils.ADMIN;
        else if (userTitle.equals(Utils.ADMIN))
          userTitle = userProfile.getUserTitle();
      }

      // -----------------
      List<String> oldModerateForum = uiForm.getModerateList(Arrays.asList(userProfile.getModerateForums()));
      List<String> newModeratorsForum = new ArrayList<String>();
      List<String> removeModerateForum = new ArrayList<String>();
      List<String> forumIdsMod = new ArrayList<String>();
      //
      newModeratorsForum = uiForm.getModerateList(uiForm.listModerate);
      forumIdsMod.addAll(newModeratorsForum);
      if (newModeratorsForum.isEmpty()) {
        removeModerateForum = oldModerateForum;
      } else {
        for (String string : oldModerateForum) {
          if (newModeratorsForum.contains(string)) {
            newModeratorsForum.remove(string);
          } else {
            removeModerateForum.add(string);
          }
        }
        if (!newModeratorsForum.isEmpty())
          uiForm.getForumService().saveModerateOfForums(newModeratorsForum, userProfile.getUserId(), false);
      }
      if (!removeModerateForum.isEmpty()) {
        uiForm.getForumService().saveModerateOfForums(removeModerateForum, userProfile.getUserId(), true);
      }

      uiForm.getForumService().saveUserModerator(userProfile.getUserId(), uiForm.listModerate, false);

      List<String> moderateCates = new ArrayList<String>();
      moderateCates.addAll(uiForm.listModCate);
      List<String> newModeratorsCate = new ArrayList<String>();
      List<String> categoryIdsMod = new ArrayList<String>();
      List<String> oldModerateCate = uiForm.getModerateList(Arrays.asList(userProfile.getModerateCategory()));
      List<String> removeModerateCate = new ArrayList<String>();
      // set moderator category
      newModeratorsCate = uiForm.getModerateList(moderateCates);
      categoryIdsMod.addAll(newModeratorsCate);
      if (newModeratorsCate.isEmpty()) {
        removeModerateCate = oldModerateCate;
      } else {
        for (String string : oldModerateCate) {
          if (newModeratorsCate.contains(string)) {
            newModeratorsCate.remove(string);
          } else {
            removeModerateCate.add(string);
          }
        }
        if (!newModeratorsCate.isEmpty()) {
          uiForm.getForumService().saveModOfCategory(newModeratorsCate, userProfile.getUserId(), true);
          if (userRole > 1)
            userRole = 1;
        }
      }
      if (removeModerateCate.size() > 0) {
        uiForm.getForumService().saveModOfCategory(removeModerateCate, userProfile.getUserId(), false);
      }

      if (userRole > 1) {
        uiForm.listModerate = uiForm.getForumService().getUserModerator(userProfile.getUserId(), false);
        if (uiForm.listModerate.size() >= 1 && !uiForm.listModerate.get(0).equals(" "))
          userRole = 1;
      }

      if (userTitle == null || userTitle.trim().length() < 1) {
        userTitle = userProfile.getUserTitle();
      } else if (!isAdmin) {
        int newPos = Arrays.asList(uiForm.permissionUser).indexOf(userTitle.toLowerCase());
        if (newPos >= 0 && newPos < userRole) {
          if (Arrays.asList(uiForm.permissionUser).indexOf(userProfile.getUserTitle().toLowerCase()) < 0)
            userTitle = userProfile.getUserTitle();
          else
            userTitle = uiForm.titleUser[(int) userRole];
        }
      } else {
        if (userTitle.equalsIgnoreCase(uiForm.titleUser[1]) || userTitle.equalsIgnoreCase(uiForm.titleUser[2]))
          userTitle = uiForm.titleUser[0];
      }
      if (userRole == 1 && userTitle.equalsIgnoreCase(uiForm.titleUser[2])) {
        userTitle = uiForm.titleUser[1];
      }

      String signature = inputSetProfile.getUIFormTextAreaInput(FIELD_SIGNATURE_TEXTAREA).getValue();
      signature = TransformHTML.enCodeHTMLTitle(signature);
      boolean isDisplaySignature = inputSetProfile.getUICheckBoxInput(FIELD_ISDISPLAYSIGNATURE_CHECKBOX).isChecked();
      Boolean isDisplayAvatar = inputSetProfile.getUICheckBoxInput(FIELD_ISDISPLAYAVATAR_CHECKBOX).isChecked();

      UIFormInputWithActions inputSetOption = uiForm.getChildById(FIELD_USEROPTION_FORM);
      double timeZone = Double.parseDouble(inputSetOption.getUIFormSelectBox(FIELD_TIMEZONE_SELECTBOX).getValue());
      String shortDateFormat = inputSetOption.getUIFormSelectBox(FIELD_SHORTDATEFORMAT_SELECTBOX).getValue();
      String longDateFormat = inputSetOption.getUIFormSelectBox(FIELD_LONGDATEFORMAT_SELECTBOX).getValue();
      String timeFormat = inputSetOption.getUIFormSelectBox(FIELD_TIMEFORMAT_SELECTBOX).getValue();
      long maxTopic = Long.parseLong(inputSetOption.getUIFormSelectBox(FIELD_MAXTOPICS_SELECTBOX).getValue().substring(2));
      long maxPost = Long.parseLong(inputSetOption.getUIFormSelectBox(FIELD_MAXPOSTS_SELECTBOX).getValue().substring(2));

      UIFormInputWithActions inputSetBan = uiForm.getChildById(FIELD_USERBAN_FORM);
      boolean wasBanned = userProfile.getIsBanned();
      boolean isBanned = inputSetBan.getUICheckBoxInput(FIELD_ISBANNED_CHECKBOX).isChecked();
      String until = inputSetBan.getUIFormSelectBox(FIELD_BANUNTIL_SELECTBOX).getValue();
      long banUntil = 0;
      if (!ForumUtils.isEmpty(until)) {
        banUntil = Long.parseLong(until.substring(6));
      }
      String banReason = inputSetBan.getUIFormTextAreaInput(FIELD_BANREASON_TEXTAREA).getValue();
      String[] banReasonSummaries = userProfile.getBanReasonSummary();
      Date date = CommonUtils.getGreenwichMeanTime().getTime();
      int banCounter = userProfile.getBanCounter();
      date.setTime(banUntil);
      StringBuffer stringBuffer = new StringBuffer();
      if (!ForumUtils.isEmpty(banReason)) {
        stringBuffer.append("Ban Reason: ").append(banReason).append(" ");
      }
      stringBuffer.append("From Date: ").append(TimeConvertUtils.getFormatDate("MM-dd-yyyy hh:mm a", CommonUtils.getGreenwichMeanTime().getTime())).append(" GMT+0 To Date: ").append(TimeConvertUtils.getFormatDate("MM-dd-yyyy hh:mm a", date)).append(" GMT+0");
      if (isBanned) {
        if (banReasonSummaries != null && banReasonSummaries.length > 0) {
          if (wasBanned) {
            banReasonSummaries[0] = stringBuffer.toString();
          } else {
            String[] temp = new String[banReasonSummaries.length + 1];
            int i = 1;
            for (String string : banReasonSummaries) {
              temp[i++] = string;
            }
            temp[0] = stringBuffer.toString();
            banReasonSummaries = temp;
            banCounter = banCounter + 1;
          }
        } else {
          banReasonSummaries = new String[] { stringBuffer.toString() };
          banCounter = 1;
        }
      }
      userProfile.setUserTitle(userTitle);
      userProfile.setScreenName(screenName);
      userProfile.setUserRole(userRole);
      userProfile.setSignature(signature);
      userProfile.setIsDisplaySignature(isDisplaySignature);
      userProfile.setModerateCategory(moderateCates.toArray(new String[] {}));
      userProfile.setIsDisplayAvatar(isDisplayAvatar);

      userProfile.setTimeZone(-timeZone);
      userProfile.setShortDateFormat(shortDateFormat);
      userProfile.setLongDateFormat(longDateFormat.replace('=', ' '));
      userProfile.setTimeFormat(timeFormat.replace('=', ' '));
      userProfile.setMaxPostInPage(maxPost);
      userProfile.setMaxTopicInPage(maxTopic);
      userProfile.setIsShowForumJump(false);

      userProfile.setIsBanned(isBanned);
      userProfile.setBanUntil(banUntil);
      userProfile.setBanReason(banReason);
      userProfile.setBanCounter(banCounter);
      userProfile.setBanReasonSummary(banReasonSummaries);
      try {
        uiForm.getForumService().saveUserProfile(userProfile, true, true);
      } catch (Exception e) {
        uiForm.log.trace("\nSave user profile fail: " + e.getMessage() + "\n" + e.getCause());
      }
      uiForm.isEdit = false;
      if (ForumUtils.isEmpty(uiForm.keyWord)) {
        uiForm.isViewSearchUser = false;
        uiForm.setPageListUserProfile();
      } else {
        uiForm.searchUserProfileByKey(uiForm.keyWord);
      }
      UIPopupWindow popupWindow = uiForm.getAncestorOfType(UIPopupWindow.class);
      popupWindow.setWindowSize(760, 350);
      event.getRequestContext().addUIComponentToUpdateByAjax(popupWindow.getParent());
    }
  }

  static public class AddValuesModCategoryActionListener extends BaseEventListener<UIModeratorManagementForm> {
    public void onEvent(Event<UIModeratorManagementForm> event, UIModeratorManagementForm uiForm, String userId) throws Exception {
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      UISelectCategoryForm selectItemForum = openPopup(popupContainer, UISelectCategoryForm.class, 400, 0);
      selectItemForum.setSelectCateId(uiForm.setListCategoryIds());
    }
  }

  static public class AddValuesModForumActionListener extends BaseEventListener<UIModeratorManagementForm> {
    public void onEvent(Event<UIModeratorManagementForm> event, UIModeratorManagementForm uiForm, String userId) throws Exception {
      UIPopupContainer popupContainer = uiForm.getAncestorOfType(UIPopupContainer.class);
      UISelectItemForum selectItemForum = openPopup(popupContainer, UISelectItemForum.class, 400, 0);
      selectItemForum.initSelectForum(uiForm.setListForumIds(), uiForm.getUserProfile().getUserId());
    }
  }

  static public class CloseActionListener extends EventListener<UIModeratorManagementForm> {
    public void execute(Event<UIModeratorManagementForm> event) throws Exception {
      UIForumPortlet forumPortlet = event.getSource().getAncestorOfType(UIForumPortlet.class);
      forumPortlet.cancelAction();
      event.getRequestContext().addUIComponentToUpdateByAjax(forumPortlet);
    }
  }

  static public class GetAllUserActionListener extends EventListener<UIModeratorManagementForm> {
    public void execute(Event<UIModeratorManagementForm> event) throws Exception {
      UIModeratorManagementForm uiForm = event.getSource();
      uiForm.isViewSearchUser = false;
      uiForm.keyWord = ForumUtils.EMPTY_STR;
      uiForm.setPageListUserProfile();
      event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
    }
  }

  static public class SearchUserActionListener extends EventListener<UIModeratorManagementForm> {
    public void execute(Event<UIModeratorManagementForm> event) throws Exception {
      UIModeratorManagementForm uiForm = event.getSource();
      String keyword = uiForm.getUIStringInput(FIELD_SEARCH_USER).getValue();
      if (Utils.isEmpty(keyword) == false) {
        uiForm.searchUserProfileByKey(keyword);
        event.getRequestContext().addUIComponentToUpdateByAjax(uiForm);
      } else {
        uiForm.warning("UIQuickSearchForm.msg.checkEmpty");
      }
    }
  }
}
