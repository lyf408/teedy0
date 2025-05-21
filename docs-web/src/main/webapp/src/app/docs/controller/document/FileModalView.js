'use strict';

/**
 * File modal view controller.
 */
angular.module('docs').controller('FileModalView', function ($uibModalInstance, $scope, $state, $stateParams, $sce, Restangular, $transitions, $filter, $interval, $uibModal) {
  var setFile = function (files) {
    // Search current file
    _.each(files, function (value) {
      if (value.id === $stateParams.fileId) {
        $scope.file = value;
        $scope.trustedFileUrl = $sce.trustAsResourceUrl('../api/file/' + $stateParams.fileId + '/data');
      }
    });
  };

  // Load files
  Restangular.one('file/list').get({ id: $stateParams.id }).then(function (data) {
    $scope.files = data.files;
    setFile(data.files);

    // File not found, maybe it's a version
    if (!$scope.file) {
      Restangular.one('file/' + $stateParams.fileId + '/versions').get().then(function (data) {
        setFile(data.files);
      });
    }
  });

  /**
   * Return the next file.
   */
  $scope.nextFile = function () {
    var next = undefined;
    _.each($scope.files, function (value, key) {
      if (value.id === $stateParams.fileId) {
        next = $scope.files[key + 1];
      }
    });
    return next;
  };

  /**
   * Return the previous file.
   */
  $scope.previousFile = function () {
    var previous = undefined;
    _.each($scope.files, function (value, key) {
      if (value.id === $stateParams.fileId) {
        previous = $scope.files[key - 1];
      }
    });
    return previous;
  };

  /**
   * Navigate to the next file.
   */
  $scope.goNextFile = function () {
    var next = $scope.nextFile();
    if (next) {
      $state.go('^.file', { id: $stateParams.id, fileId: next.id });
    }
  };

  /**
   * Navigate to the previous file.
   */
  $scope.goPreviousFile = function () {
    var previous = $scope.previousFile();
    if (previous) {
      $state.go('^.file', { id: $stateParams.id, fileId: previous.id });
    }
  };

  $scope.languageOptions = {
    'zh-CHS': '简体中文',
    'en': '英语',
    'ja': '日语',
    'ko': '韩语',
    'fr': '法语',
    'de': '德语',
    'es': '西班牙语',
    'ru': '俄语'
  };

  // 默认翻译方向
  $scope.translation = {
    from: 'zh-CHS',
    to: 'en'
  };

  $scope.translateFile = function() {
    $scope.openTranslationDialog();
  };

  $scope.openTranslationDialog = function() {
    var modalInstance = $uibModal.open({
      templateUrl: 'partial/docs/translationDialog.html',
      controller: 'TranslationDialogController',
      resolve: {
        languages: function() {
            return $scope.languageOptions;
        }
      }
    });

    modalInstance.result.then(function(translation) {
      // 用户确认翻译，执行翻译流程
      $scope.translation = translation;
      $scope.startTranslationProcess();
    }, function() {
      // 用户取消翻译
      console.log('Translation cancelled');
    });
  };

  $scope.startTranslationProcess = function() {
    // 初始化状态
    $scope.isTranslating = true;
    $scope.translationStatusMessage = $filter('translate')('file.translation.submitting')
    // 1. 提交翻译请求
    console.log($scope.translation);
    Restangular.one('file', $stateParams.fileId).post('translation/submit', {
      from: $scope.translation.from, 
      to: $scope.translation.to
    }).then(function(response) {
      // 2. 获取翻译任务ID
      var flownumber = response.flownumber;
      $scope.translationStatusMessage = $filter('translate')('file.translation.processing');

      // 3. 轮询翻译状态
      var checkInterval = $interval(function() {
        Restangular.one('file/translation/status').get({ flownumber: flownumber })
          .then(function(statusResponse) {
            switch(statusResponse.status) {
              case 1: // 排队中
                $scope.translationStatusMessage = $filter('translate')('file.translation.queued');
                break;
              case 2: // 翻译中
                $scope.translationStatusMessage = $filter('translate')('file.translation.processing');
                break;
              case 3:
              case 5: // 翻译完成
                $scope.translationStatusMessage = $filter('translate')('file.translation.completed');
                break;
              case 4: // 翻译成功
                $scope.translationStatusMessage = $filter('translate')('file.translation.success');
                $interval.cancel(checkInterval);
                $scope.isTranslating = false;
                // 4. 获取翻译结果
                window.open('../api/file/translation/result?flownumber=' + flownumber, '_blank');
                $scope.translationStatusMessage = null;
                break;
              default: // 翻译失败
                $scope.translationStatusMessage = $filter('translate')('file.translation.failed');
                $interval.cancel(checkInterval);
                $scope.isTranslating = false;
                break;
            }
          }, function(error) {
            $interval.cancel(checkInterval);
            $scope.isTranslating = false;
            $scope.translationStatusMessage = $filter('translate')('file.translation.error');
          });
      }, 2000); // 每1秒检查一
      // 设置超时（5分钟）
      $timeout(function() {
        $interval.cancel(checkInterval);
        if ($scope.isTranslating) {
          $scope.isTranslating = false;
          $scope.translationStatusMessage = $filter('translate')('file.translation.timeout');
        }
      }, 60000); // 5分钟超
    }, function(error) {
      $scope.isTranslating = false;
      $scope.translationStatusMessage = $filter('translate')('file.translation.submit_error');
    });
  };

  /**
   * Open the file in a new window.
   */
  $scope.openFile = function () {
    window.open('../api/file/' + $stateParams.fileId + '/data');
  };

  /**
   * Open the file content a new window.
   */
  $scope.openFileContent = function () {
    window.open('../api/file/' + $stateParams.fileId + '/data?size=content');
  };

  /**
   * Print the file.
   */
  $scope.printFile = function () {
    var popup = window.open('../api/file/' + $stateParams.fileId + '/data', '_blank');
    popup.onload = function () {
      popup.print();
      popup.close();
    }
  };

  /**
   * Close the file preview.
   */
  $scope.closeFile = function () {
    $uibModalInstance.dismiss();
  };

  // Close the modal when the user exits this state
  var off = $transitions.onStart({}, function(transition) {
    if (!$uibModalInstance.closed) {
      if (transition.to().name === $state.current.name) {
        $uibModalInstance.close();
      } else {
        $uibModalInstance.dismiss();
      }
    }
    off();
  });

  /**
   * Return true if we can display the preview image.
   */
  $scope.canDisplayPreview = function () {
    return $scope.file && $scope.file.mimetype !== 'application/pdf';
  };
});