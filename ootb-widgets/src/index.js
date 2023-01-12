import React from 'react';
import ReactDOM from 'react-dom';
import 'index.scss';
import App from 'App';

import 'widgets/choose-language/custom-element/ChooseLanguageElement';
import 'widgets/login-button/LoginButtonWidgetElement';
import 'widgets/navigation-bar/custom-element/NavigationBarElement';
import 'widgets/search-bar/custom-element/SearchBarElement';
import 'fragments/header-fragment/custom-element/HeaderFragmentElement';

// import './kc-init';  // COMMENT this line in prod, it does the work of keycloak_auth fragment

if (process.env.REACT_APP_LOCAL === 'true') {
  ReactDOM.render(
    <App />,
    document.getElementById('root'),
  );
}
