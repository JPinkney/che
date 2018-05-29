/**
 * ***************************************************************************** Copyright (c) 2000,
 * 2011 IBM Corporation and others. All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0 which accompanies this
 * distribution, and is available at http://www.eclipse.org/legal/epl-v10.html
 *
 * <p>Contributors: IBM Corporation - initial API and implementation
 * *****************************************************************************
 */
package org.eclipse.jdt.ui.refactoring;

import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.core.resources.mapping.ResourceMappingContext;
import org.eclipse.core.runtime.IProgressMonitor;

/**
 * Interface to define the processor IDs provided by the JDT refactoring.
 *
 * <p>This interface declares static final fields only; it is not intended to be implemented.
 *
 * @see org.eclipse.jdt.core.refactoring.participants.IRefactoringProcessorIds
 * @since 3.0
 * @noimplement This interface is not intended to be implemented by clients.
 * @noextend This interface is not intended to be extended by clients.
 */
public interface IRefactoringProcessorIds {

  /**
   * Processor ID of the delete resource processor (value <code>"org.eclipse.jdt.ui.DeleteProcessor"
   * </code>).
   *
   * <p>The delete processor loads the following participants, depending on the type of element that
   * gets deleted:
   *
   * <ul>
   *   <li><code>IJavaProject</code>: participants registered for deleting <code>IJavaProject
   *       </code> and <code>IProject</code>.
   *   <li><code>IPackageFragmentRoot</code>: participants registered for deleting <code>
   *       IPackageFragmentRoot</code> and <code>IFolder</code>.
   *   <li><code>IPackageFragment</code>: participants registered for deleting <code>
   *       IPackageFragment</code>. Additionally delete file and delete folder participants are
   *       loaded to reflect the resource changes caused by deleting a package fragment.
   *   <li><code>ICompilationUnit</code>: participants registered for deleting compilation units and
   *       files. Additionally type delete participants are loaded to reflect the deletion of the
   *       top level types declared in the compilation unit.
   *   <li><code>IType</code>: participants registered for deleting types. Additional compilation
   *       unit and file delete participants are loaded if the type to be deleted is the only top
   *       level type of a compilation unit.
   *   <li><code>IMember</code>: participants registered for deleting members.
   *   <li><code>IResource</code>: participants registered for deleting resources.
   * </ul>
   */
  public static String DELETE_PROCESSOR = "org.eclipse.jdt.ui.DeleteProcessor"; // $NON-NLS-1$

  /**
   * Processor ID of the copy processor (value <code>"org.eclipse.jdt.ui.CopyProcessor"</code>).
   *
   * <p>The copy processor is used when copying elements via drag and drop or when pasting elements
   * from the clipboard. The copy processor loads the following participants, depending on the type
   * of the element that gets copied:
   *
   * <ul>
   *   <li><code>IJavaProject</code>: no participants are loaded.
   *   <li><code>IPackageFragmentRoot</code>: participants registered for copying <code>
   *       IPackageFragmentRoot</code> and <code>ResourceMapping</code>.
   *   <li><code>IPackageFragment</code>: participants registered for copying <code>IPackageFragment
   *       </code> and <code>ResourceMapping</code>.
   *   <li><code>ICompilationUnit</code>: participants registered for copying <code>ICompilationUnit
   *       </code> and <code>ResourceMapping</code>.
   *   <li><code>IType</code>: like ICompilationUnit if the primary top level type is copied.
   *       Otherwise no participants are loaded.
   *   <li><code>IMember</code>: no participants are loaded.
   *   <li><code>IFolder</code>: participants registered for copying folders.
   *   <li><code>IFile</code>: participants registered for copying files.
   * </ul>
   *
   * <p>Use the method {@link ResourceMapping#accept(ResourceMappingContext context,
   * IResourceVisitor visitor, IProgressMonitor monitor)} to enumerate the resources which form the
   * Java element. <code>ResourceMappingContext.LOCAL_CONTEXT</code> should be use as the <code>
   * ResourceMappingContext</code> passed to the accept method.
   *
   * @see org.eclipse.core.resources.mapping.ResourceMapping
   * @since 3.3
   */
  public static String COPY_PROCESSOR = "org.eclipse.jdt.ui.CopyProcessor"; // $NON-NLS-1$
}
