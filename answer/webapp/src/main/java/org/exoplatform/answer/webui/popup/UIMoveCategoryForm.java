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
import java.util.List;

import javax.jcr.ItemExistsException;

import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.answer.webui.UIAnswersContainer;
import org.exoplatform.answer.webui.UIAnswersPortlet;
import org.exoplatform.answer.webui.UIBreadcumbs;
import org.exoplatform.answer.webui.UICategories;
import org.exoplatform.answer.webui.UIQuestions;
import org.exoplatform.container.PortalContainer;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.CategoryTree;
import org.exoplatform.faq.service.FAQService;
import org.exoplatform.faq.service.FAQSetting;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.forum.common.UserHelper;
import org.exoplatform.forum.common.webui.BaseEventListener;
import org.exoplatform.forum.common.webui.BaseUIForm;
import org.exoplatform.webui.config.annotation.ComponentConfig;
import org.exoplatform.webui.config.annotation.EventConfig;
import org.exoplatform.webui.core.UIPopupComponent;
import org.exoplatform.webui.core.lifecycle.UIFormLifecycle;
import org.exoplatform.webui.event.Event;
import org.exoplatform.webui.event.EventListener;

/**
 * Created by The eXo Platform SARL
 * Author : Hung Nguyen
 *          hung.nguyen@exoplatform.com 
 * Aus 01, 2007 2:48:18 PM
 */

@ComponentConfig(
    lifecycle = UIFormLifecycle.class, 
    template = "app:/templates/answer/webui/popup/UIMoveCategoryForm.gtmpl", 
    events = {
        @EventConfig(listeners = UIMoveCategoryForm.SaveActionListener.class), 
        @EventConfig(listeners = UIMoveCategoryForm.CancelActionListener.class) 
    }
)
public class UIMoveCategoryForm extends BaseUIForm implements UIPopupComponent {
  private String            categoryId_;

  private FAQSetting        faqSetting_;

  private boolean           isCateSelect = false;

  private static FAQService faqService_;

  public UIMoveCategoryForm() throws Exception {
    faqService_  = (FAQService) PortalContainer.getInstance().getComponentInstanceOfType(FAQService.class);
  }

  public void setCategoryId(String categoryId) {
    categoryId_ = categoryId;
  }

  public void activate() {
  }

  public void deActivate() {
  }

  protected CategoryTree getCategoryTree() throws Exception {
    return faqService_.buildCategoryTree(null);
  }

  protected String renderCategoryTree(CategoryTree categoryTree) throws Exception {
    StringBuilder builder = new StringBuilder();
    Category category = categoryTree.getCategory();
    builder.append("<a href=\"javascript:void(0);\"")
           .append(" ondblclick=\"").append(event("Save", category.getId())).append("\"");
    if(category.getId().equals(Utils.CATEGORY_HOME) == false) {
        builder.append(" class=\"uiIconNode collapseIcon\" onclick=\"eXo.answer.UIAnswersPortlet.showTreeNode(this);\">")
               .append("<i class=\"uiIconCategory uiIconLightGray\"></i>").append(category.getName());
    } else {
      builder.append(">").append("<i class=\"uiIconHome uiIconLightGray\"></i>");
    }
    builder.append("</a>");

    List<CategoryTree> categoryTrees = categoryTree.getSubCategory();
    if(categoryTrees.size() > 0) {
      builder.append("<ul class=\"nodeGroup\">");
      for(CategoryTree subTree : categoryTrees) {
        if (subTree.getCategory().getPath().indexOf(categoryId_) >= 0)
          continue;
        builder.append("<li class=\"node\">");
        builder.append(renderCategoryTree(subTree));
        builder.append("</li>");
      }
      builder.append("</ul>");
    }
    
    return builder.toString();
  }
  
  public void setIsCateSelect(boolean isCateSelect) {
    this.isCateSelect = isCateSelect;
  }

  public void setFAQSetting(FAQSetting faqSetting) {
    this.faqSetting_ = faqSetting;
  }

  static public class SaveActionListener extends BaseEventListener<UIMoveCategoryForm> {
    public void onEvent(Event<UIMoveCategoryForm> event, UIMoveCategoryForm moveCategory, String destCategoryId) throws Exception {
      UIAnswersPortlet answerPortlet = moveCategory.getAncestorOfType(UIAnswersPortlet.class);
      String categoryId = moveCategory.categoryId_;
      try {
        Category category = faqService_.getCategoryById(destCategoryId);
        boolean canMove = moveCategory.faqSetting_.isAdmin();
        if (!canMove){
          canMove = Utils.hasPermission(Arrays.asList(category.getModerators()), UserHelper.getAllGroupAndMembershipOfUser(null));
        }
        if (canMove) {
          if (!faqService_.isCategoryExist(faqService_.getCategoryNameOf(categoryId), category.getPath())) {
            faqService_.moveCategory(categoryId, destCategoryId);
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
            questions.viewAuthorInfor = faqService_.isViewAuthorInfo(tmp);
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
