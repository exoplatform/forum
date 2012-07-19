;(function($, window, document) {
  
  function UIFaqPortlet() {};
  
  UIFaqPortlet.prototype.executeLink = function (evt) {
    var onclickAction = String(this.getAttribute('actions'));
    $.globalEval(onclickAction);
    eXo.forum.ForumUtils.cancelEvent(evt);
    return false;
  };
  
  UIFaqPortlet.prototype.createLink = function (cpId, isAjax) {
    if (!isAjax || isAjax === 'false') return;
    var comp = findId(cpId);
    comp.find('a.ActionLink').on('click', this.executeLink);
  };
  
  UIFaqPortlet.prototype.checkedNode = function (elm) {
    var input = $(elm).find('input');
    var parentNode = input.parents('.FAQDomNode');
    var ancestorNode = parentNode.parents('.FAQDomNode');
    if (ancestorNode.exists()) {
      firstInput = ancestorNode.find('input.checkbox:first');
      if (input.attr('checked') && !firstInput.attr('checked')) {
        var msg = $('#viewerSettingMsg');
        if (msg.exists()) alert(msg.html());
        else alert('You need to check on parent or ancestor of this category first!');
        input.attr('checked', false);
        input.attr('disabled', true);
      }
    }
  
    var containerChild = parentNode.find('div.FAQChildNodeContainer');
    if (containerChild.exists()) {
      var checkboxes = containerChild.find('input');
      for (var i = 0; i < checkboxes.length; ++i) {
        checkboxes.eq(i).attr('checked', input.attr('checked'));
        if (!input.attr('checked')) checkboxes.eq(i).attr('disabled', true);
        else checkboxes.eq(i).attr('disabled', false);
      }
    }
  };
  
  UIFaqPortlet.prototype.treeView = function (id) {
    var obj = find(id);
    if (obj.exists()) {
      if (obj.css('display') == '' || obj.css('display') === 'none') obj.css('display', 'block');
      else obj.css('display', 'none');
    }
  };
  
  // Expose
  window.eXo = eXo || {};
  window.eXo.faq = eXo.faq || {} ;
  window.eXo.faq.UIFaqPortlet = new UIFaqPortlet();

})(gj, window, document);
