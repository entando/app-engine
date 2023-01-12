import React from 'react';
import ReactDOM from 'react-dom';
import NavigationBar from 'widgets/navigation-bar/NavigationBarWidget';

const ATTRIBUTES = {
  navItems: 'nav-items',
  currentPage: 'current-page',
};

class NavigationBarElement extends HTMLElement {
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
    this.render();
  }

  condenseNavItems(navItemString) {
    const parsed = JSON.parse(navItemString).map((navItem, navIdx) => ({ ...navItem, navIdx }));
    const parentLevels = [];
    const navItems = [];
    parsed.forEach((nav, i) => {
      if (Number(nav.level) !== parentLevels.length) {
        while (Number(nav.level) !== parentLevels.length) {
          if (nav.level > parentLevels.length) {
            parentLevels.push(parsed[i-1]);
          } else {
            parentLevels.pop();
          }
        }
      }
      const currentLevel = parentLevels.length;
      if (currentLevel > 0) {
        if (!parentLevels[currentLevel-1].children) parentLevels[currentLevel-1].children = [];
        parentLevels[currentLevel-1].children = [...parentLevels[currentLevel-1].children, nav];
      } else {
        navItems.push(nav);
      }
    });
    return navItems;
  }

  render() {
    const navItems = this.condenseNavItems(this.getAttribute(ATTRIBUTES.navItems));
    const currentPage = this.getAttribute(ATTRIBUTES.currentPage);

    ReactDOM.render(
      <NavigationBar
        navItems={navItems}
        currentPage={currentPage}
      />,
      this
    );
  }
}

if (!customElements.get('navigation-bar-widget')) {
  customElements.define('navigation-bar-widget', NavigationBarElement);
}

export default NavigationBarElement;
