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
