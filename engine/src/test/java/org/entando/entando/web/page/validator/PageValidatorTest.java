package org.entando.entando.web.page.validator;

import com.agiletec.aps.system.services.page.IPage;
import com.agiletec.aps.system.services.page.IPageManager;
import com.agiletec.aps.system.services.user.User;
import com.agiletec.aps.system.services.user.UserDetails;
import java.util.HashMap;
import java.util.Map;
import liquibase.pro.packaged.U;
import org.assertj.core.util.Maps;
import org.entando.entando.aps.system.services.page.PageAuthorizationService;
import org.entando.entando.aps.system.services.widgettype.WidgetType;
import org.entando.entando.aps.system.services.widgettype.WidgetTypeManager;
import org.entando.entando.web.common.exceptions.ValidationGenericException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;
import org.springframework.validation.Errors;

@ExtendWith(MockitoExtension.class)
class PageValidatorTest {

    @Mock
    private PageAuthorizationService pageAuthorizationService;
    @Mock
    private IPageManager pageManager;
    @Mock
    private IPage page;
    @InjectMocks
    private PageValidator validator;

    @Test
    void validateMovePagePermissionsShouldReturnOk() {
        UserDetails mockUser = getMockUser();
        BindingResult errors = (new DataBinder(new Object())).getBindingResult();

        Mockito.when(pageAuthorizationService.canEdit(mockUser,"myNewParent")).thenReturn(true);
        Mockito.when(pageAuthorizationService.canEdit(mockUser,"myOldParent")).thenReturn(true);
        Mockito.when(page.getParentCode()).thenReturn("myOldParent");
        Mockito.when(pageManager.getDraftPage("myCode")).thenReturn(page);

        validator.validateMovePagePermissions(mockUser, "myCode", "myNewParent", pageAuthorizationService, errors);
        Assertions.assertFalse(errors.hasErrors());
    }

    @Test
    void validateMovePagePermissionsShouldReturnExceptionIfYouCannotWriteNewParent() {
        UserDetails mockUser = getMockUser();
        BindingResult errors = (new DataBinder(new Object())).getBindingResult();

        Mockito.when(pageAuthorizationService.canEdit(mockUser,"myNewParent")).thenReturn(false);
        Mockito.when(pageAuthorizationService.canEdit(mockUser,"myOldParent")).thenReturn(true);
        Mockito.when(page.getParentCode()).thenReturn("myOldParent");
        Mockito.when(pageManager.getDraftPage("myCode")).thenReturn(page);

        Assertions.assertThrows(ValidationGenericException.class,
                () -> validator.validateMovePagePermissions(mockUser,
                        "myCode",
                        "myNewParent",
                        pageAuthorizationService,
                        errors));
        Assertions.assertTrue(errors.hasErrors());
    }

    @Test
    void validateMovePagePermissionsShouldReturnExceptionIfYouCannotWriteOldParent() {
        UserDetails mockUser = getMockUser();
        BindingResult errors = (new DataBinder(new Object())).getBindingResult();

        Mockito.when(pageAuthorizationService.canEdit(mockUser,"myOldParent")).thenReturn(false);
        Mockito.when(page.getParentCode()).thenReturn("myOldParent");
        Mockito.when(pageManager.getDraftPage("myCode")).thenReturn(page);

        Assertions.assertThrows(ValidationGenericException.class,
                () -> validator.validateMovePagePermissions(mockUser,
                        "myCode",
                        "myNewParent",
                        pageAuthorizationService,
                        errors));
        Assertions.assertTrue(errors.hasErrors());
    }

    @Test
    void validateRootPageShouldRejectRootPageDeletion() {
        IPage rootPage = Mockito.mock(IPage.class);
        Mockito.when(rootPage.getCode()).thenReturn("homepage");
        Mockito.when(pageManager.getDraftRoot()).thenReturn(rootPage);

        BindingResult errors = (new DataBinder(new Object())).getBindingResult();
        validator.validateRootPage("homepage", errors);
        Assertions.assertTrue(errors.hasErrors());
    }

    @Test
    void validateRootPageShouldAllowNonRootPageDeletion() {
        IPage rootPage = Mockito.mock(IPage.class);
        Mockito.when(rootPage.getCode()).thenReturn("homepage");
        Mockito.when(pageManager.getDraftRoot()).thenReturn(rootPage);

        BindingResult errors = (new DataBinder(new Object())).getBindingResult();
        validator.validateRootPage("some_other_page", errors);
        Assertions.assertFalse(errors.hasErrors());
    }

    private UserDetails getMockUser(){
        User u = new User();
        u.setUsername("mockUser");
        return u;
    }
}
