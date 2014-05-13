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
package org.exoplatform.forum.common.webui;

import java.io.Writer;
import java.util.List;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.exoplatform.services.log.ExoLogger;
import org.exoplatform.services.log.Log;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.model.SelectItemOption;
import org.exoplatform.webui.form.UIFormRadioBoxInput;

public class UIPollRadioBoxInput extends UIFormRadioBoxInput {

  protected Log log = ExoLogger.getLogger(this.getClass());
  
  /**
   * The list of radio boxes
   */

  public UIPollRadioBoxInput(String name,
                              String bindingExpression,
                              List<SelectItemOption<String>> options) {
    super(name, bindingExpression, options);
  }

  public void processRender(WebuiRequestContext context) throws Exception {
    List<SelectItemOption<String>> options = getOptions();
    if (options == null){
      return;
    }
    ResourceBundle res = context.getApplicationResourceBundle();
    Writer w = context.getWriter();
    if (value_ == null) {
      SelectItemOption<String> si = options.get(0);
      value_ = si.getValue();
    }

    int index = 0;
    for (int i = index; i < options.size(); i++) {
      SelectItemOption<String> si = options.get(i);
      String checked = "";
      if (si.getValue().equals(value_)){
        checked = " checked='checked'";
      }

      w.write("<tr>\n");
      w.write("  <td>\n");

      w.write("    <label class=\"uiRadio\" for=\"" + getName() + "\">\n");
      w.write("      <input class='radio' type='radio'");
      if (readonly_){
        w.write(" readonly ");
      }
      if (isDisabled()){
        w.write(" disabled ");
      }
      w.write(checked);
      w.write(" name='");
      w.write(getName());
      w.write("'");
      w.write(" value='");
      w.write(si.getValue());
      w.write("'/>\n");

      w.write("     <span>");
      String label = si.getLabel();
      try {
        label = res.getString(getId() + ".label." + si.getLabel());
      } catch (MissingResourceException e) {
        log.warn("Can not find resource bundle for key : " + getId() + ".label." + label);
      }
      w.write(label);
      w.write("      </span>\n");
      w.write("    </label>\n");

      w.write("  </td>\n");
      w.write("</tr>\n");
    }
  }
}
