'use strict';

import { TopNav } from './components/TopNav.js';
import { Sidebar } from './components/Sidebar.js';
import { FrontPage } from './components/FrontPage.js';
import { SignIn } from './components/SignIn.js';
import { SignUp } from './components/SignUp.js';
import { ForgotPassword } from './components/ForgotPassword.js';
import { ResetPassword } from './components/ResetPassword.js';

import React, { Component } from 'react';
import ReactDOM from 'react-dom';

module.exports ={
    buildNav: function(displayUser) {
        ReactDOM.render(React.createElement(Sidebar), document.querySelector('#sidebar'));
        ReactDOM.render(React.createElement(TopNav, displayUser), document.querySelector('#top_nav'));
    },

    buildMain: function (component) {
        const commandTable = {
            'FrontPage': function () {
                ReactDOM.render(React.createElement(FrontPage), document.querySelector('#mainComponent'));
            },
            'SignIn': function () {
                ReactDOM.render(React.createElement(SignIn), document.querySelector('#mainComponent'));
            },
            'SignUp': function () {
                ReactDOM.render(React.createElement(SignUp), document.querySelector('#mainComponent'));
            },
            'ForgotPassword': function () {
                ReactDOM.render(React.createElement(ForgotPassword), document.querySelector('#mainComponent'));
            },
            'ResetPassword': function () {
                ReactDOM.render(React.createElement(ResetPassword), document.querySelector('#mainComponent'));
            }
        };
        commandTable[component]();
    }

};