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
    new Project("org.exoplatform.commons", "exo.platform.commons.extension.webapp", "war", commonsVersion);
  module.commons.extension.deployName = "commons-extension";
  
  module.comet = {};
  module.comet.cometd =
    new Project("org.exoplatform.commons", "exo.platform.commons.comet.webapp", "war", commonsVersion).
    addDependency(new Project("org.mortbay.jetty", "cometd-bayeux", "jar", "${org.mortbay.jetty.cometd-bayeux.version}")).
    addDependency(new Project("org.mortbay.jetty", "jetty-util", "jar", "${org.mortbay.jetty.jetty-util.version}")).
    addDependency(new Project("org.mortbay.jetty", "cometd-api", "jar", "${org.mortbay.jetty.cometd-api.version}")).
    addDependency(new Project("org.exoplatform.commons", "exo.platform.commons.comet.service", "jar", commonsVersion));
  module.comet.cometd.deployName = "cometd";
  
  module.webuiExt = new Project("org.exoplatform.commons", "exo.platform.commons.webui.ext", "jar", commonsVersion);

  
  // FORUM components
  module.component = {};
  module.component.common = new Project("org.exoplatform.forum", "exo.forum.component.common", "jar", module.version).
                            addDependency(new Project("org.exoplatform.commons", "exo.platform.commons.webui", "jar", commonsVersion));
  module.component.rendering = new Project("org.exoplatform.forum", "exo.forum.component.rendering", "jar", module.version).
                            addDependency(new Project("org.exoplatform.forum", "exo.forum.component.macro.iframe", "jar", module.version)).
                            addDependency(new Project("org.exoplatform.forum", "exo.forum.component.macro.jira", "jar", module.version));
  module.component.bbcode = new Project("org.exoplatform.forum", "exo.forum.component.bbcode", "jar", module.version);

  module.component.upgrade = new Project("org.exoplatform.commons", "exo.platform.commons.component.upgrade", "jar", commonsVersion).
    addDependency(new Project("org.exoplatform.commons", "exo.platform.commons.component.product", "jar", commonsVersion));

  
  // FORUM application common
  module.application = {};
  module.application.common = new Project("org.exoplatform.forum", "exo.forum.application.common", "jar", module.version).
    addDependency(new Project("org.exoplatform.forum", "exo.forum.component.upgrade", "jar",  module.version));
  module.application.forumGadgets = new Project("org.exoplatform.forum", "exo.forum.gadgets", "war", module.version);
  module.application.forumGadgets.deployName = "forum-gadgets";

  // FORUM
  module.forum = new Project("org.exoplatform.forum", "exo.forum.webapp", "war", module.version).
    addDependency(ws.frameworks.json).
    addDependency(new Project("org.exoplatform.forum", "exo.forum.service", "jar",  module.version));
  module.forum.deployName = "forum";

  
  // Answer
  module.answer =
    new Project("org.exoplatform.forum", "exo.answer.webapp", "war", module.version).
    addDependency(new Project("org.exoplatform.forum", "exo.answer.service", "jar",  module.version));
  module.answer.deployName = "answer";


  // POLL
  module.poll = 
    new Project("org.exoplatform.forum", "exo.poll.webapp", "war", module.version) .
    addDependency(new Project("org.exoplatform.forum", "exo.poll.service", "jar",  module.version));
  module.poll.deployName = "poll";

  // FORUM we resources and services
  module.web = {}
  module.web.forumResources = 
    new Project("org.exoplatform.forum", "exo.forum.web.forumResources", "war", module.version) ;

  // FORUM extension for tomcat
  module.extension = {};
  module.extension.webapp = 
    new Project("org.exoplatform.forum", "exo.forum.extension.webapp", "war", module.version);
  module.extension.webapp.deployName = "forum-extension";
   
  module.server = {}
  module.server.tomcat = {}
  module.server.tomcat.patch =
    new Project("org.exoplatform.forum", "exo.forum.server.tomcat.patch", "jar", module.version);
	
  module.server.jboss = {}
  module.server.jboss.patchear =
    new Project("org.exoplatform.forum", "exo.forum.server.jboss.patch-ear", "jar", module.version);
   
  // FORUM demo 
  module.demo = {};
  // demo portal
  module.demo.portal =
    new Project("org.exoplatform.forum", "exo.forum.demo.webapp", "war", module.version).
    addDependency(new Project("org.exoplatform.forum", "exo.forum.component.injector", "jar", module.version)).
    addDependency(new Project("org.exoplatform.forum", "exo.forum.demo.config", "jar", module.version));
  module.demo.portal.deployName = "forumdemo";  
	
  module.demo.cometd=
    new Project("org.exoplatform.forum", "exo.forum.demo.cometd-war", "war", module.version);
  module.demo.cometd.deployName = "cometd-forumdemo";
	   
  // demo rest endpoint	   
  module.demo.rest =
    new Project("org.exoplatform.forum", "exo.forum.demo.rest-forumdemo", "war", module.version).
    addDependency(ws.frameworks.servlet);
  module.extension.deployName = "rest-forumdemo"; 
   
  return module;
}
