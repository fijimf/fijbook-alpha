'use strict';

class TopNav extends React.Component {

    constructor(props) {
        super(props);
        this.state = props;
        console.info(this.state);
    }


    render() {
        const isLoggedIn = this.state.isLoggedIn;
        const userName = this.state.user.name;
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
                        <Quote></Quote>
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


class Quote extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            quote: ""
        };
    }

    render() {
        return <div className="quote-box">
            <a href={"/api/vote/quote/"+this.state.quote} title="Like this quote"><i className="fa fa-heart ml-0 mr-2 pt-1 vote-button-liked" > </i></a>
            <a className="quote"><em>{this.state.quote}</em></a>

        </div>;
    }

    componentDidMount() {
        let component = this;
        $.getJSON("/deepfij/quotes/random", function (data) {
            component.setState(data);
        });
    }
}

const topNav= document.querySelector('#top_nav');
ReactDOM.render(React.createElement(TopNav, displayUser), topNav);

