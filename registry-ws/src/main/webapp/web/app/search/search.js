angular.module('search', [
  'organization',
  'dataset',
  'installation',
  'node'])

.config(['$stateProvider', function ($stateProvider, $stateParams) {
  $stateProvider.state('search', {
    url: '/search?q',
    templateUrl:'app/search/search-list.tpl.html',
    controller: 'SearchViewCtrl',
    reloadOnSearch: true
  })
}])

// Inspired from http://stackoverflow.com/questions/14774486/use-jquery-timeago-and-angularjs-together
.filter('timeAgo', function() {
  return function(date) {
    return moment(date).fromNow();
  }
})

// takes the start and end and returns a human readable length
.filter('duration', function() {
  return function(vals) {
    if (vals.length==2 && vals[0] !== undefined && vals[1] !== undefined ) {
      return moment.duration(vals[1], vals[0]).humanize();
    }
    return "an incalculable duration";
  }
})


// supports the functions of actions on the seach results
.controller('SearchViewCtrl', function($scope, $location, $stateParams, $http) {
  $scope.q = $location.search().q;
  if ($scope.q==null) $scope.q="";

  $http.get('../organization?limit=100&q=' + $scope.q).success(function(data) {
    $scope.organizations = data;
    $scope.organizationsLabel = 'organization'.pluralize(data.count);
  });
  $http.get('../dataset?limit=100&q=' + $scope.q).success(function(data) {
    $scope.datasets = data;
    $scope.datasetsLabel = "dataset".pluralize(data.count);
  });
  $http.get('../installation?limit=100&q=' + $scope.q).success(function(data) {
    $scope.installations = data;
    $scope.installationsLabel = "installation".pluralize(data.count);
  });
  $http.get('../node?limit=100&q=' + $scope.q).success(function(data) {
    $scope.nodes = data;
    $scope.nodesLabel = "node".pluralize(data.count);
  });

  // rewrite the query param just for display
  if ($scope.q==null || $scope.q.length==0) $scope.q="*";

  $scope.viewItem = function (item, type) {
    $location.path('/' + type + '/'+ item.key);
  }

  // for changing the type of results, d'uh
  $scope.type = "organization"; // default for the first one
  $scope.setType = function (t) {
    $scope.type=t;
  }
})


// performs the search
.controller('SearchCtrl', function($scope, $location, $stateParams, $state, $rootScope) {
  $scope.q = $location.search().q;

  $scope.search = function (query) {
    $location.search("q", query).path("/search");
  };
});

// Simple pluralizer
String.prototype.pluralize = function(count, plural) {
  if (plural == null)
    plural = this + 's';
  // adding '' to stop it adding "..." by default
  return (count == 1 ? this + '' : plural)
}
