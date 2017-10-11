/*******************************************************************************
 * Copyright (c) 2017 David Gileadi and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     David Gileadi - initial API
 *     Red Hat Inc. - initial implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Status;
import org.eclipse.jdt.core.IClassFile;
import org.eclipse.jdt.core.ToolFactory;
import org.eclipse.jdt.core.util.ClassFileBytesDisassembler;
import org.eclipse.jdt.core.util.ClassFormatException;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;

public class DisassemblerContentProvider implements IDecompiler {

	public static final String DISASSEMBLED_HEADER = " // Failed to get sources. Instead, stub sources have been generated by the disassembler.\n" + " // Implementation of methods is unavailable.\n";
	private static final String LF = "\n";

	@Override
	public void setPreferences(Preferences preferences) {
	}

	@Override
	public String getContent(URI uri, IProgressMonitor monitor) throws CoreException {
		IClassFile classFile = JDTUtils.resolveClassFile(uri);
		if (classFile != null) {
			return getSource(classFile, monitor);
		}
		try {
			return getContent(Files.readAllBytes(Paths.get(uri)), monitor);
		} catch (IOException e) {
			throw new CoreException(new Status(Status.ERROR, "", "Error opening " + uri, e));
		}
	}

	@Override
	public String getSource(IClassFile classFile, IProgressMonitor monitor) throws CoreException {
		return getContent(classFile.getBytes(), monitor);
	}

	private String getContent(byte[] bytes, IProgressMonitor monitor) throws CoreException {
		ClassFileBytesDisassembler disassembler = ToolFactory.createDefaultClassFileBytesDisassembler();
		String disassembledByteCode = null;
		try {
			disassembledByteCode = disassembler.disassemble(bytes, LF, ClassFileBytesDisassembler.WORKING_COPY);
			if (disassembledByteCode != null) {
				disassembledByteCode = DISASSEMBLED_HEADER + disassembledByteCode;
			}
		} catch (ClassFormatException e) {
			throw new CoreException(new Status(Status.ERROR, "", "Error disassembling", e));
		}
		return disassembledByteCode;
	}

}
