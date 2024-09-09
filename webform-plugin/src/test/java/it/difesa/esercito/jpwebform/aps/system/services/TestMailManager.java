package it.difesa.esercito.jpwebform.aps.system.services;

import static it.difesa.esercito.jpwebform.aps.system.services.TestMapper.getFormForTest;
import static it.difesa.esercito.plugins.jpwebform.aps.system.services.mail.IMailManager.BEAN_ID;

import it.difesa.esercito.jpwebform.aps.ApsPluginBaseTestCase;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.Form;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.mail.IMailManager;
import java.util.List;


public class TestMailManager extends ApsPluginBaseTestCase {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        this.init();
    }

    public void testMailDelivery() {
        Form form = getFormForTest();
        assertTrue(_mailManager.sendMail(form));
    }

    public void testRetry() throws Exception {
        TestFormManager.createFileForTesting(null);
        _mailManager.retry();
        List<Form> forms = _formManager.getForms();
        assertNotNull(forms);
        assertTrue(forms.isEmpty());
    }

    private void init() {
        this._mailManager = (IMailManager) this.getService(IMailManager.BEAN_ID);
        this._formManager = (IFormManager) this.getService(IFormManager.BEAN_ID);
        assertNotNull(_mailManager);
        assertNotNull(_formManager);
    }

    private IFormManager _formManager;
    private IMailManager _mailManager;
}
