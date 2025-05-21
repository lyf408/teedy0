angular.module('docs').controller('TranslationDialogController', 
function($scope, $uibModalInstance, languages) {
    $scope.languageOptions = languages;
    $scope.translation = { from: 'en', to: 'zh-CHS' };

    $scope.ok = function() {
        $uibModalInstance.close($scope.translation);
    };

    $scope.cancel = function() {
        $uibModalInstance.dismiss('cancel');
    };
});