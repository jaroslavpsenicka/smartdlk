
angular.module('smartdlk', [
  'smartdlk.services', 'smartdlk.filters',
  'ui.bootstrap', 'ngRoute', 'ngAnimate', 'ngResource', 'ngFileUpload'
])

.config(function ($routeProvider) {

	$routeProvider.when("/", {
		templateUrl: "home.html",
		controller: "HomeCtrl"
	}).otherwise("/404", {
		templateUrl: "404.html",
		controller: "PageCtrl"
	});
})

.controller('PageCtrl', function ($scope) {
})

.controller('HomeCtrl', function ($scope, $timeout, Upload, rulesService) {

    $scope.rules = [];
    $scope.loadRules = function() {
        rulesService.query({}, function(data) {
            $scope.rules = data;
        });
    };

    $scope.toggled = {};
    $scope.toggle = function(name) {
        $scope.toggled[name] = !$scope.toggled[name];
    }

    $scope.uploadFile = function(file, errFiles) {
        if (file && file.length > 0) {
            Upload.upload({url: '/api/rules', data: {file: file}, arrayKey: ''}).then(function (response) {
                $timeout(function () {
                    $scope.rules.push(response.data);
                });
            });
        }
    };

    $scope.activate = function(rule) {
        rulesService.activate({rule: rule.name}, function(response) {
            $scope.updateRule(response);
        })
    };

    $scope.deactivate = function(rule) {
        rulesService.deactivate({rule: rule.name}, function(response) {
            $scope.updateRule(response);
        })
    };

    $scope.updateRule = function(rule) {
        for (idx in $scope.rules) {
            if ($scope.rules[idx].name == rule.name) {
                $scope.rules[idx] = rule;
                return;
            }
        }
    };

    $scope.loadRules();

})



