'use strict';

angular.module('docs').controller('SettingsRegistration', function ($scope, Restangular, $dialog, $translate) {
    $scope.loadRegistrations = function () {
        Restangular.one('user/pendingRegistrations').get().then(function (data) {
            $scope.registrations = data.registrations;
        });
    };

    $scope.loadRegistrations();

    function showConfirmationDialog(titleKey, messageKey, registration, onConfirm) {
        const title = $translate.instant(titleKey);
        const msg = $translate.instant(messageKey, { username: registration.username });
        const btns = [
            { result: 'cancel', label: $translate.instant('cancel') },
            { result: 'ok', label: $translate.instant('ok'), cssClass: 'btn-primary' }
        ];

        $dialog.messageBox(title, msg, btns, function (result) {
            if (result === 'ok') {
                onConfirm(registration);
            }
        });
    }

    function handleRegistration(action, registration) {
        Restangular.one(`user/registration/${action}`).post('', {
            id: registration.id
        }).then(function () {
            $scope.loadRegistrations();
        });
    }

    $scope.acceptRegistration = function (registration) {
        showConfirmationDialog(
            'settings.registration.accept_title',
            'settings.registration.accept_message',
            registration,
            function () {
                handleRegistration('accept', registration);
            }
        );
    };

    $scope.rejectRegistration = function (registration) {
        showConfirmationDialog(
            'settings.registration.reject_title',
            'settings.registration.reject_message',
            registration,
            function () {
                handleRegistration('reject', registration);
            }
        );
    };
});
