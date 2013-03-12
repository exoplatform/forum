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

import org.exoplatform.commons.utils.HTMLEntityEncoder;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.form.UIForm;
import org.exoplatform.webui.form.UIFormInputBase;

import java.io.Writer;

/**
 * Created by The eXo Platform SAS
 * Author : Vu Duy Tu
 *          tuvd@exoplatform.com
 * Jan 3, 2013  
 */
public class UIForumFilter extends UIFormInputBase<String> {
  private String defaultSelectName = null;

  private String onchange_         = null;

  public UIForumFilter() {
  }

  public UIForumFilter(String name, String bindingExpression) {
    super(name, bindingExpression, String.class);
  }

  public UIForumFilter(String name, String bindingExpression, String value) {
    super(name, bindingExpression, String.class);
    this.value_ = value;
  }

  public void setDefaultSelect(String defaultSelectName) {
    this.defaultSelectName = defaultSelectName;
  }

  public String getCategoryId() {
    String value = (this.value_ != null && this.value_.length() > 0) ? this.value_ : " ; ";
    return value.split(";")[0].trim();
  }

  public String getForumId() {
    String value = (this.value_ != null && this.value_.length() > 0) ? this.value_ : " ; ";
    return value.split(";")[1].trim();
  }

  public String getForumName() {
    if (this.value_ != null && this.value_.length() > 0) {
      String forumName = this.value_.replaceFirst(getCategoryId() + ";", "");
      forumName = forumName.replaceFirst(getForumId() + ";", "").trim();
      return forumName;
    }

    return getDefaultSelect().trim();
  }

  private String getDefaultSelect() {
    if (defaultSelectName == null) {
      defaultSelectName = WebUIUtils.getLabel(this.getParent().getId(), "SelectAForum");
    }
    return this.defaultSelectName;
  }

  private String getFilerPlaceholder() {
    return WebUIUtils.getLabel(this.getParent().getId(), "FilterForums");
  }
  
  @Override
  public void decode(Object input, WebuiRequestContext context) {
    String val = (String) input;
    if ((val == null || val.length() == 0)) {
      return;
    }
    value_ = val;
    if (value_ != null && value_.length() == 0)
      value_ = null;
  }

  public void setOnChange(String onchange) {
    onchange_ = onchange;
  }
  
  protected String renderOnChangeEvent(UIForm uiForm) throws Exception {
    if (onchange_ != null && onchange_.length() > 0) {
      return uiForm.event(onchange_, (String) getId());
    }
    return "";
  }
  
  public void processRender(WebuiRequestContext context) throws Exception {
    UIForm uiForm = getAncestorOfType(UIForm.class);
    Writer w = context.getWriter();
    w.write("<div id=\"uiForumFilter");
    w.write(getId());
    w.write("\">\n");

    w.write("  <input name=\"");
    w.write(getName());
    w.write("\"");
    w.write(" type=\"hidden\"");

    w.write(" id=\"");
    w.write(getId());
    w.write("\"");
    String value = getValue();
    if (value != null && value.length() > 0) {
      w.write(" value=\"");
      w.write(HTMLEntityEncoder.getInstance().encodeHTMLAttribute(value));
      w.write("\"");
    }
    w.write("/>\n");

    w.write("  <div style=\"display:none\" class=\"forumFilterData\" data-onchange=\"");
    w.write(renderOnChangeEvent(uiForm));
    w.write("\"></div>\n");

    w.write("  <div class=\"uiForumFilter btn-group uiDropdownWithIcon clearfix\" ");
    renderHTMLAttributes(w);
    w.write(">\n");
    
    w.write("  <div class=\"btn dropdown-toggle\">");
    w.write("    <span class=\"titleForum\">");
    w.write(getForumName());
    w.write("    </span>\n");
    w.write("    <span class=\"spiter\"></span>\n");
    w.write("    <span class=\"caret\"></span>\n");
    w.write("</div>\n");
    
    w.write("    <div class=\"filterMenu open\" style=\"position:absolute; visibility:hidden\">\n");
    w.write("      <ul class=\"dropdown-menu\">\n");
    w.write("        <li>\n");
    w.write("          <input type=\"text\" class=\"filterInput\" placeholder=\"" + getFilerPlaceholder() + "\"/>\n");
    w.write("        </li>\n");
    w.write("      </ul>\n");
    w.write("    </div>\n");
    w.write("  </div>\n");

    w.write("</div>\n");

     context.getJavascriptManager().getRequireJS()
         .require("SHARED/forumFilter", "forumfilter")
         .addScripts("forumfilter.init('uiForumFilter" + getId() + "');");
  }

}
