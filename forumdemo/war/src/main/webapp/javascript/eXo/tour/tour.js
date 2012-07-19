if (!eXo.forum)
  eXo.forum = {};

function Tour() {
  childBts = null;
  t = 0;
};

Tour.prototype.init = function() {
  this.parent_ = gj('#UIKSGuidedTour');
  this.childBts = this.parent_.find('li.Button');
};

Tour.prototype.showContent = function(obj) {
  var t = eXo.forum.Tour.t;
  var childBts = eXo.forum.Tour.childBts;
  var parent_ = eXo.forum.Tour.parent_;
  var childContents = parent_.find('div.ContentTour');
  for ( var i = 0; i < childBts.length; i++) {
    var child = childBts[i];
    if (child === obj) {
      child.className = "Button HightLineButton";
      childContents.eq(i).show();
      t = i;
    } else {
      child.className = "Button NormalButton";
      childContents.eq(i).hide();
    }
  }
};

Tour.prototype.onMouseOverButton = function(obj) {
  var t = eXo.forum.Tour.t;
  var childBts = eXo.forum.Tour.childBts;
  for ( var i = 0; i < childBts.length; i++) {
    var child = childBts[i];
    if (child.className != "Button HightLineButton") {
      if (child === obj) {
        child.className = "Button OverButton";
      } else if (i != t) {
        child.className = "Button NormalButton";
      }
    }
  }
};

Tour.prototype.onMouseOutButton = function(obj) {
  var t = eXo.forum.Tour.t;
  var childBts = eXo.forum.Tour.childBts;
  for ( var i = 0; i < childBts.length; i++) {
    var child = childBts.eq(i);
    if (!child.hasClass('Button HightLineButton') && i != t) {
      child.attr('class', 'Button NormalButton');
    }
  }
};

eXo.forum.Tour = new Tour();