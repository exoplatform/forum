/*
 * Copyright (C) 2003-2012 eXo Platform SAS.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

package org.exoplatform.forum.extras.injection.utils;

import junit.framework.TestCase;

/**
 * @author Ly Minh Phuong - http://phuonglm.net
 * @version $Revision$
 */

public class ExoNameGeneratorTestCase extends TestCase {

  public void testNameGenerator() throws Exception {
    ExoNameGenerator exoNameGenerator = new ExoNameGenerator();
    String name = "";
    for(int i = 0; i < 100; i++){
      name = exoNameGenerator.compose(4);
      assertTrue("expect the length of name must larger or equal 4 but \"" + name + "\".length() is " + name.length(),name.length() >= 4);
      name = exoNameGenerator.compose(3);
      assertTrue("expect the length of name must larger or equal 3 but \"" + name + "\".length() is " + name.length(),name.length() >= 3);
      name = exoNameGenerator.compose(2);
      assertTrue("expect the length of name must larger or equal 2 but \"" + name + "\".length() is " + name.length(),name.length() >= 2);
      name = exoNameGenerator.compose(1);
      assertTrue("expect the length of name must larger or equal 1 but \"" + name + "\".length() is " + name.length(),name.length() >= 1);
    }
  }
}
