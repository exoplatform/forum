(function(utils, $) {
  var UISliderControl = {
    parent : null,
    init : function(contId, mValue) {
      var container = $('#' + contId);
      UISliderControl.parent = container.parents('form:first').parents('div:first');
      var slideContainer = container.find('.slideContainer:first');
      
      var datas = {
        containerId : contId,
        widthValue : parseInt(slideContainer.width()),
        percent : 0,
        maxValue : mValue,
        currentMouse : 0,
        isDown : false
      };
      //
      datas = $.extend(true, {}, datas, UISliderControl.fromCookie(contId));
      
      var circleDefault = container.find('.circleDefault:first');
      if(UISliderControl.parent.data(contId)) {
        datas = $.extend(true, {}, datas, UISliderControl.parent.data(contId));
      } else {
        UISliderControl.parent.data(contId, datas);
      }
      circleDefault.css('left', datas.percent + '%');
      container.find('.slideRange:first').css('width', datas.percent + '%');
      var point = (datas.maxValue * datas.percent) / 100;
      container.find('input.uiSliderInput:first').val(parseInt(point));

      //
      circleDefault.data('parent-id', contId);
      slideContainer.data('parent-id', contId);
      //
      circleDefault.on('mousedown', UISliderControl.start);
      container.on('mouseover', UISliderControl.execute);

      slideContainer.on('click', function(evt) {
        var slideContainer = $(this);
        var circleDefault = slideContainer.find('.circleDefault:first');

        if (UISliderControl.parent.data(slideContainer.data('parent-id')).currentMouse === 0) {
          var Browser = eXo.core.Browser;
          var X = Browser.findMouseRelativeX(slideContainer, evt, false);
          X = evt.clientX - X + 5;
          UISliderControl.saveInfoStart(circleDefault, X);
        } else {
          UISliderControl.saveInfoStart(circleDefault, 0);
        }

        var container = slideContainer.parents('.uiFormSliderInput:first');
        UISliderControl.runExecute(container, evt);
        UISliderControl.end(evt);
      });
    },

    start : function(evt) {
      var circleDefault = $(this);
      UISliderControl.saveInfoStart(circleDefault, 0, evt);
    },

    execute : function(evt) {
      var container = $(this);
      UISliderControl.runExecute(container, evt);
    },

    saveInfoStart : function(circleDefault, currentMouse, evt) {
      var datas = UISliderControl.readData(circleDefault);
      if (typeof evt !== 'undefined') {
        datas.currentMouse = evt.clientX;
      } else if (currentMouse > 0) {
        datas.currentMouse = currentMouse;
      }
      datas.isDown = true;
      //
      UISliderControl.saveData(circleDefault, datas);
    },

    end : function(evt) {
      var id = $(document.body).attr('data-currentslider');
      if (id != null) {
        var container = $('#' + id);
        var circleDefault = container.find('.circleDefault:first');
        
        var datas = UISliderControl.readData(circleDefault);
        datas.isDown = false;
        //
        UISliderControl.saveData(circleDefault, datas);
        //
        $(document.body).removeAttr('data-currentslider');
      }
    },

    runExecute : function(container, evt) {
      var circleDefault = container.find('.circleDefault:first');
      var datas = UISliderControl.readData(circleDefault);
      
      if (datas.isDown === true) {
        var next = evt.clientX;
        var deltaMove = next - datas.currentMouse;
        if (deltaMove !== 0) {
          var widthValue = datas.widthValue;
          var deltaPercent = (deltaMove / widthValue) * 100;
          var newPercent = datas.percent + deltaPercent;
          if (newPercent < 0) {
            newPercent = 0;
          } else if (newPercent > 100) {
            newPercent = 100;
          }
          datas.percent = newPercent;
          datas.currentMouse = next;
          //
          UISliderControl.saveData(circleDefault, datas);
          
          circleDefault.css('left', newPercent + '%');
          container.find('.slideRange:first').css('width', newPercent + '%');
          //
          var point = (datas.maxValue * newPercent) / 100;
          container.find('input.uiSliderInput:first').val(parseInt(point));
        }
        $(document.body).attr('data-currentslider', container.attr('id'))
      }
    },
    
    reset : function(elm) {
      if (typeof elm === 'string') {
        elm = $('#' + elm);
      }
      if (elm && elm.length > 0) {
        var circleDefault = elm.find('.circleDefault:first').css('left', '0px');
        var datas = UISliderControl.readData(circleDefault);
        datas.percent = 0;
        datas.currentMouse = 0;
        //
        UISliderControl.saveData(circleDefault, datas);
        elm.find('.slideRange:first').css('width', '0px');
        elm.find('input.uiSliderInput:first').val('0');
      }
    },
    readData : function(child) {
      return UISliderControl.parent.data(child.data('parent-id'));
    },
    saveData : function(child, datas) {
      UISliderControl.parent.data(child.data('parent-id'), datas);
      utils.setCookies(child.data('parent-id')+'_currentMouse', datas.currentMouse*1, 1);
      utils.setCookies(child.data('parent-id')+'_percent', datas.percent*1, 1);
    },
    fromCookie : function(parentId) {
      var currentMouse = utils.getCookie(parentId + '_currentMouse');
      if(!currentMouse) {
        currentMouse = 0;
      }
      var percent = utils.getCookie(parentId + '_percent');
      if(!percent) {
        percent = 0;
      }
      return { "currentMouse" : currentMouse*1, "percent" : percent*1};
    }
  };

  window.eXo = window.eXo || {};
  window.eXo.forum = window.eXo.forum || {};
  window.eXo.forum.UISliderControl = {};
  window.eXo.forum.UISliderControl.reset = UISliderControl.reset;

  $(document.body).on('mouseup', UISliderControl.end);
  return UISliderControl;
})(utils, gj);
