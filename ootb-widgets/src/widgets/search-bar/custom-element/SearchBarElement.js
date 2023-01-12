import React from 'react';
import ReactDOM from 'react-dom';
import SearchBar from 'widgets/search-bar/SearchBarWidget';

const ATTRIBUTES = {
  actionUrl: 'action-url',
  placeholder: 'placeholder',
};

class SearchBarElement extends HTMLElement {
  static get observedAttributes() {
    return Object.values(ATTRIBUTES);
  }

  attributeChangedCallback(attribute, oldValue, newValue) {
    if (!Object.values(ATTRIBUTES).includes(attribute)) {
      throw new Error(`Untracked changed attribute: ${attribute}`);
    }
    if (newValue !== oldValue) {
      this.render();
    }
  }

  connectedCallback() {
    this.render();
  }

  render() {
    const actionUrl = this.getAttribute(ATTRIBUTES.actionUrl);
    const placeholder = this.getAttribute(ATTRIBUTES.placeholder);

    ReactDOM.render(
      <SearchBar
        actionUrl={actionUrl}
        placeholder={placeholder}
      />,
      this
    );
  }
}

if (!customElements.get('search-bar-widget')) {
  customElements.define('search-bar-widget', SearchBarElement);
}

export default SearchBarElement;
