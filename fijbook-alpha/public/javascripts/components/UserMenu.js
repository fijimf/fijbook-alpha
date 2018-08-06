'use strict';

class UserMenu extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const user = this.props.user;
        if (user) {

            return <ul className="nav navbar-right top-nav">
                <li className="dropdown">

                    <a href="#" className="dropdown-toggle" data-toggle="dropdown">
                        <i className="fa fa-user"> </i>
                        {user.name}<b className="caret"> </b>
                    </a>
                    <ul className="dropdown-menu">
                        <li>
                            <a href="/deepfij/profile/{user.key}"><i className="fa fa-fw fa-user"></i> Profile</a>
                        </li>
                        <li className="divider"></li>
                        <li>
                            <a href="/deepfij/signOut"><i className="fa fa-fw fa-power-off"></i> Sign Out</a>
                        </li>
                    </ul>

                </li>
            </ul>;
        } else {
            return <ul className="nav navbar-right top-nav">
                <li className="dropdown">

                    <a href="#" className="dropdown-toggle" data-toggle="dropdown">
                        <i className="fa fa-user"> </i>
                        Guest<b className="caret"> </b>
                    </a>
                    <ul className="dropdown-menu">
                        <li>
                            <a href="#"><i className="fa fa-fw fa-user"></i> Profile</a>
                        </li>
                        <li className="divider"></li>
                        <li>
                            <a href="/deepfij/signIn"><i className="fa fa-fw fa-power-off"></i> Sign In</a>
                        </li>
                    </ul>

                </li>
            </ul>;
        }
    }
}

ReactDOM.render(<UserMenu user={{"name": "Jim F", "key": "fijimf"}}/>, document.querySelector('#user-menu'));