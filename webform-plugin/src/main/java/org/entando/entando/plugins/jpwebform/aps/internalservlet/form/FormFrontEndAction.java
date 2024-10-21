/*
 *
 * <Your licensing text here>
 *
 */
package org.entando.entando.plugins.jpwebform.aps.internalservlet.form;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.Widget;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.Form;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormData;
import org.entando.entando.plugins.jpwebform.aps.system.services.mail.IMailManager;
import org.entando.entando.plugins.jpwebform.apsadmin.form.FormAction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FormFrontEndAction extends FormAction {

    private static final Logger log =  LoggerFactory.getLogger(FormFrontEndAction.class);

    @Override
    public void validate() {
        final Widget widget = getWidgetConfig();

        if (getCurrentUser().getUsername().equals(SystemConstants.GUEST_USER_NAME)) {
            addActionError("Solo gli utenti loggati possono fare il submit del form");
            return;
        }

        if (widget != null) {
            String etichettaSel1 = (String) widget.getConfig().get("opzione1");
            String etichettaSel2 = (String) widget.getConfig().get("opzione2");
            String etichettaSel3 = (String) widget.getConfig().get("opzione3");
            String etichettaSel4 = (String) widget.getConfig().get("opzione4");
            String etichettaSel5 = (String) widget.getConfig().get("opzione5");

            String etichetta1 = (String) widget.getConfig().get("etichetta1");
            String etichetta2 = (String) widget.getConfig().get("etichetta2");
            String etichetta3 = (String) widget.getConfig().get("etichetta3");
            String etichetta4 = (String) widget.getConfig().get("etichetta4");
            String etichetta5 = (String) widget.getConfig().get("etichetta5");

            String obbligatorio1 = (String) widget.getConfig().get("obbligatorio1");
            String obbligatorio2 = (String) widget.getConfig().get("obbligatorio2");
            String obbligatorio3 = (String) widget.getConfig().get("obbligatorio3");
            String obbligatorio4 = (String) widget.getConfig().get("obbligatorio4");
            String obbligatorio5 = (String) widget.getConfig().get("obbligatorio5");

            setSubject((String) widget.getConfig().get("oggetto"));

            // update form data
            getFormData().setEtichettaSel1(etichettaSel1);
            getFormData().setEtichettaSel2(etichettaSel2);
            getFormData().setEtichettaSel3(etichettaSel3);
            getFormData().setEtichettaSel4(etichettaSel4);
            getFormData().setEtichettaSel5(etichettaSel5);

            getFormData().setEtichetta1(etichetta1);
            getFormData().setEtichetta2(etichetta2);
            getFormData().setEtichetta3(etichetta3);
            getFormData().setEtichetta4(etichetta4);
            getFormData().setEtichetta5(etichetta5);
            
            if (StringUtils.isNotBlank(etichettaSel1)
                    && StringUtils.isBlank(getFormData().getValore1())) {
                addFieldError(etichettaSel1, "Deve essere specificato un valore");
            }
            if (StringUtils.isNotBlank(etichettaSel2)
                    && StringUtils.isBlank(getFormData().getValore2())) {
                addFieldError(etichettaSel2, "Deve essere specificato un valore");
            }
            if (StringUtils.isNotBlank(etichettaSel3)
                    && StringUtils.isBlank(getFormData().getValore3())) {
                addFieldError(etichettaSel3, "Deve essere specificato un valore");
            }
            if (StringUtils.isNotBlank(etichettaSel4)
                    && StringUtils.isBlank(getFormData().getValore4())) {
                addFieldError(etichettaSel4, "Deve essere specificato un valore");
            }
            if (StringUtils.isNotBlank(etichettaSel5)
                    && StringUtils.isBlank(getFormData().getValore5())) {
                addFieldError(etichettaSel5, "Deve essere specificato un valore");
            }

            if (StringUtils.isNotBlank(etichetta1)
                    && Boolean.parseBoolean(obbligatorio1)
                    && StringUtils.isBlank(getFormData().getTesto1())) {
                addFieldError(etichetta1, etichetta1 + ": deve essere inserito un testo");
            }
            if (StringUtils.isNotBlank(etichetta2)
                    && Boolean.parseBoolean(obbligatorio2)
                    && StringUtils.isBlank(getFormData().getTesto2())) {
                addFieldError(etichetta2, etichetta2 + ": deve essere inserito un testo");
            }
            if (StringUtils.isNotBlank(etichetta3)
                    && Boolean.parseBoolean(obbligatorio3)
                    && StringUtils.isBlank(getFormData().getTesto3())) {
                addFieldError(etichetta3, etichetta3 + ": deve essere inserito un testo");
            }
            if (StringUtils.isNotBlank(etichetta4)
                    && Boolean.parseBoolean(obbligatorio4)
                    && StringUtils.isBlank(getFormData().getTesto4())) {
                addFieldError(etichetta4, etichetta4 + ": deve essere inserito un testo");
            }
            if (StringUtils.isNotBlank(etichetta5)
                    && Boolean.parseBoolean(obbligatorio5)
                    && StringUtils.isBlank(getFormData().getTesto5())) {
                addFieldError(etichetta5, etichetta5 + ": deve essere inserito un testo");
            }

//            widget.getConfig()
//                    .forEach((k,v) -> System.out.println(k + ":" + v));
        } else {
            addActionError("Impossibile processare la richiesta");
        }
    }

    private Widget getWidgetConfig() {
        final IPage page = this.getPageManager().getOnlinePage(getPageCode());
        final String currentFrame = this.getRequest().getParameterMap().get("internalServletFrameDest")[0];
        final Widget widget = page.getWidgets()[Integer.parseInt(currentFrame)];
        return widget;
    }

    public String render() {
        return SUCCESS;
    }

    public String deliver() {
        Form form = new Form();

        try {
            Widget widget = getWidgetConfig();

            if (widget == null) {
                log.error("couldn't find the widget configuration");
                return INPUT;
            }

            form.setCampagna((String) widget.getConfig().get("titolo"));

            final String currentUser = this.getCurrentUser().getUsername();
            log.debug("looking for user '{}'", currentUser);
            final String json = null; // getSigeManager().getUserInfoById(currentUser);

            final String fullName = this.getCurrentUser().getUsername();
            form.setQualifiedName(fullName);

//            if (StringUtils.isNotBlank(json)) {
                final String emailFromSPID = "email@email.it";
                form.setCc(emailFromSPID);
//            } else {
//                log.warn("Could not get SIGE data for user '{}'", currentUser);
//            }
            form.setName(currentUser);
            form.setSubmitted(LocalDateTime.now());
            form.setData(getFormData());

            final String email = getMailManager().getEmailById(getIdDestinatario());
            if (StringUtils.isBlank(email)) {
                log.warn("Could not find email with key '{}'", getIdDestinatario());
            }
            final String from = getMailManager().getEmailById(IMailManager.CFG_FROM);
            if (StringUtils.isBlank(from)) {
                log.warn("Could not find email with key '{}'", IMailManager.CFG_FROM);
            }

            form.setRecipient(email);
            form.setSubject(getSubject());

            if (getMailManager().sendMail(form)) {
                log.debug("Form successfully delivered to {}", form.getRecipient());
                form.setDelivered(true);
            } else {
                log.warn("Could not deliver email to {}, saving for later", form.getRecipient());
                form.setDelivered(false);
            }
            getFormManager().addForm(form);
        } catch (Exception e) {
            log.error("unexpected exception while processing the form from user {}", getCurrentUser());
            return FAILURE;
        }
        if (!form.getDelivered()) {
            return "not_delivered";
        }
        return SUCCESS;
    }

    public List<Long> getFormsId() {
        try {
            FieldSearchFilter[] filters = (FieldSearchFilter[]) createFilters();
            return getFormManager().search(filters);
        } catch (Exception e) {
            log.error("errore caricamento forms", e);
        }
        return null;
    }


    public String detail() {
        return SUCCESS;
    }
    public String trash() {
        return SUCCESS;
    }

    public String delete() {
        try {
            this.getFormManager().deleteForm(getId());
            log.error("deleted form id {}", getId());
        } catch (Exception e) {
            log.error("error deleting form id {}", getId(), e);
            return FAILURE;
        }
        return SUCCESS;
    }

    public Form getForm(Long id) {
        try {
            return getFormManager().getForm(id);
        } catch (Exception e) {
            log.error("error loading form {}", id, e);
        }
        return null;
    }

    private Object[] createFilters() {
        final List<FieldSearchFilter> filters = new ArrayList<>();

        if (getId() != null) {
            FieldSearchFilter idFilter = new FieldSearchFilter("id", getId(), false);
            filters.add(idFilter);
        }
        if (getFrom() != null && getTo() != null) {
            FieldSearchFilter dateFilter = new FieldSearchFilter("submitted", getFrom(), getTo());
            filters.add(dateFilter);
        }
        if (StringUtils.isNotBlank(getName())) {
            FieldSearchFilter nameFilter = new FieldSearchFilter("name", getName(), true);
            filters.add(nameFilter);
        }
        if (StringUtils.isNotBlank(getCampagna())) {
            FieldSearchFilter nameFilter = new FieldSearchFilter("campagna", getCampagna(), true);
            filters.add(nameFilter);
        }
        if (StringUtils.isNotBlank(getSeriale())) {
            FieldSearchFilter serialeFilter = new FieldSearchFilter("seriale", getSeriale(), false);
            filters.add(serialeFilter);
        }
        if (StringUtils.isNotBlank(getDelivered()) && !getDelivered().equals("--")) {
            Boolean delivered = Boolean.parseBoolean(getDelivered());
            FieldSearchFilter deliveredFilter = new FieldSearchFilter("delivered", delivered, false);
            filters.add(deliveredFilter);
        }
        return filters.toArray(new FieldSearchFilter[filters.size()]);
    }

    /**
     * Genera la lista delle opzioni dato l'ingresso della configurazione del widget
     * @param options CSV inserito in configurazione con le opzioni
     * @return lista delle opzioni come richiesto dal tag di Struts
     */
    public Map<String, String> generateDropDown(String options) {
        final Map<String, String> map = new HashMap<>();

        if (StringUtils.isNotBlank(options)) {
            String[] tokens = options.split(";");
            for (String token: tokens) {
                map.put(token, token);
            }
        }
        return map;
    }


    public String getIdDestinatario() {
        return _idDestinatario;
    }

    public void setIdDestinatario(String idDestinatario) {
        this._idDestinatario = idDestinatario;
    }

    public FormData getFormData() {
        return _formData;
    }

    public void setFormData(FormData formData) {
        this._formData = formData;
    }

    public String getPageCode() {
        return _pageCode;
    }

    public void setPageCode(String pageCode) {
        this._pageCode = pageCode;
    }

    public String getSubject() {
        return _subject;
    }

    public void setSubject(String subject) {
        this._subject = subject;
    }

    public Date getFrom() {
        return _from;
    }

    public void setFrom(Date from) {
        this._from = from;
    }

    public Date getTo() {
        return _to;
    }

    public void setTo(Date to) {
        this._to = to;
    }

    public String getDelivered() {
        return _delivered;
    }

    public void setDelivered(String delivered) {
        this._delivered = delivered;
    }

    public String getPractice() {
        return _practice;
    }

    public void setPractice(String practice) {
        this._practice = practice;
    }

    @Override
    public String getName() {
        return _name;
    }

    @Override
    public void setName(String name) {
        this._name = name;
    }

    public Long getId() {
        return _id;
    }

    public void setId(Long id) {
        this._id = id;
    }

    public String getCampagna() {
        return _campagna;
    }

    public void setCampagna(String _campagna) {
        this._campagna = _campagna;
    }

    public String getSeriale() {
        return _seriale;
    }

    public void setSeriale(String _seriale) {
        this._seriale = _seriale;
    }

    // search parameter
    private Long _id;
    private Date _from;
    private Date _to;
    private String _delivered;
    private String _practice;
    private String _name;
    private String _campagna;
    private String _seriale;

    private Form form;


    private FormData _formData;
    private String _idDestinatario;
    public String _pageCode;
    public String _subject;
}
