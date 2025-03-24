/*
 *
 * <Your licensing text here>
 *
 */
package org.entando.entando.plugins.jpwebform.aps.internalservlet.form;

import static com.agiletec.aps.system.SystemConstants.ADMIN_USER_NAME;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_DROPDOWN_LABEL_1;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_DROPDOWN_LABEL_2;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_DROPDOWN_LABEL_3;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_DROPDOWN_LABEL_4;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_DROPDOWN_LABEL_5;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_MAIL_OBJECT;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TEXT_LABEL_1;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TEXT_LABEL_2;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TEXT_LABEL_3;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TEXT_LABEL_4;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TEXT_LABEL_5;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TEXT_REQUIRED_1;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TEXT_REQUIRED_2;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TEXT_REQUIRED_3;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TEXT_REQUIRED_4;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TEXT_REQUIRED_5;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.CFG_TITLE;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.ENTANDO_GROUP_BASE;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.FOLLOW_UP_ADMITTED;
import static org.entando.entando.plugins.jpwebform.WebformSystemConstants.PERM_FORM_ADMIN;

import com.agiletec.aps.system.SystemConstants;
import com.agiletec.aps.system.common.FieldSearchFilter;
import com.agiletec.aps.system.services.authorization.Authorization;
import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.Widget;
import com.agiletec.aps.system.services.user.UserDetails;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.Form;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.DeliveryData;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormConfiguration;
import org.entando.entando.plugins.jpwebform.aps.system.services.form.model.FormData;
import org.entando.entando.plugins.jpwebform.aps.system.services.mail.MailTemplate;
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
            String etichettaSel1 = (String) widget.getConfig().get(CFG_DROPDOWN_LABEL_1);
            String etichettaSel2 = (String) widget.getConfig().get(CFG_DROPDOWN_LABEL_2);
            String etichettaSel3 = (String) widget.getConfig().get(CFG_DROPDOWN_LABEL_3);
            String etichettaSel4 = (String) widget.getConfig().get(CFG_DROPDOWN_LABEL_4);
            String etichettaSel5 = (String) widget.getConfig().get(CFG_DROPDOWN_LABEL_5);

            String etichetta1 = (String) widget.getConfig().get(CFG_TEXT_LABEL_1);
            String etichetta2 = (String) widget.getConfig().get(CFG_TEXT_LABEL_2);
            String etichetta3 = (String) widget.getConfig().get(CFG_TEXT_LABEL_3);
            String etichetta4 = (String) widget.getConfig().get(CFG_TEXT_LABEL_4);
            String etichetta5 = (String) widget.getConfig().get(CFG_TEXT_LABEL_5);

            String obbligatorio1 = (String) widget.getConfig().get(CFG_TEXT_REQUIRED_1);
            String obbligatorio2 = (String) widget.getConfig().get(CFG_TEXT_REQUIRED_2);
            String obbligatorio3 = (String) widget.getConfig().get(CFG_TEXT_REQUIRED_3);
            String obbligatorio4 = (String) widget.getConfig().get(CFG_TEXT_REQUIRED_4);
            String obbligatorio5 = (String) widget.getConfig().get(CFG_TEXT_REQUIRED_5);

            setSubject((String) widget.getConfig().get(CFG_MAIL_OBJECT));

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
        final Form form = new Form();

        form.setData(getFormData());
        form.setDelivery(new DeliveryData());
        form.setConfiguration(new FormConfiguration());
        if (StringUtils.isNotBlank(getSeriale())) {
            form.setSerial(getSeriale());
            log.info("chaining form with serial {}", getSeriale());
        }

        try {
            Widget widget = getWidgetConfig();

            if (widget == null) {
                log.error("couldn't find the widget configuration");
                return INPUT;
            }

            form.setCampaign((String) widget.getConfig().get(CFG_TITLE));

            final String currentUser = this.getCurrentUser().getUsername();
            log.debug("looking for user '{}'", currentUser);

            final String json = null; // getSigeManager().getUserInfoById(currentUser);
            final String fullName = this.getCurrentUser().getUsername();

            form.getDelivery().setQualifiedName(fullName);
//            if (StringUtils.isNotBlank(json)) {
            final String emailFromSPID = "email@email.it";
            //form.setCc(emailFromSPID);
            form.getDelivery().setCc(emailFromSPID);
//            } else {
//                log.warn("Could not get SIGE data for user '{}'", currentUser);
//            }

            form.setName(currentUser);
            form.setSubmitted(LocalDateTime.now());

//            final String email = getMailManager().getEmailById(getIdDestinatario());
//            if (StringUtils.isBlank(email)) {
//                log.warn("Could not find email with key '{}'", getIdDestinatario());
//            }
//            final String from = getMailManager().getEmailById(IMailManager.CFG_FROM);
//            if (StringUtils.isBlank(from)) {
//                log.warn("Could not find email with key '{}'", IMailManager.CFG_FROM);
//            }

            form.getDelivery().setRecipient(getIdDestinatario());
            form.getDelivery().setSubject(getSubject());

            if (getMailManager().sendMail(form, MailTemplate.EMAIL_TEMPLATE_FIRST_SUBMIT, true)) {
                log.debug("Form successfully delivered to {}", form.getDelivery().getRecipient());
                form.setDelivered(true);
            } else {
                log.warn("Could not deliver email to {}, saving for later", form.getDelivery().getRecipient());
                form.setDelivered(false);
            }
            form.getConfiguration().setProperties(widget.getConfig());
            getFormManager().addForm(form);
        } catch (Exception e) {
            log.error("unexpected exception while processing the form from user {}", getCurrentUser());
            return "not_delivered";
        }
        if (!form.getDelivered()) {
            return "not_delivered";
        }
        return SUCCESS;
    }

    public String update() {
        Form form = new Form();

        try {
            form.setId(getId());
            form.setData(getFormData());
            // this will overwrite data only, the rest is left untouched
            getFormManager().updateFormData(form);
        } catch (Throwable t) {
            log.error("unexpected exception while processing the form from user {}", getCurrentUser());
            return FAILURE;
        }
        return SUCCESS;
    }

    public List<Long> getFormsId() {
        try {
            FieldSearchFilter[] filters = (FieldSearchFilter[]) createFilters();

            // in case of standard user we restrict form by owner
            if (!isPrivilegedUser()) {
                List<Object> safeFilters = Arrays.stream(filters)
                        .filter(f ->
                                !((FieldSearchFilter<?>)f).getKey().equals("name")  // exclude NAME filter
                        )
                        .collect(Collectors.toList());
                // impose the username
                FieldSearchFilter nameFilter = new FieldSearchFilter("name", getCurrentUser().getUsername(), false);
                safeFilters.add(nameFilter);
                FieldSearchFilter[] userFilters = (FieldSearchFilter[])
                        safeFilters.toArray(FieldSearchFilter[]::new);
                return getFormManager().search(userFilters);
            }
            // if the user belongs to form group we restrict by group
            // TODO
            // we return the full list
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
            final Form form = getFormManager().getForm(getId());
            this.getFormManager().deleteForm(getId());
            this.getMailManager().sendMail(form, MailTemplate.EMAIL_TEMPLATE_DELETE, false);
            log.error("deleted form id {}", getId());
        } catch (Exception e) {
            log.error("error deleting form id {}", getId(), e);
            return FAILURE;
        }
        return SUCCESS;
    }

    public Form getForm(Long id) {
        try {
            final Form form = getFormManager().getForm(id);

            log.info("loading form id {}", id);
            // TODO controllare gruppo qui
            if (isPrivilegedUser()
                    || (!isPrivilegedUser() && form.getName().equals(getCurrentUser().getUsername()))) {
                return form;
            } else {
                log.info("Form {} does not belong to user {}", form.getName(), this.getCurrentUser().getUsername());
            }
        } catch (Exception e) {
            log.error("error loading form {}", id, e);
        }
        return null;
    }

    /**
     * Check whether the user has admin privileges on form
     * @return
     */
    public boolean isPrivilegedUser() {
        final UserDetails user = this.getCurrentUser();
        return user.getUsername().equals(ADMIN_USER_NAME)
                || isFormPrivilegedUser(user, true)
                || isFormPrivilegedUser(user, false);
    }

    /**
     * Check whether the current user has privileged permissions on form management
     *
     * @param user       the current user
     * @param checkGroup if true the grooup is checked, the permission otherwise
     * @return
     */
    private boolean isFormPrivilegedUser(UserDetails user, boolean checkGroup) {
        return user.getAuthorizations().stream()
                .anyMatch(auth -> isPrivileged(auth, checkGroup));
    }

    private boolean isPrivileged(Authorization auth, boolean checkGroup) {
                    if (checkGroup) {
            return auth.getGroup() != null && auth.getGroup().getName().startsWith(ENTANDO_GROUP_BASE);
                    } else {
            return auth.getRole() != null && auth.getRole().getPermissions().contains(PERM_FORM_ADMIN);
                        }
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
            FieldSearchFilter nameFilter = new FieldSearchFilter("campaign", getCampagna(), true);
            filters.add(nameFilter);
        }
        if (StringUtils.isNotBlank(getSeriale())) {
            FieldSearchFilter serialeFilter = new FieldSearchFilter("serial", getSeriale(), false);
            filters.add(serialeFilter);
        }
        if (StringUtils.isNotBlank(getDelivered()) && !getDelivered().equals("--")) {
            Boolean delivered = Boolean.parseBoolean(getDelivered());
            FieldSearchFilter deliveredFilter = new FieldSearchFilter("delivered", delivered, false);
            filters.add(deliveredFilter);
        }
        if (StringUtils.isNotBlank(getIsHead()) ) {
            if (getIsHead().equals("--")) {
                FieldSearchFilter isHeaddFilter = new FieldSearchFilter("is_head", Boolean.TRUE, false);
                filters.add(isHeaddFilter);
            } else {
                if (!getIsHead().equals("all")) {
                    Boolean isHead = Boolean.parseBoolean(getIsHead());
                    FieldSearchFilter isHeaddFilter = new FieldSearchFilter("is_head", isHead, false);
                    filters.add(isHeaddFilter);
                }
            }
        }
        return filters.toArray(new FieldSearchFilter[filters.size()]);
    }


    public Map<String, String> retrieveDropDown(final Form form, final String paramName) {
        if (StringUtils.isNotBlank(paramName)
                && form != null
                && form.getConfiguration() != null)  {
            String param =
                    (String) form.getConfiguration().getProperties().get(paramName);
            return generateDropDown(param);
        }
        return null;
    }

    /**
     * Genera la lista delle opzioni dato l'ingresso della configurazione del widget
     * @param options CSV inserito in configurazione con le opzioni
     * @return lista delle opzioni come richiesto dal tag di Struts
     */
    public Map<String, String> generateDropDown(final String options) {
        final Map<String, String> map = new LinkedHashMap<>();

        if (StringUtils.isNotBlank(options)) {
            String[] tokens = options.split(";");
            for (String token: tokens) {
                map.put(token, token);
            }
        }
        return map;
    }

    public String getRedirectionUrl() {
        final Widget currentWidget = this.getWidgetConfig();
        String redirection = "";

        if (currentWidget != null
                && currentWidget.getConfig() != null
                && currentWidget.getConfig().contains("redirectionUrl")) {
            redirection = (String) currentWidget.getConfig().get("redirectUrl");
        }
        return redirection;
    }

    public boolean enableFollowUp(String seriale) {
        if (StringUtils.isNotBlank(seriale)) {
            return getFormManager().getFormThreadCount(seriale) - 1 < FOLLOW_UP_ADMITTED;
        }
        return false;
    }

    public String getIdDestinatario() {
        return _idDestinatario;
    }

    public void setIdDestinatario(String idDestinatario) {
        this._idDestinatario = idDestinatario;
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

    public void setFormData(FormData formData) {
        this._formData = formData;
    }

    public FormData getFormData() {
        return _formData;
    }

    public String getIsHead() {
        return _isHead;
    }

    public void setIsHead(String isHead) {
        this._isHead = isHead;
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
    private String _isHead;

    private FormData _formData;
    private String _idDestinatario;
    public String _pageCode;
    public String _subject;
}
