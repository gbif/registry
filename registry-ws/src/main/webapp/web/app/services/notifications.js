/**
 * A generic notification service for capturing things like validation errors etc.
 */
angular.module('services.notifications', []).factory('notifications', ['$rootScope', function ($rootScope) {
  var notifications = {
    'STICKY' : [],
    'ROUTE_CURRENT' : [],
    'ROUTE_NEXT' : []
  };
  var notificationsService = {};

  var addNotification = function (notificationsArray, notificationObj) {
    if (!angular.isObject(notificationObj)) {
      throw new Error("Only object can be added to the notification service");
    }
    
    // change in functionality - clear the list, and replace, rather than append
    notificationsArray.length=0;
    notificationsArray.push(notificationObj);
    return notificationObj;
  };

  $rootScope.$on('$stateChangeSuccess', function () {
    notifications.ROUTE_CURRENT.length = 0;
    notifications.ROUTE_CURRENT = angular.copy(notifications.ROUTE_NEXT);
    notifications.ROUTE_NEXT.length = 0;
  });

  notificationsService.getCurrent = function(){
    return [].concat(notifications.STICKY, notifications.ROUTE_CURRENT);
  };

  notificationsService.pushSticky = function(message, type) {
    return addNotification(notifications.STICKY, toNotification(message,type));
  };

  notificationsService.pushForCurrentRoute = function(message, type) {
    return addNotification(notifications.ROUTE_CURRENT, toNotification(message,type));
  };

  notificationsService.pushForNextRoute = function(message, type) {
    return addNotification(notifications.ROUTE_NEXT, toNotification(message,type));
  };

  notificationsService.remove = function(notification){
    angular.forEach(notifications, function (notificationsByType) {
      var idx = notificationsByType.indexOf(notification);
      if (idx>-1){
        notificationsByType.splice(idx,1);
      }
    });
  };

  notificationsService.removeAll = function(){
    angular.forEach(notifications, function (notificationsByType) {
      notificationsByType.length = 0;
    });
  };
  
  // create a notification object
  var toNotification = function(msg, type) {
     return angular.extend({
       message: msg,
       type: type
     });
  };  
  
  return notificationsService;
}]);