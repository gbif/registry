angular.module('tag', ['services.notifications'])

.controller('TagCtrl', function ($scope, $state, $stateParams, notifications, Restangular) {
  var type = $state.current.context; // this context should be set in the parent statemachine (e.g. dataset)
  var key = $stateParams.key; // the entity key (e.g. uuid of a dataset)
  
  var tags = Restangular.one(type, key).all('tag');
  tags.getList().then(function(response) {$scope.tags = response});

	var resetState = function() {
	  $state.transitionTo(type + '.tag', { key: key}); 
	}
  
  $scope.save = function(item) {
    var success = function(data) {
      notifications.pushForCurrentRoute("Tag successfully added", 'info');
      $scope.editing = false; // close the form
      if (!item.key) {
        tags.getList().then(function(response) {$scope.tags = response});
        $scope.counts.tags++;
        resetState(); // in case we have logged in
      }
    };
    
    var failure = function(response) {
      notifications.pushForCurrentRoute(response.data, 'error');
    };    
    tags.post(item).then(success,failure);
  }
  
  $scope.delete = function(item) {
    var ngItem = _.find($scope.tags, function(i) {
      return item.key == i.key;
    });
    ngItem.remove().then(
      function() {
        notifications.pushForCurrentRoute("Tag successfully deleted", 'info');
        $scope.tags = _.without($scope.tags, ngItem);
        $scope.counts.tags--;
         $scope.editing = false; // close the form
        resetState(); // in case we have logged in
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      });
  }  
});