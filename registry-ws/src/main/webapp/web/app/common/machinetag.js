angular.module('machineTag', ['services.notifications'])

.controller('MachinetagCtrl', function ($scope, $state, $stateParams, notifications, Restangular) {
  var type = $state.current.context; // this context should be set in the parent statemachine (e.g. dataset)
  var key = $stateParams.key; // the entity key (e.g. uuid of a dataset)

  var machineTags = Restangular.one(type, key).all('machineTag');
  machineTags.getList().then(function(response) {$scope.machineTags = response});

	var resetState = function() {
	  $state.transitionTo(type + '.machineTag', { key: key});
	}

  $scope.save = function(item) {
    var success = function(data) {
      notifications.pushForCurrentRoute("MachineTag successfully added", 'info');
      $scope.editing = false; // close the form
      machineTags.getList().then(function(response) {$scope.machineTags = response});
      $scope.counts.machineTags++;
      resetState(); // in case we have logged in
    };

    var failure = function(response) {
      notifications.pushForCurrentRoute(response.data, 'error');
    };
    machineTags.post(item).then(success,failure);
  }

  $scope.delete = function(item) {
    var ngItem = _.find($scope.machineTags, function(i) {
      return item.key == i.key;
    });
    ngItem.remove().then(
      function() {
        notifications.pushForCurrentRoute("MachineTag successfully deleted", 'info');
        $scope.machineTags = _.without($scope.machineTags, ngItem);
        $scope.counts.machineTags--;
         $scope.editing = false; // close the form
        resetState(); // in case we have logged in
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      });
  }
});
