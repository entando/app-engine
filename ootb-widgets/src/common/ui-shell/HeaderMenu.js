import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { ChevronDown16 } from '@carbon/icons-react';
import { settings } from 'carbon-components';
import cx from 'classnames';

import { matches } from 'common/keyboard/match';
import { Enter, Space, Escape } from 'common/keyboard/keys';
import { AriaLabelPropType } from 'common/prop-types/AriaPropTypes';

const { prefix } = settings;

class HeaderMenu extends Component {
  _subMenus = React.createRef();

  constructor(props) {
    super(props);
    this.state = {
      expanded: false,
      selectedIndex: null,
    };
    this.items = [];
  }

  handleOnIconClick = (e) => {
    e.stopPropagation();
    e.preventDefault();

    this.setState((prevState) => ({
      expanded: !prevState.expanded,
    }));
  };

  handleOnClick = (e) => {
    const { href } = this.props;
    if (href === '#') {
      const { current: subMenusNode } = this._subMenus;
      if (!subMenusNode || !subMenusNode.contains(e.target)) {
        e.preventDefault();
      }

      this.setState((prevState) => ({
        expanded: !prevState.expanded,
      }));
    }
  };

  handleOnKeyDown = (event) => {
    if (matches(event, [Enter, Space])) {
      event.stopPropagation();
      event.preventDefault();

      this.setState((prevState) => ({
        expanded: !prevState.expanded,
      }));

      return;
    }
  };

  handleOnBlur = (event) => {
    
    if (event.relatedTarget !== null) {
      const closestRelatedSubmenu = event.relatedTarget.closest(`li.${prefix}--header__submenu`);
      const closestSubmenu = event.currentTarget;
      if (closestRelatedSubmenu) {
        if (
          closestSubmenu.dataset.uniqueId === closestRelatedSubmenu.dataset.uniqueId ||
          closestSubmenu.querySelector(`li[data-unique-id='${closestRelatedSubmenu.dataset.uniqueId}']`)
        ) {
          event.stopPropagation();
          return;
        }
      }
    }

    const itemTriggeredBlur = this.items.find(
      (element) => element === event.relatedTarget
    );
    if (
      event.relatedTarget &&
      ((event.relatedTarget.getAttribute('href') &&
        event.relatedTarget.getAttribute('href') !== '#') ||
        itemTriggeredBlur)
    ) {
      return;
    }
    this.setState({ expanded: false, selectedIndex: null }); 
  };

  handleMenuButtonRef = (node) => {
    if (this.props.focusRef) {
      this.props.focusRef(node);
    }
    this.menuButtonRef = node;
  };

  handleItemRef = (index) => (node) => {
    this.items[index] = node;
  };

  handleMenuClose = (event) => {
    if (matches(event, [Escape]) && this.state.expanded) {
      event.stopPropagation();
      event.preventDefault();

      this.setState(() => ({
        expanded: false,
        selectedIndex: null,
      }));

      this.menuButtonRef.focus();
      return;
    }
  };

  render() {
    const {
      'aria-label': ariaLabel,
      'aria-labelledby': ariaLabelledBy,
      className: customClassName,
      children,
      renderMenuContent: MenuContent,
      menuLinkName,
      href,
      uniqueId,
    } = this.props;
    const accessibilityLabel = {
      'aria-label': ariaLabel,
      'aria-labelledby': ariaLabelledBy,
    };
    const className = cx(`${prefix}--header__submenu`, customClassName);

    return (
      <li // eslint-disable-line jsx-a11y/mouse-events-have-key-events,jsx-a11y/no-noninteractive-element-interactions
        className={className}
        onKeyDown={this.handleMenuClose}
        onClick={this.handleOnClick}
        tabIndex={0}
        data-unique-id={uniqueId}
        onBlur={this.handleOnBlur}>
        <a // eslint-disable-line jsx-a11y/role-supports-aria-props,jsx-a11y/anchor-is-valid
          className={`${prefix}--header__menu-item ${prefix}--header__menu-title`}
          href={href}
          aria-expanded={this.state.expanded}
          {...accessibilityLabel}>
            {menuLinkName}
            <button
              aria-haspopup="menu" // eslint-disable-line jsx-a11y/aria-proptypes
              onClick={this.handleOnIconClick}
              onKeyDown={this.handleOnKeyDown}
              ref={this.handleMenuButtonRef}
            >
              <MenuContent />
            </button>
        </a>
        <ul
          {...accessibilityLabel}
          ref={this._subMenus}
          className={`${prefix}--header__menu`}>
          {React.Children.map(children, this._renderMenuItem)}
        </ul>
      </li>
    );
  }

  _renderMenuItem = (item, index) => {
    if (React.isValidElement(item)) {
      return React.cloneElement(item, {
        ref: this.handleItemRef(index),
      });
    }
  };
}

HeaderMenu.propTypes = {
  ...AriaLabelPropType,
  focusRef: PropTypes.func,
  menuLinkName: PropTypes.string.isRequired,
  renderMenuContent: PropTypes.func,
  tabIndex: PropTypes.number,
  uniqueId: PropTypes.string,
  href: PropTypes.string,
};

const defaultRenderMenuContent = () => (
  <ChevronDown16 className={`${prefix}--header__menu-arrow`} />
);

HeaderMenu.defaultProps = {
  renderMenuContent: defaultRenderMenuContent,
  href: '#',
  uniqueId: '',
};

const HeaderMenuForwardRef = React.forwardRef((props, ref) => {
  return <HeaderMenu {...props} focusRef={ref} />;
});

HeaderMenuForwardRef.displayName = 'HeaderMenu';
export default HeaderMenuForwardRef;