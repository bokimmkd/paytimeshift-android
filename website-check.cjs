const { chromium } = require('playwright');
const http = require('http'), fs = require('fs'), path = require('path'), assert = require('assert');
const root = path.join(__dirname, 'docs');
const types = {'.html':'text/html','.js':'text/javascript','.css':'text/css','.jpg':'image/jpeg','.png':'image/png','.svg':'image/svg+xml'};
const server = http.createServer((req,res)=>{
  const url = new URL(req.url,'http://localhost');
  const file = path.join(root,url.pathname==='/'?'index.html':decodeURIComponent(url.pathname));
  if(!file.startsWith(root+path.sep) || !fs.existsSync(file)) {res.writeHead(404);res.end();return;}
  res.setHeader('Content-Type',types[path.extname(file)]||'application/octet-stream');fs.createReadStream(file).pipe(res);
});
(async()=>{
  await new Promise(r=>server.listen(4173,'127.0.0.1',r));
  fs.mkdirSync('website-validation',{recursive:true});
  const browser=await chromium.launch({headless:true});
  const errors=[];
  const page=await browser.newPage();
  page.on('pageerror',e=>errors.push(e.message));
  for(const width of [1440,390]){
    await page.setViewportSize({width,height:900});
    await page.goto('http://127.0.0.1:4173/');
    for(const lang of ['en','mk','de','it','es','fr','sr','pt-BR','el']){
      await page.selectOption('#language-select',lang);
      assert.equal(await page.getAttribute('html','lang'),lang);
      const missing=await page.evaluate(()=>[...document.querySelectorAll('[data-t]')].filter(e=>!e.textContent.trim()||e.textContent==='undefined').map(e=>e.dataset.t));
      assert.deepEqual(missing,[]);
      for(let i=0;i<9;i++){
        await page.locator('#guide-tab-'+i).click();
        await page.locator('#guide-image').evaluate(img=>img.decode());
        assert.equal(await page.locator('#guide-steps li').count(),3);
        assert.equal(await page.locator('[role=tab][aria-selected=true]').count(),1);
        const dims=await page.evaluate(()=>({width:innerWidth,scroll:document.documentElement.scrollWidth}));
        assert(dims.scroll<=dims.width+1,lang+' overflow at '+width);
      }
      await page.locator('#guide-tab-0').click();
      if(width===1440 && lang==='en')await page.screenshot({path:'website-validation/PTS-site-desktop-en.png',fullPage:true});
      if(width===390 && lang==='mk')await page.screenshot({path:'website-validation/PTS-site-mobile-mk.png',fullPage:true});
      if(width===390 && lang==='el')await page.locator('#guide').screenshot({path:'website-validation/PTS-site-guide-el.png'});
    }
    await page.locator('#zoom-trigger').click();
    assert(await page.locator('#image-dialog').evaluate(el=>el.open));
    await page.keyboard.press('Escape');
    assert.equal(await page.locator('#image-dialog').evaluate(el=>el.open),false);
    await page.locator('#guide-tab-0').focus();
    await page.keyboard.press('ArrowRight');
    assert.equal(await page.locator('#guide-tab-1').getAttribute('aria-selected'),'true');
    await page.reload();
    assert.equal(await page.inputValue('#language-select'),'el');
  }
  const assets=await page.evaluate(()=>[...document.querySelectorAll('img')].map(i=>i.getAttribute('src')));
  for(const src of assets)assert(fs.existsSync(path.join(root,src)),src);
  const ratio=await page.locator('.cover img').evaluate(el=>({display:el.clientWidth/el.clientHeight,natural:el.naturalWidth/el.naturalHeight}));
  assert(Math.abs(ratio.display-ratio.natural)<0.03,'Cover cropped');
  assert.deepEqual(errors,[]);
  fs.writeFileSync('website-validation/result.json',JSON.stringify({languages:9,guideScreens:9,viewports:[1440,390],checks:'translations, screenshots, overflow, full banner, modal, keyboard, persistence',result:'passed'},null,2));
  console.log('Website passed: 9 languages × 9 guides × 2 widths; banner, modal, keyboard and persistence.');
  await browser.close();server.close();
})().catch(e=>{console.error(e);server.close();process.exit(1);});
