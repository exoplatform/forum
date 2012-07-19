;(function($, window, document) {
  var ForumTotalJob = {
    init : function(eXoUser, eXoToken, contextName) {
      if (String(eXoToken)) {
        if (!eXo.core.Cometd.isConnected()) {
          eXo.core.Cometd.url = '/' + contextName + '/cometd';
          eXo.core.Cometd.exoId = eXoUser;
          eXo.core.Cometd.exoToken = eXoToken;
          eXo.core.Cometd.addOnConnectionReadyCallback(ForumTotalJob.subcribeCometdTopics);
          eXo.core.Cometd.init();
        } else {
          ForumTotalJob.subcribeCometdTopics();
        }
      }
    },
    subcribeCometdTopics : function() {
      eXo.core.Cometd.subscribe('/eXo/Application/Forum/messages', function(eventObj) {
        ForumTotalJob.alarm(eventObj);
      });
    },
    alarm : function(eventObj) {
      var a = eXo.core.JSON.parse(eventObj.data);
      var pr = $('#PendingJob');
      if (pr.exists()) {
        var str = pr.html();
        str = str.substring(0, str.indexOf('=') + 1);
        pr.html(str + (((a.categoryName) * 1 > 0) ? '"font-weight:bold; text-decoration:blink;">' : '"font-weight:bold;">') + a.categoryName + "</span>)");
      }
      return;
    }
  };
  
  // Expose
  window.eXo = eXo || {};
  window.eXo.forum = eXo.forum || {};
  window.eXo.forum.ForumTotalJob = ForumTotalJob;
})(gj, window, document);