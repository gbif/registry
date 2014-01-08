angular.module('app', [
  'ui.compat', // the stateful angular router
  'restangular', // for REST calls
  'ngSanitize', // for the like	 of bind-html
  'ngCookies', // for security
  'http-auth-interceptor', // intercepts 403 responses, and triggers login
  'services.notifications',
  'home',
  'login',
  'search',
  'organization',
  'dataset',
  'node',
  'installation'
  ])

.config(['$routeProvider', 'RestangularProvider', '$httpProvider', function ($routeProvider, RestangularProvider, $httpProvider) {
  // TODO: no idea, why angular starts up redirecting to #/index.html, but this adds a second redirect
  $routeProvider.when('/index.html', {redirectTo: '/home'});
  
  // relative to /web brings us up to the root
  // should this be run outside of the registry-ws project, this will need changed
  RestangularProvider.setBaseUrl("../"); 
    
  // all GBIF entities use "key" and not "id" as the id, and this is used inn routing
  RestangularProvider.setRestangularFields({
    id: "key"
  });  
  
  // we really do not want 401 responses (or the dreaded browser login window appears)
  $httpProvider.defaults.headers.common['gbif-prefer-403-over-401']='true';
}])

// app constants are global in scope 
.constant('DEFAULT_PAGE_SIZE', 50)

.controller('AppCtrl', function ($scope, notifications, $state, $rootScope, notifications, $cookieStore, authService, Auth) {
  // register global notifications once
  $scope.notifications = notifications;

  $scope.removeNotification = function (notification) {
    notifications.remove(notification);
  };
  
  $rootScope.$on('event:auth-loginRequired', function() {
    notifications.pushForNextRoute("Requires account with administrative rights", 'error');
    $rootScope.isLoggedIn = false;        
    $state.transitionTo("login");
  });
    
  $rootScope.logout = function() {
    Auth.clearCredentials();
  }
  
  $rootScope.isLoggedIn = authService.isLoggedIn(); // initialize with a default
})

// a safe array sizing filter  
.filter('size', function() {
  return function(input) {
    if (input) {
      return  _.size(input || {});
    } else {
      return 0;
    }    
  }
})

.run(['$rootScope', '$state', '$stateParams', function ($rootScope,   $state,   $stateParams) {
  $rootScope.$state = $state;
  $rootScope.$stateParams = $stateParams;
}])

