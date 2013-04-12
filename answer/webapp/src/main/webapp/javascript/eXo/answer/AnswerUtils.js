(function(utils, $) {
  var AnswerUtils = {
    initTreeNode : function(componentId) {
      var container = $.fn.findId(componentId);
      var treeContainer = container.find('div.treeContainer:first');
      treeContainer.find('.nodeGroup').hide();
      treeContainer.find('.nodeGroup:first').show();
    },
    showTreeNode : function (obj) {
      var thiz = $(obj);
      var treeContainer = thiz.parents('div.treeContainer:first');
      treeContainer.find('.nodeGroup').hide();
      treeContainer.find('.nodeGroup:first').show();
      
      var parentNode = thiz.parents('li.node:first');
      var childrenNodeGroup = parentNode.find('ul.nodeGroup:first').show();
      
      var allNodes = treeContainer.find('a.uiIconNode');
      $.each(allNodes, function(id, elm) {
         var thizz = $(elm);
         if(thizz.hasClass('uiIconEmpty')) {
           thizz.removeClass('nodeSelected');
         } else {
           thizz.attr('class', 'uiIconNode collapseIcon');
         }
      });
      
      if(thiz.hasClass('uiIconEmpty') == false) {
        thiz.attr('class', 'uiIconNode expandIcon nodeSelected');
      } else {
        thiz.addClass('nodeSelected');
      }
      
      AnswerUtils.showNode(thiz);
    },
    showNode : function (obj) {
      if(!obj.parents('div.treeContainer').exists()) return;
      var parentNode = obj.parents('ul.nodeGroup:first').show().parents('li.node:first');
      if(parentNode.exists()) {
        var nThiz = parentNode.find('a.uiIconNode:first');
        if(nThiz.hasClass('uiIconEmpty') == false) {
          nThiz.attr('class', 'uiIconNode expandIcon');
        }
        AnswerUtils.showNode(nThiz);
      }
    },
    checkedNode : function(obj, evt) {
      var thizz = $(obj);
      if(obj.checked === true) {
        var nodes = thizz.parents('.nodeGroup:first').parents('.node');
        var inputs = nodes.find('input[type=checkbox]:first');
        inputs.prop("checked", obj.checked);
      }

      var nodeGroup = thizz.parents('.node:first').find('.nodeGroup:first');
      if (nodeGroup.length > 0) {
        var inputChilds = nodeGroup.find('.node').find('input[type=checkbox]:first');
        inputChilds.prop("checked", obj.checked);
      }
      utils.cancelEvent(evt);
    }
  };
  // Expose
  window.eXo = eXo || {};
  window.eXo.answer = eXo.answer || {};
  window.eXo.answer.AnswerUtils = AnswerUtils;
  return AnswerUtils;
})(forumUtils, gj);