'use strict';

class Breadcrumbs extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return <ol className="breadcrumb">
            {
                this.props.crumbs.map(c => <li><i className="fa"/><a href={c.link}>{c.display}</a></li>)
            }
        </ol>;
    }
}

const crumbs = [
    {
        display: "Deep Fij",
        link: "/deepfij",
        fontAwesomeIcon: "fa-circle-o"
    },
    {
        display: "Teams",
        link: "/deepfij/teams",
        fontAwesomeIcon: "fa-users"
    },
    {
        display: "Appalacian St.",
        link: "/deepfij/teams/appaliachian-st",
        fontAwesomeIcon: ""
    },
];

ReactDOM.render(<Breadcrumbs crumbs={crumbs}/>, document.querySelector('#breadcrumbs'));
