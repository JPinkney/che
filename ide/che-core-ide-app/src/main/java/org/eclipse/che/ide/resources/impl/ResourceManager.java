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
package org.eclipse.che.ide.resources.impl;

import static com.google.common.base.Optional.absent;
import static com.google.common.base.Optional.of;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Preconditions.checkState;
import static com.google.common.base.Strings.isNullOrEmpty;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.stream;
import static org.eclipse.che.ide.api.resources.Resource.FILE;
import static org.eclipse.che.ide.api.resources.ResourceDelta.ADDED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.COPIED_FROM;
import static org.eclipse.che.ide.api.resources.ResourceDelta.DERIVED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.MOVED_FROM;
import static org.eclipse.che.ide.api.resources.ResourceDelta.MOVED_TO;
import static org.eclipse.che.ide.api.resources.ResourceDelta.REMOVED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.SYNCHRONIZED;
import static org.eclipse.che.ide.api.resources.ResourceDelta.UPDATED;
import static org.eclipse.che.ide.util.Arrays.contains;
import static org.eclipse.che.ide.util.Arrays.removeAll;
import static org.eclipse.che.ide.util.NameUtils.checkFileName;
import static org.eclipse.che.ide.util.NameUtils.checkFolderName;
import static org.eclipse.che.ide.util.NameUtils.checkProjectName;

import com.google.common.annotations.Beta;
import com.google.common.base.Optional;
import com.google.inject.Inject;
import com.google.web.bindery.event.shared.EventBus;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.eclipse.che.api.core.model.project.ProjectProblem;
import org.eclipse.che.api.core.model.workspace.config.ProjectConfig;
import org.eclipse.che.api.core.model.workspace.config.SourceStorage;
import org.eclipse.che.api.core.rest.shared.dto.Link;
import org.eclipse.che.api.project.shared.NewProjectConfig;
import org.eclipse.che.api.project.shared.dto.ItemReference;
import org.eclipse.che.api.project.shared.dto.NewProjectConfigDto;
import org.eclipse.che.api.project.shared.dto.SourceEstimation;
import org.eclipse.che.api.project.shared.dto.TreeElement;
import org.eclipse.che.api.promises.client.Function;
import org.eclipse.che.api.promises.client.FunctionException;
import org.eclipse.che.api.promises.client.Promise;
import org.eclipse.che.api.promises.client.PromiseProvider;
import org.eclipse.che.api.workspace.shared.dto.ProjectConfigDto;
import org.eclipse.che.api.workspace.shared.dto.SourceStorageDto;
import org.eclipse.che.ide.api.app.AppContext;
import org.eclipse.che.ide.api.editor.DeletedFilesController;
import org.eclipse.che.ide.api.editor.EditorAgent;
import org.eclipse.che.ide.api.editor.EditorPartPresenter;
import org.eclipse.che.ide.api.filewatcher.ClientServerEventService;
import org.eclipse.che.ide.api.project.MutableProjectConfig;
import org.eclipse.che.ide.api.project.QueryExpression;
import org.eclipse.che.ide.api.project.type.ProjectTypeRegistry;
import org.eclipse.che.ide.api.resources.Container;
import org.eclipse.che.ide.api.resources.File;
import org.eclipse.che.ide.api.resources.Folder;
import org.eclipse.che.ide.api.resources.Project;
import org.eclipse.che.ide.api.resources.Project.ProblemProjectMarker;
import org.eclipse.che.ide.api.resources.Project.ProjectRequest;
import org.eclipse.che.ide.api.resources.Resource;
import org.eclipse.che.ide.api.resources.ResourceChangedEvent;
import org.eclipse.che.ide.api.resources.ResourceDelta;
import org.eclipse.che.ide.api.resources.SearchResult;
import org.eclipse.che.ide.api.resources.marker.Marker;
import org.eclipse.che.ide.api.resources.marker.MarkerChangedEvent;
import org.eclipse.che.ide.api.vcs.VcsStatus;
import org.eclipse.che.ide.context.AppContextImpl;
import org.eclipse.che.ide.core.AgentURLModifier;
import org.eclipse.che.ide.dto.DtoFactory;
import org.eclipse.che.ide.project.ProjectServiceClient;
import org.eclipse.che.ide.resource.Path;
import org.eclipse.che.ide.util.Arrays;

/**
 * Acts as the service lay between the user interactions with resources and data transfer layer.
 * Main necessity of this manager is to encapsulate business logic which serves resources from the
 * interfaces.
 *
 * <p>This manager is not intended to be operated with third-party components. Only resources can
 * operate with it. To operate with resources use {@link AppContext#getWorkspaceRoot()} and {@link
 * AppContext#getProjects()}.
 *
 * @author Vlad Zhukovskiy
 * @see AppContext
 * @see AppContextImpl
 * @since 4.4.0
 */
@Beta
public final class ResourceManager {

  /** Describes zero depth level for the descendants. */
  private static final int DEPTH_ZERO = 0;

  /** Describes first depth level for the descendants. */
  private static final int DEPTH_ONE = 1;

