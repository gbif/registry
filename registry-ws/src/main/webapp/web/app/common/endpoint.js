angular.module('endpoint', ['services.notifications'])

.controller('EndpointCtrl', function ($scope, $state, $stateParams, notifications, Restangular) {
  var type = $state.current.context; // this context should be set in the parent statemachine (e.g. dataset)
  var key = $stateParams.key; // the entity key (e.g. uuid of a dataset)
  
  var endpoints = Restangular.one(type, key).all('endpoint');
  endpoints.getList().then(function(response) {$scope.endpoints = response});

  $scope.types = [
    'EML',
    'FEED',
    'WFS',
    'WMS',
    'TCS_RDF',
    'TCS_XML',
    'DWC_ARCHIVE',
    'DIGIR',
    'DIGIR_MANIS',
    'TAPIR',
    'BIOCASE',
    'OAI_PMH',
    'OTHER'  
  ];	
  
	var resetState = function() {
	  $state.transitionTo(type + '.endpoint', { key: key}); 
	}
  
  $scope.save = function(item) {
    var success = function(data) {
      notifications.pushForCurrentRoute("Endpoint successfully added", 'info');
      $scope.editing = false; // close the form
      endpoints.getList().then(function(response) {$scope.endpoints = response});
      $scope.counts.endpoints++;
      resetState(); // in case we have logged in
    };
    
    var failure = function(response) {
      notifications.pushForCurrentRoute(response.data, 'error');
    };    
    endpoints.post(item).then(success,failure);
  }
  
  $scope.delete = function(item) {
    var ngItem = _.find($scope.endpoints, function(i) {
      return item.key == i.key;
    });
    ngItem.remove().then(
      function() {
        notifications.pushForCurrentRoute("Endpoint successfully deleted", 'info');
        $scope.endpoints = _.without($scope.endpoints, ngItem);
        $scope.counts.endpoints--;
         $scope.editing = false; // close the form
        resetState(); // in case we have logged in
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      });
  }  
});