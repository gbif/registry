angular.module('user', [
  'restangular',
  'services.notifications',
  'endpoint',
  'identifier',
  'tag',
  'machineTag',
  'comment'])

/**
 * Nested stated provider using dot notation (item.detail has a parent of item) and the
 * nested view is rendered into the parent template ui.view div.  A single controller
 * governs the actions on the page.
 */
.config(['$stateProvider', function ($stateProvider, $stateParams) {
  $stateProvider.state('user-search', {
    abstract: true,
    url: '/user-search',
    templateUrl: 'app/user/user-search.tpl.html',
    controller: 'UserSearchCtrl'
  })
  .state('user-search.search', {
    url: '',
    templateUrl: 'app/user/user-results.tpl.html'
  })
  .state('user-search.create', {
    url: '/create',
    templateUrl: 'app/user/user-edit.tpl.html',
    controller: 'UserCreateCtrl',
    resolve: {
      item: function() { return {} } // load it with an empty one
    }
  })
  .state('user', {
    url: '/user/{key}',
    abstract: true,
    templateUrl: 'app/user/user-main.tpl.html',
    controller: 'UserCtrl'
  })
  .state('user.detail', {
    url: '',
    templateUrl: 'app/user/user-overview.tpl.html'
  })
  .state('user.edit', {
    url: '/edit',
    templateUrl: 'app/user/user-edit.tpl.html'
  })
  .state('user.contact', {
    url: '/contact',
    templateUrl: 'app/user/contact-list.tpl.html' // not common since read only
  })
  .state('user.identifier', {
    url: '/identifier',
    templateUrl: 'app/common/identifier-list.tpl.html',
    controller: "IdentifierCtrl",
    context: 'user', // necessary for reusing the components
    heading: 'User identifiers' // title for the sub pane
  })
  .state('user.endpoint', {
    url: '/endpoint',
    templateUrl: 'app/common/endpoint-list.tpl.html',
    controller: "EndpointCtrl",
    context: 'user',
    heading: 'User endpoints'
  })
  .state('user.tag', {
    url: '/tag',
    templateUrl: 'app/common/tag-list.tpl.html',
    controller: "TagCtrl",
    context: 'user',
    heading: 'User tags'
  })
  .state('user.machineTag', {
    url: '/machineTag',
    templateUrl: 'app/common/machinetag-list.tpl.html',
    controller: "MachinetagCtrl",
    context: 'user',
    heading: 'User machine tags'
  })
  .state('user.comment', {
    url: '/comment',
    templateUrl: 'app/common/comment-list.tpl.html',
    controller: "CommentCtrl",
    context: 'user',
    heading: 'User comments'
  })
  .state('user.pending', {
    url: '/pending',
    templateUrl: 'app/common/organization-list.tpl.html',
    context: 'user',
    heading: 'Organizations awaiting endorsement by the user'
  })
  .state('user.organization', {
    url: '/organization',
    templateUrl: 'app/common/organization-list.tpl.html',
    context: 'user',
    heading: 'Organizations endorsed by the user'
  })
  .state('user.dataset', {
    url: '/dataset',
    templateUrl: 'app/common/dataset-list.tpl.html',
    context: 'user',
    heading: 'Datasets published through the Users endorsement'
  })
  .state('user.installation', {
    url: '/installation',
    templateUrl: 'app/common/installation-list.tpl.html',
    context: 'user',
    heading: 'Installations endorsed by the user'
  })
}])

/**
 * The single detail controller
 */