  /**
   * Relative link for the content url.
   *
   * @see #newResourceFrom(ItemReference)
   */
  private static final String GET_CONTENT_REL = "get content";

  /**
   * Empty projects container.
   *
   * @see #getWorkspaceProjects()
   */
  private static final Project[] NO_PROJECTS = new Project[0];

  private static final Resource[] NO_RESOURCES = new Resource[0];

  private final ProjectServiceClient ps;
  private final EventBus eventBus;
  private final EditorAgent editorAgent;
  private final DeletedFilesController deletedFilesController;
  private final ResourceFactory resourceFactory;
  private final PromiseProvider promises;
  private final DtoFactory dtoFactory;
  private final ProjectTypeRegistry typeRegistry;
  /** Link to the workspace content root. Immutable among the workspace life. */
  private final Container workspaceRoot;

  private final AppContext appContext;
  private final AgentURLModifier urlModifier;
  private final ClientServerEventService clientServerEventService;
  /** Internal store, which caches requested resources from the server. */
  private ResourceStore store;

  @Inject
  public ResourceManager(
      ProjectServiceClient ps,
      EventBus eventBus,
      EditorAgent editorAgent,
      DeletedFilesController deletedFilesController,
      ResourceFactory resourceFactory,
      PromiseProvider promises,
      DtoFactory dtoFactory,
      ProjectTypeRegistry typeRegistry,
      ResourceStore store,
      AgentURLModifier urlModifier,
      ClientServerEventService clientServerEventService,
      AppContext appContext) {
    this.ps = ps;
    this.eventBus = eventBus;
    this.editorAgent = editorAgent;
    this.deletedFilesController = deletedFilesController;
    this.resourceFactory = resourceFactory;
    this.promises = promises;
    this.dtoFactory = dtoFactory;
    this.typeRegistry = typeRegistry;
    this.store = store;
    this.urlModifier = urlModifier;
    this.clientServerEventService = clientServerEventService;

    this.workspaceRoot = resourceFactory.newFolderImpl(Path.ROOT, this);
    this.appContext = appContext;
  }

  /**
   * Returns the workspace registered projects.
   *
   * @return the {@link Promise} with registered projects
   * @see Project
   * @since 4.4.0
   */
  public Promise<Project[]> getWorkspaceProjects() {
    return ps.getProjects()
        .then(
            (Function<List<ProjectConfigDto>, Project[]>)
                dtoConfigs -> {
                  store.clear();

                  if (dtoConfigs.isEmpty()) {
                    return NO_PROJECTS;
                  }

                  Project[] projects = NO_PROJECTS;

                  for (ProjectConfigDto config : dtoConfigs) {
                    if (Path.valueOf(config.getPath()).segmentCount() == 1) {
                      final Project project =
                          resourceFactory.newProjectImpl(config, ResourceManager.this);
                      store.register(project);

                      final Optional<ProblemProjectMarker> optionalMarker =
                          getProblemMarker(config);

                      if (optionalMarker.isPresent()) {
                        project.addMarker(optionalMarker.get());
                      }

                      Project[] tmpProjects = copyOf(projects, projects.length + 1);
                      tmpProjects[projects.length] = project;
                      projects = tmpProjects;
                    }
                  }

                  /* We need to guarantee that list of projects would be sorted by the logic provided in compareTo method implementation. */
                  java.util.Arrays.sort(projects);

                  for (Project project : projects) {
                    eventBus.fireEvent(
                        new ResourceChangedEvent(new ResourceDeltaImpl(project, ADDED | DERIVED)));
                  }

                  return projects;
                });
  }

  /**
   * Returns the workspace root container. This container is a holder which may contains only {@link
   * Project}s.
   *
   * @return the workspace container
   * @see Container
   * @since 4.4.0
   */
  public Container getWorkspaceRoot() {
    return workspaceRoot;
  }

