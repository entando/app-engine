import React from 'react';
import ReactDOM from 'react-dom';
import LoginButtonWidget from 'widgets/login-button/LoginButtonWidget';

const ATTRIBUTES = {
  userDisplayName: 'user-display-name',
  redirectUrl: 'redirect-url',
  adminUrl: 'admin-url',
};

class LoginButtonWidgetElement extends HTMLElement {

  static TAG = 'login-button-widget';

  static get observedAttributes() {
    return Object.values(ATTRIBUTES);
  }

  attributeChangedCallback(attribute, oldValue, newValue) {
    if (!LoginButtonWidgetElement.observedAttributes.includes(attribute)) {
      throw new Error(`Untracked changed attribute: ${attribute}`);
    }
    if (this.mountPoint && newValue !== oldValue) {
      this.render();
    }
  }

  connectedCallback() {
    this.mountPoint = document.createElement('div');
    this.appendChild(this.mountPoint);
    this.render();
  }

  render() {
    const userDisplayName = this.getAttribute(ATTRIBUTES.userDisplayName);
    const redirectUrl = this.getAttribute(ATTRIBUTES.redirectUrl);
    const adminUrl = this.getAttribute(ATTRIBUTES.adminUrl);

    ReactDOM.render(
      <LoginButtonWidget
        userDisplayName={userDisplayName}
        redirectUrl={redirectUrl}
        adminUrl={adminUrl}
      />,
      this.mountPoint
    );
  }
}

if (!customElements.get(LoginButtonWidgetElement.TAG)) {
  customElements.define(LoginButtonWidgetElement.TAG, LoginButtonWidgetElement);
}

export default LoginButtonWidgetElement;