.controller('UserCtrl', function ($scope, $state, $stateParams, notifications, Restangular, DEFAULT_PAGE_SIZE) {
  var key = $stateParams.key;

  // shared across sub views
  $scope.counts = {};

  var load = function() {
    Restangular.one('user', key).get().then(function(user) {
      $scope.user = user;
      $scope.counts.contacts = _.size(user.contacts);
      $scope.counts.identifiers = _.size(user.identifiers);
      $scope.counts.endpoints = _.size(user.endpoints);
      $scope.counts.tags = _.size(user.tags);
      $scope.counts.machineTags = _.size(user.machineTags);
      $scope.counts.comments = _.size(user.comments);

      user.getList('organization', {limit: DEFAULT_PAGE_SIZE}).then(function(response) {
          $scope.organizations = response.results;
          $scope.counts.organizations = response.count;
        });
      user.getList('dataset', {limit: DEFAULT_PAGE_SIZE}).then(function(response) {
          $scope.datasets = response.results;
          $scope.counts.datasets = response.count;
        });
      user.getList('installation', {limit: DEFAULT_PAGE_SIZE}).then(function(response) {
          $scope.installations = response.results;
          $scope.counts.installations = response.count;
        });
      user.getList('pendingEndorsement', {limit: DEFAULT_PAGE_SIZE}).then(function(response) {
          $scope.pendingEndorsements = response.results;
          $scope.counts.pendingEndorsements = response.count;
        });
    });
  }
  load();

  // populate the dropdowns
  Restangular.all("enumeration/basic/UserType").getList().then(function(data){
    $scope.types = data;
  });
  Restangular.all("enumeration/basic/ParticipationStatus").getList().then(function(data){
    $scope.participationStatuses = data;
  });
  Restangular.all("enumeration/basic/GbifRegion").getList().then(function(data){
    $scope.gbifRegions = data;
  });
  Restangular.all("enumeration/basic/Continent").getList().then(function(data){
    $scope.continents = data;
  });
  Restangular.all("enumeration/basic/Country").getList().then(function(data){
    $scope.countries = data;
  });


	// transitions to a new view, correctly setting up the path
  $scope.transitionTo = function (target) {
    $state.transitionTo('user.' + target, { key: key, type: "user" });
  }

	$scope.save = function (user) {
    user.put().then(
      function() {
        notifications.pushForNextRoute("User successfully updated", 'info');
        $scope.transitionTo("detail");
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      }
    );
  }

  $scope.delete = function (entity) {
    entity.remove().then(
      function() {
        notifications.pushForNextRoute("User successfully deleted", 'info');
        load();
        $scope.transitionTo("detail");
      }
    );
  }

  $scope.restore = function (entity) {
    entity.deleted = undefined;
    entity.put().then(
      function() {
        notifications.pushForCurrentRoute("User successfully restored", 'info');
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      }
    );
  }


  $scope.cancelEdit = function () {
    load();
    $scope.transitionTo("detail");
  }

  // switch depending on the scope, which are visible
  $scope.getOrganizations = function() {
    if ($state.includes('user.pending')) {
      return $scope['pendingEndorsements'];
    } else {
      return $scope['organizations'];
    }
  }

  $scope.getDatasets = function() {
    return $scope['datasets'];
  }
})

.controller('UserSearchCtrl', function ($scope, $state, Restangular, DEFAULT_PAGE_SIZE) {
  var user = Restangular.all("user");
  $scope.search = function(q) {
    user.getList({q:q, limit:DEFAULT_PAGE_SIZE}).then(function(data) {
      $scope.resultsCount = data.count;
      $scope.results = data.results;
      $scope.searchString = q;
    });
  }
  $scope.search(""); // start with empty search

  $scope.openUser = function(user) {
    $state.transitionTo('user.detail', {key: user.key})
  }
})


.controller('UserCreateCtrl', function ($scope, $state, $http, notifications, Restangular) {
  Restangular.all("enumeration/basic/UserType").getList().then(function(data){
    $scope.types = data;
  });
  Restangular.all("enumeration/basic/ParticipationStatus").getList().then(function(data){
    $scope.participationStatuses = data;
  });
  Restangular.all("enumeration/basic/GbifRegion").getList().then(function(data){
    $scope.gbifRegions = data;
  });
  Restangular.all("enumeration/basic/Continent").getList().then(function(data){
    $scope.continents = data;
  });
  Restangular.all("enumeration/basic/Country").getList().then(function(data){
    $scope.countries = data;
  });

  $scope.save = function (user) {
    if (user != undefined) {
      Restangular.all("user").post(user).then(function(data) {
        notifications.pushForNextRoute("User successfully updated", 'info');
        // strip the quotes
        $state.transitionTo('user.detail', { key: data.replace(/["]/g,''), type: "user" });
      }, function(error) {
        notifications.pushForCurrentRoute(error.data, 'error');
      });
    }
  }

  $scope.cancelEdit = function() {
    $state.transitionTo('user-search.search');
  }
});
