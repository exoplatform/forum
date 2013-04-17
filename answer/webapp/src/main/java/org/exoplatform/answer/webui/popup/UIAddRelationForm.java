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
import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.CategoryTree;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.faq.service.Question;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;
import org.exoplatform.webui.form.input.UICheckBoxInput;

@ComponentConfig(
     lifecycle = UIFormLifecycle.class, 
     template = "app:/templates/answer/webui/popup/UIAddRelationForm.gtmpl", 
     events = {
          @EventConfig(listeners = UIAddRelationForm.SaveActionListener.class),
          @EventConfig(listeners = UIAddRelationForm.CancelActionListener.class) 
})
public class UIAddRelationForm extends BaseUIFAQForm implements UIPopupComponent {
  private List<Question> listQuestion  = new ArrayList<Question>();

  private List<String>   quesIdsSelect = new ArrayList<String>();

  private String         questionId_;

  private FAQSetting     faqSetting_   = new FAQSetting();

  private CategoryTree   categoryTree  = null;

  public void activate() {
  }

  public void deActivate() {
  }

  public UIAddRelationForm() throws Exception {
    FAQUtils.getPorletPreference(faqSetting_);
    getFAQService().getUserSetting(FAQUtils.getCurrentUser(), faqSetting_);
    setActions(new String[] { "Save", "Cancel" });
  }

  public void setFAQSetting(FAQSetting faqSetting) {
    this.faqSetting_ = faqSetting;
  }

  public void setRelationed(List<String> listRelation) {
    quesIdsSelect = listRelation;
  }

  public void setQuestionId(String questionId) {
    this.questionId_ = questionId;
    try {
      this.categoryTree = getFAQService().buildCategoryTree(null);
    } catch (Exception e) {
      this.categoryTree = new CategoryTree();
    }
  }

  protected String renderCategoryTree() throws Exception {
    listQuestion.clear();
    return renderCategoryTree(categoryTree);
  }

  private String renderCategoryTree(CategoryTree categoryTree) throws Exception {
    StringBuilder builder = new StringBuilder();
    Category category = categoryTree.getCategory();
    String categoryId = category.getId();
    if (FAQUtils.hasPermission(category)) {
      List<CategoryTree> categoryTrees = categoryTree.getSubCategory();
      List<Question> questions = getQuestionsByCategoryId(categoryId, faqSetting_);
      String clazz = "collapseIcon";
      if (categoryTrees.size() == 0 && questions.size() == 0){
        clazz = "uiIconEmpty";
      }
      builder.append("<a href=\"javascript:void(0);\"");
      if (categoryId.equals(Utils.CATEGORY_HOME) == false) {
        builder.append(" class=\"uiIconNode ").append(clazz).append("\" onclick=\"eXo.answer.AnswerUtils.showTreeNode(this);\">")
               .append("<i class=\"uiIconCategory uiIconLightGray\"></i>")
               .append(category.getName());
      } else {
        String home = this.i18n("UICategoryTree.label.home");
        builder.append(">").append("<i class=\"uiIconHome uiIconLightGray\"></i> <span>").append(home).append("</span>");
      }
      builder.append("</a>");

      listQuestion.addAll(questions);

      if (categoryTrees.size() > 0 || questions.size() > 0) {
        builder.append("<ul class=\"nodeGroup\" style=\"display: block; \">");
        for (Question question : questions) {
          if (!questionId_.equals(question.getPath())) {
            boolean isChecked = false;
            if (quesIdsSelect.contains(question.getId())) {
              isChecked = true;
            }
            addUIFormInput(new UICheckBoxInput(question.getId(), question.getId(), isChecked));
            builder.append("<li class=\"node\">")
                   .append("<span class=\"uiCheckbox mgl0\"><input name=\"")
                   .append(question.getId())
                   .append("\" id=\"")
                   .append(question.getId())
                   .append("\" type=\"checkbox\"")
                   .append((isChecked == true) ? " checked" : "")
                   .append("/><span>")
                   .append(question.getQuestion())
                   .append("</span></span>");
            builder.append("</li>");
          }
        }
        for (CategoryTree subTree : categoryTrees) {
          builder.append("<li class=\"node\">");
          builder.append(renderCategoryTree(subTree));
          builder.append("</li>");
        }
        builder.append("</ul>");
      }
    }
    return builder.toString();
  }

  private List<Question> getQuestionsByCategoryId(String categoryId, FAQSetting faqSetting) throws Exception {
    List<Question> listQuestions = new ArrayList<Question>();
    listQuestions = getFAQService().getAllQuestionsByCatetory(categoryId, faqSetting).getAll();
    return listQuestions;
  }

  static public class SaveActionListener extends EventListener<UIAddRelationForm> {
    public void execute(Event<UIAddRelationForm> event) throws Exception {
      UIAddRelationForm addRelationForm = event.getSource();
      UIResponseForm responseForm = addRelationForm.getAncestorOfType(UIAnswersPortlet.class).findFirstComponentOfType(UIResponseForm.class);
      List<String> listQuestionPath = new ArrayList<String>();
      List<String> listQuestionId = new ArrayList<String>();
      for (Question question : addRelationForm.listQuestion) {
        UICheckBoxInput chkRelated = addRelationForm.getUICheckBoxInput(question.getId());
        if (chkRelated != null && chkRelated.isChecked()) {
          listQuestionPath.add(question.getPath());
          listQuestionId.add(question.getId());
        }
      }
      responseForm.setListIdQuesRela(listQuestionId);
      List<String> contents = addRelationForm.getFAQService().getQuestionContents(listQuestionPath);
      responseForm.setListRelationQuestion(contents);
      event.getRequestContext().addUIComponentToUpdateByAjax(responseForm);
      addRelationForm.listQuestion.clear();
      addRelationForm.cancelChildPopupAction();
    }
  }

  static public class CancelActionListener extends EventListener<UIAddRelationForm> {
    public void execute(Event<UIAddRelationForm> event) throws Exception {
      UIAddRelationForm addRelationForm = event.getSource();
      addRelationForm.listQuestion.clear();
      addRelationForm.cancelChildPopupAction();
    }
  }
}
