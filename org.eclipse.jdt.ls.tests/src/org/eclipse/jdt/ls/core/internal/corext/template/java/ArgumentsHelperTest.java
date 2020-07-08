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

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ArgumentsHelperTest {

	@Test
	public void testFormat() {
		String[] args = null;
		assertEquals("", ArgumentsHelper.format(args));
		assertEquals("", ArgumentsHelper.format(new String[] {}));
		assertEquals("", ArgumentsHelper.format(new String[1]));
		args = new String[] { "foo" };
		assertEquals("\"foo = \" + foo", ArgumentsHelper.format(args));
		args = new String[] { "foo", "bar" };
		assertEquals("\"foo = \" + foo + \", bar = \" + bar", ArgumentsHelper.format(args));
		args = new String[] { "foo", "bar", "bla" };
		assertEquals("\"foo = \" + foo + \", bar = \" + bar + \", bla = \" + bla", ArgumentsHelper.format(args));
	}

}
