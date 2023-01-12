import React from 'react';
import PropTypes from 'prop-types';
import { OverflowMenu, OverflowMenuItem } from 'carbon-components-react';
import { ChevronDown24, FlagFilled24 } from '@carbon/icons-react';

import './ChooseLanguageWidget.scss';

const ChooseLanguageWidget = ({ languages, currentLang, menuLeanRight }) => (
  <OverflowMenu
    renderIcon={() => <><FlagFilled24 /><ChevronDown24 className="chooseLanguage__menu-arrow" /></> }
    flipped={menuLeanRight}
    className="chooseLanguage"
  >
    {languages.map(lang => (
      <OverflowMenuItem
        key={lang.code}
        itemText={lang.descr}
        href={lang.url}
        className={[
          'langItem',
          ...(currentLang === lang.code ? ['active'] : []),
        ].join(' ')}
      />
    ))}
  </OverflowMenu>
);

ChooseLanguageWidget.propTypes = {
  languages: PropTypes.arrayOf(
    PropTypes.shape({
      code: PropTypes.string,
      descr: PropTypes.string,
      url: PropTypes.string,
    }),
  ),
  currentLang: PropTypes.string,
  menuLeanRight: PropTypes.bool,
};

ChooseLanguageWidget.defaultProps = {
  languages: [],
  currentLang: '',
  menuLeanRight: false,
};

export default ChooseLanguageWidget;
