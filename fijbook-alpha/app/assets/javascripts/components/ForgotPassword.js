'use strict';

import React, { Component } from 'react';

export class ForgotPassword extends React.Component {

    constructor(props) {
        super(props);
        this.state = props;
    }

    render() {
        return <div className="row">
            <fieldset className="col-md-6 col-md-offset-3">
                <legend>Forgot password</legend>
                <form action="/deepfij/password/forgot" method="POST" autoComplete="off">
                    <p className="info">Please enter your email address and we will send you an email with further
                        instructions to reset your password.</p>
                    <input type="hidden" name="csrfToken"
                           value="ff8cb9041a6cc423be77c7bdd12e668a33235f85-1533913870005-d3fadd968d51a5e66781f9ce"/>
                    <div className="form-group  " id="email_field">
                        <label className="control-label sr-only" htmlFor="email">Email</label>
                        <input type="text" id="email" name="email" value="" placeholder="Email"
                               className="form-control form-control input-lg"/>
                    </div>
                    <div className="form-group">
                        <div>
                            <button id="submit" type="submit" value="submit"
                                    className="btn btn-lg btn-primary btn-block">Send
                            </button>
                        </div>
                    </div>

                </form>

            </fieldset>
        </div>;
    }
}