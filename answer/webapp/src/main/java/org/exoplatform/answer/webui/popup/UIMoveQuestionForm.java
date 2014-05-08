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
import java.util.List;

import org.exoplatform.answer.webui.BaseUIFAQForm;
import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.UIAnswersContainer;
import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.answer.webui.UIQuestions;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.CategoryTree;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.UIPopupAction;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

@ComponentConfig(
    lifecycle = UIFormLifecycle.class, 
    template = "app:/templates/answer/webui/popup/UIMoveForm.gtmpl", 
    events = {
        @EventConfig(listeners = UIMoveQuestionForm.MoveActionListener.class), 
        @EventConfig(listeners = UIMoveQuestionForm.CancelActionListener.class) 
    }
)
public class UIMoveQuestionForm extends BaseUIFAQForm implements UIPopupComponent {
  private String            questionId_      = "";

  private String            categoryId_;

  private FAQSetting faqSetting_;


  public UIMoveQuestionForm() throws Exception {
  }

  protected CategoryTree getCategoryTree() throws Exception {
    return getFAQService().buildCategoryTree(null);
  }

  protected String renderCategoryTree(CategoryTree categoryTree) throws Exception {
    return FAQUtils.renderCategoryTree(categoryTree, this, "Move", categoryId_, false);
  }
  
  public void activate() {
  }

  public void deActivate() {
  }

  public void setQuestionId(String questionId) throws Exception {
    this.questionId_ = questionId;
    Question question = getFAQService().getQuestionById(questionId_);
    this.categoryId_ = question.getCategoryId();
  }

  public void setFAQSetting(FAQSetting faqSetting) {
    this.faqSetting_ = faqSetting;
  }

  static public class MoveActionListener extends BaseEventListener<UIMoveQuestionForm> {
    public void onEvent(Event<UIMoveQuestionForm> event, UIMoveQuestionForm moveQuestionForm, String destCategoryId) throws Exception {
      UIAnswersPortlet portlet = moveQuestionForm.getAncestorOfType(UIAnswersPortlet.class);
      UIQuestions questions = portlet.getChild(UIAnswersContainer.class).getChild(UIQuestions.class);
      try {
        Category category = moveQuestionForm.getFAQService().getCategoryById(destCategoryId);
        if (!moveQuestionForm.faqSetting_.isAdmin() && !questions.getFAQService().isCategoryModerator(category.getPath(), null)) {
          warning("UIQuestions.msg.can-not-move-question");
          return;
        }
        try {
          Question question = questions.getFAQService().getQuestionById(moveQuestionForm.questionId_);
          String cateId = category.getId();
          if (cateId.equals(question.getCategoryId())) {
            warning("UIMoveQuestionForm.msg.choice-orther");
            return;
          }
          question.setCategoryId(cateId);
          question.setCategoryPath(category.getPath());
          String link = FAQUtils.getQuestionURL(new StringBuffer(category.getPath()).append("/").
                                                append(Utils.QUESTION_HOME).append("/").append(question.getId()).toString(), false);
          FAQUtils.getEmailSetting(moveQuestionForm.faqSetting_, false, false);
          FAQUtils.getEmailMoveQuestion(moveQuestionForm.faqSetting_);
          List<String> questionList = new ArrayList<String>();
          questionList.add(question.getPath());
          questions.getFAQService().moveQuestions(questionList, category.getPath(), link, moveQuestionForm.faqSetting_);
          questions.updateCurrentQuestionList();
        } catch (Exception e) {
          moveQuestionForm.log.warn("Can not move this question. Exception: " + e.getMessage());
          warning("UIQuestions.msg.question-id-deleted", false);
        }
      } catch (Exception e) {
        warning("UIQuestions.msg.category-id-deleted", false);
      }
      UIPopupAction popupAction = portlet.getChild(UIPopupAction.class);
      questions.setDefaultLanguage();
      event.getRequestContext().addUIComponentToUpdateByAjax(questions);
      popupAction.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }

  static public class CancelActionListener extends EventListener<UIMoveQuestionForm> {
    public void execute(Event<UIMoveQuestionForm> event) throws Exception {
      UIMoveQuestionForm moveQuestionForm = event.getSource();
      UIAnswersPortlet portlet = moveQuestionForm.getAncestorOfType(UIAnswersPortlet.class);
      UIPopupAction popupAction = portlet.getChild(UIPopupAction.class);
      popupAction.deActivate();
      event.getRequestContext().addUIComponentToUpdateByAjax(popupAction);
    }
  }
}
