'use strict';

import React, { Component } from 'react';

import { Quote } from './Quote.js';

export class TopNav extends React.Component {

    constructor(props) {
        super(props);
        this.state = props;
        console.info(this.state);
    }


    render() {
        const isLoggedIn = this.state.isLoggedIn;
        const user = this.state.user;
        const userName = user.name;
        const emptyQuote = {
            id: -1,
            quote: '',
            source: ''
        };
        return <nav className="navbar navbar-expand-sm navbar-dark fixed-top bg-dark flex-md-nowrap p-0 shadow">
            <a className="navbar-brand col-sm-2 mr-0" href="#"><img
                src="/assets/images/deepfij-tiny.png" width="30" height="34"
                className="d-inline-block mx-3" alt=""/>deepfij</a>
            <button className="navbar-toggler" type="button" data-toggle="collapse" data-target="#navbarsExample03"
                    aria-controls="navbarsExample03" aria-expanded="false" aria-label="Toggle navigation">
                <span className="navbar-toggler-icon"/>
            </button>

            <div className="collapse navbar-collapse col-sm-8" id="navbarsExample03">
                <ul className="navbar-nav mr-auto">
                    <li className="navbar-text ml-2">
                        <Quote user={user} quote={emptyQuote}/>
                    </li>
                </ul>
                {isLoggedIn ? (
                    <ul className="navbar-nav ml-auto">
                        <li className="nav-item dropdown">
                            <a className="nav-link dropdown-toggle" href="https://example.com" id="dropdown03"
                               data-toggle="dropdown" aria-haspopup="true"
                               aria-expanded="false">{userName}</a>
                            <div className="dropdown-menu" aria-labelledby="dropdown03">
                                <a className="dropdown-item" href="#">View Profile</a>
                                <a className="dropdown-item" href="/r/signOut">Sign Out</a>
                            </div>
                        </li>
                    </ul>
                ) : (
                    <ul className="navbar-nav ml-auto">
                        <li className="nav-item ">
                            <a className="nav-link" href="/r/signIn" id="signIn">Sign In</a>
                        </li>
                    </ul>
                )}

            </div>
            <input className="form-control form-control-dark col-sm-2 mx-1 " type="text" placeholder="Search"/>
        </nav>;
    }

}