  /**
   * Update state of specific properties in project and save this state on the server. As the result
   * method should return the {@link Promise} with new {@link Project} object.
   *
   * <p>During the update method have to iterate on children of updated resource and if any of them
   * has changed own type, e.g. folder -> project, project -> folder, specific event has to be
   * fired.
   *
   * <p>Method is not intended to be called in third party components. It is the service method for
   * {@link Project}.
   *
   * @param path the path to project which should be updated
   * @param request the update request
   * @return the {@link Promise} with new {@link Project} object.
   * @see ResourceChangedEvent
   * @see ProjectRequest
   * @see Project#update()
   * @since 4.4.0
   */
  protected Promise<Project> update(final Path path, final ProjectRequest request) {
    final ProjectConfig projectConfig = request.getBody();
    final SourceStorage source = projectConfig.getSource();
    final SourceStorageDto sourceDto = dtoFactory.createDto(SourceStorageDto.class);
    if (source != null) {
      sourceDto.setLocation(source.getLocation());
      sourceDto.setType(source.getType());
      sourceDto.setParameters(source.getParameters());
    }

    final ProjectConfigDto dto =
        dtoFactory
            .createDto(ProjectConfigDto.class)
            .withName(projectConfig.getName())
            .withPath(path.toString())
            .withDescription(projectConfig.getDescription())
            .withType(projectConfig.getType())
            .withMixins(projectConfig.getMixins())
            .withAttributes(projectConfig.getAttributes())
            .withSource(sourceDto);

    return ps.updateProject(dto)
        .thenPromise(
            reference -> {

              /* Note: After update, project may become to be other type,
              e.g. blank -> java or maven, or ant, or etc. And this may
              cause sub-project creations. Simultaneously on the client
              side there is outdated information about sub-projects, so
              we need to get updated project list. */

              // dispose outdated resource
              final Optional<Resource> outdatedResource = store.getResource(path);

              checkState(outdatedResource.isPresent(), "Outdated resource wasn't found");

              final Resource resource = outdatedResource.get();

              checkState(resource instanceof Container, "Outdated resource is not a container");

              Container container = (Container) resource;

              if (resource instanceof Folder) {
                Container parent = resource.getParent();
                checkState(parent != null, "Parent of the resource wasn't found");
                container = parent;
              }

              return synchronize(container)
                  .then(
                      new Function<Resource[], Project>() {
                        @Override
                        public Project apply(Resource[] synced) throws FunctionException {
                          final Optional<Resource> updatedProject = store.getResource(path);

                          checkState(updatedProject.isPresent(), "Updated resource is not present");
                          checkState(
                              updatedProject.get().isProject(),
                              "Updated resource is not a project");

                          eventBus.fireEvent(
                              new ResourceChangedEvent(
                                  new ResourceDeltaImpl(updatedProject.get(), UPDATED)));

                          return (Project) updatedProject.get();
                        }
                      });
            });
  }

  Promise<Folder> createFolder(final Container parent, final String name) {
    final Path path = Path.valueOf(name);

    Optional<Resource> existed = store.getResource(parent.getLocation().append(name));

    if (existed.isPresent()) {
      return promises.reject(new IllegalStateException("Resource already exists"));
    }

    if (parent.getLocation().isRoot()) {
      return promises.reject(
          new IllegalArgumentException("Failed to create folder in workspace root"));
    }

    if (path.segmentCount() == 1 && !checkFolderName(name)) {
      return promises.reject(new IllegalArgumentException("Invalid folder name"));
    }

    return ps.createFolder(parent.getLocation().append(name))
        .thenPromise(
            reference -> {
              final Resource createdFolder = newResourceFrom(reference);
              store.register(createdFolder);

              return promises.resolve(createdFolder.asFolder());
            });
  }

  Promise<File> createFile(final Container parent, final String name, final String content) {
    if (!checkFileName(name)) {
      return promises.reject(new IllegalArgumentException("Invalid file name"));
    }

    Optional<Resource> existed = store.getResource(parent.getLocation().append(name));

    if (existed.isPresent()) {
      return promises.reject(new IllegalStateException("Resource already exists"));
    }

    if (parent.getLocation().isRoot()) {
      return promises.reject(
          new IllegalArgumentException("Failed to create file in workspace root"));
    }

    return ps.createFile(parent.getLocation().append(name), content)
        .thenPromise(
            reference -> {
              final Resource createdFile = newResourceFrom(reference);
              store.register(createdFile);

              return promises.resolve(createdFile.asFile());
            });
  }

  Promise<Project> createProject(final Project.ProjectRequest createRequest) {
    checkArgument(checkProjectName(createRequest.getBody().getName()), "Invalid project name");
    checkArgument(
        typeRegistry.getProjectType(createRequest.getBody().getType()) != null,
        "Invalid project type");

    final Path path = Path.valueOf(createRequest.getBody().getPath());
    return findResource(path)
        .thenPromise(
            resource -> {
              if (resource.isPresent()) {
                if (resource.get().isProject()) {
                  return promises.reject(new IllegalStateException("Project already exists"));
                } else if (resource.get().isFile()) {
                  return promises.reject(
                      new IllegalStateException("File can not be converted to project"));
                }

                return update(path, createRequest);
              }

              final MutableProjectConfig projectConfig =
                  (MutableProjectConfig) createRequest.getBody();
              final List<NewProjectConfig> projectConfigList = projectConfig.getProjects();
              projectConfigList.add(asDto(projectConfig));
              final List<NewProjectConfigDto> configDtoList = asDto(projectConfigList);

              return ps.createBatchProjects(configDtoList)
                  .thenPromise(
                      configList ->
                          ps.getProjects()
                              .thenPromise(
                                  updatedConfiguration -> {
                                    for (ProjectConfigDto projectConfigDto : configList) {
                                      if (projectConfigDto.getPath().equals(path.toString())) {
                                        final Project newResource =
                                            resourceFactory.newProjectImpl(
                                                projectConfigDto, ResourceManager.this);
                                        store.register(newResource);
                                        eventBus.fireEvent(
                                            new ResourceChangedEvent(
                                                new ResourceDeltaImpl(
                                                    newResource, ADDED | DERIVED)));

                                        return promises.resolve(newResource);
                                      }
                                    }

                                    return promises.reject(
                                        new IllegalStateException("Created project is not found"));
                                  }));
            });
  }

