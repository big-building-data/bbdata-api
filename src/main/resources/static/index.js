function fetchDynamicInfo() {
    const xhr = new XMLHttpRequest();
    xhr.onreadystatechange = function () {
        if (xhr.readyState === 4) {
            populateDynamicInfo( JSON.parse(xhr.responseText));
        }
    };
    xhr.open('GET', '/about');
    xhr.send();
}

function populateDynamicInfo(json) {
    _innerHTML(json, 'instance-name');
    _innerHTML(json, 'repo-url', null, `
        <a href="${json['repo-url']}" target="_blank" class="button button-secondary">Code</a>
    `);

    delete json['server-time'];
    const infoBlock = document.getElementById('pre-info');
    infoBlock.innerHTML = `
    <h2>Running instance information</h2>
    <pre><code class="json">${JSON.stringify(json, null, 4)}</code></pre>`;
    hljs.highlightBlock(infoBlock.getElementsByTagName("code")[0]);

}

function _innerHTML(json, key, id, html) {
    if (json[key]) document.getElementById(id || key).innerHTML = html || json[key];
}

document.addEventListener('DOMContentLoaded', (event) => {
    fetchDynamicInfo();
    document.querySelectorAll('pre code[class]').forEach((block) => {
        hljs.highlightBlock(block);
    });
});