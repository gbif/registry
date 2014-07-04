angular.module('installation', [
  'restangular',
  'services.notifications',
  'contact',
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
   $stateProvider.state('installation-search', {
    abstract: true,
    url: '/installation-search',
    templateUrl: 'app/installation/installation-search.tpl.html',
    controller: 'InstallationSearchCtrl'
  })
  .state('installation-search.search', {
    url: '',
    templateUrl: 'app/installation/installation-results.tpl.html'
  })
  .state('installation-search.deleted', {
    url: '/deleted',
    templateUrl: 'app/installation/installation-deleted.tpl.html'
  })
  .state('installation-search.nonPublishing', {
    url: 'nonPublishing',
    templateUrl: 'app/installation/installation-nonPublishing.tpl.html'
  })
  .state('installation-search.create', {
    url: '/create',
    templateUrl: 'app/installation/installation-edit.tpl.html',
    controller: 'InstallationCreateCtrl'
  })
  .state('installation', {
    url: '/installation/{key}',
    abstract: true,
    templateUrl: 'app/installation/installation-main.tpl.html',
    controller: 'InstallationCtrl'
  })
  .state('installation.detail', {
    url: '',
    templateUrl: 'app/installation/installation-overview.tpl.html'
  })
  .state('installation.edit', {
    url: '/edit',
    templateUrl: 'app/installation/installation-edit.tpl.html'
  })
  .state('installation.contact', {
    url: '/contact',
    templateUrl: 'app/common/contact-list.tpl.html',
    controller: "ContactCtrl",
    context: 'installation', // necessary for reusing the components
    heading: 'Installation contacts' // title for the sub pane
  })
  .state('installation.endpoint', {
    url: '/endpoint',
    templateUrl: 'app/common/endpoint-list.tpl.html',
    controller: "EndpointCtrl",
    context: 'installation',
    heading: 'Installation endpoints'
  })
  .state('installation.identifier', {
    url: '/identifier',
    templateUrl: 'app/common/identifier-list.tpl.html',
    controller: "IdentifierCtrl",
    context: 'installation',
    heading: 'Installation identifiers'
  })
  .state('installation.tag', {
    url: '/tag',
    templateUrl: 'app/common/tag-list.tpl.html',
    controller: "TagCtrl",
    context: 'installation',
    heading: 'Installation tags'
  })
  .state('installation.machineTag', {
    url: '/machineTag',
    templateUrl: 'app/common/machinetag-list.tpl.html',
    controller: "MachinetagCtrl",
    context: 'installation',
    heading: 'Installation machine tags'
  })
  .state('installation.comment', {
    url: '/comment',
    templateUrl: 'app/common/comment-list.tpl.html',
    controller: "CommentCtrl",
    context: 'installation',
    heading: 'Installation comments'
  })
  .state('installation.dataset', {
    url: '/dataset',
    templateUrl: 'app/common/dataset-list.tpl.html',
    context: 'installation',
    heading: 'Datasets served by the installation'
  })
  .state('installation.sync', {
    url: '/sync',
    templateUrl: 'app/installation/installation-sync.tpl.html'
  })
}])

.filter('prettifyType', function () {
  return function(name) {
    switch (name) {
      case "BIOCASE_INSTALLATION": return "BioCASe";
      case "TAPIR_INSTALLATION": return "TAPIR";
      case "HTTP_INSTALLATION": return "HTTP";
      case "IPT_INSTALLATION": return "IPT";
      case "DIGIR_INSTALLATION": return "DiGIR";
      default: return name;
    }
  };
})

/**
 * The single detail controller
 */
