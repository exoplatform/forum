(function(utils, gj) {
  var UISliderControl = {
    container : null,
    object : null,
    parent : null,
    inputField : null,

    start : function(obj, evt) {
      this.container = obj;
      this.object = gj(obj).find('div.SliderPointer').eq(0)[0];
      this.parent = gj(obj).parent();
      this.inputField = this.parent.find('input').eq(0)[0];
      var mouseX = eXo.core.Browser.findMouseRelativeX(obj, gj.event.fix(evt));
      var props = UISliderControl.getValue(mouseX);
      gj(this.object).css('width', props[0] + 'px');
      gj(this.inputField).val(props[1] * 5);
      this.parent.find('label[for=' + this.inputField.id + ']').html(
          props[1] * 5);
      this.parent.on('mousemove', this.execute);
      this.parent.on('mouseup', this.end);
    },

    execute : function(evt) {
      var cont = UISliderControl.container;
      var mouseX = eXo.core.Browser.findMouseRelativeX(cont, gj.event.fix(evt));
      var props = UISliderControl.getValue(mouseX);
      gj(UISliderControl.object).css('width', props[0] + 'px');
      gj(UISliderControl.inputField).val(String(props[1] * 5));
      UISliderControl.parent.find(
          'label[for=' + UISliderControl.inputField.id + ']')
          .html(props[1] * 5);
    },

    getValue : function(mouseX) {
      var width = 0;
      var value = 0;
      mouseX = parseInt(mouseX);
      if (mouseX <= 7) {
        width = 14;
        value = 0;
      } else if ((mouseX > 7) && (mouseX <= 200)) {
        width = mouseX + 7;
        value = width - 14;
      } else if ((mouseX > 200) && (mouseX < 221)) {
        width = mouseX + 7;
        value = width - 28;
      } else {
        width = 228;
        value = 200;
      }
      return [ width, value ];
    },

    end : function() {
      UISliderControl.parent.off('mousemove', UISliderControl.execute);
      UISliderControl.parent.off('mouseup', UISliderControl.end);
      UISliderControl.object = null;
      UISliderControl.container = null;
    },

    reset : function(input) {
      gj(input).val('0');
      var parent = gj(input).parents('.UISliderControl');
      parent.find('label[for=' + gj(input).attr('id') + ']').html('0');
      parent.find('div.SliderPointer').css('width', '14px');
    }
  };
  window.eXo = window.eXo || {};
  window.eXo.webui = window.eXo.webui || {};
  window.eXo.webui.UISliderControl = UISliderControl;
  return window.eXo.webui.UISliderControl;
})(utils, gj);