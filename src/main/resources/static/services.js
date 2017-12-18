angular.module('smartdlk.services', ['ngResource'])

.factory('rulesService', function($resource) {
    return $resource('api/rules', {}, {
        query: {
            url: 'api/rules',
            isArray: true
        },
        activate: {
            url: 'api/rules/:rule/activate',
            method: 'POST',
            params: { rule: '@rule' }
        },
        deactivate: {
            url: 'api/rules/:rule/deactivate',
            method: 'POST',
            params: { rule: '@rule' }
        }
    });
})

.factory('errorHandler', function() {
    return function(error) {
        console.log('ERROR: ', error);
    }
});