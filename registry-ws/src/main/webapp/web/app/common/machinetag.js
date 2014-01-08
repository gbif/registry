angular.module('machinetag', ['services.notifications'])

.controller('MachinetagCtrl', function ($scope, $state, $stateParams, notifications, Restangular) {
  var type = $state.current.context; // this context should be set in the parent statemachine (e.g. dataset)
  var key = $stateParams.key; // the entity key (e.g. uuid of a dataset)
  
  var machinetags = Restangular.one(type, key).all('machinetag');
  machinetags.getList().then(function(response) {$scope.machinetags = response});

	var resetState = function() {
	  $state.transitionTo(type + '.machinetag', { key: key}); 
	}
  
  $scope.save = function(item) {
    var success = function(data) {
      notifications.pushForCurrentRoute("MachineTag successfully added", 'info');
      $scope.editing = false; // close the form
      machinetags.getList().then(function(response) {$scope.machinetags = response});
      $scope.counts.machinetags++;
      resetState(); // in case we have logged in
    };
    
    var failure = function(response) {
      notifications.pushForCurrentRoute(response.data, 'error');
    };    
    machinetags.post(item).then(success,failure);
  }
  
  $scope.delete = function(item) {
    var ngItem = _.find($scope.machinetags, function(i) {
      return item.key == i.key;
    });
    ngItem.remove().then(
      function() {
        notifications.pushForCurrentRoute("MachineTag successfully deleted", 'info');
        $scope.machinetags = _.without($scope.machinetags, ngItem);
        $scope.counts.machinetags--;
         $scope.editing = false; // close the form
        resetState(); // in case we have logged in
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      });
  }  
});