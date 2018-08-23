'use strict';

import React, { Component } from 'react';

export class Quote extends React.Component {
    constructor(props) {
        super(props);
        this.handleClick = this.handleClick.bind(this)
    }

    handleClick() {
        let component = this;
        $.getJSON('/api/vote/quote/'+this.state.quote.id, function (data) {
            component.setState(data);
        });
    }

     voteButton(isLiked, canVote, id) {
        if (canVote) {
            return <a href="#" title="Like this quote" onClick={this.handleClick}>
                <i className="fa fa-heart ml-0 mr-2 pt-1 vote-button-unliked"> </i>
            </a>;
        } else {
            if (isLiked) {
                return <a href="#" title="Quote already liked this week" className="vote-disabled">
                    <i className="fa fa-heart ml-0 mr-2 pt-1 vote-button-liked"> </i>
                </a>;
            } else {
                return <a href="#" title="Log in or create an account to like this quote" className="vote-disabled">
                    <i className="fa fa-heart ml-0 mr-2 pt-1 vote-button-unliked"> </i>
                </a>;
            }
        }
    }

    render() {
        let q = this.state;
        console.log(q);
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

    componentDidMount() {
        let component = this;
        $.getJSON('/deepfij/quotes/random', function (data) {
            component.setState(data);
        });
    }





}