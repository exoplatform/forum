(function(utils, $) {
  var UISliderControl = {
    init : function(contId, mValue) {
      var container = $('#' + contId);
      var slideContainer = container.find('.slideContainer:first');
      var datas = {
        containerId : contId,
        widthValue : parseInt(slideContainer.width()),
        percent : 0,
        maxValue : mValue,
        currentMouse : 0,
        isDown : false
      };

      var circleDefault = container.find('.circleDefault:first');
      circleDefault.data('infoSlider', datas);

      circleDefault.on('mousedown', UISliderControl.start);
      container.on('mouseover', UISliderControl.execute);

      slideContainer.on('click', function(evt) {
        var slideContainer = $(this);
        var circleDefault = slideContainer.find('.circleDefault:first');

        if (circleDefault.data('infoSlider').currentMouse === 0) {
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
      var datas = circleDefault.data('infoSlider');
      if (typeof evt !== 'undefined') {
        datas.currentMouse = evt.clientX;
      } else if (currentMouse > 0) {
        datas.currentMouse = currentMouse;
      }
      datas.isDown = true;
      circleDefault.data('infoSlider', datas);
    },

    end : function(evt) {
      var id = $(document.body).attr('data-currentslider');
      if (id != null) {
        var container = $('#' + id);
        var circleDefault = container.find('.circleDefault:first');
        var datas = circleDefault.data('infoSlider');
        datas.isDown = false;
        circleDefault.data('infoSlider', datas);
        $(document.body).removeAttr('data-currentslider');
      }
    },

    runExecute : function(container, evt) {
      var circleDefault = container.find('.circleDefault:first');
      var datas = circleDefault.data('infoSlider');
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
          circleDefault.data('infoSlider', datas);
          circleDefault.css('left', newPercent + '%');
          container.find('.slideRange:first').css('width', newPercent + '%');
          //
          var point = (datas.maxValue * newPercent) / 100;
          container.find('input.uiSliderInput:first').val(parseInt(point));
        }
        $(document.body).attr('data-currentslider', container.attr('id'))
      }
    },

    reset : function() {

    }
  };
  $(document.body).on('mouseup', UISliderControl.end);
  return UISliderControl;
})(utils, gj);
