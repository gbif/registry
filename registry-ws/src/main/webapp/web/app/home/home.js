angular.module('home', [])

.config(['$stateProvider', function ($stateProvider, $stateParams, Installation) {
  $stateProvider.state('home', {
    url: '/home',
    templateUrl: 'app/home/home.tpl.html',
    controller: 'HomeCtrl'
  })
}])

.controller('HomeCtrl', function ($scope, $state, $http) {

  $http( { method:'GET', url: "../organization?limit=1" })
    .success(function (result) { $scope.organization = result});
  $http( { method:'GET', url: "../dataset?limit=1" })
    .success(function (result) { $scope.dataset = result});
  $http( { method:'GET', url: "../installation?limit=1" })
    .success(function (result) { $scope.installation = result});
  $http( { method:'GET', url: "../node?limit=1" })
    .success(function (result) { $scope.node = result});

  $scope.redirectTo = function (type) {
    $state.transitionTo(type + '-search.search');
  }
  $scope.open = function (type, key) {
    $state.transitionTo(type + '.detail', {key : key});
  }
});
