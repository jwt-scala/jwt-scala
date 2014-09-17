angular.module('app', ['ngAnimate'])

.config(['$provide', '$httpProvider', function ($provide, $httpProvider) {
  $provide.factory('AuthenticationInterceptor', ['$q', '$rootScope', function ($q, $rootScope) {
    return {
      'request': function (config) {
        config.headers['Authorization'] = JWT.get();
        return config;
      },

      'responseError': function (rejection) {
        if (rejection.status === 401) {
          // User isn't authenticated
          $rootScope.$emit('notification', 'warning', 'You need to be authenticated to access such API.')
        } else if (rejection.status === 403) {
          // User is authenticated but do not have permission to access this API
          $rootScope.$emit('notification', 'warning', 'Sorry, you do not have access to this API... Maybe if your username was "admin"... who knows...')
        }

        return $q.reject(rejection);
      }
    }
  }]);

  $httpProvider.interceptors.push('AuthenticationInterceptor');
}])

.run(['$rootScope', 'Authenticated', function ($rootScope, Authenticated) {
  $rootScope.$on('notification', function (e, severity, message) {
    $rootScope.notification = {severity: severity, message: message};
  });

  $rootScope.closeNotification = function closeNotification() {
    $rootScope.notification = null;
  };

  $rootScope.user = Authenticated.current
}])

.factory('Authenticated', ['$http', '$rootScope', function ($http, $rootScope) {
  var user = null;
  sync();

  function sync() {
    var session = JWT.remember();
    user = session && session.claim && session.claim.user;
    $rootScope.authenticated = !!user;
  }

  function login (data) {
    return $http.post('/api/login', data).then(function (response) {
      var token = response.headers("Authorization");
      var session = JWT.read(token);

      if (JWT.validate(session)) {
        JWT.keep(session);
        sync();
      } else {
        logout();
      }
    });
  }

  function logout() {
    JWT.forget();
    sync();
  }

  function isAuthenticated() {
    return !!user;
  }

  function current() {
    return user;
  }

  return {
    login: login,
    logout: logout,
    isAuthenticated: isAuthenticated,
    current: current
  }
}])

.controller('HomeCtrl', ['$scope', '$http', 'Authenticated', function ($scope, $http, Authenticated) {
  var ctrl = this;
  ctrl.notification = null;

  ctrl.test = "test";

  ctrl.loginForm = {};

  ctrl.login = function login() {
    Authenticated.login(ctrl.loginForm).then(function () {
      ctrl.loginForm = {};
    }, function (error) {
      ctrl.notif('error', 'Invalid username or password!');
    });
  };

  ctrl.logout = function logout() {
    Authenticated.logout();
  };

  function get(endpoint) {
    return $http.get(endpoint).then(function (response) {
      ctrl.notif('success', response.data);
    }, function (error) {
      // 401 and 403 errors are already handled by the interceptor
    });
  }

  ctrl.publicCall = function publicCall() {
    get('/api/public');
  };

  ctrl.privateCall = function privateCall() {
    get('/api/private');
  };

  ctrl.adminCall = function privateCall() {
    get('/api/admin');
  };

  ctrl.notif = function notif(severity, message) {
    $scope.$emit('notification', severity, message);
  };
}]);
