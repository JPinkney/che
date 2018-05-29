/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.plugin.java.server.refactoring;

import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.RenameRefactoringSession.RenameWizard;
import static org.eclipse.che.ide.ext.java.shared.dto.refactoring.ReorgDestination.DestinationType;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;
import com.google.inject.Singleton;
import java.util.Collections;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.eclipse.che.commons.schedule.ScheduleRate;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeCreationResult;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeEnabledState;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangePreview;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringChange;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringPreview;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringResult;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ReorgDestination;
import org.eclipse.che.jdt.util.JdtFlags;
import org.eclipse.che.plugin.java.server.refactoring.session.RefactoringSession;
import org.eclipse.che.plugin.java.server.refactoring.session.ReorgRefactoringSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.core.IMember;
import org.eclipse.jdt.core.IMethod;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModelManager;
import org.eclipse.ltk.internal.ui.refactoring.ChangePreviewViewerDescriptor;
import org.eclipse.ltk.internal.ui.refactoring.PreviewNode;
import org.eclipse.ltk.ui.refactoring.IChangePreviewViewer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manager for all refactoring sessions. Handles creating caching and applying refactorings.
 *
 * @author Evgen Vidolob
 * @author Valeriy Svydenko
 */
@Singleton
public class RefactoringManager {
  private static final Logger LOG = LoggerFactory.getLogger(RefactoringManager.class);
  private static final AtomicInteger sessionId = new AtomicInteger(1);
  private final Cache<String, RefactoringSession> sessions;

  public RefactoringManager() {
    sessions =
        CacheBuilder.newBuilder()
            .expireAfterAccess(15, TimeUnit.MINUTES)
            .removalListener(
                new RemovalListener<String, RefactoringSession>() {
                  @Override
                  public void onRemoval(
                      RemovalNotification<String, RefactoringSession> notification) {
                    RefactoringSession value = notification.getValue();
                    if (value != null) {
                      value.dispose();
                    }
                  }
                })
            .build();
  }

  /** Periodically cleanup cache, to avoid memory leak. */
  @ScheduleRate(initialDelay = 1, period = 1, unit = TimeUnit.HOURS)
  void cacheClenup() {
    sessions.cleanUp();
  }

  public RefactoringStatus setRefactoringDestination(ReorgDestination destination)
      throws RefactoringException, JavaModelException {
    RefactoringSession session = getRefactoringSession(destination.getSessionId());
    if (!(session instanceof ReorgRefactoringSession)) {
      throw new RefactoringException("Can't set destination on none reorg refactoring session.");
    }

    ReorgRefactoringSession rs = ((ReorgRefactoringSession) session);
    Object dest =
        getDestination(
            destination.getProjectPath(), destination.getType(), destination.getDestination());
    org.eclipse.ltk.core.refactoring.RefactoringStatus refactoringStatus =
        rs.verifyDestination(dest);

    return DtoConverter.toRefactoringStatusDto(refactoringStatus);
  }

  private RefactoringSession getRefactoringSession(String sessionId) throws RefactoringException {
    RefactoringSession session = sessions.getIfPresent(sessionId);
    if (session == null) {
      throw new RefactoringException("Can't find refactoring session.");
    }
    return session;
  }

  private Object getDestination(String projectPath, DestinationType type, String destination)
      throws RefactoringException, JavaModelException {
    IJavaProject javaProject =
        JavaModelManager.getJavaModelManager().getJavaModel().getJavaProject(projectPath);
    if (javaProject == null) {
      throw new RefactoringException("Can't find project: " + projectPath);
    }
    switch (type) {
      case PACKAGE:
        return javaProject.findPackageFragment(new Path(destination));

      case RESOURCE:
      case SOURCE_REFERENCE:
      default:
        throw new UnsupportedOperationException(
            "Can't use destination for 'RESOURCE' or 'SOURCE_REFERENCE'.");
    }
  }

