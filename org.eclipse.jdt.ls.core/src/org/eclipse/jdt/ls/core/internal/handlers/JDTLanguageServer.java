/*******************************************************************************
 * Copyright (c) 2016-2018 Red Hat Inc. and others.
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
package org.eclipse.jdt.ls.core.internal.handlers;

import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logException;
import static org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin.logInfo;

import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jdt.core.WorkingCopyOwner;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.jdt.ls.core.internal.BuildWorkspaceStatus;
import org.eclipse.jdt.ls.core.internal.CancellableProgressMonitor;
import org.eclipse.jdt.ls.core.internal.JDTUtils;
import org.eclipse.jdt.ls.core.internal.JSONUtility;
import org.eclipse.jdt.ls.core.internal.JVMConfigurator;
import org.eclipse.jdt.ls.core.internal.JavaClientConnection;
import org.eclipse.jdt.ls.core.internal.JavaClientConnection.JavaLanguageClient;
import org.eclipse.jdt.ls.core.internal.JavaLanguageServerPlugin;
import org.eclipse.jdt.ls.core.internal.JobHelpers;
import org.eclipse.jdt.ls.core.internal.LanguageServerWorkingCopyOwner;
import org.eclipse.jdt.ls.core.internal.ServiceStatus;
import org.eclipse.jdt.ls.core.internal.codemanipulation.GenerateGetterSetterOperation.AccessorField;
import org.eclipse.jdt.ls.core.internal.handlers.FindLinksHandler.FindLinksParams;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateAccessorsHandler.GenerateAccessorsParams;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateConstructorsHandler.CheckConstructorsResponse;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateConstructorsHandler.GenerateConstructorsParams;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateDelegateMethodsHandler.CheckDelegateMethodsResponse;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateDelegateMethodsHandler.GenerateDelegateMethodsParams;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateToStringHandler.CheckToStringResponse;
import org.eclipse.jdt.ls.core.internal.handlers.GenerateToStringHandler.GenerateToStringParams;
import org.eclipse.jdt.ls.core.internal.handlers.GetRefactorEditHandler.GetRefactorEditParams;
import org.eclipse.jdt.ls.core.internal.handlers.GetRefactorEditHandler.RefactorWorkspaceEdit;
import org.eclipse.jdt.ls.core.internal.handlers.HashCodeEqualsHandler.CheckHashCodeEqualsResponse;
import org.eclipse.jdt.ls.core.internal.handlers.HashCodeEqualsHandler.GenerateHashCodeEqualsParams;
import org.eclipse.jdt.ls.core.internal.handlers.MoveHandler.MoveDestinationsResponse;
import org.eclipse.jdt.ls.core.internal.handlers.MoveHandler.MoveParams;
import org.eclipse.jdt.ls.core.internal.handlers.OverrideMethodsHandler.AddOverridableMethodParams;
import org.eclipse.jdt.ls.core.internal.handlers.OverrideMethodsHandler.OverridableMethodsResponse;
import org.eclipse.jdt.ls.core.internal.handlers.WorkspaceSymbolHandler.SearchSymbolParams;
import org.eclipse.jdt.ls.core.internal.lsp.JavaProtocolExtensions;
import org.eclipse.jdt.ls.core.internal.managers.ContentProviderManager;
import org.eclipse.jdt.ls.core.internal.managers.FormatterManager;
import org.eclipse.jdt.ls.core.internal.managers.ProjectsManager;
import org.eclipse.jdt.ls.core.internal.preferences.PreferenceManager;
import org.eclipse.jdt.ls.core.internal.preferences.Preferences;
import org.eclipse.lsp4j.CallHierarchyIncomingCall;
import org.eclipse.lsp4j.CallHierarchyIncomingCallsParams;
import org.eclipse.lsp4j.CallHierarchyItem;
import org.eclipse.lsp4j.CallHierarchyOutgoingCall;
import org.eclipse.lsp4j.CallHierarchyOutgoingCallsParams;
import org.eclipse.lsp4j.CallHierarchyPrepareParams;
import org.eclipse.lsp4j.CodeAction;
import org.eclipse.lsp4j.CodeActionKind;
import org.eclipse.lsp4j.CodeActionOptions;
import org.eclipse.lsp4j.CodeActionParams;
import org.eclipse.lsp4j.CodeLens;
import org.eclipse.lsp4j.CodeLensOptions;
import org.eclipse.lsp4j.CodeLensParams;
import org.eclipse.lsp4j.Command;
import org.eclipse.lsp4j.CompletionItem;
import org.eclipse.lsp4j.CompletionList;
import org.eclipse.lsp4j.CompletionParams;
import org.eclipse.lsp4j.DidChangeConfigurationParams;
import org.eclipse.lsp4j.DidChangeTextDocumentParams;
import org.eclipse.lsp4j.DidChangeWatchedFilesParams;
import org.eclipse.lsp4j.DidChangeWorkspaceFoldersParams;
import org.eclipse.lsp4j.DidCloseTextDocumentParams;
import org.eclipse.lsp4j.DidOpenTextDocumentParams;
import org.eclipse.lsp4j.DidSaveTextDocumentParams;
import org.eclipse.lsp4j.DocumentFormattingParams;
import org.eclipse.lsp4j.DocumentHighlight;
import org.eclipse.lsp4j.DocumentOnTypeFormattingOptions;
import org.eclipse.lsp4j.DocumentOnTypeFormattingParams;
import org.eclipse.lsp4j.DocumentRangeFormattingParams;
import org.eclipse.lsp4j.DocumentSymbol;
import org.eclipse.lsp4j.DocumentSymbolParams;
import org.eclipse.lsp4j.ExecuteCommandOptions;
import org.eclipse.lsp4j.ExecuteCommandParams;
import org.eclipse.lsp4j.FoldingRange;
import org.eclipse.lsp4j.FoldingRangeRequestParams;
import org.eclipse.lsp4j.Hover;
import org.eclipse.lsp4j.InitializeParams;
import org.eclipse.lsp4j.InitializeResult;
import org.eclipse.lsp4j.InitializedParams;
import org.eclipse.lsp4j.Location;
import org.eclipse.lsp4j.LocationLink;
import org.eclipse.lsp4j.PrepareRenameResult;
import org.eclipse.lsp4j.Range;
import org.eclipse.lsp4j.ReferenceParams;
import org.eclipse.lsp4j.Registration;
import org.eclipse.lsp4j.RegistrationParams;
import org.eclipse.lsp4j.RenameParams;
import org.eclipse.lsp4j.SelectionRange;
import org.eclipse.lsp4j.SelectionRangeParams;
import org.eclipse.lsp4j.SignatureHelp;
import org.eclipse.lsp4j.SymbolInformation;
import org.eclipse.lsp4j.TextDocumentIdentifier;
import org.eclipse.lsp4j.TextDocumentPositionParams;
import org.eclipse.lsp4j.TextEdit;
import org.eclipse.lsp4j.Unregistration;
import org.eclipse.lsp4j.UnregistrationParams;
import org.eclipse.lsp4j.WillSaveTextDocumentParams;
import org.eclipse.lsp4j.WorkspaceEdit;
import org.eclipse.lsp4j.WorkspaceSymbolParams;
import org.eclipse.lsp4j.jsonrpc.CancelChecker;
import org.eclipse.lsp4j.jsonrpc.CompletableFutures;
import org.eclipse.lsp4j.jsonrpc.messages.Either;
import org.eclipse.lsp4j.jsonrpc.services.JsonDelegate;
import org.eclipse.lsp4j.services.LanguageServer;
import org.eclipse.lsp4j.services.TextDocumentService;
import org.eclipse.lsp4j.services.WorkspaceService;

/**
 * @author Gorkem Ercan
 *
 */
