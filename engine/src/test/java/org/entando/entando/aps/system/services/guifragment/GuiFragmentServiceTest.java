/*
 * Copyright 2018-Present Entando Inc. (http://www.entando.com) All rights reserved.
 *
 * This library is free software; you can redistribute it and/or modify it under
 * the terms of the GNU Lesser General Public License as published by the Free
 * Software Foundation; either version 2.1 of the License, or (at your option)
 * any later version.
 *
 * This library is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public License for more
 * details.
 */
package org.entando.entando.aps.system.services.guifragment;

import static org.assertj.core.api.Assertions.assertThat;
import static org.entando.entando.aps.system.services.guifragment.FragmentTestUtil.validFragmentRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agiletec.aps.system.services.lang.ILangManager;
import com.agiletec.aps.system.services.lang.Lang;
import com.agiletec.aps.util.ApsProperties;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;
import org.entando.entando.aps.system.services.IComponentDto;
import org.entando.entando.aps.system.services.assertionhelper.ComponentUsageEntityAssertionHelper;
import org.entando.entando.aps.system.services.guifragment.model.GuiFragmentDto;
import org.entando.entando.aps.system.services.guifragment.model.GuiFragmentDtoBuilder;
import org.entando.entando.aps.system.services.mockhelper.FragmentMockHelper;
import org.entando.entando.aps.system.services.mockhelper.PageMockHelper;
import org.entando.entando.aps.system.services.mockhelper.WidgetMockHelper;
import org.entando.entando.aps.system.services.widgettype.IWidgetTypeManager;
import org.entando.entando.aps.system.services.widgettype.WidgetType;
import org.entando.entando.web.common.assembler.PagedMetadataMapper;
import org.entando.entando.web.common.exceptions.ValidationGenericException;
import org.entando.entando.web.common.model.PagedMetadata;
import org.entando.entando.web.component.ComponentUsageEntity;
import org.entando.entando.web.guifragment.model.GuiFragmentRequestBody;
import org.entando.entando.web.page.model.PageSearchRequest;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ListableBeanFactory;

@ExtendWith(MockitoExtension.class)
class GuiFragmentServiceTest {

    @InjectMocks
    private GuiFragmentService guiFragmentService;

    @Mock
    private GuiFragmentDtoBuilder dtoBuilder;

    @Mock
    private IWidgetTypeManager widgetTypeManager;

    @Mock
    private IGuiFragmentManager guiFragmentManager;

    @Mock
    private ILangManager langManager;

    @Spy
    private PagedMetadataMapper pagedMetadataMapper = new PagedMetadataMapper();

