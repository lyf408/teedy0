'use strict';

angular.module('docs').controller('Register', function ($scope, Restangular, $translate, $uibModalInstance) {
    $scope.request = {};
    $scope.error = null; // 用于模板绑定错误信息

    $scope.submit = function () {
        $scope.error = null;
        Restangular.one('user').post('register', {
            username: $scope.request.username,
            email: $scope.request.email
        }).then(function () {
            // 注册成功，关闭模态框并返回数据
            $uibModalInstance.close({
                success: true,
                title: $translate.instant('register.success_title'),
                message: $translate.instant('register.success_message')
            });
        }, function (response) {
            // 注册失败，显示错误信息（绑定到模板）
            $scope.error = {
                title: $translate.instant('register.error_title'),
                message: getErrorMessage(response)
            };
        });
    };

    function getErrorMessage(response) {
        if (response.data.type === 'AlreadyExistingUsername') {
            return $translate.instant('register.error_username_exists');
        } else if (response.data.type === 'AlreadyExistingEmail') {
            return $translate.instant('register.error_email_exists');
        } else {
            return $translate.instant('register.error_message');
        }
    }

    $scope.cancel = function () {
        $scope.error = null;
        $uibModalInstance.dismiss('cancel');
    };
});