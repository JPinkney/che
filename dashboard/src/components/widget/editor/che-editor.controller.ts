/*
 * Copyright (c) 2015-2018 Red Hat, Inc.
 * This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License 2.0
 * which is available at https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
'use strict';

import { CheAPI } from '../../api/che-api.factory';

interface IEditor {
  refresh: Function;
  on(name: string, listener: (...args: any[]) => any);
  getModel(): any;
  getCursor(): ICursorPos;
  setCursor(cursorPos: ICursorPos): void;
}

interface ICursorPos {
  line: number;
  column: number;
}

interface IEditorState {
  isValid: boolean;
  errors: Array<string>;
}

/**
 * @ngdoc controller
 * @name components.directive:cheEditorController
 * @description This class is handling the controller for the editor.
 * @author Oleksii Orel
 */
export class CheEditorController {

  static $inject = ['$timeout', 'cheAPI'];

  $timeout: ng.ITimeoutService;

  setEditorValue: (content: string) => void;
  /**
   * Editor options object.
   */
  private editorOptions: {
    mode?: string;
    readOnly?: boolean;
    lineWrapping?: boolean;
    lineNumbers?: boolean;
    onLoad: Function;
  };
  /**
   * Editor form controller.
   */
  private editorForm: ng.IFormController;
  /**
   * Editor state object.
   */
  private editorState: IEditorState;
  /**
   * Custom validator callback.
   */
  private validator: Function;
  /**
   * On content change callback.
   */
  private onContentChange: Function;
  /**
   * Is editor read only.
   */
  private editorReadOnly: boolean;
  /**
   * Editor mode.
   */
  private editorMode: string;
  /**
   * Cursor position.
   */
  private cursorPos: ICursorPos = { line: 0, column: 0 };

  private cheAPI: CheAPI;

  /**
   * Default constructor that is using resource injection
   */
  constructor($timeout: ng.ITimeoutService, cheAPI: CheAPI) {
    this.$timeout = $timeout;
    this.cheAPI = cheAPI;
  }

  $onInit(): void {
    this.editorState = { isValid: true, errors: [] };
    this.editorOptions = {
      mode: angular.isString(this.editorMode) ? this.editorMode : 'application/json',
      readOnly: this.editorReadOnly ? this.editorReadOnly : false,
      lineWrapping: true,
      lineNumbers: true,
      onLoad: (editor: IEditor) => {
        const doc = editor.getModel();
        this.setEditorValue = (content: string) => {
          doc.setValue(content);
        };
        doc.onDidChangeContent(() => {
          this.$timeout(() => {
            this.editorState.errors.length = 0;
            if (angular.isFunction(this.validator)) {
              try {
                const customValidatorState: IEditorState = this.validator();
                if (customValidatorState && angular.isArray(customValidatorState.errors)) {
                  customValidatorState.errors.forEach((error: string) => {
                    this.editorState.errors.push(error);
                  });
                }
              } catch (error) {
                this.editorState.errors.push(error.toString());
              }
            }
            this.editorState.isValid = this.editorState.errors.length === 0;
            if (angular.isFunction(this.onContentChange)) {
              this.onContentChange({ editorState: this.editorState });
            }

            this.editorForm.$setValidity('custom-validator', this.editorState.isValid, null);
          }, 500);
        });
        this.registerYAMLinMonaco((window as any).Monaco);
      }
    };
  }

  registerYAMLinMonaco(monaco) {
      const LANGUAGE_ID = 'yaml';
      const MODEL_URI = 'inmemory://model.yaml';
      const MONACO_URI = monaco.Uri.parse(MODEL_URI);

      const m2p = new (window as any).monacoConversion.MonacoToProtocolConverter();
      const p2m = new (window as any).monacoConversion.ProtocolToMonacoConverter();

      function createDocument(model) {
        return (window as any).yamlLanguageServer.TextDocument.create(
          MODEL_URI,
          model.getModeId(),
          model.getVersionId(),
          model.getValue()
        );
      }

      const yamlService = this.createYAMLService();
      this.cheAPI.getDevfile().fetchDevfileSchema().then(jsonSchema => {
        const schemas = [{
          uri: 'inmemory:yaml',
          fileMatch: ['*'],
          schema: jsonSchema.data
        }];
        yamlService.configure({
          validate: true,
          schemas,
          hover: true,
          completion: true,
        });
      });

      // validation is not a 'registered' feature like the others, it relies on calling the yamlService
      // directly for validation results when content in the editor has changed
      this.YAMLValidation(monaco, p2m, MONACO_URI, createDocument, yamlService);

      /**
       * This exists because react-monaco-editor passes the same monaco
       * object each time. Without it you would be registering all the features again and
       * getting duplicate results.
       *
       * Monaco does not provide any apis for unregistering or checking if the features have already
       * been registered for a language.
       *
       * We check that > 1 YAML language exists because one is the default and one is the initial register
       * that setups our features.
       */
      if (monaco.languages.getLanguages().filter((x) => x.id === LANGUAGE_ID).length > 1) {
        return;
      }

      this.registerYAMLCompletion(LANGUAGE_ID, monaco, m2p, p2m, createDocument, yamlService);
      this.registerYAMLDocumentSymbols(LANGUAGE_ID, monaco, p2m, createDocument, yamlService);
      this.registerYAMLHover(LANGUAGE_ID, monaco, m2p, p2m, createDocument, yamlService);

    }

