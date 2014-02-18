/*
 * Copyright (C) 2009 eXo Platform SAS.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.exoplatform.forum.common.lifecycle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.Callable;

import junit.framework.TestCase;

import org.exoplatform.container.xml.InitParams;
import org.exoplatform.container.xml.ValueParam;

public class AbstractLifeCycleTest extends TestCase {

  private LifeCycleCompletionService asyncCompletionService;

  private LifeCycleCompletionService syncCompletionService;

  @Override
  public void setUp() throws Exception {

    super.setUp();

    InitParams asyncParams = new InitParams();

    asyncParams.addParameter(createParam("thread-number", "10"));
    asyncParams.addParameter(createParam("async-execution", "true"));

    asyncCompletionService = new LifeCycleCompletionService(asyncParams);

    InitParams syncParams = new InitParams();

    syncParams.addParameter(createParam("thread-number", "10"));
    syncParams.addParameter(createParam("async-execution", "false"));

    syncCompletionService = new LifeCycleCompletionService(syncParams);

  }

  @Override
  public void tearDown() throws Exception {
    super.tearDown();
  }

  private ValueParam createParam(String key, String value) {

    ValueParam valueParam = new ValueParam();

    valueParam.setName(key);
    valueParam.setValue(value);

    return valueParam;
  }

  public void testAsyncSimpleBroadcast() {

    AwaitingLifeCycle lifecycle = new AwaitingLifeCycle(asyncCompletionService);

    MockListener capture = new MockListener();
    lifecycle.addListener(capture);
    MockListener capture2 = new MockListener();
    lifecycle.addListener(capture2);

    lifecycle.event1(null, "foo");
    lifecycle.event1(null, "bar");

    asyncCompletionService.waitCompletionFinished();

    assertTrue(capture.hasEvent("bar"));
    assertTrue(capture.hasEvent("foo"));
    assertTrue(capture2.hasEvent("bar"));
    assertTrue(capture2.hasEvent("foo"));

  }

  public void testAsyncBroadcastWithFailingListener() {

    AwaitingLifeCycle lifecycle = new AwaitingLifeCycle(asyncCompletionService);
    MockListener capture = new MockListener();
    lifecycle.addListener(capture);
    MockFailingListener failing = new MockFailingListener();
    lifecycle.addListener(failing);
    MockListener capture2 = new MockListener();
    lifecycle.addListener(capture2);

    lifecycle.event1(null, "foo");
    lifecycle.event1(null, "bar");

    asyncCompletionService.waitCompletionFinished();

    assertTrue(capture.hasEvent("bar"));
    assertTrue(capture.hasEvent("foo"));
    assertTrue(capture2.hasEvent("bar"));
    assertTrue(capture2.hasEvent("foo"));
    assertFalse(failing.hasEvent("bar"));
    assertFalse(failing.hasEvent("foo"));

  }

  public void testSyncSimpleBroadcast() {

    AwaitingLifeCycle lifecycle = new AwaitingLifeCycle(syncCompletionService);

    MockListener capture = new MockListener();
    lifecycle.addListener(capture);
    MockListener capture2 = new MockListener();
    lifecycle.addListener(capture2);

    lifecycle.event1(null, "foo");
    lifecycle.event1(null, "bar");

    assertTrue(capture.hasEvent("bar"));
    assertTrue(capture.hasEvent("foo"));
    assertTrue(capture2.hasEvent("bar"));
    assertTrue(capture2.hasEvent("foo"));

  }

  public void testSyncBroadcastWithFailingListener() {

    AwaitingLifeCycle lifecycle = new AwaitingLifeCycle(syncCompletionService);
    MockListener capture = new MockListener();
    lifecycle.addListener(capture);
    MockFailingListener failing = new MockFailingListener();
    lifecycle.addListener(failing);
    MockListener capture2 = new MockListener();
    lifecycle.addListener(capture2);

    lifecycle.event1(null, "foo");
    lifecycle.event1(null, "bar");

    assertTrue(capture.hasEvent("bar"));
    assertTrue(capture.hasEvent("foo"));
    assertTrue(capture2.hasEvent("bar"));
    assertTrue(capture2.hasEvent("foo"));
    assertFalse(failing.hasEvent("bar"));
    assertFalse(failing.hasEvent("foo"));

  }

  class MockEvent {
    String source, payload;

    public MockEvent(String source, String payload) {
      this.source = source;
      this.payload = payload;
    }

    public String getSource() {
      return source;
    }

    public String getPayload() {
      return payload;
    }
  }

  class MockListener {
    public Collection<String> events = new ArrayList<String>();

    public boolean hasEvent(String event) {
      return events.contains(event);
    }

    protected void recordEvent(MockEvent event) {
      events.add(event.getPayload());
    }

    public void event1(MockEvent event) {
      recordEvent(event);
    }

  }

  class MockFailingListener extends MockListener {
    protected void recordEvent(MockEvent event) {
      throw new RuntimeException("fake runtime exception thrown on purpose");
    }
  }

  /**
   * Custom LifeCycle for testing purpose, that can wait until all events are
   * dispatched. Necessary to avoid test failures when the internal executor has
   * not finished executing all listeners.
   * 
   * @author <a href="mailto:patrice.lamarque@exoplatform.com">Patrice
   *         Lamarque</a>
   * @version $Revision$
   */
  class AwaitingLifeCycle {
    protected Set<MockListener> listeners = new HashSet<MockListener>();

    LifeCycleCompletionService  completionService;

    AwaitingLifeCycle(LifeCycleCompletionService service) {
      completionService = service;
    }

    public void event1(String source, String payload) {
      broadcast(new MockEvent(source, payload));
    }

    public void addListener(MockListener listener) {
      listeners.add(listener);
    }

    protected void dispatchEvent(MockListener listener, MockEvent event) {
      listener.event1(event);
    }

    protected void broadcast(final MockEvent event) {
      addTasks(event);
    }

    protected void addTasks(final MockEvent event) {
      for (final MockListener listener : listeners) {
        completionService.addTask(new Callable<MockEvent>() {
          public MockEvent call() throws Exception {
            try {
              begin();
              dispatchEvent(listener, event);
            } catch (Exception e) {
            } finally {
              end();
            }
            return event;
          }
        });
      }
    }

    protected void begin() {
    }

    protected void end() {
    }
  }

}
