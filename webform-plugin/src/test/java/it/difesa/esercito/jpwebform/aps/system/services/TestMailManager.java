package it.difesa.esercito.jpwebform.aps.system.services;

import static it.difesa.esercito.jpwebform.aps.system.services.TestMapper.getFormForTest;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.BaseTestCase;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.Form;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.form.IFormManager;
import it.difesa.esercito.plugins.jpwebform.aps.system.services.mail.IMailManager;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;


public class TestMailManager extends BaseTestCase {


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

    @BeforeEach
    public void init() {
        this._mailManager = (IMailManager) this.getApplicationContext().getBean(IMailManager.BEAN_ID);
        this._formManager = (IFormManager) this.getApplicationContext().getBean(IFormManager.BEAN_ID);
        assertNotNull(_mailManager);
        assertNotNull(_formManager);
    }

    private IFormManager _formManager;
    private IMailManager _mailManager;
}
