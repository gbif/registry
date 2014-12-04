angular.module('contact', ['services.notifications'])

.controller('ContactCtrl', function ($scope, $state, $stateParams, notifications, Restangular) {
	var type = $state.current.context; // this context should be set in the parent statemachine (e.g. dataset)
	var key = $stateParams.key; // the entity key (e.g. uuid of a dataset)

	var resetState = function() {
	  $state.transitionTo(type + '.contact', { key: key});
	}

  var contacts = Restangular.one(type, key).all('contact');
  contacts.getList().then(function(response) {$scope.contacts = response});

  Restangular.all("enumeration/basic/ContactType").getList().then(function(data){
    $scope.contactTypes = data;
  });

  Restangular.all("enumeration/basic/Country").getList().then(function(data){
    $scope.countries = data;
  });

  $scope.save = function(item) {
    var success = function(data) {
      notifications.pushForCurrentRoute("Contact successfully updated", 'info');
      $scope.close();
      if (!item.key) {
        contacts.getList().then(function(response) {$scope.contacts = response});
        $scope.counts.contacts++;
        resetState(); // in case we have logged in
      }
    };

    var failure = function(response) {
      notifications.pushForCurrentRoute(response.data, 'error');
    };

    if (item.key != null) {
      var ngItem = _.find($scope.contacts, function(i) {
        return item.key == i.key;
      });
      ngItem.put().then(success,failure);
    } else {
      Restangular.one(type, key).all("contact").post(item).then(success,failure);
    }

  }

  $scope.delete = function(item) {
    var ngItem = _.find($scope.contacts, function(i) {
      return item.key == i.key;
    });
    ngItem.remove().then(
      function() {
        notifications.pushForCurrentRoute("Contact successfully deleted", 'info');
        $scope.contacts = _.without($scope.contacts, ngItem);
        $scope.counts.contacts--;
        $scope.close();
        resetState(); // in case we have logged in
      },
      function(response) {
        notifications.pushForCurrentRoute(response.data, 'error');
      });
  }

  $scope.close = function() {
    $scope.createNew=false;
    $.each($scope.contacts, function(i,contact) {
      contact.editMode = false;
    });
  }
})


.filter('prettifyContactType', function () {
  return function(name) {
    switch (name) {
      case "TECHNICAL_POINT_OF_CONTACT": return "Technical point of contact";
      case "ADMINISTRATIVE_POINT_OF_CONTACT": return "Administrative point of contact";
      case "POINT_OF_CONTACT": return "Point of contact";
      case "ORIGINATOR": return "Originator";
      case "METADATA_AUTHOR": return "Metadata author";
      case "PRINCIPAL_INVESTIGATOR": return "Principal investigator";
      case "AUTHOR": return "Author";
      case "CONTENT_PROVIDER": return "Content provider";
      case "CUSTODIAN_STEWARD": return "Custodian steward";
      case "DISTRIBUTOR": return "Distributor";
      case "EDITOR": return "Editor";
      case "OWNER": return "Owner";
      case "PROCESSOR": return "Processor";
      case "PUBLISHER": return "Publisher";
      case "USER": return "User";
      case "PROGRAMMER": return "Programmer";
      case "DATA_ADMINISTRATOR": return "Data administrator";
      case "SYSTEM_ADMINISTRATOR": return "System administrator";
      case "TEMPORARY_HEAD_OF_DELEGATION": return "Temporary head of delegation";
      case "TEMPORARY_DELEGATE": return "Temporary delegate";
      case "REGIONAL_NODE_REPRESENTATIVE": return "Regional node representative";
      case "NODE_STAFF": return "Node staff";
      case "HEAD_OF_DELEGATION": return "Head of delegation";
      case "NODE_MANAGER": return "Node manager";
      case "ADDITIONAL_DELEGATE": return "Additional delegate";
      default: return name;
    }
  };
});
