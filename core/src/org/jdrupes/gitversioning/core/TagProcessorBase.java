/*
 * JDrupes GitVersioning
 * Copyright (C) 2025 Michael N. Lipp
 * 
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package org.jdrupes.gitversioning.core;

import java.util.logging.Logger;
import org.jdrupes.gitversioning.api.TagProcessor;

/**
 * A base class for {@link TagProcessor}s.
 */
public abstract class TagProcessorBase implements TagProcessor {

    /** The logger. */
    protected final Logger log = Logger.getLogger(getClass().getName());

    /**
     * Initializes a new tag processor base.
     */
    protected TagProcessorBase() {
        // Make javadoc happy.
    }

}
