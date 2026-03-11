package org.entando.entando.aps.system.services;

/**
 * Feature flag for headless widget configuration mode.
 * When enabled, widget configuration pages can be rendered without the
 * admin-console header/menu (headless layout) for embedding in app-builder via iframe.
 * <p>
 * Activated via environment variable: {@code ENTANDO_FEATURE_FLAGS=HEADLESS_WIDGET_CONFIG}
 * <p>
 * <b>Widget-side activation:</b> for app-builder to open a widget's configuration
 * in headless mode, the widget must declare the following {@code configUi} in the
 * {@code widgetcatalog} table (or in the corresponding Liquibase insert):
 * <pre>{@code
 * {"customElement":"LEGACY_CONFIG","resources":[]}
 * }</pre>
 * When app-builder detects {@code customElement: "LEGACY_CONFIG"}, it renders the
 * widget configuration inside an iframe pointing to the admin-console Struts action
 * with the {@code entandoHeadless=true} parameter, which triggers the
 * {@link org.entando.entando.apsadmin.system.HeadlessLayoutInterceptor}.
 */
public interface IFHeadlessWidgetConfig extends IFeatureFlag {

    boolean HEADLESS_WIDGET_CONFIG = IFeatureFlag.readEnablementStatus("HEADLESS_WIDGET_CONFIG");

    @Override
    default boolean isEnabled() {
        return HEADLESS_WIDGET_CONFIG;
    }
}