(function(Cometd, $, window, document) {
  var ForumTotalJob = {
    init : function(eXoUser, eXoToken, contextName) {
      if (String(eXoToken)) {
        if (!Cometd.isConnected()) {
          Cometd.url = '/' + contextName + '/cometd';
          Cometd.exoId = eXoUser;
          Cometd.exoToken = eXoToken;
          Cometd.addOnConnectionReadyCallback(ForumTotalJob.subcribeCometdTopics);
          Cometd.init(Cometd.url);
        } else {
          ForumTotalJob.subcribeCometdTopics();
        }
      }
    },
    subcribeCometdTopics : function() {
      Cometd.subscribe('/eXo/Application/Forum/messages', function(eventObj) {
        ForumTotalJob.alarm(eventObj);
      });
    },
    alarm : function(eventObj) {
      var a = JSON.parse(eventObj.data);
      var pr = $('#PendingJob');
      if (pr.exists()) {
        var str = pr.html();
        str = str.substring(0, str.indexOf('=') + 1);
        pr.html(str + (((a.categoryName) * 1 > 0) ? '"font-weight:bold; text-decoration:blink;">' : '"font-weight:bold;">') + a.categoryName + "</span>)");
      }
      return;
    }
  };

  return ForumTotalJob;

})(cometd, gj, window, document);
