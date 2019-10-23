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
/* --------------------------------------------------------------------------------------------
    * Copyright (c) 2018 TypeFox GmbH (http://www.typefox.io). All rights reserved.
    * Licensed under the MIT License. See License.txt in the project root for license information.
    * ------------------------------------------------------------------------------------------ */
'use strict';

const UI_MONACO_CONFIG = {
  wordWrap: 'on',
  lineNumbers: 'on',
  matchBrackets: true,
  autoClosingBrackets: 'always',
  readOnly: false
};

/**
 * Binds a Monaco widget to a textarea element.
 *
 * @author Oleksii Orel
 * @author Josh Pinkney
 */

/**
 * @ngdoc directive
 * @name components.directive:uiCodemirror
 * @restrict A
 * @function
 * @element
 *
 * @description
 * `ui-monaco` defines an attribute for Binds a Monaco widget to a div element.
 *
 * @usage
 *   <div ui-monaco="editorOptions" ng-model="val"></div>
 *
 * @author Oleksii Orel
 * @author Josh Pinkney
 */
export class UiMonaco implements ng.IDirective {

  restrict = 'A';
  require = 'ngModel';

  link($scope: ng.IScope, $element: ng.IAugmentedJQuery, $attrs: ng.IAttributes, $ctrl: ng.INgModelController): void {
    const element = $element[0];
    if (element.tagName !== 'DIV') {
      throw new Error(`the ui-monaco attribute should be used with a div elements only`);
    }

    const uiMonaco: string = ($attrs as any).uiMonaco;
    const monacoOptions = angular.extend(
      {value: ''},
      UI_MONACO_CONFIG,
      $scope.$eval(uiMonaco));

    const Monaco = (window as any).Monaco;

    const LANGUAGE_ID = 'yaml';
    const MODEL_URI = 'inmemory://model.yaml';
    const MONACO_URI = Monaco.Uri.parse(MODEL_URI);

    const editor = Monaco.editor.create(element, {
      model: Monaco.editor.createModel('', LANGUAGE_ID, MONACO_URI)
    });

    editor.layout({height: '600', width: '1000'});

    this.configOptionsWatcher(editor, UI_MONACO_CONFIG, uiMonaco, $scope);
    this.configNgModelLink(editor, $ctrl, $scope);

    // allow access to the Monaco instance through a broadcasted event
    // eg: $broadcast('Monaco', function(cm){...});
    $scope.$on('Monaco', (event: ng.IAngularEvent, callback: Function) => {
      if (angular.isFunction(callback)) {
        callback(editor);
      } else {
        throw new Error('the Monaco event requires a callback function');
      }
    });

    $scope.$on('$destroy', () => {
        if (editor) {
          editor.getModel().dispose();
          editor.dispose();
        }
    });

    // onLoad callback
    if (angular.isFunction(monacoOptions.onLoad)) {
      monacoOptions.onLoad(editor);
    }
  }

  private configOptionsWatcher(editor: any, codeMirrorDefaults: {}, uiCodemirrorAttr: string, scope: ng.IScope): void {
    if (!uiCodemirrorAttr) {
      return;
    }

    // scope.$watch(uiCodemirrorAttr, (newValues: Object, oldValue: Object) => {
    //   if (!angular.isObject(newValues)) {
    //     return;
    //   }
    //   codeMirrorDefaults.forEach((key: string) => {
    //     if (newValues.hasOwnProperty(key)) {
    //       if (oldValue && newValues[key] === oldValue[key]) {
    //         return;
    //       }
    //       editor.setOption(key, newValues[key]);
    //     }
    //   });
    // }, true);

  }

  private configNgModelLink(editor: any, ngModel: ng.INgModelController, scope: ng.IScope): void {
    if (!ngModel) {
      return;
    }

    const formatter: ng.IModelFormatter[] = ngModel.$formatters;
    // monaco expects a string, so make sure it gets one.
    // this does not change the model.
    formatter.push((value: any) => {
      if (angular.isUndefined(value) || value === null) {
        return '';
      } else if (angular.isObject(value) || angular.isArray(value)) {
        throw new Error('ui-monaco cannot use an object or an array as a model');
      }
      return value;
    });

    // override the ngModelController $render method, which is what gets called when the model is updated.
    // this takes care of the synchronizing the monaco element with the underlying model.
    ngModel.$render = () => {
      const safeViewValue = ngModel.$viewValue || '';
      editor.setValue(safeViewValue);
    };

    editor.onDidChangeModelContent(function () {
        var newValue = editor.getValue();
        if (newValue !== ngModel.$viewValue) {
            scope.$evalAsync(function () {
                ngModel.$setViewValue(newValue);
            });
        }
    });
  }

}
