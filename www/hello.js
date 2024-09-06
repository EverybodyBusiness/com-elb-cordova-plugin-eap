/*global cordova, module*/

module.exports = {
    printerInit: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Hello", "printerInit", []);
    },
    print: function (data, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Hello", "print", [data]);
    },
    nfcInit: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Hello", "nfcInit", []);
    },
    nfcRead: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Hello", "nfcRead", []);
    },
    nfcClose: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Hello", "nfcClose", []);
    },
};
