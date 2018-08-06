'use strict';

class MainMenu extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        const isAdmin = this.props.user.isAdmin;
        return <div className="collapse navbar-collapse navbar-ex1-collapse">
            <ul className="nav navbar-nav side-nav">
                <li><a href="/deepfij/index"><i className="fa fa-fw fa-dashboard"> </i> Dashboard</a></li>
                <li className="active"><a href="/deepfij/teams>"><i className="fa fa-fw fa-users"> </i> Teams</a></li>
                <li><a href="/deepfij/conferences"><i className="fa fa-fw fa-list"> </i> Conferences</a></li>
                <li><a href="/deepfij/games"><i className="fa fa-fw fa-calendar"> </i> Games</a></li>
                <li><a href="/deepfij/stats"><i className="fa fa-fw fa-area-chart"> </i> Statistics</a></li>
                <li><a href="/deepfij/blog"><i className="fa fa-fw  fa-book"> </i> Blog</a></li>
                <li><a href="/deepfij/about"><i className="fa fa-fw fa-question"> </i> About</a></li>
                {isAdmin && <li><a href="/deepfij/admin"><i className="fa fa-fw fa-wrench"> </i> Admin</a></li>}
            </ul>
        </div>;
    }
}

ReactDOM.render(<MainMenu user={{"isAdmin": false}}/>, document.querySelector('#main-menu'));