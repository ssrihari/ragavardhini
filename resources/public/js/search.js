var Ragam = React.createClass({
  render: function() {
    return (
	<div className="ragam">
	<h2 className="ragam-name">{this.props.name}</h2>
	  <p className="notes">{this.props.arohanam}</p>
	  <p className="notes">{this.props.avarohanam}</p>
	  <p className="more-info">{this.props.more_info}</p>
	</div>
    );
  }
});

var SearchResult = React.createClass({
  render: function() {
    return (
      <div className="search-result">
        Hello, world! I am the search results.
        <Ragam name="malahari" arohanam="something here" avarohanam="another thing" more_info="some yet another thing"/>
      </div>
    );
  }
});

React.render(
  <SearchResult />,
  document.getElementById('container')
);