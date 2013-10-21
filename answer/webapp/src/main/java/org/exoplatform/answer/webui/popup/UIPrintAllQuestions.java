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
package org.exoplatform.answer.webui.popup;

import java.util.ArrayList;
import java.util.List;

import org.exoplatform.answer.rendering.RenderHelper;
import org.exoplatform.answer.rendering.RenderingException;
import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.faq.service.Answer;
import org.exoplatform.faq.service.Comment;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.faq.service.Question;
import org.exoplatform.forum.common.CommonUtils;
import org.exoplatform.forum.common.webui.BaseUIForm;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.cssfile.CssClassUtils;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class, 
    template = "app:/templates/answer/webui/popup/UIPrintAllQuestions.gtmpl", 
    events = {
        @EventConfig(listeners = UIPrintAllQuestions.CloseActionListener.class) 
    }
)
public class UIPrintAllQuestions extends BaseUIForm implements UIPopupComponent {
  private String[]     sizes_          = new String[] { "bytes", "KB", "MB" };

  private String       categoryId      = null;
  
  private Question     question        = null;

  protected String     currentUser_;

  private boolean      canEditQuestion = false;

  private FAQService   faqService_     = null;

  private FAQSetting   faqSetting_     = null;

  protected boolean    viewAuthorInfor = true;

  private RenderHelper renderHelper    = new RenderHelper();
  
  public void activate() {
  }

  public void deActivate() {
  }

  public UIPrintAllQuestions() {
    try {
      currentUser_ = FAQUtils.getCurrentUser();
    } catch (Exception e) {
      log.debug("Current user must exist: ", e);
    }
  }

  protected String getCSSByFileType(String fileName, String fileType) {
    return CssClassUtils.getCSSClassByFileNameAndFileType(fileName, fileType, null);
  }

  protected String getQuestionRelationById(String questionId) {
    try {
      Question question = faqService_.getQuestionById(questionId);
      if (question != null) {
        return question.getCategoryId() + "/" + question.getId() + "/" + question.getQuestion();
      }
    } catch (Exception e) {
      log.error("Can not get Question Relation by Id, exception: " + e.getMessage());
    }
    return "";
  }

  public String getImageUrl(String imagePath) throws Exception {
    String url = "";
    try {
      url = CommonUtils.getImageUrl(imagePath);
    } catch (Exception e) {
      log.debug("Image must exist: ", e);
    }
    return url;
  }

  protected String getAvatarUrl(String userId) throws Exception {
    return FAQUtils.getUserAvatar(userId);
  }

  protected String convertSize(long size) {
    String result = "";
    long residual = 0;
    int i = 0;
    while (size >= 1000) {
      i++;
      residual = size % 1024;
      size /= 1024;
    }
    if (residual > 500) {
      result = (size + 1) + " " + sizes_[i];
    } else {
      result = size + " " + sizes_[i];
    }
    return result;
  }

  public void setCategoryId(String cateId, FAQService service, FAQSetting setting, boolean canEdit, Question question) throws Exception {
    categoryId = cateId;
    this.question = question;
    faqService_ = service;
    faqSetting_ = setting;
    viewAuthorInfor = faqService_.isViewAuthorInfo(categoryId);
    canEditQuestion = faqSetting_.isAdmin();
    if (!canEditQuestion)
      canEditQuestion = canEdit;
  }

  public String render(Object obj) throws RenderingException {
    if (obj instanceof Question)
      return renderHelper.renderQuestion((Question) obj);
    else if (obj instanceof Answer)
      return renderHelper.renderAnswer((Answer) obj);
    else if (obj instanceof Comment)
      return renderHelper.renderComment((Comment) obj);
    return CommonUtils.EMPTY_STR;
  }

  public List<Question> getListQuestion() {
    List<Question> list = new ArrayList<Question>();
    if (question == null) {
      try {
        return faqService_.getQuestionsByCatetory(categoryId, faqSetting_).getAll();
      } catch (Exception e) {
        return list;
      }
    } else {
      list.add(question);
      return list;
    }
  }

  public List<Answer> getListAnswers(String questionId) {
    try {
      return faqService_.getPageListAnswer(questionId, false).getPageItem(0);
    } catch (Exception e) {
      return new ArrayList<Answer>();
    }
  }

  public List<Comment> getListComments(String questionId) {
    try {
      return faqService_.getPageListComment(questionId).getPageItem(0);
    } catch (Exception e) {
      return new ArrayList<Comment>();
    }
  }

  static public class CloseActionListener extends EventListener<UIPrintAllQuestions> {
    public void execute(Event<UIPrintAllQuestions> event) throws Exception {
      UIPrintAllQuestions uiForm = event.getSource();
      UIAnswersPortlet portlet = uiForm.getAncestorOfType(UIAnswersPortlet.class);
      UIPopupAction popupAction = portlet.getChild(UIPopupAction.class);
      popupAction.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(portlet);
    }
  }
}
