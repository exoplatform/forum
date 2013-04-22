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
package org.exoplatform.forum.bbcode.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.jcr.Node;

import org.exoplatform.forum.bbcode.api.BBCode;
import org.exoplatform.forum.bbcode.base.BaseBBcodeTestCase;
import org.exoplatform.forum.bbcode.core.cache.CachedBBCodeService;
import org.exoplatform.services.cache.CacheService;
import org.junit.Test;

public class TestBBcodeCacheService extends BaseBBcodeTestCase {

  private CachedBBCodeService cachedBBCodeService;
  
  public void setUp() throws Exception {
    super.setUp();
    
    if(cachedBBCodeService == null) {
      CacheService  service = getService(CacheService.class);
      cachedBBCodeService = new CachedBBCodeService(service, bbcodeServiceImpl);
      cachedBBCodeService.start();
    }
  }

  
  @Test
  public void testSave() throws Exception {
    List<BBCode> bbcodes = Arrays.asList(createBBCode("list", "replacement", "description", "example", false, false));
    cachedBBCodeService.save(bbcodes);
    String targetPath = bbcodesPath + "/" + "list";
    assertNodeExists(targetPath);
    Node n = getNode(targetPath);
    assertEquals("list", n.getProperty("exo:tagName").getString());
    assertEquals("replacement", n.getProperty("exo:replacement").getString());
    assertEquals("description", n.getProperty("exo:description").getString());
    assertEquals("example", n.getProperty("exo:example").getString());
    assertEquals(false, n.getProperty("exo:isOption").getBoolean());
    assertEquals(false, n.getProperty("exo:isActive").getBoolean());
    n.getSession().logout();
  }

  @Test
  public void testGetAll() throws Exception {
    List<BBCode> bbcodes = new ArrayList<BBCode>();
    bbcodes.add(createBBCode("foo", "replacement", "description", "example", false, true));
    bbcodes.add(createBBCode("foo", "replacement", "description", "example", true, false));
    bbcodes.add(createBBCode("foo2", "replacement", "description", "example", false, true));
    bbcodes.add(createBBCode("foo3", "replacement", "description", "example", false, true));
    bbcodes.add(createBBCode("foo4", "replacement", "description", "example", false, true));
    cachedBBCodeService.save(bbcodes);
    List<BBCode> actual = cachedBBCodeService.getAll();
    assertEquals(bbcodes.size(), actual.size());
  }

  @Test
  public void testGetActive() throws Exception {
    List<BBCode> bbcodes = new ArrayList<BBCode>();
    bbcodes.add(createBBCode("foo", "replacement", "description", "example", false, true));
    bbcodes.add(createBBCode("foo", "replacement", "description", "example", true, false));
    bbcodes.add(createBBCode("foo2", "replacement", "description", "example", true, true));
    bbcodes.add(createBBCode("foo3", "replacement", "description", "example", false, true));
    bbcodes.add(createBBCode("foo4", "replacement", "description", "example", false, true));
    cachedBBCodeService.save(bbcodes);
    List<String> actual = cachedBBCodeService.getActive();
    assertEquals(bbcodes.size() - 1, actual.size());
    assertContains(actual, "foo", "foo2=", "foo3", "foo4");
  }

  @Test
  public void testFindById() throws Exception {
    List<BBCode> bbcodes = Arrays.asList(createBBCode("foo", "replacement", "description", "example", true, true));
    cachedBBCodeService.save(bbcodes);
    assertNodeExists(bbcodesPath + "/" + "foo=");
    BBCode actual = cachedBBCodeService.findById("foo=");
    assertNotNull(actual);
    assertEquals("foo", actual.getTagName());
    assertEquals("replacement", actual.getReplacement());
    assertEquals("description", actual.getDescription());
    assertEquals("example", actual.getExample());
    assertEquals(true, actual.isOption());
    assertEquals(true, actual.isActive());
  }

  @Test
  public void testDelete() throws Exception {
    List<BBCode> bbcodes = Arrays.asList(createBBCode("foo", "replacement", "description", "example", false, false));
    cachedBBCodeService.save(bbcodes);
    String targetPath = bbcodesPath + "/" + "foo";
    assertNodeExists(targetPath);
    cachedBBCodeService.delete("foo");
    assertNodeNotExists(targetPath);
  }

}
