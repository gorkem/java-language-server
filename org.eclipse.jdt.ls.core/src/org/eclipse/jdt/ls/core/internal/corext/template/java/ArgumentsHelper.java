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

/**
 * Helper class to handle method arguments
 *
 * @author Fred Bricon
 *
 */
public class ArgumentsHelper {

	private ArgumentsHelper() {
		//No public instantiation
	}

	/**
	 * Formats an array of arguments as:
	 *
	 * <pre>
	 * "arg1="+arg1+", arg2="+arg2+...+", argN="+argN
	 * </pre>
	 *
	 * @param args
	 *            the arguments to format
	 * @return a formatted string enumerating arguments, or an empty string.
	 */
	public static String format(String... args) {
		if (args != null && args.length > 0 && args[0] != null) {
			StringBuilder builder = new StringBuilder("\"");
			for (int i = 0; i < args.length; i++) {
				String arg = args[i];
				if (arg != null) {
					if (i > 0) {
						builder.append(" + \", ");
					}
					builder.append(arg).append(" = \" + ").append(arg);
				}
			}
			return builder.toString();
		}
		return "";
	}

}
