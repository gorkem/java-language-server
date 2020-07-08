/*******************************************************************************
 * Copyright (c) 2016-2019 Red Hat Inc. and others.
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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.Authenticator;
import java.net.InetSocketAddress;
import java.net.PasswordAuthentication;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.Channels;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.internal.net.ProxySelector;
import org.eclipse.core.net.proxy.IProxyData;
import org.eclipse.core.net.proxy.IProxyService;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.core.manipulation.JavaManipulation;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.jdt.internal.core.manipulation.JavaManipulationPlugin;
import org.eclipse.jdt.internal.core.manipulation.MembersOrderPreferenceCacheCommon;
import org.eclipse.jdt.internal.core.search.indexing.IndexManager;
import org.eclipse.jdt.internal.corext.codemanipulation.CodeGenerationSettingsConstants;
import org.eclipse.jdt.ls.core.internal.JavaClientConnection.JavaLanguageClient;
import org.eclipse.jdt.ls.core.internal.contentassist.TypeFilter;
import org.eclipse.jdt.ls.core.internal.corext.template.java.JavaContextTypeRegistry;
import org.eclipse.jdt.ls.core.internal.corext.template.java.JavaLanguageServerTemplateStore;
import org.eclipse.jdt.ls.core.internal.handlers.JDTLanguageServer;
import org.eclipse.jdt.ls.core.internal.managers.ContentProviderManager;
import org.eclipse.jdt.ls.core.internal.managers.DigestStore;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager;
import org.eclipse.jdt.ls.core.internal.managers.StandardProjectsManager;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.jdt.ls.core.internal.preferences.StandardPreferenceManager;
import org.eclipse.jdt.ls.core.internal.syntaxserver.SyntaxLanguageServer;
import org.eclipse.jdt.ls.core.internal.syntaxserver.SyntaxProjectsManager;
import org.eclipse.lsp4j.jsonrpc.Launcher;
import org.eclipse.lsp4j.jsonrpc.MessageConsumer;
import org.eclipse.text.templates.ContextTypeRegistry;
import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.util.tracker.ServiceTracker;

import com.google.common.base.Throwables;

public class JavaLanguageServerPlugin extends Plugin {

	private static final String JDT_UI_PLUGIN = "org.eclipse.jdt.ui";
	public static final String MANUAL = "Manual";
	public static final String HTTP_NON_PROXY_HOSTS = "http.nonProxyHosts";
	public static final String HTTPS_NON_PROXY_HOSTS = "https.nonProxyHosts";
	public static final String HTTPS_PROXY_PASSWORD = "https.proxyPassword";
	public static final String HTTPS_PROXY_PORT = "https.proxyPort";
	public static final String HTTPS_PROXY_HOST = "https.proxyHost";
	public static final String HTTP_PROXY_PASSWORD = "http.proxyPassword";
	public static final String HTTP_PROXY_PORT = "http.proxyPort";
	public static final String HTTP_PROXY_HOST = "http.proxyHost";
	public static final String HTTPS_PROXY_USER = "https.proxyUser";
	public static final String HTTP_PROXY_USER = "http.proxyUser";

	/**
	 * Source string send to clients for messages such as diagnostics.
	 **/
	public static final String SERVER_SOURCE_ID = "Java";

	/**
	 * Use IConstants.PLUGIN_ID
	 */
	@Deprecated
	public static final String PLUGIN_ID = IConstants.PLUGIN_ID;

	public static final String DEFAULT_MEMBER_SORT_ORDER = "T,SF,SI,SM,F,I,C,M"; //$NON-NLS-1$

	private static JavaLanguageServerPlugin pluginInstance;
	private static BundleContext context;
	private ServiceTracker<IProxyService, IProxyService> proxyServiceTracker = null;
	private static InputStream in;
	private static PrintStream out;
	private static PrintStream err;
	
	private LanguageServer languageServer;
	private ProjectsManager projectsManager;
	private DigestStore digestStore;
	private ContentProviderManager contentProviderManager;

	private BaseJDTLanguageServer protocol;

	private PreferenceManager preferenceManager;

	private TypeFilter typeFilter;

	private ContextTypeRegistry fContextTypeRegistry;
	private JavaLanguageServerTemplateStore fTemplateStore;

	private DiagnosticsState nonProjectDiagnosticsState;

	public static LanguageServer getLanguageServer() {
		return pluginInstance == null ? null : pluginInstance.languageServer;
	}

	public static BundleContext getBundleContext() {
		return JavaLanguageServerPlugin.context;
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext bundleContext) throws Exception {
		super.start(bundleContext);
		try {
			Platform.getBundle(ResourcesPlugin.PI_RESOURCES).start(Bundle.START_TRANSIENT);
			IWorkspace workspace = ResourcesPlugin.getWorkspace();
			IWorkspaceDescription description = workspace.getDescription();
			description.setAutoBuilding(false);
			workspace.setDescription(description);
		} catch (BundleException e) {
			logException(e.getMessage(), e);
		}
		try {
			redirectStandardStreams();
		} catch (FileNotFoundException e) {
			logException(e.getMessage(), e);
		}
		JavaLanguageServerPlugin.context = bundleContext;
		JavaLanguageServerPlugin.pluginInstance = this;
		setPreferenceNodeId();

		if (JDTEnvironmentUtils.isSyntaxServer()) {
			disableServices();
			preferenceManager = new PreferenceManager();
			projectsManager = new SyntaxProjectsManager(preferenceManager);
		} else {
			preferenceManager = new StandardPreferenceManager();
			projectsManager = new StandardProjectsManager(preferenceManager);
		}

		digestStore = new DigestStore(getStateLocation().toFile());
		try {
			ResourcesPlugin.getWorkspace().addSaveParticipant(IConstants.PLUGIN_ID, projectsManager);
		} catch (CoreException e) {
			logException(e.getMessage(), e);
		}
		contentProviderManager = new ContentProviderManager(preferenceManager);
		nonProjectDiagnosticsState = new DiagnosticsState();
		logInfo(getClass() + " is started");
		configureProxy();
	}

	private void disableServices() {
		try {
			ProjectsManager.setAutoBuilding(false);
		} catch (CoreException e1) {
			JavaLanguageServerPlugin.logException(e1);
		}

		IndexManager indexManager = JavaModelManager.getIndexManager();
		if (indexManager != null) {
			indexManager.shutdown();
		}
	}

	private void setPreferenceNodeId() {
		// a hack to ensure unit tests work in Eclipse and CLI builds on Mac
		Bundle bundle = Platform.getBundle(JDT_UI_PLUGIN);
		if (bundle != null && bundle.getState() != Bundle.ACTIVE) {
			// start the org.eclipse.jdt.ui plugin if it exists
			try {
				bundle.start();
			} catch (BundleException e) {
				JavaLanguageServerPlugin.logException(e.getMessage(), e);
			}
		}
		// if preferenceNodeId is already set, we have to nullify it
		// https://git.eclipse.org/c/jdt/eclipse.jdt.ui.git/commit/?id=4c731bc9cc7e1cfd2e67746171aede8d7719e9c1
		JavaManipulation.setPreferenceNodeId(null);
		// Set the ID to use for preference lookups
		JavaManipulation.setPreferenceNodeId(IConstants.PLUGIN_ID);

		IEclipsePreferences fDefaultPreferenceStore = DefaultScope.INSTANCE.getNode(JavaManipulation.getPreferenceNodeId());
		fDefaultPreferenceStore.put(MembersOrderPreferenceCacheCommon.APPEARANCE_MEMBER_SORT_ORDER, DEFAULT_MEMBER_SORT_ORDER);
		fDefaultPreferenceStore.put(CodeGenerationSettingsConstants.CODEGEN_USE_OVERRIDE_ANNOTATION, Boolean.TRUE.toString());

		// initialize MembersOrderPreferenceCacheCommon used by BodyDeclarationRewrite
		MembersOrderPreferenceCacheCommon preferenceCache = JavaManipulationPlugin.getDefault().getMembersOrderPreferenceCacheCommon();
		preferenceCache.install();
	}

	private void configureProxy() {
		// It seems there is no way to set a proxy provider type (manual, native or
		// direct) without the Eclipse UI.
		// The org.eclipse.core.net plugin removes the http., https. system properties
		// when setting its preferences and a proxy provider isn't manual.
		// We save these parameters and set them after starting the
		// org.eclipse.core.net plugin.
		String httpHost = System.getProperty(HTTP_PROXY_HOST);
		String httpPort = System.getProperty(HTTP_PROXY_PORT);
		String httpUser = System.getProperty(HTTP_PROXY_USER);
		String httpPassword = System.getProperty(HTTP_PROXY_PASSWORD);
		String httpsHost = System.getProperty(HTTPS_PROXY_HOST);
		String httpsPort = System.getProperty(HTTPS_PROXY_PORT);
		String httpsUser = System.getProperty(HTTPS_PROXY_USER);
		String httpsPassword = System.getProperty(HTTPS_PROXY_PASSWORD);
		String httpsNonProxyHosts = System.getProperty(HTTPS_NON_PROXY_HOSTS);
		String httpNonProxyHosts = System.getProperty(HTTP_NON_PROXY_HOSTS);
		if (StringUtils.isNotBlank(httpUser) || StringUtils.isNotBlank(httpsUser)) {
			try {
				Platform.getBundle("org.eclipse.core.net").start(Bundle.START_TRANSIENT);
			} catch (BundleException e) {
				logException(e.getMessage(), e);
			}
			if (StringUtils.isNotBlank(httpUser) && StringUtils.isNotBlank(httpPassword)) {
				Authenticator.setDefault(new Authenticator() {
					@Override
					public PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(httpUser, httpPassword.toCharArray());
					}
				});
			}
			IProxyService proxyService = getProxyService();
			if (proxyService != null) {
				ProxySelector.setActiveProvider(MANUAL);
				IProxyData[] proxies = proxyService.getProxyData();
				for (IProxyData proxy : proxies) {
					if ("HTTP".equals(proxy.getType())) {
						proxy.setHost(httpHost);
						proxy.setPort(httpPort == null ? -1 : Integer.valueOf(httpPort));
						proxy.setPassword(httpPassword);
						proxy.setUserid(httpUser);
					}
					if ("HTTPS".equals(proxy.getType())) {
						proxy.setHost(httpsHost);
						proxy.setPort(httpsPort == null ? -1 : Integer.valueOf(httpsPort));
						proxy.setPassword(httpsPassword);
						proxy.setUserid(httpsUser);
					}
				}
				try {
					proxyService.setProxyData(proxies);
					if (httpHost != null) {
						System.setProperty(HTTP_PROXY_HOST, httpHost);
					}
					if (httpPort != null) {
						System.setProperty(HTTP_PROXY_PORT, httpPort);
					}
					if (httpUser != null) {
						System.setProperty(HTTP_PROXY_USER, httpUser);
					}
					if (httpPassword != null) {
						System.setProperty(HTTP_PROXY_PASSWORD, httpPassword);
					}
					if (httpsHost != null) {
						System.setProperty(HTTPS_PROXY_HOST, httpsHost);
					}
					if (httpsPort != null) {
						System.setProperty(HTTPS_PROXY_PORT, httpsPort);
					}
					if (httpsUser != null) {
						System.setProperty(HTTPS_PROXY_USER, httpsUser);
					}
					if (httpsPassword != null) {
						System.setProperty(HTTPS_PROXY_PASSWORD, httpsPassword);
					}
					if (httpsNonProxyHosts != null) {
						System.setProperty(HTTPS_NON_PROXY_HOSTS, httpsNonProxyHosts);
					}
					if (httpNonProxyHosts != null) {
						System.setProperty(HTTP_NON_PROXY_HOSTS, httpNonProxyHosts);
					}
				} catch (CoreException e) {
					logException(e.getMessage(), e);
				}
			}
		}
	}

	public IProxyService getProxyService() {
		try {
			if (proxyServiceTracker == null) {
				proxyServiceTracker = new ServiceTracker<>(context, IProxyService.class.getName(), null);
				proxyServiceTracker.open();
			}
			return proxyServiceTracker.getService();
		} catch (Exception e) {
			logException(e.getMessage(), e);
		}
		return null;
	}

	private void startConnection() throws IOException {
		Launcher<JavaLanguageClient> launcher;
		ExecutorService executorService = Executors.newCachedThreadPool();
		if (JDTEnvironmentUtils.isSyntaxServer()) {
			protocol = new SyntaxLanguageServer(contentProviderManager, projectsManager, preferenceManager);
		} else {
			protocol = new JDTLanguageServer(projectsManager, preferenceManager);
		}
		if (JDTEnvironmentUtils.inSocketStreamDebugMode()) {
			String host = JDTEnvironmentUtils.getClientHost();
			Integer port = JDTEnvironmentUtils.getClientPort();
			InetSocketAddress inetSocketAddress = new InetSocketAddress(host, port);
			AsynchronousServerSocketChannel serverSocket = AsynchronousServerSocketChannel.open().bind(inetSocketAddress);
			try {
				AsynchronousSocketChannel socketChannel = serverSocket.accept().get();
				InputStream in = Channels.newInputStream(socketChannel);
				OutputStream out = Channels.newOutputStream(socketChannel);
				Function<MessageConsumer, MessageConsumer> messageConsumer = it -> it;
				launcher = Launcher.createIoLauncher(protocol, JavaLanguageClient.class, in, out, executorService, messageConsumer);
			} catch (InterruptedException | ExecutionException e) {
				throw new RuntimeException("Error when opening a socket channel at " + host + ":" + port + ".", e);
			}
		} else {
			ConnectionStreamFactory connectionFactory = new ConnectionStreamFactory();
			InputStream in = connectionFactory.getInputStream();
			OutputStream out = connectionFactory.getOutputStream();
			Function<MessageConsumer, MessageConsumer> wrapper;
			if ("false".equals(System.getProperty("watchParentProcess"))) {
				wrapper = it -> it;
			} else {
				wrapper = new ParentProcessWatcher(this.languageServer);
			}
			launcher = Launcher.createLauncher(protocol, JavaLanguageClient.class, in, out, executorService, wrapper);
		}
		protocol.connectClient(launcher.getRemoteProxy());
		launcher.startListening();
	}

	/*
	 * (non-Javadoc)
	 * @see org.osgi.framework.BundleActivator#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext bundleContext) throws Exception {
		logInfo(getClass() + " is stopping:");
		JavaLanguageServerPlugin.pluginInstance = null;
		JavaLanguageServerPlugin.context = null;
		ResourcesPlugin.getWorkspace().removeSaveParticipant(IConstants.PLUGIN_ID);
		projectsManager = null;
		contentProviderManager = null;
		languageServer = null;
	}

	public WorkingCopyOwner getWorkingCopyOwner() {
		return this.protocol.getWorkingCopyOwner();
	}

	public static JavaLanguageServerPlugin getInstance() {
		return pluginInstance;
	}

	public static DiagnosticsState getNonProjectDiagnosticsState() {
		return pluginInstance.nonProjectDiagnosticsState;
	}

	public static void log(IStatus status) {
		if (context != null) {
			Platform.getLog(JavaLanguageServerPlugin.context.getBundle()).log(status);
		}
	}

	public static void log(CoreException e) {
		log(e.getStatus());
	}

	public static void logError(String message) {
		if (context != null) {
			log(new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), message));
		}
	}

	public static void logInfo(String message) {
		if (context != null) {
			log(new Status(IStatus.INFO, context.getBundle().getSymbolicName(), message));
		}
	}

	public static void logException(Throwable ex) {
		if (context != null) {
			String message = ex.getMessage();
			if (message == null) {
				message = Throwables.getStackTraceAsString(ex);
			}
			logException(message, ex);
		}
	}

	public static void logException(String message, Throwable ex) {
		if (context != null) {
			log(new Status(IStatus.ERROR, context.getBundle().getSymbolicName(), message, ex));
		}
	}

	public static void sendStatus(ServiceStatus serverStatus, String status) {
		if (pluginInstance != null && pluginInstance.protocol != null) {
			pluginInstance.protocol.sendStatus(serverStatus, status);
		}
	}

	static void startLanguageServer(LanguageServer newLanguageServer) throws IOException {
		if (pluginInstance != null) {
			pluginInstance.languageServer = newLanguageServer;
			pluginInstance.startConnection();
		}
	}

	/**
	 * @return
	 */
	public static ProjectsManager getProjectsManager() {
		return pluginInstance.projectsManager;
	}

	public static DigestStore getDigestStore() {
		return pluginInstance.digestStore;
	}

	/**
	 * @return
	 */
	public static ContentProviderManager getContentProviderManager() {
		return pluginInstance.contentProviderManager;
	}

	/**
	 * @return the Java Language Server version
	 */
	public static String getVersion() {
		return context == null ? "Unknown" : context.getBundle().getVersion().toString();
	}

	private static void redirectStandardStreams() throws FileNotFoundException {
		in = System.in;
		out = System.out;
		err = System.err;
		System.setIn(new ByteArrayInputStream(new byte[0]));
		boolean isDebug = Boolean.getBoolean("jdt.ls.debug");
		if (isDebug) {
			String id = "jdt.ls-" + new SimpleDateFormat("yyyyMMddHHmmss").format(new Date());
			IWorkspaceRoot root = ResourcesPlugin.getWorkspace().getRoot();
			File workspaceFile = root.getRawLocation().makeAbsolute().toFile();
			File rootFile = new File(workspaceFile, ".metadata");
			rootFile.mkdirs();
			File outFile = new File(rootFile, ".out-" + id + ".log");
			FileOutputStream stdFileOut = new FileOutputStream(outFile);
			System.setOut(new PrintStream(stdFileOut));
			File errFile = new File(rootFile, ".error-" + id + ".log");
			FileOutputStream stdFileErr = new FileOutputStream(errFile);
			System.setErr(new PrintStream(stdFileErr));
		} else {
			System.setOut(new PrintStream(new ByteArrayOutputStream()));
			System.setErr(new PrintStream(new ByteArrayOutputStream()));
		}
	}

	public static InputStream getIn() {
		return in;
	}

	public static PrintStream getOut() {
		return out;
	}

	public static PrintStream getErr() {
		return err;
	}

	public static PreferenceManager getPreferencesManager() {
		if (JavaLanguageServerPlugin.pluginInstance != null) {
			return JavaLanguageServerPlugin.pluginInstance.preferenceManager;
		}
		return null;
	}

	public void unregisterCapability(String id, String method) {
		if (protocol != null) {
			protocol.unregisterCapability(id, method);
		}
	}

	public void registerCapability(String id, String method) {
		registerCapability(id, method, null);
	}

	public void registerCapability(String id, String method, Object options) {
		if (protocol != null) {
			protocol.registerCapability(id, method, options);
		}
	}

	public void setProtocol(JDTLanguageServer protocol) {
		this.protocol = protocol;
	}

	public BaseJDTLanguageServer getProtocol() {
		return protocol;
	}

	public JavaClientConnection getClientConnection() {
		if (protocol != null) {
			return protocol.getClientConnection();
		}
		return null;
	}

	/**
	 * Returns the template context type registry for the java plug-in.
	 *
	 * @return the template context type registry for the java plug-in
	 */
	public synchronized ContextTypeRegistry getTemplateContextRegistry() {
		if (fContextTypeRegistry == null) {
			fContextTypeRegistry = new JavaContextTypeRegistry();
		}
		return fContextTypeRegistry;
	}

	/**
	 * Returns the template store for the java editor templates.
	 *
	 * @return the template store for the java editor templates
	 */
	public JavaLanguageServerTemplateStore getTemplateStore() {
		if (fTemplateStore == null) {
			fTemplateStore = new JavaLanguageServerTemplateStore(getTemplateContextRegistry(), DefaultScope.INSTANCE.getNode(JavaManipulation.getPreferenceNodeId()), "");
			try {
				fTemplateStore.load();
			} catch (IOException e) {
				logException(e.getMessage(), e);
			}
		}

		return fTemplateStore;
	}

	//Public for testing purposes
	public static void setPreferencesManager(PreferenceManager preferenceManager) {
		if (pluginInstance != null) {
			pluginInstance.preferenceManager = preferenceManager;
		}
	}

	public synchronized TypeFilter getTypeFilter() {
		if (typeFilter == null) {
			typeFilter = new TypeFilter();
		}
		return typeFilter;
	}

}
