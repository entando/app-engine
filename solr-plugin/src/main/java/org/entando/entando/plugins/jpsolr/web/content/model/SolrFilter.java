/*
 * Copyright 2021-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.plugins.jpsolr.web.content.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import org.entando.entando.web.common.model.Filter;

/**
 * @author E.Santoboni
 */
@EqualsAndHashCode(callSuper = true)
public class SolrFilter extends Filter {

    @Getter @Setter private Integer relevancy;
    @Getter @Setter private boolean fullText;
    @Getter @Setter private String searchOption;

    public SolrFilter() {
    }

    public SolrFilter(String attribute, String value) {
        super(attribute, value);
    }

    public SolrFilter(String attribute, String value, String operator) {
        super(attribute, value, operator);
    }

}
