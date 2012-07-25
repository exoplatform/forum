;(function($, window, document) {
  
  function UISliderControl() {
    this.container = null;
    this.object = null;
    this.parent = null;
    this.inputField = null;
  }
  
  UISliderControl.prototype.start = function(obj, evt) {
    this.container = obj;
    this.object = $(obj).find('div.SliderPointer').eq(0)[0];
    this.parent = $(obj).parent();
    this.inputField = this.parent.find('input').eq(0)[0];
    var mouseX = eXo.core.Browser.findMouseRelativeX(obj, $.event.fix(evt));
    var props = eXo.webui.UISliderControl.getValue(mouseX);
    $(this.object).css('width', props[0] + 'px');
    $(this.inputField).val(props[1] * 5);
    this.parent.find('label[for=' + this.inputField.id + ']').html(props[1] * 5);
    this.parent.on('mousemove', this.execute);
    this.parent.on('mouseup', this.end);
  };
  
  UISliderControl.prototype.execute = function(evt) {
    var UISliderControl = eXo.webui.UISliderControl;
    var cont = UISliderControl.container;
    var mouseX = eXo.core.Browser.findMouseRelativeX(cont, $.event.fix(evt));
    var props = UISliderControl.getValue(mouseX);
    $(UISliderControl.object).css('width', props[0] + 'px');
    $(UISliderControl.inputField).val(String(props[1] * 5));
    UISliderControl.parent.find('label[for=' + UISliderControl.inputField.id + ']').html(props[1] * 5);
  };
  
  UISliderControl.prototype.getValue = function(mouseX) {
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
  };
  
  UISliderControl.prototype.end = function() {
    eXo.webui.UISliderControl.parent.off('mousemove', eXo.webui.UISliderControl.execute);
    eXo.webui.UISliderControl.parent.off('mouseup', eXo.webui.UISliderControl.end);
    eXo.webui.UISliderControl.object = null;
    eXo.webui.UISliderControl.container = null;
  };
  
  UISliderControl.prototype.reset = function(input) {
    $(input).val('0');
    var parent = $(input).parents('.UISliderControl');
    parent.find('label[for=' + $(input).attr('id') + ']').html('0');
    parent.find('div.SliderPointer').css('width', '14px');
  };
  
  window.eXo.webui = window.eXo.webui || {};
  window.eXo.webui.UISliderControl = new UISliderControl();
})(gj, window, document);