.controller('InstallationCtrl', function ($scope, $state, $stateParams, notifications, Restangular, DEFAULT_PAGE_SIZE) {
  var key =  $stateParams.key;

  // shared across sub views
  $scope.counts = {};

  var load = function() {
    Restangular.one('installation', key).get()
    .then(function(installation) {
      $scope.installation = installation;
      $scope.counts.contacts = _.size(installation.contacts);
      $scope.counts.endpoints = _.size(installation.endpoints);
      $scope.counts.identifiers = _.size(installation.identifiers);
      $scope.counts.tags = _.size(installation.tags);
      $scope.counts.machineTags = _.size(installation.machineTags);
      $scope.counts.comments = _.size(installation.comments);

      // served datasets
      installation.getList('dataset', {limit: DEFAULT_PAGE_SIZE})
        .then(function(response) {
          installation.datasets = response.results;
          $scope.counts.datasets = response.count;
        });

      // syncs
      installation.getList('metasync', {limit: DEFAULT_PAGE_SIZE})
        .then(function(response) {
          installation.syncs = response.results;
          $scope.counts.syncs = response.count;
        });

      // the hosting organization
      installation.organization = Restangular.one('organization', installation.organizationKey).get();
    });
  }
  load();

  // populate the dropdowns
  $scope.installationTypes = Restangular.all("enumeration/basic/InstallationType").getList();

	// transitions to a new view, correctly setting up the path
  $scope.transitionTo = function (target) {
    $state.transitionTo('installation.' + target, { key: key, type: "installation" });
  }

	$scope.save = function (installation) {
    installation.put().then(
      function() {
        notifications.pushForNextRoute("Installation successfully updated", 'info');
        $scope.transitionTo("detail");
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      }
    );
  }

  $scope.openOrganization = function (organizationKey) {
    $state.transitionTo('organization.detail', {key : organizationKey});
  }

  $scope.cancelEdit = function () {
    load();
    $scope.transitionTo("detail");
  }

  $scope.delete = function (installation) {
    installation.remove().then(
      function() {
        notifications.pushForNextRoute("Installation successfully deleted", 'info');
        load();
        $scope.transitionTo("detail");
      }
    );
  }

  $scope.restore = function (installation) {
    installation.deleted = undefined;
    installation.put().then(
      function() {
        notifications.pushForCurrentRoute("Installation successfully restored", 'info');
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      }
    );
  }

  $scope.getDatasets = function () {
    if ($scope.installation) return $scope.installation.datasets;
  }

  $scope.synchronize = function (installation)  {
		Restangular.one('installation', key).one("synchronize").post().then(
		  function(response) {
		    if ($state.includes('login'))
		      notifications.pushForNextRoute("Installation synchronizing", 'info');
		    else
		      notifications.pushForCurrentRoute("Installation synchronizing", 'info');
		    $scope.transitionTo("detail");

		  },function(error) {
		    notifications.pushForCurrentRoute(response.data, 'error');
		  }
		);
  }
})

/**
 * The search controller
 */
.controller('InstallationSearchCtrl', function ($scope, $state, Restangular, DEFAULT_PAGE_SIZE) {
  var installation = Restangular.all("installation");

  $scope.search = function(q) {
    installation.getList({q:q, limit:DEFAULT_PAGE_SIZE}).then(function(data) {
      $scope.resultsCount = data.count;
      $scope.results = data.results;
      $scope.searchString = q;
    });
  }
  $scope.search(""); // start with empty search

  installation.all("deleted").getList({limit:DEFAULT_PAGE_SIZE}).then(function(data) {
    $scope.deletedCount = data.count;
    $scope.deleted = data.results;
  });
  installation.all("nonPublishing").getList({limit:DEFAULT_PAGE_SIZE}).then(function(data) {
    $scope.nonPublishingCount = data.count;
    $scope.nonPublishing = data.results;
  });

  $scope.openInstallation = function(installation) {
    $state.transitionTo('installation.detail', {key: installation.key})
  }
})

/**
 * The create controller
 */
.controller('InstallationCreateCtrl', function ($scope, $state, notifications, Restangular) {
  $scope.installationTypes = Restangular.all("enumeration/basic/InstallationType").getList();

  $scope.save = function (installation) {
    // ignore empty forms
    if (installation != undefined) {
      Restangular.all("installation").post(installation).then(function(data) {
        notifications.pushForNextRoute("Installation successfully updated", 'info');
        // strip the quotes
        $state.transitionTo('installation.detail', { key: data.replace(/["]/g,''), type: "installation" });
      }, function(error) {
        notifications.pushForCurrentRoute(error.data, 'error');
      });
    }
  }

  $scope.cancelEdit = function() {
    $state.transitionTo('installation-search.search');
  }
});
