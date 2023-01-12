import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { Header } from 'carbon-components-react';

import './HeaderFragment.scss';

class HeaderFragment extends Component {
  handleLogoAreaRef = (ref) => {
    const { childNodes } = this.props;
    const [ logo ] = childNodes.filter(child => (
      child.getAttribute('role') === 'logo'
    ));
    if (logo) {
      const clonedLogo = logo.cloneNode(true);
      clonedLogo.classList.add('HeaderFragment__logo');
      ref.appendChild(clonedLogo);
    }
  }

  handleMenuAreaRef = (ref) => {
    const { childNodes } = this.props;
    const [ navbar ] = childNodes.filter(child => (
      child.tagName.toLowerCase() === 'navigation-bar-widget'
    ));
    if (navbar) ref.appendChild(navbar.cloneNode(true));
  }

  handleActionsRef = (ref) => {
    const { childNodes } = this.props;
    const actionItems = childNodes.filter((child) => {
      const tag = child.tagName.toLowerCase();
      return (tag !== 'navigation-bar-widget'
      && tag !== 'script'
      && tag !== 'link'
      && child.getAttribute('role') !== 'logo')
      || tag === 'login-button-widget'
      || tag === 'choose-language-widget'
    });
    actionItems.forEach((action) => {
      const clonedAction = action.cloneNode(true);
      if (clonedAction.classList.contains('navbar-search')) {
        clonedAction.classList.add('Homepage__search');
      }
      ref.appendChild(clonedAction);
    });
  }

  render() {
    const { applicationBaseURL } = this.props;
    return (
      <Header aria-label="Entando" className="HeaderFragment">
        <a ref={this.handleLogoAreaRef} />
        <div ref={this.handleMenuAreaRef} className="HeaderFragment__menu-area" />
        <div ref={this.handleActionsRef} className="bx--header__global HeaderFragment__actions-area" />
      </Header>
    );
  }
}

HeaderFragment.propTypes = {
  childNodes: PropTypes.oneOfType([
    PropTypes.arrayOf(PropTypes.instanceOf(Element)),
    PropTypes.instanceOf(Element),
  ]),
  applicationBaseURL: PropTypes.string,
};

HeaderFragment.defaultProps = {
  applicationBaseURL: process.env.REACT_APP_BASEURL,
};

export default HeaderFragment;
