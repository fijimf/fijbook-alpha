'use strict';

import React, { Component } from 'react';

export class Quote extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        let q = this.state;
        console.log(q)
        if (q && q.quote) {
            return <div className="quote-box">
                {this.voteButton(q.isLiked, q.canVote, q.quote.id)}
                <a className="quote" title={q.quote.source}><em>{q.quote.quote}</em></a>
            </div>;
        } else {
            return <div className="quote-box">
            </div>;
        }
    }

    voteButton(isLiked, canVote, id) {
        if (canVote) {
            return <a href={'/api/vote/quote/' + id} title="Like this quote">
                <i className="fa fa-heart ml-0 mr-2 pt-1 vote-button-unliked"> </i>
            </a>;
        } else {
            if (isLiked) {
                return <a href="#" title="Quote already like this week." className="vote-disabled">
                    <i className="fa fa-heart ml-0 mr-2 pt-1 vote-button-liked"> </i>
                </a>;
            } else {
                return <a href="#" title="Log in or create an account to like this quote" className="vote-disabled">
                    <i className="fa fa-heart ml-0 mr-2 pt-1 vote-button-unliked"> </i>
                </a>;
            }
        }
    }

    componentDidMount() {
        let component = this;
        $.getJSON("/deepfij/quotes/random", function (data) {
            component.setState(data);
        });
    }
}