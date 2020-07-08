/*******************************************************************************
 * Copyright (c) 2020 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal.corext.template.java;

import org.eclipse.text.templates.ContextTypeRegistry;

/**
 * A registry for context types used by code snippet templates.
 *
 * @author Fred Bricon
 * @see JavaContextType
 * @see CodeSnippetTemplate
 */
public class JavaContextTypeRegistry extends ContextTypeRegistry {

	public JavaContextTypeRegistry() {
		JavaContextType contextType = new JavaContextType();
		contextType.initializeContextTypeResolvers();
		addContextType(contextType);
	}

}
