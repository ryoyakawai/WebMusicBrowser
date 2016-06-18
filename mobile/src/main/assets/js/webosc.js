/*
  Copyright 2016 Ryoya Kawai
  Licensed under the Apache License, Version 2.0 (the "License");
  you may not use this file except in compliance with the License.
  You may obtain a copy of the License at
  http://www.apache.org/licenses/LICENSE-2.0
  Unless required by applicable law or agreed to in writing, software
  distributed under the License is distributed on an "AS IS" BASIS,
  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  See the License for the specific language governing permissions and
  limitations under the License.
*/
var customObj=osc;
window.navigator.requestOSCAccess=function(param) {
    
    return new Promise(function(resolve, reject) {
        osc.requestOSCAccess();
        if(typeof osc!="undefined") {
            var ret = {
                getIPAddress: function() {
                    return customObj.getIPAddress();
                },
                setClient: function(targetIP, targetPort) {
                    customObj.setClient(targetIP, targetPort);
                },
                send: function(json) {
                    customObj.send(json);
                },
                setServer: function(listenPort, addressPattern) {
                    var status=(customObj.setServer(listenPort, addressPattern)=="true"?true:false);
                    return status;
                },
                startServer: function() {
                    var status=(customObj.startServer()=="true"?true:false);
                    this.startPassEvent();
                    return status;
                },
                stopServer: function() {
                    customObj.stopServer();
                    this.stopPassEvent();
                },
                onoscmessage: function(msg) {
                    console.log("[Event: onoscmessage] " , msg);
                },
                startPassEvent: function () {
                    document.addEventListener("onoscmessage", this.passMessage.bind(this), false);
                },
                stopPassEnvent: function () {
                    document.removeEventListener("onoscmessage", this.passMessage.bind(this), false);
                },
                passMessage: function(msg) {
                    this.onoscmessage(msg);
                }
            };
            resolve(ret);
        } else {
            reject("This broser is not support Web OSC.");
        }

    });
};
