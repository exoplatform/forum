;(function($, window, document) {
  
  function UIPollPortlet() {
    this.obj = null;
    this.event = null;
    this.wait = false;
  };

  UIPollPortlet.prototype.OpenPrivateField = function(elm) {
    if(elm === 'DivCheckBox') elm = findId(elm);
    if (elm.exists()) {
      var parent = elm.parents('.OptionField');
      var childs = parent.find('div.Display');
      var input = elm.find('input.checkbox:first');
      if (input.exists()) {
        for(var i = 0; i < childs.length; i++) {
          if(input.attr('checked')) childs.eq(i).css('display', 'none');
          else childs.eq(i).css('display', 'block');
        }
      }
    }
  };

  UIPollPortlet.prototype.OverButton = function(object) {
    if($(object).attr('class').indexOf('Action') > 0){
      var str = '';
      for(var i = 0; i < $(object).attr('class').length - 6; i++) {
        str = str + $(object).attr('class').charAt(i);
      }
      $(object).attr('class', str);
    } else {
      $(object).attr('class', $(object).attr('class') + 'Action');
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
