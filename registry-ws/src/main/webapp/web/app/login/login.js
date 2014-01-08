/**
 * Login works as follows.
 * HTTP requests are intercepted, and should a 401 be observed it kicks in.  See http-auth-interceptor.js
 * The user is presented with a login form, and the HTTP Basic auth string is built and stored as a cookie (see this file)
 * The interceptor retries the request. 
 * Repeat and rinse until you don't get 401's anymore.
 */
angular.module('login', ['ngCookies'])

.config(['$stateProvider', function ($stateProvider, $stateParams, Installation) {
  $stateProvider.state('login', {
    url: '/login',  
    templateUrl: 'app/login/login.tpl.html',
    controller: 'LoginCtrl'
  })
}])

// a simple Base64 function
// taken from http://wemadeyoulook.at/blogg/implementing-basic-http-authentication-http-requests-angular/
.factory('Base64', function() {
    var keyStr = 'ABCDEFGHIJKLMNOP' +
        'QRSTUVWXYZabcdef' +
        'ghijklmnopqrstuv' +
        'wxyz0123456789+/' +
        '=';
    return {
        encode: function (input) {
            var output = "";
            var chr1, chr2, chr3 = "";
            var enc1, enc2, enc3, enc4 = "";
            var i = 0;
 
            do {
                chr1 = input.charCodeAt(i++);
                chr2 = input.charCodeAt(i++);
                chr3 = input.charCodeAt(i++);
 
                enc1 = chr1 >> 2;
                enc2 = ((chr1 & 3) << 4) | (chr2 >> 4);
                enc3 = ((chr2 & 15) << 2) | (chr3 >> 6);
                enc4 = chr3 & 63;
 
                if (isNaN(chr2)) {
                    enc3 = enc4 = 64;
                } else if (isNaN(chr3)) {
                    enc4 = 64;
                }
 
                output = output +
                    keyStr.charAt(enc1) +
                    keyStr.charAt(enc2) +
                    keyStr.charAt(enc3) +
                    keyStr.charAt(enc4);
                chr1 = chr2 = chr3 = "";
                enc1 = enc2 = enc3 = enc4 = "";
            } while (i < input.length);
 
            return output;
        },
 
        decode: function (input) {
            var output = "";
            var chr1, chr2, chr3 = "";
            var enc1, enc2, enc3, enc4 = "";
            var i = 0;
 
            // remove all characters that are not A-Z, a-z, 0-9, +, /, or =
            var base64test = /[^A-Za-z0-9\+\/\=]/g;
            if (base64test.exec(input)) {
                alert("There were invalid base64 characters in the input text.\n" +
                    "Valid base64 characters are A-Z, a-z, 0-9, '+', '/',and '='\n" +
                    "Expect errors in decoding.");
            }
            input = input.replace(/[^A-Za-z0-9\+\/\=]/g, "");
 
            do {
                enc1 = keyStr.indexOf(input.charAt(i++));
                enc2 = keyStr.indexOf(input.charAt(i++));
                enc3 = keyStr.indexOf(input.charAt(i++));
                enc4 = keyStr.indexOf(input.charAt(i++));
 
                chr1 = (enc1 << 2) | (enc2 >> 4);
                chr2 = ((enc2 & 15) << 4) | (enc3 >> 2);
                chr3 = ((enc3 & 3) << 6) | enc4;
 
                output = output + String.fromCharCode(chr1);
 
                if (enc3 != 64) {
                    output = output + String.fromCharCode(chr2);
                }
                if (enc4 != 64) {
                    output = output + String.fromCharCode(chr3);
                }
 
                chr1 = chr2 = chr3 = "";
                enc1 = enc2 = enc3 = enc4 = "";
 
            } while (i < input.length);
 
            return output;
        }
    };
})

// builds the authentication string, storing as a cookie
// taken from http://wemadeyoulook.at/blogg/implementing-basic-http-authentication-http-requests-angular/
.factory('Auth', ['Base64', '$cookieStore', '$http', '$rootScope', function (Base64, $cookieStore, $http, $rootScope) {
    // initialize to whatever is in the cookie, if anything
    if ($cookieStore.get('authdata') !== undefined) {
      $http.defaults.headers.common['Authorization'] = 'Basic ' + $cookieStore.get('authdata');
    }
    
 
    return {
      setCredentials: function (username, password) {
        var encoded = Base64.encode(username + ':' + password);
        $http.defaults.headers.common.Authorization = 'Basic ' + encoded;
        $cookieStore.put('authdata', encoded);
        $rootScope.isLoggedIn = true;        
      },
      clearCredentials: function () {
        document.execCommand("ClearAuthenticationCache");
        $cookieStore.remove('authdata');
        $http.defaults.headers.common.Authorization = 'Basic ';
        $rootScope.isLoggedIn = false;        
      }
    };
}])

.controller('LoginCtrl', function ($scope, $state, $http, Auth, authService) {
  $scope.account = {};
  // does not actually do a login, but rather sets the basic http secret string which 
  // will be automatically set on all requests
  $scope.login = function(account) {
    Auth.clearCredentials();
    Auth.setCredentials(account.username, account.password);
    authService.loginConfirmed(); // trigger a retry
  }
});