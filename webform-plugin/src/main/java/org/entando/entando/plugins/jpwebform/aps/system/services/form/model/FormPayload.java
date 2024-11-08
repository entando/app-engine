package org.entando.entando.plugins.jpwebform.aps.system.services.form.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public class FormPayload {

    private FormData formData;
    private DeliveryData deliveryData;

    public FormData getFormData() {
        return formData;
    }

    public void setFormData(FormData formData) {
        this.formData = formData;
    }

    public DeliveryData getDeliveryData() {
        return deliveryData;
    }

    public void setDeliveryData(DeliveryData deliveryData) {
        this.deliveryData = deliveryData;
    }
}
