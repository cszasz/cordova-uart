/**
 * Copyright (c) Yu Yen Kan, 2020
 */
module.exports = {
	list: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'UART',
            'list',
            [{'opts': {}}]
        );
    },
	open: function(opts, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'UART',
            'open',
            [{'opts': opts}]
        );
    },
	write: function(opts, successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'UART',
            'write',
            [{'opts': opts}]
        );
    },
	close: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'UART',
            'close',
            []
        );
    },
    registerReadCallback: function(successCallback, errorCallback) {
        cordova.exec(
            successCallback,
            errorCallback,
            'UART',
            'register_callback',
            []
        );
    }
}