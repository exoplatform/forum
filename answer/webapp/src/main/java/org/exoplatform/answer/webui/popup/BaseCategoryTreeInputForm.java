/*
 * Copyright (C) 2003-2013 eXo Platform SAS.
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.exoplatform.answer.webui.BaseUIFAQForm;
import org.exoplatform.answer.webui.FAQUtils;
import org.exoplatform.faq.service.Category;
import org.exoplatform.faq.service.CategoryTree;
import org.exoplatform.faq.service.Utils;
import org.exoplatform.webui.form.UIFormInputWithActions;
import org.exoplatform.webui.form.input.UICheckBoxInput;

/**
 * Created by The eXo Platform SAS
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * Apr 12, 2013  
 */
public class BaseCategoryTreeInputForm extends BaseUIFAQForm {
  public static final String  CATEGORY_SCOPING                 = "CategoryScoping";
  
  protected Map<String, Boolean> categoryStatus                  = new HashMap<String, Boolean>();

  protected Map<String, String> categoryMap                      = new HashMap<String, String>();
  
  protected List<String>         categoriesChecked               = null;
  
  protected CategoryTree        categoryTree                     = new CategoryTree();

  public BaseCategoryTreeInputForm() {
    UIFormInputWithActions categoriesMainTab = new UIFormInputWithActions(CATEGORY_SCOPING);
    addChild(categoriesMainTab);
  }
  
  protected String renderCategoryTree() throws Exception {
    return renderCategoryTree(this.categoryTree);
  }
  
  private String renderCategoryTree(CategoryTree categoryTree) throws Exception {
    UIFormInputWithActions categoryScoping = getChildById(CATEGORY_SCOPING);
    StringBuilder builder = new StringBuilder();
    Category category = categoryTree.getCategory();
    List<CategoryTree> categoryTrees = categoryTree.getSubCategory();
    String clazz = "collapseIcon";
    if (categoryTrees.size() == 0){
      clazz = "uiIconEmpty";
    }
    
    if(category.getId().equals(Utils.CATEGORY_HOME) == false) {
      boolean isChecked = category.isView();
      if(categoriesChecked != null) {
        isChecked = categoriesChecked.isEmpty() || categoriesChecked.contains(category.getId());
      }
      categoryMap.put(category.getId(), category.getPath());
      categoryStatus.put(category.getId(), Boolean.valueOf(isChecked));
      UICheckBoxInput checkBoxInput = categoryScoping.getUICheckBoxInput(category.getId());
      if (checkBoxInput == null) {
        checkBoxInput = new UICheckBoxInput(category.getId(), category.getId(), isChecked);
        categoryScoping.addUIFormInput(checkBoxInput);
      }
      checkBoxInput.setValue(isChecked);

      builder.append("<a href=\"javascript:void(0);\" class=\"uiIconNode ").append(clazz).append("\" onclick=\"eXo.answer.AnswerUtils.showTreeNode(this);\">")
             .append("  <span class=\"uiCheckbox\">")
             .append("    <input onclick=\"eXo.answer.AnswerUtils.checkedNode(this)\" type=\"checkbox\" ")
             .append(isChecked ? "checked " : "").append("name=\"").append(category.getId())
             .append("\" class=\"checkbox\" id=\"").append(category.getId()).append("\">")
             .append("    <span><i class=\"uiIconCategory uiIconLightGray\"></i> ").append(FAQUtils.getSubString(category.getName(), 20)).append("</span>")
             .append("  </span>");
      builder.append("</a>");
    }

    if (categoryTrees.size() > 0) {
      builder.append("<ul class=\"nodeGroup\">");
      for (CategoryTree subTree : categoryTrees) {
        builder.append("<li class=\"node\">");
        builder.append(renderCategoryTree(subTree));
        builder.append("</li>");
      }
      builder.append("</ul>");
    }

    return builder.toString();
  }
}
