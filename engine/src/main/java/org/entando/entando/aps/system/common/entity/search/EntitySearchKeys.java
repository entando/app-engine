/*
 * Copyright 2015-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.common.entity.search;

import java.util.ArrayList;
import java.util.List;

/**
 * How an attribute path becomes the {@code attrname} the search tables store, and the label a search form
 * shows for it.
 *
 * <p>The key joins the path segments with {@link #KEY_SEPARATOR}, doubling that separator inside each
 * segment: composite {@code a_b} + child {@code c} becomes {@code a__b_c}, composite {@code a} + child
 * {@code b_c} becomes {@code a_b__c}. Doubling alone is not injective - see
 * {@link #isEncodableSegment(String)}. A top-level attribute's key is its <b>raw</b> name, because that is
 * what the search tables have always stored.</p>
 *
 * <p>How long a key may be is <b>not</b> here: each entity manager owns its own search table, so the
 * column width is a property of that manager - see {@code IEntityManager.getMaxSearchKeyLength()}.
 * Neither is whether an attribute may be path-indexed at all: that is
 * {@code AttributeInterface.isNestedSearchSupported()}.</p>
 */
public final class EntitySearchKeys {

    /** Separator rendering a nested hierarchy for humans; never occurs in a name. */
    public static final String LABEL_SEPARATOR = " > ";

    /** Separator joining path segments into the machine key. */
    public static final String KEY_SEPARATOR = "_";

    /** {@link #KEY_SEPARATOR} doubled, which is how it is represented inside a segment. */
    public static final String ESCAPED_KEY_SEPARATOR = "__";

    /**
     * The {@code attrname} width of every search table the platform ships: {@code contentsearch},
     * {@code workcontentsearch}, {@code authuserprofilesearch}, {@code jpwebdynamicform_search}.
     */
    public static final int DEFAULT_MAX_KEY_LENGTH = 255;

    private EntitySearchKeys() {
        // utility class
    }

    /**
     * Whether {@link #buildKey} can encode this segment unambiguously. Rejects a segment that begins or
     * ends with {@link #KEY_SEPARATOR}: doubling alone leaves a run of separators ambiguous -
     * {@code a_} + {@code b} and {@code a} + {@code _b} both give {@code a___b} - and forbidding one at a
     * boundary removes exactly that freedom. An underscore inside a name is fine: {@code press_kit} +
     * {@code published} encodes as {@code press__kit_published}.
     */
    public static boolean isEncodableSegment(String segment) {
        return null == segment
                || !(segment.startsWith(KEY_SEPARATOR) || segment.endsWith(KEY_SEPARATOR));
    }

    /**
     * The machine key of an attribute path.
     *
     * @param segments the attribute names along the path, from the top-level attribute down.
     */
    public static String buildKey(List<String> segments) {
        if (null == segments) {
            return null;
        }
        if (segments.size() < 2) {
            // a top-level attribute keeps its raw name
            return String.join(KEY_SEPARATOR, segments);
        }
        List<String> escaped = new ArrayList<>(segments.size());
        for (String segment : segments) {
            escaped.add(null == segment ? null : segment.replace(KEY_SEPARATOR, ESCAPED_KEY_SEPARATOR));
        }
        return String.join(KEY_SEPARATOR, escaped);
    }

    /** The path rendered for a search form. Need not be reversible or collision-free. */
    public static String buildLabel(List<String> segments) {
        return (null == segments) ? null : String.join(LABEL_SEPARATOR, segments);
    }

}
