
angular.module('smartdlk', [
  'smartdlk.services', 'smartdlk.filters',
  'ui.bootstrap', 'ngRoute', 'ngAnimate', 'ngResource', 'ngFileUpload'
])

.config(function ($routeProvider) {

	$routeProvider.when("/", {
		templateUrl: "home.html",
		controller: "HomeCtrl"
	}).when("/rule/:ruleName", {
        templateUrl: "rule.html",
        controller: "RuleCtrl"
    }).otherwise("/404", {
		templateUrl: "404.html",
		controller: "PageCtrl"
	});
})

.directive('hcPie', function() {
    return {
      restrict: 'C',
      replace: true,
      scope: {
        items: '=',
        active: '='
      },
      controller: function($scope, $element, $attrs) {
      },
      template: '<div id="container">not working</div>',
      link: function(scope, element, attrs) {
          Highcharts.setOptions({
            global: {
              useUTC: false
            }
          });

          var chart;
          var chartOptions = {
            chart: {
              renderTo: 'container',
              height: 200,
              backgroundColor: "transparent",
              events: {
                load: function() {
                  var series = this.series[0];
                  setInterval(function() {
                    var x = (new Date()).getTime(), // current time
                    y = scope.active ? Math.round(Math.random() * 100) : 0;
                    series.addPoint([x, y], true, true);
                  }, 1000);
                }
              }
            },

            rangeSelector: {
              buttons: [{
                count: 1,
                type: 'minute',
                text: '1M'
              }, {
                count: 1,
                type: 'hour',
                text: '1H'
              }, {
                count: 1,
                type: 'day',
                text: '1D'
              }, {
                type: 'all',
                text: 'All'
              }],
              inputEnabled: false,
              selected: 0
            },
            title: { text: null },
            exporting: {
              enabled: false
            },
            series: [{
              name: null,
              data: (function() {
                // generate an array of random data
                var data = [],
                  time = (new Date()).getTime(),
                  i;

                for (i = -999; i <= 0; i += 1) {
                  var x = time + i * 1000,
                    y = Math.round(Math.random() * 100);
                  data.push([x, y]);
                }
                return data;
              }())
            }]
          };

            chart = new Highcharts.StockChart(chartOptions);
        }
    };
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

.controller('RuleCtrl', function ($scope, $routeParams, rulesService) {

    rulesService.get({rule: $routeParams.ruleName}, function(result) {
        $scope.rule = result;
    });

})


