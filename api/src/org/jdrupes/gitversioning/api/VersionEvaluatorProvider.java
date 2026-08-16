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

package org.jdrupes.gitversioning.api;

import java.util.ServiceLoader;
import org.eclipse.jgit.lib.Repository;

/**
 * SPI for version evaluator implementations.
 *
 * <p>Implementations are discovered via the {@link ServiceLoader}
 * mechanism. Register the provider in
 * {@code META-INF/services/org.jdrupes.gitversioning.api.VersionEvaluatorProvider}.
 */
public interface VersionEvaluatorProvider extends VersionEvaluator {

    /**
     * Sets the repository to use.
     *
     * @param repository the Git repository
     * @return this provider for chaining
     */
    VersionEvaluatorProvider repository(Repository repository);

}
