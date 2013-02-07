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
package org.exoplatform.forum.common.webui;

import java.io.Writer;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputBase;

/**
 * Created by The eXo Platform SAS
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * Feb 5, 2013  
 */
public class UIFormScrollSelectBox extends UIFormInputBase<String> {

  private String                         onchange_         = null;

  /**
   * The size of the list (number of select options)
   */
  private int                            displayNumber_             = 10;

  /**
   * The list of options
   */
  private List<SelectItemOption<String>> options_;

  public UIFormScrollSelectBox() {
  }

  public UIFormScrollSelectBox(String name, String bindingExpression) {
    super(name, bindingExpression, String.class);
  }

  public UIFormScrollSelectBox(String name, String bindingExpression, String defaultValue) {
    super(name, bindingExpression, String.class);
    this.value_ = defaultValue;
    resetSelect();
  }

  public UIFormScrollSelectBox(String name, String bindingExpression, List<SelectItemOption<String>> options) {
    super(name, bindingExpression, null);
    setOptions(options);
    resetSelect();
  }

  final public UIFormScrollSelectBox setMaxDisplayOption(int maxDisplayOption) {
    displayNumber_ = maxDisplayOption;
    return this;
  }

  public int getMaxDisplayOption() {
    return displayNumber_;
  }

  public UIFormScrollSelectBox setValue(String value) {
    value_ = value;
    resetSelect();
    return this;
  }

  final public List<SelectItemOption<String>> getOptions() {
    return options_;
  }

  final public UIFormScrollSelectBox setOptions(List<SelectItemOption<String>> options) {
    options_ = options;
    if (options_ == null || options_.size() < 1)
      return this;
    value_ = options_.get(0).getValue();
    resetSelect();
    return this;
  }
  
  public String getOnchange() {
    return onchange_;
  }

  public UIFormScrollSelectBox setOnChange(String onchange) {
    this.onchange_ = onchange;
    return this;
  }

  protected String renderOnChangeEvent(UIForm uiForm) throws Exception {
    if (onchange_ != null && onchange_.length() > 0) {
      return uiForm.event(onchange_, (String) getId());
    }
    return "";
  }
  
  @Override
  public void decode(Object input, WebuiRequestContext context) {
    String val = (String)input;
    if ((val == null || val.length() == 0)) {
      value_ = null;
      return;
    }
    value_ = val;
    resetSelect();
    
  }
  
  private void resetSelect() {
    if (value_ == null || value_.length() == 0 && options_.size() > 0) {
      options_.get(0).setSelected(true);
    } else {
      for (SelectItemOption<String> item : options_) {
        item.setSelected(item.getValue().equals(value_) ? true : false);
      }
    }
  }
  
  public String getSelectedValue() {
    SelectItemOption<String> option = getSelectedItem();
    return (option != null) ? option.getValue() : null;
  }

  public String getSelectedLabel() {
    SelectItemOption<String> option = getSelectedItem();
    return (option != null) ? option.getLabel() : "";
  }

  public SelectItemOption<String> getSelectedItem() {
    for (SelectItemOption<String> item : options_) {
      if(item.isSelected()){
        return item;
      }
    }
    return null;
  }
  
  public void processRender(WebuiRequestContext context) throws Exception {
    ResourceBundle res = context.getApplicationResourceBundle();
    UIForm uiForm = getAncestorOfType(UIForm.class);
    String formId = uiForm.getId();
    String clazzMaxSize = (options_.size() > displayNumber_) ? " scroll-menu" : "";

    Writer w = context.getWriter();
    w.write("<div class=\"uiFormScrollSelectBox\" id=\"ScrollSelect");
    w.write(getId());
    w.write("\" ");
    renderHTMLAttributes(w);
    w.write(">");

    //
    w.write("<input name=\"");
    w.write(getName());
    w.write("\"");
    w.write(" type=\"hidden\"");
    w.write(" id=\"");
    w.write(getId());
    w.write("\"");
    if (value_ != null && value_.length() > 0) {
      value_ = HTMLEntityEncoder.getInstance().encodeHTMLAttribute(value_);
      w.write(" value=\"");
      w.write(value_);
      w.write("\"");
    }
    w.write("/>");

    //
    w.write("  <div style=\"display:none\" class=\"selectInfoData\" data-onchange=\"");
    w.write(renderOnChangeEvent(uiForm));
    w.write("\" data-size=\"");
    w.write(displayNumber_);
    w.write("\" data-disabled=\"");
    w.write(String.valueOf(isDisabled()));
    w.write("\"></div>\n");

    //
    String clazzDisable = (isDisabled()) ? "disabled " : "";
    w.write("<div class=\"uiFormScrollMenu " + clazzDisable + "ClearFix\" style=\"position:relative\" ");
    renderHTMLAttributes(w);
    w.write(">\n");
    w.write("  <span>");
    w.write(getSelectedLabel());
    w.write("  </span>\n");
    w.write("  <div class=\"rightArrow\"></div>\n");
    w.write("  <div class=\"optionMenu" + clazzMaxSize + "\" style=\"position:absolute; visibility:hidden\">\n");
    w.write("    <ul class=\"option-list\">\n");

    for (SelectItemOption<String> item : options_) {
      String label = item.getLabel();
      try {
        label = res.getString(formId + ".option." + item.getValue());
      } catch (MissingResourceException ex) {
      }

      String value = item.getValue();
      value = HTMLEntityEncoder.getInstance().encodeHTMLAttribute(value);
      if (item.isSelected()) {
        w.write("      <li data-selected=\"true\" class=\"option selected\" data-value=\"");
      } else {
        w.write("      <li data-selected=\"false\" class=\"option\" data-value=\"");
      }
      w.write(value);
      w.write("\">");

      w.write(label);
      w.write("</li>\n");
    }

    w.write("    </ul>\n");
    w.write("  </div>\n");
    w.write("</div>\n");

    if (this.isMandatory())
      w.write(" *");

    w.write("</div>\n");

    context.getJavascriptManager()
           .getRequireJS().require("SHARED/scrollSelectBox", "formScrollSelectBox")
           .addScripts("formScrollSelectBox.init('ScrollSelect" + getId() + "');");
  }

}