  private NewProjectConfigDto asDto(MutableProjectConfig config) {
    final SourceStorage source = config.getSource();
    final SourceStorageDto sourceStorageDto =
        dtoFactory
            .createDto(SourceStorageDto.class)
            .withType(source.getType())
            .withLocation(source.getLocation())
            .withParameters(source.getParameters());

    return dtoFactory
        .createDto(NewProjectConfigDto.class)
        .withName(config.getName())
        .withPath(config.getPath())
        .withDescription(config.getDescription())
        .withSource(sourceStorageDto)
        .withType(config.getType())
        .withMixins(config.getMixins())
        .withAttributes(config.getAttributes())
        .withOptions(config.getOptions());
  }

  private List<NewProjectConfigDto> asDto(List<NewProjectConfig> configList) {
    List<NewProjectConfigDto> result = new ArrayList<>(configList.size());
    for (NewProjectConfig config : configList) {
      final SourceStorage source = config.getSource();
      final SourceStorageDto sourceStorageDto =
          dtoFactory
              .createDto(SourceStorageDto.class)
              .withType(source.getType())
              .withLocation(source.getLocation())
              .withParameters(source.getParameters());

      result.add(
          dtoFactory
              .createDto(NewProjectConfigDto.class)
              .withName(config.getName())
              .withPath(config.getPath())
              .withDescription(config.getDescription())
              .withSource(sourceStorageDto)
              .withType(config.getType())
              .withMixins(config.getMixins())
              .withAttributes(config.getAttributes())
              .withOptions(config.getOptions()));
    }
    return result;
  }

  protected Promise<Project> importProject(final Project.ProjectRequest importRequest) {
    checkArgument(checkProjectName(importRequest.getBody().getName()), "Invalid project name");
    checkNotNull(importRequest.getBody().getSource(), "Null source configuration occurred");

    final Path path = Path.valueOf(importRequest.getBody().getPath());

    return findResource(path)
        .thenPromise(
            resource -> {
              final SourceStorage sourceStorage = importRequest.getBody().getSource();
              final SourceStorageDto sourceStorageDto =
                  dtoFactory
                      .createDto(SourceStorageDto.class)
                      .withType(sourceStorage.getType())
                      .withLocation(sourceStorage.getLocation())
                      .withParameters(sourceStorage.getParameters());

              return ps.importProject(path, sourceStorageDto)
                  .thenPromise(
                      ignored ->
                          ps.getProject(path)
                              .then(
                                  (Function<ProjectConfigDto, Project>)
                                      config -> {
                                        Resource project =
                                            resourceFactory.newProjectImpl(
                                                config, ResourceManager.this);

                                        checkState(
                                            project != null,
                                            "Failed to locate imported project's configuration");

                                        store.register(project);

                                        eventBus.fireEvent(
                                            new ResourceChangedEvent(
                                                new ResourceDeltaImpl(
                                                    project,
                                                    (resource.isPresent() ? UPDATED : ADDED)
                                                        | DERIVED)));

                                        return (Project) project;
                                      }));
            });
  }

  protected Promise<Resource> move(
      final Resource source, final Path destination, final boolean force) {
    checkArgument(!source.getLocation().isRoot(), "Workspace root is not allowed to be moved");

    return findResource(destination)
        .thenPromise(
            resource -> {
              checkState(
                  !resource.isPresent() || force,
                  "Cannot create '" + destination.toString() + "'. Resource already exists.");

              if (isResourceOpened(source)) {
                deletedFilesController.add(source.getLocation().toString());
              }

              return ps.move(
                      source.getLocation(), destination.parent(), destination.lastSegment(), force)
                  .thenPromise(
                      ignored2 ->
                          findResource(destination)
                              .thenPromise(
                                  movedResource -> {
                                    if (movedResource.isPresent()) {
                                      eventBus.fireEvent(
                                          new ResourceChangedEvent(
                                              new ResourceDeltaImpl(
                                                  movedResource.get(),
                                                  source,
                                                  ADDED | MOVED_FROM | MOVED_TO | DERIVED)));
                                      return promises.resolve(movedResource.get());
                                    }

                                    throw new IllegalStateException("Resource not found");
                                  }));
            });
  }

  protected Promise<Resource> copy(
      final Resource source, final Path destination, final boolean force) {
    checkArgument(!source.getLocation().isRoot(), "Workspace root is not allowed to be copied");

    return findResource(destination)
        .thenPromise(
            resource -> {
              if (resource.isPresent() && !force) {
                return promises.reject(
                    new IllegalStateException(
                        "Cannot create '"
                            + destination.toString()
                            + "'. Resource already exists."));
              }

              return ps.copy(
                      source.getLocation(), destination.parent(), destination.lastSegment(), force)
                  .thenPromise(
                      ignored ->
                          findResource(destination)
                              .then(
                                  (Function<Optional<Resource>, Resource>)
                                      copiedResource -> {
                                        if (copiedResource.isPresent()) {
                                          eventBus.fireEvent(
                                              new ResourceChangedEvent(
                                                  new ResourceDeltaImpl(
                                                      copiedResource.get(),
                                                      source,
                                                      ADDED | COPIED_FROM | DERIVED)));
                                          return copiedResource.get();
                                        }

                                        throw new IllegalStateException("Resource not found");
                                      }));
            });
  }

