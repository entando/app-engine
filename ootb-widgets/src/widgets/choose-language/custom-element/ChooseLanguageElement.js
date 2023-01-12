import React from 'react';
import ReactDOM from 'react-dom';
import ChooseLanguage from 'widgets/choose-language/ChooseLanguageWidget';

const ATTRIBUTES = {
  languages: 'languages',
  currentLang: 'current-lang',
};

class ChooseLanguageElement extends HTMLElement {
  static get observedAttributes() {
    return Object.values(ATTRIBUTES);
  }

  attributeChangedCallback(attribute, oldValue, newValue) {
    if (!Object.values(ATTRIBUTES).includes(attribute)) {
      throw new Error(`Untracked changed attribute: ${attribute}`);
    }
    if (this.mountPoint && newValue !== oldValue) {
      this.render();
    }
  }

  connectedCallback() {
    this.mountPoint = document.createElement('div');
    this.appendChild(this.mountPoint);
    setTimeout(() => this.render(), 500);
  }

  render() {
    const languages = JSON.parse(this.getAttribute(ATTRIBUTES.languages));
    const currentLang = this.getAttribute(ATTRIBUTES.currentLang);
    const rightBound = this.offsetLeft > (document.body.clientWidth / 2);

    ReactDOM.render(
      <ChooseLanguage
        languages={languages}
        currentLang={currentLang}
        menuLeanRight={rightBound}
      />,
      this.mountPoint
    );
  }
}

if (!customElements.get('choose-language-widget')) {
  customElements.define('choose-language-widget', ChooseLanguageElement);
}

export default ChooseLanguageElement;
