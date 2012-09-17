eXo.require("eXo.projects.Module");
eXo.require("eXo.projects.Product");

function getModule(params) {
  var ws = params.ws;
  var portal = params.portal;
  var module = new Module();

  module.version = "${project.version}";
  module.relativeMavenRepo = "org/exoplatform/forum";
  module.relativeSRCRepo = "forum";
  module.name = "forum";
  
  var commonsVersion = "${org.exoplatform.commons.version}";

  module.commons = {};
  module.commons.extension = 
    new Project("org.exoplatform.commons", "commons-extension-webapp", "war", commonsVersion);
  module.commons.extension.deployName = "commons-extension";
  
  module.comet = {};
  module.comet.cometd =
    new Project("org.exoplatform.commons", "commons-comet-webapp", "war", commonsVersion).
    addDependency(new Project("org.mortbay.jetty", "cometd-bayeux", "jar", "${org.mortbay.jetty.cometd-bayeux.version}")).
    addDependency(new Project("org.mortbay.jetty", "jetty-util", "jar", "${org.mortbay.jetty.jetty-util.version}")).
    addDependency(new Project("org.mortbay.jetty", "cometd-api", "jar", "${org.mortbay.jetty.cometd-api.version}")).
    addDependency(new Project("org.exoplatform.commons", "commons-comet-service", "jar", commonsVersion));
  module.comet.cometd.deployName = "cometd";
  
  module.webuiExt = new Project("org.exoplatform.commons", "commons-webui-ext", "jar", commonsVersion);

  
  // FORUM components
  module.component = {};
  module.component.common = new Project("org.exoplatform.forum", "forum-component-common", "jar", module.version).
                            addDependency(new Project("org.exoplatform.commons", "commons-webui-component", "jar", commonsVersion));

  module.component.rendering = new Project("org.exoplatform.forum", "forum-component-rendering", "jar", module.version);

  module.component.bbcode = new Project("org.exoplatform.forum", "forum-component-bbcode", "jar", module.version);

  module.component.upgrade = new Project("org.exoplatform.commons", "commons-component-upgrade", "jar", commonsVersion).
    addDependency(new Project("org.exoplatform.commons", "commons-component-product", "jar", commonsVersion));

  
  // FORUM application common
  module.application = {};
  module.application.common = new Project("org.exoplatform.forum", "forum-application-common", "jar", module.version).
    addDependency(new Project("org.exoplatform.forum", "forum-component-upgrade", "jar",  module.version));
  module.application.forumGadgets = new Project("org.exoplatform.forum", "forum-gadgets", "war", module.version);
  module.application.forumGadgets.deployName = "forum-gadgets";

  // FORUM
  module.forum = new Project("org.exoplatform.forum", "forum-forum-webapp", "war", module.version).
    addDependency(ws.frameworks.json).
    addDependency(new Project("org.exoplatform.forum", "forum-forum-service", "jar",  module.version));
  module.forum.deployName = "forum";

  
  // Answer
  module.answer =
    new Project("org.exoplatform.forum", "forum-answer-webapp", "war", module.version).
    addDependency(new Project("org.exoplatform.forum", "forum-answer-service", "jar",  module.version));
  module.answer.deployName = "answer";


  // POLL
  module.poll = 
    new Project("org.exoplatform.forum", "forum-poll-webapp", "war", module.version) .
    addDependency(new Project("org.exoplatform.forum", "forum-poll-service", "jar",  module.version));
  module.poll.deployName = "poll";

  // FORUM we resources and services
  module.web = {}
  module.web.forumResources = 
    new Project("org.exoplatform.forum", "forum-forumResources", "war", module.version) ;
  module.web.forumResources.deployName = "forumResources";
  
  // FORUM extension for tomcat
  module.extension = {};
  module.extension.webapp = 
    new Project("org.exoplatform.forum", "forum-extension-webapp", "war", module.version);
  module.extension.webapp.deployName = "forum-extension";
   
  module.server = {}
  module.server.tomcat = {}
  module.server.tomcat.patch =
    new Project("org.exoplatform.forum", "forum-server-tomcat-patch", "jar", module.version);
	
  module.server.jboss = {}
  module.server.jboss.patchear =
    new Project("org.exoplatform.forum", "forum-server-jboss-patch-ear", "jar", module.version);
   
  // FORUM demo 
  module.demo = {};
  // demo portal
  module.demo.portal =
    new Project("org.exoplatform.forum", "forum-demo-webapp", "war", module.version).
    addDependency(new Project("org.exoplatform.forum", "forum-component-injector", "jar", module.version)).
    addDependency(new Project("org.exoplatform.forum", "forum-demo-config", "jar", module.version));
  module.demo.portal.deployName = "forumdemo";  
	
  module.demo.cometd=
    new Project("org.exoplatform.forum", "forum-demo-cometd-war", "war", module.version);
  module.demo.cometd.deployName = "cometd-forumdemo";
	   
  // demo rest endpoint	   
  module.demo.rest =
    new Project("org.exoplatform.forum", "forum-demo-rest-forumdemo", "war", module.version).
    addDependency(ws.frameworks.servlet);
  module.demo.rest.deployName = "rest-forumdemo"; 
  
  //xwiki-rendering
  module.component.bbcode. 
    addDependency(new Project("org.exoplatform.wiki", "wiki-renderer", "jar", "${org.exoplatform.wiki.version}")).
    addDependency(new Project("org.exoplatform.wiki", "wiki-macros-iframe", "jar", "${org.exoplatform.wiki.version}")).
    addDependency(new Project("com.google.gwt", "gwt-servlet", "jar",  "${gwt.version}")).
    addDependency(new Project("com.google.gwt", "gwt-user", "jar",  "${gwt.version}")).
    addDependency(new Project("javax.inject", "javax.inject", "jar",  "${javax.inject.version}")).
    addDependency(new Project("net.sourceforge.cssparser", "cssparser", "jar",  "${cssparser.version}")).
    addDependency(new Project("org.apache.commons", "commons-lang3", "jar",  "${org.apache.commons.version}")).
    addDependency(new Project("javax.validation", "validation-api", "jar",  "${javax.validation.version}")).
    addDependency(new Project("org.python", "jython-standalone", "jar",  "${jython-standalone.version}")).
    addDependency(new Project("pygments", "pygments", "jar",  "${pygments.version}")).
    addDependency(new Project("net.sourceforge.htmlcleaner", "htmlcleaner", "jar",  "${net.sourceforge.htmlcleaner.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-configuration-api", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-context", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-component-api", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-component-default", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-properties", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-xml", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-script", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.commons", "xwiki-commons-text", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-api", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-wikimodel", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-xwiki20", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-xwiki21", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-xhtml", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-confluence", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-syntax-plain", "jar",  "${org.xwiki.platform.version}")).    
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-transformation-macro", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-transformation-icon", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-macro-toc", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-macro-box", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-macro-message", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.platform", "xwiki-platform-rendering-macro-code", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.rendering", "xwiki-rendering-wikimodel", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.platform", "xwiki-platform-model", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.xwiki.platform", "xwiki-platform-wysiwyg-client", "jar",  "${org.xwiki.platform.version}")).
    addDependency(new Project("org.wikimodel", "org.wikimodel.wem", "jar",  "${org.wikimodel.version}"));
  
   
  return module;
}