  protected Promise<Void> delete(final Resource resource) {
    checkArgument(!resource.getLocation().isRoot(), "Workspace root is not allowed to be moved");

    return ps.deleteItem(resource.getLocation())
        .then(
            (Function<Void, Void>)
                ignored -> {
                  Resource[] descToRemove = null;

                  if (resource instanceof Container) {
                    final Optional<Resource[]> optDescendants =
                        store.getAll(resource.getLocation());

                    if (optDescendants.isPresent()) {
                      descToRemove = optDescendants.get();
                    }
                  }

                  store.dispose(resource.getLocation(), !resource.isFile());

                  if (isResourceOpened(resource)) {
                    deletedFilesController.add(resource.getLocation().toString());
                  }

                  eventBus.fireEvent(
                      new ResourceChangedEvent(new ResourceDeltaImpl(resource, REMOVED | DERIVED)));

                  if (descToRemove != null) {
                    for (Resource toRemove : descToRemove) {
                      if (isResourceOpened(toRemove)) {
                        deletedFilesController.add(toRemove.getLocation().toString());
                      }

                      eventBus.fireEvent(
                          new ResourceChangedEvent(
                              new ResourceDeltaImpl(toRemove, REMOVED | DERIVED)));
                    }
                  }

                  return null;
                });
  }

  protected Promise<Void> write(final File file, String content) {
    checkArgument(content != null, "Null content occurred");

    return ps.setFileContent(file.getLocation(), content);
  }

  protected Promise<String> read(File file) {
    return ps.getFileContent(file.getLocation());
  }

  Promise<Resource[]> getRemoteResources(
      final Container container, final int depth, boolean includeFiles) {
    checkArgument(depth > -2, "Invalid depth");

    if (depth == DEPTH_ZERO) {
      return promises.resolve(NO_RESOURCES);
    }

    int depthToReload = depth;
    final Optional<Resource[]> descendants = store.getAll(container.getLocation());

    if (depthToReload != -1 && descendants.isPresent()) {
      for (Resource resource : descendants.get()) {
        if (resource.getLocation().segmentCount() - container.getLocation().segmentCount()
            > depth) {
          depthToReload =
              resource.getLocation().segmentCount() - container.getLocation().segmentCount();
        }
      }
    }

    return ps.getTree(container.getLocation(), depthToReload, includeFiles)
        .then(
            new Function<TreeElement, Resource[]>() {
              @Override
              public Resource[] apply(TreeElement tree) throws FunctionException {

                class Visitor implements ResourceVisitor {

                  Resource[] resources;

                  private int size = 0; // size of total items
                  private int incStep = 50; // step to increase resource array

                  private Visitor() {
                    this.resources = NO_RESOURCES;
                  }

                  @Override
                  public void visit(Resource resource) {
                    if (resource.isProject()) {
                      inspectProject(resource.asProject());
                    }

                    if (size
                        > resources.length - 1) { // check load factor and increase resource array
                      resources = copyOf(resources, resources.length + incStep);
                    }

                    resources[size++] = resource;
                  }
                }

                final Visitor visitor = new Visitor();
                traverse(tree, visitor);

                return copyOf(visitor.resources, visitor.size);
              }
            })
        .then(
            (Function<Resource[], Resource[]>)
                reloaded -> {
                  Resource[] result = new Resource[0];

                  if (descendants.isPresent()) {
                    Resource[] outdated = descendants.get();

                    final Resource[] removed = removeAll(outdated, reloaded, false);
                    for (Resource resource : removed) {
                      store.dispose(resource.getLocation(), false);
                      eventBus.fireEvent(
                          new ResourceChangedEvent(new ResourceDeltaImpl(resource, REMOVED)));
                    }

                    final Resource[] updated =
                        stream(reloaded)
                            .filter(resource -> contains(outdated, resource))
                            .toArray(Resource[]::new);
                    for (Resource resource : updated) {
                      store.register(resource);

                      eventBus.fireEvent(
                          new ResourceChangedEvent(new ResourceDeltaImpl(resource, UPDATED)));

                      final Optional<Resource> registered =
                          store.getResource(resource.getLocation());
                      if (registered.isPresent()) {
                        result = Arrays.add(result, registered.get());
                      }
                    }

                    final Resource[] added = removeAll(reloaded, outdated, false);
                    for (Resource resource : added) {
                      store.register(resource);

                      eventBus.fireEvent(
                          new ResourceChangedEvent(new ResourceDeltaImpl(resource, ADDED)));

                      final Optional<Resource> registered =
                          store.getResource(resource.getLocation());
                      if (registered.isPresent()) {
                        result = Arrays.add(result, registered.get());
                      }
                    }

                  } else {
                    for (Resource resource : reloaded) {
                      store.register(resource);

                      eventBus.fireEvent(
                          new ResourceChangedEvent(new ResourceDeltaImpl(resource, ADDED)));

                      final Optional<Resource> registered =
                          store.getResource(resource.getLocation());
                      if (registered.isPresent()) {
                        result = Arrays.add(result, registered.get());
                      }
                    }
                  }

                  return result;
                });
  }

