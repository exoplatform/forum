/**
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

package org.exoplatform.poll.webservice;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.MultivaluedMap;

import org.exoplatform.component.test.ConfigurationUnit;
import org.exoplatform.component.test.ConfiguredBy;
import org.exoplatform.component.test.ContainerScope;
import org.exoplatform.poll.base.BaseTestCase;
import org.exoplatform.services.jcr.ext.app.SessionProviderService;
import org.exoplatform.services.jcr.ext.common.SessionProvider;
import org.exoplatform.services.rest.ContainerResponseWriter;
import org.exoplatform.services.rest.impl.ApplicationContextImpl;
import org.exoplatform.services.rest.impl.ContainerRequest;
import org.exoplatform.services.rest.impl.ContainerResponse;
import org.exoplatform.services.rest.impl.EnvironmentContext;
import org.exoplatform.services.rest.impl.InputHeadersMap;
import org.exoplatform.services.rest.impl.MultivaluedMapImpl;
import org.exoplatform.services.rest.impl.ProviderBinder;
import org.exoplatform.services.rest.impl.RequestHandlerImpl;
import org.exoplatform.services.rest.impl.ResourceBinder;
import org.exoplatform.services.rest.tools.DummyContainerResponseWriter;
import org.exoplatform.services.security.ConversationState;
import org.exoplatform.services.security.Identity;
import org.exoplatform.services.security.MembershipEntry;

@ConfiguredBy({
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.portal-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/exo.portal.component.identity-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.poll.component.core.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.poll.component.service.test.configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.poll.test.jcr-configuration.xml"),
  @ConfigurationUnit(scope = ContainerScope.PORTAL, path = "conf/standalone/exo.poll.test.portal-configuration.xml")
})
public abstract class AbstractResourceTest extends BaseTestCase {
  
  protected SessionProvider sessionProvider;
  protected SessionProviderService sessionProviderService;
  protected RequestHandlerImpl requestHandler;
  protected ResourceBinder resourceBinder;
  protected ProviderBinder providerBinder;
  private Collection<MembershipEntry> membershipEntries = new ArrayList<MembershipEntry>();
  
  public void setUp() throws Exception {
    sessionProviderService = (SessionProviderService) getContainer().
        getComponentInstanceOfType(SessionProviderService.class);
    resourceBinder = (ResourceBinder) getContainer().getComponentInstanceOfType(ResourceBinder.class);
    requestHandler = (RequestHandlerImpl) getContainer().getComponentInstanceOfType(RequestHandlerImpl.class);
    ProviderBinder.setInstance(new ProviderBinder());
    providerBinder = ProviderBinder.getInstance();
    ApplicationContextImpl.setCurrent(new ApplicationContextImpl(null, null, providerBinder));
    resourceBinder.clear();
    begin();
  }
  
  /**
   * registry resource object
   *
   * @param resource
   * @return
   * @throws Exception
   */
  public boolean registry(Object resource) throws Exception {
    // container.registerComponentInstance(resource);
    return resourceBinder.bind(resource);
  }
  /**
   * registry resource class
   *
   * @param resourceClass
   * @return
   * @throws Exception
   */
  public boolean registry(Class<?> resourceClass) throws Exception {
    // container.registerComponentImplementation(resourceClass.getName(),
    // resourceClass);
    return resourceBinder.bind(resourceClass);
  }

  /**
   * unregistry resource object
   *
   * @param resource
   * @return
   * @deprecated Use {@link #addResource(Object, javax.ws.rs.core.MultivaluedMap)} instead.
   *             Will be removed by 1.3.x.
   */
  @Deprecated
  public boolean unregistry(Object resource) {
    // container.unregisterComponentByInstance(resource);
    return resourceBinder.unbind(resource.getClass());
  }

  /**
   * unregistry resource class
   *
   * @param resourceClass
   * @return
   * @deprecated Use {@link #removeResource(Class)} instead.
   *             Will be removed by 1.3.x
   */
  @Deprecated
  public boolean unregistry(Class<?> resourceClass) {
    // container.unregisterComponent(resourceClass.getName());
    return resourceBinder.unbind(resourceClass);
  }

   /**
    * Registers supplied class as per-request root resource if it has valid
    * JAX-RS annotations and no one resource with the same UriPattern already
    * registered.
    *
    * @param resourceClass class of candidate to be root resource
    * @param properties optional resource properties. It may contains additional
    *        info about resource, e.g. description of resource, its
    *        responsibility, etc. This info can be retrieved
    *        {@link org.exoplatform.services.rest.ObjectModel#getProperties()}. This parameter may be
    *        <code>null</code>
    */
  public void addResource(final Class<?> resourceClass, MultivaluedMap<String, String> properties) {
    resourceBinder.addResource(resourceClass, properties);
  }

  /**
    * Registers supplied Object as singleton root resource if it has valid JAX-RS
    * annotations and no one resource with the same UriPattern already
    * registered.
    *
    * @param resource candidate to be root resource
    * @param properties optional resource properties. It may contains additional
    *        info about resource, e.g. description of resource, its
    *        responsibility, etc. This info can be retrieved
    *        {@link org.exoplatform.services.rest.ObjectModel#getProperties()}. This parameter may be
    *        <code>null</code>
   */
  public void addResource(final Object resource, MultivaluedMap<String, String> properties) {
    resourceBinder.addResource(resource, properties);
  }


  /**
   * Removes the resource instance of provided class from root resource container.
   *
   * @param clazz the class of resource
   */
  public void removeResource(Class clazz) {
    resourceBinder.removeResource(clazz);
  }

  
  public void tearDown() throws Exception {
    endSession();
    end();
  }
  
  protected void startSystemSession() {
    sessionProvider = sessionProviderService.getSystemSessionProvider(null);
  }
  
  protected void endSession() {
    sessionProviderService.removeSessionProvider(null);
    ConversationState.setCurrent(null);
    startSystemSession();
  }
  
  protected void startSessionAs(String user) {
    setMembershipEntry("/platform/users", "member", true);
    Identity identity = new Identity(user, membershipEntries);
    ConversationState state = new ConversationState(identity);
    ConversationState.setCurrent(state);
    sessionProviderService.setSessionProvider(null, new SessionProvider(state));
    sessionProvider = sessionProviderService.getSessionProvider(null);
  }
  
  
  private void setMembershipEntry(String group, String membershipType, boolean isNew) {
    MembershipEntry membershipEntry = new MembershipEntry(group, membershipType);
    if (isNew) {
      membershipEntries.clear();
    }
    membershipEntries.add(membershipEntry);
  }

  public ContainerResponse service(String method,
                                   String requestURI,
                                   String baseURI,
                                   Map<String, List<String>> headers,
                                   byte[] data,
                                   ContainerResponseWriter writer) throws Exception {

    if (headers == null) {
      headers = new MultivaluedMapImpl();
    }

    ByteArrayInputStream in = null;
    if (data != null) {
      in = new ByteArrayInputStream(data);
    }

    EnvironmentContext envctx = new EnvironmentContext();
    HttpServletRequest httpRequest = new PollMockHttpServletRequest("",
                                                                      in,
                                                                      in != null ? in.available() : 0,
                                                                      method,
                                                                      headers);
    envctx.put(HttpServletRequest.class, httpRequest);
    EnvironmentContext.setCurrent(envctx);
    ContainerRequest request = new ContainerRequest(method,
                                                    new URI(requestURI),
                                                    new URI(baseURI),
                                                    in,
                                                    new InputHeadersMap(headers));
    ContainerResponse response = new ContainerResponse(writer);
    requestHandler.handleRequest(request, response);
    return response;
  }

  public ContainerResponse service(String method,
                                   String requestURI,
                                   String baseURI,
                                   MultivaluedMap<String, String> headers,
                                   byte[] data) throws Exception {
    return service(method, requestURI, baseURI, headers, data, new DummyContainerResponseWriter());
  }
  
}