.filter('prettifyCountry', function () {
  return function(name) {
    switch (name) {
      case "AD": return "Andorra";
      case "AE": return "United Arab Emirates";
      case "AF": return "Afghanistan";
      case "AG": return "Antigua and Barbuda";
      case "AI": return "Anguilla";
      case "AL": return "Albania";
      case "AM": return "Armenia";
      case "AO": return "Angola";
      case "AQ": return "Antarctica";
      case "AR": return "Argentina";
      case "AS": return "American Samoa";
      case "AT": return "Austria";
      case "AU": return "Australia";
      case "AW": return "Aruba";
      case "AX": return "Åland Islands";
      case "AZ": return "Azerbaijan";
      case "BA": return "Bosnia and Herzegovina";
      case "BB": return "Barbados";
      case "BD": return "Bangladesh";
      case "BE": return "Belgium";
      case "BF": return "Burkina Faso";
      case "BG": return "Bulgaria";
      case "BH": return "Bahrain";
      case "BI": return "Burundi";
      case "BJ": return "Benin";
      case "BL": return "Saint Barthélemy";
      case "BM": return "Bermuda";
      case "BN": return "Brunei Darussalam";
      case "BO": return "Bolivia, Plurinational State of";
      case "BQ": return "Bonaire, Sint Eustatius and Saba";
      case "BR": return "Brazil";
      case "BS": return "Bahamas";
      case "BT": return "Bhutan";
      case "BV": return "Bouvet Island";
      case "BW": return "Botswana";
      case "BY": return "Belarus";
      case "BZ": return "Belize";
      case "CA": return "Canada";
      case "CC": return "Cocos (Keeling) Islands";
      case "CD": return "Congo, the Democratic Republic of the";
      case "CF": return "Central African Republic";
      case "CG": return "Congo";
      case "CH": return "Switzerland";
      case "CI": return "Côte d'Ivoire";
      case "CK": return "Cook Islands";
      case "CL": return "Chile";
      case "CM": return "Cameroon";
      case "CN": return "China";
      case "CO": return "Colombia";
      case "CR": return "Costa Rica";
      case "CU": return "Cuba";
      case "CV": return "Cape Verde";
      case "CW": return "Curaçao";
      case "CX": return "Christmas Island";
      case "CY": return "Cyprus";
      case "CZ": return "Czech Republic";
      case "DE": return "Germany";
      case "DJ": return "Djibouti";
      case "DK": return "Denmark";
      case "DM": return "Dominica";
      case "DO": return "Dominican Republic";
      case "DZ": return "Algeria";
      case "EC": return "Ecuador";
      case "EE": return "Estonia";
      case "EG": return "Egypt";
      case "EH": return "Western Sahara";
      case "ER": return "Eritrea";
      case "ES": return "Spain";
      case "ET": return "Ethiopia";
      case "FI": return "Finland";
      case "FJ": return "Fiji";
      case "FK": return "Falkland Islands (Malvinas)";
      case "FM": return "Micronesia, Federated States of";
      case "FO": return "Faroe Islands";
      case "FR": return "France";
      case "GA": return "Gabon";
      case "GB": return "United Kingdom";
      case "GD": return "Grenada";
      case "GE": return "Georgia";
      case "GF": return "French Guiana";
      case "GG": return "Guernsey";
      case "GH": return "Ghana";
      case "GI": return "Gibraltar";
      case "GL": return "Greenland";
      case "GM": return "Gambia";
      case "GN": return "Guinea";
      case "GP": return "Guadeloupe";
      case "GQ": return "Equatorial Guinea";
      case "GR": return "Greece";
      case "GS": return "South Georgia and the South Sandwich Islands";
      case "GT": return "Guatemala";
      case "GU": return "Guam";
      case "GW": return "Guinea-Bissau";
      case "GY": return "Guyana";
      case "HK": return "Hong Kong";
      case "HM": return "Heard Island and McDonald Islands";
      case "HN": return "Honduras";
      case "HR": return "Croatia";
      case "HT": return "Haiti";
      case "HU": return "Hungary";
      case "ID": return "Indonesia";
      case "IE": return "Ireland";
      case "IL": return "Israel";
      case "IM": return "Isle of Man";
      case "IN": return "India";
      case "IO": return "British Indian Ocean Territory";
      case "IQ": return "Iraq";
      case "IR": return "Iran, Islamic Republic of";
      case "IS": return "Iceland";
      case "IT": return "Italy";
      case "JE": return "Jersey";
      case "JM": return "Jamaica";
      case "JO": return "Jordan";
      case "JP": return "Japan";
      case "KE": return "Kenya";
      case "KG": return "Kyrgyzstan";
      case "KH": return "Cambodia";
      case "KI": return "Kiribati";
      case "KM": return "Comoros";
      case "KN": return "Saint Kitts and Nevis";
      case "KP": return "Korea, Democratic People's Republic of";
      case "KR": return "Korea, Republic of";
      case "KW": return "Kuwait";
      case "KY": return "Cayman Islands";
      case "KZ": return "Kazakhstan";
      case "LA": return "Lao People's Democratic Republic";
      case "LB": return "Lebanon";
      case "LC": return "Saint Lucia";
      case "LI": return "Liechtenstein";
      case "LK": return "Sri Lanka";
      case "LR": return "Liberia";
      case "LS": return "Lesotho";
      case "LT": return "Lithuania";
      case "LU": return "Luxembourg";
      case "LV": return "Latvia";
      case "LY": return "Libya";
      case "MA": return "Morocco";
      case "MC": return "Monaco";
      case "MD": return "Moldova, Republic of";
      case "ME": return "Montenegro";
      case "MF": return "Saint Martin (French part)";
      case "MG": return "Madagascar";
      case "MH": return "Marshall Islands";
      case "MK": return "Macedonia, the former Yugoslav Republic of";
      case "ML": return "Mali";
      case "MM": return "Myanmar";
      case "MN": return "Mongolia";
      case "MO": return "Macao";
      case "MP": return "Northern Mariana Islands";
      case "MQ": return "Martinique";
      case "MR": return "Mauritania";
      case "MS": return "Montserrat";
      case "MT": return "Malta";
      case "MU": return "Mauritius";
      case "MV": return "Maldives";
      case "MW": return "Malawi";
      case "MX": return "Mexico";
      case "MY": return "Malaysia";
      case "MZ": return "Mozambique";
      case "NA": return "Namibia";
      case "NC": return "New Caledonia";
      case "NE": return "Niger";
      case "NF": return "Norfolk Island";
      case "NG": return "Nigeria";
      case "NI": return "Nicaragua";
      case "NL": return "Netherlands";
      case "NO": return "Norway";
      case "NP": return "Nepal";
      case "NR": return "Nauru";
      case "NU": return "Niue";
      case "NZ": return "New Zealand";
      case "OM": return "Oman";
      case "PA": return "Panama";
      case "PE": return "Peru";
      case "PF": return "French Polynesia";
      case "PG": return "Papua New Guinea";
      case "PH": return "Philippines";
      case "PK": return "Pakistan";
      case "PL": return "Poland";
      case "PM": return "Saint Pierre and Miquelon";
      case "PN": return "Pitcairn";
      case "PR": return "Puerto Rico";
      case "PS": return "Palestine, State of";
      case "PT": return "Portugal";
      case "PW": return "Palau";
      case "PY": return "Paraguay";
      case "QA": return "Qatar";
      case "RE": return "Réunion";
      case "RO": return "Romania";
      case "RS": return "Serbia";
      case "RU": return "Russian Federation";
      case "RW": return "Rwanda";
      case "SA": return "Saudi Arabia";
      case "SB": return "Solomon Islands";
      case "SC": return "Seychelles";
      case "SD": return "Sudan";
      case "SE": return "Sweden";
      case "SG": return "Singapore";
      case "SH": return "Saint Helena, Ascension and Tristan da Cunha";
      case "SI": return "Slovenia";
      case "SJ": return "Svalbard and Jan Mayen";
      case "SK": return "Slovakia";
      case "SL": return "Sierra Leone";
      case "SM": return "San Marino";
      case "SN": return "Senegal";
      case "SO": return "Somalia";
      case "SR": return "Suriname";
      case "SS": return "South Sudan";
      case "ST": return "Sao Tome and Principe";
      case "SV": return "El Salvador";
      case "SX": return "Sint Maarten (Dutch part)";
      case "SY": return "Syrian Arab Republic";
      case "SZ": return "Swaziland";
      case "TC": return "Turks and Caicos Islands";
      case "TD": return "Chad";
      case "TF": return "French Southern Territories";
      case "TG": return "Togo";
      case "TH": return "Thailand";
      case "TJ": return "Tajikistan";
      case "TK": return "Tokelau";
      case "TL": return "Timor-Leste";
      case "TM": return "Turkmenistan";
      case "TN": return "Tunisia";
      case "TO": return "Tonga";
      case "TR": return "Turkey";
      case "TT": return "Trinidad and Tobago";
      case "TV": return "Tuvalu";
      case "TW": return "Chinese Taipei";
      case "TZ": return "Tanzania, United Republic of";
      case "UA": return "Ukraine";
      case "UG": return "Uganda";
      case "UM": return "United States Minor Outlying Islands";
      case "US": return "United States";
      case "UY": return "Uruguay";
      case "UZ": return "Uzbekistan";
      case "VA": return "Holy See (Vatican City State)";
      case "VC": return "Saint Vincent and the Grenadines";
      case "VE": return "Venezuela, Bolivarian Republic of";
      case "VG": return "Virgin Islands, British";
      case "VI": return "Virgin Islands, U.S.";
      case "VN": return "Viet Nam";
      case "VU": return "Vanuatu";
      case "WF": return "Wallis and Futuna";
      case "WS": return "Samoa";
      case "YE": return "Yemen";
      case "YT": return "Mayotte";
      case "ZA": return "South Africa";
      case "ZM": return "Zambia";
      case "AA": return "User defined";
      case "XZ": return "International waters";
      case "QO": return "Oceania";
      case "ZZ": return "Unknown or invalid (used internally at GBIF for int. orgs)";
      default: return name;
    }
  };
});
