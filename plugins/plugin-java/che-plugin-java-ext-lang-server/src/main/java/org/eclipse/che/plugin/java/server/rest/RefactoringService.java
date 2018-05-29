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
package org.eclipse.che.plugin.java.server.rest;

import com.google.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeCreationResult;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangeEnabledState;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ChangePreview;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringChange;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringPreview;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringResult;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringSession;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.RefactoringStatus;
import org.eclipse.che.ide.ext.java.shared.dto.refactoring.ReorgDestination;
import org.eclipse.che.plugin.java.server.refactoring.RefactoringException;
import org.eclipse.che.plugin.java.server.refactoring.RefactoringManager;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.IJavaElement;
import org.eclipse.jdt.core.JavaModelException;
import org.eclipse.jdt.internal.core.JavaModel;
import org.eclipse.jdt.internal.core.JavaModelManager;

/**
 * Service for all Java refactorings
 *
 * @author Evgen Vidolob
 */
@Path("java/refactoring")
public class RefactoringService {
  private static final JavaModel model = JavaModelManager.getJavaModelManager().getJavaModel();
  private RefactoringManager manager;

  @Inject
  public RefactoringService(RefactoringManager manager) {
    this.manager = manager;
  }

  /**
   * Set destination for reorg refactorings.
   *
   * @param destination the destination for reorg refactoring
   * @return refactoring status
   * @throws RefactoringException when there are no corresponding refactoring session
   * @throws JavaModelException when JavaModel has a failure
   */
  @POST
  @Path("set/destination")
  @Produces("application/json")
  @Consumes("application/json")
  public RefactoringStatus setDestination(ReorgDestination destination)
      throws RefactoringException, JavaModelException {
    return manager.setRefactoringDestination(destination);
  }

  /**
   * Create refactoring change. Creation of the change starts final checking for refactoring.
   * Without creating change refactoring can't be applied.
   *
   * @param refactoringSession the refactoring session.
   * @return result of creation of the change.
   * @throws RefactoringException when there are no corresponding refactoring session
   */
  @POST
  @Path("create/change")
  @Produces("application/json")
  @Consumes("application/json")
  public ChangeCreationResult createChange(RefactoringSession refactoringSession)
      throws RefactoringException {
    return manager.createChange(refactoringSession.getSessionId());
  }

  /**
   * Get refactoring preview. Preview is tree of refactoring changes.
   *
   * @param refactoringSession the refactoring session.
   * @return refactoring preview tree
   * @throws RefactoringException when there are no corresponding refactoring session
   */
  @POST
  @Path("get/preview")
  @Produces("application/json")
  @Consumes("application/json")
  public RefactoringPreview getRefactoringPreview(RefactoringSession refactoringSession)
      throws RefactoringException {
    return manager.getRefactoringPreview(refactoringSession.getSessionId());
  }

  /**
   * Change enabled/disabled state of the corresponding refactoring change.
   *
   * @param state the state of refactoring change
   * @throws RefactoringException when there are no corresponding refactoring session or refactoring
   *     change
   */
  @POST
  @Path("change/enabled")
  public void changeChangeEnabledState(ChangeEnabledState state) throws RefactoringException {
    manager.changeChangeEnabled(state);
  }

  /**
   * Get refactoring change preview. Preview contains new and old content of the file
   *
   * @param change the change to get preview
   * @return refactoring change preview
   * @throws RefactoringException
   */
  @POST
  @Path("change/preview")
  @Produces("application/json")
  @Consumes("application/json")
  public ChangePreview getChangePreview(RefactoringChange change) throws RefactoringException {
    return manager.getChangePreview(change);
  }

  /**
   * Apply refactoring.
   *
   * @param session the refactoring session
   * @return the result fo applied refactoring
   * @throws RefactoringException when there are no corresponding refactoring session
   */
  @POST
  @Path("apply")
  @Produces("application/json")
  @Consumes("application/json")
  public RefactoringResult applyRefactoring(RefactoringSession session)
      throws RefactoringException, JavaModelException {
    return manager.applyRefactoring(session.getSessionId());
  }

  /**
   * Make reindex for the project.
   *
   * @param projectPath path to the project
   * @throws JavaModelException when something is wrong
   */
  @GET
  @Path("reindex")
  @Consumes("text/plain")
  public Response reindexProject(@QueryParam("projectpath") String projectPath)
      throws JavaModelException {
    manager.reindexProject(model.getJavaProject(projectPath));
    return Response.ok().build();
  }

  private IJavaElement getSelectionElement(ICompilationUnit compilationUnit, int offset)
      throws JavaModelException, RefactoringException {
    IJavaElement[] javaElements = compilationUnit.codeSelect(offset, 0);
    if (javaElements != null && javaElements.length > 0) {
      return javaElements[0];
    }
    throw new RefactoringException("Can't find java element to rename.");
  }
}
