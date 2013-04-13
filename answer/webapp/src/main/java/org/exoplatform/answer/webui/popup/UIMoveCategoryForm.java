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
package org.exoplatform.answer.webui.popup;

import java.util.Arrays;

import javax.jcr.ItemExistsException;

import org.exoplatform.answer.webui.BaseUIFAQForm;
import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.UIAnswersContainer;
import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.answer.webui.UIBreadcumbs;
import org.exoplatform.answer.webui.UICategories;
import org.exoplatform.answer.webui.UIQuestions;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.CategoryTree;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.BaseEventListener;
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
        @EventConfig(listeners = UIMoveCategoryForm.MoveActionListener.class), 
        @EventConfig(listeners = UIMoveCategoryForm.CancelActionListener.class) 
    }
)
public class UIMoveCategoryForm extends BaseUIFAQForm implements UIPopupComponent {
  private String            categoryId_;

  private FAQSetting        faqSetting_;

  private boolean           isCateSelect = false;

  public UIMoveCategoryForm() throws Exception {
  }

  public void setCategoryId(String categoryId) {
    categoryId_ = categoryId;
  }

  public void activate() {
  }

  public void deActivate() {
  }

  protected CategoryTree getCategoryTree() throws Exception {
    return getFAQService().buildCategoryTree(null);
  }

  protected String renderCategoryTree(CategoryTree categoryTree) throws Exception {
    return FAQUtils.renderCategoryTree(categoryTree, this, "Move", categoryId_, true);
  }
  
  public void setIsCateSelect(boolean isCateSelect) {
    this.isCateSelect = isCateSelect;
  }

  public void setFAQSetting(FAQSetting faqSetting) {
    this.faqSetting_ = faqSetting;
  }

  static public class MoveActionListener extends BaseEventListener<UIMoveCategoryForm> {
    public void onEvent(Event<UIMoveCategoryForm> event, UIMoveCategoryForm moveCategory, String destCategoryId) throws Exception {
      UIAnswersPortlet answerPortlet = moveCategory.getAncestorOfType(UIAnswersPortlet.class);
      String categoryId = moveCategory.categoryId_;
      try {
        Category category = moveCategory.getFAQService().getCategoryById(destCategoryId);
        boolean canMove = moveCategory.faqSetting_.isAdmin();
        if (!canMove){
          canMove = Utils.hasPermission(Arrays.asList(category.getModerators()), UserHelper.getAllGroupAndMembershipOfUser(null));
        }
        if (canMove) {
          if (!moveCategory.getFAQService().isCategoryExist(moveCategory.getFAQService().getCategoryNameOf(categoryId), category.getPath())) {
            moveCategory.getFAQService().moveCategory(categoryId, category.getPath());
          } else {
            warning("UIQuestions.msg.can-not-move-category-same-name");
            return;
          }
        } else {
          warning("UIQuestions.msg.can-not-move-category");
          return;
        }
        if (moveCategory.isCateSelect) {
          String tmp = moveCategory.categoryId_;
          if (tmp.indexOf("/") > 0)
            tmp = tmp.substring(0, tmp.lastIndexOf("/"));
          UIAnswersContainer container = answerPortlet.findFirstComponentOfType(UIAnswersContainer.class);
          UICategories uiCategories = container.findFirstComponentOfType(UICategories.class);
          uiCategories.setPathCategory(tmp);
          UIQuestions questions = container.getChild(UIQuestions.class);
          questions.pageSelect = 0;
          questions.backPath_ = "";
          questions.setLanguage(FAQUtils.getDefaultLanguage());
          try {
            questions.viewAuthorInfor = moveCategory.getFAQService().isViewAuthorInfo(tmp);
            questions.setCategoryId(tmp);
            questions.updateCurrentQuestionList();
            questions.viewingQuestionId_ = "";
            questions.updateCurrentLanguage();
          } catch (Exception e) {
            if (moveCategory.log.isDebugEnabled()) {
              moveCategory.log.debug("Failed to update question form", e);
            }
          }
          UIBreadcumbs breadcumbs = answerPortlet.findFirstComponentOfType(UIBreadcumbs.class);
          breadcumbs.setUpdataPath(tmp);
        }
        moveCategory.isCateSelect = false;
      } catch (ItemExistsException ie) {
        warning("UIQuestions.msg.already-in-destination", false);
      } catch (Exception e) {
        moveCategory.log.warn("Can not move this category. Exception: " + e.getMessage());
        warning("UIQuestions.msg.category-id-deleted", false);
      }
      event.getRequestContext().addUIComponentToUpdateByAjax(answerPortlet);
      answerPortlet.cancelAction();
    }
  }

  static public class CancelActionListener extends EventListener<UIMoveCategoryForm> {
    public void execute(Event<UIMoveCategoryForm> event) throws Exception {
      UIAnswersPortlet answerPortlet = event.getSource().getAncestorOfType(UIAnswersPortlet.class);
      answerPortlet.cancelAction();
    }
  }
}
