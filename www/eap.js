/*global cordova, module*/

module.exports = {
    printerInit: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Eap", "printerInit", []);
    },
    print: function (data, successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Eap", "print", [data]);
    },
    nfcInit: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Eap", "nfcInit", []);
    },
    nfcRead: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Eap", "nfcRead", []);
    },
    nfcClose: function (successCallback, errorCallback) {
        cordova.exec(successCallback, errorCallback, "Eap", "nfcClose", []);
    },
};
