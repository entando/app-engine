import React, { Component } from 'react';
import PropTypes from 'prop-types';
import { HeaderNavigation, HeaderMenuItem } from 'carbon-components-react';
import { ChevronRight16, ChevronDown16 } from '@carbon/icons-react';
import { settings } from 'carbon-components';

import HeaderMenu from 'common/ui-shell/HeaderMenu';

import './NavigationBarWidget.scss';

const { prefix } = settings;

const subMenuRenderMenuContent = isTop => () => {
  const Icon = isTop ? ChevronDown16 : ChevronRight16;
  return <Icon className={`${prefix}--header__menu-arrow`} />;
};


class NavigationBarWidget extends Component {

  currentPageMarked = false;

  renderNav(items, top, isCurrent) {
    if (items == null || items.length === 0) {
      return (
        <HeaderMenu className="navigationMenu" />
      )
    }

    const { currentPage } = this.props;
    const isTopMenu = Number(items[0].level) === 0;
    const isFirstLevelMenu = Number(items[0].level) === 1;
    const HeadComp = isTopMenu ? HeaderNavigation : HeaderMenu;

    const selectedMenu = items.find(item => (
      item.code === currentPage
      || (item.children && item.children.find(childitem => childitem.code === currentPage))
    ));

    let uniqueId = '{}';
    if (top) {
      let { children: ignoreme, ...itemNoChild } = top;
      uniqueId = JSON.stringify(itemNoChild);
    }

    const menuProps = {
      'aria-label': isTopMenu ? 'Menu' : top.title,
      ...(isTopMenu
        ? { className: 'navigationMenu' }
        : {
          menuLinkName: top.title,
          renderMenuContent: subMenuRenderMenuContent(isFirstLevelMenu),
          uniqueId,
          key: uniqueId,
        }
      ),
      ...(isCurrent ? { isCurrentPage: isCurrent } : {}),
      ...(top && top.url && !top.voidPage ? { href: top.url } : { href: '#' }),
    };

    return (
      <HeadComp {...menuProps}>
        {items.map((item) => {
          const isCurrentPage = !this.currentPageMarked && selectedMenu && selectedMenu.code === item.code;
          if (isCurrentPage) this.currentPageMarked = true;

          return item.children ? (
            this.renderNav(item.children, item, isCurrentPage)
          ) : (
            <HeaderMenuItem
              key={JSON.stringify(item)}
              href={item.url}
              isCurrentPage={isCurrentPage}
            >
              {item.title}
            </HeaderMenuItem>
          );
        })}
      </HeadComp>
    );
  }

  render() {
    const { navItems } = this.props;
    this.currentPageMarked = false;
    return this.renderNav(navItems);
  }
}

NavigationBarWidget.propTypes = {
  navItems: PropTypes.arrayOf(
    PropTypes.shape({
      code: PropTypes.string,
      title: PropTypes.string,
      url: PropTypes.string,
    }),
  ),
  currentPage: PropTypes.string.isRequired,
};

export default NavigationBarWidget;