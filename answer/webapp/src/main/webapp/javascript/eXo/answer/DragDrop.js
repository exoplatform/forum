(function($, eventManager, window, document) {

  var DragDrop = {
    dragObject : null,
    targetClass : [],
    component : null,

    init : function(compid) {
      DragDrop.component = $.fn.findId(compid);
      DragDrop.component.find('li.faqCategory').on('mousedown', DragDrop.attach);

      var leftColumn = DragDrop.component.parents('#FAQViewCategoriesColumn');
      DragDrop.disableSelection(leftColumn);
    },

    attach : function(evt) {
      DragDrop.rootNode = this;
      DragDrop.hided = false;

      DragDrop.dragObject = this.cloneNode(true);

      var jDragObject = $(DragDrop.dragObject);

      jDragObject.css('width', $(this).width()).attr('class', 'faqDnDCategory');
      var ul = $('<ul class="faqDn"></ul>');
      ul.append(jDragObject);

      $('#UIPortalApplication').append(ul);

      DragDrop.mousePos = {
        x : evt.clientX,
        y : evt.clientY
      };
      DragDrop.setup([ "faqCategory", "faqBack", "faqTmpCategory" ]);
    },

    dragCallback : function(target) {
      if (DragDrop.lastTarget) {
        $(DragDrop.lastTarget).css('border', 'none');
        if ($(DragDrop.lastTarget).hasClass('faqHighlightCategory')) {
          $(DragDrop.lastTarget).removeClass('faqHighlightCategory');
        }
      }

      if (!target) {
        return;
      }

      DragDrop.lastTarget = target;

      var jTarget = $(target);
      if (jTarget.hasClass('faqBack')) {
        jTarget.trigger('click');
      }
      if (jTarget.hasClass('faqTmpCategory')) {
        jTarget.addClass('faqHighlightCategory');
      }
      jTarget.css('border', 'dotted 1px #cfcfcf');
      if (DragDrop.hided === false) {
        DragDrop.hideElement(DragDrop.rootNode);
      }
    },

    dropCallback : function(target) {
      if (DragDrop.lastTarget) {
        $(DragDrop.lastTarget).css('border', 'none');
      }
      if (target && DragDrop.isMoved) {
        var action = DragDrop.getAction(DragDrop.dragObject, target);
        if (action && action.length > 0) {
          $.globalEval(action);
        } else {
          DragDrop.showElement();
        }
      } else {
        DragDrop.showElement();
      }
    },

    setup : function(targetClass) {
      DragDrop.targetClass = targetClass;
      $(DragDrop.dragObject).css({
        'border' : 'solid 1px #cfcfcf',
        'background' : '#fff'
      });
      if($.browser.msie) {
        $(document).on('dragstart', function(e){
          return false;
        });
      }
      $(document).on('mousemove', DragDrop.onDrag);
      $(document).on('mouseup', DragDrop.onDrop);
    },

    onDrag : function(evt) {
      if (DragDrop.dragObject) {
        var jDragObject = $(DragDrop.dragObject);
        jDragObject.css({
          'left' : (evt.pageX + 2) + 'px',
          'top' : (evt.pageY + 2) + 'px'
        });
        var target = DragDrop.findTarget(evt);
        DragDrop.dragCallback(target);
      }
    },

    onDrop : function(evt) {
      evt = evt || window.event;
      DragDrop.isMoved = true;
      if (DragDrop.mousePos.x == evt.clientX && DragDrop.mousePos.y == evt.clientY) {
        DragDrop.isMoved = false;
      }
      if (DragDrop.dragObject) {
        var target = DragDrop.findTarget(evt);
        DragDrop.dropCallback(target);
      }
      DragDrop.endDrop();
    },
    
    endDrop : function() {
      delete DragDrop.dragObject;
      delete DragDrop.targetClass;
      delete DragDrop.hided;
      delete DragDrop.rootNode;
      $('#UIPortalApplication  > ul.faqDn').remove();
      
      if($.browser.msie) {
        $(document).off('dragstart');
      }
      $(document).off('mousemove', DragDrop.onDrag);
      $(document).off('mouseup', DragDrop.onDrop);
    },
    
    findTarget : function(evt) {
      var targetClass = DragDrop.targetClass;
      if (targetClass) {
        for(var i = 0; i < targetClass.length; ++i) {
          var target = eventManager.getEventTargetByClass(evt, targetClass[i]);
          if (target) {
            return target;
          }
        }
      }
      return null;
    },

    disableSelection : function(jelm) {
      jelm.attr('unselectable', 'on').css('user-select', 'none').on('selectstart', function() {
        return false;
      });
    },

    hideElement : function(obj) {
      $(obj).prev('li').css('display', 'none');
      $(obj).css('display', 'none');
      DragDrop.hided = true;
    },

    showElement : function() {
      if (!DragDrop.rootNode) {
        return;
      }
      DragDrop.hided = false;
      var preElement = $(DragDrop.rootNode).prev('li');
      if (preElement.exists()) {
        preElement.css('display', 'block');
      }
      $(DragDrop.rootNode).css('display', 'block');
      if (DragDrop.lastTarget) {
        $(DragDrop.lastTarget).css('border', 'none');
        if ($(DragDrop.lastTarget).hasClass('faqHighlightCategory')) {
          $(DragDrop.lastTarget).removeClass('faqHighlightCategory');
        }
      }
    },

    getAction : function(obj, target) {
      var info = $(obj).find('input.infoCategory:first');
      var actionLink = '';
      if ($(target).hasClass('faqTmpCategory')) {
        var preElement = $(target).prev('li');
        var top = ' ';
        if (!preElement.exists()) {
          preElement = $(target).next('li');
          top = 'top';
        }
        var preElementInfo = preElement.find('input.infoCategory:first');
        if ($.trim(info.attr('id')) == $.trim(preElementInfo.attr('id'))) {
          return actionLink;
        }
        actionLink = info.attr('value').replace("=objectId", ("=" + $.trim(info.attr('id')) + "," + $.trim(preElementInfo.attr('id')) + "," + top));
      } else if ($(target).hasClass('faqCategory')) {
        var targetInfo = $(target).find('input.infoCategory:first');
        actionLink = info.attr('value').replace("=objectId", "=" + $.trim(info.attr('id')) + "," + $.trim(targetInfo.attr('id')) );
        actionLink = actionLink.replace("ChangeIndex", "MoveCategoryInto");
      }
      return actionLink;
    }
  };

  return DragDrop;
})(gj, forumEventManager, window, document);
