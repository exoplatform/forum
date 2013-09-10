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
package org.exoplatform.answer.webui.popup;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.UIAnswersContainer;
import org.exoplatform.answer.webui.UIAnswersPageIterator;
import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.answer.webui.UIBreadcumbs;
import org.exoplatform.answer.webui.UICategories;
import org.exoplatform.answer.webui.UIQuestions;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.faq.service.JCRPageList;
import org.exoplatform.faq.service.Question;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.web.application.ApplicationMessage;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIContainer;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    template = "app:/templates/answer/webui/popup/UIUserWatchManager.gtmpl", 
    events = {
        @EventConfig(listeners = UIUserWatchManager.LinkActionListener.class), 
        @EventConfig(listeners = UIUserWatchManager.UnWatchActionListener.class, 
                      confirm = "UIUserWatchManager.msg.confirm-unwatch-category") 
    }
)
public class UIUserWatchManager extends UIContainer {
  private FAQSetting              faqSetting_            = null;

  protected UIAnswersPageIterator pageIteratorCate;

  protected JCRPageList           pageListCate;

  private UIAnswersPageIterator   pageIteratorQues;

  private UIAnswersPageIterator   pageIteratorCates;

  private JCRPageList             pageListQues;

  private JCRPageList             pageListCates;

  private String                  LIST_QUESTIONS_WATCHED = "listQuestionsWatch";

  private String                  LIST_CATES_WATCHED     = "listCatesWatch";

  private static String          FULL_PATH_NAME         = "fullPathName";

  private static String          SUB_PATH_NAME          = "subPathName";

  private String                  emailAddress;

  private static FAQService      faqService_;

  public UIUserWatchManager() throws Exception {
    setId("UIUswerWatchManager");
    addChild(UIAnswersPageIterator.class, null, LIST_QUESTIONS_WATCHED);
    addChild(UIAnswersPageIterator.class, null, LIST_CATES_WATCHED);
    emailAddress = FAQUtils.getEmailUser(null);
    faqService_ = CommonUtils.getComponent(FAQService.class);
  }

  protected String getEmailAddress() {
    return emailAddress;
  }

  protected static Map<String, String> getCategoryPathName(String categoryPath) throws Exception {
    String pathName = faqService_.getParentCategoriesName(categoryPath);
    Map<String, String> dataPathName = new HashMap<String, String>();
    if (FAQUtils.isFieldEmpty(pathName) == false) {
      dataPathName.put(FULL_PATH_NAME, FAQUtils.getCategoryPathName(pathName, false));
      String[] paths = pathName.split(" > ");
      if (paths.length > 3) {
        pathName = new StringBuffer(paths[0]).append(" > ... > ").append(paths[paths.length - 1]).toString();
      }
      dataPathName.put(SUB_PATH_NAME, FAQUtils.getCategoryPathName(pathName, true));
    } else {
      dataPathName.put(FULL_PATH_NAME, "");
      dataPathName.put(SUB_PATH_NAME, "");
    }
    return dataPathName;
  }

  public void setFAQSetting(FAQSetting setting) {
    this.faqSetting_ = setting;
  }

  protected List<Category> getListCategory() {
    try {
      if (pageListCates == null) {
        pageListCates = faqService_.getWatchedCategoryByUser(FAQUtils.getCurrentUser());
        pageListCates.setPageSize(5);
        pageIteratorCates = this.getChildById(LIST_CATES_WATCHED);
        pageIteratorCates.updatePageList(pageListCates);
      }
      long pageSelect = pageIteratorCates.getPageSelected();        
      
      List<Category> listCategories = new ArrayList<Category>();
      listCategories.addAll(this.pageListCates.getPageResultCategoriesSearch(pageSelect, null));
      if (listCategories.isEmpty()) {
        UIAnswersPageIterator pageIterator = null;
        while (listCategories.isEmpty() && pageSelect > 1) {
          pageIterator = this.getChildById(LIST_CATES_WATCHED);
          listCategories.addAll(this.pageListCates.getPageResultCategoriesSearch(--pageSelect, null));
          pageIterator.setSelectPage(pageSelect);
        }
      }
      return listCategories;
    } catch (Exception e) {
      return null;
    }
  }