public class JDTLanguageServer implements LanguageServer, TextDocumentService, WorkspaceService, JavaProtocolExtensions {

	public static final String JAVA_LSP_JOIN_ON_COMPLETION = "java.lsp.joinOnCompletion";
	/**
	 * Exit code returned when JDTLanguageServer is forced to exit.
	 */
	private static final int FORCED_EXIT_CODE = 1;
	private JavaClientConnection client;
	private ProjectsManager pm;
	private LanguageServerWorkingCopyOwner workingCopyOwner;
	private PreferenceManager preferenceManager;
	private DocumentLifeCycleHandler documentLifeCycleHandler;
	private WorkspaceDiagnosticsHandler workspaceDiagnosticsHandler;
	private JVMConfigurator jvmConfigurator;
	private WorkspaceExecuteCommandHandler commandHandler;

	private Set<String> registeredCapabilities = new HashSet<>(3);

	private ProgressReporterManager progressReporterManager;

	public LanguageServerWorkingCopyOwner getWorkingCopyOwner() {
		return workingCopyOwner;
	}

	public JDTLanguageServer(ProjectsManager projects, PreferenceManager preferenceManager) {
		this(projects, preferenceManager, WorkspaceExecuteCommandHandler.getInstance());
	}

	public JDTLanguageServer(ProjectsManager projects, PreferenceManager preferenceManager, WorkspaceExecuteCommandHandler commandHandler) {
		this.pm = projects;
		this.preferenceManager = preferenceManager;
		this.jvmConfigurator = new JVMConfigurator();
		JavaRuntime.addVMInstallChangedListener(jvmConfigurator);
		this.commandHandler = commandHandler;
	}

	public void connectClient(JavaLanguageClient client) {
		this.client = new JavaClientConnection(client);
		progressReporterManager = new ProgressReporterManager(client, preferenceManager);
		Job.getJobManager().setProgressProvider(progressReporterManager);
		this.workingCopyOwner = new LanguageServerWorkingCopyOwner(this.client);
		pm.setConnection(client);
		WorkingCopyOwner.setPrimaryBufferProvider(this.workingCopyOwner);
		this.documentLifeCycleHandler = new DocumentLifeCycleHandler(this.client, preferenceManager, pm, true);
	}

