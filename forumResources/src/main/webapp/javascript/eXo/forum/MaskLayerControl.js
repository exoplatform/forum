(function(utils, gj) {
  var MaskLayerControl = {

    init : function(root) {
      root = (typeof (root) == 'string') ? findId(root) : root;
      var jnodeList = gj(root).find('span.ViewDownloadIcon');
      for ( var i = 0; i < nodeList.length; i++) {
        jnodeList.eq(i).find('a:first').on('click', function() {
          MaskLayerControl.showPicture(this);
        });
      }
    },

    getContainerNode : function() {
      var containerNode = gj("#UIPictutreContainer");
      if (!containerNode.exists()) {
        containerNode = gj('<div></div>');
        containerNode
            .attr('id', 'UIPictutreContainer')
            .attr('style',
                'position:absolute; top:0px; width:100%; height:100%; text-align:center')
            .attr('title', 'Click to close').on('click',
                MaskLayerControl.hidePicture);
        gj("#UIPortalApplication").append(containerNode)
      }
      return containerNode;
    },

    showPicture : function(node) {
      if (typeof (node) == "string") {
        var imgSrcNode = new Image();
        imgSrcNode.src = node;
      } else {
        var attachmentContent = gj(node).parents('.AttachmentContent');
        var imgSrcNode = attachmentContent.find('img.AttachmentFile:first')[0];
      }
      var src = imgSrcNode.src;
      if (String(src).length > 0) {

        var imgSize = this.getImageSize(imgSrcNode);
        var windowHeight = document.documentElement.clientHeight;
        var windowWidth = document.documentElement.clientWidth;
        var marginTop = (windowHeight < parseInt(imgSize.height)) ? 0
            : parseInt((windowHeight - parseInt(imgSize.height)) / 2);
        var imgHeight = (windowHeight < parseInt(imgSize.height)) ? windowHeight
            + "px"
            : "auto";
        var imgWidth = (windowWidth < parseInt(imgSize.width)) ? windowWidth
            + "px" : "auto";
        var imageNode = "<img src='" + imgSrcNode.src + "' style='height:"
            + imgHeight + ";width:" + imgWidth + ";margin-top:" + marginTop
            + "px;' alt='Click to close'/>";
        var containerNode = MaskLayerControl.getContainerNode();
        containerNode.html(imageNode);
        var maskNode = eXo.core.UIMaskLayer.createMask('UIPortalApplication',
            containerNode[0], 30, 'CENTER');
        this.scrollHandler();

      }
    },

    reSizeImage : function() {

    },

    scrollHandler : function() {
      eXo.core.UIMaskLayer.object.style.top = document
          .getElementById("MaskLayer").offsetTop
          + "px";
      MaskLayerControl.timer = setTimeout(MaskLayerControl.scrollHandler, 1);
    },

    hidePicture : function() {
      eXo.core.Browser.onScrollCallback.remove('MaskLayerControl');
      var maskContent = eXo.core.UIMaskLayer.object;
      var maskNode = document.getElementById("MaskLayer")
          || document.getElementById("subMaskLayer");
      if (maskContent)
        maskContent.parentNode.removeChild(maskContent);
      if (maskNode)
        maskNode.parentNode.removeChild(maskNode);
      clearTimeout(MaskLayerControl.timer);
      delete MaskLayerControl.timer;
    },

    getImageSize : function(img) {
      var imgNode = new Image();
      imgNode.src = img.src;
      return {
        "height" : imgNode.height,
        "width" : imgNode.width
      };
    }
  };
  return MaskLayerControl;
})(utils, gj);
