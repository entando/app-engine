package org.entando.entando.plugins.jpwebform.aps.system.services.mail;

import static org.entando.entando.plugins.jpwebform.aps.system.services.mail.MailTemplate.EMAIL_TEMPLATE;

import com.agiletec.aps.system.common.AbstractService;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.Form;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.IFormManager;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormData;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Arrays;
import java.util.Date;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MailManager extends AbstractService implements IMailManager {

    private static final Logger log =  LoggerFactory.getLogger(MailManager.class);

    private String _mailHost;
    private boolean _debug;
    private int _mailPort;
    private String _mailUsername;
    private String _mailPassword;
    private boolean _mailAuth;
    // TLS
    private boolean _startTls;
    private boolean _tlsRequired;
    // SMTPS
    private boolean _sslEnable;
    // recipients
    private String _recipientsCSV;

    private Map<String, String> _recipients;

    private IFormManager _formManager;


    @Override
    public void init() throws Exception {
        log.info("Mail configuration parameters");
        log.info("\tHost: " + getMailHost());
        log.info("\tmailPort: " + getMailPort());
        log.info("\tmailUsername: " + getMailUsername());
        if (StringUtils.isNotBlank(getMailPassword())) {
            log.info("\tpassword: ****");
        } else {
            log.warn("\tpassword: !!!NOT SET!!!");
        }
        log.info("\tmailAuth: " + isMailAuth());
        log.info("\tstartTls: " + isStartTls());
        log.info("\ttlsRequired: " + isTlsRequired());
        log.info("\tsslEnable: " + isSslEnable());
        log.info("\tdebug: " + isDebug());
        // process recipients
        if (StringUtils.isNotBlank(getRecipientsCSV())) {
            String[] tokens = getRecipientsCSV().split(",");
            _recipients = Arrays.stream(tokens)
                    .map(s -> s.split("="))
                    .collect(Collectors.toMap(a -> a[0], a -> a[1]));
            log.info("{} email address(es) configured", _recipients.size());
            // checks
            if (!_recipients.containsKey(CFG_FROM)) {
                log.error("Invalid mail configuration (missing \"FROM\" key); mail delivery won't work");
            }
            if (_recipients.size() < 2) {
                log.error("Invalid mail configuration (at least a key for the recipient and for the address \"FROM\" must be defined ); mail delivery won't work");
            }
        }
        log.debug("{} ready.", this.getClass().getName());
    }


    @Override
    public boolean sendMail(Form form) {
        Message message = getMessage();
        final String from = _recipients.get(IMailManager.CFG_FROM);
        final String body = processTemplate(EMAIL_TEMPLATE, form);

        try {
            // from
            message.setFrom(new InternetAddress(from));
            // recipients TO, BCC
            message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(form.getCc())
            );
            log.info("Mail TO: {}", form.getCc());
            message.setRecipients(
                    Message.RecipientType.BCC,
                    InternetAddress.parse(form.getRecipient())
            );
            log.info("Mail BCC: {}", form.getRecipient());
            if (StringUtils.isNotBlank(form.getSubject())) {
                message.setSubject(form.getSubject());
            } else {
                message.setSubject("Mail automatizzata");
            }
            // body
            message.setText(body);
            // finally
            Transport.send(message);
            return true;
        } catch (Exception e) {
            log.error("error delivering mail", e);
        }
        return false;
    }

    private MimeMessage getMessage() {
        Properties props = new Properties();
        Session session;

        props.put("mail.smtp.host", getMailHost());
        props.put("mail.smtp.port", String.valueOf(getMailPort()));
        props.put("mail.debug", String.valueOf(isDebug()));
        if (isStartTls()
                || isTlsRequired()) {
            log.debug("setting TLS properties");
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", String.valueOf(isMailAuth()));
            props.put("mail.smtp.starttls.enable", String.valueOf(isStartTls()));
            props.put("mail.smtp.starttls.required", String.valueOf(isTlsRequired()));
        } else {
            log.debug("setting SMTP(S) properties");
            props.put("mail.transport.protocol", "smtp");
            props.put("mail.smtp.auth", String.valueOf(isMailAuth()));
            props.put("mail.smtp.ssl.enable", String.valueOf(isSslEnable()));
        }
        if (isMailAuth()) {
            // Creazione della sessione
            session = Session.getInstance(props,
                    new javax.mail.Authenticator() {
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(getMailUsername(), getMailPassword());
                        }
                    });
            log.debug("Auth information provided");
        } else {
            session = Session.getInstance(props);
            log.warn("Auth information not provided");
        }
        return new MimeMessage(session);
    }

    @Override
    public String getEmailById(String key) {
        return _recipients.get(key);
    }

    private String processTemplate(String template, Form form) {
        if (StringUtils.isNotBlank(template)
                && form != null) {
            // process select options
            final StringBuilder sb = new StringBuilder();
            final FormData fd = form.getData();
            final SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
            String selectText = null;

            if (fd != null) {
                // dropdown #1
                if (StringUtils.isNotBlank(fd.getValore1())) {
                    sb.append(fd.getEtichettaSel1()).append(": ").append(fd.getValore1());
                    sb.append("\n");
                }
                // dropdown #2
                if (StringUtils.isNotBlank(fd.getValore2())) {
                    sb.append(fd.getEtichettaSel2()).append(": ").append(fd.getValore2());
                    sb.append("\n");
                }
                // dropdown #3
                if (StringUtils.isNotBlank(fd.getValore3())) {
                    sb.append(fd.getEtichettaSel3()).append(": ").append(fd.getValore3());
                    sb.append("\n");
                }
                // dropdown #4
                if (StringUtils.isNotBlank(fd.getValore4())) {
                    sb.append(fd.getEtichettaSel4()).append(": ").append(fd.getValore4());
                    sb.append("\n");
                }
                // dropdown #5
                if (StringUtils.isNotBlank(fd.getValore5())) {
                    sb.append(fd.getEtichettaSel5()).append(": ").append(fd.getValore5());
                    sb.append("\n");
                }
                selectText = sb.toString();
                template = template.replace("${DROPDOWN}", selectText);
                // reset the builder
                sb.setLength(0);

                // text #1
                if (StringUtils.isNotBlank(fd.getTesto1())) {
                    sb.append(fd.getEtichetta1()).append(":\n\t").append(fd.getTesto1());
                    sb.append("\n");
                }
                // text #2
                if (StringUtils.isNotBlank(fd.getTesto2())) {
                    sb.append(fd.getEtichetta2()).append(":\n\t").append(fd.getTesto2());
                    sb.append("\n");
                }
                // text #3
                if (StringUtils.isNotBlank(fd.getTesto3())) {
                    sb.append(fd.getEtichetta3()).append(":\n\t").append(fd.getTesto3());
                    sb.append("\n");
                }
                // text #4
                if (StringUtils.isNotBlank(fd.getTesto4())) {
                    sb.append(fd.getEtichetta4()).append(":\n\t").append(fd.getTesto4());
                    sb.append("\n");
                }
                // text #5
                if (StringUtils.isNotBlank(fd.getTesto5())) {
                    sb.append(fd.getEtichetta5()).append(":\n\t").append(fd.getTesto5());
                    sb.append("\n");
                }
                template = template.replace("${TESTO}", sb.toString());
            }
            // date
            if (form.getSubmitted() != null) {
                LocalDateTime localDateTime = form.getSubmitted();
                ZonedDateTime zonedDateTime = localDateTime.atZone(ZoneId.systemDefault());
                Date date = Date.from(zonedDateTime.toInstant());

                String formattedDate = formatter.format(date);
                template = template.replace("${DATA}", formattedDate);
            }
            // user qualified name
            if (StringUtils.isNotBlank(form.getQualifiedName())) {
                template = template.replace("${UTENTE}", form.getQualifiedName());
            }
        }
        return template;
    }

    /**
     * Usiamo la configurazione XML per consistenza con il resto
     * del progetto!
     */
    @Override
    public void retry() {
       /* log.info("retry service triggered");
        try {
            List<Form> forms = getFormManager().getForms();
            forms.forEach(f -> {
                try {
                    log.debug("delivering mail originally intended for {}", f.getSubmitted());
                    if (sendMail(f)) {
                        getFormManager().deleteForm(String.valueOf(f.getId()));
                    } else {
                        log.error("Could not deliver the form {} again", f.getId());
                    }
                } catch (Exception e) {
                    log.error("error while delivering email ", e);
                }
            });
        } catch (ApsSystemException e) {
            log.error("Unexpected error while delivering non expired mail", e);
        }*/
        log.info("retry service completed execution");
    }


    public String getMailHost() {
        return _mailHost;
    }

    public void setMailHost(String _mailHost) {
        this._mailHost = _mailHost;
    }

    public boolean isDebug() {
        return _debug;
    }

    public void setDebug(boolean _debug) {
        this._debug = _debug;
    }

    public int getMailPort() {
        return _mailPort;
    }

    public void setMailPort(int _mailPort) {
        this._mailPort = _mailPort;
    }

    public String getMailUsername() {
        return _mailUsername;
    }

    public void setMailUsername(String _mailUsername) {
        this._mailUsername = _mailUsername;
    }

    public String getMailPassword() {
        return _mailPassword;
    }

    public void setMailPassword(String _mailPassword) {
        this._mailPassword = _mailPassword;
    }

    public boolean isMailAuth() {
        return _mailAuth;
    }

    public void setMailAuth(boolean _mailAuth) {
        this._mailAuth = _mailAuth;
    }

    public boolean isStartTls() {
        return _startTls;
    }

    public void setStartTls(boolean _startTls) {
        this._startTls = _startTls;
    }

    public boolean isTlsRequired() {
        return _tlsRequired;
    }

    public void setTlsRequired(boolean _tlsRequired) {
        this._tlsRequired = _tlsRequired;
    }

    public boolean isSslEnable() {
        return _sslEnable;
    }

    public void setSslEnable(boolean _sslEnable) {
        this._sslEnable = _sslEnable;
    }

    public String getRecipientsCSV() {
        return _recipientsCSV;
    }

    public void setRecipientsCSV(String _recipientsCSV) {
        this._recipientsCSV = _recipientsCSV;
    }

    public IFormManager getFormManager() {
        return _formManager;
    }

    public void setFormManager(IFormManager formManager) {
        this._formManager = formManager;
    }
}
