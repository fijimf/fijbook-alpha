'use strict';

/*
 {
    user:{

    sidebar:{

    }
 }
 */
class TopNav extends React.Component {

    render() {
        return <nav className="navbar navbar-dark fixed-top bg-dark flex-md-nowrap p-0 shadow">
            <a className="navbar-brand col-sm-3 col-md-2 mr-0" href="#"><img
                src="/assets/images/deepfij-tiny.png" width="30" height="30"
                className="d-inline-block align-top mx-3" alt="" />Deep FIJ</a>
            <span className="navbar-text w-75 ml-2">
                <Quote></Quote>
            </span>
            <input className="form-control form-control-dark w-25 mr-sm-2" type="text" placeholder="Search"
                   aria-label="Search"/>
            <ul className="navbar-nav px-3">
                <li className="nav-item text-nowrap">
                    <a className="nav-link" href="#">Sign out</a>
                </li>
            </ul>
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
            <a className="quote"><em>{this.state.quote}</em></a>
            <a href={"/api/vote/quote/"+this.state.quote} title="Like this quote"><i className="fa fa-heart mx-1 vote-button-unliked" > </i></a>
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
ReactDOM.render(React.createElement(TopNav), topNav);