  Promise<Optional<Container>> getContainer(final Path absolutePath) {
    return findResource(absolutePath)
        .thenPromise(
            optResource -> {
              if (optResource.isPresent()) {
                Resource resource = optResource.get();
                checkState(resource instanceof Container, "Not a container");
                return promises.resolve(of((Container) resource));
              }

              return promises.resolve(absent());
            });
  }

  protected Promise<Optional<Resource>> getResource(final Path absolutePath) {
    return findResource(absolutePath);
  }

  protected Promise<Optional<File>> getFile(final Path absolutePath) {
    final Optional<Resource> resourceOptional = store.getResource(absolutePath);

    if (resourceOptional.isPresent() && resourceOptional.get().isFile()) {
      return promises.resolve(of(resourceOptional.get().asFile()));
    }

    if (store.getResource(absolutePath.parent()).isPresent()) {
      return findResource(absolutePath)
          .thenPromise(
              optionalFile -> {
                if (optionalFile.isPresent()) {
                  final Resource resource = optionalFile.get();
                  checkState(resource.getResourceType() == FILE, "Not a file");

                  return promises.resolve(of((File) resource));
                }

                return promises.resolve(absent());
              });
    } else {
      return findResourceForExternalOperation(absolutePath)
          .thenPromise(
              optionalFile -> {
                if (optionalFile.isPresent()) {
                  final Resource resource = optionalFile.get();
                  checkState(resource.getResourceType() == FILE, "Not a file");

                  return promises.resolve(of((File) resource));
                }

                return promises.resolve(absent());
              });
    }
  }

  Optional<Container> parentOf(Resource resource) {
    final Path parentLocation =
        resource.getLocation().segmentCount() == 1 ? Path.ROOT : resource.getLocation().parent();
    final Optional<Resource> optionalParent = store.getResource(parentLocation);

    if (!optionalParent.isPresent()) {
      return absent();
    }

    final Resource parentResource = optionalParent.get();

    checkState(parentResource instanceof Container, "Parent resource is not a container");

    return of((Container) parentResource);
  }

  Promise<Resource[]> childrenOf(final Container container, boolean forceUpdate) {
    if (forceUpdate) {
      return getRemoteResources(container, DEPTH_ONE, true);
    }

    Promise<Optional<Resource[]>> promise = promises.resolve(store.get(container.getLocation()));

    return promise.thenPromise(
        children ->
            ps.getTree(container.getLocation(), 1, true)
                .thenPromise(
                    loadedChildren -> {
                      if (!children.isPresent()) {
                        return promises.resolve(NO_RESOURCES);
                      }

                      Resource[] resources = children.get();

                      if (resources.length == loadedChildren.getChildren().size()) {
                        return promises.resolve(resources);
                      } else {
                        // situation, when we have outdated cached children
                        java.util.Arrays.stream(resources)
                            .forEach(outdated -> store.dispose(outdated.getLocation(), false));

                        List<Resource> updated =
                            new ArrayList<>(loadedChildren.getChildren().size());

                        for (TreeElement nodeElement : loadedChildren.getChildren()) {
                          ItemReference reference = nodeElement.getNode();
                          Resource tempResource = newResourceFrom(reference);
                          store.register(tempResource);

                          if (tempResource.isProject()) {
                            inspectProject(tempResource.asProject());
                          }

                          updated.add(tempResource);
                        }

                        return promises.resolve(updated.toArray(new Resource[updated.size()]));
                      }
                    }));
  }

  private Promise<Optional<Resource>> doFindResource(Path path) {
    return ps.getTree(path.parent(), 1, true)
        .thenPromise(
            treeElement -> {
              Resource resource = null;

              for (TreeElement nodeElement : treeElement.getChildren()) {
                ItemReference reference = nodeElement.getNode();
                Resource tempResource = newResourceFrom(reference);
                store.register(tempResource);

                if (tempResource.isProject()) {
                  inspectProject(tempResource.asProject());
                }

                if (tempResource.getLocation().equals(path)) {
                  resource = tempResource;
                  eventBus.fireEvent(
                      new ResourceChangedEvent(new ResourceDeltaImpl(resource, UPDATED)));
                }
              }

              return promises.resolve(Optional.fromNullable(resource));
            })
        .catchErrorPromise(error -> promises.resolve(absent()));
  }

  private Promise<Optional<Resource>> findResource(final Path absolutePath) {
    String[] segments = absolutePath.segments();

    Promise<Optional<Resource>> chain = promises.resolve(null);

    for (int i = 0; i <= segments.length; i++) {
      Path pathToRetrieve = absolutePath.removeLastSegments(segments.length - i);
      chain = chain.thenPromise(ignored -> doFindResource(pathToRetrieve));
    }

    return chain;
  }

  private Promise<Optional<Resource>> findResourceForExternalOperation(final Path absolutePath) {
    Promise<Void> derived = promises.resolve(null);

    for (int i = absolutePath.segmentCount() - 1; i > 0; i--) {
      final Path pathToCache = absolutePath.removeLastSegments(i);

      derived = derived.thenPromise(arg -> loadAndRegisterResources(pathToCache));
    }

    return derived.thenPromise(ignored -> findResource(absolutePath));
  }

