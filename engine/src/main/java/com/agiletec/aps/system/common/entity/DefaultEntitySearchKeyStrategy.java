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
package com.agiletec.aps.system.common.entity;

import java.util.ArrayList;
import java.util.List;

/**
 * The shipped {@link IEntitySearchKeyStrategy}: the encoding every entity manager uses unless it is given
 * another one.
 *
 * <p><b>Which</b> attributes get a nested key is not decided here - each attribute declares that for
 * itself through {@code isNestedSearchSupported()}, the boolean-like family today. This class decides only
 * what the key of a given path looks like and how long it may be.</p>
 *
 * <p><b>The key</b> joins the path segments with {@link #KEY_SEPARATOR} and escapes that separator inside
 * each segment by doubling it, so composite {@code a_b} + child {@code c} becomes {@code a__b_c} while
 * composite {@code a} + child {@code b_c} becomes {@code a_b__c}. Doubling alone is <b>not</b> sufficient
 * for injectivity - see {@link #isEncodableSegment(String)}, which excludes the one case it cannot
 * separate; the two together make the encoding unambiguous. A single-segment path - a top-level
 * attribute - is returned <b>raw</b>: that is what the search tables have always stored, and escaping it
 * would change the key of every existing attribute whose name contains {@code '_'}. The escaping stays
 * inside {@code [a-zA-Z0-9_]}, so it needs no widening of Solr's field-name charset nor of the
 * injection allowlist in the searcher.</p>
 *
 * <p><b>The length bound</b> defaults to {@link #DEFAULT_MAX_KEY_LENGTH}, matching the {@code attrname}
 * column of the search tables the platform ships. A manager whose table is narrower - or wider -
 * declares its own through {@link #DefaultEntitySearchKeyStrategy(int)} rather than relying on a comment.</p>
 *
 * @author Entando
 */
public class DefaultEntitySearchKeyStrategy implements IEntitySearchKeyStrategy {

    /** Separator used to render a nested attribute's hierarchy for humans (never occurs in a name). */
    public static final String LABEL_SEPARATOR = " > ";

    /** Separator joining the path segments into the machine key (DB {@code attrname} / Solr field). */
    public static final String KEY_SEPARATOR = "_";

    /**
     * How {@link #KEY_SEPARATOR} is represented <b>inside</b> a path segment: doubled.
     *
     * <p>This alone does not make the key reversible. A run of {@code n} separators can be read as
     * {@code k} literals ending a segment, the separator, then {@code m} literals starting the next, for
     * any {@code 2k + 1 + 2m = n} - so {@code a_} + {@code b} and {@code a} + {@code _b} would both give
     * {@code a___b}. Reversibility comes from doubling <b>plus</b>
     * {@link #isEncodableSegment(String)}, which forbids a separator against a segment boundary: every
     * run is then a single separator or an even number of literals.</p>
     */
    public static final String ESCAPED_KEY_SEPARATOR = "__";

    /**
     * The {@code attrname} width of every search table the platform ships: {@code contentsearch} and
     * {@code workcontentsearch} (cms-plugin), {@code authuserprofilesearch} (engine) and
     * {@code jpwebdynamicform_search} (webdynamicform-plugin).
     */
    public static final int DEFAULT_MAX_KEY_LENGTH = 255;

    /** The strategy used by any manager that has not been given one of its own. */
    public static final DefaultEntitySearchKeyStrategy INSTANCE = new DefaultEntitySearchKeyStrategy();

    private final int maxKeyLength;

    public DefaultEntitySearchKeyStrategy() {
        this(DEFAULT_MAX_KEY_LENGTH);
    }

    /**
     * @param maxKeyLength the width of this manager's {@code attrname} column.
     */
    public DefaultEntitySearchKeyStrategy(int maxKeyLength) {
        this.maxKeyLength = maxKeyLength;
    }

    @Override
    public int getMaxKeyLength() {
        return this.maxKeyLength;
    }

    /**
     * Rejects a segment that begins or ends with {@link #KEY_SEPARATOR}.
     *
     * <p>Doubling the separator inside a segment is <b>not</b> enough to make the encoding injective on
     * its own: a run of {@code n} separators can be read as {@code k} literals ending one segment, the
     * separator, then {@code m} literals starting the next, for any {@code 2k + 1 + 2m = n}. Concretely,
     * composite {@code a_} + child {@code b} and composite {@code a} + child {@code _b} both flatten to
     * {@code a___b}. Excluding separators at a segment boundary removes exactly that freedom: every run
     * is then either a single separator or an even number of literals, and the key is unambiguous.</p>
     *
     * <p>An underscore <i>inside</i> a name is unaffected - {@code press_kit} is a perfectly good
     * segment, and {@code press_kit} + {@code published} encodes as {@code press__kit_published}.</p>
     */
    @Override
    public boolean isEncodableSegment(String segment) {
        return null == segment
                || !(segment.startsWith(KEY_SEPARATOR) || segment.endsWith(KEY_SEPARATOR));
    }

    @Override
    public String buildKey(List<String> segments) {
        if (null == segments) {
            return null;
        }
        if (segments.size() < 2) {
            // a top-level attribute keeps its raw name: unchanged from before nested keys existed
            return String.join(KEY_SEPARATOR, segments);
        }
        List<String> escaped = new ArrayList<>(segments.size());
        for (String segment : segments) {
            escaped.add(null == segment ? null : segment.replace(KEY_SEPARATOR, ESCAPED_KEY_SEPARATOR));
        }
        return String.join(KEY_SEPARATOR, escaped);
    }

    @Override
    public String buildLabel(List<String> segments) {
        return (null == segments) ? null : String.join(LABEL_SEPARATOR, segments);
    }

}
