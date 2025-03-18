package it.difesa.esercito.jpwebform.aps.system.services;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.agiletec.aps.BaseTestCase;
import com.agiletec.aps.system.exception.ApsSystemException;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.Form;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.IFormManager;
import org.entando.entando.plugins.jpwebform.aps.system.services.mail.IMailManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;


public class TestMailManager extends BaseTestCase {

    @BeforeEach
    public void init() {
        this._mailManager = (IMailManager) this.getApplicationContext().getBean(IMailManager.BEAN_ID);
        this._formManager = (IFormManager) this.getApplicationContext().getBean(IFormManager.BEAN_ID);
        assertNotNull(_mailManager);
        assertNotNull(_formManager);
    }

    @Test
    public void testMailDelivery() throws ApsSystemException {
        Form form3 = new Form();


        form3.setName("Anco Marzio");
        form3.setData(TestFormManager.getFormDataForTest());
        form3.setSubmitted(LocalDateTime.now());
        form3.setCampaign("campagna");

        form3.setDelivered(true);
        form3.setSerial("");

        _formManager.addForm(form3);

        Form verify = _formManager.getForm(form3.getId());
        
        assertTrue(_mailManager.sendMail(verify));

        _formManager.deleteForm(form3.getId());

    }

    public void testRetry() throws Exception {
//        TestFormManager.createFileForTesting(null);
//        _mailManager.retry();
//        List<Form> forms = _formManager.getForms();
//        assertNotNull(forms);
//        assertTrue(forms.isEmpty());
    }


    private IFormManager _formManager;
    private IMailManager _mailManager;
}
