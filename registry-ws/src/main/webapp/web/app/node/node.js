angular.module('node', [
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
  $stateProvider.state('node-search', {
    abstract: true,
    url: '/node-search',
    templateUrl: 'app/node/node-search.tpl.html',
    controller: 'NodeSearchCtrl'
  })
  .state('node-search.search', {
    url: '',
    templateUrl: 'app/node/node-results.tpl.html'
  })
  .state('node-search.create', {
    url: '/create',
    templateUrl: 'app/node/node-edit.tpl.html',
    controller: 'NodeCreateCtrl',
    resolve: {
      item: function() { return {} } // load it with an empty one
    }
  })
  .state('node', {
    url: '/node/{key}',
    abstract: true,
    templateUrl: 'app/node/node-main.tpl.html',
    controller: 'NodeCtrl'
  })
  .state('node.detail', {
    url: '',
    templateUrl: 'app/node/node-overview.tpl.html'
  })
  .state('node.edit', {
    url: '/edit',
    templateUrl: 'app/node/node-edit.tpl.html'
  })
  .state('node.contact', {
    url: '/contact',
    templateUrl: 'app/node/contact-list.tpl.html' // not common since read only
  })
  .state('node.identifier', {
    url: '/identifier',
    templateUrl: 'app/common/identifier-list.tpl.html',
    controller: "IdentifierCtrl",
    context: 'node', // necessary for reusing the components
    heading: 'Node identifiers' // title for the sub pane
  })
  .state('node.endpoint', {
    url: '/endpoint',
    templateUrl: 'app/common/endpoint-list.tpl.html',
    controller: "EndpointCtrl",
    context: 'node',
    heading: 'Node endpoints'
  })
  .state('node.tag', {
    url: '/tag',
    templateUrl: 'app/common/tag-list.tpl.html',
    controller: "TagCtrl",
    context: 'node',
    heading: 'Node tags'
  })
  .state('node.machineTag', {
    url: '/machineTag',
    templateUrl: 'app/common/machinetag-list.tpl.html',
    controller: "MachinetagCtrl",
    context: 'node',
    heading: 'Node machine tags'
  })
  .state('node.comment', {
    url: '/comment',
    templateUrl: 'app/common/comment-list.tpl.html',
    controller: "CommentCtrl",
    context: 'node',
    heading: 'Node comments'
  })
  .state('node.pending', {
    url: '/pending',
    templateUrl: 'app/common/organization-list.tpl.html',
    context: 'node',
    heading: 'Organizations awaiting endorsement by the node'
  })
  .state('node.organization', {
    url: '/organization',
    templateUrl: 'app/common/organization-list.tpl.html',
    context: 'node',
    heading: 'Organizations endorsed by the node'
  })
  .state('node.dataset', {
    url: '/dataset',
    templateUrl: 'app/common/dataset-list.tpl.html',
    context: 'node',
    heading: 'Datasets published through the Nodes endorsement'
  })
  .state('node.installation', {
    url: '/installation',
    templateUrl: 'app/common/installation-list.tpl.html',
    context: 'node',
    heading: 'Installations endorsed by the node'
  })
}])

/**
 * The single detail controller
 */
.controller('NodeCtrl', function ($scope, $state, $stateParams, notifications, Restangular, DEFAULT_PAGE_SIZE) {
  var key = $stateParams.key;

  // shared across sub views
  $scope.counts = {};

  var load = function() {
    Restangular.one('node', key).get().then(function(node) {
      $scope.node = node;
      $scope.counts.contacts = _.size(node.contacts);
      $scope.counts.identifiers = _.size(node.identifiers);
      $scope.counts.endpoints = _.size(node.endpoints);
      $scope.counts.tags = _.size(node.tags);
      $scope.counts.machineTags = _.size(node.machineTags);
      $scope.counts.comments = _.size(node.comments);

      node.getList('organization', {limit: DEFAULT_PAGE_SIZE}).then(function(response) {
          $scope.organizations = response.results;
          $scope.counts.organizations = response.count;
        });
      node.getList('dataset', {limit: DEFAULT_PAGE_SIZE}).then(function(response) {
          $scope.datasets = response.results;
          $scope.counts.datasets = response.count;
        });
      node.getList('installation', {limit: DEFAULT_PAGE_SIZE}).then(function(response) {
          $scope.installations = response.results;
          $scope.counts.installations = response.count;
        });
      node.getList('pendingEndorsement', {limit: DEFAULT_PAGE_SIZE}).then(function(response) {
          $scope.pendingEndorsements = response.results;
          $scope.counts.pendingEndorsements = response.count;
        });
    });
  }
  load();

  // populate the dropdowns
  $scope.types = Restangular.all("enumeration/basic/NodeType").getList();
  $scope.participationStatuses = Restangular.all("enumeration/basic/ParticipationStatus").getList();
  $scope.gbifRegions = Restangular.all("enumeration/basic/GbifRegion").getList();
  $scope.continents = Restangular.all("enumeration/basic/Continent").getList();
  $scope.countries = Restangular.all("enumeration/basic/Country").getList();


	// transitions to a new view, correctly setting up the path
  $scope.transitionTo = function (target) {
    $state.transitionTo('node.' + target, { key: key, type: "node" });
  }

	$scope.save = function (node) {
    node.put().then(
      function() {
        notifications.pushForNextRoute("Node successfully updated", 'info');
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
        notifications.pushForNextRoute("Node successfully deleted", 'info');
        load();
        $scope.transitionTo("detail");
      }
    );
  }

  $scope.restore = function (entity) {
    entity.deleted = undefined;
    entity.put().then(
      function() {
        notifications.pushForCurrentRoute("Node successfully restored", 'info');
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
    if ($state.includes('node.pending')) {
      return $scope['pendingEndorsements'];
    } else {
      return $scope['organizations'];
    }
  }

  $scope.getDatasets = function() {
    return $scope['datasets'];
  }
})

.controller('NodeSearchCtrl', function ($scope, $state, Restangular, DEFAULT_PAGE_SIZE) {
  var node = Restangular.all("node");
  $scope.search = function(q) {
    node.getList({q:q, limit:DEFAULT_PAGE_SIZE}).then(function(data) {
      $scope.resultsCount = data.count;
      $scope.results = data.results;
      $scope.searchString = q;
    });
  }
  $scope.search(""); // start with empty search

  $scope.openNode = function(node) {
    $state.transitionTo('node.detail', {key: node.key})
  }
})


.controller('NodeCreateCtrl', function ($scope, $state, $http, notifications, Restangular) {
  $scope.types = Restangular.all("enumeration/basic/NodeType").getList();
  $scope.participationStatuses = Restangular.all("enumeration/basic/ParticipationStatus").getList();
  $scope.gbifRegions = Restangular.all("enumeration/basic/GbifRegion").getList();
  $scope.continents = Restangular.all("enumeration/basic/Continent").getList();
  $scope.countries = Restangular.all("enumeration/basic/Country").getList();

  $scope.save = function (node) {
    if (node != undefined) {
      Restangular.all("node").post(node).then(function(data) {
        notifications.pushForNextRoute("Node successfully updated", 'info');
        // strip the quotes
        $state.transitionTo('node.detail', { key: data.replace(/["]/g,''), type: "node" });
      }, function(error) {
        notifications.pushForCurrentRoute(error.data, 'error');
      });
    }
  }

  $scope.cancelEdit = function() {
    $state.transitionTo('node-search.search');
  }
});
