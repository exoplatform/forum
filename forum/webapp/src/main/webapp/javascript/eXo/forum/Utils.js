function showHide() {
  var x = document.getElementById("myTopnav");
  x.classList.remove("pull-right")
  if (x.classList.contains("topnav") && !x.classList.contains("responsive")) {
    x.className += " responsive";
  } else {
    x.className = "topnav";
  }
}