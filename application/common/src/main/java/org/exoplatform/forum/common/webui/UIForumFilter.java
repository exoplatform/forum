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

  public String getDefaultSelect() {
    if(defaultSelectName == null) {
      defaultSelectName = WebUIUtils.getLabel(this.getParent().getId(), "SelectAForum");
    }
    return this.defaultSelectName;
  }

  private String getFilerPlaceholder() {
    return WebUIUtils.getLabel(this.getParent().getId(), "FilterForums");
  }
  
  @Override
  public void decode(Object input, WebuiRequestContext context) {
    String val = (String)input;
    if ((val == null || val.length() == 0)) {
      return;
    }
    value_ = val;
    if (value_ != null && value_.length() == 0)
       value_ = null;
  }
  
  public void setOnChange(String onchange)
  {
     onchange_ = onchange;
  }
  
  protected String renderOnChangeEvent(UIForm uiForm) throws Exception
  {
    if(onchange_ != null && onchange_.length() > 0) {
      return uiForm.event(onchange_, (String)getId());
    }
    return "";
  }
  
  public void processRender(WebuiRequestContext context) throws Exception
  {
     UIForm uiForm = getAncestorOfType(UIForm.class);
     String value = getValue();
     Writer w = context.getWriter();
     w.write("<input name=\"");
     w.write(getName());
     w.write("\"");
     w.write(" type=\"hidden\"");
    
     w.write(" id=\"");
     w.write(getId());
     w.write("\"");
     if (value != null && value.length() > 0)
     {
        value = HTMLEntityEncoder.getInstance().encodeHTMLAttribute(value);
        w.write(" value=\"");
        w.write(value);
        w.write("\"");
     }
     w.write("/>");
     w.write("  <div style=\"display:none\" id=\"onChange");
     w.write(getId());
     w.write("\" data-onchange=\"");
     w.write(renderOnChangeEvent(uiForm));
     w.write("\"></div>\n");
     w.write("<div class=\"UIForumFilter ClearFix\" id=\"Fake");
     w.write(getId());
     w.write("\" style=\"position:relative\" ");
     renderHTMLAttributes(w);
     w.write(">\n");
     w.write("  <span>");
     w.write(getDefaultSelect());
     w.write("  </span>\n");
     w.write("  <div class=\"RightArrow\"></div>\n");
     w.write("  <div class=\"FilterMenu\" style=\"position:absolute; visibility:hidden\">\n");
     w.write("    <ul>\n");
     w.write("      <li>\n");
     w.write("        <input type=\"text\" class=\"FilterInput\" placeholder=\""+getFilerPlaceholder()+"\"/>\n");
     w.write("      </li>\n");
     w.write("    </ul>\n");
     w.write("  </div>\n");
     w.write("</div>\n");
      
     context.getJavascriptManager().getRequireJS()
         .require("SHARED/forumFilter", "forumfilter")
         .addScripts("forumfilter.init('Fake" + getId() + "');");
  }

}
