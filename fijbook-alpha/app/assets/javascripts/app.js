'use strict';

import {TopNav} from './components/TopNav.js';
import {Sidebar} from './components/Sidebar.js';
import React, { Component } from "react";
import ReactDOM from "react-dom";
module.exports ={
    buildNav: function(displayUser) {
        ReactDOM.render(React.createElement(Sidebar), document.querySelector('#sidebar'));
        ReactDOM.render(React.createElement(TopNav, displayUser), document.querySelector('#top_nav'));
    }
}