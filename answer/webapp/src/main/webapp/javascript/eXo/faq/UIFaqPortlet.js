(function($, utils) {
  var FaqPortlet = {

    executeLink : function(evt) {
      var onclickAction = String(this.getAttribute('actions'));
      $.globalEval(onclickAction);
      utils.cancelEvent(evt);
      return false;
    },

    createLink : function(cpId, isAjax) {
      if (!isAjax || isAjax === 'false')
        return;
      var comp = findId(cpId);
      if (comp.exists()) {
        comp.find('a.ActionLink').on('click', this.executeLink);
      }
    },

    focusQuestion : function() {
      var as = $('a[href^="#Question"]');
      as.on('click', function() {
        var href = String(this.href);
        href = href.substring(href.indexOf('#') + 1);
        var elm = $('*[name="' + href + '"]');
        if (elm.exists()) {
          elm.parent()[0].scrollIntoView(true);
        }
      });
    },
    
    backtotop: function() {
    	$(".backtotop").hide();
    	
    	$(window).scroll(function() {
    		if($(this).scrollTop() > 200) {
    			$(".backtotop").fadeIn();
    		} else {
    			$(".backtotop").fadeOut();
    		}
    	});
    	
    	$(".backtotop").on('click', function() {
    		 $("html, body").animate({ scrollTop: 150 }, "slow");
    		 return false;
    	});
    }
  };
  return FaqPortlet;
})(gj, utils);
