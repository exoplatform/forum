eXo.require("eXo.projects.Module") ;
eXo.require("eXo.projects.Product") ;

function getProduct(version) {
  var product = new Product();
  
  product.name = "eXoPortal" ;
  product.portalwar = "portal.war" ;
  product.codeRepo = "portal" ;//module in modules/portal/module.js
  product.serverPluginVersion = "${org.exoplatform.portal.version}"; // CHANGED for FORUM to match portal version. It was ${project.version}

  var kernel = Module.GetModule("kernel") ;
  var core = Module.GetModule("core") ;
  var ws = Module.GetModule("ws", {kernel : kernel, core : core});
  var eXoJcr = Module.GetModule("jcr", {kernel : kernel, core : core, ws : ws}) ;
  var portal = Module.GetModule("portal", {kernel : kernel, ws:ws, core : core, eXoJcr : eXoJcr});
  var FORUM = Module.GetModule("forum", {portal:portal, ws:ws});
  
  product.addDependencies(portal.portlet.exoadmin) ;
  product.addDependencies(portal.portlet.web) ;
  product.addDependencies(portal.portlet.dashboard) ;
  product.addDependencies(portal.eXoGadgetServer) ;
  product.addDependencies(portal.eXoGadgets) ;
  product.addDependencies(portal.webui.portal);
  
  product.addDependencies(portal.web.eXoResources);

  product.addDependencies(portal.web.portal);
  
  // Portal extension starter required by FORUM etension
  portal.starter = new Project("org.exoplatform.portal", "exo.portal.starter.war", "war", portal.version);
  portal.starter.deployName = "starter";
  //product.addDependencies(portal.starter);
  
  portal.fck = new Project("org.exoplatform.commons", "commons-fck", "war", "${org.exoplatform.commons.version}");
  portal.fck.deployName = "fck";
  product.addDependencies(portal.fck);
  
  //cometd (requried for FORUM)
  product.addDependencies(FORUM.comet.cometd);
  
  product.addDependencies(FORUM.webuiExt);
   
  // FORUM extension
  product.addDependencies(FORUM.commons.extension);//commons-extension in platform
  product.addDependencies(FORUM.component.upgrade);
  product.addDependencies(FORUM.component.common);
  product.addDependencies(FORUM.component.rendering);
  product.addDependencies(FORUM.component.bbcode);
  product.addDependencies(FORUM.application.common);
  product.addDependencies(FORUM.application.forumGadgets);
  product.addDependencies(FORUM.answer);
  product.addDependencies(FORUM.forum);
  product.addDependencies(FORUM.poll);
  product.addDependencies(FORUM.web.forumResources);
  product.addDependencies(FORUM.extension.webapp);

  // FORUM demo
  product.addDependencies(FORUM.demo.portal);
  product.addDependencies(FORUM.demo.cometd);
  product.addDependencies(FORUM.demo.rest);
  
  product.addDependencies(new Project("org.exoplatform.commons", "commons-component-common", "jar", "${org.exoplatform.commons.version}"));
  
  product.addServerPatch("tomcat", FORUM.server.tomcat.patch) ;
  //product.addServerPatch("jboss",  FORUM.server.jboss.patch) ;
  product.addServerPatch("jbossear",  FORUM.server.jboss.patchear) ;

  /* cleanup duplicated lib */
  product.removeDependency(new Project("commons-httpclient", "commons-httpclient", "jar", "3.0"));
  product.removeDependency(new Project("commons-collections", "commons-collections", "jar", "3.1"));
  product.removeDependency(new Project("commons-collections", "commons-collections", "jar", "3.2"));
  product.removeDependency(new Project("commons-lang", "commons-lang", "jar", "2.3")); // exclusion added by FORUM. lib dir un tomcat contains versions 2.3 and 2.4. Keeping the newest.
  product.removeDependency(new Project("org.apache.poi", "poi", "jar", "3.0.2-FINAL"));
  product.removeDependency(new Project("org.apache.poi", "poi-scratchpad", "jar", "3.0.2-FINAL"));

  product.module = FORUM ;
  product.dependencyModule = [ kernel, core, ws, eXoJcr];

  return product ;
}
