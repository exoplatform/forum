(function(utils, gj) {
  var AnswerUtils = {
    checkedNode : function(elm) {
      var input = gj(elm).find('input');
      var parentNode = input.parents('.FAQDomNode');
      var ancestorNode = parentNode.parents('.FAQDomNode');
      if (ancestorNode.exists()) {
        firstInput = ancestorNode.find('input.checkbox:first');
        if (input.attr('checked') && !firstInput.attr('checked')) {
          var msg = gj('#viewerSettingMsg');
          if (msg.exists())
            alert(msg.html());
          else
            alert('You need to check on parent or ancestor of this category first!');
          input.attr('checked', false);
          input.attr('disabled', true);
        }
      }

      var containerChild = parentNode.find('div.FAQChildNodeContainer');
      if (containerChild.exists()) {
        var checkboxes = containerChild.find('input');
        for ( var i = 0; i < checkboxes.length; ++i) {
          checkboxes.eq(i).attr('checked', input.attr('checked'));
          if (!input.attr('checked'))
            checkboxes.eq(i).attr('disabled', true);
          else
            checkboxes.eq(i).attr('disabled', false);
        }
      }
    },
    treeView : function(id) {
      var obj = findId(id);
      if (obj.exists()) {
        if (obj.css('display') == '' || obj.css('display') === 'none')
          obj.show();
        else
          obj.hide();
      }
    }
  };
  // Expose
  window.eXo = eXo || {};
  window.eXo.answer = eXo.answer || {};
  window.eXo.answer.AnswerUtils = AnswerUtils;
  return AnswerUtils;
})(forumUtils, gj);
