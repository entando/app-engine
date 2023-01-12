import React, { Component } from 'react';
import get from 'lodash/get';
import { HeaderNavigation, HeaderMenuItem } from 'carbon-components-react';
import { Login24, Logout16, ChevronDown16 } from '@carbon/icons-react';
import { settings } from 'carbon-components';
import HeaderMenu from 'common/ui-shell/HeaderMenu';

import 'widgets/login-button/LoginButtonWidget.scss';

const { prefix } = settings;

const KEYCLOAK_EVENT_ID = 'keycloak';

const getKeycloakInstance = () => {
  const entando = window.entando || {};
  const initialized = !!entando.keycloak;
  return initialized ? { ...entando.keycloak, initialized} : { initialized };
};

class LoginButtonWidget extends Component {
  constructor(props) {
    super(props)
    this.state = {
      username: null
    };
    this.keycloakEventHandler = this.keycloakEventHandler.bind(this);
  }

  keycloakEventHandler(event) {
    const keycloakEvent = event.detail.eventType;
    const keycloak = getKeycloakInstance();
    switch (keycloakEvent) {
      case 'onAuthSuccess':
      case 'onAuthRefreshSuccess':
        const username = get(keycloak, 'idTokenParsed.preferred_username');
        this.updateUsername(username);
        break;
      case 'onAuthRefreshError':
        this.resetUsername();
        keycloak.logout();
        break;
      case 'onAuthLogout':
        this.resetUsername();
        break;
      default:
        break;
    }
  }

  componentDidMount() {    
    window.addEventListener(KEYCLOAK_EVENT_ID, this.keycloakEventHandler);
  }

  componentWillUnmount() {
    window.removeEventListener(KEYCLOAK_EVENT_ID, this.keycloakEventHandler);
  }

  updateUsername(username) {
    this.setState({
      username,
    })
  }
  
  resetUsername() {
    this.setState({
      username: null,
    })
  }  

  render() {
    const {
      adminUrl,
      userDisplayName,
      redirectUrl,
    } = this.props;

    const keycloak = getKeycloakInstance();

    const displayName = userDisplayName || this.state.username;

    const handleLogin = () => getKeycloakInstance().login({ redirectUri: redirectUrl });

    const handleLogout = () => getKeycloakInstance().logout({ redirectUri: redirectUrl });

    return (
      <div className="LoginButtonWidget">
        {keycloak.authenticated ? (
          <div className="LoginButtonWidget__welcome">
            <HeaderNavigation aria-label="My Account">
              <HeaderMenu
                aria-label={displayName}
                menuLinkName={displayName}
                renderMenuContent={() => <ChevronDown16 className={`${prefix}--header__menu-arrow`} />}
                className="navigationMenu"
              >
                <HeaderMenuItem href={adminUrl}>
                  Dashboard
                </HeaderMenuItem>
                <HeaderMenuItem onClick={handleLogout}>
                  <Logout16 className="LoginButtonWidget__logoutsvg" /> Logout
                </HeaderMenuItem>
              </HeaderMenu>
            </HeaderNavigation>
          </div>
        ) : (
          <Login24 onClick={handleLogin} className="LoginButtonWidget__loginsvg" />
        )}
      </div>      
    );
  }
}

export default LoginButtonWidget;
