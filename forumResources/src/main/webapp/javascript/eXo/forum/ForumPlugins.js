// this plugin to check existing element.
;(function($, window, document, undefined) {
  // preventing against multiple instantiations
  $.fn.exists = function() {
    return ($(this).length > 0);
  }
})(gj, window, document);

// this plugin to find element by id.
;(function($, window, document, undefined) {
  // preventing against multiple instantiations
  $.fn.findId = function(elm) {
    
    if (!$(elm).exists() && String(elm).indexOf('#') != 0 && $('#' + elm).exists()) {
      elm = '#' + elm;
    }
    
    if ($(this).exists() && !$.isWindow(this)) {
      return $(this).find(elm);
    } else {
      return $(elm);
    }
  }

  window.findId = window.findId || $.fn.findId;
  $.findId = window.findId;
})(gj, window, document);
