angular.module('identifier', ['services.notifications'])

.controller('IdentifierCtrl', function ($scope, $state, $stateParams, notifications, Restangular) {
  var type = $state.current.context; // this context should be set in the parent statemachine (e.g. dataset)
  var key = $stateParams.key; // the entity key (e.g. uuid of a dataset)
  
  var identifiers = Restangular.one(type, key).all('identifier');
  identifiers.getList().then(function(response) {$scope.identifiers = response});

  $scope.types = [
    'SOURCE_ID',
    'URL',
    'LSID',
    'HANDLER',
    'DOI',
    'UUID',
    'FTP',
    'URI',
    'UNKNOWN',
    'GBIF_PORTAL',
    'GBIF_NODE',
    'GBIF_PARTICIPANT'
  ];	
  
	var resetState = function() {
	  $state.transitionTo(type + '.identifier', { key: key}); 
	}
  
  $scope.save = function(item) {
    var success = function(data) {
      notifications.pushForCurrentRoute("Identifier successfully added", 'info');
      $scope.editing = false; // close the form
      identifiers.getList().then(function(response) {$scope.identifiers = response});
      $scope.counts.identifiers++;
      resetState(); // in case we have logged in
    };
    
    var failure = function(response) {
      notifications.pushForCurrentRoute(response.data, 'error');
    };    
    identifiers.post(item).then(success,failure);
  }
  
  $scope.delete = function(item) {
    var ngItem = _.find($scope.identifiers, function(i) {
      return item.key == i.key;
    });
    ngItem.remove().then(
      function() {
        notifications.pushForCurrentRoute("Identifier successfully deleted", 'info');
        $scope.identifiers = _.without($scope.identifiers, ngItem);
        $scope.counts.identifiers--;
         $scope.editing = false; // close the form
        resetState(); // in case we have logged in
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      });
  }  
});