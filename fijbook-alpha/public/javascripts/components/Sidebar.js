'use strict';

class Sidebar extends React.Component {
    constructor(props) {
        super(props);
    }

    render() {
        return <nav className="col-md-2 d-none d-md-block bg-light sidebar">
            <div className="sidebar-sticky">
                <ul className="nav flex-column">
                    <li className="nav-item">
                        <a className="nav-link active" href="#">
                            <span className="fa fa-newspaper">&nbsp;&nbsp;</span>
                            Front Page <span className="sr-only">(current)</span>
                        </a>
                    </li>
                    <li className="nav-item">
                        <a className="nav-link" href="#">
                            <span className="fa fa-basketball-ball">&nbsp;&nbsp;</span>
                            Teams
                        </a>
                    </li>
                    <li className="nav-item">
                        <a className="nav-link" href="#">
                            <span className="fa fa-table">&nbsp;&nbsp;</span>
                            Conferences
                        </a>
                    </li>
                    <li className="nav-item">
                        <a className="nav-link" href="#">
                            <span className="fa fa-calendar">&nbsp;&nbsp;</span>
                            Games
                        </a>
                    </li>
                    <li className="nav-item">
                        <a className="nav-link" href="#">
                            <span className="fa fa-bar-chart-o">&nbsp;&nbsp;</span>
                            Statistics
                        </a>
                    </li>
                    <li className="nav-item">
                        <a className="nav-link" href="#">
                            <span className="fa fa-grip-horizontal">&nbsp;&nbsp;</span>
                            Blog
                        </a>
                    </li>
                    <li className="nav-item">
                        <a className="nav-link" href="#">
                            <span className="fa fa-question-circle">&nbsp;&nbsp;</span>
                            About
                        </a>
                    </li>
                </ul>

                <h6 className="sidebar-heading d-flex justify-content-between align-items-center px-3 mt-4 mb-1 text-muted">
                    <span>Favorites</span>
                    <a className="d-flex align-items-center text-muted" href="#">
                        <span className="fa a-star-o">&nbsp;&nbsp;</span>
                    </a>
                </h6>
                <ul className="nav flex-column mb-2">
                    <li className="nav-item">
                        <a className="nav-link" href="#">
                            <span className="fa fa-star-o">&nbsp;&nbsp;</span>
                            Georgetown
                        </a>
                    </li>
                    <li className="nav-item">
                        <a className="nav-link" href="#">
                            <span className="fa fa-star-o">&nbsp;&nbsp;</span>
                            Gonzaga
                        </a>
                    </li>
                    <li className="nav-item">
                        <a className="nav-link" href="#">
                            <span className="fa fa-star-o">&nbsp;&nbsp;</span>
                            RPI
                        </a>
                    </li>
                </ul>
            </div>
        </nav>;
    }
}

const sidebar= document.querySelector('#sidebar');
ReactDOM.render(React.createElement(Sidebar), sidebar);
