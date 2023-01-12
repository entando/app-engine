import React, { Component } from 'react';
import { BrowserRouter as Router, Route } from 'react-router-dom';
import { AppSwitcher20 } from '@carbon/icons-react';
import {
  Header,
  HeaderName,
  HeaderPanel,
  Switcher,
  SwitcherItem,
  SwitcherDivider,
  Content,
  HeaderGlobalBar,
  HeaderGlobalAction,
} from 'carbon-components-react';

const languages = [
  { code: 'EN', descr: 'English' },
  { code: 'IT', descr: 'Italian' },
];

const sampleMenu = [
  {
    code: 'homepage',
    title: 'Home',
    level: '0',
    url: 'http://localhost:8090/entando-de-app/en/homepage.page',
    voidPage: true
  },
  {
    code: 'my_page',
    title: 'My Page',
    level: '1',
    url: 'http://localhost:8090/entando-de-app/en/my_page.page',
    voidPage: false
  },
  {
    code: 'homepage_test',
    title: 'Home - test',
    level: '1',
    url: 'http://localhost:8090/entando-de-app/en/homepage_test.page',
    voidPage: false
  },
  {
    code: 'sitemap',
    title: 'Sitemap',
    level: '2',
    url: 'http://localhost:8090/entando-de-app/en/sitemap.page',
    voidPage: false
  },
  {
    code: 'homepage',
    title: 'Home',
    level: '0',
    url: 'http://localhost:8090/entando-de-app/en/homepage.page',
    voidPage: true
  },
  {
    code: 'my_page',
    title: 'My Page',
    level: '0',
    url: 'http://localhost:8090/entando-de-app/en/my_page.page',
    voidPage: false
  },
  {
    code: 'homepage_test',
    title: 'Home - test',
    level: '1',
    url: 'http://localhost:8090/entando-de-app/en/homepage_test.page',
    voidPage: true
  },
  {
    code: 'sitemap',
    title: 'Sitemap',
    level: '2',
    url: 'http://localhost:8090/entando-de-app/en/sitemap.page',
    voidPage: false
  },
  {
    code: 'my_pagesdfdf',
    title: 'My Page Wa',
    level: '2',
    url: 'http://localhost:8090/entando-de-app/en/my_page.page',
    voidPage: false
  },
  {
    code: 'gbadoasd',
    title: 'Forever',
    level: '3',
    url: 'http://localhost:8090/entando-de-app/en/my_page.page',
    voidPage: false
  },
  {
    code: 'contact',
    title: 'Contact',
    level: '2',
    url: 'http://localhost:8090/entando-de-app/en/contact.page',
    voidPage: false
  },
  {
    code: 'address',
    title: 'Address',
    level: '3',
    url: 'http://localhost:8090/entando-de-app/en/address.page',
    voidPage: false
  },
  {
    code: 'about',
    title: 'About',
    level: '1',
    url: 'http://localhost:8090/entando-de-app/en/homepage.page',
    voidPage: true
  },
  {
    code: 'homepage',
    title: 'Home',
    level: '0',
    url: 'http://localhost:8090/entando-de-app/en/homepage.page',
    voidPage: true
  }
];

class App extends Component {
  constructor(props) {
    super(props);
    this.state = {
      expanded: false,
    };
    this.handleExpandSidePanel = this.handleExpandSidePanel.bind(this);
  }

  handleExpandSidePanel() {
    this.setState(prevState => ({
      expanded: !prevState.expanded,
    }));
  }

  render() {
    return (
      <Router>
        <Header aria-label="Entando OOTB Widgets Dev">
          <HeaderName href="#" prefix="Entando">
            OOTB Widgets Dev
          </HeaderName>
          <HeaderGlobalBar>
          <HeaderGlobalAction
            aria-label="Components List"
            onClick={this.handleExpandSidePanel}>
            <AppSwitcher20 />
          </HeaderGlobalAction>
          </HeaderGlobalBar>
          <HeaderPanel aria-label="Header Panel" expanded={this.state.expanded}>
            <Switcher aria-label="Switcher Container">
              <SwitcherItem aria-label="Language Chooser" href="/">
                Language Chooser
              </SwitcherItem>
              <SwitcherItem href="/nav-bar" aria-label="Navigation Bar">
                Navigation Bar
              </SwitcherItem>
              <SwitcherItem href="/login-button" aria-label="Login Button">
                Login Button
              </SwitcherItem>
              <SwitcherItem href="/search-bar" aria-label="Search Bar">
                Search Bar
              </SwitcherItem>
              <SwitcherItem href="/header-fragment" aria-label="Header Fragment">
                Header Fragment
              </SwitcherItem>
              <SwitcherDivider />
            </Switcher>
          </HeaderPanel>
        </Header>
        <Content>
          <Route
            path="/"
            exact
            render={() => (
              <choose-language-widget
                current-lang="EN"
                languages={JSON.stringify(languages)}
              />
            )} />
          <Route
            path="/login-button"
            exact
            render={() => (
              <div style={{paddingLeft: '100px'}}>
                <login-button-widget
                  admin-url={process.env.REACT_APP_BASEURL}
                  redirect-url={`${process.env.REACT_APP_BASEURL}login-button`}
                />
              </div>
            )} />
          <Route
            path="/nav-bar"
            exact
            render={() => (
              <Header aria-label="Entando Navigation Bar">
                <navigation-bar-widget
                  current-page="homepage"
                  nav-items={JSON.stringify(sampleMenu)}
                />
              </Header>
            )} />
          <Route
            path="/search-bar"
            exact
            render={() => (
              <search-bar-widget
                action-url={process.env.REACT_APP_BASEURL}
                placeholder="Search"
              />
            )} />
          <Route
            path="/header-fragment"
            exact
            render={() => (
              <header-fragment app-url={process.env.REACT_APP_BASEURL}>
                <template>
                  <navigation-bar-widget
                    current-page="homepage"
                    nav-items={JSON.stringify(sampleMenu)}
                  />
                  <choose-language-widget
                    current-lang="EN"
                    languages={JSON.stringify(languages)}
                  />
                  <login-button-widget
                    app-url={process.env.REACT_APP_BASEURL}
                    page={`${process.env.REACT_APP_BASEURL}en/homepage_test.page`}
                  />
                </template>
              </header-fragment>
            )} />
        </Content>
      </Router>
    );
  }
}

export default App;