  protected List<Question> getListQuestionsWatch() {
    try {
      if (pageListQues == null) {
        pageListQues = faqService_.getListQuestionsWatch(faqSetting_, FAQUtils.getCurrentUser());
        pageListQues.setPageSize(5);
        pageIteratorQues = this.getChildById(LIST_QUESTIONS_WATCHED);
        pageIteratorQues.updatePageList(pageListQues);
      }

      long pageSelect = pageIteratorQues.getPageSelected();
      List<Question> listQuestion_ = new ArrayList<Question>();
      listQuestion_.addAll(this.pageListQues.getPage(pageSelect, null));
      if (listQuestion_.isEmpty()) {        
        
        UIAnswersPageIterator pageIterator = null;
        while (listQuestion_.isEmpty() && pageSelect > 1) {
          pageIterator = this.getChildById(LIST_QUESTIONS_WATCHED);
          listQuestion_.addAll(this.pageListQues.getPage(--pageSelect, null));
          pageIterator.setSelectPage(pageSelect);
        }
      }
      return listQuestion_;
    } catch (Exception e) {
      return null;
    }
  }

  protected long getTotalpages(String pageInteratorId) {
    UIAnswersPageIterator pageIterator = this.getChildById(pageInteratorId);
    try {
      return pageIterator.getInfoPage().get(3);
    } catch (Exception e) {
      return 1;
    }
  }
  
  private static void warning(WebuiRequestContext ctx, String msgKey) {
    ctx.getUIApplication().addMessage(new ApplicationMessage(msgKey, null, ApplicationMessage.WARNING));
  }

  static public class LinkActionListener extends EventListener<UIUserWatchManager> {
    public void execute(Event<UIUserWatchManager> event) throws Exception {
      UIUserWatchManager watchManager = event.getSource();
      WebuiRequestContext ctx = event.getRequestContext();
      String categoryId = ctx.getRequestParameter(OBJECTID);
      UIAnswersPortlet uiPortlet = watchManager.getAncestorOfType(UIAnswersPortlet.class);
      UIAnswersContainer uiContainer = uiPortlet.getChild(UIAnswersContainer.class);
      UIQuestions uiQuestions = uiContainer.getChild(UIQuestions.class);
      if (faqService_.isExisting(categoryId) == false) {
        warning(ctx, "UIQuestions.msg.category-id-deleted");
        uiQuestions.setDefaultLanguage();
        uiPortlet.getChild(UIPopupAction.class).deActivate();
        ctx.addUIComponentToUpdateByAjax(uiPortlet);
        return;
      }
      uiQuestions.setCategoryId(categoryId);
      uiContainer.getChild(UIBreadcumbs.class).setUpdataPath(categoryId);
      uiContainer.getChild(UICategories.class).setPathCategory(categoryId);
      ctx.addUIComponentToUpdateByAjax(uiContainer);
    }
  }

  static public class UnWatchActionListener extends EventListener<UIUserWatchManager> {
    public void execute(Event<UIUserWatchManager> event) throws Exception {
      UIUserWatchManager watchManager = event.getSource();
      WebuiRequestContext ctx = event.getRequestContext();
      String categoryId = ctx.getRequestParameter(OBJECTID);
      if (faqService_.isExisting(categoryId) == false) {
        warning(ctx, "UIQuestions.msg.category-id-deleted");
        UIAnswersPortlet uiPortlet = watchManager.getAncestorOfType(UIAnswersPortlet.class);
        uiPortlet.findFirstComponentOfType(UIQuestions.class).setDefaultLanguage();
        ctx.addUIComponentToUpdateByAjax(uiPortlet);
        return;
      }
      faqService_.unWatchCategory(categoryId, FAQUtils.getCurrentUser());
      event.getRequestContext().addUIComponentToUpdateByAjax(watchManager);
    }
  }
}
