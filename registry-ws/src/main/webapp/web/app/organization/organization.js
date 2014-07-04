angular.module('organization', [
  'restangular',
  'services.notifications',
  'contact',
  'organization',
  'identifier',
  'tag',
  'machineTag',
  'comment'])

/**
 * Nested stated provider using dot notation (item.detail has a parent of item) and the
 * nested view is rendered into the parent template ui.view div.  A single controller
 * governs the actions on the page.
 */
.config(['$stateProvider', function ($stateProvider, $stateParams, Organization) {
  $stateProvider.state('organization-search', {
    abstract: true,
    url: '/organization-search',
    templateUrl: 'app/organization/organization-search.tpl.html',
    controller: 'OrganizationSearchCtrl'
  })
  .state('organization-search.search', {
    url: '',
    templateUrl: 'app/organization/organization-results.tpl.html'
  })
  .state('organization-search.deleted', {
    url: '/deleted',
    templateUrl: 'app/organization/organization-deleted.tpl.html'
  })
  .state('organization-search.pending', {
    url: '/pending',
    templateUrl: 'app/organization/organization-pending.tpl.html'
  })
  .state('organization-search.nonPublishing', {
    url: 'nonPublishing',
    templateUrl: 'app/organization/organization-nonPublishing.tpl.html'
  })
  .state('organization-search.create', {
    url: '/create',
    templateUrl: 'app/organization/organization-edit.tpl.html',
    controller: 'OrganizationCreateCtrl'
  }).state('organization', {
    url: '/organization/{key}',
    abstract: true,
    templateUrl: 'app/organization/organization-main.tpl.html',
    controller: 'OrganizationCtrl'
  })
  .state('organization.detail', {
    url: '',
    templateUrl: 'app/organization/organization-overview.tpl.html'
  })
  .state('organization.edit', {
    url: '/edit',
    templateUrl: 'app/organization/organization-edit.tpl.html'
  })
  .state('organization.contact', {
    url: '/contact',
    templateUrl: 'app/common/contact-list.tpl.html',
    controller: "ContactCtrl",
    context: 'organization', // necessary for reusing the components
    heading: 'Organization contacts' // title for the sub pane
  })
  .state('organization.endpoint', {
    url: '/endpoint',
    templateUrl: 'app/common/endpoint-list.tpl.html',
    controller: "EndpointCtrl",
    context: 'organization',
    heading: 'Organization endpoints'
  })
  .state('organization.identifier', {
    url: '/identifier',
    templateUrl: 'app/common/identifier-list.tpl.html',
    controller: "IdentifierCtrl",
    context: 'organization',
    heading: 'Organization identifiers'
  })
  .state('organization.tag', {
    url: '/tag',
    templateUrl: 'app/common/tag-list.tpl.html',
    controller: "TagCtrl",
    context: 'organization',
    heading: 'Organization tags'
  })
  .state('organization.machineTag', {
    url: '/machineTag',
    templateUrl: 'app/common/machinetag-list.tpl.html',
    controller: "MachinetagCtrl",
    context: 'organization',
    heading: 'Organization machine tags'
  })
  .state('organization.comment', {
    url: '/comment',
    templateUrl: 'app/common/comment-list.tpl.html',
    controller: "CommentCtrl",
    context: 'organization',
    heading: 'Organization comments'
  })
  .state('organization.published', {
    url: '/published',
    templateUrl: 'app/common/dataset-list.tpl.html',
    context: 'organization',
    heading: 'Datasets published by the organization'
  })
  .state('organization.hosted', {
    url: '/hosted',
    templateUrl: 'app/common/dataset-hosted-list.tpl.html',
    context: 'organization',
    heading: 'Datasets hosted by the organization'
  })
  .state('organization.installation', {
    url: '/installation',
    templateUrl: 'app/common/installation-list.tpl.html',
    context: 'organization', // necessary for reusing the components
    heading: 'Installations hosted by the organization'
  })

}])

/**
 * The single detail controller
 */
