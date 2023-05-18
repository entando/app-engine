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
package org.entando.entando.plugins.jpseo.web.page.validator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.entando.entando.plugins.jpseo.aps.system.services.mapping.ISeoMappingManager;
import org.entando.entando.plugins.jpseo.web.page.model.SeoDataByLang;
import org.entando.entando.plugins.jpseo.web.page.model.SeoMetaTag;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.validation.BindingResult;
import org.springframework.validation.DataBinder;

@ExtendWith(MockitoExtension.class)
class SeoPageValidatorTest {

    @Mock
    private ISeoMappingManager seoMappingManager;

    @InjectMocks
    private SeoPageValidator validator;

    @BeforeEach
    public void setUp() throws Exception {
    }

    @Test
    void shouldValidateKeysDuplicatedWorkCorreclty() throws Exception {
        SeoDataByLang seo = new SeoDataByLang("mykeywords",
                "my description",
                "my frindly code",
                new ArrayList<>(),
                false, false,false);
        Map<String, SeoDataByLang> seoDataByLang = Map.of("it", seo);

        BindingResult errors =  (new DataBinder(new Object())).getBindingResult();
        validator.validateKeysDuplicated(seoDataByLang, errors);
        Assertions.assertFalse(errors.hasErrors());

        seo = new SeoDataByLang("mykeywords",
                "my description",
                "my frindly code",
                Collections.singletonList(new SeoMetaTag("keywords","","",false)),
                false, false,false);
        seoDataByLang = Map.of("it", seo);

        errors =  (new DataBinder(new Object())).getBindingResult();
        validator.validateKeysDuplicated(seoDataByLang, errors);
        Assertions.assertTrue(errors.hasErrors());

        seo = new SeoDataByLang("mykeywords",
                "my description",
                "my frindly code",
                Collections.singletonList(new SeoMetaTag("description","","",false)),
                false, false,false);
        seoDataByLang = Map.of("it", seo);

        errors =  (new DataBinder(new Object())).getBindingResult();
        validator.validateKeysDuplicated(seoDataByLang, errors);
        Assertions.assertTrue(errors.hasErrors());

        seo = new SeoDataByLang("mykeywords",
                "my description",
                "my frindly code",
                Arrays.asList(
                        new SeoMetaTag("testKey1","","",false),
                        new SeoMetaTag("testKey2","","",false),
                        new SeoMetaTag("testKey2","","",false)
                ),
                false, false,false);
        seoDataByLang = Map.of("it", seo);

        errors =  (new DataBinder(new Object())).getBindingResult();
        validator.validateKeysDuplicated(seoDataByLang, errors);
        Assertions.assertTrue(errors.hasErrors());

        seo = new SeoDataByLang("mykeywords",
                "my description",
                "my frindly code",
                Arrays.asList(
                        new SeoMetaTag("testKey1","","",false),
                        new SeoMetaTag("testKey2","","",false),
                        new SeoMetaTag("testKey3","","",false)
                ),
                false, false,false);
        seoDataByLang = Map.of("it", seo);

        errors =  (new DataBinder(new Object())).getBindingResult();
        validator.validateKeysDuplicated(seoDataByLang, errors);
        Assertions.assertFalse(errors.hasErrors());

        seo = new SeoDataByLang("mykeywords",
                "my description",
                "my frindly code",
                Arrays.asList(
                        new SeoMetaTag(null,"",null,false),
                        new SeoMetaTag("testKey2","",null,false)
                ),
                false, false,false);
        seoDataByLang = Map.of("it", seo);

        errors =  (new DataBinder(new Object())).getBindingResult();
        validator.validateKeysDuplicated(seoDataByLang, errors);
        Assertions.assertFalse(errors.hasErrors());

    }

}
