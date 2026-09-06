/* Runtime local do arquivo .dc.html — apenas para visualizar o protótipo fora
   da canvas do Claude Design. Reimplementa o mínimo que o Certame.dc.html usa:
   a classe base DCLogic (state + setState) e o template com {{ }}, <sc-if> e
   <sc-for>. Não faz parte do produto e não vai para o frontend Angular. */
(() => {
  'use strict';

  const EVENTOS = { onclick: 'click', onchange: 'input', oninput: 'input',
                    onsubmit: 'submit', onkeydown: 'keydown', onfocus: 'focus', onblur: 'blur' };
  const EXPRESSAO = /\{\{([^}]*)\}\}/g;
  const SO_EXPRESSAO = /^\s*\{\{([^}]*)\}\}\s*$/;

  // Resolve "a.b", "!a" e "a" contra o escopo do laço e, depois, os valores do componente.
  function resolver(expr, vals, escopo) {
    let e = String(expr).trim(), negar = false;
    while (e.startsWith('!')) { negar = !negar; e = e.slice(1).trim(); }
    const partes = e.split('.');
    let v = escopo && partes[0] in escopo ? escopo[partes[0]] : vals[partes[0]];
    for (let i = 1; i < partes.length && v != null; i++) v = v[partes[i]];
    return negar ? !v : v;
  }

  const interpolar = (texto, vals, escopo) =>
    texto.replace(EXPRESSAO, (_, e) => { const v = resolver(e, vals, escopo); return v == null ? '' : String(v); });

  function montarFilhos(pai, destino, vals, escopo, chave, indice) {
    pai.childNodes.forEach((filho, i) => montar(filho, destino, vals, escopo, `${chave}/${i}`, indice));
  }

  function montar(no, destino, vals, escopo, chave, indice) {
    if (no.nodeType === 3) {                                   // texto
      const t = interpolar(no.nodeValue, vals, escopo);
      if (t.trim() || no.nodeValue.trim() === '') destino.appendChild(document.createTextNode(t));
      return;
    }
    if (no.nodeType !== 1) return;
    const tag = no.tagName.toLowerCase();

    if (tag === 'sc-if') {
      if (resolver(no.getAttribute('value').replace(/[{}]/g, ''), vals, escopo)) {
        montarFilhos(no, destino, vals, escopo, chave, indice);
      }
      return;
    }
    if (tag === 'sc-for') {
      const lista = resolver(no.getAttribute('list').replace(/[{}]/g, ''), vals, escopo) || [];
      const nome = no.getAttribute('as');
      lista.forEach((item, i) => montarFilhos(no, destino, vals, { ...escopo, [nome]: item }, `${chave}#${i}`, indice));
      return;
    }

    const el = document.createElement(tag);
    el.__k = chave;
    indice.set(chave, el);
    for (const { name, value } of Array.from(no.attributes)) {
      const evento = EVENTOS[name.toLowerCase()];
      const so = value.match(SO_EXPRESSAO);
      if (evento && so) {
        const fn = resolver(so[1], vals, escopo);
        if (typeof fn === 'function') el.addEventListener(evento, fn);
        continue;
      }
      if (name.startsWith('hint-')) continue;                  // dica só da canvas
      const v = interpolar(value, vals, escopo);
      el.setAttribute(name, v);
      if (name === 'value' && 'value' in el) el.value = v;     // input controlado
    }
    montarFilhos(no, el, vals, escopo, chave, indice);
    destino.appendChild(el);
  }

  class DCLogic {
    setState(patch) {
      const parcial = typeof patch === 'function' ? patch(this.state) : patch;
      this.state = { ...this.state, ...parcial };
      this.__render();
    }
    __render() {
      const antes = document.activeElement;
      const foco = antes && antes.__k;
      const ini = antes && antes.selectionStart, fim = antes && antes.selectionEnd;

      const vals = this.renderVals();
      const indice = new Map();
      const frag = document.createDocumentFragment();
      montarFilhos(this.__tpl, frag, vals, null, 'r', indice);
      this.__raiz.replaceChildren(frag);

      if (foco) {
        const el = indice.get(foco);
        if (el) { el.focus(); if (ini != null && el.setSelectionRange) { try { el.setSelectionRange(ini, fim); } catch (_) {} } }
      }
      this.__indice = indice;
    }
  }
  window.DCLogic = DCLogic;

  document.addEventListener('DOMContentLoaded', () => {
    const raiz = document.querySelector('x-dc');
    if (!raiz) return;

    const helmet = raiz.querySelector('helmet');            // vai para o <head>
    if (helmet) { document.head.append(...helmet.childNodes); helmet.remove(); }

    const script = document.querySelector('script[data-dc-script]');
    const fonte = script ? script.textContent : '';
    if (script) script.remove();

    const tpl = document.createElement('template');
    tpl.content.append(...raiz.childNodes);

    const Component = new Function('DCLogic', `${fonte}\nreturn Component;`)(DCLogic);
    const app = new Component();
    app.__raiz = raiz;
    app.__tpl = tpl.content;
    app.__render();
    if (app.componentDidMount) app.componentDidMount();
    window.__app = app;                                     // atalho para inspecionar no console
  });
})();