    createYAMLService() {
      var resolveSchema = function (url: string): Promise<string> {
        const promise = new Promise<string>((resolve, reject) => {
            const xhr = new XMLHttpRequest();
            xhr.onload = () => resolve(xhr.responseText);
            xhr.onerror = () => reject(xhr.statusText);
            xhr.open('GET', url, true);
            xhr.send();
            console.log('resolving schema ' + url);
        });
        return promise;
      };

      const workspaceContext = {
        resolveRelativePath: (relativePath, resource) => (window as any).url.resolve(resource, relativePath),
      };

      const yamlService = (window as any).yamlLanguageServer.getLanguageService(resolveSchema, workspaceContext, []);
      return yamlService;
    }

    registerYAMLCompletion(languageID, monaco, m2p, p2m, createDocument, yamlService) {
      monaco.languages.registerCompletionItemProvider(languageID, {
        provideCompletionItems(model, position) {
          const document = createDocument(model);
          return yamlService
            .doComplete(document, m2p.asPosition(position.lineNumber, position.column), true)
            .then((list) => {
              return p2m.asCompletionResult(list);
            });
        },

        resolveCompletionItem(item) {
          return yamlService
            .doResolve(m2p.asCompletionItem(item))
            .then((result) => p2m.asCompletionItem(result));
        },
      });
    }

    registerYAMLDocumentSymbols(languageID, monaco, p2m, createDocument, yamlService) {
      monaco.languages.registerDocumentSymbolProvider(languageID, {
        provideDocumentSymbols(model) {
          const document = createDocument(model);
          return p2m.asSymbolInformations(yamlService.findDocumentSymbols(document));
        },
      });
    }

    registerYAMLHover(languageID, monaco, m2p, p2m, createDocument, yamlService) {
      monaco.languages.registerHoverProvider(languageID, {
        provideHover(model, position) {
          const doc = createDocument(model);
          return yamlService
            .doHover(doc, m2p.asPosition(position.lineNumber, position.column))
            .then((hover) => {
              return p2m.asHover(hover);
            });
        },
      });
    }

    YAMLValidation(monaco, p2m, monacoURI, createDocument, yamlService) {
      const pendingValidationRequests = new Map();

      const getModel = () => monaco.editor.getModels()[0];

      const cleanPendingValidation = (document) => {
        const request = pendingValidationRequests.get(document.uri);
        if (request !== undefined) {
          clearTimeout(request);
          pendingValidationRequests.delete(document.uri);
        }
      };

      const cleanDiagnostics = () =>
        monaco.editor.setModelMarkers(monaco.editor.getModel(monacoURI), 'default', []);

      const doValidate = (document) => {
        if (document.getText().length === 0) {
          cleanDiagnostics();
          return;
        }
        yamlService.doValidation(document, true).then((diagnostics) => {
          const markers = p2m.asDiagnostics(diagnostics);
          monaco.editor.setModelMarkers(getModel(), 'default', markers);
        });
      };

      getModel().onDidChangeContent(() => {
        const document = createDocument(getModel());
        cleanPendingValidation(document);
        pendingValidationRequests.set(
          document.uri,
          setTimeout(() => {
            pendingValidationRequests.delete(document.uri);
            doValidate(document);
          })
        );
      });
    }

  /**
   * Returns validation state of the editor content.
   * @returns {boolean}
   */
  isEditorValid(): boolean {
    return this.editorState && this.editorState.isValid;
  }
}
