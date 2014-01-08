angular.module('comment', ['services.notifications'])

.controller('CommentCtrl', function ($scope, $state, $stateParams, notifications, Restangular) {
  var type = $state.current.context; // this context should be set in the parent statemachine (e.g. dataset)
  var key = $stateParams.key; // the entity key (e.g. uuid of a dataset)
  
  var comments = Restangular.one(type, key).all('comment');
  comments.getList().then(function(response) {$scope.comments = response});

	var resetState = function() {
	  $state.transitionTo(type + '.comment', { key: key}); 
	}
  
  $scope.save = function(item) {
    var success = function(data) {
      notifications.pushForCurrentRoute("Comment successfully added", 'info');
      $scope.editing = false; // close the form
      comments.getList().then(function(response) {$scope.comments = response});
      $scope.counts.comments++;
      resetState(); // in case we have logged in
    };
    
    var failure = function(response) {
      notifications.pushForCurrentRoute(response.data, 'error');
    };    
    comments.post(item).then(success,failure);
  }
  
  $scope.delete = function(item) {
    var ngItem = _.find($scope.comments, function(i) {
      return item.key == i.key;
    });
    ngItem.remove().then(
      function() {
        notifications.pushForCurrentRoute("Comment successfully deleted", 'info');
        $scope.comments = _.without($scope.comments, ngItem);
        $scope.counts.comments--;
         $scope.editing = false; // close the form
        resetState(); // in case we have logged in
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      });
  }  
});