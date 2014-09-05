/*
 * Copyright (C) 2003-2009 eXo Platform SAS.
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
package org.exoplatform.forum.rendering;

import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

import org.apache.commons.collections.CollectionUtils;
import org.exoplatform.commons.testing.mock.MockWebUIRequestContext;
import org.exoplatform.forum.bbcode.core.BBCodeRenderer;
import org.exoplatform.forum.common.webui.BuildRendering;
import org.exoplatform.forum.common.webui.WebUIUtils;
import org.exoplatform.forum.service.Post;
import org.exoplatform.webui.application.WebuiRequestContext;
import org.exoplatform.webui.core.UIApplication;

/**
 * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice Lamarque</a>
 * @version $Revision$
 */
public class TestRenderHelper extends TestCase {
  private MarkupRenderingService service = new MarkupRenderingService();
  private RenderHelper helper;

  protected void setUp() throws Exception {
    service.registerRenderer(new BBCodeRenderer());
    helper = new RenderHelper();
    helper.setMarkupRenderingService(service);
    super.setUp();
  }

  public void testRenderPost() {
    
    String message = "this is [b]bold[/bold]";
    Post post = new Post();
    post.setMessage(message);
    
    String actual = helper.renderPost(post);
    String expected = service.getRenderer("bbcode").render(message);
    assertEquals(expected, actual);
  }
  
  public void testSupportedBBCode() throws Exception {
    WebuiRequestContext context = new MockWebUIRequestContext(null, null) {
      private Writer writer = new StringWriter();
      @Override
      public Writer getWriter() throws Exception {
        return writer;
      }
      @Override
      public UIApplication getUIApplication() {
        UIApplication app = new UIApplication() {
          @Override
          public String getId() {
            return "AppTest";
          }
        };
        return app;
      }
    };
    context.setCurrentInstance(context);
    BuildRendering.startRender(context);
    //
    String message = "my test bbcode CODE<option> tag[code=java]The java code[/code]<pre class=\"brush:js;toolbar:false;\"> The code js</pre>";
    Post post = new Post();
    post.setMessage(message);
    //
    helper.renderPost(post);
    //
    List<String> codeHighlighters = new ArrayList<String>(BuildRendering.getCodeHighlighters(context));
    assertEquals("[JScript, Java]", codeHighlighters.toString());
    String expected = 
        "<script src=\"/forumResources/syntaxhighlighter/Scripts/shCore.js\" id=\"script_0_UIScriptBBCodeContainer\" type=\"text/javascript\"></script>" +
        "<script src=\"/forumResources/syntaxhighlighter/Scripts/shAutoloader.js\" id=\"script_1_UIScriptBBCodeContainer\" type=\"text/javascript\"></script>" +
        "<script src=\"/forumResources/syntaxhighlighter/Scripts/shBrushJScript.js\" id=\"script_2_UIScriptBBCodeContainer\" type=\"text/javascript\"></script>" +
        "<script src=\"/forumResources/syntaxhighlighter/Scripts/shBrushJava.js\" id=\"script_3_UIScriptBBCodeContainer\" type=\"text/javascript\"></script>" +
        "<script src=\"/forumResources/syntaxhighlighter/Scripts/shLegacy.js\" id=\"script_4_UIScriptBBCodeContainer\" type=\"text/javascript\"></script>" +
        "<script src=\"/forumResources/syntaxhighlighter/Scripts/load_syntaxhighlighter.js\" id=\"script_5_UIScriptBBCodeContainer\" type=\"text/javascript\"></script>";
    assertEquals(expected, WebUIUtils.attachJSSyntaxHighlighter(codeHighlighters));
    //
    BuildRendering.endRender(context);
    //
    assertNull(BuildRendering.getCodeHighlighters(context));
  }

  public void testGetCodeSupportedLangs() {
    List<String> expected = Arrays.asList("Bash", "AppleScript", "Diff", "JavaFX", "Perl", "Java", "AS3", "Erlang", "Scala", "Cpp", "Python", "JScript", "CSharp",
                                          "Sass", "Ruby", "ColdFusion", "Sql", "PowerShell", "Php", "Delphi", "Xml", "Vb", "Haxe", "TypeScript", "Plain", "Groovy", "Css");
    assertTrue(CollectionUtils.isEqualCollection(expected, BuildRendering.getCodeSupportedLangs().keySet()));
  }
}
