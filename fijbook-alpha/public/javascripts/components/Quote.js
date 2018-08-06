'use strict';

class Quote extends React.Component {
    constructor(props) {
        super(props);
        this.state = {
            quote: ""
        };
    }

    render() {
        return <a class="quote"><em>{this.state.quote}</em></a>
    }

    componentDidMount() {
        let component = this;
        $.getJSON("/deepfij/quotes/random", function (data) {
            component.setState(data);
        });
    }
}

const domContainer = document.querySelector('#quote_container');
ReactDOM.render(React.createElement(Quote), domContainer);