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

/**
 * Public API for Git-based version evaluation.
 *
 * <p>Use
 * {@link VersionEvaluator#forRepository(org.eclipse.jgit.lib.Repository)}
 * to obtain a version evaluator for a Git repository. Configure tag filtering
 * via {@link TagFilter}, version generation via {@link TagProcessor}, and
 * file selection via the {@code matching} and {@code subDirectory} methods on
 * {@link VersionEvaluator}.
 *
 * <p>Implementations are loaded through {@link VersionEvaluatorProvider} using
 * the {@link java.util.ServiceLoader} mechanism.
 */
package org.jdrupes.gitversioning.api;