.controller('OrganizationCtrl', function ($scope, $state, $stateParams, notifications, Restangular, DEFAULT_PAGE_SIZE, $http) {
  var key =  $stateParams.key;

  // shared across sub views
  $scope.counts = {};
  $scope.countries = Restangular.all("enumeration/basic/Country").getList();

  var load = function() {
    Restangular.one('organization', key).get()
    .then(function(organization) {
      $scope.organization = organization;
      $scope.counts.contacts = _.size(organization.contacts);
      $scope.counts.endpoints = _.size(organization.endpoints);
      $scope.counts.identifiers = _.size(organization.identifiers);
      $scope.counts.tags = _.size(organization.tags);
      $scope.counts.machineTags = _.size(organization.machineTags);
      $scope.counts.comments = _.size(organization.comments);

      organization.getList('publishedDataset', {limit: DEFAULT_PAGE_SIZE})
        .then(function(response) {
          $scope.publishedDatasets = response.results;
          $scope.counts.publishedDatasets = response.count;
        });
      organization.getList('installation', {limit: DEFAULT_PAGE_SIZE})
        .then(function(response) {
          $scope.installations = response.results;
          $scope.counts.installations = response.count;
        });
      organization.getList('hostedDataset', {limit: DEFAULT_PAGE_SIZE})
        .then(function(response) {
          $scope.hostedDatasets = response.results;
          $scope.counts.hostedDatasets = response.count;
          $.each(response.results, function(index, dataset) {
            Restangular.one('organization', dataset.publishingOrganizationKey).get().then(function(organization) {
              dataset.publishingOrganizationTitle = organization.title;
            });
          });
        });

      organization.node = Restangular.one('node', organization.endorsingNodeKey).get();
    });
  }
  load();

	// transitions to a new view, correctly setting up the path
  $scope.transitionTo = function (target) {
    $state.transitionTo('organization.' + target, { key: key, type: "organization" });
  }

	$scope.save = function (organization) {
    organization.put().then(
      function() {
        notifications.pushForNextRoute("Organization successfully updated", 'info');
        $scope.transitionTo("detail");
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      }
    );
  }

  $scope.openNode = function (nodeKey) {
    $state.transitionTo('node.detail', {key : nodeKey});
  }

  $scope.cancelEdit = function () {
    load();
    $scope.transitionTo("detail");
  }

  $scope.getDatasets = function() {
    if ($state.includes('organization.hosted')) {
      return $scope['hostedDatasets'];
    } else {
      return $scope['publishedDatasets'];
    }
  }

  $scope.delete = function (organization) {
    organization.remove().then(
      function() {
        notifications.pushForNextRoute("Organization successfully deleted", 'info');
        load();
        $scope.transitionTo("detail");
      }
    );
  }

  $scope.restore = function (organization) {
    organization.deleted = undefined;
    organization.put().then(
      function() {
        notifications.pushForCurrentRoute("Organization successfully restored", 'info');
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      }
    );
  }

  // retrieve the password for an organization
  $scope.retrievePassword = function () {
    $http( { method:'GET', url: "../organization/"+key+"/password" })
      .success(function (data) { $scope.password = data});
  }
})

.controller('OrganizationSearchCtrl', function ($scope, $state, Restangular, DEFAULT_PAGE_SIZE) {
  var organization = Restangular.all("organization");

  $scope.search = function(q) {
    organization.getList({q:q, limit:DEFAULT_PAGE_SIZE}).then(function(data) {
      $scope.resultsCount = data.count;
      $scope.results = data.results;
      $scope.searchString = q;
    });
  }
  $scope.search(""); // start with empty search

  // load quick lists
  organization.all("deleted").getList({limit:DEFAULT_PAGE_SIZE}).then(function(data) {
    $scope.deletedCount = data.count;
    $scope.deleted = data.results;
  });
  organization.all("pending").getList({limit:DEFAULT_PAGE_SIZE}).then(function(data) {
    $scope.pendingCount = data.count;
    $scope.pending = data.results;
  });
  organization.all("nonPublishing").getList({limit:DEFAULT_PAGE_SIZE}).then(function(data) {
    $scope.nonPublishingCount = data.count;
    $scope.nonPublishing = data.results;
  });

  $scope.openOrganization = function(organization) {
    $state.transitionTo('organization.detail', {key: organization.key})
  }
})


.controller('OrganizationCreateCtrl', function ($scope, $state, notifications, Restangular) {

  $scope.countries = Restangular.all("enumeration/basic/Country").getList();

  $scope.save = function (organization) {
    if (organization != undefined) {
      Restangular.all("organization").post(organization).then(function(data) {
        notifications.pushForNextRoute("Organization successfully updated", 'info');
        // strip the quotes
        $state.transitionTo('organization.detail', { key: data.replace(/["]/g,''), type: "organization" });
      }, function(error) {
        notifications.pushForCurrentRoute(error.data, 'error');
      });
    }
  }

  $scope.cancelEdit = function() {
    $state.transitionTo('organization-search.search');
  }
});
