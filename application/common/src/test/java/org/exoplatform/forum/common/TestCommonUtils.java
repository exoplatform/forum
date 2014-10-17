/*
 * Copyright (C) 2003-2011 eXo Platform SAS.
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
package org.exoplatform.forum.common;

import java.util.Arrays;
import java.util.List;

import junit.framework.TestCase;

public class TestCommonUtils extends TestCase {

  public void testIsEmpty() {
    String s = null;
    assertEquals(true, CommonUtils.isEmpty(s));
    s = CommonUtils.EMPTY_STR;
    assertEquals(true, CommonUtils.isEmpty(s));
    s = CommonUtils.SPACE;
    assertEquals(true, CommonUtils.isEmpty(s));
    s = "abc";
    assertEquals(false, CommonUtils.isEmpty(s));
  }

  public void testIsArrayEmpty() {
    String []strs = null;
    assertEquals(true, CommonUtils.isEmpty(strs));
    strs = new String[]{};
    assertEquals(true, CommonUtils.isEmpty(strs));
    strs = new String[]{CommonUtils.EMPTY_STR};
    assertEquals(true, CommonUtils.isEmpty(strs));
    strs = new String[]{CommonUtils.SPACE};
    assertEquals(true, CommonUtils.isEmpty(strs));
    strs = new String[]{"abc"};
    assertEquals(false, CommonUtils.isEmpty(strs));
  }
  
  public void testEncodeSpecialCharInSearchTerm() {
    //test for text null
    String s = null;
    assertEquals("",CommonUtils.encodeSpecialCharInSearchTerm(s));
    //test for text empty
    s = "";
    assertEquals("",CommonUtils.encodeSpecialCharInSearchTerm(s));
    // normal text
    s = "normal text";
    assertEquals("normal text",CommonUtils.encodeSpecialCharInSearchTerm(s));
    // all characters is special characters.
    s = "@#$%^&*()\"/-=~`'.,";
    assertEquals("&#64;&#35;&#36;&#37;&#94;&#38;&#42;&#40;&#41;&#34;&#47;&#45;&#61;&#126;&#96;&#39;&#46;&#44;",CommonUtils.encodeSpecialCharInSearchTerm(s));
    // has ignore special characters.
    s = "abc !#:? =., +;";
    assertEquals("abc !#:? =., +;",CommonUtils.encodeSpecialCharInSearchTerm(s));
    // has ignore and not ignore special characters.
    s = "abc !#: ()\" ' | ] [";
    assertEquals("abc !#: &#40;&#41;&#34; &#39; &#124; &#93; &#91;",CommonUtils.encodeSpecialCharInSearchTerm(s));
  }

  public void testEncodeSpecialCharInTitle() {
    //test for text null
    String s = null;
    assertEquals("",CommonUtils.encodeSpecialCharInTitle(s));
    //test for text empty
    s = "";
    assertEquals("",CommonUtils.encodeSpecialCharInTitle(s));
    // normal text
    s = "normal text";
    assertEquals("normal text",CommonUtils.encodeSpecialCharInTitle(s));
    // has double space .
    s = "   abc   aa s   s";
    assertEquals("abc aa s s", CommonUtils.encodeSpecialCharInTitle(s));
    // has ignore special characters.
    s = "abc !#:?=.,()+; ddd";
    assertEquals("abc !#:?=.,&#40;&#41;+; ddd",CommonUtils.encodeSpecialCharInTitle(s));
    // has ignore and not ignore special characters.
    s = "abc !# :?=.,' | ] [";
    assertEquals("abc !# :?=.,&#39; &#124; &#93; &#91;",CommonUtils.encodeSpecialCharInTitle(s));
  }
  
  public void testEncodeSpecialCharInContent() {
    //test for text null
    String s = null;
    assertEquals("",CommonUtils.encodeSpecialCharInContent(s));
    //test for text empty
    s = "";
    assertEquals("",CommonUtils.encodeSpecialCharInContent(s));
    // normal text
    s = "normal text";
    assertEquals("normal text",CommonUtils.encodeSpecialCharInContent(s));
        // has ignore special characters.
    s = "abc &#<>[]/:?\"=.,*$%()\\+@!^*-}{;`~_ ddd";
    assertEquals("abc &#<>[]/:?\"=.,*$%()\\+@!^*-}{;`~_ ddd",CommonUtils.encodeSpecialCharInContent(s));
    // has ignore and not ignore special characters.
    s = "abc !# :?=.,' | ] [";
    assertEquals("abc !# :?=.,&#39; &#124; ] [",CommonUtils.encodeSpecialCharInContent(s));
  }
  
  public void testEncodeSpecialCharToHTMLnumber() {
    /* 
     * when test successful encodeSpecialCharInSearchTerm(), encodeSpecialCharInTitle() and
     * encodeSpecialCharInContent, this function encodeSpecialCharToHTMLnumber tested.
    */
  }
  
  
  public void testDecodeSpecialCharToHTMLnumber() throws Exception {
    String input = null;
    assertEquals(null, CommonUtils.decodeSpecialCharToHTMLnumber(input));
    input = "";
    assertEquals(input, CommonUtils.decodeSpecialCharToHTMLnumber(input));
    input = "Normal text abc";
    assertEquals(input, CommonUtils.decodeSpecialCharToHTMLnumber(input));
    input = "Text ...&#60;&#64;&#35;&#36;&#37;&#94;&#38;&#42;&#40;&#41;&#34;&#47;&#45;&#61;&#126;&#96;&#39;&#46;&#44;&#62; too";
    assertEquals("Text ...<@#$%^&*()\"/-=~`'.,> too", CommonUtils.decodeSpecialCharToHTMLnumber(input));
    //content extend token
    input = "Text ...&lt;div class=&quot;&amp;XZY&quot;&gt;Test&lt;&#47;div&gt;&#40;&#41;&#34;&#47;&#45;&#61;&#126;&#96;&#39;&#46;&#44;&#60;strong&#62;too&#60;&#47;strong&#62;";
    assertEquals("Text ...<div class=\"&XZY\">Test</div>()\"/-=~`'.,<strong>too</strong>", CommonUtils.decodeSpecialCharToHTMLnumber(input));
    
    // ignore case
    List<String> ig = Arrays.asList(new String[]{"&gt;", "&lt;", "&#46;"});
    assertEquals("Text ...&lt;div class=\"&XZY\"&gt;Test&lt;/div&gt;()\"/-=~`'&#46;,<strong>too</strong>", CommonUtils.decodeSpecialCharToHTMLnumber(input, ig));
    
    //
    assertEquals("Text ...&lt;div class=\"&amp;XZY\"&gt;Test&lt;/div&gt;()\"/-=~`'.,<strong>too</strong>", CommonUtils.decodeSpecialCharToHTMLnumberIgnore(input));
  }
  
  public void testIsContainSpecialCharacter() {
    String input = null;
    assertEquals(false, CommonUtils.isContainSpecialCharacter(input));
    input = "";
    assertEquals(false, CommonUtils.isContainSpecialCharacter(input));
    input = "abcgde";
    assertEquals(false, CommonUtils.isContainSpecialCharacter(input));
    input = "abcg#$de";
    assertEquals(true, CommonUtils.isContainSpecialCharacter(input));
    input = "!@#abcgde";
    assertEquals(true, CommonUtils.isContainSpecialCharacter(input));
    input = "&#<>[]/:?\"=.,*$%()\\+@!^*-}{;`~_";
    assertEquals(true, CommonUtils.isContainSpecialCharacter(input));
  }
  
  public void testRemoveSpecialCharacter() {
    String input = null;
    assertNull(CommonUtils.removeSpecialCharacterForSearch(input));
    input = "";
    assertEquals("", CommonUtils.removeSpecialCharacterForSearch(input));
    input = "a  bc ";
    assertEquals("a bc", CommonUtils.removeSpecialCharacterForSearch(input));
    input = " a&#<>[]/:?\"=.,*$%()\\+@!^*-}{;`~_bc    ";
    assertEquals("a ? * % * _bc", CommonUtils.removeSpecialCharacterForSearch(input));
  }

  public void testNormalizeUnifiedSearchInput() {
    String input = "";
    assertEquals("", CommonUtils.normalizeUnifiedSearchInput(input));
    input = "normal   text";
    assertEquals("*normal* *text*", CommonUtils.normalizeUnifiedSearchInput(input));
    input = "normal text";
    assertEquals("*normal* *text*", CommonUtils.normalizeUnifiedSearchInput(input));
    input = "normal~0.5 text";
    assertEquals("*normal* *text*", CommonUtils.normalizeUnifiedSearchInput(input));
    input = "normal~0.5 text~0.5 text~1 text~1.0";
    assertEquals("*normal* *text* *text* *text*", CommonUtils.normalizeUnifiedSearchInput(input));
    input = "normal~0.5 text~0.5 text~1 text~1.0 text~text";
    assertEquals("*normal* *text* *text* *text* *texttext*", CommonUtils.normalizeUnifiedSearchInput(input));
    input = "abc z!@#";
    assertEquals("*abc* *z*", CommonUtils.normalizeUnifiedSearchInput(input));
    //
    input = "Japan~1 日本~1";
    assertEquals("%Japan% %日本%", CommonUtils.normalizeUnifiedSearchInput(input));
    input = "Việt~1.0 nam";
    assertEquals("%Việt% %nam%", CommonUtils.normalizeUnifiedSearchInput(input));
    input = "Russia~0.5 Частушки";
    assertEquals("%Russia% %Частушки%", CommonUtils.normalizeUnifiedSearchInput(input));
    input = "Korea 한국";
    assertEquals("%Korea% %한국%", CommonUtils.normalizeUnifiedSearchInput(input));
  }

  public void testHasSpecialCharacter() {
    String input = null;
    assertFalse(CommonUtils.hasSpecialCharacter(input));
    input = "";
    assertFalse(CommonUtils.hasSpecialCharacter(input));
    input = "abc";
    assertFalse(CommonUtils.hasSpecialCharacter(input));
    input = " AB a X bcS 012 9 ";
    assertFalse(CommonUtils.hasSpecialCharacter(input));
    input = "!@#";
    assertTrue(CommonUtils.hasSpecialCharacter(input));
  }

  public void testProcessSearchCondition() {
    String input = null;
    assertNull(CommonUtils.processSearchCondition(input));
    input = "";
    assertEquals("", CommonUtils.processSearchCondition(input));

    input = "abc";
    assertEquals("%abc%", CommonUtils.processSearchCondition(input));

    input = " AB a X bcS 012 9 ";
    assertEquals("%AB a X bcS 012 9%", CommonUtils.processSearchCondition(input));

    input = " *AB %a X bcS% 012* *a9* ";
    assertEquals("%%AB %a X bcS% 012% %a9%%", CommonUtils.processSearchCondition(input));

    input = "  &#<>[]/:\"=.,$()\\+@!^-}{;`~  ";
    assertEquals("%%", CommonUtils.processSearchCondition(input));

    input = "  a&#<>[]/:?\"=.,*$%()\\+@!^*-}{;`~_bc  ";
    assertEquals("%a ? % % % _bc%", CommonUtils.processSearchCondition(input));
  }
  
  public void testGetExcerpt() throws Exception {
    String str = "Revived after the Restoration of 1660. They ended again in 1852, when the common land on " +
    		         "which they had been staged was partitioned and enclosed. Since 1966 the Games have been held" +
    		         " each year on the Friday after Spring Bank Holiday. Events have included the tug of war, " +
    		         "the first stirrings of Britain's Olympic beginnings. (Full article...) excerpt The Cotswold " +
    		         " throwing, fighting with swords, and wrestling. By the time of James's death in 1625, many Puritan" +
    		         " landowners had forbidden their workers to attend, and the outbreak of the English Civil War in 1642";
    
    String expecString = "... and enclosed. Since 1966 the Games have been held each year on the Friday after Spring" +
    		                 " Bank Holiday. Events have included the tug of war, the first stirrings of Britain's Olympic beginnings." +
    		                 " (Full article...) excerpt The Cotswold  throwing, fighting with swords, and wrestling. By the time" +
    		                 " of James's death in 1625, many Puritan landowners had forbidden their workers to attend, and the" +
    		                 " outbreak of the English Civil War in ...";
    
    String truncStr = CommonUtils.getExcerpt(str, "excerpt", 430);
    //
    assertEquals(expecString, truncStr);
    
    int middlePosition = str.indexOf("excerpt");
    //
    truncStr = CommonUtils.centerTrunc( str, middlePosition, 430);
    
    //
    assertEquals(expecString, truncStr);
  }

  public void testGetURI() {
    //
    assertNull(CommonUtils.getURI(null));
    //
    assertEquals("", CommonUtils.getURI(""));
    //
    assertEquals("/path1/path2/", CommonUtils.getURI("/path1/path2/"));
    //
    assertEquals("http://domain.com", CommonUtils.getURI("http://domain.com"));
    //
    assertEquals("/", CommonUtils.getURI("http://domain.com/"));
    //
    assertEquals("/path1/path2/", CommonUtils.getURI("http://domain.com/path1/path2/"));
    //
    assertEquals("/path1/?q=flower", CommonUtils.getURI("http://domain.com/path1/?q=flower"));
  }

}
