/*
 * Copyright 2026-Present Entando Inc. (http://www.entando.com) All rights reserved.
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
package org.entando.entando.aps.system.services.cache;

import java.util.function.UnaryOperator;

/**
 * A cache whose single entries can be read, changed and written back atomically.
 */
public interface IAtomicUpdateCache {

    /**
     * Applies {@code updater} to the value currently stored under {@code key} and stores its result, retrying until
     * the write is applied over the value read.
     *
     * @param key the cache key
     * @param updater receives the stored value, or null when the key is absent; must not mutate its argument and
     * must be free of side effects. Returning the same instance it received writes nothing.
     * @return the value now stored
     */
    <T> T updateAtomically(Object key, UnaryOperator<T> updater);

}
