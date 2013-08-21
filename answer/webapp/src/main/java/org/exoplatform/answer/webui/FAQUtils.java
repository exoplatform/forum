/*
 * Copyright (C) 2003-2008 eXo Platform SAS.
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
package org.exoplatform.answer.webui;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ResourceBundle;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.portlet.PortletPreferences;

import org.apache.commons.lang.StringUtils;
import org.exoplatform.container.ExoContainerContext;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.download.DownloadService;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.CategoryTree;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.faq.service.FileAttachment;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.user.CommonContact;
import org.exoplatform.forum.common.user.ContactProvider;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.portal.application.PortalRequestContext;
import org.exoplatform.portal.webui.util.Util;
import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.services.organization.OrganizationService;
import org.exoplatform.services.organization.User;
import org.exoplatform.services.resources.LocaleConfig;
import org.exoplatform.services.resources.LocaleConfigService;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.web.CacheUserProfileFilter;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.application.portlet.PortletRequestContext;
import org.exoplatform.webui.core.UIComponent;
import org.exoplatform.webui.utils.TimeConvertUtils;

public class FAQUtils {
  public static String       DISPLAYAPPROVED             = "approved";

  public static String       DISPLAYBOTH                 = "both";

  public static String       UPLOAD_FILE_SIZE            = "uploadFileSizeLimitMB";

  public static String       UPLOAD_AVATAR_SIZE          = "uploadAvatarSizeLimitMB";

  public static final String COMMA                       = ",".intern();

  public static final int    DEFAULT_VALUE_UPLOAD_PORTAL = -1;

  static private Log         log                         = ExoLogger.getLogger(FAQUtils.class);

  public static FAQService getFAQService() throws Exception {
    return (FAQService) PortalContainer.getComponent(FAQService.class);
  }

  public static String getDefaultLanguage() {
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    return context.getLocale().getDisplayLanguage();
  }

  public static List<String> getAllLanguages(UIComponent component) {
    LocaleConfigService configService = component.getApplicationComponent(LocaleConfigService.class);
    List<String> languages = new ArrayList<String>();
    for (LocaleConfig localeConfig : configService.getLocalConfigs()) {
      languages.add(localeConfig.getLocale().getDisplayLanguage());
    }
    return languages;
  }

  /**
   * Find category which is already exist.<br/>
   * for example: when you are standing in category D in path: Root\A\B\C\D, you do some action (add new category, add question, go out to category C or B) but another moderator delete category C (or
   * B, A). Then this function will be use to find the nearest category with category D (which is exist) and move you into this category.<br/>
   * <u>Detail:</u><br/>
   * the first, system get category C, if C is exist, you will be moved into C else jump to B and test again.<br/>
   * This processing is done until find a category already exist.
   * 
   * @param faqService_
   *          FAQ Service
   * @param fAQContainer
   *          UIAnswersContainer this component is used to updated data
   * @param sessionProvider
   *          SessionProvider
   * @throws Exception
   */
  public static void findCateExist(FAQService faqService_, UIAnswersContainer fAQContainer) throws Exception {
    UIBreadcumbs breadcumbs = fAQContainer.findFirstComponentOfType(UIBreadcumbs.class);
    String pathCate = "";
    for (String path : breadcumbs.pathList_.get(breadcumbs.pathList_.size() - 1).split("/")) {
      if (path.equals("FAQService")) {
        pathCate = path;
        continue;
      }
      try {
        faqService_.getCategoryById(path);
        if (pathCate.trim().length() > 0)
          pathCate += "/";
        pathCate += path;
      } catch (Exception pathExc) {
        UIQuestions questions = fAQContainer.findFirstComponentOfType(UIQuestions.class);
        try {
          breadcumbs.setUpdataPath(pathCate);
        } catch (Exception exc) {
          log.debug("Setting update path fail: " + exc.getMessage(), exc);
        }
        if (pathCate.indexOf("/") > 0) {
          questions.setCategoryId(pathCate.substring(pathCate.lastIndexOf("/") + 1));
        } else {
          questions.categoryId_ = null;
          questions.setListObject();
          // questions.setIsNotChangeLanguage() ;
        }
        fAQContainer.findFirstComponentOfType(UICategories.class).setPathCategory(pathCate);
        break;
      }
    }
  }
  
  
  public static String getCategoryPathName(String categoryPathName, boolean isReplaceIcon) {
    if (isFieldEmpty(categoryPathName)) {
      return "";
    }
    StringBuffer buffer = new StringBuffer(WebUIUtils.getLabel("UIBreadcumbs", Utils.CATEGORY_HOME));
    categoryPathName = categoryPathName.replaceFirst(Utils.CATEGORY_HOME, "");
    if(isReplaceIcon == true) {
      categoryPathName = categoryPathName.replaceAll(" > ", "<i class=\"uiIconArrowRightMini uiIconLightGray\"></i>");
    }

    return buffer.append(categoryPathName).toString();
  } 

  public static InternetAddress[] getInternetAddress(String addressList) throws Exception {
    if (isFieldEmpty(addressList))
      return new InternetAddress[1];
    try {
      return InternetAddress.parse(addressList);
    } catch (Exception e) {
      return new InternetAddress[1];
    }
  }

  public static String[] splitForFAQ(String str) throws Exception {
    if (!isFieldEmpty(str)) {
      String[] strs = new String[] { str };
      if (str.contains(COMMA)) {
        str = str.trim().replaceAll("(,\\s*)", COMMA).replaceAll("(\\s*,)", COMMA).replaceAll("(,,*)", COMMA);
        strs = str.trim().split(",");
      } else if (str.contains(";")) {
        str = str.trim().replaceAll("(;\\s*)", ";").replaceAll("(\\s*;)", ";").replaceAll("(;;*)", ";");
        strs = str.split(";");
      }
      return strs;
    }
    return new String[] {};
  }

  static public String getCurrentUser() throws Exception {
    return UserHelper.getCurrentUser();
  }

  static public User getCurrentUserObject() throws Exception {
    try {
      ConversationState state = ConversationState.getCurrent();
      User user = (User) state.getAttribute(CacheUserProfileFilter.USER_PROFILE);
      if (user == null) {
        user = UserHelper.getOrganizationService().getUserHandler().findUserByName(UserHelper.getCurrentUser());
      }
      return user;
    } catch (Exception e) {
      return null;
    }
  }
  
  /**
   * @param userName
   * @return email of the user. The current user is implied if userName is null.
   * @throws Exception
   */
  static public String getEmailUser(String userName) throws Exception {
    if (userName == null) {
      return getCurrentUserObject().getEmail();
    } else {
      OrganizationService organizationService = (OrganizationService) PortalContainer.getComponent(OrganizationService.class);
      User user = organizationService.getUserHandler().findUserByName(userName);
      String email = user.getEmail();
      return email;
    }
  }

  /**
   * @param userName
   * @return Full name of user. The current user is implied if userName is null.
   * @throws Exception
   */
  static public String getFullName(String userName) throws Exception {
    if (userName == null) {
      return getUserFullName(getCurrentUserObject());
    }
    try {
      OrganizationService organizationService = (OrganizationService) PortalContainer.getComponent(OrganizationService.class);
      User user = organizationService.getUserHandler().findUserByName(userName);
      return getUserFullName(user);
    } catch (Exception e) {
      return getScreenName(userName, "");
    }
  }

  public static String getUserFullName(User user) {
    String displayName = user.getDisplayName();
    if (isFieldEmpty(displayName)) {
      displayName = new StringBuffer(user.getFirstName()).append(" ").append(user.getLastName()).toString();
    }
    return displayName;
  }

  public static String getScreenName(String userName, String fullName) {
    return (userName.contains(Utils.DELETED)) ? ("<s>" + ((isFieldEmpty(fullName)) ? 
                (userName.substring(0, userName.indexOf(Utils.DELETED))) : fullName) + "</s>") : userName;
  }
  
  public static boolean isFieldEmpty(String s) {
    return (s == null || s.trim().length() <= 0) ? true : false;
  }

  public static boolean isValidEmailAddresses(String addressList) throws Exception {
    if (isFieldEmpty(addressList))
      return true;
    boolean isInvalid = true;
    try {
      InternetAddress[] iAdds = InternetAddress.parse(addressList, true);
      String emailRegex = "[_A-Za-z0-9-]+(\\.[_A-Za-z0-9-]+)*@[_A-Za-z0-9-.]+\\.[A-Za-z]{2,5}";
      for (int i = 0; i < iAdds.length; i++) {
        if (!iAdds[i].getAddress().matches(emailRegex))
          isInvalid = false;
      }
    } catch (AddressException e) {
      return false;
    }
    return isInvalid;
  }

  public static String getSubString(String str, int max) {
    if (str.length() > max) {
      String newStr = str.substring(0, (max - 3));
      return newStr.trim() + "...";
    }
    return str;
  }

  public static String getTitle(String text) {
    if (isFieldEmpty(text)){
      return StringUtils.EMPTY;
    }
    text = text.replaceAll("&nbsp;", CommonUtils.SPACE).replaceAll("( \\s*)", CommonUtils.SPACE);
    return StringUtils.replace(text, "\"", "&quot;");
  }

  public static List<String> getCategoriesIdFAQPortlet() throws Exception {
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String str = portletPref.getValue("displayCategories", "");
    List<String> list = new ArrayList<String>();
    if (!isFieldEmpty(str)) {
      list.addAll(Arrays.asList(str.split(",")));
    }
    return list;
  }

  public static boolean getUseAjaxFAQPortlet() {
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    return Boolean.parseBoolean(portletPref.getValue("useAjax", "false"));
  }

  public static void saveFAQPortletPreference(List<String> list, boolean useAjax) throws Exception {
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    
    //
    if (list.size() > 0) {
      String str = list.toString();
      str = str.replace("[", "").replace("]", "").replaceAll(" ", "");
      portletPref.setValue("displayCategories", str);
    } else {
      portletPref.setValue("displayCategories", "");
    }
    
    //
    portletPref.setValue("useAjax", String.valueOf(useAjax));
    portletPref.store();
  }

  public static void getPorletPreference(FAQSetting faqSetting) {
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    faqSetting.setEnableViewAvatar(Boolean.parseBoolean(portletPref.getValue("enableViewAvatar", "")));
    faqSetting.setEnableAutomaticRSS(Boolean.parseBoolean(portletPref.getValue("enableAutomaticRSS", "")));
    faqSetting.setEnanbleVotesAndComments(Boolean.parseBoolean(portletPref.getValue("enanbleVotesAndComments", "")));
    faqSetting.setEnableAnonymousSubmitQuestion(Boolean.parseBoolean(portletPref.getValue("enableAnonymousSubmitQuestion", "")));
    faqSetting.setDisplayMode(portletPref.getValue("display", ""));
    faqSetting.setOrderBy(portletPref.getValue("orderBy", ""));
    faqSetting.setOrderType(portletPref.getValue("orderType", ""));
    faqSetting.setIsDiscussForum(Boolean.parseBoolean(portletPref.getValue("isDiscussForum", "")));
    faqSetting.setIdNameCategoryForum(portletPref.getValue("idNameCategoryForum", ""));
    faqSetting.setEmailMoveQuestion(portletPref.getValue("emailMoveQuestion", ""));
    faqSetting.setPostQuestionInRootCategory(Boolean.parseBoolean(portletPref.getValue("isPostQuestionInRootCategory", "true")));
  }

  public static void getEmailSetting(FAQSetting faqSetting, boolean isNew, boolean isSettingForm) {
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String emailContent = "";
    if (isNew) {
      emailContent = portletPref.getValue("SendMailAddNewQuestion", "");
    } else {
      if (isSettingForm)
        emailContent = portletPref.getValue("SendMailEditResponseQuestion", "");
    }
    WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
    ResourceBundle res = context.getApplicationResourceBundle();
    if (emailContent == null || emailContent.trim().length() < 1) {
      if (isNew) {
        emailContent = res.getString("SendEmail.AddNewQuestion.Default");
      } else {
        if (isSettingForm)
          emailContent = res.getString("SendEmail.EditQuestion.Default");
        else
          emailContent = res.getString("SendEmail.ResponseQuestion.Default");
      }
    }
    faqSetting.setEmailSettingSubject(res.getString("SendEmail.Default.Subject"));
    faqSetting.setEmailSettingContent(emailContent);
  }

  public static String getEmailMoveQuestion(FAQSetting faqSetting) {
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    String str = portletPref.getValue("emailMoveQuestion", "");
    if (isFieldEmpty(str)) {
      WebuiRequestContext context = WebuiRequestContext.getCurrentInstance();
      ResourceBundle res = context.getApplicationResourceBundle();
      str = res.getString("SendEmail.MoveQuetstion.Default");
    }
    faqSetting.setEmailMoveQuestion(str);
    return str;
  }

  public static void savePortletPreference(FAQSetting setting, String emailAddNewQuestion, String emailEditResponseQuestion) {
    try {
      PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
      PortletPreferences portletPref = pcontext.getRequest().getPreferences();
      portletPref.setValue("display", setting.getDisplayMode());
      portletPref.setValue("orderBy", setting.getOrderBy());
      portletPref.setValue("orderType", setting.getOrderType());
      portletPref.setValue("isDiscussForum", String.valueOf(setting.getIsDiscussForum()));
      portletPref.setValue("idNameCategoryForum", setting.getIdNameCategoryForum());
      portletPref.setValue("enableAutomaticRSS", setting.isEnableAutomaticRSS() + "");
      portletPref.setValue("enableViewAvatar", setting.isEnableViewAvatar() + "");
      portletPref.setValue("enanbleVotesAndComments", setting.isEnanbleVotesAndComments() + "");
      portletPref.setValue("enableAnonymousSubmitQuestion", setting.isEnableAnonymousSubmitQuestion() + "");
      portletPref.setValue("SendMailAddNewQuestion", emailAddNewQuestion);
      portletPref.setValue("SendMailEditResponseQuestion", emailEditResponseQuestion);
      portletPref.setValue("emailMoveQuestion", setting.getEmailMoveQuestion());
      portletPref.setValue("isPostQuestionInRootCategory", setting.isPostQuestionInRootCategory() + "");
      portletPref.store();
    } catch (Exception e) {
      log.error("Fail to save portlet preferences: ", e);
    }
  }

  private static String getFormatDate(int dateFormat, Date myDate) {
    if (myDate == null)
      return "";
    String format = (dateFormat == DateFormat.LONG) ? "EEE, MMM dd, yyyy" : "MM/dd/yyyy";
    try {
      String userName = UserHelper.getCurrentUser();
      if (!isFieldEmpty(userName)) {
        org.exoplatform.forum.service.ForumService forumService = (org.exoplatform.forum.service.ForumService)
        ExoContainerContext.getCurrentContainer().getComponentInstanceOfType(org.exoplatform.forum.service.ForumService.class);
        org.exoplatform.forum.service.UserProfile profile = forumService.getUserSettingProfile(userName);
        format = (dateFormat == DateFormat.LONG) ? profile.getLongDateFormat() : profile.getShortDateFormat();
      }
    } catch (Exception e) {
      log.debug("No forum settings found for date format. Will use format " + format);
    }
    format = format.replaceAll("D", "E").replace("EEE,MMM", "EEE, MMM").replace("d,y", "d, y");
    return TimeConvertUtils.getFormatDate(myDate, format);
  }

  public static String getLongDateFormat(Date myDate) {
    return getFormatDate(DateFormat.LONG, myDate);
  }

  public static String getShortDateFormat(Date myDate) {
    return getFormatDate(DateFormat.SHORT, myDate);
  }

  public static String getUserAvatar(String userName) throws Exception {
    String url = "";
    try {
      FAQService service = getFAQService();
      FileAttachment avatar = service.getUserAvatar(userName);
      if (avatar != null) {
        url = CommonUtils.getImageUrl(avatar.getPath()) + "?size=" + avatar.getSize();
      }
    } catch (Exception e) {
      log.debug("Failed to get user avatar of user: " + userName, e);
    }
    if (url == null || url.trim().length() < 1) {
      CommonContact contact = getPersonalContact(userName);
      if (!isFieldEmpty(contact.getAvatarUrl())) {
        url = contact.getAvatarUrl();
      }
      url = (url == null || url.trim().length() < 1) ? org.exoplatform.faq.service.Utils.DEFAULT_AVATAR_URL : url;
    }
    return url;
  }
  
  public static CommonContact getPersonalContact(String userId) {
    try {
      if (userId.indexOf(Utils.DELETED) > 0)
        return new CommonContact();
      ContactProvider provider = (ContactProvider) PortalContainer.getComponent(ContactProvider.class);
      return provider.getCommonContact(userId);
    } catch (Exception e) {
      return new CommonContact();
    }
  }

  public static String getFileSource(FileAttachment attachment) throws Exception {
    DownloadService dservice = (DownloadService)PortalContainer.getComponent(DownloadService.class);
    try {
        FAQDownloadResource dresource = new FAQDownloadResource(attachment.getMimeType(), "image");
        dresource.setFileAttachment(attachment);
        dresource.setDownloadName(attachment.getName());
        return dservice.getDownloadLink(dservice.addDownloadResource(dresource));
    } catch (Exception e) {
      log.error("Can not get File Source, exception: " + e.getMessage());
    }
    return "";
  }
  /**
   * Get question uri by question id of question relative path.
   * 
   * @param: param the question id or question relative path.
   * @param: isAnswer is display form answer question or not.
   * @return: the link go to the question and show form answer or not.
   * @throws Exception
  */
  public static String getQuestionURI(String param, boolean isAnswer) throws Exception {
    PortalRequestContext portalContext = Util.getPortalRequestContext();
    String selectedNode = Util.getUIPortal().getSelectedUserNode().getURI();
    return  portalContext.getPortalURI().concat(selectedNode)
                         .concat(Utils.QUESTION_ID).concat(param).concat((isAnswer)?Utils.ANSWER_NOW.concat("true"):"");
  }
  
  public static String getLinkDiscuss(String topicId) throws Exception {
    try {
      PortalRequestContext portalContext = Util.getPortalRequestContext();
      String link = portalContext.getPortalURI().concat("forum/")
                                 .concat(org.exoplatform.forum.service.Utils.TOPIC).concat("/").concat(topicId);
      
      return link;
    } catch (Exception e) {
      log.error("Fail to get link discuss: ", e);
    }
    return "";
  }

  public static int getLimitUploadSize(boolean isAvatar) {
    PortletRequestContext pcontext = (PortletRequestContext) WebuiRequestContext.getCurrentInstance();
    PortletPreferences portletPref = pcontext.getRequest().getPreferences();
    int limitMB = DEFAULT_VALUE_UPLOAD_PORTAL;
    if (isAvatar) {
      limitMB = Integer.parseInt(portletPref.getValue(UPLOAD_AVATAR_SIZE, "").trim());
    } else {
      limitMB = Integer.parseInt(portletPref.getValue(UPLOAD_FILE_SIZE, "").trim());
    }
    return limitMB;
  }
  
  /**
   * The class use for comparator Answers by markVotes.
   * 
   * @param: isASC the type of comparator.
   *  + isASC == true: comparator by ascending
   *  + isASC == false: comparator by descending
  */
  static public class VoteComparator implements Comparator<Answer> {
    protected boolean isASC = true;

    public VoteComparator(boolean isASC) {
      this.isASC = isASC;
    }

    public int compare(Answer answer1, Answer answer2) {
      Long vote1 = answer1.getMarkVotes();
      Long vote2 = answer2.getMarkVotes();
      if (isASC) {
        return vote1.compareTo(vote2);
      } else {
        return vote2.compareTo(vote1);
      }
    }
  }
  
  public static String renderCategoryTree(CategoryTree categoryTree, BaseUIFAQForm uiForm, String actionName,  String categoryId, boolean isAddSup) throws Exception {
    StringBuilder builder = new StringBuilder();
    Category category = categoryTree.getCategory();
    if (hasPermission(category)) {
      List<CategoryTree> categoryTrees = categoryTree.getSubCategory();
      String clazz = "collapseIcon";
      if (categoryTrees.size() == 0){
        clazz = "uiIconEmpty";
      }
  
      builder.append("<a href=\"javascript:void(0);\"");
      if (isAddSup == false && category.getId().equals(categoryId) == true) {
        String warning = uiForm.i18n("UIMoveQuestionForm.msg.choice-orther");
        builder.append(" ondblclick=\"alert('").append(warning).append("');\"");
      } else {
        builder.append(" ondblclick=\"").append(uiForm.event(actionName, category.getId())).append("\"");
      }
      if(category.getId().equals(Utils.CATEGORY_HOME) == false) {
          builder.append(" class=\"uiIconNode ").append(clazz).append("\" onclick=\"eXo.answer.AnswerUtils.showTreeNode(this);\">")
                 .append("<i class=\"uiIconCategory uiIconLightGray\"></i>").append(category.getName());
      } else {
        String home = uiForm.i18n("UICategoryTree.label.home");
        builder.append(">").append("<i class=\"uiIconHome uiIconLightGray\"></i>  <span>").append(home).append("</span>");
      }
      builder.append("</a>");
  
      
      if(categoryTrees.size() > 0) {
        builder.append("<ul class=\"nodeGroup\">");
        for(CategoryTree subTree : categoryTrees) {
          if (isAddSup && subTree.getCategory().getPath().indexOf(categoryId) >= 0){
            continue;
          }
          builder.append("<li class=\"node\">");
          builder.append(renderCategoryTree(subTree, uiForm, actionName, categoryId, isAddSup));
          builder.append("</li>");
        }
        builder.append("</ul>");
      }
    }
    return builder.toString();
  }
  
  public static boolean hasPermission(Category category) {
    List<String> listOfUser = UserHelper.getAllGroupAndMembershipOfUser(null);
    if(CommonUtils.isEmpty(category.getModerators()) == false && 
        Utils.hasPermission(Arrays.asList(category.getModerators()), listOfUser)) {
      return true;
    }
    if (CommonUtils.isEmpty(category.getUserPrivate()) == false) {
      return Utils.hasPermission(Arrays.asList(category.getUserPrivate()), listOfUser);
    }
    return true;
  }
  
}