	//For testing purposes
	public void disconnectClient() {
		Job.getJobManager().setProgressProvider(null);
		this.client.disconnect();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.LanguageServer#initialize(org.eclipse.lsp4j.InitializeParams)
	 */
	@Override
	public CompletableFuture<InitializeResult> initialize(InitializeParams params) {
		logInfo(">> initialize");
		InitHandler handler = new InitHandler(pm, preferenceManager, client);
		InitializeResult result = handler.initialize(params);
		try {
			jvmConfigurator.configureDefaultVM(preferenceManager.getPreferences());
		} catch (Exception e) {
			JavaLanguageServerPlugin.logException(e.getMessage(), e);
		}

		return CompletableFuture.completedFuture(result);
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.LanguageServer#initialized(org.eclipse.lsp4j.InitializedParams)
	 */
	@Override
	public void initialized(InitializedParams params) {
		logInfo(">> initialized");
		try {
			Job.getJobManager().join(InitHandler.JAVA_LS_INITIALIZATION_JOBS, null);
		} catch (OperationCanceledException | InterruptedException e) {
			logException(e.getMessage(), e);
		}
		logInfo(">> initialization job finished");
		if (preferenceManager.getClientPreferences().isCompletionDynamicRegistered()) {
			registerCapability(Preferences.COMPLETION_ID, Preferences.COMPLETION, CompletionHandler.DEFAULT_COMPLETION_OPTIONS);
		}
		if (preferenceManager.getClientPreferences().isWorkspaceSymbolDynamicRegistered()) {
			registerCapability(Preferences.WORKSPACE_SYMBOL_ID, Preferences.WORKSPACE_SYMBOL);
		}
		if (preferenceManager.getClientPreferences().isDocumentSymbolDynamicRegistered()) {
			registerCapability(Preferences.DOCUMENT_SYMBOL_ID, Preferences.DOCUMENT_SYMBOL);
		}
		if (preferenceManager.getClientPreferences().isCodeActionDynamicRegistered()) {
			registerCapability(Preferences.CODE_ACTION_ID, Preferences.CODE_ACTION, getCodeActionOptions());
		}
		if (preferenceManager.getClientPreferences().isDefinitionDynamicRegistered()) {
			registerCapability(Preferences.DEFINITION_ID, Preferences.DEFINITION);
		}
		if (preferenceManager.getClientPreferences().isTypeDefinitionDynamicRegistered()) {
			registerCapability(Preferences.TYPEDEFINITION_ID, Preferences.TYPEDEFINITION);
		}
		if (!preferenceManager.getClientPreferences().isClientHoverProviderRegistered() && preferenceManager.getClientPreferences().isHoverDynamicRegistered()) {
			registerCapability(Preferences.HOVER_ID, Preferences.HOVER);
		}
		if (preferenceManager.getClientPreferences().isReferencesDynamicRegistered()) {
			registerCapability(Preferences.REFERENCES_ID, Preferences.REFERENCES);
		}
		if (preferenceManager.getClientPreferences().isDocumentHighlightDynamicRegistered()) {
			registerCapability(Preferences.DOCUMENT_HIGHLIGHT_ID, Preferences.DOCUMENT_HIGHLIGHT);
		}
		if (preferenceManager.getClientPreferences().isFoldgingRangeDynamicRegistered()) {
			registerCapability(Preferences.FOLDINGRANGE_ID, Preferences.FOLDINGRANGE);
		}
		if (preferenceManager.getClientPreferences().isWorkspaceFoldersSupported()) {
			registerCapability(Preferences.WORKSPACE_CHANGE_FOLDERS_ID, Preferences.WORKSPACE_CHANGE_FOLDERS);
		}
		if (preferenceManager.getClientPreferences().isImplementationDynamicRegistered()) {
			registerCapability(Preferences.IMPLEMENTATION_ID, Preferences.IMPLEMENTATION);
		}
		if (preferenceManager.getClientPreferences().isSelectionRangeDynamicRegistered()) {
			registerCapability(Preferences.SELECTION_RANGE_ID, Preferences.SELECTION_RANGE);
		}
		// we do not have the user setting initialized yet at this point but we should
		// still call to enable defaults in case client does not support configuration changes
		syncCapabilitiesToSettings();

		Job initializeWorkspace = new Job("Initialize workspace") {

			@Override
			public IStatus run(IProgressMonitor monitor) {
				try {
					JobHelpers.waitForBuildJobs(60 * 60 * 1000); // 1 hour
					logInfo(">> build jobs finished");
					workspaceDiagnosticsHandler = new WorkspaceDiagnosticsHandler(JDTLanguageServer.this.client, pm, preferenceManager.getClientPreferences());
					workspaceDiagnosticsHandler.publishDiagnostics(monitor);
					workspaceDiagnosticsHandler.addResourceChangeListener();
					pm.registerWatchers();
					pm.registerListeners();
					logInfo(">> watchers registered");
				} catch (OperationCanceledException | CoreException e) {
					logException(e.getMessage(), e);
					return Status.CANCEL_STATUS;
				}
				return Status.OK_STATUS;
			}
		};
		initializeWorkspace.setPriority(Job.BUILD);
		initializeWorkspace.setSystem(true);
		initializeWorkspace.schedule();
	}

	/**
	 * Toggles the server capabilities according to user preferences.
	 */
	private void syncCapabilitiesToSettings() {
		if (preferenceManager.getClientPreferences().isCompletionDynamicRegistered()) {
			toggleCapability(preferenceManager.getPreferences().isCompletionEnabled(), Preferences.COMPLETION_ID, Preferences.COMPLETION, CompletionHandler.DEFAULT_COMPLETION_OPTIONS);
		}
		if (preferenceManager.getClientPreferences().isFormattingDynamicRegistrationSupported()) {
			toggleCapability(preferenceManager.getPreferences().isJavaFormatEnabled(), Preferences.FORMATTING_ID, Preferences.TEXT_DOCUMENT_FORMATTING, null);
		}
		if (preferenceManager.getClientPreferences().isRangeFormattingDynamicRegistrationSupported()) {
			toggleCapability(preferenceManager.getPreferences().isJavaFormatEnabled(), Preferences.FORMATTING_RANGE_ID, Preferences.TEXT_DOCUMENT_RANGE_FORMATTING, null);
		}
		if (preferenceManager.getClientPreferences().isOnTypeFormattingDynamicRegistrationSupported()) {
			toggleCapability(preferenceManager.getPreferences().isJavaFormatOnTypeEnabled(), Preferences.FORMATTING_ON_TYPE_ID, Preferences.TEXT_DOCUMENT_ON_TYPE_FORMATTING,
					new DocumentOnTypeFormattingOptions(";", Arrays.asList("\n", "}")));
		}
		if (preferenceManager.getClientPreferences().isCodeLensDynamicRegistrationSupported()) {
			toggleCapability(preferenceManager.getPreferences().isCodeLensEnabled(), Preferences.CODE_LENS_ID, Preferences.TEXT_DOCUMENT_CODE_LENS, new CodeLensOptions(true));
		}
		if (preferenceManager.getClientPreferences().isSignatureHelpDynamicRegistrationSupported()) {
			toggleCapability(preferenceManager.getPreferences().isSignatureHelpEnabled(), Preferences.SIGNATURE_HELP_ID, Preferences.TEXT_DOCUMENT_SIGNATURE_HELP, SignatureHelpHandler.createOptions());
		}
		if (preferenceManager.getClientPreferences().isRenameDynamicRegistrationSupported()) {
			toggleCapability(preferenceManager.getPreferences().isRenameEnabled(), Preferences.RENAME_ID, Preferences.TEXT_DOCUMENT_RENAME, RenameHandler.createOptions());
		}
		if (preferenceManager.getClientPreferences().isExecuteCommandDynamicRegistrationSupported()) {
			toggleCapability(preferenceManager.getPreferences().isExecuteCommandEnabled(), Preferences.EXECUTE_COMMAND_ID, Preferences.WORKSPACE_EXECUTE_COMMAND,
					new ExecuteCommandOptions(new ArrayList<>(commandHandler.getNonStaticCommands())));
		}
		if (preferenceManager.getClientPreferences().isCodeActionDynamicRegistered()) {
			toggleCapability(preferenceManager.getClientPreferences().isCodeActionDynamicRegistered(), Preferences.CODE_ACTION_ID, Preferences.CODE_ACTION, getCodeActionOptions());
		}
		if (preferenceManager.getClientPreferences().isFoldgingRangeDynamicRegistered()) {
			toggleCapability(preferenceManager.getPreferences().isFoldingRangeEnabled(), Preferences.FOLDINGRANGE_ID, Preferences.FOLDINGRANGE, null);
		}
		if (preferenceManager.getClientPreferences().isSelectionRangeDynamicRegistered()) {
			toggleCapability(preferenceManager.getPreferences().isSelectionRangeEnabled(), Preferences.SELECTION_RANGE_ID, Preferences.SELECTION_RANGE, null);
		}
	}

	private CodeActionOptions getCodeActionOptions() {
		String[] kinds = { CodeActionKind.QuickFix, CodeActionKind.Refactor, CodeActionKind.RefactorExtract, CodeActionKind.RefactorInline, CodeActionKind.RefactorRewrite, CodeActionKind.Source, CodeActionKind.SourceOrganizeImports };
		List<String> codeActionKinds = new ArrayList<>();
		for (String kind : kinds) {
			if (preferenceManager.getClientPreferences().isSupportedCodeActionKind(kind)) {
				codeActionKinds.add(kind);
			}
		}
		CodeActionOptions options = new CodeActionOptions(codeActionKinds);
		return options;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.LanguageServer#shutdown()
	 */
	@Override
	public CompletableFuture<Object> shutdown() {
		logInfo(">> shutdown");
		return computeAsync((monitor) -> {
			try {
				JavaRuntime.removeVMInstallChangedListener(jvmConfigurator);
				if (workspaceDiagnosticsHandler != null) {
					workspaceDiagnosticsHandler.removeResourceChangeListener();
					workspaceDiagnosticsHandler = null;
				}
				ResourcesPlugin.getWorkspace().save(true, monitor);
			} catch (CoreException e) {
				logException(e.getMessage(), e);
			}
			return new Object();
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.LanguageServer#exit()
	 */
	@Override
	public void exit() {
		logInfo(">> exit");
		JavaLanguageServerPlugin.getLanguageServer().exit();
		Executors.newSingleThreadScheduledExecutor().schedule(() -> {
			logInfo("Forcing exit after 1 min.");
			System.exit(FORCED_EXIT_CODE);
		}, 1, TimeUnit.MINUTES);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.LanguageServer#getTextDocumentService()
	 */
	@Override
	public TextDocumentService getTextDocumentService() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.LanguageServer#getWorkspaceService()
	 */
	@Override
	public WorkspaceService getWorkspaceService() {
		return this;
	}

	@JsonDelegate
	public JavaProtocolExtensions getJavaExtensions() {
		return this;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.WorkspaceService#symbol(org.eclipse.lsp4j.WorkspaceSymbolParams)
	 */
	@Override
	public CompletableFuture<List<? extends SymbolInformation>> symbol(WorkspaceSymbolParams params) {
		logInfo(">> workspace/symbol");
		return computeAsync((monitor) -> {
			return WorkspaceSymbolHandler.search(params.getQuery(), monitor);
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.WorkspaceService#didChangeConfiguration(org.eclipse.lsp4j.DidChangeConfigurationParams)
	 */
	@Override
	public void didChangeConfiguration(DidChangeConfigurationParams params) {
		logInfo(">> workspace/didChangeConfiguration");
		Object settings = JSONUtility.toModel(params.getSettings(), Map.class);
		if (settings instanceof Map) {
			Collection<IPath> rootPaths = preferenceManager.getPreferences().getRootPaths();
			@SuppressWarnings("unchecked")
			Preferences prefs = Preferences.createFrom((Map<String, Object>) settings);
			prefs.setRootPaths(rootPaths);
			preferenceManager.update(prefs);
		}
		syncCapabilitiesToSettings();
		boolean jvmChanged = false;
		try {
			jvmChanged = jvmConfigurator.configureDefaultVM(preferenceManager.getPreferences());
		} catch (Exception e) {
			JavaLanguageServerPlugin.logException(e.getMessage(), e);
		}
		try {
			boolean autoBuildChanged = pm.setAutoBuilding(preferenceManager.getPreferences().isAutobuildEnabled());
			if (jvmChanged) {
				buildWorkspace(true);
			} else if (autoBuildChanged) {
				buildWorkspace(false);
			}
		} catch (CoreException e) {
			JavaLanguageServerPlugin.logException(e.getMessage(), e);
		}
		FormatterManager.configureFormatter(preferenceManager, pm);
		logInfo(">> New configuration: " + settings);
	}



	private void toggleCapability(boolean enabled, String id, String capability, Object options) {
		if (enabled) {
			registerCapability(id, capability, options);
		} else {
			unregisterCapability(id, capability);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.WorkspaceService#didChangeWatchedFiles(org.eclipse.lsp4j.DidChangeWatchedFilesParams)
	 */
	@Override
	public void didChangeWatchedFiles(DidChangeWatchedFilesParams params) {
		logInfo(">> workspace/didChangeWatchedFiles ");
		WorkspaceEventsHandler handler = new WorkspaceEventsHandler(pm, client, this.documentLifeCycleHandler);
		handler.didChangeWatchedFiles(params);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.WorkspaceService#executeCommand(org.eclipse.lsp4j.ExecuteCommandParams)
	 */
	@Override
	public CompletableFuture<Object> executeCommand(ExecuteCommandParams params) {
		logInfo(">> workspace/executeCommand " + (params == null ? null : params.getCommand()));
		return computeAsync((monitor) -> {
			return commandHandler.executeCommand(params, monitor);
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#completion(org.eclipse.lsp4j.CompletionParams)
	 */
	@Override
	public CompletableFuture<Either<List<CompletionItem>, CompletionList>> completion(CompletionParams position) {
		logInfo(">> document/completion");
		CompletionHandler handler = new CompletionHandler(preferenceManager);
		final IProgressMonitor[] monitors = new IProgressMonitor[1];
		CompletableFuture<Either<List<CompletionItem>, CompletionList>> result = computeAsync((monitor) -> {
			monitors[0] = monitor;
			if (Boolean.getBoolean(JAVA_LSP_JOIN_ON_COMPLETION)) {
				waitForLifecycleJobs(monitor);
			}
			return handler.completion(position, monitor);
		});
		result.join();
		if (monitors[0].isCanceled()) {
			result.cancel(true);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#resolveCompletionItem(org.eclipse.lsp4j.CompletionItem)
	 */
	@Override
	public CompletableFuture<CompletionItem> resolveCompletionItem(CompletionItem unresolved) {
		logInfo(">> document/resolveCompletionItem");
		CompletionResolveHandler handler = new CompletionResolveHandler(preferenceManager);
		final IProgressMonitor[] monitors = new IProgressMonitor[1];
		CompletableFuture<CompletionItem> result = computeAsync((monitor) -> {
			monitors[0] = monitor;
			if ((Boolean.getBoolean(JAVA_LSP_JOIN_ON_COMPLETION))) {
				waitForLifecycleJobs(monitor);
			}
			return handler.resolve(unresolved, monitor);
		});
		result.join();
		if (monitors[0].isCanceled()) {
			result.cancel(true);
		}
		return result;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#hover(org.eclipse.lsp4j.TextDocumentPositionParams)
	 */
	@Override
	public CompletableFuture<Hover> hover(TextDocumentPositionParams position) {
		logInfo(">> document/hover");
		HoverHandler handler = new HoverHandler(this.preferenceManager);
		return computeAsync((monitor) -> handler.hover(position, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#signatureHelp(org.eclipse.lsp4j.TextDocumentPositionParams)
	 */
	@Override
	public CompletableFuture<SignatureHelp> signatureHelp(TextDocumentPositionParams position) {
		logInfo(">> document/signatureHelp");
		SignatureHelpHandler handler = new SignatureHelpHandler(preferenceManager);
		return computeAsync((monitor) -> handler.signatureHelp(position, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#definition(org.eclipse.lsp4j.TextDocumentPositionParams)
	 */
	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> definition(TextDocumentPositionParams position) {
		logInfo(">> document/definition");
		NavigateToDefinitionHandler handler = new NavigateToDefinitionHandler(this.preferenceManager);
		return computeAsync((monitor) -> {
			waitForLifecycleJobs(monitor);
			return Either.forLeft(handler.definition(position, monitor));
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#typeDefinition(org.eclipse.lsp4j.TextDocumentPositionParams)
	 */
	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> typeDefinition(TextDocumentPositionParams position) {
		logInfo(">> document/typeDefinition");
		NavigateToTypeDefinitionHandler handler = new NavigateToTypeDefinitionHandler();
		return computeAsync((monitor) -> {
			waitForLifecycleJobs(monitor);
			return Either.forLeft((handler.typeDefinition(position, monitor)));
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#references(org.eclipse.lsp4j.ReferenceParams)
	 */
	@Override
	public CompletableFuture<List<? extends Location>> references(ReferenceParams params) {
		logInfo(">> document/references");
		ReferencesHandler handler = new ReferencesHandler(this.preferenceManager);
		return computeAsync((monitor) -> handler.findReferences(params, monitor));
	}

	public CompletableFuture<List<? extends Location>> findLinks(FindLinksParams params) {
		logInfo(">> java/findLinks");
		return computeAsync((monitor) -> FindLinksHandler.findLinks(params.type, params.position, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#documentHighlight(org.eclipse.lsp4j.TextDocumentPositionParams)
	 */
	@Override
	public CompletableFuture<List<? extends DocumentHighlight>> documentHighlight(TextDocumentPositionParams position) {
		logInfo(">> document/documentHighlight");
		DocumentHighlightHandler handler = new DocumentHighlightHandler();
		return computeAsync((monitor) -> handler.documentHighlight(position, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#documentSymbol(org.eclipse.lsp4j.DocumentSymbolParams)
	 */
	@Override
	public CompletableFuture<List<Either<SymbolInformation, DocumentSymbol>>> documentSymbol(DocumentSymbolParams params) {
		logInfo(">> document/documentSymbol");
		boolean hierarchicalDocumentSymbolSupported = preferenceManager.getClientPreferences().isHierarchicalDocumentSymbolSupported();
		DocumentSymbolHandler handler = new DocumentSymbolHandler(hierarchicalDocumentSymbolSupported);
		return computeAsync((monitor) -> {
			waitForLifecycleJobs(monitor);
			return handler.documentSymbol(params, monitor);
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#codeAction(org.eclipse.lsp4j.CodeActionParams)
	 */
	@Override
	public CompletableFuture<List<Either<Command, CodeAction>>> codeAction(CodeActionParams params) {
		logInfo(">> document/codeAction");
		CodeActionHandler handler = new CodeActionHandler(this.preferenceManager);
		return computeAsync((monitor) -> {
			waitForLifecycleJobs(monitor);
			return handler.getCodeActionCommands(params, monitor);
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#codeLens(org.eclipse.lsp4j.CodeLensParams)
	 */
	@Override
	public CompletableFuture<List<? extends CodeLens>> codeLens(CodeLensParams params) {
		logInfo(">> document/codeLens");
		CodeLensHandler handler = new CodeLensHandler(preferenceManager);
		return computeAsync((monitor) -> {
			waitForLifecycleJobs(monitor);
			return handler.getCodeLensSymbols(params.getTextDocument().getUri(), monitor);
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#resolveCodeLens(org.eclipse.lsp4j.CodeLens)
	 */
	@Override
	public CompletableFuture<CodeLens> resolveCodeLens(CodeLens unresolved) {
		logInfo(">> codeLens/resolve");
		CodeLensHandler handler = new CodeLensHandler(preferenceManager);
		return computeAsync((monitor) -> {
			waitForLifecycleJobs(monitor);
			return handler.resolve(unresolved, monitor);
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#formatting(org.eclipse.lsp4j.DocumentFormattingParams)
	 */
	@Override
	public CompletableFuture<List<? extends TextEdit>> formatting(DocumentFormattingParams params) {
		logInfo(">> document/formatting");
		FormatterHandler handler = new FormatterHandler(preferenceManager);
		return computeAsync((monitor) -> handler.formatting(params, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#rangeFormatting(org.eclipse.lsp4j.DocumentRangeFormattingParams)
	 */
	@Override
	public CompletableFuture<List<? extends TextEdit>> rangeFormatting(DocumentRangeFormattingParams params) {
		logInfo(">> document/rangeFormatting");
		FormatterHandler handler = new FormatterHandler(preferenceManager);
		return computeAsync((monitor) -> handler.rangeFormatting(params, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#onTypeFormatting(org.eclipse.lsp4j.DocumentOnTypeFormattingParams)
	 */
	@Override
	public CompletableFuture<List<? extends TextEdit>> onTypeFormatting(DocumentOnTypeFormattingParams params) {
		logInfo(">> document/onTypeFormatting");
		FormatterHandler handler = new FormatterHandler(preferenceManager);
		return computeAsync((monitor) -> handler.onTypeFormatting(params, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#prepareRename(org.eclipse.lsp4j.TextDocumentPositionParams)
	 */
	@Override
	public CompletableFuture<Either<Range, PrepareRenameResult>> prepareRename(TextDocumentPositionParams params) {
		logInfo(">> document/prepareRename");

		PrepareRenameHandler handler = new PrepareRenameHandler();
		return computeAsync((monitor) -> handler.prepareRename(params, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#rename(org.eclipse.lsp4j.RenameParams)
	 */
	@Override
	public CompletableFuture<WorkspaceEdit> rename(RenameParams params) {
		logInfo(">> document/rename");
		RenameHandler handler = new RenameHandler(preferenceManager);
		return computeAsync((monitor) -> handler.rename(params, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#didOpen(org.eclipse.lsp4j.DidOpenTextDocumentParams)
	 */
	@Override
	public void didOpen(DidOpenTextDocumentParams params) {
		logInfo(">> document/didOpen");
		documentLifeCycleHandler.didOpen(params);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#didChange(org.eclipse.lsp4j.DidChangeTextDocumentParams)
	 */
	@Override
	public void didChange(DidChangeTextDocumentParams params) {
		logInfo(">> document/didChange");
		documentLifeCycleHandler.didChange(params);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#didClose(org.eclipse.lsp4j.DidCloseTextDocumentParams)
	 */
	@Override
	public void didClose(DidCloseTextDocumentParams params) {
		logInfo(">> document/didClose");
		documentLifeCycleHandler.didClose(params);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#willSaveWaitUntil(org.eclipse.lsp4j.WillSaveTextDocumentParams)
	 */
	@Override
	public CompletableFuture<List<TextEdit>> willSaveWaitUntil(WillSaveTextDocumentParams params) {
		logInfo(">> document/willSaveWaitUntil");
		SaveActionHandler handler = new SaveActionHandler(preferenceManager);
		return computeAsync((monitor) -> handler.willSaveWaitUntil(params, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#didSave(org.eclipse.lsp4j.DidSaveTextDocumentParams)
	 */
	@Override
	public void didSave(DidSaveTextDocumentParams params) {
		logInfo(">> document/didSave");
		documentLifeCycleHandler.didSave(params);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ls.core.internal.JavaProtocolExtensions#ClassFileContents(org.eclipse.lsp4j.TextDocumentIdentifier)
	 */
	@Override
	public CompletableFuture<String> classFileContents(TextDocumentIdentifier param) {
		logInfo(">> java/classFileContents");
		ContentProviderManager handler = JavaLanguageServerPlugin.getContentProviderManager();
		URI uri = JDTUtils.toURI(param.getUri());
		return computeAsync((monitor) -> handler.getContent(uri, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ls.core.internal.JavaProtocolExtensions#projectConfigurationUpdate(org.eclipse.lsp4j.TextDocumentIdentifier)
	 */
	@Override
	public void projectConfigurationUpdate(TextDocumentIdentifier param) {
		logInfo(">> java/projectConfigurationUpdate");
		ProjectConfigurationUpdateHandler handler = new ProjectConfigurationUpdateHandler(pm);
		handler.updateConfiguration(param);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.jdt.ls.core.internal.JavaProtocolExtensions#buildWorkspace(boolean)
	 */
	@Override
	public CompletableFuture<BuildWorkspaceStatus> buildWorkspace(boolean forceReBuild) {
		logInfo(">> java/buildWorkspace (" + (forceReBuild ? "full)" : "incremental)"));
		BuildWorkspaceHandler handler = new BuildWorkspaceHandler(pm);
		return computeAsync((monitor) -> handler.buildWorkspace(forceReBuild, monitor));
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.WorkspaceService#didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams)
	 */
	@Override
	public void didChangeWorkspaceFolders(DidChangeWorkspaceFoldersParams params) {
		logInfo(">> java/didChangeWorkspaceFolders");
		WorkspaceFolderChangeHandler handler = new WorkspaceFolderChangeHandler(pm);
		handler.update(params);
	}

	@Override
	public CompletableFuture<Either<List<? extends Location>, List<? extends LocationLink>>> implementation(TextDocumentPositionParams position) {
		logInfo(">> document/implementation");
		return computeAsyncWithClientProgress((monitor) -> {
			ImplementationsHandler handler = new ImplementationsHandler(preferenceManager);
			return Either.forLeft(handler.findImplementations(position, monitor));
		});
	}

	/* (non-Javadoc)
	 * @see org.eclipse.lsp4j.services.TextDocumentService#foldingRange(org.eclipse.lsp4j.FoldingRangeRequestParams)
	 */
	@Override
	public CompletableFuture<List<FoldingRange>> foldingRange(FoldingRangeRequestParams params) {
		logInfo(">> document/foldingRange");
		return computeAsyncWithClientProgress((monitor) -> {
			waitForLifecycleJobs(monitor);
			return new FoldingRangeHandler().foldingRange(params, monitor);
		});
	}

	@Override
	public CompletableFuture<List<SelectionRange>> selectionRange(SelectionRangeParams params) {
		logInfo(">> document/selectionRange");
		return computeAsyncWithClientProgress((monitor) -> {
			waitForLifecycleJobs(monitor);
			return new SelectionRangeHandler().selectionRange(params, monitor);
		});
	}

	@Override
	public CompletableFuture<OverridableMethodsResponse> listOverridableMethods(CodeActionParams params) {
		logInfo(">> java/listOverridableMethods");
		return computeAsync((monitor) -> OverrideMethodsHandler.listOverridableMethods(params));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> addOverridableMethods(AddOverridableMethodParams params) {
		logInfo(">> java/addOverridableMethods");
		return computeAsync((monitor) -> OverrideMethodsHandler.addOverridableMethods(params));
	}

	@Override
	public CompletableFuture<CheckHashCodeEqualsResponse> checkHashCodeEqualsStatus(CodeActionParams params) {
		logInfo(">> java/checkHashCodeEqualsStatus");
		return computeAsync((monitor) -> HashCodeEqualsHandler.checkHashCodeEqualsStatus(params));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> generateHashCodeEquals(GenerateHashCodeEqualsParams params) {
		logInfo(">> java/generateHashCodeEquals");
		return computeAsync((monitor) -> HashCodeEqualsHandler.generateHashCodeEquals(params));
	}

	@Override
	public CompletableFuture<CheckToStringResponse> checkToStringStatus(CodeActionParams params) {
		logInfo(">> java/checkToStringStatus");
		return computeAsync((monitor) -> GenerateToStringHandler.checkToStringStatus(params));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> generateToString(GenerateToStringParams params) {
		logInfo(">> java/generateToString");
		return computeAsync((monitor) -> GenerateToStringHandler.generateToString(params));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> organizeImports(CodeActionParams params) {
		logInfo(">> java/organizeImports");
		return computeAsync((monitor) -> OrganizeImportsHandler.organizeImports(client, params));
	}

	@Override
	public CompletableFuture<AccessorField[]> resolveUnimplementedAccessors(CodeActionParams params) {
		logInfo(">> java/resolveUnimplementedAccessors");
		return computeAsync((monitor) -> GenerateAccessorsHandler.getUnimplementedAccessors(params));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> generateAccessors(GenerateAccessorsParams params) {
		logInfo(">> java/generateAccessors");
		return computeAsync((monitor) -> GenerateAccessorsHandler.generateAccessors(params));
	}

	@Override
	public CompletableFuture<CheckConstructorsResponse> checkConstructorsStatus(CodeActionParams params) {
		logInfo(">> java/checkConstructorsStatus");
		return computeAsync((monitor) -> GenerateConstructorsHandler.checkConstructorsStatus(params));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> generateConstructors(GenerateConstructorsParams params) {
		logInfo(">> java/generateConstructors");
		return computeAsync((monitor) -> GenerateConstructorsHandler.generateConstructors(params));
	}

	@Override
	public CompletableFuture<CheckDelegateMethodsResponse> checkDelegateMethodsStatus(CodeActionParams params) {
		logInfo(">> java/checkDelegateMethodsStatus");
		return computeAsync((monitor) -> GenerateDelegateMethodsHandler.checkDelegateMethodsStatus(params));
	}

	@Override
	public CompletableFuture<WorkspaceEdit> generateDelegateMethods(GenerateDelegateMethodsParams params) {
		logInfo(">> java/generateDelegateMethods");
		return computeAsync((monitor) -> GenerateDelegateMethodsHandler.generateDelegateMethods(params));
	}

	@Override
	public CompletableFuture<RefactorWorkspaceEdit> getRefactorEdit(GetRefactorEditParams params) {
		logInfo(">> java/getRefactorEdit");
		return computeAsync((monitor) -> GetRefactorEditHandler.getEditsForRefactor(params));
	}

	@Override
	public CompletableFuture<MoveDestinationsResponse> getMoveDestinations(MoveParams params) {
		logInfo(">> java/getMoveDestinations");
		return computeAsync((monitor) -> MoveHandler.getMoveDestinations(params));
	}

	@Override
	public CompletableFuture<RefactorWorkspaceEdit> move(MoveParams params) {
		logInfo(">> java/move");
		return computeAsyncWithClientProgress((monitor) -> MoveHandler.move(params, monitor));
	}

	@Override
	public CompletableFuture<List<SymbolInformation>> searchSymbols(SearchSymbolParams params) {
		logInfo(">> java/searchSymbols");
		return computeAsyncWithClientProgress((monitor) -> WorkspaceSymbolHandler.search(params.getQuery(), params.maxResults, params.projectName, params.sourceOnly, monitor));
	}

	@Override
	public CompletableFuture<List<CallHierarchyItem>> prepareCallHierarchy(CallHierarchyPrepareParams params) {
		logInfo(">> textDocumentt/prepareCallHierarchy");
		return computeAsyncWithClientProgress((monitor) -> new CallHierarchyHandler().prepareCallHierarchy(params, monitor));
	}

	@Override
	public CompletableFuture<List<CallHierarchyIncomingCall>> callHierarchyIncomingCalls(CallHierarchyIncomingCallsParams params) {
		logInfo(">> callHierarchy/incomingCalls");
		return computeAsyncWithClientProgress((monitor) -> new CallHierarchyHandler().callHierarchyIncomingCalls(params, monitor));
	}

	@Override
	public CompletableFuture<List<CallHierarchyOutgoingCall>> callHierarchyOutgoingCalls(CallHierarchyOutgoingCallsParams params) {
		logInfo(">> callHierarchy/outgoingCalls");
		return computeAsyncWithClientProgress((monitor) -> new CallHierarchyHandler().callHierarchyOutgoingCalls(params, monitor));
	}

	public void sendStatus(ServiceStatus serverStatus, String status) {
		if (client != null) {
			client.sendStatus(serverStatus, status);
		}
	}

	public void unregisterCapability(String id, String method) {
		if (registeredCapabilities.remove(id)) {
			Unregistration unregistration = new Unregistration(id, method);
			UnregistrationParams unregistrationParams = new UnregistrationParams(Collections.singletonList(unregistration));
			client.unregisterCapability(unregistrationParams);
		}
	}

	public void registerCapability(String id, String method) {
		registerCapability(id, method, null);
	}

	public void registerCapability(String id, String method, Object options) {
		if (registeredCapabilities.add(id)) {
			Registration registration = new Registration(id, method, options);
			RegistrationParams registrationParams = new RegistrationParams(Collections.singletonList(registration));
			client.registerCapability(registrationParams);
		}
	}

	public JavaClientConnection getClientConnection() {
		return client;
	}

	private <R> CompletableFuture<R> computeAsync(Function<IProgressMonitor, R> code) {
		return CompletableFutures.computeAsync(cc -> code.apply(toMonitor(cc)));
	}

	private <R> CompletableFuture<R> computeAsyncWithClientProgress(Function<IProgressMonitor, R> code) {
		return CompletableFutures.computeAsync((cc) -> {
			IProgressMonitor monitor = progressReporterManager.getProgressReporter(cc);
			return code.apply(monitor);
		});
	}

	private IProgressMonitor toMonitor(CancelChecker checker) {
		return new CancellableProgressMonitor(checker);
	}

	private void waitForLifecycleJobs(IProgressMonitor monitor) {
		try {
			Job.getJobManager().join(DocumentLifeCycleHandler.DOCUMENT_LIFE_CYCLE_JOBS, monitor);
		} catch (OperationCanceledException ignorable) {
			// No need to pollute logs when query is cancelled
		} catch (InterruptedException e) {
			JavaLanguageServerPlugin.logException(e.getMessage(), e);
		}
	}

}
