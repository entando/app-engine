package org.entando.entando.web.userprofile.validator;

import static org.mockito.Mockito.*;

import java.util.List;
import java.util.Map;
import org.entando.entando.aps.system.services.entity.model.EntityAttributeDto;
import org.entando.entando.aps.system.services.entity.model.EntityDto;
import org.entando.entando.web.common.exceptions.ValidationConflictException;
import org.junit.jupiter.api.Assertions;
import org.mockito.Mockito;
import org.junit.jupiter.api.Test;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;

class ProfileValidatorTest {

    @Test
    void validateFullNameShouldThrowExceptionIfFullNameIsNotValid() {

        // --Given
        EntityDto entityDto = new EntityDto();
        EntityAttributeDto entityAttributeDto = new EntityAttributeDto();
        entityAttributeDto.setCode("fullname");
        entityAttributeDto.setValue(" Not Valid Full Name");
        entityDto.setAttributes(List.of(entityAttributeDto));
        BindingResult bindingResult = new BeanPropertyBindingResult(entityDto, "entityDto");

        // Then
        ProfileValidator profileValidator = new ProfileValidator();
        Assertions.assertThrows(ValidationConflictException.class,
                () -> profileValidator.validateFullName(entityDto, bindingResult));

    }

    @Test
    void validateFullNameShouldNotThrowExceptionIfFullNameIsValid() {

        // --Given
        EntityDto entityDto = new EntityDto();
        EntityAttributeDto entityAttributeDto = new EntityAttributeDto();
        entityAttributeDto.setCode("fullname");
        entityAttributeDto.setValue("Valid  Full-Name");
        entityDto.setAttributes(List.of(entityAttributeDto));
        BindingResult bindingResult = new BeanPropertyBindingResult(entityDto, "entityDto");

        // Then
        ProfileValidator profileValidator = new ProfileValidator();
        Assertions.assertDoesNotThrow(() -> profileValidator.validateFullName(entityDto, bindingResult));

    }

}
