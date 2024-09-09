package it.difesa.esercito.plugins.jpwebform.aps.system.services.mail;

public interface MailTemplate {

    String EMAIL_TEMPLATE =
        "Buongiorno,\n"
        + " in data ${DATA} l'utente ${UTENTE} ha inviato i seguenti dati:\n"
        + " \n"
        + " ${DROPDOWN}\n"
        + " \n"
        + " ${TESTO}\n"
        + " \n"
        + "Cordiali saluti";

}
