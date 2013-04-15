twitter = function(url){
  window.twttr=window.twttr||{};
  var D=550,A=450,C=screen.height,B=screen.width,H=Math.round((B/2)-(D/2)),G=0,F=document;
  if(C<A) {
    G=Math.round((C/2)-(A/2));
  }
  window.twttr.shareWin=window.open(url,'','left='+H+',top='+G+',width='+D+',height='+A+',personalbar=0,toolbar=0,scrollbars=1,resizable=1');
};
