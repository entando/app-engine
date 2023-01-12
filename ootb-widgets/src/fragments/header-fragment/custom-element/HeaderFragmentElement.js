import React from 'react';
import ReactDOM from 'react-dom';
import HeaderFragment from 'fragments/header-fragment/HeaderFragment';

const ATTRIBUTES = {
  applicationBaseURL: 'app-url',
};

class HeaderFragmentElement extends HTMLElement {
  observer = null;
  childs = [];

  static get observedAttributes() {
    return Object.values(ATTRIBUTES);
  }
  

  attributeChangedCallback(attribute, oldValue, newValue) {
    if (!Object.values(ATTRIBUTES).includes(attribute)) {
      throw new Error(`Untracked changed attribute: ${attribute}`);
    }
    if (newValue !== oldValue && this.childs.length) {
      this.extractTemplateTag();
    }
  }

  extractTemplateTag(ttag) {
    const templ = ttag || this.getElementsByTagName('template')[0];
    if (templ) {
      const contentChildren = templ.content && templ.content.children;
      if (contentChildren.length) {
        this.childs = [...contentChildren];
      } else {
        this.childs = [...templ.childNodes];
      }
      this.render();
    }
  }

  activateObserve() {
    const templateTag = this.querySelector('template');
    if (templateTag) {
      this.extractTemplateTag(templateTag);
    } else {
      this.observer = new MutationObserver((mutations) => {
        mutations.forEach((mutation) => {
          if (mutation.addedNodes.length) {
          }
          if (mutation.addedNodes.length
            && mutation.addedNodes[0].tagName
            && mutation.addedNodes[0].tagName.toLowerCase() === 'template'
          ) {
            setTimeout(() => (
              this.extractTemplateTag(mutation.addedNodes[0])
            ), 500);
          }
        });
      });
      this.observer.observe(this, { childList: true });
    }
  }

  connectedCallback() {
    this.activateObserve();
  }

  render() {
    const applicationBaseURL = this.getAttribute(ATTRIBUTES.applicationBaseURL);

    ReactDOM.render(
      <HeaderFragment
        applicationBaseURL={applicationBaseURL}
        childNodes={this.childs}
      />,
      this
    );
  }
}

if (!customElements.get('header-fragment')) {
  customElements.define('header-fragment', HeaderFragmentElement);
}

export default HeaderFragmentElement;
