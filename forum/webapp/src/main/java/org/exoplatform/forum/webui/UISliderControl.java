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

import org.exoplatform.forum.ForumUtils;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.form.UIFormInputBase;

public class UISliderControl extends UIFormInputBase<String> {
  
  
  int maxValue = 100;

  public UISliderControl(String name, String bindingExpression, String value) {
     this(name, bindingExpression, value, 100);
  }

  public UISliderControl(String name, String bindingExpression, String value, int maxValue) {
    super(name, bindingExpression, String.class);
    this.value_ = value;
    this.maxValue = maxValue;
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    Writer w = context.getWriter();
    w.write("<div class=\"uiFormSliderInput clearfix\" id=\"uiSliderContainer" + getName() + "\" >");
    w.write("  <div class=\"slideSearch pull-left\">");
    w.write("    <div class=\"slide slideContainer\">");
    w.write("      <div class=\"slideRange sllideHeader\" style=\"width: 0px;\"></div>");
    w.write("      <a href=\"javascript:void(0);\" class=\"circleDefault\" style=\"left: 0%;\"></a>");
    w.write("    </div>");
    w.write("  </div>");
    w.write("  <div class=\"boxNumber pull-left\">");
    w.write(new StringBuilder("    <input class=\"uiSliderInput\" type=\"text\" name=\"").append(getName()).append("\" id=\"").append(getName()).append("\" value=\"").append(value_).append("\" readonly disabled/>").toString());
    w.write("  </div>");
    w.write("</div>");

    ForumUtils.addScripts("UISliderControl", "forumSliderControl", "forumSliderControl.init('uiSliderContainer" + getId() + "', " + maxValue + ");");
  }

  public void decode(Object input, WebuiRequestContext context) {
    String val = (String) input;
    if (ForumUtils.isEmpty(val) || (val.equals("null"))){
      value_ = "0";
    } else {
      value_ = val;
    }
  }

}
