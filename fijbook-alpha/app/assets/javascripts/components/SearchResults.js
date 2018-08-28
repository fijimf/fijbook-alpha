'use strict';

import React, { Component } from 'react';

export class SearchResults extends React.Component {

    constructor(props) {
        super(props);
        this.state = props;
        console.info(this.state);
    }


    render() {
        return <div
            className="d-flex justify-content-between flex-wrap flex-md-nowrap align-items-center pt-3 pb-2 mb-3 border-bottom">
            <h1 className="h2">Deep Fij</h1>
            <div className="btn-toolbar mb-2 mb-md-0">
                <div className="btn-group mr-2">
                    <button className="btn btn-sm btn-outline-secondary">Share</button>
                    <button className="btn btn-sm btn-outline-secondary">Export</button>
                </div>
                <button className="btn btn-sm btn-outline-secondary dropdown-toggle">
                    This week
                </button>
            </div>
        </div>;
    }
}