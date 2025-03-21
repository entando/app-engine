package org.entando.entando.plugins.jpwebform.aps.system.services.mail;

public interface MailTemplate {

    String EMAIL_TEMPLATE_FIRST_SUBMIT =
            "Buongiorno,\n" +
                    "in data ${DATA} l'utente ${UTENTE} ha inviato i seguenti dati:\n" +
                    "\n"+
                    "${DROPDOWN}" +
                    "\n"+
                    "${TESTO}"+
                    "\n"+
                    "(ID comunicazione: ${SERIALE})"+
                    "\n"+
                    "Cordiali saluti";

    String EMAIL_TEMPLATE_DELETE =
            "Buongiorno,\n" +
                    "in data ${DATA} l'utente ${UTENTE} ha cancellato il form \n" +
                    "con codice ${SERIALE})"+
                    "\n"+
                    "Cordiali saluti";

}
