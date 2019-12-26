/*******************************************************************************
 * Copyright (c) 2019 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Red Hat Inc. - initial API and implementation
 *******************************************************************************/
package org.eclipse.jdt.ls.core.internal;

import java.io.File;
import java.util.Hashtable;
import java.util.Map;
import java.util.Objects;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.internal.compiler.impl.CompilerOptions;
import org.eclipse.jdt.internal.launching.StandardVMType;
import org.eclipse.jdt.launching.AbstractVMInstall;
import org.eclipse.jdt.launching.IVMInstall;
import org.eclipse.jdt.launching.IVMInstallChangedListener;
import org.eclipse.jdt.launching.IVMInstallType;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.launching.PropertyChangeEvent;
import org.eclipse.jdt.launching.VMStandin;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;

/**
 * Configures and listens to JVM changes.
 *
 * @author Fred Bricon
 *
 */
public class JVMConfigurator implements IVMInstallChangedListener {

	public static boolean configureDefaultVM(Preferences preferences) throws CoreException {
		if (preferences == null) {
			return false;
		}
		String javaHome = preferences.getJavaHome();
		if (javaHome != null) {
			File jvmHome = new File(javaHome);
			if (jvmHome.isDirectory()) {
				IVMInstall defaultVM = JavaRuntime.getDefaultVMInstall();
				File location = (defaultVM != null) ? defaultVM.getInstallLocation() : null;
				if (location == null || !location.equals(jvmHome)) {
					IVMInstall vm = findVM(jvmHome);
					if (vm == null) {
						IVMInstallType installType = JavaRuntime.getVMInstallType(StandardVMType.ID_STANDARD_VM_TYPE);
						long unique = System.currentTimeMillis();
						while (installType.findVMInstall(String.valueOf(unique)) != null) {
							unique++;
						}
						String vmId = String.valueOf(unique);
						VMStandin vmStandin = new VMStandin(installType, vmId);
						String name = StringUtils.defaultIfBlank(jvmHome.getName(), "JRE");
						vmStandin.setName(name);
						vmStandin.setInstallLocation(jvmHome);
						vm = vmStandin.convertToRealVM();
					}
					JavaRuntime.setDefaultVMInstall(vm, new NullProgressMonitor());
					JDTUtils.setCompatibleVMs(vm.getId());
					return true;
				}
			}
		}
		return false;
	}

	private static IVMInstall findVM(File jvmHome) {
		IVMInstallType[] types = JavaRuntime.getVMInstallTypes();
		for (IVMInstallType type : types) {
			IVMInstall[] installs = type.getVMInstalls();
			for (IVMInstall install : installs) {
				if (jvmHome.equals(install.getInstallLocation())) {
					return install;
				}
			}
		}
		return null;
	}

	@Override
	public void defaultVMInstallChanged(IVMInstall previous, IVMInstall current) {
		if (Objects.equals(previous, current)) {
			return;
		}

		//Reset global compliance settings
		Hashtable<String, String> options = JavaCore.getOptions();
		AbstractVMInstall jvm = (AbstractVMInstall) current;
		long jdkLevel = CompilerOptions.versionToJdkLevel(jvm.getJavaVersion());
		String compliance = CompilerOptions.versionFromJdkLevel(jdkLevel);
		JavaCore.setComplianceOptions(compliance, options);
		JavaCore.setOptions(options);

		IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
		for (IProject project : projects) {
			if (!ProjectUtils.isVisibleProject(project) && ProjectUtils.isJavaProject(project)) {
				IJavaProject javaProject = JavaCore.create(project);
				configureJVMSettings(javaProject, current);
			}
		}
	}

	@Override
	public void vmChanged(PropertyChangeEvent event) {
	}

	@Override
	public void vmAdded(IVMInstall vm) {
	}

	@Override
	public void vmRemoved(IVMInstall vm) {
	}

	public static void configureJVMSettings(IJavaProject javaProject) {
		configureJVMSettings(javaProject, JavaRuntime.getDefaultVMInstall());
	}

	public static void configureJVMSettings(IJavaProject javaProject, IVMInstall vmInstall) {
		if (javaProject == null) {
			return;
		}
		String version = "";
		if (vmInstall instanceof AbstractVMInstall) {
			AbstractVMInstall jvm = (AbstractVMInstall) vmInstall;
			version = jvm.getJavaVersion();
			long jdkLevel = CompilerOptions.versionToJdkLevel(jvm.getJavaVersion());
			String compliance = CompilerOptions.versionFromJdkLevel(jdkLevel);
			Map<String, String> options = javaProject.getOptions(false);
			JavaCore.setComplianceOptions(compliance, options);
		}
		;
		if (JavaCore.compareJavaVersions(version, JavaCore.latestSupportedJavaVersion()) >= 0) {
			//Enable Java preview features for the latest JDK release by default and stfu about it
			javaProject.setOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.ENABLED);
			javaProject.setOption(JavaCore.COMPILER_PB_REPORT_PREVIEW_FEATURES, JavaCore.IGNORE);
		} else {
			javaProject.setOption(JavaCore.COMPILER_PB_ENABLE_PREVIEW_FEATURES, JavaCore.DISABLED);
		}
	}

}
