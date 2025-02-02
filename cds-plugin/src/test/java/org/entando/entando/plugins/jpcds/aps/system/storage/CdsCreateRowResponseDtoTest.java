/*
 * Copyright 2024-Present Entando S.r.l. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpcds.aps.system.storage;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

class CdsCreateRowResponseDtoTest {


    @Test
    void shouldMarshallingAndUnmarshallingUseTheCorrectFieldsName()
            throws JsonParseException, IOException {
        ObjectMapper mapper = new ObjectMapper();
        CdsCreateRowResponseDto dtoObject = new CdsCreateRowResponseDto();
        dtoObject.setProtectedFile(true);
        dtoObject.setStatus("OK");
        dtoObject.setFilename("nome_file");
        dtoObject.setPath("/mypath");

        String dtoAsString = mapper.writeValueAsString(dtoObject);

        Assertions.assertThat(dtoAsString)
                .contains("\"status\":\"OK\"")
                .contains("\"is_protected_file\":true")
                .contains("\"filename\":\"nome_file\"")
                .contains("\"path\":\"/mypath\"")
                .contains("\"date\":null");

        String test = "{\"status\":\"OK\",\"filename\":\"nome_file\",\"date\":1234,\"path\":\"/her_path\",\"is_protected_file\":false}";
        CdsCreateRowResponseDto exp = mapper.readValue(test, CdsCreateRowResponseDto.class);
        Assertions.assertThat(exp.getStatus()).isEqualTo("OK");
        Assertions.assertThat(exp.getFilename()).isEqualTo("nome_file");
        Assertions.assertThat(exp.getPath()).isEqualTo("/her_path");
        Assertions.assertThat(exp.getDate()).isEqualTo(1234);
        Assertions.assertThat(exp.isProtectedFile()).isFalse();
    }

    @Test
    void shouldWorkFineWithHashEqualsAndToString() {

        CdsCreateRowResponseDto dtoObject1 = new CdsCreateRowResponseDto();
        dtoObject1.setProtectedFile(true);
        dtoObject1.setStatus("OK");
        dtoObject1.setFilename("nome_file");
        dtoObject1.setPath("/mypath");

        CdsCreateRowResponseDto dtoObject2 = new CdsCreateRowResponseDto();
        dtoObject2.setProtectedFile(true);
        dtoObject2.setStatus("OK");
        dtoObject2.setFilename("nome_file");
        dtoObject2.setPath("/mypath");

        Assertions.assertThat(dtoObject2.toString()).contains("nome_file","/mypath","OK");
        Assertions.assertThat(dtoObject2).hasSameHashCodeAs(dtoObject1);
        Assertions.assertThat(dtoObject2.equals(dtoObject1)).isTrue();
    }

}
