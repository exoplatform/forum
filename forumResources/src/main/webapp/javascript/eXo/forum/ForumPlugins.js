(function($) {

  // check existing element.
  $.fn.exists = function() {
    return ($(this).length > 0);
  }

  // find element by id.
  var findId = function(elm) {

    if (!$(elm).exists() && String(elm).indexOf('#') != 0
        && $('#' + elm).exists()) {
      elm = '#' + elm;
    }

    if ($(this).exists() && !$.isWindow(this)) {
      return $(this).find(elm);
    } else {
      return $(elm);
    }
  }
  $.fn.findId = findId;
  window.findId = findId;
})(gj);