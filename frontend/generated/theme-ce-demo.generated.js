import 'construct-style-sheets-polyfill';
import { DomModule } from "@polymer/polymer/lib/elements/dom-module";
import { stylesFromTemplate } from "@polymer/polymer/lib/utils/style-gather";
import { css, unsafeCSS, registerStyles } from '@vaadin/vaadin-themable-mixin/register-styles';

// target: Document | ShadowRoot
export const injectGlobalCss = (css, target, first) => {
  const sheet = new CSSStyleSheet();
  sheet.replaceSync(css);
  if (first) {
    target.adoptedStyleSheets = [sheet, ...target.adoptedStyleSheets];
  } else {
    target.adoptedStyleSheets = [...target.adoptedStyleSheets, sheet];
  }
};

const addCssBlock = function (block, before = false) {
  const tpl = document.createElement("template");
  tpl.innerHTML = block;
  document.head[before ? "insertBefore" : "appendChild"](
    tpl.content,
    document.head.firstChild
  );
};

const addStyleInclude = (module, target) => {
  addCssBlock(`<custom-style><style include="${module}"></style></custom-style>`, true);
};

const getStyleModule = (id) => {
  const template = DomModule.import(id, "template");
  const cssText =
    template &&
    stylesFromTemplate(template, "")
      .map((style) => style.textContent)
      .join(" ");
  return cssText;
};
import stylesCss from 'themes/ce-demo/styles.css';
import '@vaadin/vaadin-lumo-styles/color.js';
import '@vaadin/vaadin-lumo-styles/typography.js';
import vaadinDialogOverlayCss from 'themes/ce-demo/components/vaadin-dialog-overlay.css';

window.Vaadin = window.Vaadin || {};
window.Vaadin['_vaadintheme_ce-demo_globalCss'] = window.Vaadin['_vaadintheme_ce-demo_globalCss'] || [];
export const applyTheme = (target) => {
  
  const injectGlobal = (window.Vaadin['_vaadintheme_ce-demo_globalCss'].length === 0) || (!window.Vaadin['_vaadintheme_ce-demo_globalCss'].includes(target) && target !== document);
  if (injectGlobal) {
    injectGlobalCss(stylesCss.toString(), target);
    
    window.Vaadin['_vaadintheme_ce-demo_globalCss'].push(target);
  }
  if (!document['_vaadintheme_ce-demo_componentCss']) {
    registerStyles(
      'vaadin-dialog-overlay',
      css`
        ${unsafeCSS(vaadinDialogOverlayCss.toString())}
      `
    );
    
    document['_vaadintheme_ce-demo_componentCss'] = true;
  }
  // Lumo styles are injected into shadow roots.
// For the document, we need to be compatible with flow-generated-imports and add missing <style> tags.
const shadowRoot = (target instanceof ShadowRoot);
if (shadowRoot) {
injectGlobalCss(getStyleModule("lumo-color"), target, true);
injectGlobalCss(getStyleModule("lumo-typography"), target, true);
} else if (!document['_vaadinthemelumoimports_']) {
addStyleInclude("lumo-color", target);
addStyleInclude("lumo-typography", target);
document['_vaadinthemelumoimports_'] = true;
}

}
