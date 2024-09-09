package it.difesa.esercito.jpwebform.aps.system.services;


import it.difesa.esercito.jpwebform.aps.ApsPluginBaseTestCase;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.sige.ISigeManager;

public class TestSigeManager extends ApsPluginBaseTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.init();
    }

    public void testSigeAuthentication() {
        String at = _sigeManager.getClientAuthAccessToken();
        assertNotNull(at);
    }

    public void testGetUserInfoByCf() {
        String body = _sigeManager.getUserInfoById("admin");
        assertNotNull(body);
    }

    private void init() {
        this._sigeManager = (ISigeManager) this.getService(ISigeManager.BEAN_ID);
        assertNotNull(_sigeManager);
    }

    private ISigeManager _sigeManager;
}