  /**
   * Get refactoring preview tree.
   *
   * @param sessionId id of the refactoring session
   * @return refactoring preview
   * @throws RefactoringException when refactoring session not found.
   */
  public RefactoringPreview getRefactoringPreview(String sessionId) throws RefactoringException {
    RefactoringSession session = getRefactoringSession(sessionId);
    PreviewNode node = session.getChangePreview();
    return DtoConverter.toRefactoringPreview(node);
  }

  /**
   * Create refactoring change and return status of creating changes.
   *
   * @param sessionId id of the refactoring session
   * @return change creations result
   * @throws RefactoringException when refactoring session not found.
   */
  public ChangeCreationResult createChange(String sessionId) throws RefactoringException {
    RefactoringSession session = getRefactoringSession(sessionId);
    return session.createChange();
  }

  /**
   * Apply refactoring.
   *
   * @param sessionId id of the refactoring session
   * @return refactoring result
   * @throws RefactoringException when refactoring session not found.
   */
  public RefactoringResult applyRefactoring(String sessionId) throws RefactoringException {
    RefactoringSession session = getRefactoringSession(sessionId);
    RefactoringResult result = session.apply();
    deleteRefactoringSession(sessionId);
    return result;
  }

  private void deleteRefactoringSession(String sessionId) {
    sessions.invalidate(sessionId);
  }

  private RenameWizard getWizardType(IJavaElement element) throws JavaModelException {
    switch (element.getElementType()) {
      case IJavaElement.PACKAGE_FRAGMENT:
        return RenameWizard.PACKAGE;
      case IJavaElement.COMPILATION_UNIT:
        return RenameWizard.COMPILATION_UNIT;
      case IJavaElement.TYPE:
        return RenameWizard.TYPE;
      case IJavaElement.METHOD:
        final IMethod method = (IMethod) element;
        if (method.isConstructor()) return RenameWizard.TYPE;
        else return RenameWizard.METHOD;
      case IJavaElement.FIELD:
        if (JdtFlags.isEnum((IMember) element)) {
          return RenameWizard.ENUM_CONSTANT;
        }
        return RenameWizard.FIELD;
      case IJavaElement.TYPE_PARAMETER:
        return RenameWizard.TYPE_PARAMETER;
      case IJavaElement.LOCAL_VARIABLE:
        return RenameWizard.LOCAL_VARIABLE;
    }
    return null;
  }

  /**
   * Include/exclude refactoring change from refactoring
   *
   * @param state updating state
   * @throws RefactoringException when refactoring session not found.
   */
  public void changeChangeEnabled(ChangeEnabledState state) throws RefactoringException {
    RefactoringSession session = getRefactoringSession(state.getSessionId());
    session.updateChangeEnabled(state.getChangeId(), state.isEnabled());
  }

  /**
   * generate preview for refactoring change
   *
   * @param change the refactoring change
   * @return refactoring change preview
   * @throws RefactoringException when refactoring session or change not found.
   */
  public ChangePreview getChangePreview(RefactoringChange change) throws RefactoringException {
    RefactoringSession session = getRefactoringSession(change.getSessionId());
    PreviewNode previewNode = session.getChangePreview(change.getChangeId());
    try {
      ChangePreviewViewerDescriptor descriptor = previewNode.getChangePreviewViewerDescriptor();
      if (descriptor != null) {
        IChangePreviewViewer viewer = descriptor.createViewer();
        if (viewer != null) {
          return previewNode.feedInput(viewer, Collections.EMPTY_LIST);
        }
      }
    } catch (CoreException e) {
      throw new RefactoringException(e.getMessage());
    }
    return null;
  }

  /**
   * Make reindex for the project.
   *
   * @param javaProject java project
   * @throws JavaModelException when something is wrong
   */
  public void reindexProject(IJavaProject javaProject) throws JavaModelException {
    if (javaProject != null) {
      JavaModelManager.getIndexManager().indexAll(javaProject.getProject());
    }
  }
}