    @BeforeEach
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        Lang defaultLang = mock(Lang.class);
        Mockito.lenient().when(defaultLang.getCode()).thenReturn("en");
        Mockito.lenient().when(langManager.getDefaultLang()).thenReturn(defaultLang);
    }

    @Test
    void shouldRaiseExceptionOnDeleteReservedFragment() throws Throwable {
        GuiFragment reference = new GuiFragment();
        reference.setCode("referenced_code");
        reference.setGui("<p>Code</p>");
        GuiFragmentDto dto = new GuiFragmentDto();
        dto.setCode("master");
        dto.setCode("<p>Code of master</p>");
        dto.addFragmentRef(reference);
        when(this.dtoBuilder.convert(any(GuiFragment.class))).thenReturn(dto);
        GuiFragment fragment = new GuiFragment();
        fragment.setCode("test_code");
        when(guiFragmentManager.getGuiFragment("test_code")).thenReturn(fragment);
        Assertions.assertThrows(ValidationGenericException.class, () -> {
            this.guiFragmentService.removeGuiFragment(fragment.getCode());
        });
    }

    @Test
    void shouldCreateFragment() throws Exception {
        GuiFragment fragment = FragmentMockHelper.mockGuiFragment();
        GuiFragmentDto fragmentDto = FragmentMockHelper.mockGuiFragmentDto(fragment, langManager);

        when(this.dtoBuilder.convert(any(GuiFragment.class))).thenReturn(fragmentDto);

        // Given
        String expectedGui = "<#assign wp=JspTaglibs[ \"/aps-core\"]>\n"
                + "<script nonce=\"<@wp.cspNonce />\">my_js_script</script>";
        GuiFragmentRequestBody request = validFragmentRequest();

        // When
        guiFragmentService.addGuiFragment(request);

        // Then
        ArgumentCaptor<GuiFragment> argumentCaptor = ArgumentCaptor.forClass(GuiFragment.class);
        verify(guiFragmentManager).addGuiFragment(argumentCaptor.capture());
        GuiFragment argument = argumentCaptor.getValue();
        assertThat(argument.getCode()).isEqualTo(request.getCode());
        assertThat(argument.getGui()).isEqualTo(expectedGui);
    }

    @Test
    void shouldUpdateFragment() throws Exception {
        GuiFragment fragment = FragmentMockHelper.mockGuiFragment();
        GuiFragmentDto fragmentDto = FragmentMockHelper.mockGuiFragmentDto(fragment, langManager);

        Mockito.lenient().when(guiFragmentManager.getGuiFragment(anyString())).thenReturn(fragment);
        when(this.dtoBuilder.convert(any(GuiFragment.class))).thenReturn(fragmentDto);

        // Given
        String expectedGui = "<#assign wp=JspTaglibs[ \"/aps-core\"]>\n"
                + "<script nonce=\"<@wp.cspNonce />\">my_js_script</script>";
        GuiFragmentRequestBody request = validFragmentRequest();

        // When
        guiFragmentService.addGuiFragment(request);

        // Then
        ArgumentCaptor<GuiFragment> argumentCaptor = ArgumentCaptor.forClass(GuiFragment.class);
        verify(guiFragmentManager).addGuiFragment(argumentCaptor.capture());
        GuiFragment argument = argumentCaptor.getValue();
        assertThat(argument.getCode()).isEqualTo(request.getCode());
        assertThat(argument.getGui()).isEqualTo(expectedGui);
    }

    @Test
    void shouldNotUpdateGuiNonce() throws Exception {
        GuiFragment fragment = FragmentMockHelper.mockGuiFragment();
        GuiFragmentDto fragmentDto = FragmentMockHelper.mockGuiFragmentDto(fragment, langManager);

        Mockito.lenient().when(guiFragmentManager.getGuiFragment(anyString())).thenReturn(fragment);
        when(this.dtoBuilder.convert(any(GuiFragment.class))).thenReturn(fragmentDto);

        // Given
        String expectedGui = "<#assign wp=JspTaglibs[ \"/aps-core\"]>\n"
                + "<script nonce=\"<@wp.cspNonce />\">my_js_script</script>";
        GuiFragmentRequestBody request = validFragmentRequest();
        request.setGuiCode(expectedGui);

        // When
        guiFragmentService.addGuiFragment(request);

        // Then
        ArgumentCaptor<GuiFragment> argumentCaptor = ArgumentCaptor.forClass(GuiFragment.class);
        verify(guiFragmentManager).addGuiFragment(argumentCaptor.capture());
        GuiFragment argument = argumentCaptor.getValue();
        assertThat(argument.getCode()).isEqualTo(request.getCode());
        assertThat(argument.getGui()).isEqualTo(expectedGui);
    }

    @Test
    void getFragmentUsageForNonExistingCodeShouldReturnZero() {
        int componentUsage = guiFragmentService.getComponentUsage("non_existing");
        Assertions.assertEquals(0, componentUsage);
    }
    
    @Test
    void shouldFindComponentDtoWithWidgetRef() throws Exception {
        this.shouldFindComponentDto(false);
    }
    
    @Test
    void shouldFindComponentDtoWithoutWidgetRef() throws Exception {
        this.shouldFindComponentDto(true);
    }
    
    private void shouldFindComponentDto(boolean nullWidgetRef) throws Exception {
        GuiFragment fragment = new GuiFragment();
        fragment.setCode("test_dto");
        fragment.setGui("<h1>test</h1>");
        if (!nullWidgetRef) {
            fragment.setWidgetTypeCode("ref_widget_code");
        }
        GuiFragmentDtoBuilder builder = new GuiFragmentDtoBuilder();
        builder.setLangManager(langManager);
        Lang defaultLang = new Lang();
        defaultLang.setCode("en");
        Mockito.lenient().when(this.langManager.getDefaultLang()).thenReturn(defaultLang);
        builder.setWidgetTypeManager(widgetTypeManager);
        if (!nullWidgetRef) {
            WidgetType widgetType = new WidgetType();
            widgetType.setCode("ref_widget_code");
            ApsProperties titles = new ApsProperties();
            titles.putAll(Map.of("it", "titolo", "en", "title"));
            widgetType.setTitles(titles);
            Mockito.lenient().when(this.widgetTypeManager.getWidgetType("ref_widget_code")).thenReturn(widgetType);
        }
        ListableBeanFactory factory = Mockito.mock(ListableBeanFactory.class);
        Mockito.when(factory.getBeanNamesForType(GuiFragmentUtilizer.class)).thenReturn(new String[0]);
        builder.setBeanFactory(factory);
        guiFragmentService.setDtoBuilder(builder);
        Mockito.when(guiFragmentManager.getGuiFragment("test_dto")).thenReturn(fragment);
        IComponentDto dto = this.guiFragmentService.getComponentDto("test_dto");
        assertThat(dto).isNotNull()
                .isInstanceOf(GuiFragmentDto.class);
        if (nullWidgetRef) {
            Assertions.assertNull(((GuiFragmentDto) dto).getWidgetType());
        } else {
            Assertions.assertNotNull(((GuiFragmentDto) dto).getWidgetType());
        }
    }
    
    @Test
    void getFragmentUsageTest() throws Exception {
        this.getFragmentUsageTest(false);
    }
    
    @Test
    void getFragmentUsageTestWithNullRelatedWidget() throws Exception {
        this.getFragmentUsageTest(true);
    }
    
    private void getFragmentUsageTest(boolean nullWidgetRef) throws Exception {
        Lang lang = new Lang();
        lang.setCode("IT");
        when(langManager.getDefaultLang()).thenReturn(lang);
        GuiFragment fragment = FragmentMockHelper.mockGuiFragment();
        GuiFragmentDto fragmentDto = FragmentMockHelper.mockGuiFragmentDto(fragment, langManager);
        if (nullWidgetRef) {
            fragment.setWidgetTypeCode(null);
            fragmentDto.setWidgetType(null);
        }
        when(guiFragmentManager.getGuiFragment(anyString())).thenReturn(fragment);
        when(this.dtoBuilder.convert(any(GuiFragment.class))).thenReturn(fragmentDto);
        PagedMetadata<ComponentUsageEntity> componentUsageDetails = guiFragmentService.getComponentUsageDetails(fragment.getCode(), new PageSearchRequest(PageMockHelper.PAGE_CODE));
        assertUsageDetails(componentUsageDetails, nullWidgetRef);
    }
    
    public static void assertUsageDetails(PagedMetadata<ComponentUsageEntity> usageDetails, boolean nullWidgetRef) {
        assertEquals(1, usageDetails.getPage());
        List<ComponentUsageEntity> expectedUsageEntityList = new ArrayList<>(Arrays.asList(
                new ComponentUsageEntity(ComponentUsageEntity.TYPE_FRAGMENT, FragmentMockHelper.FRAGMENT_REF_1_CODE),
                new ComponentUsageEntity(ComponentUsageEntity.TYPE_FRAGMENT, FragmentMockHelper.FRAGMENT_REF_2_CODE),
                new ComponentUsageEntity(ComponentUsageEntity.TYPE_PAGE_TEMPLATE, PageMockHelper.PAGE_MODEL_REF_CODE_1),
                new ComponentUsageEntity(ComponentUsageEntity.TYPE_PAGE_TEMPLATE, PageMockHelper.PAGE_MODEL_REF_CODE_2)));
        if (!nullWidgetRef) {
            expectedUsageEntityList.add(new ComponentUsageEntity(ComponentUsageEntity.TYPE_WIDGET, WidgetMockHelper.WIDGET_1_CODE));
        }
        Assertions.assertEquals(expectedUsageEntityList.size(), usageDetails.getBody().size());
        Assertions.assertEquals(expectedUsageEntityList.size(), usageDetails.getTotalItems());
        IntStream.range(0, usageDetails.getBody().size())
                .forEach(i -> ComponentUsageEntityAssertionHelper.assertComponentUsageEntity(expectedUsageEntityList.get(i), usageDetails.getBody().get(i)));
    }
    
}
