var V='jion-v2';
var CORE=['./','./index.html','./manifest.webmanifest','./icon-192.png','./icon-512.png','./icon-maskable.png',
'./char/j1.png','./char/j2.png','./char/j3.png','./char/j4.png','./char/j5.png','./char/j6.png',
'./char/m1.png','./char/m2.png','./char/m3.png','./char/m4.png','./char/m5.png','./char/m6.png',
'./char/bg-maru.png','./char/s-good.png','./char/s-fight.png','./char/s-thanks.png','./char/s-hmm.png','./char/s-love.png','./char/s-haha.png'];
self.addEventListener('install',function(e){
  e.waitUntil(caches.open(V).then(function(c){return c.addAll(CORE)}).then(function(){return self.skipWaiting()}))});
self.addEventListener('activate',function(e){
  e.waitUntil(caches.keys().then(function(ks){return Promise.all(ks.map(function(k){return k===V?null:caches.delete(k)}))}).then(function(){return self.clients.claim()}))});
self.addEventListener('fetch',function(e){
  var r=e.request;if(r.method!=='GET')return;
  var u=new URL(r.url);
  if(u.origin!==self.location.origin){
    if(u.hostname==='www.gstatic.com'||u.hostname==='fonts.googleapis.com'||u.hostname==='fonts.gstatic.com'){
      e.respondWith(caches.open(V+'-ext').then(function(c){
        return c.match(r).then(function(hit){
          var net=fetch(r).then(function(res){if(res&&res.status===200)c.put(r,res.clone());return res}).catch(function(){return hit});
          return hit||net})}))}
    return}
  if(r.mode==='navigate'){e.respondWith(fetch(r).catch(function(){return caches.match('./index.html')}));return}
  e.respondWith(caches.match(r).then(function(hit){
    return hit||fetch(r).then(function(res){
      if(res&&res.status===200)caches.open(V).then(function(c){c.put(r,res.clone())});
      return res})}))});
