;(function($, window, document) {
  
  function UIPollPortlet() {
    this.obj = null;
    this.event = null;
    this.wait = false;
  };

  UIPollPortlet.prototype.OpenPrivateField = function(elm) {
    var jelm = findId(elm) || $(elm);
    if (jelm.exists()) {
      var parent = jelm.parents('.OptionField');
      var childs = parent.find('div.Display');
      var input = jelm.find('input.checkbox:first');
      if (input.exists()) {
        if(input.attr('checked')){
          childs.hide();
        } else {
          childs.show();
        }
      }
    }
  };

  UIPollPortlet.prototype.expandCollapse = function(obj) {
    var forumToolbar = $(obj).parents('.ForumToolbar');
    var contentContainer = forumToolbar.next('div');
    if(contentContainer.css('display') != 'none') {
      contentContainer.css('display', 'none');
      $(obj).attr('class', 'IconRight ExpandButton').attr('title', $(obj).attr('expand'));
      forumToolbar.css('borderBottom', 'solid 1px #b7b7b7');
    } else {
      contentContainer.css('display', 'block');
      $(obj).attr('class', 'IconRight CollapseButton').attr('title', $(obj).attr('collapse'));
      forumToolbar.css('borderBottom', 'none');
    }
  };

  // Expose
  window.eXo = eXo || {};
  window.eXo.poll = eXo.poll || {} ;
  window.eXo.poll.UIPollPortlet = new UIPollPortlet();

})(gj, window, document);
