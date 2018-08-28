'use strict';

import React, { Component } from 'react';

export class ResetPassword extends React.Component {

    constructor(props) {
        super(props);
        this.state = props;
        console.info(this.state);
    }

    render() {
        return <div className="row">
            <fieldset className="col-md-6 col-md-offset-3">
                <legend>Sign in with your credentials</legend>
                <form action="/auth/deepfij/signIn" method="POST" > //TODO fixme
                    <input type="hidden" name="csrfToken" value="70a278b055a6d73f31f2479601d832ec888f4cd0-1533871115934-2ac4b8015857af5d48664d7f"/>
                    <div className="form-group  " id="email_field">
                        <label className="control-label sr-only" htmlFor="email">Email</label>
                        <input type="email" id="email" name="email" defaultValue="" placeholder="Email" className="form-control form-control input-lg"/>
                    </div>
                    <div className="form-group  " id="password_field">
                        <label className="control-label sr-only" htmlFor="password">Password</label>
                        <input type="password" id="password" name="password" defaultValue="" required="true" placeholder="Password" className="form-control form-control input-lg"/>
                    </div>
                    <div className="form-group  " id="rememberMe_field">
                        <div className="checkbox">
                            <label htmlFor="rememberMe">
                                <input type="checkbox" id="rememberMe" name="rememberMe" value="true" defaultChecked="true"/>
                                    Remember my login on this computer
                            </label>
                        </div>
                    </div>
                    <div className="form-group">
                        <div>
                            <button id="submit" type="submit" value="submit" className="btn btn-lg btn-primary btn-block">Sign in</button>
                        </div>
                    </div>
                </form>
                <div>
                    <p className="not-a-member">Not a member? <a href="/r/signUp">Sign up now</a> | <a href="/auth/deepfij/password/forgot" title="Forgot your password?">Forgot your password?</a></p>
                </div>
            </fieldset>
        </div>;
    }
}
