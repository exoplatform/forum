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
package org.exoplatform.forum.webui;

import java.io.Writer;

import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.input.UICheckBoxInput;

public class UIForumCheckBoxInput extends UICheckBoxInput {

  private String label; 

  private String  onchange_;
  
  private boolean isInTable = false;
  
  public UIForumCheckBoxInput(String name, String bindingExpression, String label, Boolean value) {
    super(name, bindingExpression, value);
    this.label = label;
    isInTable = false;
  }

  @Override
  public String getLabel() {
    return label;
  }

  @Override
  public void setLabel(String label) {
    this.label = label;
  }

  public boolean isInTable() {
    return isInTable;
  }

  public UIForumCheckBoxInput setInTable(boolean isInTable) {
    this.isInTable = isInTable;
    return this;
  }
  
  @Override
  public void setOnChange(String onchange) {
    super.setOnChange(onchange);
    onchange_ = onchange;
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    Writer w = context.getWriter();
    if(isInTable) {
      w.write("<tr>\n");
      w.write("  <td>\n");
    }
    w.write("<span class=\"uiCheckbox\">\n");

    w.write("<input type=\"checkbox\" class=\"checkbox\" name=\"");
    w.write(name);
    w.write("\" id=\"");
    w.write(name);
    w.write("\"");
    if (onchange_ != null) {
      UIForm uiForm = getAncestorOfType(UIForm.class);
      w.append(" onclick=\"").append(renderOnChangeEvent(uiForm)).append("\"");
    }
    if (isChecked()) {
      w.write(" checked");
    }
    if (isDisabled()) {
      w.write(" disabled");
    }
    renderHTMLAttributes(w);
    w.write("/>");
    
    w.write("<span>" + label + "</span>\n");

    w.write("</span>");   
    
    if (this.isMandatory()) {
      w.write(" *");
    }

    if(isInTable) {
      w.write("  </td>\n");
      w.write("</tr>\n");
    }
  }
}
