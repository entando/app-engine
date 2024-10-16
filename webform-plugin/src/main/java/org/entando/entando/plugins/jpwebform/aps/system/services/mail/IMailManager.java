package org.entando.entando.plugins.jpwebform.aps.system.services.mail;

import org.entando.entando.plugins.jpwebform.aps.system.services.form.Form;

public interface IMailManager {

    String BEAN_ID = "jpwebformMailManager";
    String CFG_FROM = "from";
    String DATA_SUBJECT = "_subject";
    String DATA_ADDRESS = "address";
    String DATA_CC_ADDRESS = "ccAddress";

    /**
     * Deliver mail with the form prospect
     *
     * @param form submitted data
     * @return true if the mail was delivered, false otherwise
     */
    boolean sendMail(Form form);

    /**
     * Return the email associated to the given key
     * @param key id of the mail to get
     * @return the desired email
     */
    String getEmailById(String key);

    /**
     * Try to deliver again forms submitted no longer than 6 hours before.
     * If the delivery is successful, the form is deleted
     */
    void retry();
}
