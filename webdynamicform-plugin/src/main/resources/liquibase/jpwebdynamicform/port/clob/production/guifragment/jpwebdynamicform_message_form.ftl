<#assign wp=JspTaglibs["/aps-core"]>
<#--
    jpwebdynamicform_message_form - Message Form Widget
    ====================================================
    This widget renders a dynamic message form via internalServlet.
    The Struts action dispatches to GUI fragments (stored in DB, tenant-aware)
    with JSP fallback.

    Struts action results GUI fragments (see userMessage.xml):
      - jpwebdynamicform_is_entryMessage        : Main form with all attribute fields
      - jpwebdynamicform_is_messageTypeFinding   : Message type selection (if multiple types)
      - jpwebdynamicform_is_captchaPage          : CAPTCHA confirmation step
      - jpwebdynamicform_is_messageSaveConfirmed : Success confirmation page

    Attribute type fragments (customizable per-tenant via DB):
      - jpwebdynform_is_front-BooleanAttribute      : Boolean (Yes/No radio buttons)
      - jpwebdynform_is_front-CheckboxAttribute      : CheckBox (single checkbox)
      - jpwebdynform_is_front-DateAttribute          : Date (native HTML5 date input)
      - jpwebdynform_is_front-EnumeratorAttribute    : Enumerator (select dropdown)
      - jpwebdynform_is_front-EnumeratorMapAttribute : EnumeratorMap (select with key/value)
      - jpwebdynform_is_front-LongtextAttribute      : Longtext (textarea)
      - jpwebdynform_is_front-NumberAttribute         : Number (text input)
      - jpwebdynform_is_front-MonotextAttribute      : Monotext/Text (text input, also default fallback)
      - jpwebdynform_is_front-ThreeStateAttribute    : ThreeState (Yes/No/None radio buttons)
      - jpwebdynform_is_front-CompositeAttribute     : Composite (group of sub-attributes)

    Utility fragments:
      - jpwebdynform_is_front_AttributeInfo          : Required field indicator (*)
      - jpwebdynform_is_front_attributeInfo-help-block : Validation hints (min/max length, OGNL help)
      - jpwebdynform_is_front-DateSubmitHandler      : Date format conversion (yyyy-MM-dd -> dd/MM/yyyy)
      - jpwebdynamicform_is_captchaInclude           : Google reCAPTCHA v2/v3 integration
-->
<@wp.internalServlet actionPath="/ExtStr2/do/jpwebdynamicform/Message/User/new" />