  private Promise<Void> loadAndRegisterResources(Path absolutePath) {
    return ps.getTree(absolutePath, 1, true)
        .thenPromise(
            treeElement -> {
              final Optional<Resource[]> optionalChildren = store.get(absolutePath);

              if (optionalChildren.isPresent()) {
                for (Resource child : optionalChildren.get()) {
                  store.dispose(child.getLocation(), false);
                }
              }

              for (TreeElement element : treeElement.getChildren()) {
                final Resource resource = newResourceFrom(element.getNode());

                if (resource.isProject()) {
                  inspectProject(resource.asProject());
                }

                store.register(resource);
              }

              return promises.resolve(null);
            });
  }

  private void inspectProject(Project project) {
    final Optional<ProblemProjectMarker> optionalMarker = getProblemMarker(project);

    if (optionalMarker.isPresent()) {
      project.addMarker(optionalMarker.get());
    }
  }

  private boolean isResourceOpened(final Resource resource) {
    if (!resource.isFile()) {
      return false;
    }

    File file = (File) resource;

    for (EditorPartPresenter editor : editorAgent.getOpenedEditors()) {
      Path editorPath = editor.getEditorInput().getFile().getLocation();
      if (editorPath.equals(file.getLocation())) {
        return true;
      }
    }

    return false;
  }

  private void traverse(TreeElement tree, ResourceVisitor visitor) {
    for (final TreeElement element : tree.getChildren()) {

      final Resource resource = newResourceFrom(element.getNode());
      visitor.visit(resource);

      if (resource instanceof Container) {
        traverse(element, visitor);
      }
    }
  }

  private Resource newResourceFrom(final ItemReference reference) {
    final Path path = Path.valueOf(reference.getPath());

    switch (reference.getType()) {
      case "file":
        final Link link = reference.getLink(GET_CONTENT_REL);
        String vcsStatusAttribute = reference.getAttributes().get("vcs.status");
        return resourceFactory.newFileImpl(
            path,
            link.getHref(),
            this,
            vcsStatusAttribute == null
                ? VcsStatus.NOT_MODIFIED
                : VcsStatus.from(vcsStatusAttribute));
      case "folder":
        return resourceFactory.newFolderImpl(path, this);
      case "project":
        ProjectConfigDto config = reference.getProjectConfig();

        if (config != null) {
          return resourceFactory.newProjectImpl(config, this);
        } else {
          return resourceFactory.newFolderImpl(path, this);
        }
      default:
        throw new IllegalArgumentException("Failed to recognize resource type to create.");
    }
  }

  private Optional<ProblemProjectMarker> getProblemMarker(ProjectConfig projectConfigDto) {
    List<? extends ProjectProblem> problems = projectConfigDto.getProblems();
    if (problems == null || problems.isEmpty()) {
      return absent();
    }

    Map<Integer, String> code2Message = new HashMap<>(problems.size());
    for (ProjectProblem problem : problems) {
      code2Message.put(problem.getCode(), problem.getMessage());
    }

    return of(new ProblemProjectMarker(code2Message));
  }

  protected Promise<Resource[]> synchronize(final Container container) {
    final int[] maxDepth = new int[] {1};

    final Optional<Resource[]> descendants = store.getAll(container.getLocation());

    if (descendants.isPresent()) {
      final Resource[] resources = descendants.get();

      for (Resource resource : resources) {
        final int segCount =
            resource.getLocation().segmentCount() - container.getLocation().segmentCount();

        if (segCount > maxDepth[0]) {
          maxDepth[0] = segCount;
        }
      }

      java.util.Arrays.stream(descendants.get())
          .forEach(resource -> store.dispose(resource.getLocation(), false));
    }

    if (container.getLocation().isRoot()) {
      return getRemoteResources(container, maxDepth[0], true)
          .thenPromise(
              resources -> {
                eventBus.fireEvent(
                    new ResourceChangedEvent(
                        new ResourceDeltaImpl(container, SYNCHRONIZED | DERIVED)));
                eventBus.fireEvent(
                    new ResourceChangedEvent(new ResourceDeltaImpl(container, UPDATED)));
                return promises.resolve(resources);
              });
    } else {
      return findResource(container.getLocation())
          .thenPromise(
              updatedContainer -> {
                if (updatedContainer.isPresent()) {
                  return getRemoteResources(container, maxDepth[0], true)
                      .thenPromise(
                          resources -> {
                            eventBus.fireEvent(
                                new ResourceChangedEvent(
                                    new ResourceDeltaImpl(
                                        updatedContainer.get(), SYNCHRONIZED | DERIVED)));
                            eventBus.fireEvent(
                                new ResourceChangedEvent(
                                    new ResourceDeltaImpl(updatedContainer.get(), UPDATED)));
                            return promises.resolve(resources);
                          });
                }

                return promises.resolve(null);
              });
    }
  }

