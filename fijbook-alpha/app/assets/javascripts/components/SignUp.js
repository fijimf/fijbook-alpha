'use strict';

import React, { Component } from 'react';

export class SignUp extends React.Component {

    constructor(props) {
        super(props);
        this.state = props;
    }

    render() {
        return <div className="row">
            <fieldset className="col-md-6 col-md-offset-3">
                <legend>Sign up for a new account</legend>
                <form action="/auth/deepfij/signUp" method="POST">
                    <input type="hidden" name="csrfToken"
                           value="bb3fa591bca7b6611316ca576bcad04cddb2045f-1533872525287-2ac4b8015857af5d48664d7f"/>
                    <div className="form-group  " id="firstName_field">
                        <label className="control-label sr-only" htmlFor="firstName">First name</label>
                        <input type="text" id="firstName" name="firstName" defaultValue="" required="true"
                               placeholder="First name" className="form-control form-control input-lg"/>
                    </div>
                    <div className="form-group  " id="lastName_field">
                        <label className="control-label sr-only" htmlFor="lastName">Last name</label>
                        <input type="text" id="lastName" name="lastName" defaultValue="" required="true"
                               placeholder="Last name" className="form-control form-control input-lg"/>
                    </div>
                    <div className="form-group  " id="email_field">
                        <label className="control-label sr-only" htmlFor="email">Email</label>
                        <input type="text" id="email" name="email" defaultValue="" placeholder="Email"
                               className="form-control form-control input-lg"/>
                    </div>
                    <div className="form-group  " id="password_field">
                        <label className="control-label sr-only" htmlFor="password">Password</label>
                        <input type="password" id="password" name="password" defaultValue="" required="true"
                               placeholder="Password" className="form-control form-control input-lg"/>
                    </div>
                    <div className="form-group">
                        <div>
                            <button id="submit" type="submit" defaultValue="submit"
                                    className="btn btn-lg btn-primary btn-block">Sign up
                            </button>
                        </div>
                    </div>
                    <div className="sign-in-now">
                        <p>Already a member? <a href="/auth/deepfij/signIn">Sign in now</a></p>
                    </div>
                </form>
            </fieldset>
        </div>;
    }
}