  protected Promise<ResourceDelta[]> synchronize(final ResourceDelta[] deltas) {
    Promise<Void> chain = promises.resolve(null);

    for (final ResourceDelta delta : deltas) {
      if (delta.getKind() == ADDED) {
        if (delta.getFlags() == (MOVED_FROM | MOVED_TO)) {
          chain = chain.thenPromise(ignored -> onExternalDeltaMoved(delta));
        } else {
          chain = chain.thenPromise(ignored -> onExternalDeltaAdded(delta));
        }
      } else if (delta.getKind() == REMOVED) {
        chain = chain.thenPromise(ignored -> onExternalDeltaRemoved(delta));
      } else if (delta.getKind() == UPDATED) {
        chain = chain.thenPromise(ignored -> onExternalDeltaUpdated(delta));
      }
    }
    return chain.thenPromise(ignored -> promises.resolve(deltas));
  }

  private Promise<Void> onExternalDeltaMoved(final ResourceDelta delta) {
    Optional<Resource> toRemove = store.getResource(delta.getFromPath());
    Path pathToDispose = Path.commonPath(delta.getFromPath(), delta.getToPath());

    store.dispose(pathToDispose, true);

    return findResource(delta.getToPath())
        .thenPromise(
            resource -> {
              if (resource.isPresent() && toRemove.isPresent()) {
                eventBus.fireEvent(
                    new ResourceChangedEvent(
                        new ResourceDeltaImpl(
                            resource.get(),
                            toRemove.get(),
                            ADDED | MOVED_FROM | MOVED_TO | DERIVED)));
              }

              return promises.resolve(null);
            });
  }

  private Promise<Void> onExternalDeltaAdded(final ResourceDelta delta) {
    return findResource(delta.getToPath())
        .thenPromise(
            resource -> {
              if (resource.isPresent()) {
                eventBus.fireEvent(
                    new ResourceChangedEvent(
                        new ResourceDeltaImpl(resource.get(), ADDED | DERIVED)));
              }

              return promises.resolve(null);
            });
  }

  private Promise<Void> onExternalDeltaUpdated(final ResourceDelta delta) {
    if (delta.getToPath().segmentCount() == 0) {
      workspaceRoot.synchronize();

      return promises.resolve(null);
    }

    return findResource(delta.getToPath())
        .thenPromise(
            resource -> {
              if (resource.isPresent()) {
                eventBus.fireEvent(
                    new ResourceChangedEvent(
                        new ResourceDeltaImpl(resource.get(), UPDATED | DERIVED)));
              }

              return promises.resolve(null);
            });
  }

  private Promise<Void> onExternalDeltaRemoved(final ResourceDelta delta) {

    final Optional<Resource> resourceOptional = store.getResource(delta.getFromPath());

    if (resourceOptional.isPresent()) {
      final Resource resource = resourceOptional.get();
      store.dispose(resource.getLocation(), true);
      eventBus.fireEvent(
          new ResourceChangedEvent(new ResourceDeltaImpl(resource, REMOVED | DERIVED)));
    }

    return promises.resolve(null);
  }

  protected Promise<SearchResult> search(
      final Container container, String fileMask, String contentMask) {
    QueryExpression queryExpression = new QueryExpression();
    if (!isNullOrEmpty(contentMask)) {
      queryExpression.setText(contentMask);
    }
    if (!isNullOrEmpty(fileMask)) {
      queryExpression.setName(fileMask);
    }
    if (!container.getLocation().isRoot()) {
      queryExpression.setPath(container.getLocation().toString());
    }

    return ps.search(queryExpression);
  }

  protected Promise<SearchResult> search(QueryExpression queryExpression) {
    return ps.search(queryExpression);
  }

  Promise<SourceEstimation> estimate(Container container, String projectType) {
    checkArgument(projectType != null, "Null project type");
    checkArgument(!projectType.isEmpty(), "Empty project type");

    return ps.estimate(container.getLocation(), projectType);
  }

  void notifyMarkerChanged(Resource resource, Marker marker, int status) {
    eventBus.fireEvent(new MarkerChangedEvent(resource, marker, status));
    eventBus.fireEvent(new ResourceChangedEvent(new ResourceDeltaImpl(resource, UPDATED)));
  }

  protected String getUrl(Resource resource) {
    checkArgument(!resource.getLocation().isRoot(), "Workspace root doesn't have export URL");

    final String baseUrl = appContext.getWsAgentServerApiEndpoint() + "/project/export";

    if (resource.getResourceType() == FILE) {
      return baseUrl + "/file" + resource.getLocation();
    }

    return urlModifier.modify(baseUrl + resource.getLocation());
  }

  public Promise<List<SourceEstimation>> resolve(Project project) {
    return ps.resolveSources(project.getLocation());
  }

  interface ResourceVisitor {

    void visit(Resource resource);
  }

  public interface ResourceFactory {

    ProjectImpl newProjectImpl(ProjectConfig reference, ResourceManager resourceManager);

    FolderImpl newFolderImpl(Path path, ResourceManager resourceManager);

    FileImpl newFileImpl(
        Path path, String contentUrl, ResourceManager resourceManager, VcsStatus vcsStatus);
  }

  public interface ResourceManagerFactory {
    ResourceManager newResourceManager();
  }
}
